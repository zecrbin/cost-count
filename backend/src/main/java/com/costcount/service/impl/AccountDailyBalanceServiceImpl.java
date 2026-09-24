package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.costcount.common.LedgerBalance;
import com.costcount.dto.account.AccountDailyBalanceQueryDTO;
import com.costcount.entity.AccountDailyBalance;
import com.costcount.entity.AccountType;
import com.costcount.entity.Transaction;
import com.costcount.exception.BizException;
import com.costcount.mapper.AccountDailyBalanceMapper;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.mapper.TransactionMapper;
import com.costcount.service.AccountDailyBalanceService;
import com.costcount.vo.account.AccountDailyBalanceVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.costcount.common.CommonConstant.CREDIT_ACCOUNT_TYPE;
import static com.costcount.common.TransactionConstant.TRANSFER;

/** 根据流水事实重建账户活动日余额缓存。 */
@Service
public class AccountDailyBalanceServiceImpl
        extends MPJBaseServiceImpl<AccountDailyBalanceMapper, AccountDailyBalance>
        implements AccountDailyBalanceService {

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private TransactionMapper transactionMapper;

    @Resource
    private AccountTypeMapper accountTypeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appendTransactionChanges(LocalDate statDate, Map<Long, BigDecimal> changes,
                                         Map<Long, BigDecimal> closingBalances) {
        if (changes == null || changes.isEmpty()) {
            return;
        }
        List<AccountDailyBalance> dailyBalances = baseMapper.selectList(
                new LambdaQueryWrapper<AccountDailyBalance>()
                .in(AccountDailyBalance::getAccountId, changes.keySet())
                .eq(AccountDailyBalance::getStatDate, statDate));
        Map<Long, AccountDailyBalance> dailyBalanceMap = new HashMap<>();
        dailyBalances.forEach(item -> dailyBalanceMap.put(item.getAccountId(), item));

        List<AccountDailyBalance> inserts = new ArrayList<>();
        List<AccountDailyBalance> updates = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : changes.entrySet()) {
            Long accountId = entry.getKey();
            BigDecimal change = entry.getValue();
            BigDecimal closingBalance = closingBalances.get(accountId);
            if (closingBalance == null) {
                throw new BizException(500, "账户余额计算异常");
            }
            AccountDailyBalance dailyBalance = dailyBalanceMap.get(accountId);
            if (dailyBalance == null) {
                inserts.add(buildDailyBalance(
                        accountId, statDate, closingBalance.subtract(change), change));
                continue;
            }
            dailyBalance.setTransactionChange(defaultZero(dailyBalance.getTransactionChange()).add(change));
            dailyBalance.setClosingBalance(closingBalance);
            dailyBalance.setRebuiltTime(LocalDateTime.now());
            updates.add(dailyBalance);
        }
        if (!inserts.isEmpty()) {
            baseMapper.insert(inserts);
        }
        if (!updates.isEmpty()) {
            baseMapper.updateById(updates);
        }
    }

    @Override
    public List<AccountDailyBalanceVO> listAccountDailyBalances(AccountDailyBalanceQueryDTO query) {
        if (query.getStartDate() != null && query.getEndDate() != null
                && query.getStartDate().isAfter(query.getEndDate())) {
            throw new BizException(400, "开始日期不能晚于结束日期");
        }
        if (accountMapper.selectById(query.getAccountId()) == null) {
            throw new BizException(404, "账户不存在");
        }
        List<AccountDailyBalance> balances = baseMapper.selectList(new LambdaQueryWrapper<AccountDailyBalance>()
                .eq(AccountDailyBalance::getAccountId, query.getAccountId())
                .ge(query.getStartDate() != null, AccountDailyBalance::getStatDate, query.getStartDate())
                .le(query.getEndDate() != null, AccountDailyBalance::getStatDate, query.getEndDate())
                .orderByAsc(AccountDailyBalance::getStatDate));
        return balances.stream().map(balance -> {
            AccountDailyBalanceVO vo = new AccountDailyBalanceVO();
            BeanUtils.copyProperties(balance, vo);
            return vo;
        }).toList();
    }

    @Override
    public BigDecimal getClosingBalanceBefore(Long accountId, LocalDate statDate) {
        if (accountId == null || statDate == null) {
            throw new BizException(400, "账户 ID 和快照日期不能为空");
        }
        return calculateClosingBalanceBefore(accountId, statDate, isCreditAccount(accountId));
    }

    private BigDecimal calculateClosingBalanceBefore(Long accountId, LocalDate statDate, boolean credit) {
        AccountDailyBalance previousBalance = baseMapper.selectOne(new LambdaQueryWrapper<AccountDailyBalance>()
                .eq(AccountDailyBalance::getAccountId, accountId)
                .lt(AccountDailyBalance::getStatDate, statDate)
                .orderByDesc(AccountDailyBalance::getStatDate)
                .last("LIMIT 1"));
        BigDecimal closingBalance = previousBalance == null
                ? BigDecimal.ZERO
                : defaultZero(previousBalance.getClosingBalance());
        LocalDate gapStartDate = previousBalance == null ? null : previousBalance.getStatDate().plusDays(1);
        if (gapStartDate != null && !gapStartDate.isBefore(statDate)) {
            return closingBalance;
        }

        LambdaQueryWrapper<Transaction> gapWrapper = new LambdaQueryWrapper<Transaction>()
                .and(w -> w
                        .eq(Transaction::getAccountId, accountId)
                        .or()
                        .eq(Transaction::getTargetAccountId, accountId))
                .lt(Transaction::getTransactionTime, statDate.atStartOfDay());
        gapWrapper.ge(gapStartDate != null, Transaction::getTransactionTime,
                gapStartDate == null ? null : gapStartDate.atStartOfDay());
        List<Transaction> gapTransactions = transactionMapper.selectList(gapWrapper);
        for (Transaction transaction : gapTransactions) {
            closingBalance = closingBalance.add(defaultZero(calculateChange(accountId, transaction, credit)));
        }
        return closingBalance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildAccountDailyBalance(Long accountId, LocalDate fromDate) {
        boolean credit = isCreditAccount(accountId);

        BigDecimal openingBalance = fromDate == null
                ? BigDecimal.ZERO
                : calculateClosingBalanceBefore(accountId, fromDate, credit);
        LambdaQueryWrapper<Transaction> transactionWrapper = new LambdaQueryWrapper<Transaction>()
                .and(wrapper -> wrapper.eq(Transaction::getAccountId, accountId)
                        .or().eq(Transaction::getTargetAccountId, accountId))
                .orderByAsc(Transaction::getTransactionTime)
                .orderByAsc(Transaction::getId);
        transactionWrapper.ge(fromDate != null, Transaction::getTransactionTime,
                fromDate == null ? null : fromDate.atStartOfDay());
        List<Transaction> transactions = transactionMapper.selectList(transactionWrapper);
        Map<LocalDate, BigDecimal> dailyChanges = new TreeMap<>();
        for (Transaction transaction : transactions) {
            if (transaction.getTransactionTime() == null) {
                continue;
            }
            BigDecimal change = calculateChange(accountId, transaction, credit);
            if (change == null) {
                continue;
            }
            LocalDate transactionDate = transaction.getTransactionTime().toLocalDate();
            dailyChanges.merge(transactionDate, change, BigDecimal::add);
        }

        List<AccountDailyBalance> balances = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> entry : dailyChanges.entrySet()) {
            balances.add(buildDailyBalance(accountId, entry.getKey(), openingBalance, entry.getValue()));
            openingBalance = openingBalance.add(entry.getValue());
        }

        LambdaQueryWrapper<AccountDailyBalance> deleteWrapper = new LambdaQueryWrapper<AccountDailyBalance>()
                .eq(AccountDailyBalance::getAccountId, accountId);
        deleteWrapper.ge(fromDate != null, AccountDailyBalance::getStatDate, fromDate);
        remove(deleteWrapper);
        if (!balances.isEmpty()) {
            saveBatch(balances);
        }
    }

    /**
     * 计算当前账户在一笔流水中的余额变化。
     *
     * <p>主账户直接使用已落库的变化值；转账目标账户没有独立变化字段，需要按账户性质反推，
     * 规则见 {@link LedgerBalance#targetChange(BigDecimal, boolean)}。</p>
     */
    private BigDecimal calculateChange(Long accountId, Transaction transaction, boolean credit) {
        if (accountId.equals(transaction.getAccountId())) {
            return transaction.getBalanceChange();
        }
        if (!TRANSFER.equals(transaction.getTransactionType()) || transaction.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        return LedgerBalance.targetChange(transaction.getAmount(), credit);
    }

    /** 构造一个活动日的余额快照。 */
    private AccountDailyBalance buildDailyBalance(Long accountId, LocalDate statDate, BigDecimal openingBalance,
                                                   BigDecimal transactionChange) {
        AccountDailyBalance dailyBalance = new AccountDailyBalance();
        dailyBalance.setAccountId(accountId);
        dailyBalance.setStatDate(statDate);
        dailyBalance.setOpeningBalance(openingBalance);
        dailyBalance.setTransactionChange(transactionChange);
        dailyBalance.setCorrectionChange(BigDecimal.ZERO);
        dailyBalance.setClosingBalance(openingBalance.add(transactionChange));
        dailyBalance.setRebuiltTime(LocalDateTime.now());
        return dailyBalance;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isCreditAccount(Long accountId) {
        var account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(404, "账户不存在");
        }
        AccountType accountType = accountTypeMapper.selectById(account.getTypeId());
        if (accountType == null) {
            throw new BizException(404, "账户类型不存在");
        }
        return CREDIT_ACCOUNT_TYPE.equalsIgnoreCase(accountType.getTypeCode());
    }
}

package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.costcount.common.BalanceChanges;
import com.costcount.entity.Account;
import com.costcount.entity.AccountDailyBalance;
import com.costcount.entity.AccountType;
import com.costcount.entity.Transaction;
import com.costcount.exception.BizException;
import com.costcount.mapper.AccountDailyBalanceMapper;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.mapper.TransactionMapper;
import com.costcount.service.AccountDailyBalanceService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.costcount.common.CommonConstant.isCreditType;
import static com.costcount.common.TransactionConstant.ADJUSTMENT;

/** 维护账户活动日余额缓存：末尾流水直接追加，补充流水按流水事实重建。 */
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
    public BigDecimal getClosingBalanceBefore(Long accountId, LocalDate statDate) {
        checkArguments(accountId, statDate);
        return calculateClosingBalanceBefore(accountId, statDate, isCreditAccount(accountId));
    }

    @Override
    public BigDecimal getClosingBalanceBefore(Long accountId, LocalDate statDate, boolean credit) {
        checkArguments(accountId, statDate);
        return calculateClosingBalanceBefore(accountId, statDate, credit);
    }

    private void checkArguments(Long accountId, LocalDate statDate) {
        if (accountId == null || statDate == null) {
            throw new BizException(400, "账户 ID 和快照日期不能为空");
        }
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
            closingBalance = closingBalance.add(BalanceChanges.of(accountId, transaction, credit));
        }
        return closingBalance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appendDailyBalance(Long accountId, LocalDate statDate, BigDecimal change, boolean correction,
                                   BigDecimal closingBalance) {
        AccountDailyBalance dailyBalance = lambdaQuery()
                .eq(AccountDailyBalance::getAccountId, accountId)
                .eq(AccountDailyBalance::getStatDate, statDate)
                .one();
        if (dailyBalance == null) {
            // 当天第一笔流水：期初即入账前的余额
            save(buildDailyBalance(accountId, statDate, closingBalance.subtract(change),
                    change, correction ? change : BigDecimal.ZERO));
            return;
        }
        // 当日变动包含全部流水；余额调整另在修正列里记一份明细，不单独计入余额
        dailyBalance.setTransactionChange(defaultZero(dailyBalance.getTransactionChange()).add(change));
        if (correction) {
            dailyBalance.setCorrectionChange(defaultZero(dailyBalance.getCorrectionChange()).add(change));
        }
        dailyBalance.setClosingBalance(closingBalance);
        dailyBalance.setRebuiltTime(LocalDateTime.now());
        updateById(dailyBalance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildAccountDailyBalance(Long accountId, LocalDate fromDate) {
        // 重建会整段删除并重写快照，必须先锁账户：与并发记账交错会互相覆盖，
        // 或在 uk_account_stat_date 上撞唯一键
        List<Account> lockedAccounts = accountMapper.selectByIdsForUpdate(List.of(accountId));
        if (lockedAccounts.isEmpty()) {
            throw new BizException(404, "账户不存在");
        }

        boolean credit = isCreditType(resolveTypeCode(lockedAccounts.get(0).getTypeId()));
        BigDecimal openingBalance = fromDate == null
                ? BigDecimal.ZERO
                : calculateClosingBalanceBefore(accountId, fromDate, credit);

        rebuildDailyBalances(accountId, fromDate, credit, openingBalance,
                transactionMapper.selectAccountTransactionsFrom(accountId, fromDate));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildDailyBalances(Long accountId, LocalDate fromDate, boolean credit,
                                     BigDecimal openingBalance, List<Transaction> transactions) {
        Map<LocalDate, DailyChange> dailyChanges = new TreeMap<>();
        for (Transaction transaction : transactions) {
            if (transaction.getTransactionTime() == null) {
                continue;
            }
            LocalDate transactionDate = transaction.getTransactionTime().toLocalDate();
            // 全部流水计入当日变动，余额调整另记一份到修正列
            dailyChanges.computeIfAbsent(transactionDate, date -> new DailyChange())
                    .add(BalanceChanges.of(accountId, transaction, credit),
                            ADJUSTMENT.equals(transaction.getTransactionType()));
        }

        BigDecimal opening = openingBalance;
        List<AccountDailyBalance> balances = new ArrayList<>();
        for (Map.Entry<LocalDate, DailyChange> entry : dailyChanges.entrySet()) {
            DailyChange dailyChange = entry.getValue();
            balances.add(buildDailyBalance(accountId, entry.getKey(), opening,
                    dailyChange.transactionChange, dailyChange.correctionChange));
            opening = opening.add(dailyChange.transactionChange);
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
     * 构造一个活动日的余额快照。
     *
     * @param transactionChange 当日全部流水的余额变动，期末余额只由它推出
     * @param correctionChange 其中余额调整的部分，是 transactionChange 的子集
     */
    private AccountDailyBalance buildDailyBalance(Long accountId, LocalDate statDate, BigDecimal openingBalance,
                                                   BigDecimal transactionChange, BigDecimal correctionChange) {
        AccountDailyBalance dailyBalance = new AccountDailyBalance();
        dailyBalance.setAccountId(accountId);
        dailyBalance.setStatDate(statDate);
        dailyBalance.setOpeningBalance(openingBalance);
        dailyBalance.setTransactionChange(transactionChange);
        dailyBalance.setCorrectionChange(correctionChange);
        dailyBalance.setClosingBalance(openingBalance.add(transactionChange));
        dailyBalance.setRebuiltTime(LocalDateTime.now());
        return dailyBalance;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isCreditAccount(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(404, "账户不存在");
        }
        return isCreditType(resolveTypeCode(account.getTypeId()));
    }

    private String resolveTypeCode(Long typeId) {
        AccountType accountType = typeId == null ? null : accountTypeMapper.selectById(typeId);
        if (accountType == null) {
            throw new BizException(404, "账户类型不存在");
        }
        return accountType.getTypeCode();
    }

    /** 一个活动日内的余额变化：全部流水的合计，以及其中余额调整的部分。 */
    private static final class DailyChange {

        private BigDecimal transactionChange = BigDecimal.ZERO;

        private BigDecimal correctionChange = BigDecimal.ZERO;

        private void add(BigDecimal change, boolean correction) {
            transactionChange = transactionChange.add(change);
            if (correction) {
                correctionChange = correctionChange.add(change);
            }
        }
    }
}

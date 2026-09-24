package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.costcount.common.BalanceChanges;
import com.costcount.common.PageQuery;
import com.costcount.dto.transaction.InitialTransactionSaveDTO;
import com.costcount.dto.transaction.TransactionQueryDTO;
import com.costcount.dto.transaction.TransactionSaveDTO;
import com.costcount.entity.*;
import com.costcount.exception.BizException;
import com.costcount.mapper.*;
import com.costcount.service.AccountDailyBalanceService;
import com.costcount.service.TransactionService;
import com.costcount.vo.transaction.TransactionVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.costcount.common.CommonConstant.isCreditType;
import static com.costcount.common.TransactionConstant.*;

/**
 * 流水记账服务。
 *
 * <p>流水是账户余额的事实来源。录入时按账户分别判断走哪条路径，对调用方透明：</p>
 * <ul>
 *     <li>追加：发生时间不早于账户最近一笔流水，余额和发生日快照直接累加，不触碰历史快照；</li>
 *     <li>补充：发生时间落在历史区间，从发生日起重放流水，修正其后每笔流水的期末余额、账户余额并重建快照。</li>
 * </ul>
 */
@Slf4j
@Validated
@Service
public class TransactionServiceImpl
        extends MPJBaseServiceImpl<TransactionMapper, Transaction>
        implements TransactionService {

    @Resource
    private TransactionMapper transactionMapper;

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private AccountTypeMapper accountTypeMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private AccountDailyBalanceService accountDailyBalanceService;

    @Override
    public Page<TransactionVO> pageQueryTransactions(PageQuery<TransactionQueryDTO> pageQuery) {

        TransactionQueryDTO params = Optional.ofNullable(pageQuery.getParams()).orElseGet(TransactionQueryDTO::new);

        MPJLambdaWrapper<Transaction> wrapper = new MPJLambdaWrapper<>();
        wrapper.select(
                Transaction::getId,
                Transaction::getTransactionType,
                Transaction::getCategoryId,
                Transaction::getAccountId,
                Transaction::getTargetAccountId,
                Transaction::getAmount,
                Transaction::getBalanceChange,
                Transaction::getBalanceAfter,
                Transaction::getTransactionTime,
                Transaction::getCounterparty,
                Transaction::getSource,
                Transaction::getRequestId,
                Transaction::getRemark
                )
                .selectAs("account", Account::getAccName, TransactionVO::getAccountName)
                .selectAs("targetAccount", Account::getAccName, TransactionVO::getTargetAccountName)
                .leftJoin(Account.class, "account", Account::getId, Transaction::getAccountId)
                .leftJoin(Account.class, "targetAccount", Account::getId, Transaction::getTargetAccountId)
                // 条件要加在 and 本身上：只把条件放在里面的 eq 上，不传账户时会留下一个空的 "AND ()"
                .and(params.getAccountId() != null, w -> w
                        .eq(Transaction::getAccountId, params.getAccountId())
                        .or()
                        .eq(Transaction::getTargetAccountId, params.getAccountId()))
                .eq(StringUtils.isNotBlank(params.getTransactionType()), Transaction::getTransactionType,
                        params.getTransactionType())
                .ge(params.getStartTime() != null, Transaction::getTransactionTime, params.getStartTime())
                .le(params.getEndTime() != null, Transaction::getTransactionTime, params.getEndTime())
                .orderByDesc(Transaction::getTransactionTime)
                .orderByDesc(Transaction::getId);

        Page<TransactionVO> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        page = transactionMapper.selectJoinPage(page, TransactionVO.class, wrapper);

        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveInitialTransaction(InitialTransactionSaveDTO initialTransactionSaveDTO) {

        Transaction transaction = new Transaction();
        transaction.setTransactionType(INITIAL);
        transaction.setAccountId(initialTransactionSaveDTO.getAccountId());
        transaction.setAmount(initialTransactionSaveDTO.getAmount());
        transaction.setTransactionTime(initialTransactionSaveDTO.getTransactionTime());
        transaction.setCategoryId(null);
        transaction.setBalanceChange(initialTransactionSaveDTO.getAmount());
        transaction.setBalanceAfter(initialTransactionSaveDTO.getAmount());
        transaction.setCounterparty(null);
        transaction.setSource(SOURCE_SYSTEM);
        transaction.setRequestId(null);

        save(transaction);

        // 初始流水是新账户的第一笔流水，同样只需追加开立日快照
        accountDailyBalanceService.appendDailyBalance(initialTransactionSaveDTO.getAccountId(),
                initialTransactionSaveDTO.getTransactionTime().toLocalDate(),
                initialTransactionSaveDTO.getAmount(), false, initialTransactionSaveDTO.getAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveTransaction(TransactionSaveDTO transactionSaveDTO) {
        return recordTransaction(transactionSaveDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveBalanceAdjustment(Long accountId, BigDecimal targetBalance) {

        Account account = accountMapper.selectByIdsForUpdate(List.of(accountId)).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("账户不存在"));

        BigDecimal balanceChange = targetBalance.subtract(account.getBalance());
        if (balanceChange.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        // 调整记在当前时间，而流水的发生时间都不晚于当前，这笔调整必然走追加
        TransactionSaveDTO transactionSaveDTO = new TransactionSaveDTO();
        transactionSaveDTO.setTransactionType(ADJUSTMENT);
        transactionSaveDTO.setAccountId(accountId);
        transactionSaveDTO.setBalanceChange(balanceChange);
        transactionSaveDTO.setTransactionTime(LocalDateTime.now());
        transactionSaveDTO.setRemark("账户余额修正");
        return recordTransaction(transactionSaveDTO);
    }

    /** 记账流程：校验、加锁、落库，再按账户分别以追加或重放的方式联动余额和快照。 */
    private String recordTransaction(TransactionSaveDTO transactionSaveDTO) {

        String transactionType = transactionSaveDTO.getTransactionType();
        if (transactionType == null) {
            throw new IllegalArgumentException("流水类型不能为空");
        }

        LocalDateTime transactionTime = transactionSaveDTO.getTransactionTime();
        if (transactionTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("发生时间不能晚于当前时间");
        }

        // 幂等号按空白即未传处理：空串在 uk_request_id 上是一个真实值，
        // 原样落库会让后续同样传空串的请求都被判成重试而被丢弃
        String requestId = StringUtils.trimToNull(transactionSaveDTO.getRequestId());

        BigDecimal amount = transactionSaveDTO.getAmount();
        boolean hasCategory = false;
        switch (transactionType) {
            case INCOME, EXPENSE -> {

                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("交易金额必须大于 0");
                }

                if (transactionSaveDTO.getCategoryId() == null) {
                    throw new IllegalArgumentException("收入或支出必须指定分类");
                }
                hasCategory = true;
            }
            case TRANSFER -> {

                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("交易金额必须大于 0");
                }

                if (transactionSaveDTO.getTargetAccountId() == null) {
                    throw new IllegalArgumentException("转账必须指定目标账户");
                }

                if (transactionSaveDTO.getTargetAccountId().equals(transactionSaveDTO.getAccountId())) {
                    throw new IllegalArgumentException("转入账户不能与转出账户相同");
                }
            }
            case ADJUSTMENT -> {
                if (transactionSaveDTO.getBalanceChange() == null
                        || transactionSaveDTO.getBalanceChange().compareTo(BigDecimal.ZERO) == 0) {
                    throw new IllegalArgumentException("余额调整值不能为 0");
                }
                amount = transactionSaveDTO.getBalanceChange().abs();
            }
            default -> throw new IllegalArgumentException("不支持的流水类型：" + transactionType);
        }

        // 主账户和目标账户按 ID 升序加行锁后再读取余额
        Set<Long> lockIds = new TreeSet<>();
        lockIds.add(transactionSaveDTO.getAccountId());

        // 如果是转账，同时锁定目标账户
        if (TRANSFER.equals(transactionType)) {
            lockIds.add(transactionSaveDTO.getTargetAccountId());
        }

        Map<Long, Account> lockedAccounts = accountMapper.selectByIdsForUpdate(lockIds).stream()
                .collect(Collectors.toMap(Account::getId, account -> account));

        Account account = lockedAccounts.get(transactionSaveDTO.getAccountId());
        if (account == null) {
            throw new IllegalArgumentException("账户不存在");
        }
        Account targetAccount = null;

        // 转账要先验证目标账户存在
        if (TRANSFER.equals(transactionType)) {
            targetAccount = lockedAccounts.get(transactionSaveDTO.getTargetAccountId());
            if (targetAccount == null) {
                throw new IllegalArgumentException("目标账户不存在");
            }
        }

        // 普通查询必须在账户锁之后执行，避免 REPEATABLE READ 固定住等待锁之前的快照。
        if (requestId != null) {
            Transaction existed = transactionMapper.selectOne(new LambdaQueryWrapper<Transaction>()
                    .eq(Transaction::getRequestId, requestId));
            if (existed != null) {
                return String.valueOf(existed.getId());
            }
        }
        if (hasCategory) {
            Category category = categoryMapper.selectById(transactionSaveDTO.getCategoryId());
            if (category == null) {
                throw new IllegalArgumentException("分类不存在");
            }
            if (!Objects.equals(category.getCategoryType(), transactionType)) {
                throw new IllegalArgumentException("分类类型与流水类型不一致");
            }
        }

        // 必须在落库前判断：落库后这笔流水自己就成了最近一笔。
        // 转账两侧分别判断，同一笔转账可能对一侧是历史、对另一侧是最新
        boolean accountBackfill = isBackfill(account, transactionTime);
        boolean targetBackfill = targetAccount != null && isBackfill(targetAccount, transactionTime);

        Set<Long> typeIds = new HashSet<>();
        typeIds.add(account.getTypeId());

        if (targetAccount != null) {
            typeIds.add(targetAccount.getTypeId());
        }

        // type_id 允许为空，带着 NULL 进 IN 列表会变成 500，这里提前拦成 400
        if (typeIds.contains(null)) {
            throw new IllegalArgumentException("账户类型不存在");
        }

        Map<Long, String> typeCodeMap = accountTypeMapper.selectByIds(typeIds).stream()
                .collect(Collectors.toMap(AccountType::getId, AccountType::getTypeCode));
        if (!typeCodeMap.keySet().containsAll(typeIds)) {
            throw new IllegalArgumentException("账户类型不存在");
        }

        boolean accountIsCredit = isCreditType(typeCodeMap.get(account.getTypeId()));
        boolean targetIsCredit = targetAccount != null
                && isCreditType(typeCodeMap.get(targetAccount.getTypeId()));

        // 资金方向：流入记 +1，流出记 -1，余额调整不涉及资金方向
        int flowDirection = switch (transactionType) {
            case INCOME -> 1;
            case EXPENSE, TRANSFER -> -1;
            case ADJUSTMENT -> 0;
            default -> throw new IllegalArgumentException("不支持的流水类型：" + transactionType);
        };

        BigDecimal balanceChange;
        if (flowDirection == 0) {
            // 余额调整直接采用校正值
            balanceChange = transactionSaveDTO.getBalanceChange();
        } else {
            // 资产账户余额是可用资金，随资金同向增减；信用账户余额是待还金额，方向相反
            balanceChange = amount.multiply(BigDecimal.valueOf(accountIsCredit ? -flowDirection : flowDirection));
        }
        BigDecimal balanceAfter = account.getBalance().add(balanceChange);

        BigDecimal targetBalanceChange = null;
        if (targetAccount != null) {
            // 转入账户收到资金流入：资产余额增加，信用待还减少
            targetBalanceChange = amount.multiply(BigDecimal.valueOf(targetIsCredit ? -1 : 1));
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionType(transactionType);
        transaction.setAccountId(account.getId());
        transaction.setTargetAccountId(targetAccount == null ? null : targetAccount.getId());
        transaction.setCategoryId(hasCategory ? transactionSaveDTO.getCategoryId() : null);
        transaction.setAmount(amount);
        transaction.setBalanceChange(balanceChange);
        // 追加时这就是它的期末余额；补充时先按此暂填，随后由重放修正
        transaction.setBalanceAfter(balanceAfter);
        transaction.setTransactionTime(transactionTime);
        transaction.setCounterparty(transactionSaveDTO.getCounterparty());
        transaction.setSource(SOURCE_MANUAL);
        transaction.setRequestId(requestId);
        transaction.setRemark(transactionSaveDTO.getRemark());

        try {
            save(transaction);
        } catch (DuplicateKeyException e) {
            // 并发重复请求由 uk_request_id 兜底，返回先落库的流水，不再重复调整余额。
            // 普通查询受 REPEATABLE READ 快照限制，可能看不到并发事务刚提交的流水，
            // 因此用 FOR UPDATE 当前读按 request_id 取对方已落库的流水
            if (requestId != null) {
                Transaction existed = lambdaQuery()
                        .eq(Transaction::getRequestId, requestId)
                        .last("FOR UPDATE")
                        .one();
                if (existed != null) {
                    log.warn("流水插入命中 request_id 唯一冲突，返回已落库流水，requestId={}，已有流水ID={}",
                            requestId, existed.getId());
                    return String.valueOf(existed.getId());
                }
            }
            log.error("流水插入命中唯一键冲突，requestId={}", requestId, e);
            throw new BizException(409, "请求重复，请勿重复提交");
        }

        LocalDate statDate = transactionTime.toLocalDate();
        settle(account, accountBackfill, statDate, balanceChange, ADJUSTMENT.equals(transactionType), accountIsCredit);
        if (targetAccount != null) {
            settle(targetAccount, targetBackfill, statDate, targetBalanceChange, false, targetIsCredit);
        }

        return String.valueOf(transaction.getId());
    }

    /** 发生时间早于账户最近一笔流水，说明这笔流水插在了历史区间里。 */
    private boolean isBackfill(Account account, LocalDateTime transactionTime) {
        LocalDateTime latest = transactionMapper.selectLatestTransactionTime(account.getId());
        return latest != null && transactionTime.isBefore(latest);
    }

    /**
     * 流水落库后联动单个账户的余额和快照。
     *
     * <p>最新的流水只影响发生日，直接累加；历史区间的流水改变了其后每一笔流水的余额，
     * 从发生日起按流水事实重放。</p>
     */
    private void settle(Account account, boolean backfill, LocalDate statDate, BigDecimal change,
                        boolean correction, boolean credit) {
        if (backfill) {
            log.info("账户 {} 录入了 {} 的历史流水，从该日起重放流水并重建快照", account.getId(), statDate);
            replayAccountFrom(account, statDate, credit);
        } else {
            appendToAccount(account, statDate, change, correction);
        }
    }

    /**
     * 把末尾流水的变化累加到账户余额和发生日快照。
     *
     * <p>传入的必须是完整账户行，不能用只带主键的对象替代，
     * 否则更新策略为 ALWAYS 的字段会被一并写成 NULL。</p>
     */
    private void appendToAccount(Account account, LocalDate statDate, BigDecimal change, boolean correction) {
        BigDecimal balance = account.getBalance().add(change);
        account.setBalance(balance);
        accountMapper.updateById(account);
        accountDailyBalanceService.appendDailyBalance(account.getId(), statDate, change, correction, balance);
    }

    /**
     * 从业务发生日重放账户流水，修正每笔流水的期末余额、账户余额和活动日快照。
     *
     * <p>调用前账户必须已加行锁。传入的必须是完整账户行，不能用只带主键的对象替代，
     * 否则更新策略为 ALWAYS 的字段会被一并写成 NULL。</p>
     */
    private void replayAccountFrom(Account account, LocalDate fromDate, boolean credit) {

        Long accountId = account.getId();
        BigDecimal openingBalance = accountDailyBalanceService.getClosingBalanceBefore(accountId, fromDate, credit);
        List<Transaction> transactions = transactionMapper.selectAccountTransactionsFrom(accountId, fromDate);

        BigDecimal balance = openingBalance;
        List<Transaction> corrections = new ArrayList<>();
        for (Transaction item : transactions) {
            balance = balance.add(BalanceChanges.of(accountId, item, credit));
            // 期末余额只记在主账户一侧，转入方的余额由它自己的流水体现
            if (!accountId.equals(item.getAccountId())
                    || (item.getBalanceAfter() != null && balance.compareTo(item.getBalanceAfter()) == 0)) {
                continue;
            }
            Transaction correction = new Transaction();
            correction.setId(item.getId());
            correction.setBalanceAfter(balance);
            corrections.add(correction);
        }
        if (!corrections.isEmpty()) {
            transactionMapper.updateById(corrections);
        }

        account.setBalance(balance);
        accountMapper.updateById(account);

        // 复用已经取好的流水重建快照，省掉重复的账户类型、期初锚点和流水查询
        accountDailyBalanceService.rebuildDailyBalances(accountId, fromDate, credit, openingBalance, transactions);
    }
}

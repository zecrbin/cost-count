package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.costcount.common.CommonConstant;
import com.costcount.dto.TransactionSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.AccountType;
import com.costcount.entity.Category;
import com.costcount.entity.Transaction;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.mapper.CategoryMapper;
import com.costcount.mapper.TransactionMapper;
import com.costcount.service.TransactionService;
import com.costcount.service.AccountDailyBalanceService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.costcount.common.CommonConstant.CREDIT;
import static com.costcount.common.CommonConstant.DEBIT;

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
    @Transactional
    /* 创建并入账一笔普通交易。 */
    public synchronized String createTransaction(TransactionSaveDTO dto) {
        // synchronized 用于单实例线程池场景；事务和 FOR UPDATE 仍负责数据库层一致性。
        validateRequest(dto);
        String transactionType = dto.getTransactionType().toUpperCase();
        // 转账必须按固定 ID 顺序锁定两个账户，避免并发请求产生死锁。
        List<Long> accountIds = new ArrayList<>();
        accountIds.add(dto.getAccountId());
        if (dto.getTargetAccountId() != null) {
            accountIds.add(dto.getTargetAccountId());
        }
        List<Account> accounts = lockAccounts(accountIds);
        Account account = findAccount(accounts, dto.getAccountId());
        Account targetAccount = dto.getTargetAccountId() == null
                ? null : findAccount(accounts, dto.getTargetAccountId());
        validateCategory(dto, transactionType);

        BigDecimal balanceChange = calculateBalanceChange(dto, account);
        BigDecimal targetBalanceChange = targetAccount == null
                ? null : calculateTargetBalanceChange(dto.getAmount(), targetAccount);

        Transaction transaction = new Transaction();
        transaction.setTransactionType(transactionType);
        transaction.setCategoryId(dto.getCategoryId());
        transaction.setAccountId(account.getId());
        transaction.setTargetAccountId(targetAccount == null ? null : targetAccount.getId());
        transaction.setAmount(dto.getAmount());
        transaction.setBalanceChange(balanceChange);
        transaction.setBalanceAfter(account.getBalance().add(balanceChange));
        transaction.setTargetBalanceChange(targetBalanceChange);
        transaction.setTargetBalanceAfter(targetAccount == null
                ? null : targetAccount.getBalance().add(targetBalanceChange));
        transaction.setTransactionTime(dto.getTransactionTime() == null
                ? LocalDateTime.now() : dto.getTransactionTime());
        transaction.setPostedTime(LocalDateTime.now());
        transaction.setCounterparty(dto.getCounterparty());
        transaction.setSource(StringUtils.hasText(dto.getSource())
                ? dto.getSource().toUpperCase() : CommonConstant.SOURCE_MANUAL);
        transaction.setRequestId(dto.getRequestId());
        transaction.setAdjustmentReason(dto.getAdjustmentReason());
        transaction.setStatus(CommonConstant.TRANSACTION_POSTED);
        transaction.setRemarks(dto.getRemarks());
        Transaction existing = findByRequestId(transaction.getSource(), transaction.getRequestId());
        if (existing != null) {
            return existing.getId().toString();
        }
        transactionMapper.insert(transaction);

        // 重新计算而不是直接沿用旧快照，保证补录历史时间的流水也能修正后续快照。
        rebuildAccountLedger(account.getId());
        if (targetAccount != null) {
            rebuildAccountLedger(targetAccount.getId());
        }
        accountDailyBalanceService.rebuildFrom(account.getId(), transaction.getTransactionTime().toLocalDate());
        if (targetAccount != null) {
            accountDailyBalanceService.rebuildFrom(targetAccount.getId(), transaction.getTransactionTime().toLocalDate());
        }
        return transaction.getId().toString();
    }

    @Override
    @Transactional
    /* 为新账户创建唯一的 INITIAL 流水。 */
    public synchronized void createInitialTransaction(Long accountId, BigDecimal amount, String requestId) {
        // INITIAL 只能由账户创建流程调用，且通过 requestId 保证重试不会重复入账。
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("初始余额不能小于0");
        }
        Transaction existing = findByRequestId(CommonConstant.SOURCE_SYSTEM, requestId);
        if (existing != null) {
            return;
        }
        Account account = lockAccount(accountId);
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("账户初始流水要求账户当前余额为0");
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionType(CommonConstant.TRANSACTION_INITIAL);
        transaction.setAccountId(accountId);
        transaction.setAmount(amount);
        transaction.setBalanceChange(amount);
        transaction.setBalanceAfter(amount);
        transaction.setTransactionTime(LocalDateTime.now());
        transaction.setPostedTime(LocalDateTime.now());
        transaction.setSource(CommonConstant.SOURCE_SYSTEM);
        transaction.setRequestId(requestId);
        transaction.setStatus(CommonConstant.TRANSACTION_POSTED);
        transactionMapper.insert(transaction);

        rebuildAccountLedger(accountId);
        accountDailyBalanceService.rebuildFrom(accountId, transaction.getTransactionTime().toLocalDate());
    }

    @Override
    @Transactional
    /* 新增反向流水冲正原流水，并重放相关账户的流水链。 */
    public synchronized String reverseTransaction(Long id) {
        Transaction transaction = transactionMapper.selectById(id);
        if (transaction == null) {
            throw new IllegalArgumentException("流水不存在");
        }
        if (!CommonConstant.TRANSACTION_POSTED.equals(transaction.getStatus())) {
            throw new IllegalArgumentException("只有已入账流水可以冲正");
        }
        if (CommonConstant.TRANSACTION_INITIAL.equals(transaction.getTransactionType())) {
            throw new IllegalArgumentException("账户初始流水不能冲正");
        }
        if (transaction.getReversalOfId() != null) {
            throw new IllegalArgumentException("冲正流水不能再次冲正");
        }
        Transaction existing = transactionMapper.selectOne(Wrappers.lambdaQuery(Transaction.class)
                .eq(Transaction::getReversalOfId, id));
        if (existing != null) {
            return existing.getId().toString();
        }

        List<Long> accountIds = new ArrayList<>();
        accountIds.add(transaction.getAccountId());
        if (transaction.getTargetAccountId() != null) {
            accountIds.add(transaction.getTargetAccountId());
        }
        List<Account> accounts = lockAccounts(accountIds);

        Transaction reversal = new Transaction();
        reversal.setTransactionType(CommonConstant.TRANSACTION_ADJUSTMENT);
        reversal.setAccountId(transaction.getAccountId());
        reversal.setTargetAccountId(transaction.getTargetAccountId());
        reversal.setAmount(transaction.getAmount());
        reversal.setBalanceChange(transaction.getBalanceChange().negate());
        reversal.setTargetBalanceChange(transaction.getTargetBalanceChange() == null
                ? null : transaction.getTargetBalanceChange().negate());
        Account account = findAccount(accounts, transaction.getAccountId());
        reversal.setBalanceAfter(account.getBalance().add(reversal.getBalanceChange()));
        if (transaction.getTargetAccountId() != null) {
            Account target = findAccount(accounts, transaction.getTargetAccountId());
            reversal.setTargetBalanceAfter(target.getBalance().add(reversal.getTargetBalanceChange()));
        }
        reversal.setTransactionTime(LocalDateTime.now());
        reversal.setPostedTime(LocalDateTime.now());
        reversal.setSource(CommonConstant.SOURCE_SYSTEM);
        reversal.setRequestId("REVERSAL_" + id);
        reversal.setAdjustmentReason("冲正流水 " + id);
        reversal.setReversalOfId(id);
        reversal.setStatus(CommonConstant.TRANSACTION_POSTED);
        reversal.setRemarks("系统冲正原流水：" + id);
        transactionMapper.insert(reversal);

        transaction.setStatus(CommonConstant.TRANSACTION_REVERSED);
        transactionMapper.updateById(transaction);
        rebuildAccountLedger(account.getId());
        accountDailyBalanceService.rebuildFrom(account.getId(), transaction.getTransactionTime().toLocalDate());
        if (transaction.getTargetAccountId() != null) {
            Account target = findAccount(accounts, transaction.getTargetAccountId());
            rebuildAccountLedger(target.getId());
            accountDailyBalanceService.rebuildFrom(
                    transaction.getTargetAccountId(), transaction.getTransactionTime().toLocalDate());
        }
        return reversal.getId().toString();
    }

    @Override
    /* 只检查未被逻辑删除的有效流水。 */
    public boolean existsForAccount(Long accountId) {
        return transactionMapper.selectCount(Wrappers.lambdaQuery(Transaction.class)
                .eq(Transaction::getAccountId, accountId)
                .or(wrapper -> wrapper.eq(Transaction::getTargetAccountId, accountId))) > 0;
    }

    /** 校验不同流水类型之间互斥的字段组合。 */
    private void validateRequest(TransactionSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getTransactionType())) {
            throw new IllegalArgumentException("流水类型不能为空");
        }
        if (dto.getAccountId() == null || dto.getAmount() == null
                || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("账户和金额不能为空，金额必须大于0");
        }
        String type = dto.getTransactionType().toUpperCase();
        if (CommonConstant.TRANSACTION_INITIAL.equals(type)) {
            throw new IllegalArgumentException("初始流水只能在创建账户时生成");
        }
        if (CommonConstant.TRANSACTION_TRANSFER.equals(type)) {
            if (dto.getTargetAccountId() == null || dto.getAccountId().equals(dto.getTargetAccountId())) {
                throw new IllegalArgumentException("转账目标账户不能为空且不能与主账户相同");
            }
            if (dto.getCategoryId() != null) {
                throw new IllegalArgumentException("转账不能选择分类");
            }
        } else if (dto.getTargetAccountId() != null) {
            throw new IllegalArgumentException("非转账流水不能填写目标账户");
        }
        if (CommonConstant.TRANSACTION_ADJUSTMENT.equals(type)
                && (dto.getBalanceChange() == null
                || dto.getBalanceChange().compareTo(BigDecimal.ZERO) == 0)) {
            throw new IllegalArgumentException("余额调整必须填写非零的余额变化值");
        }
        if (!CommonConstant.TRANSACTION_ADJUSTMENT.equals(type)
                && dto.getBalanceChange() != null) {
            throw new IllegalArgumentException("只有余额调整可以手工填写余额变化值");
        }
    }

    /** 校验收入/支出分类存在且与流水类型一致。 */
    private void validateCategory(TransactionSaveDTO dto, String transactionType) {
        if (CommonConstant.TRANSACTION_TRANSFER.equals(transactionType)
                || CommonConstant.TRANSACTION_ADJUSTMENT.equals(transactionType)) {
            if (dto.getCategoryId() != null) {
                throw new IllegalArgumentException("该类型流水不能选择分类");
            }
            return;
        }
        if (dto.getCategoryId() == null) {
            throw new IllegalArgumentException("收入或支出流水必须选择分类");
        }
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null || !transactionType.equalsIgnoreCase(category.getCategoryType())) {
            throw new IllegalArgumentException("流水分类不存在或类型不匹配");
        }
    }

    /** 按固定顺序加行锁，保证转账涉及的两个账户不会互相等待。 */
    private List<Account> lockAccounts(List<Long> ids) {
        ids.sort(Comparator.naturalOrder());
        return accountMapper.selectList(Wrappers.lambdaQuery(Account.class)
                .in(Account::getId, ids)
                .eq(Account::getStatus, CommonConstant.NORMAL_STATUS)
                .orderByAsc(Account::getId)
                .last("FOR UPDATE"));
    }

    private void rebuildAccountLedger(Long accountId) {
        // 账户余额和 balance_after 都是流水派生值，因此历史变化后统一按业务时间重放。
        List<Transaction> transactions = transactionMapper.selectList(Wrappers.lambdaQuery(Transaction.class)
                .and(wrapper -> wrapper.eq(Transaction::getAccountId, accountId)
                        .or().eq(Transaction::getTargetAccountId, accountId))
                .in(Transaction::getStatus,
                        CommonConstant.TRANSACTION_POSTED, CommonConstant.TRANSACTION_REVERSED)
                .orderByAsc(Transaction::getTransactionTime)
                .orderByAsc(Transaction::getId));
        BigDecimal running = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            boolean mainAccount = accountId.equals(transaction.getAccountId());
            BigDecimal change = mainAccount
                    ? transaction.getBalanceChange() : transaction.getTargetBalanceChange();
            if (change == null) {
                continue;
            }
            running = running.add(change);
            if (mainAccount) {
                if (transaction.getBalanceAfter() == null
                        || transaction.getBalanceAfter().compareTo(running) != 0) {
                    Transaction patch = new Transaction();
                    patch.setId(transaction.getId());
                    patch.setBalanceAfter(running);
                    transactionMapper.updateById(patch);
                }
            } else if (transaction.getTargetBalanceAfter() == null
                    || transaction.getTargetBalanceAfter().compareTo(running) != 0) {
                Transaction patch = new Transaction();
                patch.setId(transaction.getId());
                patch.setTargetBalanceAfter(running);
                transactionMapper.updateById(patch);
            }
        }
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("账户不存在");
        }
        account.setBalance(running);
        accountMapper.updateById(account);
    }

    private Account lockAccount(Long accountId) {
        List<Account> accounts = lockAccounts(List.of(accountId));
        return findAccount(accounts, accountId);
    }

    /** 查询有效幂等流水；空 requestId 表示调用方不要求幂等。 */
    private Transaction findByRequestId(String source, String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return null;
        }
        return transactionMapper.selectOne(Wrappers.lambdaQuery(Transaction.class)
                .eq(Transaction::getSource, source)
                .eq(Transaction::getRequestId, requestId));
    }

    private Account findAccount(List<Account> accounts, Long accountId) {
        for (Account account : accounts) {
            if (accountId.equals(account.getId())) {
                return account;
            }
        }
        throw new IllegalArgumentException("账户不存在或已停用");
    }

    /** 按账户性质计算主账户余额变化，信用账户余额表示待还金额。 */
    private BigDecimal calculateBalanceChange(TransactionSaveDTO dto, Account account) {
        String type = dto.getTransactionType().toUpperCase();
        if (CommonConstant.TRANSACTION_ADJUSTMENT.equals(type)) {
            return dto.getBalanceChange();
        }
        // CREDIT 的 balance 表示待还金额，收入/转入与 DEBIT 的正负方向相反。
        boolean debit = isDebit(account);
        return switch (type) {
            case CommonConstant.TRANSACTION_INCOME -> debit ? dto.getAmount() : dto.getAmount().negate();
            case CommonConstant.TRANSACTION_EXPENSE -> debit ? dto.getAmount().negate() : dto.getAmount();
            case CommonConstant.TRANSACTION_TRANSFER -> debit ? dto.getAmount().negate() : dto.getAmount();
            default -> throw new IllegalArgumentException("流水类型无效");
        };
    }

    /** 计算转账目标账户的余额变化。 */
    private BigDecimal calculateTargetBalanceChange(BigDecimal amount, Account targetAccount) {
        return isDebit(targetAccount) ? amount : amount.negate();
    }

    private boolean isDebit(Account account) {
        AccountType accountType = accountTypeMapper.selectById(account.getAccTypeId());
        if (accountType == null || !StringUtils.hasText(accountType.getTypeCode())) {
            throw new IllegalArgumentException("账户类型不存在或无效");
        }
        if (DEBIT.equalsIgnoreCase(accountType.getTypeCode())) {
            return true;
        }
        if (CREDIT.equalsIgnoreCase(accountType.getTypeCode())) {
            return false;
        }
        throw new IllegalArgumentException("账户类型编码无效");
    }
}

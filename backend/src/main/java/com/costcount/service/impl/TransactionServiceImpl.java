package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.costcount.common.AccountDictionaryWriteLock;
import com.costcount.common.LedgerBalance;
import com.costcount.common.PageQuery;
import com.costcount.dto.transaction.InitialTransactionSaveDTO;
import com.costcount.dto.transaction.TransactionQueryDTO;
import com.costcount.dto.transaction.TransactionSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.AccountDailyBalance;
import com.costcount.entity.AccountType;
import com.costcount.entity.Category;
import com.costcount.entity.Transaction;
import com.costcount.exception.BizException;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.mapper.CategoryMapper;
import com.costcount.mapper.TransactionMapper;
import com.costcount.service.AccountDailyBalanceService;
import com.costcount.service.TransactionService;
import com.costcount.vo.transaction.TransactionVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import static com.costcount.common.CommonConstant.CREDIT_ACCOUNT_TYPE;
import static com.costcount.common.CommonConstant.STATUS_DISABLED;
import static com.costcount.common.TransactionConstant.ADJUSTMENT;
import static com.costcount.common.TransactionConstant.EXPENSE;
import static com.costcount.common.TransactionConstant.INCOME;
import static com.costcount.common.TransactionConstant.INITIAL;
import static com.costcount.common.TransactionConstant.SOURCE_MANUAL;
import static com.costcount.common.TransactionConstant.SOURCE_SYSTEM;
import static com.costcount.common.TransactionConstant.TRANSFER;

/**
 * 流水记账服务。
 *
 * <p>所有写操作在账户写锁内完成：先校验并落库流水，再从受影响日期起顺序回放相关账户的流水，
 * 同步账户余额、流水余额快照和日余额缓存。</p>
 */
@Service
@Validated
public class TransactionServiceImpl
        extends MPJBaseServiceImpl<TransactionMapper, Transaction>
        implements TransactionService {

    /** 允许通过流水接口录入的类型；初始资金流水只能随账户创建或修正。 */
    private static final Set<String> MANUAL_TYPES = Set.of(INCOME, EXPENSE, TRANSFER, ADJUSTMENT);

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
        return baseMapper.selectJoinPage(pageQuery.toPage(), TransactionVO.class, buildPageQueryWrapper(params));
    }

    /** 构造流水分页查询条件，关联分类、主账户和转账目标账户名称。 */
    MPJLambdaWrapper<Transaction> buildPageQueryWrapper(TransactionQueryDTO params) {
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
                .selectAs(Category::getCategoryName, TransactionVO::getCategoryName)
                .selectAs("account", Account::getAccName, TransactionVO::getAccountName)
                .selectAs("targetAccount", Account::getAccName, TransactionVO::getTargetAccountName)
                .leftJoin(Category.class, Category::getId, Transaction::getCategoryId)
                .leftJoin(Account.class, "account", Account::getId, Transaction::getAccountId)
                .leftJoin(Account.class, "targetAccount", Account::getId, Transaction::getTargetAccountId)
                // 条件放在外层：内层条件全部不成立时 MyBatis-Plus 仍会拼出空括号，生成非法 SQL。
                .and(params.getAccountId() != null, w -> w
                        .eq(Transaction::getAccountId, params.getAccountId())
                        .or()
                        .eq(Transaction::getTargetAccountId, params.getAccountId()))
                .eq(params.getCategoryId() != null, Transaction::getCategoryId, params.getCategoryId())
                .eq(StringUtils.isNotBlank(params.getTransactionType()), Transaction::getTransactionType,
                        StringUtils.upperCase(StringUtils.trim(params.getTransactionType()), Locale.ROOT))
                .ge(params.getStartTime() != null, Transaction::getTransactionTime, params.getStartTime())
                .le(params.getEndTime() != null, Transaction::getTransactionTime, params.getEndTime())
                .orderByDesc(Transaction::getTransactionTime)
                .orderByDesc(Transaction::getId);
        return wrapper;
    }

    @Override
    public TransactionVO getTransaction(Long id) {
        MPJLambdaWrapper<Transaction> wrapper = buildPageQueryWrapper(new TransactionQueryDTO())
                .eq(Transaction::getId, id);
        TransactionVO transaction = baseMapper.selectJoinOne(TransactionVO.class, wrapper);
        if (transaction == null) {
            throw new BizException(404, "流水不存在");
        }
        return transaction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveInitialTransaction(InitialTransactionSaveDTO initialTransactionSaveDTO) {
        ReentrantLock lock = AccountDictionaryWriteLock.acquire();
        try {
            Long accountId = initialTransactionSaveDTO.getAccountId();
            requireAccount(accountId, "账户");
            if (findInitialTransaction(accountId) != null) {
                throw new BizException(409, "账户已存在初始资金流水");
            }

            Transaction transaction = new Transaction();
            transaction.setTransactionType(INITIAL);
            transaction.setAccountId(accountId);
            transaction.setAmount(initialTransactionSaveDTO.getAmount());
            transaction.setTransactionTime(initialTransactionSaveDTO.getTransactionTime());
            transaction.setBalanceChange(LedgerBalance.mainChange(INITIAL, initialTransactionSaveDTO.getAmount(), false));
            transaction.setSource(SOURCE_SYSTEM);
            transaction.setRemark(normalize(initialTransactionSaveDTO.getRemark()));
            save(transaction);

            recalculateBalances(Set.of(accountId), transaction.getTransactionTime().toLocalDate());
            return String.valueOf(transaction.getId());
        } finally {
            AccountDictionaryWriteLock.release(lock);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInitialBalance(Long accountId, BigDecimal initialBalance, String remark) {
        if (initialBalance == null || initialBalance.signum() < 0) {
            throw new BizException(400, "初始资金不能为空且不能小于0");
        }
        ReentrantLock lock = AccountDictionaryWriteLock.acquire();
        try {
            requireAccount(accountId, "账户");
            Transaction initial = findInitialTransaction(accountId);
            if (initial == null) {
                throw new BizException(404, "账户初始资金流水不存在");
            }
            initial.setAmount(initialBalance);
            initial.setBalanceChange(LedgerBalance.mainChange(INITIAL, initialBalance, false));
            if (StringUtils.isNotBlank(remark)) {
                initial.setRemark(remark.trim());
            }
            updateById(initial);
            recalculateBalances(Set.of(accountId), initial.getTransactionTime().toLocalDate());
        } finally {
            AccountDictionaryWriteLock.release(lock);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccountLedger(Long accountId) {
        ReentrantLock lock = AccountDictionaryWriteLock.acquire();
        try {
            boolean hasBusinessTransactions = lambdaQuery()
                    .and(w -> w.eq(Transaction::getAccountId, accountId)
                            .or()
                            .eq(Transaction::getTargetAccountId, accountId))
                    .ne(Transaction::getTransactionType, INITIAL)
                    .exists();
            if (hasBusinessTransactions) {
                throw new BizException(409, "账户已有交易流水，无法删除，可改为停用");
            }
            remove(new LambdaQueryWrapper<Transaction>()
                    .eq(Transaction::getAccountId, accountId)
                    .eq(Transaction::getTransactionType, INITIAL));
            accountDailyBalanceService.remove(new LambdaQueryWrapper<AccountDailyBalance>()
                    .eq(AccountDailyBalance::getAccountId, accountId));
        } finally {
            AccountDictionaryWriteLock.release(lock);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveTransaction(TransactionSaveDTO transactionSaveDTO) {
        ReentrantLock lock = AccountDictionaryWriteLock.acquire();
        try {
            String requestId = normalize(transactionSaveDTO.getRequestId());
            if (requestId != null) {
                Transaction existing = baseMapper.selectByRequestIdIncludingDeleted(requestId);
                if (existing != null) {
                    if (Integer.valueOf(1).equals(existing.getIsDeleted())) {
                        throw new BizException(409, "该幂等号对应的流水已被删除，请使用新的幂等号");
                    }
                    return String.valueOf(existing.getId());
                }
            }

            Transaction transaction = new Transaction();
            applySaveDTO(transaction, transactionSaveDTO);
            transaction.setSource(SOURCE_MANUAL);
            transaction.setRequestId(requestId);
            save(transaction);

            recalculateBalances(involvedAccountIds(transaction), transaction.getTransactionTime().toLocalDate());
            return String.valueOf(transaction.getId());
        } finally {
            AccountDictionaryWriteLock.release(lock);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateTransaction(TransactionSaveDTO transactionSaveDTO) {
        if (transactionSaveDTO.getId() == null) {
            throw new BizException(400, "流水 ID 不能为空");
        }
        ReentrantLock lock = AccountDictionaryWriteLock.acquire();
        try {
            Transaction transaction = requireEditableTransaction(transactionSaveDTO.getId());
            Set<Long> accountIds = involvedAccountIds(transaction);
            LocalDate fromDate = transaction.getTransactionTime().toLocalDate();

            applySaveDTO(transaction, transactionSaveDTO);
            updateTransactionRow(transaction);

            // 账户或日期变化时，原账户和新账户都要从较早的日期开始重算。
            accountIds.addAll(involvedAccountIds(transaction));
            LocalDate newDate = transaction.getTransactionTime().toLocalDate();
            recalculateBalances(accountIds, newDate.isBefore(fromDate) ? newDate : fromDate);
            return String.valueOf(transaction.getId());
        } finally {
            AccountDictionaryWriteLock.release(lock);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTransaction(Long id) {
        ReentrantLock lock = AccountDictionaryWriteLock.acquire();
        try {
            Transaction transaction = requireEditableTransaction(id);
            removeById(id);
            recalculateBalances(involvedAccountIds(transaction), transaction.getTransactionTime().toLocalDate());
        } finally {
            AccountDictionaryWriteLock.release(lock);
        }
    }

    /** 校验保存参数并写入流水字段，余额快照由回放计算。 */
    private void applySaveDTO(Transaction transaction, TransactionSaveDTO dto) {
        String type = StringUtils.upperCase(StringUtils.trim(dto.getTransactionType()), Locale.ROOT);
        if (!MANUAL_TYPES.contains(type)) {
            throw new BizException(400, "流水类型只能是 INCOME、EXPENSE、TRANSFER 或 ADJUSTMENT");
        }
        Account account = requireEnabledAccount(dto.getAccountId(), "主账户");
        ensureNotBeforeInitial(account, dto.getTransactionTime());

        Long targetAccountId = null;
        Long categoryId = null;
        BigDecimal amount;
        BigDecimal balanceChange;
        if (ADJUSTMENT.equals(type)) {
            if (dto.getBalanceChange() == null || dto.getBalanceChange().signum() == 0) {
                throw new BizException(400, "余额调整值不能为空或为0");
            }
            if (dto.getTargetAccountId() != null || dto.getCategoryId() != null) {
                throw new BizException(400, "余额调整不能指定目标账户或分类");
            }
            balanceChange = dto.getBalanceChange();
            amount = balanceChange.abs();
        } else {
            amount = dto.getAmount();
            if (amount == null) {
                throw new BizException(400, "业务金额不能为空");
            }
            if (TRANSFER.equals(type)) {
                targetAccountId = requireTransferTarget(dto, account);
                categoryId = dto.getCategoryId() == null ? null : requireCategory(dto.getCategoryId(), type);
            } else {
                if (dto.getTargetAccountId() != null) {
                    throw new BizException(400, "仅转账流水可以指定目标账户");
                }
                if (dto.getCategoryId() == null) {
                    throw new BizException(400, "收入和支出流水必须选择分类");
                }
                categoryId = requireCategory(dto.getCategoryId(), type);
            }
            balanceChange = LedgerBalance.mainChange(type, amount, isCreditAccount(account));
        }

        transaction.setTransactionType(type);
        transaction.setAccountId(account.getId());
        transaction.setTargetAccountId(targetAccountId);
        transaction.setCategoryId(categoryId);
        transaction.setAmount(amount);
        transaction.setBalanceChange(balanceChange);
        transaction.setTransactionTime(dto.getTransactionTime());
        transaction.setCounterparty(normalize(dto.getCounterparty()));
        transaction.setRemark(normalize(dto.getRemark()));
    }

    private Long requireTransferTarget(TransactionSaveDTO dto, Account account) {
        if (dto.getTargetAccountId() == null) {
            throw new BizException(400, "转账目标账户不能为空");
        }
        if (dto.getTargetAccountId().equals(account.getId())) {
            throw new BizException(400, "转出账户和转入账户不能相同");
        }
        Account target = requireEnabledAccount(dto.getTargetAccountId(), "转入账户");
        ensureNotBeforeInitial(target, dto.getTransactionTime());
        return target.getId();
    }

    private Long requireCategory(Long categoryId, String transactionType) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BizException(404, "分类不存在");
        }
        if (!transactionType.equals(category.getCategoryType())) {
            throw new BizException(400, "分类类型与流水类型不一致");
        }
        return category.getId();
    }

    private Transaction requireEditableTransaction(Long id) {
        Transaction transaction = getById(id);
        if (transaction == null) {
            throw new BizException(404, "流水不存在");
        }
        if (INITIAL.equals(transaction.getTransactionType())) {
            throw new BizException(400, "初始资金流水请通过账户初始资金修正接口调整");
        }
        return transaction;
    }

    private Account requireAccount(Long accountId, String label) {
        Account account = accountId == null ? null : accountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(404, label + "不存在");
        }
        return account;
    }

    private Account requireEnabledAccount(Long accountId, String label) {
        Account account = requireAccount(accountId, label);
        if (STATUS_DISABLED.equals(account.getStatus())) {
            throw new BizException(400, label + "已停用");
        }
        return account;
    }

    /** 流水不能早于账户初始资金时间，否则回放时会出现开户前的余额。 */
    private void ensureNotBeforeInitial(Account account, LocalDateTime transactionTime) {
        Transaction initial = findInitialTransaction(account.getId());
        if (initial != null && transactionTime.isBefore(initial.getTransactionTime())) {
            throw new BizException(400, "交易时间不能早于账户「" + account.getAccName() + "」的初始资金时间");
        }
    }

    private Transaction findInitialTransaction(Long accountId) {
        return lambdaQuery()
                .eq(Transaction::getAccountId, accountId)
                .eq(Transaction::getTransactionType, INITIAL)
                .last("LIMIT 1")
                .one();
    }

    private boolean isCreditAccount(Account account) {
        AccountType accountType = accountTypeMapper.selectById(account.getTypeId());
        if (accountType == null) {
            throw new BizException(404, "账户类型不存在");
        }
        return CREDIT_ACCOUNT_TYPE.equalsIgnoreCase(accountType.getTypeCode());
    }

    private Set<Long> involvedAccountIds(Transaction transaction) {
        // 按 ID 排序回放，保证多账户处理顺序稳定。
        Set<Long> accountIds = new TreeSet<>();
        accountIds.add(transaction.getAccountId());
        if (transaction.getTargetAccountId() != null) {
            accountIds.add(transaction.getTargetAccountId());
        }
        return accountIds;
    }

    /**
     * 从指定日期起按时间和 ID 顺序回放账户流水。
     *
     * <p>起始余额取自该日期之前的日余额快照；回放时修正主账户流水的 {@code balanceAfter}，
     * 最终余额写回账户，并重建该日期之后的日余额。</p>
     */
    private void recalculateBalances(Set<Long> accountIds, LocalDate fromDate) {
        for (Long accountId : accountIds) {
            Account account = requireAccount(accountId, "账户");
            boolean credit = isCreditAccount(account);
            BigDecimal balance = accountDailyBalanceService.getClosingBalanceBefore(accountId, fromDate);

            List<Transaction> transactions = lambdaQuery()
                    .and(w -> w.eq(Transaction::getAccountId, accountId)
                            .or()
                            .eq(Transaction::getTargetAccountId, accountId))
                    .ge(Transaction::getTransactionTime, fromDate.atStartOfDay())
                    .orderByAsc(Transaction::getTransactionTime)
                    .orderByAsc(Transaction::getId)
                    .list();
            List<Transaction> changedTransactions = new ArrayList<>();
            for (Transaction transaction : transactions) {
                if (accountId.equals(transaction.getAccountId())) {
                    balance = balance.add(transaction.getBalanceChange());
                    if (transaction.getBalanceAfter() == null || balance.compareTo(transaction.getBalanceAfter()) != 0) {
                        transaction.setBalanceAfter(balance);
                        changedTransactions.add(transaction);
                    }
                } else {
                    balance = balance.add(LedgerBalance.targetChange(transaction.getAmount(), credit));
                }
            }
            if (!changedTransactions.isEmpty()) {
                updateBatchById(changedTransactions);
            }

            account.setBalance(balance);
            accountMapper.updateById(account);
            accountDailyBalanceService.rebuildAccountDailyBalance(accountId, fromDate);
        }
    }

    /** 按主键更新流水；备注位于通用基类且默认忽略空值，需要单独设置以支持清空。 */
    private void updateTransactionRow(Transaction transaction) {
        String remark = transaction.getRemark();
        transaction.setRemark(null);
        update(transaction, new LambdaUpdateWrapper<Transaction>()
                .eq(Transaction::getId, transaction.getId())
                .set(Transaction::getRemark, remark));
        transaction.setRemark(remark);
    }

    private String normalize(String value) {
        return StringUtils.isNotBlank(value) ? value.trim() : null;
    }
}

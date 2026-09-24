package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.costcount.common.AccountDictionaryWriteLock;
import com.costcount.common.AfterCommit;
import com.costcount.dto.account.AccountInitialBalanceAdjustDTO;
import com.costcount.dto.account.AccountQueryDTO;
import com.costcount.dto.account.AccountSaveDTO;
import com.costcount.dto.transaction.InitialTransactionSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.AccountProvider;
import com.costcount.entity.AccountType;
import com.costcount.entity.Transaction;
import com.costcount.exception.BizException;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.AccountProviderMapper;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.service.AccountIconStorageService;
import com.costcount.service.AccountService;
import com.costcount.service.TransactionService;
import com.costcount.service.UploadedIconService;
import com.costcount.vo.account.AccountVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.costcount.common.CommonConstant.CREDIT_ACCOUNT_TYPE;
import static com.costcount.common.CommonConstant.DEFAULT_SORT;
import static com.costcount.common.CommonConstant.STATUS_ENABLED;
import static com.costcount.common.TransactionConstant.INITIAL;

/** 账户聚合服务，负责账户资料、初始流水及动态图标的协同维护。 */
@Service
public class AccountServiceImpl
        extends MPJBaseServiceImpl<AccountMapper, Account>
        implements AccountService {

    @Resource
    private AccountTypeMapper accountTypeMapper;

    @Resource
    private AccountProviderMapper accountProviderMapper;

    @Resource
    private TransactionService transactionService;

    @Resource
    private AccountIconStorageService accountIconStorageService;

    @Resource
    private UploadedIconService uploadedIconService;

    @Override
    public List<AccountVO> listAccounts(AccountQueryDTO query) {
        AccountQueryDTO param = query == null ? new AccountQueryDTO() : query;
        MPJLambdaWrapper<Account> wrapper = buildQueryWrapper()
                .eq(param.getTypeId() != null, Account::getTypeId, param.getTypeId())
                .like(StringUtils.hasText(param.getAccName()), Account::getAccName, normalize(param.getAccName()))
                .like(StringUtils.hasText(param.getAccTailNum()), Account::getAccTailNum,
                        normalize(param.getAccTailNum()))
                .eq(param.getStatus() != null, Account::getStatus, param.getStatus())
                .orderByAsc(Account::getSort)
                .orderByDesc(Account::getCreatedTime);
        List<AccountVO> accounts = selectJoinList(AccountVO.class, wrapper);
        fillInitialBalances(accounts);
        return accounts;
    }

    @Override
    public AccountVO getAccount(Long id) {
        AccountVO account = selectJoinOne(AccountVO.class, buildQueryWrapper().eq(Account::getId, id));
        if (account == null) {
            throw new BizException(404, "账户不存在");
        }
        fillInitialBalances(List.of(account));
        return account;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addAccount(AccountSaveDTO dto) {
        ReentrantLock lock = AccountDictionaryWriteLock.acquire();
        try {
            AccountType accountType = requireAccountType(dto.getTypeId());
            Account account = new Account();
            // 预先分配 ID，使生成的银行卡图标可以随账户一次写入。
            account.setId(IdWorker.getId());
            account.setBalance(BigDecimal.ZERO);
            applySaveDTO(account, dto, accountType);
            account.setIcon(resolveIcon(account, normalize(dto.getIcon()), accountType));
            save(account);

            InitialTransactionSaveDTO initial = new InitialTransactionSaveDTO();
            initial.setAccountId(account.getId());
            initial.setAmount(dto.getInitialBalance() == null ? BigDecimal.ZERO : dto.getInitialBalance());
            initial.setTransactionTime(dto.getInitialTransactionTime() == null
                    ? LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                    : dto.getInitialTransactionTime());
            transactionService.saveInitialTransaction(initial);
            return String.valueOf(account.getId());
        } finally {
            AccountDictionaryWriteLock.release(lock);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateAccount(AccountSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(400, "账户 ID 不能为空");
        }
        ReentrantLock lock = AccountDictionaryWriteLock.acquire();
        try {
            Account account = getById(dto.getId());
            if (account == null) {
                throw new BizException(404, "账户不存在");
            }
            AccountType accountType = requireAccountType(dto.getTypeId());
            if (!accountType.getId().equals(account.getTypeId())) {
                AccountType currentType = accountTypeMapper.selectById(account.getTypeId());
                // 类型编码决定余额方向，换成方向相反的类型会让已有流水的余额含义反转。
                if (currentType == null || !currentType.getTypeCode().equals(accountType.getTypeCode())) {
                    throw new BizException(409, "只能更换为余额方向相同（同为 DEBIT 或 CREDIT）的账户类型");
                }
            }
            String previousIcon = account.getIcon();
            applySaveDTO(account, dto, accountType);
            account.setIcon(resolveIcon(account, normalize(dto.getIcon()), accountType));
            updateById(account);
            if (!Objects.equals(previousIcon, account.getIcon())) {
                deleteOwnedIconAfterCommit(previousIcon);
            }
            return String.valueOf(account.getId());
        } finally {
            AccountDictionaryWriteLock.release(lock);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustInitialBalance(Long id, AccountInitialBalanceAdjustDTO dto) {
        transactionService.updateInitialBalance(id, dto.getInitialBalance(), dto.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccounts(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty() || accountIds.stream().anyMatch(Objects::isNull)) {
            throw new BizException(400, "账户 ID 不能为空");
        }
        ReentrantLock lock = AccountDictionaryWriteLock.acquire();
        try {
            List<Long> ids = accountIds.stream().distinct().toList();
            List<Account> accounts = listByIds(ids);
            if (accounts.size() != ids.size()) {
                throw new BizException(404, "账户不存在");
            }
            accounts.forEach(account -> transactionService.deleteAccountLedger(account.getId()));
            removeByIds(ids);
            accounts.forEach(account -> deleteOwnedIconAfterCommit(account.getIcon()));
        } finally {
            AccountDictionaryWriteLock.release(lock);
        }
    }

    private MPJLambdaWrapper<Account> buildQueryWrapper() {
        return new MPJLambdaWrapper<Account>()
                .select(
                        Account::getId,
                        Account::getTypeId,
                        Account::getAccName,
                        Account::getAccTailNum,
                        Account::getBalance,
                        Account::getCreditLimit,
                        Account::getIdealCreditLimit,
                        Account::getIcon,
                        Account::getSort,
                        Account::getStatus,
                        Account::getRemark
                )
                .select(AccountType::getTypeCode, AccountType::getTypeName)
                .select(AccountProvider::getProviderName)
                .selectAs(AccountProvider::getIcon, AccountVO::getProviderIcon)
                .leftJoin(AccountType.class, AccountType::getId, Account::getTypeId)
                .leftJoin(AccountProvider.class, AccountProvider::getId, AccountType::getProviderId);
    }

    /** 用初始资金流水补充账户的初始资金和开户时间。 */
    private void fillInitialBalances(List<AccountVO> accounts) {
        if (accounts.isEmpty()) {
            return;
        }
        Map<Long, Transaction> initials = transactionService.lambdaQuery()
                .in(Transaction::getAccountId, accounts.stream().map(AccountVO::getId).toList())
                .eq(Transaction::getTransactionType, INITIAL)
                .list()
                .stream()
                .collect(Collectors.toMap(Transaction::getAccountId, Function.identity(), (first, second) -> first));
        accounts.forEach(account -> {
            Transaction initial = initials.get(account.getId());
            if (initial != null) {
                account.setInitialBalance(initial.getAmount());
                account.setInitialTransactionTime(initial.getTransactionTime());
            }
        });
    }

    private void applySaveDTO(Account account, AccountSaveDTO dto, AccountType accountType) {
        boolean credit = CREDIT_ACCOUNT_TYPE.equals(accountType.getTypeCode());
        account.setTypeId(accountType.getId());
        account.setAccName(dto.getAccName().trim());
        account.setAccTailNum(normalize(dto.getAccTailNum()));
        // 信用额度只对信用账户有意义，存储账户统一清空，避免遗留脏数据。
        account.setCreditLimit(credit ? dto.getCreditLimit() : null);
        account.setIdealCreditLimit(credit ? dto.getIdealCreditLimit() : null);
        account.setSort(dto.getSort() == null ? DEFAULT_SORT : dto.getSort());
        account.setStatus(dto.getStatus() == null ? STATUS_ENABLED : dto.getStatus());
        account.setRemark(normalize(dto.getRemark()));
    }

    /**
     * 决定账户图标：显式指定的非生成图标优先；否则有尾号时按提供方和尾号生成银行卡图标，没有尾号时清空，
     * 由前端回退使用提供方图标。
     */
    private String resolveIcon(Account account, String requestedIcon, AccountType accountType) {
        if (requestedIcon != null && !accountIconStorageService.isGeneratedIcon(requestedIcon)) {
            return requestedIcon;
        }
        if (!StringUtils.hasText(account.getAccTailNum())) {
            return null;
        }
        AccountProvider provider = accountProviderMapper.selectById(accountType.getProviderId());
        String providerName = provider == null ? accountType.getTypeName() : provider.getProviderName();
        return accountIconStorageService.storeBankCardIcon(account.getId(), providerName, account.getAccTailNum());
    }

    /** 事务提交后再删除账户不再使用的生成图标或上传图标，避免回滚后引用的文件已被删除。 */
    private void deleteOwnedIconAfterCommit(String iconPath) {
        if (accountIconStorageService.isGeneratedIcon(iconPath)) {
            AfterCommit.run("删除账户图标 " + iconPath, () -> accountIconStorageService.deleteBankCardIcon(iconPath));
        } else {
            uploadedIconService.deleteReplacedIconAfterCommit(iconPath, null);
        }
    }

    private AccountType requireAccountType(Long typeId) {
        AccountType accountType = typeId == null ? null : accountTypeMapper.selectById(typeId);
        if (accountType == null) {
            throw new BizException(404, "账户类型不存在");
        }
        return accountType;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

package com.costcount.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.costcount.dto.AccountQueryDTO;
import com.costcount.dto.AccountSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.AccountProvider;
import com.costcount.entity.AccountType;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.AccountProviderMapper;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.service.AccountIconService;
import com.costcount.service.AccountService;
import com.costcount.vo.AccountVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.costcount.common.CommonConstant.CREDIT;
import static com.costcount.common.CommonConstant.DEBIT;
import static com.costcount.common.CommonConstant.DEFAULT_SORT;
import static com.costcount.common.CommonConstant.NORMAL_STATUS;

@Service
public class AccountServiceImpl
        extends MPJBaseServiceImpl<AccountMapper, Account>
        implements AccountService {

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private AccountTypeMapper accountTypeMapper;

    @Resource
    private AccountProviderMapper accountProviderMapper;

    @Resource
    private AccountIconService accountIconService;

    @Override
    public List<AccountVO> listAllAccount(AccountQueryDTO dto) {
        AccountQueryDTO query = Optional.ofNullable(dto).orElseGet(AccountQueryDTO::new);

        MPJLambdaWrapper<Account> wrapper = new MPJLambdaWrapper<Account>()
                .selectAsClass(Account.class, AccountVO.class)
                .selectAs(AccountProvider::getId, AccountVO::getProviderId)
                .select(AccountProvider::getProviderName)
                .select(AccountType::getTypeName, AccountType::getTypeCode)
                .leftJoin(AccountType.class, AccountType::getId, Account::getAccTypeId)
                .leftJoin(AccountProvider.class, AccountProvider::getId, AccountType::getAccProviderId)
                .eq(query.getProviderId() != null, AccountProvider::getId, query.getProviderId())
                .eq(StringUtils.hasText(query.getTypeCode()), AccountType::getTypeCode, query.getTypeCode())
                .eq(query.getStatus() != null, Account::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getKeyword()), Account::getAccName, query.getKeyword())
                .orderByAsc(Account::getSort)
                .orderByDesc(Account::getCreatedTime);

        List<AccountVO> accounts = accountMapper.selectJoinList(AccountVO.class, wrapper);
        fillCalculatedFields(accounts);
        return accounts;
    }

    @Override
    public AccountVO getAccount(Long id) {
        MPJLambdaWrapper<Account> wrapper = new MPJLambdaWrapper<Account>()
                .selectAsClass(Account.class, AccountVO.class)
                .selectAs(AccountProvider::getId, AccountVO::getProviderId)
                .select(AccountProvider::getProviderName)
                .select(AccountType::getTypeName, AccountType::getTypeCode)
                .leftJoin(AccountType.class, AccountType::getId, Account::getAccTypeId)
                .leftJoin(AccountProvider.class, AccountProvider::getId, AccountType::getAccProviderId)
                .eq(Account::getId, id);

        AccountVO account = accountMapper.selectJoinOne(AccountVO.class, wrapper);
        if (account != null) {
            fillCalculatedFields(List.of(account));
        }
        return account;
    }

    @Override
    public String createAccount(AccountSaveDTO dto) {
        AccountType accountType = accountTypeMapper.selectById(dto.getAccTypeId());
        if (accountType == null) {
            throw new IllegalArgumentException("账户类型不存在");
        }

        AccountProvider accountProvider = accountProviderMapper.selectById(accountType.getAccProviderId());
        if (accountProvider == null) {
            throw new IllegalArgumentException("账户提供方不存在");
        }

        Account account = new Account();
        account.setAccTypeId(accountType.getId());
        account.setAccTailNum(dto.getAccTailNum());
        account.setAccName(buildAccountName(accountProvider, accountType, dto.getAccTailNum()));

        if (dto.getBalance() == null || dto.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("账户金额不能为空且不能小于0");
        }
        if (DEBIT.equalsIgnoreCase(accountType.getTypeCode())) {
            fillDebitAccountBalance(account, dto);
        } else if (CREDIT.equalsIgnoreCase(accountType.getTypeCode())) {
            fillCreditAccountBalance(account, dto);
        } else {
            throw new IllegalArgumentException("账户类型编码无效");
        }

        account.setSort(dto.getSort() == null ? DEFAULT_SORT : dto.getSort());
        account.setStatus(dto.getStatus() == null ? NORMAL_STATUS : dto.getStatus());
        account.setRemarks(dto.getRemark());

        boolean dynamicIcon = StringUtils.hasText(account.getAccTailNum());
        try {
            account.setIcon(buildAccountIcon(accountType, accountProvider.getIcon(), account));
            accountMapper.insert(account);
        } catch (RuntimeException exception) {
            // 数据保存失败时清理本次生成的动态图标，避免遗留无归属文件。
            if (dynamicIcon) {
                accountIconService.deleteAccountIcon(account.getIcon());
            }
            throw exception;
        }
        return account.getId().toString();
    }

    private void fillDebitAccountBalance(Account account, AccountSaveDTO dto) {
        account.setBalance(dto.getBalance());
        account.setCreditLimit(null);
        account.setIdealCreditLimit(null);
    }

    private void fillCreditAccountBalance(Account account, AccountSaveDTO dto) {
        BigDecimal creditLimit = Optional.ofNullable(dto.getCreditLimit()).orElse(BigDecimal.ZERO);
        BigDecimal idealCreditLimit = Optional.ofNullable(dto.getIdealCreditLimit()).orElse(BigDecimal.ZERO);
        if (creditLimit.compareTo(BigDecimal.ZERO) < 0 || idealCreditLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("信用额度和理想信用额度不能小于0");
        }
        account.setBalance(dto.getBalance());
        account.setCreditLimit(creditLimit);
        account.setIdealCreditLimit(idealCreditLimit);
    }

    @Override
    public String updateAccount(AccountSaveDTO dto) {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("修改账户时账户ID不能为空");
        }

        Account account = accountMapper.selectById(dto.getId());
        if (account == null) {
            throw new IllegalArgumentException("账户不存在");
        }

        AccountType accountType = accountTypeMapper.selectById(account.getAccTypeId());
        if (accountType == null) {
            throw new IllegalArgumentException("账户类型不存在");
        }

        String previousIcon = account.getIcon();

        if (StringUtils.hasText(dto.getAccTailNum())
                && !dto.getAccTailNum().equals(account.getAccTailNum())) {
            Long count = accountMapper.selectCount(Wrappers.lambdaQuery(Account.class)
                    .eq(Account::getAccTailNum, dto.getAccTailNum())
                    .eq(Account::getAccTypeId, account.getAccTypeId())
                    .ne(Account::getId, account.getId()));
            if (count != null && count > 0) {
                throw new IllegalArgumentException("同一账户类型下已存在相同尾号的账户");
            }
        }

        if (dto.getBalance() != null && dto.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("账户金额不能小于0");
        }
        if (DEBIT.equalsIgnoreCase(accountType.getTypeCode())) {
            account.setBalance(dto.getBalance() == null ? account.getBalance() : dto.getBalance());
        } else if (CREDIT.equalsIgnoreCase(accountType.getTypeCode())) {
            updateCreditAccountBalance(account, dto);
        } else {
            throw new IllegalArgumentException("账户类型编码无效");
        }

        // 账户类型不允许在修改接口中变更；尾号变化时同步重建名称和动态图标。
        if (dto.getAccTailNum() != null && !dto.getAccTailNum().equals(account.getAccTailNum())) {
            AccountProvider accountProvider = accountProviderMapper.selectById(accountType.getAccProviderId());
            if (accountProvider == null) {
                throw new IllegalArgumentException("账户提供方不存在");
            }
            account.setAccTailNum(dto.getAccTailNum());
            account.setAccName(buildAccountName(accountProvider, accountType, dto.getAccTailNum()));
            account.setIcon(buildAccountIcon(accountType, accountProvider.getIcon(), account));
        }

        account.setSort(dto.getSort() == null ? account.getSort() : dto.getSort());
        account.setStatus(dto.getStatus() == null ? account.getStatus() : dto.getStatus());
        account.setRemarks(dto.getRemark());

        accountMapper.updateById(account);
        if (!Objects.equals(previousIcon, account.getIcon())) {
            accountIconService.deleteAccountIcon(previousIcon);
        }
        return account.getId().toString();
    }

    private void updateCreditAccountBalance(Account account, AccountSaveDTO dto) {
        if (dto.getCreditLimit() != null && dto.getCreditLimit().compareTo(BigDecimal.ZERO) < 0
                || dto.getIdealCreditLimit() != null
                && dto.getIdealCreditLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("信用额度和理想信用额度不能小于0");
        }
        account.setBalance(dto.getBalance() == null ? account.getBalance() : dto.getBalance());
        account.setCreditLimit(dto.getCreditLimit() == null ? account.getCreditLimit() : dto.getCreditLimit());
        account.setIdealCreditLimit(dto.getIdealCreditLimit() == null
                ? account.getIdealCreditLimit() : dto.getIdealCreditLimit());
    }

    @Override
    public void deleteAccount(Long id) {
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw new IllegalArgumentException("账户不存在");
        }

        accountMapper.deleteById(id);
        accountIconService.deleteAccountIcon(account.getIcon());
    }

    private String buildAccountName(AccountProvider provider, AccountType type, String tailNum) {
        String suffix = StringUtils.hasText(tailNum) ? " " + tailNum.trim() : "";
        return provider.getProviderName() + " " + type.getTypeName() + suffix;
    }

    private String buildAccountIcon(AccountType accountType, String providerIcon, Account account) {
        if (!StringUtils.hasText(account.getAccTailNum())) {
            return providerIcon;
        }
        return accountIconService.generateBankCardIcon(accountType.getTypeName(), providerIcon, account);
    }

    private void fillCalculatedFields(List<AccountVO> accounts) {
        // 可用额度属于实时派生值，不落库，避免余额或信用额度变化后产生冗余数据不一致。
        for (AccountVO account : accounts) {
            if (!CREDIT.equalsIgnoreCase(account.getTypeCode())) {
                account.setAvailableCredit(null);
                continue;
            }
            BigDecimal creditLimit = Optional.ofNullable(account.getCreditLimit()).orElse(BigDecimal.ZERO);
            BigDecimal balance = Optional.ofNullable(account.getBalance()).orElse(BigDecimal.ZERO);
            account.setAvailableCredit(creditLimit.subtract(balance));
        }
    }
}

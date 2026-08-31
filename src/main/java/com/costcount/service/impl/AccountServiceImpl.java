package com.costcount.service.impl;

import com.costcount.dto.AccountQueryDTO;
import com.costcount.dto.AccountSaveDTO;
import com.costcount.entity.Account;
import com.costcount.entity.AccountProvider;
import com.costcount.entity.AccountType;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.AccountProviderMapper;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.service.AccountService;
import com.costcount.vo.AccountVO;
import com.github.yulichang.base.MPJBaseServiceImpl;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.costcount.common.CommonConstant.*;

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

    private static final Set<String> FIXED_ACCOUNT_TYPE_CODES = Set.of(ZHI_FU_BAO_CREDIT, JING_DONG_CREDIT);

    @Override
    public List<AccountVO> listAllAccount(AccountQueryDTO dto) {

        MPJLambdaWrapper<Account> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAsClass(Account.class, AccountVO.class)
                .selectAs(AccountProvider::getId, AccountVO::getProviderId)
                .select(AccountProvider::getProviderName)
                .select(AccountType::getTypeName, AccountType::getTypeCode)
                .leftJoin(AccountType.class, AccountType::getId, Account::getAccTypeId)
                .leftJoin(AccountProvider.class, AccountProvider::getId, AccountType::getAccProviderId)
                .orderByAsc(Account::getSort)
                .orderByDesc(Account::getCreatedTime);

        List<AccountVO> accountVOList = accountMapper.selectJoinList(AccountVO.class, wrapper);

        return Optional.ofNullable(accountVOList).orElse(List.of());
    }

    @Override
    public AccountVO getAccount(Long id) {
        MPJLambdaWrapper<Account> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAsClass(Account.class, AccountVO.class)
                .selectAs(AccountProvider::getId, AccountVO::getProviderId)
                .select(AccountProvider::getProviderName)
                .select(AccountType::getTypeName, AccountType::getTypeCode)
                .leftJoin(AccountType.class, AccountType::getId, Account::getAccTypeId)
                .leftJoin(AccountProvider.class, AccountProvider::getId, AccountType::getAccProviderId)
                .eq(Account::getId, id);

        return accountMapper.selectJoinOne(AccountVO.class, wrapper);
    }

    @Override
    public String createAccount(AccountSaveDTO dto) {

        Long accTypeId = dto.getAccTypeId();

        AccountType accountType = accountTypeMapper.selectById(accTypeId);

        if (accountType == null) {
            throw new IllegalArgumentException("Account type not found");
        }

        AccountProvider accountProvider = accountProviderMapper.selectById(accountType.getAccProviderId());

        if (accountProvider == null) {
            throw new IllegalArgumentException("Account provider not found");
        }

        Account account = new Account();
        account.setAccTypeId(accTypeId);
        account.setAccTailNum(dto.getAccTailNum());
        account.setAccName(accountProvider.getProviderName() + " " + accountType.getTypeName() + dto.getAccTailNum());

        if (dto.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Debit account balance cannot be negative");
        }

        if (DEBIT.equalsIgnoreCase(accountType.getTypeCode())) {
            fillDEBITAccountBalance(account, dto);
        } else if (CREDIT.equalsIgnoreCase(accountType.getTypeCode())) {
            fillCREDITAccountBalance(account, dto);
        } else {
            throw new IllegalArgumentException("Invalid account type code");
        }

        account.setSort(dto.getSort() == null ? DEFAULT_SORT : dto.getSort());
        account.setStatus(dto.getStatus() == null ? NORMAL_STATUS : dto.getStatus());

        account.setIcon(buildAccountIcon(accountType.getTypeName(), accountProvider.getIcon(), account));


        account.setRemarks(dto.getRemark());

        accountMapper.insert(account);

        return String.valueOf(account.getId());
    }

    void fillDEBITAccountBalance(Account account, AccountSaveDTO dto) {
        account.setBalance(dto.getBalance() == null ? BigDecimal.ZERO : dto.getBalance());
        account.setCreditLimit(null);
        account.setIdealCreditLimit(null);
    }

    void fillCREDITAccountBalance(Account account, AccountSaveDTO dto) {
        account.setBalance(dto.getBalance() == null ? BigDecimal.ZERO : dto.getBalance());
        account.setCreditLimit(dto.getCreditLimit() == null ? BigDecimal.ZERO : dto.getCreditLimit());
        account.setIdealCreditLimit(dto.getIdealCreditLimit() == null ? BigDecimal.ZERO : dto.getIdealCreditLimit());
    }

    @Override
    public String updateAccount(AccountSaveDTO dto) {

        if (dto.getId() == null) {
            throw new IllegalArgumentException("Account ID is required for update");
        }

        Account account = getById(dto.getId());
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }

        if (dto.getBalance().compareTo(BigDecimal.ZERO) < 0 ||
                dto.getCreditLimit().compareTo(BigDecimal.ZERO) < 0 ||
                dto.getIdealCreditLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Account balance, credit limit, and ideal credit limit cannot be negative");
        }

        AccountType accountType = accountTypeMapper.selectById(account.getAccTypeId());

        if (accountType == null) {
            throw new IllegalArgumentException("Account type not found");
        }

        if (DEBIT.equalsIgnoreCase(accountType.getTypeCode())) {
            account.setBalance(dto.getBalance() == null ? account.getBalance() : dto.getBalance());
        } else if (CREDIT.equalsIgnoreCase(accountType.getTypeCode())) {
            account.setBalance(dto.getBalance() == null ? account.getBalance() : dto.getBalance());
            account.setCreditLimit(dto.getCreditLimit() == null ? account.getCreditLimit() : dto.getCreditLimit());
            account.setIdealCreditLimit(dto.getIdealCreditLimit() == null ? account.getIdealCreditLimit() : dto.getIdealCreditLimit());
        } else {
            throw new IllegalArgumentException("Invalid account type code");
        }

        String accTailNum = account.getAccTailNum();
        String updateTailNum = dto.getAccTailNum();

        if (updateTailNum != null && !updateTailNum.equals(accTailNum)) {
            boolean exists = lambdaQuery().eq(Account::getAccTailNum, accTailNum)
                    .eq(Account::getAccTypeId, account.getAccTypeId())
                    .exists();

            if (exists) {
                throw new IllegalArgumentException("Account tail number already exists");
            }

            account.setAccTailNum(updateTailNum);

            AccountProvider accountProvider = accountProviderMapper.selectById(accountType.getAccProviderId());

            account.setIcon(buildAccountIcon(accountType.getTypeName(), accountProvider.getIcon(), account));
        }

        account.setAccTailNum(accTailNum);

        account.setSort(dto.getSort() == null ? account.getSort() : dto.getSort());
        account.setRemarks(dto.getRemark());

        accountMapper.updateById(account);

        return String.valueOf(account.getId());
    }

    private String buildAccountIcon(String accountTypeName, String providerIcon, Account account) {
        // TODO: Implement icon building logic
        return providerIcon;
    }

    @Override
    public void deleteAccount(Long id) {
        Account account = getById(id);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }

        // TODO: Add logic to check if the account is referenced by other entities before deletion
        removeById(id);
    }
}
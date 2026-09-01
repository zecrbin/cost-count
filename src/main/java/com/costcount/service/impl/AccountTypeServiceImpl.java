package com.costcount.service.impl;

import com.costcount.dto.account.type.AccountTypeQueryDto;
import com.costcount.entity.Account;
import com.costcount.entity.AccountProvider;
import com.costcount.entity.AccountType;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.AccountProviderMapper;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.service.AccountTypeService;
import com.costcount.vo.account.type.AccountTypeVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class AccountTypeServiceImpl
    extends MPJBaseServiceImpl<AccountTypeMapper, AccountType>
    implements AccountTypeService {

    @Resource
    private AccountTypeMapper accountTypeMapper;

    @Resource
    private AccountProviderMapper accountProviderMapper;

    @Resource
    private AccountMapper accountMapper;

    @Override
    public List<AccountTypeVO> listAccountTypes(AccountTypeQueryDto queryDTO) {
        AccountTypeQueryDto query = Optional.ofNullable(queryDTO).orElseGet(AccountTypeQueryDto::new);
        MPJLambdaWrapper<AccountType> wrapper = new MPJLambdaWrapper<AccountType>()
                .selectAsClass(AccountType.class, AccountTypeVO.class)
                .select(AccountProvider::getProviderName)
                .leftJoin(AccountProvider.class, AccountProvider::getId, AccountType::getAccProviderId)
                .eq(query.getAccProviderId() != null, AccountType::getAccProviderId, query.getAccProviderId())
                .eq(query.getStatus() != null, AccountType::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getTypeName()), AccountType::getTypeName, query.getTypeName())
                .like(StringUtils.hasText(query.getTypeCode()), AccountType::getTypeCode, query.getTypeCode())
                .orderByAsc(AccountType::getSort);

        return Optional.ofNullable(accountTypeMapper.selectJoinList(AccountTypeVO.class, wrapper))
                .orElseGet(List::of);
    }

    @Override
    public String addAccountType(AccountType accountType) {

        if (accountType.getAccProviderId() == null) {
            throw new IllegalArgumentException("Account provider ID cannot be null");
        }

        AccountProvider accountProvider = accountProviderMapper.selectById(accountType.getAccProviderId());

        if (accountProvider == null) {
            throw new IllegalArgumentException("Account provider does not exist");
        }

        if (lambdaQuery().eq(AccountType::getAccProviderId, accountType.getAccProviderId())
                .eq(AccountType::getTypeName, accountType.getTypeName())
                .exists()) {
            throw new IllegalArgumentException("Account type name already exists for this provider");
        }

        accountType.setId(null);
        accountTypeMapper.insert(accountType);
        return accountType.getId().toString();
    }

    @Override
    public String updateAccountType(AccountType accountType) {
        if (accountType == null || accountType.getId() == null) {
            throw new IllegalArgumentException("Account type ID cannot be null");
        }

        AccountProvider accountProvider = accountProviderMapper.selectById(accountType.getAccProviderId());
        if (accountProvider == null) {
            throw new IllegalArgumentException("Account provider does not exist");
        }

        if (lambdaQuery().eq(AccountType::getAccProviderId, accountType.getAccProviderId())
                .eq(AccountType::getTypeName, accountType.getTypeName())
                .ne(AccountType::getId, accountType.getId())
                .exists()) {
            throw new IllegalArgumentException("Account type name already exists for this provider");
        }

        if (!updateById(accountType)) {
            throw new IllegalArgumentException("Account type does not exist");
        }

        return accountType.getId().toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Void deleteAccountType(List<Long> accountTypeIds) {

        MPJLambdaWrapper<Account> wrapper = new MPJLambdaWrapper<>();
        wrapper.in(Account::getAccTypeId, accountTypeIds);
        Long count = accountMapper.selectJoinCount(wrapper);

        if (count > 0) {
            throw new IllegalArgumentException("Cannot delete account type(s) that are in use by accounts");
        }

        removeByIds(accountTypeIds);
        return null;
    }
}

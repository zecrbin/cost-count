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
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        MPJLambdaWrapper<AccountType> wrapper = new MPJLambdaWrapper<>();

        wrapper.selectAsClass(AccountType.class, AccountTypeVO.class)
                .eq(queryDTO.getAccProviderId() != null, AccountType::getAccProviderId, queryDTO.getAccProviderId())
                .eq(queryDTO.getStatus() != null, AccountType::getStatus, queryDTO.getStatus())
                .like(queryDTO.getTypeName() != null, AccountType::getTypeName, queryDTO.getTypeName())
                .like(queryDTO.getTypeCode() != null, AccountType::getTypeCode, queryDTO.getTypeCode())
                .orderByAsc(AccountType::getSort);

        return Optional.ofNullable(accountTypeMapper.selectList(wrapper))
                .orElse(List.of())
                .stream()
                .map(accountType -> {
                    AccountTypeVO accountTypeVO = new AccountTypeVO();
                    BeanUtils.copyProperties(accountType, accountTypeVO);
                    return accountTypeVO;
                })
                .toList();

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

        if (lambdaQuery().eq(AccountType::getTypeName, accountType.getTypeName())
                .ne(AccountType::getId, accountType.getId())
                .exists()) {
            throw new IllegalArgumentException("Account type name already exists");
        }

        updateById(accountType);

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
package com.costcount.service.impl;

import com.costcount.entity.AccountProvider;
import com.costcount.entity.AccountType;
import com.costcount.exception.BizException;
import com.costcount.mapper.AccountProviderMapper;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.service.AccountProviderService;
import com.costcount.vo.account.provider.AccountProviderVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AccountProviderServiceImpl
        extends MPJBaseServiceImpl<AccountProviderMapper, AccountProvider>
        implements AccountProviderService {

    @Resource
    private AccountTypeMapper accountTypeMapper;

    @Override
    public List<AccountProviderVO> listAccountProviders(String providerName) {

        return Optional.ofNullable(lambdaQuery().select(AccountProvider::getId, AccountProvider::getProviderName, AccountProvider::getIcon)
                .like(providerName != null, AccountProvider::getProviderName, providerName)
                .orderByAsc(AccountProvider::getProviderName)
                .list())
                .orElse(List.of())
                .stream()
                .map(accountProvider -> {
                    AccountProviderVO vo = new AccountProviderVO();
                    BeanUtils.copyProperties(accountProvider, vo);
                    return vo;
                })
                .toList();
    }

    @Override
    public String addAccountProvider(AccountProvider accountProvider) {

        if (accountProvider == null || StringUtils.isBlank(accountProvider.getProviderName())) {
            throw new BizException("Provider name cannot be empty");
        }

        if (lambdaQuery().eq(AccountProvider::getProviderName, accountProvider.getProviderName())
                .exists()) {
            throw new BizException("Provider name already exists");
        }

        accountProvider.setId(null);
        save(accountProvider);
        return accountProvider.getId().toString();
    }

    @Override
    public String updateAccountProvider(AccountProvider accountProvider) {
        if (accountProvider == null || accountProvider.getId() == null) {
            throw new BizException("Account provider ID cannot be null");
        }

        if (StringUtils.isBlank(accountProvider.getProviderName())) {
            throw new BizException("Provider name cannot be empty");
        }

        if (lambdaQuery().eq(AccountProvider::getProviderName, accountProvider.getProviderName())
                .ne(AccountProvider::getId, accountProvider.getId())
                .exists()) {
            throw new BizException("Provider name already exists");
        }

        updateById(accountProvider);
        return accountProvider.getId().toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Void deleteAccountProvider(List<Long> accountProviderIds) {

        MPJLambdaWrapper<AccountType> wrapper = new MPJLambdaWrapper<>();
        wrapper.in(AccountType::getAccProviderId, accountProviderIds);

        if (accountTypeMapper.selectCount(wrapper) > 0) {
            throw new BizException("Cannot delete provider with associated account types");
        }

        removeByIds(accountProviderIds);
        return null;
    }

}
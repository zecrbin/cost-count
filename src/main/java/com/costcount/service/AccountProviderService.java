package com.costcount.service;

import com.costcount.entity.AccountProvider;
import com.costcount.vo.account.provider.AccountProviderVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

public interface AccountProviderService extends MPJBaseService<AccountProvider> {

    List<AccountProviderVO> listAccountProviders(String providerName);

    String addAccountProvider(AccountProvider accountProvider);

    String updateAccountProvider(AccountProvider accountProvider);

    Void deleteAccountProvider(List<Long> accountProviderIds);
}
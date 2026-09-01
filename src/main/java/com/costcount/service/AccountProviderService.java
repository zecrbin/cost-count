package com.costcount.service;

import com.costcount.entity.AccountProvider;
import com.costcount.vo.account.provider.AccountProviderVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

/**
 * 账户提供方维护服务。
 */
public interface AccountProviderService extends MPJBaseService<AccountProvider> {

    /** 按名称模糊查询账户提供方。 */
    List<AccountProviderVO> listAccountProviders(String providerName);

    /** 新增账户提供方并返回ID。 */
    String addAccountProvider(AccountProvider accountProvider);

    /** 修改账户提供方并返回ID。 */
    String updateAccountProvider(AccountProvider accountProvider);

    /** 批量删除未关联账户类型的账户提供方。 */
    Void deleteAccountProvider(List<Long> accountProviderIds);
}

package com.costcount.service;

import com.costcount.dto.account.provider.AccountProviderSaveDTO;
import com.costcount.dto.account.provider.AccountProviderQueryDTO;
import com.costcount.entity.AccountProvider;
import com.costcount.vo.account.provider.AccountProviderVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

/**
 * 账户提供方维护服务。
 */
public interface AccountProviderService extends MPJBaseService<AccountProvider> {

    /** 查询账户提供方，支持按名称模糊筛选。 */
    List<AccountProviderVO> listAccountProviders(AccountProviderQueryDTO query);

    /** 查询账户提供方详情。 */
    AccountProviderVO getAccountProvider(Long id);

    /** 新增账户提供方并返回ID。 */
    String addAccountProvider(AccountProviderSaveDTO dto);

    /** 修改账户提供方并返回ID。 */
    String updateAccountProvider(AccountProviderSaveDTO dto);

    /** 批量删除未关联账户类型的账户提供方。 */
    void deleteAccountProviders(List<Long> accountProviderIds);
}

package com.costcount.service;

import com.costcount.dto.account.type.AccountTypeQueryDto;
import com.costcount.entity.AccountType;
import com.costcount.vo.account.type.AccountTypeVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

/**
 * 账户类型查询及维护服务。
 */
public interface AccountTypeService extends MPJBaseService<AccountType> {

    /** 根据提供方、状态、编码或名称查询账户类型。 */
    List<AccountTypeVO> listAccountTypes(AccountTypeQueryDto queryDTO);

    /** 新增账户类型并返回ID。 */
    String addAccountType(AccountType accountType);

    /** 修改账户类型并返回ID。 */
    String updateAccountType(AccountType accountType);

    /** 批量删除未被账户使用的账户类型。 */
    Void deleteAccountType(List<Long> accountTypeIds);
}

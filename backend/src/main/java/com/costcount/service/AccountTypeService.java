package com.costcount.service;

import com.costcount.dto.account.type.AccountTypeQueryDTO;
import com.costcount.dto.account.type.AccountTypeSaveDTO;
import com.costcount.entity.AccountType;
import com.costcount.vo.account.type.AccountTypeVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

/**
 * 账户类型服务。
 */
public interface AccountTypeService extends MPJBaseService<AccountType> {

    /**
     * 查询账户类型列表。
     */
    List<AccountTypeVO> listAccountTypes(AccountTypeQueryDTO query);

    /**
     * 查询账户类型详情。
     */
    AccountTypeVO getAccountType(Long id);

    /**
     * 新增账户类型并返回 ID。
     */
    String addAccountType(AccountTypeSaveDTO dto);

    /**
     * 修改账户类型并返回 ID。
     */
    String updateAccountType(AccountTypeSaveDTO dto);

    /**
     * 批量删除未被账户使用的账户类型。
     */
    void deleteAccountTypes(List<Long> accountTypeIds);
}

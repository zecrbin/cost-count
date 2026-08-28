package com.costcount.service;

import com.costcount.dto.AccountQueryDTO;
import com.costcount.dto.account.type.AccountTypeQueryDto;
import com.costcount.entity.AccountType;
import com.costcount.vo.account.type.AccountTypeVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

public interface AccountTypeService extends MPJBaseService<AccountType> {

    List<AccountTypeVO> listAccountTypes(AccountTypeQueryDto queryDTO);

    String addAccountType(AccountType accountType);

    String updateAccountType(AccountType accountType);

    Void deleteAccountType(List<Long> accountTypeIds);
}
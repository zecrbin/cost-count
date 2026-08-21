package com.costcount.service;

import com.costcount.dto.AccountSaveDTO;
import com.costcount.entity.Account;
import com.costcount.vo.AccountVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

public interface AccountService extends MPJBaseService<Account> {
    List<AccountVO> listAll();
    String create(AccountSaveDTO dto);
    String update(Long id, AccountSaveDTO dto);
}

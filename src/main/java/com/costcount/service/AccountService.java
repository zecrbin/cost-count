package com.costcount.service;

import com.costcount.dto.AccountQueryDTO;
import com.costcount.dto.AccountSaveDTO;
import com.costcount.entity.Account;
import com.costcount.vo.AccountVO;
import com.github.yulichang.base.MPJBaseService;

import java.util.List;

public interface AccountService extends MPJBaseService<Account> {

    List<AccountVO> listAllAccount(AccountQueryDTO dto);

    AccountVO getAccount(Long id);

    String createAccount(AccountSaveDTO dto);

    String updateAccount(AccountSaveDTO dto);

    void deleteAccount(Long id);
}
package com.costcount.service.impl;

import com.costcount.entity.Account;
import com.costcount.mapper.AccountMapper;
import com.costcount.service.AccountService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl
        extends MPJBaseServiceImpl<AccountMapper, Account>
        implements AccountService {
}

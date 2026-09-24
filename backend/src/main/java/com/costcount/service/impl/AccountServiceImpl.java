package com.costcount.service.impl;

import com.costcount.entity.Account;
import com.costcount.mapper.AccountMapper;
import com.costcount.service.AccountService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import org.springframework.stereotype.Service;

/** 账户聚合服务，负责账户资料、初始流水及动态图标的协同维护。 */
@Service
public class AccountServiceImpl
        extends MPJBaseServiceImpl<AccountMapper, Account>
        implements AccountService {


}

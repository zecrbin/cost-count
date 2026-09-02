package com.costcount.service.impl;

import com.costcount.entity.AccountType;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.service.AccountTypeService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AccountTypeServiceImpl
        extends MPJBaseServiceImpl<AccountTypeMapper, AccountType>
        implements AccountTypeService {
}

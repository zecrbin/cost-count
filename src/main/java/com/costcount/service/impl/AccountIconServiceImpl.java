package com.costcount.service.impl;

import com.costcount.entity.AccountIcon;
import com.costcount.mapper.AccountIconMapper;
import com.costcount.service.AccountIconService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AccountIconServiceImpl
        extends MPJBaseServiceImpl<AccountIconMapper, AccountIcon>
        implements AccountIconService {
}

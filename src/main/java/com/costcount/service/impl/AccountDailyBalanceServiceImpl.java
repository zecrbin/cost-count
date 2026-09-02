package com.costcount.service.impl;

import com.costcount.entity.AccountDailyBalance;
import com.costcount.mapper.AccountDailyBalanceMapper;
import com.costcount.service.AccountDailyBalanceService;
import com.github.yulichang.base.MPJBaseServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AccountDailyBalanceServiceImpl
        extends MPJBaseServiceImpl<AccountDailyBalanceMapper, AccountDailyBalance>
        implements AccountDailyBalanceService {
}

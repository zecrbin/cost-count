package com.costcount.service.impl;

import com.costcount.entity.Account;
import com.costcount.mapper.AccountMapper;
import com.costcount.mapper.AccountTypeMapper;
import com.costcount.service.AccountService;
import com.costcount.vo.AccountVO;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl
    extends MPJBaseServiceImpl<AccountMapper, Account>
        implements AccountService {

    private static final String DEBIT = "DEBIT";

    private static final String CREDIT = "CREDIT";

    private final AccountMapper accountMapper;

    private final AccountTypeMapper accountTypeMapper;

    private void fillCalculatedFields(
        List<AccountVO> accounts
    ) {

        for (AccountVO account : accounts) {

            if (!CREDIT.equals(account.getTypeCode())) {
                account.setAvailableCredit(null);
                continue;
            }

            BigDecimal creditLimit =
                defaultZero(account.getCreditLimit());

            BigDecimal outstanding =
                defaultZero(account.getBalance());

            account.setAvailableCredit(
                creditLimit.subtract(outstanding)
            );
        }
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null
            ? BigDecimal.ZERO
            : value;
    }
}
package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.account.AccountDailyBalanceQueryDTO;
import com.costcount.service.AccountDailyBalanceService;
import com.costcount.vo.account.AccountDailyBalanceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "账户日余额管理")
@RestController
@RequestMapping("/api/account-daily-balances")
public class AccountDailyBalanceController {

    @Resource
    private AccountDailyBalanceService accountDailyBalanceService;

    @Operation(summary = "查询账户活动日余额")
    @PostMapping("/list")
    public R<List<AccountDailyBalanceVO>> list(@RequestBody @Valid AccountDailyBalanceQueryDTO query) {
        return R.ok(accountDailyBalanceService.listAccountDailyBalances(query));
    }
}

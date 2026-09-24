package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.account.AccountBalanceDto;
import com.costcount.dto.account.AccountQueryDTO;
import com.costcount.dto.account.AccountSaveDTO;
import com.costcount.service.AccountService;
import com.costcount.vo.account.AccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 提供账户查询、维护及余额修正接口。 */
@Tag(name = "账户管理")
@Validated
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Resource
    private AccountService accountService;

    @Operation(summary = "查询账户列表")
    @PostMapping("/list")
    public R<List<AccountVO>> list(@RequestBody AccountQueryDTO query) {
        return R.ok(accountService.listAccounts(query));
    }

    @Operation(summary = "查询账户详情")
    @GetMapping("/{id}")
    public R<AccountVO> get(@PathVariable Long id) {
        return R.ok(accountService.getAccount(id));
    }

    @Operation(summary = "新增账户")
    @PostMapping
    public R<String> save(@RequestBody @Validated AccountSaveDTO accountSaveDTO) {
        return R.ok(accountService.saveAccount(accountSaveDTO));
    }

    @Operation(summary = "修改账户", description = "初始资金仅新增时生效；余额请使用修改账户余额接口")
    @PutMapping
    public R<String> update(@RequestBody @Validated AccountSaveDTO accountSaveDTO) {
        return R.ok(accountService.updateAccount(accountSaveDTO));
    }

    @Operation(summary = "批量删除账户", description = "只剩初始流水的账户才能删除，有其他流水的请改为停用")
    @DeleteMapping
    public R<Void> delete(
            @RequestBody
            @NotEmpty(message = "账户 ID 不能为空")
            List<@NotNull(message = "账户 ID 不能为空") Long> ids
    ) {
        accountService.deleteAccounts(ids);
        return R.ok();
    }

    @Operation(summary = "修改账户余额")
    @PostMapping("/updateBalance")
    public R<String> updateBalance(@RequestBody @Validated AccountBalanceDto accountBalanceDto) {
        return R.ok(accountService.updateBalance(accountBalanceDto));
    }

}

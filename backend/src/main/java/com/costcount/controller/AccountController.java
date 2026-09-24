package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.account.AccountInitialBalanceAdjustDTO;
import com.costcount.dto.account.AccountQueryDTO;
import com.costcount.dto.account.AccountSaveDTO;
import com.costcount.service.AccountService;
import com.costcount.vo.account.AccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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

/** 提供账户查询、维护及初始资金修正接口。 */
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

    @Operation(summary = "新增账户", description = "同时写入初始资金流水；账户余额此后只由流水维护")
    @PostMapping
    public R<String> create(@RequestBody @Valid AccountSaveDTO dto) {
        return R.ok(accountService.addAccount(dto));
    }

    @Operation(summary = "修改账户资料", description = "不修改余额和初始资金")
    @PutMapping
    public R<String> update(@RequestBody @Valid AccountSaveDTO dto) {
        return R.ok(accountService.updateAccount(dto));
    }

    @Operation(summary = "修正账户初始资金", description = "修改初始资金流水金额并重算账户余额")
    @PutMapping("/{id}/initial-balance")
    public R<Void> adjustInitialBalance(@PathVariable Long id,
                                        @RequestBody @Valid AccountInitialBalanceAdjustDTO dto) {
        accountService.adjustInitialBalance(id, dto);
        return R.ok();
    }

    @Operation(summary = "批量删除账户", description = "仅允许删除没有交易流水的账户，有流水的账户请改为停用")
    @DeleteMapping
    public R<Void> delete(
            @RequestBody
            @NotEmpty(message = "账户 ID 不能为空")
            List<@NotNull(message = "账户 ID 不能为空") Long> ids
    ) {
        accountService.deleteAccounts(ids);
        return R.ok();
    }
}

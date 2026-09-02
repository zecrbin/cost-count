package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.account.provider.AccountProviderQueryDTO;
import com.costcount.dto.account.provider.AccountProviderSaveDTO;
import com.costcount.service.AccountProviderService;
import com.costcount.vo.account.provider.AccountProviderVO;
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

@Tag(name = "账户提供方管理")
@Validated
@RestController
@RequestMapping("/api/account-providers")
public class AccountProviderController {

    @Resource
    private AccountProviderService accountProviderService;

    @Operation(summary = "查询账户提供方列表")
    @PostMapping("/list")
    public R<List<AccountProviderVO>> list(@RequestBody AccountProviderQueryDTO query) {
        return R.ok(accountProviderService.listAccountProviders(query));
    }

    @Operation(summary = "查询账户提供方详情")
    @GetMapping("/{id}")
    public R<AccountProviderVO> get(@PathVariable Long id) {
        return R.ok(accountProviderService.getAccountProvider(id));
    }

    @Operation(summary = "新增账户提供方")
    @PostMapping
    public R<String> create(@RequestBody @Valid AccountProviderSaveDTO dto) {
        return R.ok(accountProviderService.addAccountProvider(dto));
    }

    @Operation(summary = "修改账户提供方")
    @PutMapping
    public R<String> update(@RequestBody @Valid AccountProviderSaveDTO dto) {
        return R.ok(accountProviderService.updateAccountProvider(dto));
    }

    @Operation(summary = "批量删除账户提供方")
    @DeleteMapping
    public R<Void> delete(
            @RequestBody
            @NotEmpty(message = "账户提供方ID不能为空")
            List<@NotNull(message = "账户提供方ID不能为空") Long> ids
    ) {
        accountProviderService.deleteAccountProviders(ids);
        return R.ok();
    }
}

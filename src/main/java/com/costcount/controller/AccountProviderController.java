package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.account.provider.AccountProviderSaveDTO;
import com.costcount.entity.AccountProvider;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    @GetMapping("/list")
    public R<List<AccountProviderVO>> list(@RequestParam(required = false) String providerName) {
        return R.ok(accountProviderService.listAccountProviders(providerName));
    }

    @Operation(summary = "新增账户提供方")
    @PostMapping
    public R<String> create(@RequestBody @Valid AccountProviderSaveDTO dto) {
        return R.ok(accountProviderService.addAccountProvider(toEntity(dto, null)));
    }

    @Operation(summary = "修改账户提供方")
    @PutMapping("/{id}")
    public R<String> update(
            @PathVariable("id") Long id,
            @RequestBody @Valid AccountProviderSaveDTO dto
    ) {
        return R.ok(accountProviderService.updateAccountProvider(toEntity(dto, id)));
    }

    @Operation(summary = "批量删除账户提供方")
    @DeleteMapping
    public R<Void> delete(@RequestBody @NotEmpty(message = "账户提供方ID不能为空") List<@NotNull Long> ids) {
        accountProviderService.deleteAccountProvider(ids);
        return R.ok();
    }

    private AccountProvider toEntity(AccountProviderSaveDTO dto, Long id) {
        AccountProvider accountProvider = new AccountProvider();
        accountProvider.setId(id);
        accountProvider.setProviderName(dto.getProviderName());
        accountProvider.setIcon(dto.getIcon());
        return accountProvider;
    }
}

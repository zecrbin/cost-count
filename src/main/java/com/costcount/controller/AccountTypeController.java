package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.account.type.AccountTypeQueryDTO;
import com.costcount.dto.account.type.AccountTypeSaveDTO;
import com.costcount.service.AccountTypeService;
import com.costcount.vo.account.type.AccountTypeVO;
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

@Tag(name = "账户类型管理")
@Validated
@RestController
@RequestMapping("/api/account-types")
public class AccountTypeController {

    @Resource
    private AccountTypeService accountTypeService;

    @Operation(summary = "查询账户类型列表")
    @PostMapping("/list")
    public R<List<AccountTypeVO>> list(@RequestBody AccountTypeQueryDTO query) {
        return R.ok(accountTypeService.listAccountTypes(query));
    }

    @Operation(summary = "查询账户类型详情")
    @GetMapping("/{id}")
    public R<AccountTypeVO> get(@PathVariable Long id) {
        return R.ok(accountTypeService.getAccountType(id));
    }

    @Operation(summary = "新增账户类型")
    @PostMapping
    public R<String> create(@RequestBody @Valid AccountTypeSaveDTO dto) {
        return R.ok(accountTypeService.addAccountType(dto));
    }

    @Operation(summary = "修改账户类型")
    @PutMapping
    public R<String> update(@RequestBody @Valid AccountTypeSaveDTO dto) {
        return R.ok(accountTypeService.updateAccountType(dto));
    }

    @Operation(summary = "批量删除账户类型")
    @DeleteMapping
    public R<Void> delete(
            @RequestBody
            @NotEmpty(message = "账户类型 ID 不能为空")
            List<@NotNull(message = "账户类型 ID 不能为空") Long> ids
    ) {
        accountTypeService.deleteAccountTypes(ids);
        return R.ok();
    }
}

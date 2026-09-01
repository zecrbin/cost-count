package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.account.type.AccountTypeQueryDto;
import com.costcount.dto.account.type.AccountTypeSaveDTO;
import com.costcount.entity.AccountType;
import com.costcount.service.AccountTypeService;
import com.costcount.vo.account.type.AccountTypeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;
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
    @GetMapping("/list")
    public R<List<AccountTypeVO>> list(@ParameterObject AccountTypeQueryDto query) {
        return R.ok(accountTypeService.listAccountTypes(query));
    }

    @Operation(summary = "新增账户类型")
    @PostMapping
    public R<String> create(@RequestBody @Valid AccountTypeSaveDTO dto) {
        return R.ok(accountTypeService.addAccountType(toEntity(dto, null)));
    }

    @Operation(summary = "修改账户类型")
    @PutMapping("/{id}")
    public R<String> update(
            @PathVariable("id") Long id,
            @RequestBody @Valid AccountTypeSaveDTO dto
    ) {
        return R.ok(accountTypeService.updateAccountType(toEntity(dto, id)));
    }

    @Operation(summary = "批量删除账户类型")
    @DeleteMapping
    public R<Void> delete(@RequestBody @NotEmpty(message = "账户类型ID不能为空") List<@NotNull Long> ids) {
        accountTypeService.deleteAccountType(ids);
        return R.ok();
    }

    private AccountType toEntity(AccountTypeSaveDTO dto, Long id) {
        AccountType accountType = new AccountType();
        accountType.setId(id);
        accountType.setAccProviderId(dto.getAccProviderId());
        accountType.setTypeCode(dto.getTypeCode());
        accountType.setTypeName(dto.getTypeName());
        accountType.setSort(dto.getSort());
        accountType.setStatus(dto.getStatus());
        return accountType;
    }
}

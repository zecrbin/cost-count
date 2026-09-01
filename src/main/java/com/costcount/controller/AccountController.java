package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.AccountQueryDTO;
import com.costcount.dto.AccountSaveDTO;
import com.costcount.service.AccountService;
import com.costcount.vo.AccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "账户管理")
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Resource
    private AccountService accountService;

    @Operation(summary = "查询账户列表")
    @GetMapping("/list")
    public R<List<AccountVO>> list(@ParameterObject AccountQueryDTO query) {
        return R.ok(accountService.listAllAccount(query));
    }

    @Operation(summary = "查询账户详情")
    @GetMapping("/{id}")
    public R<AccountVO> get(@PathVariable Long id) {
        return R.ok(accountService.getAccount(id));
    }

    @Operation(summary = "新增账户")
    @PostMapping
    public R<String> create(@RequestBody @Valid AccountSaveDTO dto) {
        return R.ok(accountService.createAccount(dto));
    }

    @Operation(summary = "修改账户")
    @PutMapping
    public R<String> update(
            @RequestBody @Valid AccountSaveDTO dto
    ) {

        return R.ok(accountService.updateAccount(dto));
    }

    @Operation(summary = "删除账户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return R.ok();
    }
}

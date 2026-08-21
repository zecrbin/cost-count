package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.AccountSaveDTO;
import com.costcount.service.AccountService;
import com.costcount.vo.AccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "资金账户")
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    @Resource
    private AccountService accountService;

    @Operation(summary = "查询全部账户")
    @GetMapping("/list")
    public R<List<AccountVO>> list() { return R.ok(accountService.listAll()); }

    @Operation(summary = "新增账户", description = "首次新增时，balance 同时作为初始金额")
    @PostMapping
    public R<String> create(@RequestBody @Valid AccountSaveDTO dto) { return R.ok(accountService.create(dto)); }

    @Operation(summary = "修改账户及校正余额")
    @PutMapping("/{id}")
    public R<String> update(@PathVariable Long id, @RequestBody @Valid AccountSaveDTO dto) {
        return R.ok(accountService.update(id, dto));
    }
}

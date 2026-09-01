package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.service.AccountIconLibraryService;
import com.costcount.vo.account.icon.AccountIconVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "账户图标库")
@RestController
@RequestMapping("/api/account-icons")
public class AccountIconController {

    @Resource
    private AccountIconLibraryService accountIconLibraryService;

    @Operation(summary = "查询默认账户图标")
    @GetMapping("/defaults")
    public R<List<AccountIconVO>> listDefaults() {
        return R.ok(accountIconLibraryService.listDefaultAccountIcons());
    }
}

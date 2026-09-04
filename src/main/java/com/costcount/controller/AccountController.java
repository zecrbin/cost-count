package com.costcount.controller;

import com.costcount.service.AccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供账户查询、维护及初始资金修正接口。 */
@Tag(name = "账户管理")
@Validated
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Resource
    private AccountService accountService;


}

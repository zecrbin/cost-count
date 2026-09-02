package com.costcount.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "交易流水管理")
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
}

package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.TransactionSaveDTO;
import com.costcount.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 记账流水接口。
 *
 * <p>流水写入、账户余额更新和每日汇总重算均由服务层在同一事务中完成，
 * Controller 只负责参数校验和响应封装。</p>
 */
@Tag(name = "记账流水")
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Resource
    private TransactionService transactionService;

    /** 创建收入、支出、转账或余额调整流水。 */
    @Operation(summary = "新增记账流水")
    @PostMapping
    public R<String> create(@RequestBody @Valid TransactionSaveDTO dto) {
        return R.ok(transactionService.createTransaction(dto));
    }

    /** 通过新增反向流水冲正已入账流水，保留原流水审计记录。 */
    @Operation(summary = "冲正记账流水")
    @PostMapping("/{id}/reverse")
    public R<String> reverse(@PathVariable Long id) {
        return R.ok(transactionService.reverseTransaction(id));
    }
}

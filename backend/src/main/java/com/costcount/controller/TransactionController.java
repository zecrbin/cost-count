package com.costcount.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.costcount.common.PageQuery;
import com.costcount.common.R;
import com.costcount.dto.transaction.TransactionQueryDTO;
import com.costcount.dto.transaction.TransactionSaveDTO;
import com.costcount.service.TransactionService;
import com.costcount.vo.transaction.TransactionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供交易流水查询、新增和修改接口。 */
@Tag(name = "交易流水管理")
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Resource
    private TransactionService transactionService;

    @Operation(summary = "分页查询交易流水列表")
    @PostMapping("/page")
    public R<Page<TransactionVO>> pageQuery(@RequestBody PageQuery<TransactionQueryDTO> pageQuery){
        return R.ok(transactionService.pageQueryTransactions(pageQuery));
    }

    @Operation(summary = "录入交易流水", description = "按发生时间自动判断：最新的流水直接追加，历史流水重放并重建余额快照")
    @PostMapping
    public R<String> save(@Valid @RequestBody TransactionSaveDTO transactionSaveDTO) {
        return R.ok(transactionService.saveTransaction(transactionSaveDTO));
    }
}

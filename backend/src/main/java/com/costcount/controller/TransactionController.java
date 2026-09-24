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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供交易流水查询、新增、修改和删除接口。 */
@Tag(name = "交易流水管理")
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Resource
    private TransactionService transactionService;

    @Operation(summary = "分页查询交易流水列表")
    @PostMapping("/page")
    public R<Page<TransactionVO>> pageQuery(@Valid @RequestBody PageQuery<TransactionQueryDTO> pageQuery) {
        return R.ok(transactionService.pageQueryTransactions(pageQuery));
    }

    @Operation(summary = "查询交易流水详情")
    @GetMapping("/{id}")
    public R<TransactionVO> get(@PathVariable Long id) {
        return R.ok(transactionService.getTransaction(id));
    }

    @Operation(summary = "新增交易流水")
    @PostMapping
    public R<String> save(@Valid @RequestBody TransactionSaveDTO transactionSaveDTO) {
        return R.ok(transactionService.saveTransaction(transactionSaveDTO));
    }

    @Operation(summary = "修改交易流水")
    @PutMapping
    public R<String> update(@Valid @RequestBody TransactionSaveDTO transactionSaveDTO) {
        return R.ok(transactionService.updateTransaction(transactionSaveDTO));
    }

    @Operation(summary = "删除交易流水")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return R.ok();
    }
}

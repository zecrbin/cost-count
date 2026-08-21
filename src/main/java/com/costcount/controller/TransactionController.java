package com.costcount.controller;

import com.costcount.common.R;
import com.costcount.dto.TransactionPageQuery;
import com.costcount.dto.TransactionSaveDTO;
import com.costcount.service.TransactionRecordService;
import com.costcount.vo.PageResult;
import com.costcount.vo.TransactionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "日常收支")
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    @Resource
    private TransactionRecordService transactionRecordService;

    @Operation(summary = "分页查询日常收支")
    @PostMapping("/page")
    public R<PageResult<TransactionVO>> page(@RequestBody @Valid TransactionPageQuery query) {
        return R.ok(transactionRecordService.pageList(query));
    }

    @Operation(summary = "新增一笔日常收支", description = "保存后自动增减对应账户余额")
    @PostMapping
    public R<String> create(@RequestBody @Valid TransactionSaveDTO dto) {
        return R.ok(transactionRecordService.create(dto));
    }

    @Operation(summary = "删除一笔收支", description = "删除后自动恢复对应账户余额")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) { return R.ok(transactionRecordService.delete(id)); }
}

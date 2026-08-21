package com.costcount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "收支记录查询条件")
public class TransactionQueryDTO {
    @Schema(description = "账目类型，INCOME-收入，EXPENSE-支出，TRANSFER-转账", example = "EXPENSE")
    private String type;
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;
    @Schema(description = "账户ID", example = "1")
    private Long accountId;
    @Schema(description = "开始日期", example = "2026-08-01")
    private LocalDate startDate;
    @Schema(description = "结束日期", example = "2026-08-31")
    private LocalDate endDate;
    @Schema(description = "商户或备注关键词", example = "超市")
    private String keyword;
}

package com.costcount.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "交易流水查询参数")
public class TransactionQueryDTO {

    @Schema(description = "关联账户 ID，匹配主账户或转账目标账户", example = "30001")
    private Long accountId;

    @Schema(description = "分类 ID", example = "50001")
    private Long categoryId;

    @Schema(description = "流水类型：INITIAL、INCOME、EXPENSE、TRANSFER、ADJUSTMENT", example = "EXPENSE")
    private String transactionType;

    @Schema(description = "流水发生时间起点", example = "2026-09-01 00:00:00")
    private LocalDateTime startTime;

    @Schema(description = "流水发生时间终点", example = "2026-09-30 23:59:59")
    private LocalDateTime endTime;
}

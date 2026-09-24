package com.costcount.vo.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "交易流水信息")
public class TransactionVO {

    @Schema(description = "流水 ID", example = "40001")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "流水类型", example = "EXPENSE")
    private String transactionType;

    @Schema(description = "分类 ID", example = "50001")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long categoryId;

    @Schema(description = "主账户 ID", example = "30001")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long accountId;

    private String accountName;

    @Schema(description = "目标账户 ID", example = "30002")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long targetAccountId;

    private String targetAccountName;

    @Schema(description = "业务金额", example = "99.99")
    private BigDecimal amount;

    @Schema(description = "主账户余额变化", example = "-99.99")
    private BigDecimal balanceChange;

    @Schema(description = "主账户本笔后余额", example = "900.01")
    private BigDecimal balanceAfter;

    @Schema(description = "交易发生时间", example = "2026-09-03 12:30:00")
    private LocalDateTime transactionTime;

    @Schema(description = "交易对象或商户", example = "京东商城")
    private String counterparty;

    @Schema(description = "流水来源：MANUAL、IMPORT、AI、SYSTEM", example = "MANUAL")
    private String source;

    @Schema(description = "请求幂等号", example = "client-20260903-0001")
    private String requestId;

    @Schema(description = "备注", example = "购买日用品")
    private String remark;
}

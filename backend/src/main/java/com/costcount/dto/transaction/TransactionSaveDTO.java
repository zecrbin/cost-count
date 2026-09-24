package com.costcount.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "交易流水保存参数")
public class TransactionSaveDTO {

    @Schema(description = "ID，修改时必填", example = "40001")
    private Long id;

    @Schema(description = "流水类型：INCOME收入、EXPENSE支出、TRANSFER转账、ADJUSTMENT余额调整", example = "EXPENSE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "流水类型不能为空")
    @Size(max = 64, message = "流水类型不能超过64个字符")
    private String transactionType;

    @Schema(description = "主账户 ID；转账时为转出账户", example = "30001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "主账户 ID 不能为空")
    private Long accountId;

    @Schema(description = "转账目标账户 ID，仅转账时必填", example = "30002")
    private Long targetAccountId;

    @Schema(description = "收入或支出分类 ID；收入和支出时必填", example = "50001")
    private Long categoryId;

    @Schema(description = "业务金额，始终为正数", example = "99.99", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "余额调整值，可正可负，仅 ADJUSTMENT 时必填", example = "-20.00")
    private BigDecimal balanceChange;

    @Schema(description = "交易发生时间", example = "2026-09-03 12:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "交易发生时间不能为空")
    private LocalDateTime transactionTime;

    @Schema(description = "交易对象或商户", example = "京东商城")
    @Size(max = 256, message = "交易对象不能超过256个字符")
    private String counterparty;

    @Schema(description = "请求幂等号；相同请求重试时返回已有流水", example = "client-20260903-0001")
    @Size(max = 128, message = "请求幂等号不能超过128个字符")
    private String requestId;

    @Schema(description = "备注", example = "购买日用品")
    @Size(max = 256, message = "备注不能超过256个字符")
    private String remark;
}

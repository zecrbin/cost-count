package com.costcount.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.costcount.common.TransactionConstant.INITIAL;

@Data
public class InitialTransactionSaveDTO {

    @Schema(description = "流水类型，固定为 INITIAL", example = "INITIAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String transactionType = INITIAL;

    @Schema(description = "主账户 ID", example = "30001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "主账户 ID 不能为空")
    private Long accountId;

    @Schema(description = "业务金额，始终为正数", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "业务金额不能为空")
    @DecimalMin(value = "0.00", message = "初始资金不能小于0")
    @Digits(integer = 16, fraction = 2, message = "初始资金最多保留2位小数")
    private BigDecimal amount;

    @Schema(description = "交易时间，初始流水时间，通常为账户创建时间", example = "2026-09-03 12:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "交易时间不能为空")
    private LocalDateTime transactionTime;

    @Schema(description = "备注", example = "开户余额")
    private String remark;
}

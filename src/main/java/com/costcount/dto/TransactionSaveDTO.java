package com.costcount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "记账流水请求")
public class TransactionSaveDTO {

    @NotBlank
    @Schema(description = "流水类型：INCOME收入、EXPENSE支出、TRANSFER转账、ADJUSTMENT调整", example = "EXPENSE")
    private String transactionType;

    @Schema(description = "收入或支出分类ID")
    private Long categoryId;

    @NotNull
    @Schema(description = "主账户ID")
    private Long accountId;

    @Schema(description = "转账目标账户ID")
    private Long targetAccountId;

    @NotNull
    @DecimalMin(value = "0.01")
    @Schema(description = "业务金额，必须为正数", example = "12.50")
    private BigDecimal amount;

    @Schema(description = "ADJUSTMENT实际余额变化，可正可负；其他类型无需填写")
    private BigDecimal balanceChange;

    @Schema(description = "业务发生时间，未填写时使用当前时间")
    private LocalDateTime transactionTime;

    @Size(max = 128)
    private String counterparty;

    @Size(max = 32)
    @Schema(description = "来源：MANUAL、IMPORT、AI、SYSTEM")
    private String source;

    @Size(max = 64)
    @Schema(description = "业务幂等号，同一请求重试必须复用")
    private String requestId;

    @Size(max = 128)
    private String adjustmentReason;

    @Size(max = 500)
    private String remarks;
}

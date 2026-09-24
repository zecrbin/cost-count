package com.costcount.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "账户初始资金修正参数")
public class AccountInitialBalanceAdjustDTO {

    @Schema(description = "修正后的初始资金；资产账户为初始余额，负债账户为初始欠款", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "初始资金不能为空")
    @DecimalMin(value = "0.00", message = "初始资金不能小于0")
    @Digits(integer = 16, fraction = 2, message = "初始资金最多保留2位小数")
    private BigDecimal initialBalance;

    @Schema(description = "修正备注", example = "补录开户日余额")
    @Size(max = 256, message = "备注不能超过256个字符")
    private String remark;
}

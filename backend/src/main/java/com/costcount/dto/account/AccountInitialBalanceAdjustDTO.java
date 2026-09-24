package com.costcount.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "账户初始资金修正参数")
public class AccountInitialBalanceAdjustDTO {

    @Schema(description = "修正后的初始资金；资产账户为初始余额，负债账户为初始欠款", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "初始资金不能为空")
    private BigDecimal initialBalance;

    @Schema(description = "修正备注", example = "补录开户日余额")
    private String remark;
}

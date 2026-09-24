package com.costcount.dto.account;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountBalanceDto {
    /**
     * 账户 ID
     */
    @NotNull(message = "账户 ID 不能为空")
    private Long accountId;

    /**
     * 当前余额
     */
    @NotNull(message = "当前余额不能为空")
    private BigDecimal currentBalance;
}

package com.costcount.vo.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "账户活动日余额")
public class AccountDailyBalanceVO {

    @Schema(description = "账户 ID", example = "30001")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long accountId;

    @Schema(description = "发生流水的日期", example = "2026-09-03")
    private LocalDate statDate;

    @Schema(description = "当日开始余额", example = "1000.00")
    private BigDecimal openingBalance;

    @Schema(description = "当日流水净变化", example = "-99.99")
    private BigDecimal transactionChange;

    @Schema(description = "当日结束余额", example = "900.01")
    private BigDecimal closingBalance;
}

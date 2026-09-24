package com.costcount.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "账户日余额查询参数")
public class AccountDailyBalanceQueryDTO {

    @Schema(description = "账户 ID", example = "30001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账户 ID 不能为空")
    private Long accountId;

    @Schema(description = "开始日期（含）", example = "2026-09-01")
    private LocalDate startDate;

    @Schema(description = "结束日期（含）", example = "2026-09-30")
    private LocalDate endDate;
}

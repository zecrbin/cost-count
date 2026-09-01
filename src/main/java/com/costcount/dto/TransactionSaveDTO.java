package com.costcount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 新增流水请求。
 *
 * <p>普通收入/支出由服务层根据账户性质计算余额变化；只有 ADJUSTMENT
 * 允许调用方显式传入 {@code balanceChange}。</p>
 */
@Data
@Schema(description = "记账流水请求")
public class TransactionSaveDTO {

    /** 流水业务类型。 */
    @NotBlank
    @Schema(description = "流水类型：INCOME收入、EXPENSE支出、TRANSFER转账、ADJUSTMENT调整", example = "EXPENSE")
    private String transactionType;

    /** 收入或支出分类；转账和调整为空。 */
    @Schema(description = "收入或支出分类ID")
    private Long categoryId;

    /** 主账户；转账时表示转出账户。 */
    @NotNull
    @Schema(description = "主账户ID")
    private Long accountId;

    /** 转账目标账户，非转账为空。 */
    @Schema(description = "转账目标账户ID")
    private Long targetAccountId;

    /** 正数业务金额。 */
    @NotNull
    @DecimalMin(value = "0.01")
    @Schema(description = "业务金额，必须为正数", example = "12.50")
    private BigDecimal amount;

    /** 仅 ADJUSTMENT 使用的实际余额变化。 */
    @Schema(description = "ADJUSTMENT实际余额变化，可正可负；其他类型无需填写")
    private BigDecimal balanceChange;

    /** 业务发生时间；补录时可以早于当前入账时间。 */
    @Schema(description = "业务发生时间，未填写时使用当前时间")
    private LocalDateTime transactionTime;

    /** 交易对象或商户。 */
    @Size(max = 128)
    private String counterparty;

    /** 流水来源。 */
    @Size(max = 32)
    @Schema(description = "来源：MANUAL、IMPORT、AI、SYSTEM")
    private String source;

    /** 业务幂等号。 */
    @Size(max = 64)
    @Schema(description = "业务幂等号，同一请求重试必须复用")
    private String requestId;

    /** ADJUSTMENT 的业务原因，普通流水不使用。 */
    @Size(max = 128)
    private String adjustmentReason;

    /** 备注信息。 */
    @Size(max = 500)
    private String remarks;
}

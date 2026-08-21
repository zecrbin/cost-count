package com.costcount.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "资金账户")
public class AccountVO {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "账户ID", example = "1")
    private Long id;
    @Schema(description = "账户名称，与账户类型一致", example = "招商银行")
    private String name;
    @Schema(description = "具体账户类型，例如微信、支付宝、建设银行、花呗", example = "招商银行")
    private String type;
    @Schema(description = "账户性质，ASSET-资产账户，LIABILITY-负债账户", example = "ASSET")
    private String nature;
    @Schema(description = "当前金额；资产账户表示可用余额，负债账户表示待还金额", example = "12580.50")
    private BigDecimal balance;
    @Schema(description = "初始金额", example = "10000.00")
    private BigDecimal initialBalance;
    @Schema(description = "标识颜色", example = "#3154E5")
    private String color;
}

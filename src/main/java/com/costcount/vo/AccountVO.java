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
    @Schema(description = "账户名称", example = "招商银行")
    private String name;
    @Schema(description = "账户类型", example = "储蓄卡")
    private String type;
    @Schema(description = "当前余额", example = "12580.50")
    private BigDecimal balance;
    @Schema(description = "初始金额", example = "10000.00")
    private BigDecimal initialBalance;
    @Schema(description = "标识颜色", example = "#3154E5")
    private String color;
}

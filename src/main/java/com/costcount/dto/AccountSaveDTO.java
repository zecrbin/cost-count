package com.costcount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "账户新增或修改请求")
public class AccountSaveDTO {
    @NotBlank(message = "账户名称不能为空")
    @Size(max = 30, message = "账户名称最多30个字符")
    @Schema(description = "账户名称", example = "招商银行")
    private String name;

    @NotBlank(message = "账户类型不能为空")
    @Schema(description = "账户类型", example = "储蓄卡")
    private String type;

    @NotNull(message = "账户余额不能为空")
    @DecimalMin(value = "0", message = "账户余额不能小于0")
    @Schema(description = "当前余额，首次新增时作为初始金额", example = "12580.50")
    private BigDecimal balance;

    @Schema(description = "账户标识颜色", example = "#3154E5")
    private String color;
}

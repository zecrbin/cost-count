package com.costcount.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountSaveDTO {

    private Long id;

    @NotNull(message = "账户类型不能为空")
    private Long accTypeId;

    @Pattern(
        regexp = "^\\d{4}$",
        message = "账户尾号必须为4位数字"
    )
    private String accTailNum;

    @NotNull(message = "账户金额不能为空")
    @DecimalMin(value = "0.00", message = "账户金额不能小于0")
    private BigDecimal balance;

    @DecimalMin(value = "0.00", message = "信用额度不能小于0")
    private BigDecimal creditLimit;

    @DecimalMin(value = "0.00", message = "理想信用额度不能小于0")
    private BigDecimal idealCreditLimit;

    private Integer sort = 0;

    private Integer status = 1;

    @Size(max = 256, message = "备注不能超过256个字符")
    private String remark;
}

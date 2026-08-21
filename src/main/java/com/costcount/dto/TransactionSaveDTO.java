package com.costcount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "日常收支新增请求")
public class TransactionSaveDTO {
    @NotBlank(message = "收支类型不能为空")
    @Pattern(regexp = "INCOME|EXPENSE", message = "收支类型仅支持 INCOME 或 EXPENSE")
    @Schema(description = "收支类型，INCOME-收入，EXPENSE-支出", example = "EXPENSE", allowableValues = {"INCOME", "EXPENSE"})
    private String type;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    @Schema(description = "交易金额", example = "36.50")
    private BigDecimal amount;

    @NotNull(message = "分类不能为空")
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @NotNull(message = "账户不能为空")
    @Schema(description = "账户ID", example = "1")
    private Long accountId;

    @NotNull(message = "交易日期不能为空")
    @Schema(description = "交易日期", example = "2026-08-21")
    private LocalDate transactionDate;

    @NotBlank(message = "交易对象不能为空")
    @Size(max = 60, message = "交易对象最多60个字符")
    @Schema(description = "商户或收入来源", example = "盒马鲜生")
    private String merchant;

    @Size(max = 200, message = "备注最多200个字符")
    @Schema(description = "备注", example = "家庭采购")
    private String note;
}

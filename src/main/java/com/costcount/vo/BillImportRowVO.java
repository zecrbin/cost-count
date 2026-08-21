package com.costcount.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "账单导入预览行")
public class BillImportRowVO {
    @Schema(description = "来源行号或图片序号", example = "2")
    private Integer rowNumber;
    @Schema(description = "INCOME-收入，EXPENSE-支出", example = "EXPENSE")
    private String type;
    @Schema(description = "金额", example = "36.50")
    private BigDecimal amount;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "匹配到的分类ID", example = "12")
    private Long categoryId;
    @Schema(description = "原始或匹配后的分类名称", example = "午餐")
    private String categoryName;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "匹配到的账户ID", example = "1")
    private Long accountId;
    @Schema(description = "原始或匹配后的账户名称", example = "微信")
    private String accountName;
    @Schema(description = "交易日期", example = "2026-08-21")
    private LocalDate transactionDate;
    @Schema(description = "交易对象", example = "便利店")
    private String merchant;
    @Schema(description = "备注", example = "Excel 导入")
    private String note;
    @Schema(description = "识别置信度，0-100", example = "85")
    private Integer confidence;
    @Schema(description = "当前行是否可以直接导入", example = "true")
    private Boolean valid;
    @Schema(description = "需要人工修正的原因", example = "请选择资金账户")
    private String errorMessage;
}

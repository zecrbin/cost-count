package com.costcount.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "日常收支记录")
public class TransactionVO {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "记录ID", example = "1")
    private Long id;
    @Schema(description = "类型，INCOME-收入，EXPENSE-支出，TRANSFER-转账", example = "EXPENSE")
    private String type;
    @Schema(description = "金额", example = "36.50")
    private BigDecimal amount;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;
    @Schema(description = "分类名称", example = "餐饮")
    private String categoryName;
    @Schema(description = "分类颜色", example = "#FF7467")
    private String categoryColor;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "账户ID", example = "1")
    private Long accountId;
    @Schema(description = "账户名称", example = "日常消费账户")
    private String accountName;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "转入账户ID；仅转账记录有值", example = "2")
    private Long targetAccountId;
    @Schema(description = "交易日期", example = "2026-08-21")
    private LocalDate transactionDate;
    @Schema(description = "交易对象", example = "盒马鲜生")
    private String merchant;
    @Schema(description = "备注", example = "家庭采购")
    private String note;
    @Schema(description = "记录来源，MANUAL-手工，OCR-截图识别", example = "MANUAL")
    private String source;
    @Schema(description = "创建时间", example = "2026-08-21T12:30:00")
    private LocalDateTime createdTime;
}

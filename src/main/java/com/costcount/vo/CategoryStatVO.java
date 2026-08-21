package com.costcount.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "分类支出统计")
public record CategoryStatVO(
    @Schema(description = "分类名称", example = "餐饮") String categoryName,
    @Schema(description = "分类颜色", example = "#FF7467") String color,
    @Schema(description = "支出金额", example = "1380.50") BigDecimal amount,
    @Schema(description = "占本月支出的百分比", example = "32.6") BigDecimal percent
) {
}

package com.costcount.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "日常收支分类")
public class CategoryVO {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "分类ID", example = "1")
    private Long id;
    @Schema(description = "分类名称", example = "餐饮")
    private String name;
    @Schema(description = "类型，INCOME-收入，EXPENSE-支出", example = "EXPENSE")
    private String type;
    @Schema(description = "图标名称", example = "Utensils")
    private String icon;
    @Schema(description = "标识颜色", example = "#FF7467")
    private String color;
    @Schema(description = "是否系统内置分类", example = "true")
    private Boolean systemCategory;
}

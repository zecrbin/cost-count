package com.costcount.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "收支分类新增或修改请求")
public class CategorySaveDTO {
    @Schema(description = "父分类ID；不传表示新增一级分类", example = "1")
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 20, message = "分类名称最多20个字符")
    @Schema(description = "分类名称", example = "餐饮")
    private String name;

    @NotBlank(message = "收支类型不能为空")
    @Pattern(regexp = "INCOME|EXPENSE", message = "收支类型仅支持 INCOME 或 EXPENSE")
    @Schema(description = "分类类型，INCOME-收入，EXPENSE-支出", example = "EXPENSE", allowableValues = {"INCOME", "EXPENSE"})
    private String type;

    @Schema(description = "前端图标名称", example = "Utensils")
    private String icon;

    @Schema(description = "分类标识颜色", example = "#FF7467")
    private String color;
}

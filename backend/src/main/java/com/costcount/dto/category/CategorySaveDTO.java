package com.costcount.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "分类保存参数")
public class CategorySaveDTO {

    @Schema(description = "分类 ID，修改时必填", example = "50001")
    private Long id;

    @Schema(description = "父分类 ID；为空或 0 表示一级分类", example = "0")
    private Long pid;

    @Schema(description = "分类类型：INCOME、EXPENSE、TRANSFER", example = "EXPENSE",
            allowableValues = {"INCOME", "EXPENSE", "TRANSFER"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分类类型不能为空")
    private String categoryType;

    @Schema(description = "分类名称", example = "餐饮", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称不能超过64个字符")
    private String categoryName;

    @Schema(description = "分类图标路径或图标标识", example = "Utensils")
    @Size(max = 256, message = "图标不能超过256个字符")
    private String icon;

    @Schema(description = "同层级显示顺序，数值越小越靠前", example = "1")
    @Min(value = 0, message = "排序值不能小于0")
    private Integer sort = 0;

    @Schema(description = "备注", example = "日常三餐")
    @Size(max = 256, message = "备注不能超过256个字符")
    private String remark;
}

package com.costcount.vo.category;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "收支分类信息")
public class CategoryVO {

    @Schema(description = "分类 ID", example = "50101")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "父分类 ID，一级分类为 0", example = "50100")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long pid;

    @Schema(description = "分类类型：INCOME收入、EXPENSE支出", example = "EXPENSE")
    private String categoryType;

    @Schema(description = "分类名称", example = "早餐")
    private String categoryName;

    @Schema(description = "分类图标", example = "utensils")
    private String icon;

    @Schema(description = "同层级显示顺序", example = "1")
    private Integer sort;
}

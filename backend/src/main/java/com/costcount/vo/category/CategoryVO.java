package com.costcount.vo.category;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "分类信息")
public class CategoryVO {

    @Schema(description = "分类 ID", example = "50001")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "父分类 ID，0 表示一级分类", example = "0")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long pid;

    @Schema(description = "分类类型", example = "EXPENSE")
    private String categoryType;

    @Schema(description = "分类名称", example = "餐饮")
    private String categoryName;

    @Schema(description = "分类图标", example = "Utensils")
    private String icon;

    @Schema(description = "显示顺序", example = "1")
    private Integer sort;

    @Schema(description = "备注", example = "日常三餐")
    private String remark;

    @Schema(description = "子分类")
    private List<CategoryVO> children = new ArrayList<>();
}

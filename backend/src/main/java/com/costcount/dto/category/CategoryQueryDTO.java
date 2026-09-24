package com.costcount.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分类查询参数")
public class CategoryQueryDTO {

    @Schema(description = "分类类型：INCOME、EXPENSE、TRANSFER；为空时查询全部", example = "EXPENSE")
    private String categoryType;
}

package com.costcount.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "收支分类查询参数")
public class CategoryQueryDTO {

    @Schema(description = "分类类型：INCOME收入、EXPENSE支出；为空时返回全部", example = "EXPENSE")
    private String categoryType;
}

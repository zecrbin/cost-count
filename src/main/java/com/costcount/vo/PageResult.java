package com.costcount.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分页结果")
public record PageResult<T>(
    @Schema(description = "总记录数", example = "20") long total,
    @Schema(description = "当前页", example = "1") long pageNum,
    @Schema(description = "每页条数", example = "20") long pageSize,
    @Schema(description = "数据列表") List<T> records
) {
}

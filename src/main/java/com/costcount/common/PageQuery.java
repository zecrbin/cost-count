package com.costcount.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "分页请求")
public class PageQuery<T> {
    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Schema(description = "每页条数", example = "20")
    private Integer pageSize = 20;

    @Valid
    @Schema(description = "查询条件")
    private T params;

    public <E> Page<E> toPage() { return new Page<>(pageNum, pageSize); }
}

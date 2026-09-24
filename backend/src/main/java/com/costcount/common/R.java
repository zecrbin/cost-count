package com.costcount.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一响应")
public record R<T>(
        @Schema(description = "业务状态码", example = "200") Integer code,
        @Schema(description = "响应消息", example = "success") String message,
        @Schema(description = "响应数据") T data
) {
    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    public static <T> R<T> ok() {
        return new R<>(200, "success", null);
    }

    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message, null);
    }
}

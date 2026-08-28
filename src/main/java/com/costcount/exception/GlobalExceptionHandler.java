package com.costcount.exception;

import com.costcount.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException exception) {
        log.error("业务异常", exception);
        return R.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidException(MethodArgumentNotValidException exception) {
        log.error("参数校验失败", exception);
        String message = exception.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage).filter(Objects::nonNull).findFirst().orElse("参数校验失败");
        return R.fail(400, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleMessageNotReadableException(HttpMessageNotReadableException exception) {
        log.error("请求参数格式错误", exception);
        return R.fail(400, "请求参数格式错误，请检查枚举值和字段类型");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return R.fail(500, "系统繁忙，请稍后再试");
    }
}

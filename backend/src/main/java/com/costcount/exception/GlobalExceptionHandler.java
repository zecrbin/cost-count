package com.costcount.exception;

import com.costcount.common.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException exception) {
        log.error("业务异常", exception);
        return R.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 参数或业务前置条件不满足时，向前端返回具体的中文原因。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.error("请求参数不合法", exception);
        return R.fail(400, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidException(MethodArgumentNotValidException exception) {
        log.error("参数校验失败", exception);
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage).filter(Objects::nonNull).findFirst().orElse("参数校验失败");
        return R.fail(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        log.error("参数校验失败", exception);
        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("；"));

        if (!StringUtils.hasText(message)) {
            message = "参数校验失败";
        }
        return R.fail(400, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleMessageNotReadableException(HttpMessageNotReadableException exception) {
        log.error("请求参数格式错误", exception);
        return R.fail(400, "请求参数格式错误，请检查枚举值和字段类型");
    }

    /** 静态资源（如图标）不存在时返回真正的 404，而不是被兜底为 200 的"系统繁忙"。 */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNoResourceFoundException(NoResourceFoundException exception) {
        log.warn("资源不存在：{}", exception.getResourcePath());
        return R.fail(404, "资源不存在");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception) {
        log.warn("上传文件过大", exception);
        return R.fail(400, "图片不能超过 2MB");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public R<Void> handleMissingServletRequestPartException(MissingServletRequestPartException exception) {
        return R.fail(400, "请选择要上传的图片");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return R.fail(500, "系统繁忙，请稍后再试");
    }
}

package com.costcount.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final Integer code;

    public BizException(String message) { this(500, message); }
    public BizException(Integer code, String message) { super(message); this.code = code; }
}

package com.guolian.pm.common.exception;

import lombok.Getter;

/**
 * 类型化业务异常。code 直接映射到统一响应体的 code。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ErrorCode.BIZ_ERROR.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException badRequest(String message) {
        return new BizException(ErrorCode.BAD_REQUEST.getCode(), message);
    }

    public static BizException forbidden(String message) {
        return new BizException(ErrorCode.FORBIDDEN.getCode(), message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(ErrorCode.UNAUTHORIZED.getCode(), message);
    }

    public static BizException notFound(String message) {
        return new BizException(ErrorCode.NOT_FOUND.getCode(), message);
    }

    public static BizException biz(String message) {
        return new BizException(ErrorCode.BIZ_ERROR.getCode(), message);
    }
}

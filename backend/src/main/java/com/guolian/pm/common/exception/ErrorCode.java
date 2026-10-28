package com.guolian.pm.common.exception;

import lombok.Getter;

/**
 * 业务错误码。客户端只看到 code + msg，绝不返回堆栈与内部细节。
 */
@Getter
public enum ErrorCode {

    OK(0, "ok"),
    BAD_REQUEST(400, "请求参数不合法"),
    UNAUTHORIZED(401, "登录已失效，请重新登录"),
    FORBIDDEN(403, "没有权限执行该操作"),
    NOT_FOUND(404, "资源不存在"),
    BIZ_ERROR(4001, "业务处理失败"),
    SERVER_ERROR(500, "服务器开小差了，请稍后重试");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}

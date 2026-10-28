package com.guolian.pm.common.web;

import com.guolian.pm.common.exception.BizException;

/**
 * 请求级登录上下文（ThreadLocal），拦截器入口写入，出口清理。
 */
public final class AuthContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BizException.unauthorized("登录已失效，请重新登录");
        }
        return user;
    }

    public static Long userId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    public static Long roleId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getRoleId();
    }

    public static boolean isImpersonate() {
        LoginUser u = HOLDER.get();
        return u != null && u.isImpersonate();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

package com.guolian.pm.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 功能权限校验：标注在控制器方法上，无权限返回 403。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Perm {

    /** 权限标识，如 system:user:add；多个时满足任意一个即可 */
    String[] value();
}

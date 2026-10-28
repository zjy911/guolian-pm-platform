package com.guolian.pm.common.scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要行级数据权限的查询。
 * 业务模块新增自己的业务表时，在 DataScopeRegistry 注册「表名 → 归属人字段」即可自动生效。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /** 业务资源标识，如 user / transfer / project */
    String resource();
}

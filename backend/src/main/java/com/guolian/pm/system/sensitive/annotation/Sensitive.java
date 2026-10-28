package com.guolian.pm.system.sensitive.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.guolian.pm.system.sensitive.serializer.SensitiveSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段脱敏标记。算法不硬编码在字段上，由字段组 + 当前角色档位在序列化时决定。
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerializer.class)
public @interface Sensitive {

    /** 字段组编码，如 MOBILE / ID_CARD / BANK_ACCOUNT */
    String group();

    /** 业务对象，如 user / company */
    String bizObject() default "user";
}

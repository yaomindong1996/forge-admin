package com.mdframe.forge.starter.crypto.desensitize.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mdframe.forge.starter.crypto.desensitize.serializer.DesensitizeSerializer;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeType;

import java.lang.annotation.*;

/**
 * 字段脱敏注解
 * <p>
 * 标注在实体类字段上，该字段在序列化时进行脱敏处理
 *
 * @author forge
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = DesensitizeSerializer.class)
public @interface Desensitize {

    /**
     * 脱敏类型
     */
    DesensitizeType type() default DesensitizeType.CUSTOM;

    /**
     * 前置保留长度（当type为CUSTOM时生效）
     */
    int prefixKeep() default 0;

    /**
     * 后置保留长度（当type为CUSTOM时生效）
     */
    int suffixKeep() default 0;

    /**
     * 替换字符（当type为CUSTOM时生效）
     */
    char replaceChar() default '*';

    /**
     * 是否启用脱敏
     */
    boolean enabled() default true;
}

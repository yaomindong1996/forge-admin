package com.mdframe.forge.starter.trans.annotation;

import com.mdframe.forge.starter.trans.handler.TransHandler;

import java.lang.annotation.*;

/**
 * 字段级翻译注解
 * 标记在需要翻译的字段上
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TransField {

    /**
     * 字典类型（如：user_status）
     */
    String dictType() default "";

    /**
     * 自定义目标字段名
     * 默认：原字段名 + "Name"
     */
    String target() default "";

    /**
     * 使用的处理器类型
     */
    Class<? extends TransHandler> handler() default TransHandler.class;

    /**
     * 枚举处理器使用的枚举类
     */
    Class<? extends Enum<?>> enumClass() default DefaultEnum.class;

    /**
     * 表达式处理器使用的表达式
     * 示例："1:正常,2:失败"
     */
    String expression() default "";

    /**
     * 是否在ORM层自动翻译
     */
    boolean orm() default false;

    /**
     * 内部占位默认枚举类，避免null
     */
    enum DefaultEnum {
    }
}

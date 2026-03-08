package com.mdframe.forge.starter.trans.annotation;

import java.lang.annotation.*;

/**
 * 方法级字典翻译启用注解
 * 标注在方法上，自动对返回值执行字典翻译
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DictTranslate {
    /**
     * 是否启用当前方法返回值的翻译
     */
    boolean enabled() default true;
}

package com.mdframe.forge.starter.trans.annotation;

import com.mdframe.forge.starter.trans.handler.TransHandler;

import java.lang.annotation.*;

/**
 * 主注解：标记在实体类上，启用字典翻译能力
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DictTrans {

    /**
     * 是否启用当前类的翻译
     */
    boolean enabled() default true;
}

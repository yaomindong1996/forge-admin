package com.mdframe.forge.starter.core.annotation.tenant;

import java.lang.annotation.*;

/**
 * 忽略租户注解
 * 用于标记不需要租户隔离的表或方法
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreTenant {
    
    /**
     * 是否忽略租户
     */
    boolean value() default true;
}

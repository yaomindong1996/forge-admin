package com.mdframe.forge.starter.core.annotation.crypto;

import java.lang.annotation.*;

/**
 * API响应加密注解
 * <p>
 * 标注在Controller方法或类上，响应数据将被加密后返回
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiEncrypt {

    /**
     * 加密算法，默认使用配置文件中的算法
     */
    String algorithm() default "";
}

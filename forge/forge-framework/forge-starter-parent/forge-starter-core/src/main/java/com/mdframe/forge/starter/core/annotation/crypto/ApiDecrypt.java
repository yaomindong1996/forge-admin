package com.mdframe.forge.starter.core.annotation.crypto;

import java.lang.annotation.*;

/**
 * API请求解密注解
 * <p>
 * 标注在Controller方法或类上，请求数据将被解密后处理
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiDecrypt {

    /**
     * 解密算法，默认使用配置文件中的算法
     */
    String algorithm() default "";
}

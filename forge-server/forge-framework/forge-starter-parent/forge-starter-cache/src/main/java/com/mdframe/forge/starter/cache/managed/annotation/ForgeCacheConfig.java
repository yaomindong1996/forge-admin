package com.mdframe.forge.starter.cache.managed.annotation;

import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import com.mdframe.forge.starter.cache.managed.enums.CacheScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明一个命名缓存的代码默认策略和不可变安全边界。
 */
@Inherited
@Repeatable(ForgeCacheConfigs.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ForgeCacheConfig {

    String name();

    String description() default "";

    CacheMode mode() default CacheMode.REDIS;

    CacheMode[] allowedModes() default {CacheMode.LOCAL, CacheMode.REDIS, CacheMode.MULTI};

    CacheScope scope() default CacheScope.TENANT;

    long localTtlSeconds() default 60;

    long redisTtlSeconds() default 1800;

    int localMaxSize() default 1000;

    boolean cacheNull() default false;

    long nullTtlSeconds() default 30;
}

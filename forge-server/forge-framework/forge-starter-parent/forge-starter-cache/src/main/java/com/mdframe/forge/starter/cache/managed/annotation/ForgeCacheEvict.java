package com.mdframe.forge.starter.cache.managed.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ForgeCacheEvict {

    String cacheName();

    String key() default "";

    boolean allEntries() default false;
}

package com.mdframe.forge.starter.idempotent.annotation;

import com.mdframe.forge.starter.idempotent.constant.IdempotentConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    String prefix() default IdempotentConstant.DEFAULT_PREFIX;
    int expire() default IdempotentConstant.DEFAULT_EXPIRE;
    String key() default "";
    String message() default IdempotentConstant.DEFAULT_MESSAGE;
    boolean deleteKeyAfterSuccess() default false;
}

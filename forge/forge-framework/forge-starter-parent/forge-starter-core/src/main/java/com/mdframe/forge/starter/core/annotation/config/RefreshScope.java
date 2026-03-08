package com.mdframe.forge.starter.core.annotation.config;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.lang.annotation.*;

/**
 * 标记Bean支持配置动态刷新
 * 类似于Nacos的@RefreshScope
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Scope(scopeName = "refresh", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Documented
public @interface RefreshScope {
}

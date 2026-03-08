package com.mdframe.forge.starter.job.annotation;

import java.lang.annotation.*;

/**
 * 标记定时任务处理器
 * 应用启动时自动扫描注册到任务注册中心
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JobHandler {
    
    /**
     * 任务名称（唯一标识）
     */
    String value();
    
    /**
     * 任务描述
     */
    String description() default "";
    
    /**
     * 任务分组
     */
    String group() default "DEFAULT";
}

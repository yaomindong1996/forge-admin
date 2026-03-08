package com.mdframe.forge.starter.job.annotation;

import java.lang.annotation.*;

/**
 * Spring Bean直连模式的定时任务注解
 * 自动注册到Quartz调度器
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScheduledJob {
    
    /**
     * Cron表达式
     */
    String cron();
    
    /**
     * 任务名称
     */
    String name() default "";
    
    /**
     * 任务分组
     */
    String group() default "DEFAULT";
    
    /**
     * 任务描述
     */
    String description() default "";
    
    /**
     * 是否启用
     */
    boolean enabled() default true;
}

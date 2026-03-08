package com.mdframe.forge.starter.core.annotation.log;


import com.mdframe.forge.starter.core.domain.OperationType;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标注在Controller方法上，自动记录操作日志
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    
    /**
     * 操作模块
     */
    String module() default "";
    
    /**
     * 操作类型
     */
    OperationType type() default OperationType.OTHER;
    
    /**
     * 操作描述
     */
    String desc() default "";
    
    /**
     * 是否保存请求参数
     */
    boolean saveRequestParams() default true;
    
    /**
     * 是否保存响应结果
     */
    boolean saveResponseResult() default true;
}

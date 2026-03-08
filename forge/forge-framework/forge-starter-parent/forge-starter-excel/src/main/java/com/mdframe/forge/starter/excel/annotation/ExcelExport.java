package com.mdframe.forge.starter.excel.annotation;

import java.lang.annotation.*;

/**
 * 标记实体类支持Excel导出
 * 配置类级别的导出属性
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelExport {

    /**
     * Sheet名称
     */
    String sheetName() default "Sheet1";

    /**
     * 是否自动翻译字典
     */
    boolean autoTrans() default true;

    /**
     * 导出前是否自动过滤null值
     */
    boolean filterNull() default false;
}

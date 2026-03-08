package com.mdframe.forge.starter.excel.annotation;

import java.lang.annotation.*;

/**
 * Excel导出字段配置注解
 * 标注在实体字段上，配置导出行为
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelColumn {

    /**
     * 列名（表头）
     */
    String value() default "";

    /**
     * 列宽度
     */
    int width() default 20;

    /**
     * 排序（值越小越靠前）
     */
    int order() default Integer.MAX_VALUE;

    /**
     * 是否导出
     */
    boolean export() default true;

    /**
     * 日期格式化
     */
    String dateFormat() default "yyyy-MM-dd HH:mm:ss";

    /**
     * 数字格式化
     */
    String numberFormat() default "";

    /**
     * 字典类型（集成翻译字典功能）
     */
    String dictType() default "";

    /**
     * 是否需要字典翻译（自动从 @TransField 推断）
     */
    boolean needTrans() default false;
}

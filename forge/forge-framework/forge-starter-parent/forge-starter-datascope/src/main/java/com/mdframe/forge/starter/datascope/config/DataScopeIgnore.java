package com.mdframe.forge.starter.datascope.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解
 * 用于控制数据访问范围，基于行政区划和用户级别的权限过滤
 *
 * 权限规则：
 * - 自治区级别（level=1）：不控制权限
 * - 盟市级别（level=2）：只控制行政区划
 * - 旗区县级别（level=3）：既控制行政区划，又控制用户级权限
 *
 * @author legal-aid
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScopeIgnore {
}

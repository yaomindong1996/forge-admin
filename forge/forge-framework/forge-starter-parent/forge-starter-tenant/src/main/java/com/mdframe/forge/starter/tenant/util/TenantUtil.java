package com.mdframe.forge.starter.tenant.util;

import com.mdframe.forge.starter.tenant.context.TenantContextHolder;

import java.util.function.Supplier;

/**
 * 租户工具类
 * 提供便捷的租户操作方法
 */
public class TenantUtil {

    private TenantUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID
     */
    public static Long getTenantId() {
        return TenantContextHolder.getTenantId();
    }

    /**
     * 设置租户ID
     *
     * @param tenantId 租户ID
     */
    public static void setTenantId(Long tenantId) {
        TenantContextHolder.setTenantId(tenantId);
    }

    /**
     * 清除租户ID
     */
    public static void clear() {
        TenantContextHolder.clear();
    }

    /**
     * 判断是否有租户ID
     *
     * @return 是否有租户ID
     */
    public static boolean hasTenantId() {
        return TenantContextHolder.getTenantId() != null;
    }

    /**
     * 忽略租户执行操作
     *
     * @param runnable 操作
     */
    public static void ignore(Runnable runnable) {
        TenantContextHolder.executeIgnore(runnable);
    }

    /**
     * 忽略租户执行操作并返回结果
     *
     * @param supplier 操作
     * @param <T>      返回类型
     * @return 操作结果
     */
    public static <T> T ignore(Supplier<T> supplier) {
        return TenantContextHolder.executeIgnore(supplier);
    }

    /**
     * 使用指定租户执行操作
     *
     * @param tenantId 租户ID
     * @param runnable 操作
     */
    public static void with(Long tenantId, Runnable runnable) {
        TenantContextHolder.executeWithTenant(tenantId, runnable);
    }

    /**
     * 使用指定租户执行操作并返回结果
     *
     * @param tenantId 租户ID
     * @param supplier 操作
     * @param <T>      返回类型
     * @return 操作结果
     */
    public static <T> T with(Long tenantId, Supplier<T> supplier) {
        return TenantContextHolder.executeWithTenant(tenantId, supplier);
    }

    /**
     * 判断是否忽略租户
     *
     * @return 是否忽略
     */
    public static boolean isIgnore() {
        return TenantContextHolder.isIgnore();
    }

    /**
     * 设置忽略租户
     *
     * @param ignore 是否忽略
     */
    public static void setIgnore(boolean ignore) {
        TenantContextHolder.setIgnore(ignore);
    }
}

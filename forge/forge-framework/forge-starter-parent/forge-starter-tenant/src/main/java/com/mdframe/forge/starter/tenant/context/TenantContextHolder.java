package com.mdframe.forge.starter.tenant.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 租户上下文持有者
 * 使用TransmittableThreadLocal支持异步线程传递
 */
public class TenantContextHolder {

    /**
     * 使用阿里的TransmittableThreadLocal，支持线程池场景下的上下文传递
     */
    private static final ThreadLocal<Long> TENANT_ID_HOLDER = new TransmittableThreadLocal<>();

    /**
     * 是否忽略租户（用于特殊场景，如系统级操作）
     */
    private static final ThreadLocal<Boolean> IGNORE_TENANT = new TransmittableThreadLocal<>();

    /**
     * 设置租户ID
     *
     * @param tenantId 租户ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    /**
     * 获取租户ID
     *
     * @return 租户ID
     */
    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    /**
     * 清除租户ID
     */
    public static void clear() {
        TENANT_ID_HOLDER.remove();
        IGNORE_TENANT.remove();
    }

    /**
     * 设置忽略租户
     *
     * @param ignore 是否忽略
     */
    public static void setIgnore(Boolean ignore) {
        IGNORE_TENANT.set(ignore);
    }

    /**
     * 是否忽略租户
     *
     * @return 是否忽略
     */
    public static Boolean isIgnore() {
        return IGNORE_TENANT.get() != null && IGNORE_TENANT.get();
    }

    /**
     * 执行忽略租户的操作
     *
     * @param runnable 操作
     */
    public static void executeIgnore(Runnable runnable) {
        Boolean oldIgnore = IGNORE_TENANT.get();
        try {
            IGNORE_TENANT.set(true);
            runnable.run();
        } finally {
            if (oldIgnore != null) {
                IGNORE_TENANT.set(oldIgnore);
            } else {
                IGNORE_TENANT.remove();
            }
        }
    }

    /**
     * 执行忽略租户的操作并返回结果
     *
     * @param supplier 操作
     * @param <T>      返回类型
     * @return 操作结果
     */
    public static <T> T executeIgnore(java.util.function.Supplier<T> supplier) {
        Boolean oldIgnore = IGNORE_TENANT.get();
        try {
            IGNORE_TENANT.set(true);
            return supplier.get();
        } finally {
            if (oldIgnore != null) {
                IGNORE_TENANT.set(oldIgnore);
            } else {
                IGNORE_TENANT.remove();
            }
        }
    }

    /**
     * 执行指定租户的操作
     *
     * @param tenantId 租户ID
     * @param runnable 操作
     */
    public static void executeWithTenant(Long tenantId, Runnable runnable) {
        Long oldTenantId = TENANT_ID_HOLDER.get();
        try {
            TENANT_ID_HOLDER.set(tenantId);
            runnable.run();
        } finally {
            if (oldTenantId != null) {
                TENANT_ID_HOLDER.set(oldTenantId);
            } else {
                TENANT_ID_HOLDER.remove();
            }
        }
    }

    /**
     * 执行指定租户的操作并返回结果
     *
     * @param tenantId 租户ID
     * @param supplier 操作
     * @param <T>      返回类型
     * @return 操作结果
     */
    public static <T> T executeWithTenant(Long tenantId, java.util.function.Supplier<T> supplier) {
        Long oldTenantId = TENANT_ID_HOLDER.get();
        try {
            TENANT_ID_HOLDER.set(tenantId);
            return supplier.get();
        } finally {
            if (oldTenantId != null) {
                TENANT_ID_HOLDER.set(oldTenantId);
            } else {
                TENANT_ID_HOLDER.remove();
            }
        }
    }
}

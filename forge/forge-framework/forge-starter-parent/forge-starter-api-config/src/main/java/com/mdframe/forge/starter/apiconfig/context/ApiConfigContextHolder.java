package com.mdframe.forge.starter.apiconfig.context;

import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigInfo;

/**
 * API配置上下文持有者
 * 使用ThreadLocal存储当前请求的API配置
 */
public class ApiConfigContextHolder {

    private static final ThreadLocal<ApiConfigInfo> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前API配置
     */
    public static void setConfig(ApiConfigInfo config) {
        CONTEXT_HOLDER.set(config);
    }

    /**
     * 获取当前API配置
     */
    public static ApiConfigInfo getConfig() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 判断是否需要鉴权
     */
    public static boolean needAuth() {
        ApiConfigInfo config = CONTEXT_HOLDER.get();
        return config != null && config.getNeedAuth();
    }

    /**
     * 判断是否需要加密
     */
    public static boolean needEncrypt() {
        ApiConfigInfo config = CONTEXT_HOLDER.get();
        return config != null && config.getNeedEncrypt();
    }

    /**
     * 判断是否需要租户隔离
     */
    public static boolean needTenant() {
        ApiConfigInfo config = CONTEXT_HOLDER.get();
        return config != null && config.getNeedTenant();
    }

    /**
     * 判断是否需要限流
     */
    public static boolean needLimit() {
        ApiConfigInfo config = CONTEXT_HOLDER.get();
        return config != null && config.getNeedLimit();
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}

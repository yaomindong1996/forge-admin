package com.mdframe.forge.starter.apiconfig.domain.event;

import org.springframework.context.ApplicationEvent;

/**
 * API配置刷新事件
 * 用于在配置变更时通知所有节点刷新缓存
 */
public class ApiConfigRefreshEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 刷新类型
     */
    private final RefreshType refreshType;

    /**
     * 接口请求路径
     */
    private final String urlPath;

    /**
     * 请求方式
     */
    private final String method;

    /**
     * 配置ID（单条刷新时使用）
     */
    private final Long configId;

    /**
     * 刷新原因
     */
    private final String reason;

    public ApiConfigRefreshEvent(Object source, RefreshType refreshType) {
        super(source);
        this.refreshType = refreshType;
        this.urlPath = null;
        this.method = null;
        this.configId = null;
        this.reason = "手动刷新全部缓存";
    }

    public ApiConfigRefreshEvent(Object source, RefreshType refreshType, String urlPath, String method) {
        super(source);
        this.refreshType = refreshType;
        this.urlPath = urlPath;
        this.method = method;
        this.configId = null;
        this.reason = "刷新单个接口缓存: " + urlPath + ":" + method;
    }

    public ApiConfigRefreshEvent(Object source, RefreshType refreshType, Long configId, String reason) {
        super(source);
        this.refreshType = refreshType;
        this.urlPath = null;
        this.method = null;
        this.configId = configId;
        this.reason = reason;
    }

    public RefreshType getRefreshType() {
        return refreshType;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public String getMethod() {
        return method;
    }

    public Long getConfigId() {
        return configId;
    }

    public String getReason() {
        return reason;
    }

    /**
     * 刷新类型枚举
     */
    public enum RefreshType {
        /**
         * 刷新单个接口配置
         */
        SINGLE,

        /**
         * 刷新所有接口配置
         */
        ALL,

        /**
         * 刷新指定模块的配置
         */
        MODULE
    }
}

package com.mdframe.forge.starter.apiconfig.service;

import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigInfo;
import com.mdframe.forge.starter.apiconfig.domain.entity.SysApiConfig;

import java.util.List;

/**
 * API配置管理器接口（核心决策引擎 + 缓存管理）
 * 优先级：数据库配置 > 注解配置 > 系统默认值
 */
public interface IApiConfigManager {

    /**
     * 根据请求路径和方法获取接口配置
     * 优先级：数据库配置 > 注解配置 > 默认值
     *
     * @param urlPath 请求路径
     * @param method  请求方式
     * @return 配置信息
     */
    ApiConfigInfo getApiConfig(String urlPath, String method);

    /**
     * 根据接口编码获取接口配置
     *
     * @param apiCode 接口编码
     * @return 配置信息
     */
    ApiConfigInfo getApiConfigByCode(String apiCode);

    /**
     * 刷新指定接口配置缓存
     *
     * @param urlPath 请求路径
     * @param method  请求方式
     */
    void refreshApiConfig(String urlPath, String method);

    /**
     * 刷新指定接口配置缓存（根据ID）
     *
     * @param configId 配置ID
     */
    void refreshApiConfigById(Long configId);

    /**
     * 刷新所有接口配置缓存
     */
    void refreshAllApiConfig();

    /**
     * 刷新指定模块的配置缓存
     *
     * @param moduleCode 模块编码
     */
    void refreshApiConfigByModule(String moduleCode);

    /**
     * 判断接口是否需要鉴权
     *
     * @param urlPath 请求路径
     * @param method  请求方式
     * @return true-需要鉴权, false-不需要
     */
    boolean needAuth(String urlPath, String method);

    /**
     * 判断接口是否需要加密
     *
     * @param urlPath 请求路径
     * @param method  请求方式
     * @return true-需要加密, false-不需要
     */
    boolean needEncrypt(String urlPath, String method);

    /**
     * 判断接口是否需要租户隔离
     *
     * @param urlPath 请求路径
     * @param method  请求方式
     * @return true-需要租户隔离, false-不需要
     */
    boolean needTenant(String urlPath, String method);

    /**
     * 判断接口是否需要限流
     *
     * @param urlPath 请求路径
     * @param method  请求方式
     * @return true-需要限流, false-不需要
     */
    boolean needLimit(String urlPath, String method);

    /**
     * 获取所有启用的配置
     *
     * @return 配置列表
     */
    List<ApiConfigInfo> getAllEnabledConfigs();

    /**
     * 预热缓存（启动时调用）
     */
    void warmUpCache();

    /**
     * 清空所有缓存
     */
    void clearAllCache();

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    String getCacheStats();
}

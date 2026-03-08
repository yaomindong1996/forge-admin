package com.mdframe.forge.starter.auth.strategy;

import com.mdframe.forge.starter.auth.domain.LoginRequest;
import com.mdframe.forge.starter.core.session.LoginUser;

/**
 * 认证策略接口
 * 所有认证方式都需要实现此接口
 */
public interface IAuthStrategy {

    /**
     * 执行认证
     *
     * @param request 登录请求
     * @return 登录用户信息（包含角色、权限）
     */
    LoginUser authenticate(LoginRequest request);

    /**
     * 获取策略支持的认证类型
     *
     * @return 认证类型编码
     */
    String getAuthType();

    /**
     * 获取策略支持的客户端类型（可选）
     * 返回null表示支持所有客户端
     *
     * @return 客户端类型，如：pc, app, h5, wechat
     */
    default String getSupportedClient() {
        return null;
    }

    /**
     * 判断是否支持该认证请求
     *
     * @param request 登录请求
     * @return 是否支持
     */
    default boolean supports(LoginRequest request) {
        if (!getAuthType().equals(request.getAuthType())) {
            return false;
        }
        
        // 如果策略指定了客户端类型，需要匹配
        if (getSupportedClient() != null && request.getUserClient() != null) {
            return getSupportedClient().equals(request.getUserClient());
        }
        
        return true;
    }
}

package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.flow.client.FlowTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * 基于 Sa-Token 的流程客户端 Token 供应商
 * <p>
 * 在引入 {@code forge-starter-auth} 的服务中自动注册。
 * 每次 {@link com.mdframe.forge.flow.client.FlowClient} 发起 HTTP 请求前，
 * 自动从当前请求上下文中取出 Sa-Token，透传到 Authorization 请求头，
 * 解决服务内调用流程服务时出现"未登录"的问题。
 *
 * @author forge
 */
@Slf4j
@Component
@ConditionalOnClass(name = "com.mdframe.forge.flow.client.FlowClient")
public class SaTokenFlowTokenProvider implements FlowTokenProvider {

    @Override
    public String getToken() {
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getTokenValue();
            }
        } catch (Exception e) {
            log.debug("[FlowTokenProvider] 获取 Sa-Token 失败（当前线程无登录上下文）: {}", e.getMessage());
        }
        return null;
    }
}

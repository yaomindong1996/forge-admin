package com.mdframe.forge.flow.client;

/**
 * 流程客户端 Token 供应商接口
 * <p>
 * 用于在服务间调用时动态提供当前用户的认证 Token，
 * 实现 Token 透传，避免跨服务调用时出现未登录异常。
 *
 * <h3>默认实现</h3>
 * <ul>
 *   <li>引入 {@code forge-starter-auth} 时，框架会自动注册基于 Sa-Token 的实现
 *       {@code SaTokenFlowTokenProvider}，无需手动配置</li>
 *   <li>也可自定义实现此接口并注册为 Spring Bean 覆盖默认行为</li>
 * </ul>
 *
 * <h3>优先级（从高到低）</h3>
 * <ol>
 *   <li>此接口返回的动态 Token（当前请求用户 Token）</li>
 *   <li>{@code forge.flow.client.token} 配置的静态 Token</li>
 * </ol>
 *
 * @author forge
 */
public interface FlowTokenProvider {

    /**
     * 获取当前上下文的 Token 值（不含 Bearer 前缀）
     *
     * @return token 字符串，无法获取时返回 null
     */
    String getToken();
}

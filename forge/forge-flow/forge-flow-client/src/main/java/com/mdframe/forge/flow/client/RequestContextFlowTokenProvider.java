package com.mdframe.forge.flow.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 基于 Spring RequestContext 的默认 Token 透传实现
 * <p>
 * 从当前 HTTP 请求的 {@code Authorization} 头中读取 Token，
 * 自动用于 {@link FlowClient} 调用流程服务时的鉴权透传。
 *
 * <h3>生效条件</h3>
 * <ul>
 *   <li>类路径中存在 {@code spring-web}（包含 RequestContextHolder）</li>
 *   <li>容器中不存在其他 {@link FlowTokenProvider} 实现（如 forge-starter-auth 的 SaTokenFlowTokenProvider）</li>
 * </ul>
 *
 * <h3>非 Web 场景</h3>
 * 在定时任务、消息消费等没有 HTTP 上下文的场景，{@link RequestContextHolder#getRequestAttributes()}
 * 返回 null，此时安全返回 null，{@link FlowClient} 会降级使用配置文件中的静态 token。
 *
 * @author forge
 * @see FlowTokenProvider
 * @see FlowClientAutoConfiguration
 */
@Slf4j
public class RequestContextFlowTokenProvider implements FlowTokenProvider {

    @Override
    public String getToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization == null || authorization.isBlank()) {
                return null;
            }
            // 去掉 "Bearer " 前缀，FlowClient.buildHeaders() 会重新加上
            if (authorization.startsWith("Bearer ")) {
                return authorization.substring(7);
            }
            return authorization;
        } catch (Exception e) {
            log.debug("[FlowTokenProvider] 获取请求 Token 失败，降级为静态 token: {}", e.getMessage());
            return null;
        }
    }
}

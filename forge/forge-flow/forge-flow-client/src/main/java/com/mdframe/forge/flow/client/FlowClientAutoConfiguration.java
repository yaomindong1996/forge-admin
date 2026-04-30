package com.mdframe.forge.flow.client;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 流程客户端 Spring Boot 自动配置
 * <p>
 * 引入 forge-flow-client 依赖后自动生效，无需手动 {@code @Bean}。
 * 如需自定义，可自行声明 {@link FlowClient} Bean 覆盖此处默认实现。
 *
 * @author forge
 */
@AutoConfiguration
@EnableConfigurationProperties(FlowClientProperties.class)
public class FlowClientAutoConfiguration {

    /**
     * 默认 Token 透传实现：从当前 HTTP 请求头读取 Authorization
     * <p>
     * 当引入了 forge-starter-auth 时，该模块会注册更高级的 SaTokenFlowTokenProvider，
     * 此默认实现因 {@code @ConditionalOnMissingBean} 而不会生效，无需担心冲突。
     * 在非 Web 上下文（定时任务等）中获取失败时安全返回 null，降级使用静态 token 配置。
     */
    @Bean
    @ConditionalOnMissingBean(FlowTokenProvider.class)
    @ConditionalOnClass(name = "org.springframework.web.context.request.RequestContextHolder")
    public FlowTokenProvider requestContextFlowTokenProvider() {
        return new RequestContextFlowTokenProvider();
    }

    /**
     * 注册 RestTemplate（带超时配置）
     * <p>
     * 仅在容器中不存在名为 {@code flowRestTemplate} 的 Bean 时生效。
     */
    @Bean("flowRestTemplate")
    @ConditionalOnMissingBean(name = "flowRestTemplate")
    public RestTemplate flowRestTemplate(FlowClientProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeout());
        factory.setReadTimeout(properties.getReadTimeout());
        return new RestTemplate(factory);
    }

    /**
     * 注册 FlowClient
     * <p>
     * 如果容器中存在 {@link FlowTokenProvider} Bean（如引入 forge-starter-auth 自动注入的
     * {@code SaTokenFlowTokenProvider}），则自动启用动态 Token 透传机制。
     */
    @Bean
    @ConditionalOnMissingBean
    public FlowClient flowClient(FlowClientProperties properties,
                                 RestTemplate flowRestTemplate,
                                 ObjectProvider<FlowTokenProvider> tokenProviderObjectProvider) {
        FlowClient flowClient = new FlowClient(flowRestTemplate, properties.getUrl(), properties.getToken());
        // 自动注入 TokenProvider（存在时）
        FlowTokenProvider tokenProvider = tokenProviderObjectProvider.getIfAvailable();
        if (tokenProvider != null) {
            flowClient.setTokenProvider(tokenProvider);
        }
        return flowClient;
    }
}

package com.mdframe.forge.flow.client;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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

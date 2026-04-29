package com.mdframe.forge.flow.client.helper;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowClientProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * FlowHelper SDK 客户端自动配置
 * <p>
 * 注册 {@link FlowStartAspect} 和 {@link FlowEventSubscriber}。
 * 如果类路径中存在 Redis，则同时注册 Redis 事件监听器（自动订阅 flow:event:all 频道）。
 *
 * @author forge
 */
@Configuration
public class FlowClientHelperAutoConfiguration {

    // ==================== @FlowStart 切面（必须有 AspectJ） ====================

    @Bean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnBean(FlowClient.class)
    public FlowStartAspect flowStartAspect(FlowClient flowClient,
                                           ApplicationContext applicationContext) {
        return new FlowStartAspect(flowClient, applicationContext);
    }

    // ==================== 事件分发器 ====================

    @Bean
    public FlowEventSubscriber flowEventSubscriber(ApplicationContext applicationContext) {
        return new FlowEventSubscriber(applicationContext);
    }

    // ==================== Redis 事件订阅（可选，引入 spring-data-redis 时自动生效） ====================

    /**
     * Redis 消息监听容器，订阅 flow-server 发布的所有流程事件频道
     * <p>
     * 订阅两个频道：
     * <ul>
     *   <li>{@code flow:event:all} — 全量事件</li>
     *   <li>{@code flow:event:*}  — 精准频道（按 processDefKey 过滤）</li>
     * </ul>
     * 仅在以下条件同时满足时生效：
     * <ol>
     *   <li>类路径中存在 {@code spring-data-redis}</li>
     *   <li>容器中存在 {@code RedisConnectionFactory} Bean</li>
     *   <li>配置项 {@code forge.flow.client.redis-subscribe=true}（默认 true）</li>
     * </ol>
     */
    @Bean("flowRedisListenerContainer")
    @ConditionalOnClass(name = "org.springframework.data.redis.connection.RedisConnectionFactory")
    @ConditionalOnProperty(prefix = "forge.flow.client", name = "redis-subscribe", havingValue = "true", matchIfMissing = true)
    public RedisMessageListenerContainer flowRedisListenerContainer(
            RedisConnectionFactory factory,
            FlowEventSubscriber subscriber) {

        // 适配器：将 String 消息路由到 FlowEventSubscriber.onMessage
        // 必须设置 StringRedisSerializer，否则 Redis 原始字节会带序列化头导致 JSON 解析失败
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "onMessage");
        adapter.setSerializer(new StringRedisSerializer());
        adapter.afterPropertiesSet();

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        // 订阅全量频道
        container.addMessageListener(adapter, new PatternTopic("flow:event:all"));
        // 订阅精准频道（按 processDefKey）
        container.addMessageListener(adapter, new PatternTopic("flow:event:*"));
        return container;
    }
}

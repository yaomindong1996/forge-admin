package com.mdframe.forge.starter.flow.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 流程事件 Redis Pub/Sub 发布器
 *
 * <p>将流程事件异步发布到 Redis 频道，频道命名规则：</p>
 * <pre>
 *   flow:event:{processDefKey}     — 按流程 Key 订阅（精准订阅某类流程的事件）
 *   flow:event:*                   — 通配符订阅所有流程事件（Redis PSUBSCRIBE）
 * </pre>
 *
 * <p>只有当 Spring Data Redis 存在于类路径时才会生效（optional 依赖），
 * 若未引入 Redis 依赖，此 Bean 不会被注册。</p>
 *
 * <h3>业务侧消费示例（Spring Boot 业务服务）</h3>
 * <pre>
 * {@literal @}Configuration
 * public class FlowEventConfig {
 *     {@literal @}Bean
 *     public MessageListenerAdapter flowEventListener(MyFlowEventHandler handler) {
 *         return new MessageListenerAdapter(handler, "onMessage");
 *     }
 *
 *     {@literal @}Bean
 *     public RedisMessageListenerContainer listenerContainer(
 *             RedisConnectionFactory factory,
 *             MessageListenerAdapter flowEventListener) {
 *         RedisMessageListenerContainer container = new RedisMessageListenerContainer();
 *         container.setConnectionFactory(factory);
 *         // 精准订阅某个流程
 *         container.addMessageListener(flowEventListener,
 *             new ChannelTopic("flow:event:leave-apply"));
 *         // 或通配符订阅全部流程事件
 *         // container.addMessageListener(flowEventListener,
 *         //     new PatternTopic("flow:event:*"));
 *         return container;
 *     }
 * }
 * </pre>
 */
@Slf4j
@Component
@ConditionalOnClass(StringRedisTemplate.class)
public class FlowEventPublisher {

    /** Redis 频道前缀 */
    public static final String CHANNEL_PREFIX = "flow:event:";

    /** 全量事件频道（所有流程类型都会发布到此频道，配合 PSUBSCRIBE 使用）*/
    public static final String CHANNEL_ALL = "flow:event:all";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    public FlowEventPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        // 注册 Java 8 时间类型支持
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 异步发布流程事件到 Redis
     *
     * <p>同时发布到两个频道：</p>
     * <ol>
     *   <li>{@code flow:event:{processDefKey}} - 按流程 Key 细分的频道</li>
     *   <li>{@code flow:event:all}             - 全量事件汇聚频道</li>
     * </ol>
     *
     * @param message 流程事件消息
     */
    @Async("flowEventExecutor")
    public void publish(FlowEventMessage message) {
        if (redisTemplate == null) {
            log.warn("[FlowEvent] Redis 未配置，跳过事件发布: eventType={}, businessKey={}",
                    message.getEventType(), message.getBusinessKey());
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(message);

            // 发布到按流程 Key 的精准频道
            if (message.getProcessDefKey() != null) {
                String channel = CHANNEL_PREFIX + message.getProcessDefKey();
                redisTemplate.convertAndSend(channel, json);
                log.debug("[FlowEvent] 发布到频道 {}: eventType={}, businessKey={}",
                        channel, message.getEventType(), message.getBusinessKey());
            }

            // 发布到全量频道（方便监听所有流程事件）
            redisTemplate.convertAndSend(CHANNEL_ALL, json);
            log.info("[FlowEvent] Redis Pub/Sub 发布成功: eventType={}, processDefKey={}, businessKey={}",
                    message.getEventType(), message.getProcessDefKey(), message.getBusinessKey());

        } catch (JsonProcessingException e) {
            log.error("[FlowEvent] 序列化事件消息失败: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("[FlowEvent] Redis 发布事件失败: eventType={}, error={}",
                    message.getEventType(), e.getMessage(), e);
        }
    }
}

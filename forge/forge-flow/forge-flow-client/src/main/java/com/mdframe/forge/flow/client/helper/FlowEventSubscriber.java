package com.mdframe.forge.flow.client.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mdframe.forge.flow.client.annotation.FlowBind;
import com.mdframe.forge.flow.client.annotation.FlowCallback;
import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

/**
 * 流程事件分发器（业务侧）
 * <p>
 * 接收来自 flow-server 的事件消息（Redis 频道消息 or Webhook 请求），
 * 自动路由到当前 Spring 容器中持有 {@link FlowBind} 且 modelKey 匹配的 Bean 的
 * {@link FlowCallback} 方法。
 *
 * <h3>触发方式</h3>
 * <ul>
 *   <li><b>Redis Pub/Sub</b>：由 {@link FlowRedisSubscriber} 监听 {@code flow:event:all} 频道后调用</li>
 *   <li><b>Webhook</b>：由业务方在 Webhook 接收 Controller 中手动调用 {@link #dispatch(FlowEventContext)}</li>
 * </ul>
 *
 * @author forge
 */
@Slf4j
@RequiredArgsConstructor
public class FlowEventSubscriber {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    public FlowEventSubscriber(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * 接收 JSON 消息字符串并分发（Redis 监听器调用此方法）
     */
    public void onMessage(String jsonMessage) {
        try {
            FlowEventContext ctx = objectMapper.readValue(jsonMessage, FlowEventContext.class);
            dispatch(ctx);
        } catch (Exception e) {
            log.warn("[FlowCallback] 消息反序列化失败: {} | raw={}", e.getMessage(), jsonMessage);
        }
    }

    /**
     * 分发事件到匹配的 {@link FlowCallback} 方法
     *
     * @param ctx 流程事件上下文
     */
    public void dispatch(FlowEventContext ctx) {
        if (ctx == null || ctx.getProcessDefKey() == null) {
            return;
        }

        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(FlowBind.class);
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object bean = entry.getValue();
            // 获取目标类（处理 CGLIB/JDK 代理）
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            FlowBind bind = targetClass.getAnnotation(FlowBind.class);
            if (bind == null || !bind.modelKey().equals(ctx.getProcessDefKey())) {
                continue;
            }
            invokeCallbacks(bean, targetClass, ctx);
        }
    }

    private void invokeCallbacks(Object bean, Class<?> targetClass, FlowEventContext ctx) {
        for (Method method : targetClass.getDeclaredMethods()) {
            FlowCallback callback = method.getAnnotation(FlowCallback.class);
            if (callback == null) {
                continue;
            }

            // 检查事件类型是否命中
            boolean matched = Arrays.stream(callback.on()).anyMatch(e -> e.equals(ctx.getEvent()));
            if (!matched) {
                continue;
            }

            // 支持两种签名：(FlowEventContext) 或无参
            try {
                method.setAccessible(true);
                if (method.getParameterCount() == 0) {
                    method.invoke(bean);
                } else if (method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isAssignableFrom(FlowEventContext.class)) {
                    method.invoke(bean, ctx);
                } else {
                    log.warn("[FlowCallback] 方法签名不符合规范（须无参或接收 FlowEventContext）: {}.{}",
                            targetClass.getSimpleName(), method.getName());
                    continue;
                }
                log.debug("[FlowCallback] 回调成功 {}.{} event={} businessKey={}",
                        targetClass.getSimpleName(), method.getName(), ctx.getEvent(), ctx.getBusinessKey());
            } catch (Exception e) {
                log.error("[FlowCallback] 回调执行失败 {}.{}: {}",
                        targetClass.getSimpleName(), method.getName(), e.getMessage(), e);
            }
        }
    }
}

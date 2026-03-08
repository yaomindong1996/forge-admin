package com.mdframe.forge.starter.websocket.service.impl;

import com.mdframe.forge.starter.websocket.domain.WebSocketMessage;
import com.mdframe.forge.starter.websocket.service.IMessagePushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WebSocket消息推送服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePushServiceImpl implements IMessagePushService {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String USER_DESTINATION = "/queue/messages";
    private static final String TOPIC_DESTINATION = "/topic/";

    @Override
    public void pushToUser(Long userId, WebSocketMessage message) {
        pushToUser(userId.toString(), message);
    }

    @Override
    public void pushToUser(String userId, WebSocketMessage message) {
        try {
            // 确保时间戳存在
            if (message.getTimestamp() == null) {
                message.setTimestamp(System.currentTimeMillis());
            }

            messagingTemplate.convertAndSendToUser(
                    userId,
                    USER_DESTINATION,
                    message
            );

            log.debug("推送消息成功: userId={}, type={}, message={}", 
                     userId, message.getType(), message.getMessage());
        } catch (Exception e) {
            log.error("推送消息失败: userId={}, message={}", userId, message, e);
        }
    }

    @Override
    public void pushToUsers(List<Long> userIds, WebSocketMessage message) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (Long userId : userIds) {
            pushToUser(userId, message);
        }
    }

    @Override
    public void pushToAll(WebSocketMessage message) {
        try {
            // 确保时间戳存在
            if (message.getTimestamp() == null) {
                message.setTimestamp(System.currentTimeMillis());
            }

            // 广播到所有订阅了/topic/broadcast的客户端
            messagingTemplate.convertAndSend("/topic/broadcast", message);

            log.debug("广播消息成功: type={}, message={}", 
                     message.getType(), message.getMessage());
        } catch (Exception e) {
            log.error("广播消息失败: message={}", message, e);
        }
    }

    @Override
    public void pushToTopic(String topic, WebSocketMessage message) {
        try {
            // 确保时间戳存在
            if (message.getTimestamp() == null) {
                message.setTimestamp(System.currentTimeMillis());
            }

            String destination = TOPIC_DESTINATION + topic;
            messagingTemplate.convertAndSend(destination, message);

            log.debug("推送主题消息成功: topic={}, type={}, message={}", 
                     topic, message.getType(), message.getMessage());
        } catch (Exception e) {
            log.error("推送主题消息失败: topic={}, message={}", topic, message, e);
        }
    }

    @Async
    @Override
    public void pushToUserAsync(Long userId, WebSocketMessage message) {
        pushToUser(userId, message);
    }

    @Async
    @Override
    public void pushToUsersAsync(List<Long> userIds, WebSocketMessage message) {
        pushToUsers(userIds, message);
    }
}

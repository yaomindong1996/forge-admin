package com.mdframe.forge.starter.websocket.service;

import com.mdframe.forge.starter.websocket.domain.WebSocketMessage;

import java.util.List;

/**
 * WebSocket通用消息推送服务接口
 */
public interface IMessagePushService {

    /**
     * 推送消息给指定用户
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    void pushToUser(Long userId, WebSocketMessage message);

    /**
     * 推送消息给指定用户(字符串userId)
     *
     * @param userId  用户ID字符串
     * @param message 消息内容
     */
    void pushToUser(String userId, WebSocketMessage message);

    /**
     * 推送消息给多个用户
     *
     * @param userIds 用户ID列表
     * @param message 消息内容
     */
    void pushToUsers(List<Long> userIds, WebSocketMessage message);

    /**
     * 推送消息给所有在线用户(广播)
     *
     * @param message 消息内容
     */
    void pushToAll(WebSocketMessage message);

    /**
     * 推送消息到指定主题(订阅模式)
     *
     * @param topic   主题名称
     * @param message 消息内容
     */
    void pushToTopic(String topic, WebSocketMessage message);

    /**
     * 异步推送消息给指定用户
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    void pushToUserAsync(Long userId, WebSocketMessage message);

    /**
     * 异步推送消息给多个用户
     *
     * @param userIds 用户ID列表
     * @param message 消息内容
     */
    void pushToUsersAsync(List<Long> userIds, WebSocketMessage message);
}

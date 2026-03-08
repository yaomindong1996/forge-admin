package com.mdframe.forge.starter.websocket.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * WebSocket统一消息格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage implements Serializable {

    /**
     * 消息类型
     * 例如: auth.kickout, system.notice, task.progress等
     */
    private String type;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息数据(可以是任意对象)
     */
    private Object data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 消息级别: info, warning, error, success
     */
    private String level;

    /**
     * 是否需要确认
     */
    private Boolean requireConfirm;

    /**
     * 消息来源
     */
    private String source;

    /**
     * 快捷构建方法
     */
    public static WebSocketMessage info(String type, String message) {
        return builder()
                .type(type)
                .message(message)
                .level("info")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static WebSocketMessage warning(String type, String message) {
        return builder()
                .type(type)
                .message(message)
                .level("warning")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static WebSocketMessage error(String type, String message) {
        return builder()
                .type(type)
                .message(message)
                .level("error")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static WebSocketMessage success(String type, String message) {
        return builder()
                .type(type)
                .message(message)
                .level("success")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}

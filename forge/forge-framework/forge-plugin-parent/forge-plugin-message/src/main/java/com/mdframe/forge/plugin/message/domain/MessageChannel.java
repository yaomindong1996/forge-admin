package com.mdframe.forge.plugin.message.domain;

import lombok.Getter;

/**
 * 消息发送渠道枚举
 */
@Getter
public enum MessageChannel {
    
    /**
     * 站内信（WEB端）
     */
    WEB("WEB", "站内信"),
    
    /**
     * 短信
     */
    SMS("SMS", "短信"),
    
    /**
     * 邮件
     */
    EMAIL("EMAIL", "邮件"),
    
    /**
     * 推送通知
     */
    PUSH("PUSH", "推送通知");
    
    private final String code;
    private final String name;
    
    MessageChannel(String code, String name) {
        this.code = code;
        this.name = name;
    }
}

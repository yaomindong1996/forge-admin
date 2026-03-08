package com.mdframe.forge.plugin.message.domain;

import lombok.Getter;

/**
 * 消息类型枚举
 */
@Getter
public enum MessageType {
    
    /**
     * 系统消息
     */
    SYSTEM("SYSTEM", "系统消息"),
    
    /**
     * 短信
     */
    SMS("SMS", "短信"),
    
    /**
     * 邮件
     */
    EMAIL("EMAIL", "邮件"),
    
    /**
     * 自定义
     */
    CUSTOM("CUSTOM", "自定义");
    
    private final String code;
    private final String name;
    
    MessageType(String code, String name) {
        this.code = code;
        this.name = name;
    }
}

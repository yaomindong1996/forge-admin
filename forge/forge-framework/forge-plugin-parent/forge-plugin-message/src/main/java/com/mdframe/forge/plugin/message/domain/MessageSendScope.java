package com.mdframe.forge.plugin.message.domain;

import lombok.Getter;

/**
 * 消息发送范围枚举
 */
@Getter
public enum MessageSendScope {
    
    /**
     * 全员
     */
    ALL("ALL", "全员"),
    
    /**
     * 指定组织
     */
    ORG("ORG", "指定组织"),
    
    /**
     * 指定人员
     */
    USERS("USERS", "指定人员");
    
    private final String code;
    private final String name;
    
    MessageSendScope(String code, String name) {
        this.code = code;
        this.name = name;
    }
}

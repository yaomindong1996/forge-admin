package com.mdframe.forge.plugin.ai.session.enums;

/** 会话历史协议：0 正常、1 删除，不是通用 enabled 开关。 */
public enum AiChatSessionStatus {
    ACTIVE("0"), DELETED("1");

    private final String code;

    AiChatSessionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean matches(String value) {
        return code.equals(value);
    }
}

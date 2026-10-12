package com.mdframe.forge.starter.idempotent.enums;

/** Token 生命周期存储码；未知状态不能当成未消费。 */
public enum IdempotentTokenStatus {
    UNUSED("UNUSED"),
    CONSUMED("CONSUMED");

    private final String code;

    IdempotentTokenStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean matches(Object value) {
        return code.equals(value);
    }
}

package com.mdframe.forge.plugin.print.enums;

/**
 * 打印领域的持久化状态码，与 sys_print_* 字典一致。
 */
public enum PrintScene {

    LIST, DETAIL, FLOW_TODO, FLOW_DONE, FLOW_STARTED;

    public String getCode() {
        return name();
    }

    public boolean matches(String value) {
        return getCode().equals(value);
    }
}

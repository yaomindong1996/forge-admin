package com.mdframe.forge.plugin.print.enums;

public enum PrintDesignAction {

    VIEW("print:template:view"), MANAGE("print:template:manage"), PUBLISH("print:template:publish");

    private final String permission;

    PrintDesignAction(String permission) {
        this.permission = permission;
    }

    public String permission() {
        return permission;
    }
}

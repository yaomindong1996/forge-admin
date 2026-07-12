package com.mdframe.forge.plugin.capability.schema;

public class CapabilitySchemaValidationException extends IllegalArgumentException {

    private final String path;

    public CapabilitySchemaValidationException(String path, String message) {
        super(message + "，路径：" + path);
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}

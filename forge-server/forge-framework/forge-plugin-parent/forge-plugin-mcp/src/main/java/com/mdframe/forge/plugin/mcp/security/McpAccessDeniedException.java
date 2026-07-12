package com.mdframe.forge.plugin.mcp.security;

public class McpAccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public McpAccessDeniedException(String message) {
        super(message);
    }
}

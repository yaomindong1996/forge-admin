package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * 可供低代码扩展选择的服务端处理器目录。
 */
@Data
public class ServerBindingHandlerVO {

    private String handlerCode;

    private String handlerName;

    private Set<String> allowedHooks;

    private Map<String, Object> inputSchema;

    private Map<String, Object> outputSchema;

    private Integer timeoutMs;

    private String riskLevel;

    private String requiredPermission;
}

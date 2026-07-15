package com.mdframe.forge.plugin.generator.service.businessapp.extension;

import java.util.Map;
import java.util.Set;

/**
 * 管理员显式注册的低代码服务端扩展处理器。
 */
public interface LowcodeExtensionHandler {

    String handlerCode();

    String handlerName();

    Set<String> allowedHooks();

    Map<String, ExtensionInputField> inputSchema();

    default Map<String, ExtensionInputField> outputSchema() {
        return Map.of();
    }

    default int timeoutMs() {
        return 1000;
    }

    default String riskLevel() {
        return "MEDIUM";
    }

    default String requiredPermission() {
        return null;
    }

    ExtensionExecutionResult execute(ExtensionExecutionContext context);
}

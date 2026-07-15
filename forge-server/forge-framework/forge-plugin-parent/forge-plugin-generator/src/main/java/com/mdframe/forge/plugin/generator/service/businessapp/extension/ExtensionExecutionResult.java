package com.mdframe.forge.plugin.generator.service.businessapp.extension;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 结构化扩展执行结果，不携带原始异常。
 */
@Data
public class ExtensionExecutionResult {

    private boolean success;

    private String code;

    private String message;

    private Map<String, Object> output = new LinkedHashMap<>();

    public static ExtensionExecutionResult success(Map<String, Object> output) {
        ExtensionExecutionResult result = new ExtensionExecutionResult();
        result.setSuccess(true);
        result.setCode("SUCCESS");
        result.setMessage("执行成功");
        result.setOutput(output == null ? Map.of() : new LinkedHashMap<>(output));
        return result;
    }

    public static ExtensionExecutionResult failure(String code, String message) {
        ExtensionExecutionResult result = new ExtensionExecutionResult();
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(message);
        result.setOutput(Map.of());
        return result;
    }
}

package com.mdframe.forge.plugin.ai.routing.constant;

import com.mdframe.forge.starter.core.exception.BusinessException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum AiModelCapabilityCode {
    STREAMING("streaming"), REASONING("reasoning"), TOOL_CALLING("tool_calling"),
    VISION("vision"), STRUCTURED_OUTPUT("structured_output");

    private static final Set<String> CODES = Arrays.stream(values()).map(AiModelCapabilityCode::code).collect(Collectors.toUnmodifiableSet());
    private final String code;

    AiModelCapabilityCode(String code) { this.code = code; }
    public String code() { return code; }
    public static boolean supports(String code) { return code != null && CODES.contains(code.trim()); }
    public static String require(String code) {
        if (!supports(code)) throw new BusinessException("未知的模型能力代码: " + code);
        return code.trim();
    }
}

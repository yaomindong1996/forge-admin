package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * AI 供应商连接适配器代码。
 */
@Getter
@RequiredArgsConstructor
public enum AiProviderAdapterCode {

    OPENAI_COMPATIBLE("openai_compatible"),
    DASHSCOPE_NATIVE("dashscope_native");

    private final String code;

    /**
     * 解析并校验适配器代码，任何缺失或未知值均失败关闭。
     *
     * @param code 适配器代码
     * @return 已知适配器枚举
     */
    public static AiProviderAdapterCode require(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException("AI供应商连接协议不能为空");
        }
        String normalizedCode = code.trim();
        for (AiProviderAdapterCode adapterCode : values()) {
            if (adapterCode.code.equals(normalizedCode)) {
                return adapterCode;
            }
        }
        throw new BusinessException("不支持的AI供应商连接协议: " + normalizedCode);
    }
}

package com.mdframe.forge.plugin.ai.provider.adapter;

/**
 * 与模型供应商协议无关的 Chat 运行参数。
 *
 * @param model 模型标识
 * @param temperature 温度
 * @param maxTokens 最大输出 Token
 */
public record AiModelRuntimeOptions(String model, Double temperature, Integer maxTokens) {

    /**
     * 生成不包含凭据的稳定缓存片段。
     *
     * @return 缓存键片段
     */
    public String cacheKeyFragment() {
        return String.valueOf(model) + ":"
                + String.valueOf(temperature) + ":"
                + String.valueOf(maxTokens);
    }
}

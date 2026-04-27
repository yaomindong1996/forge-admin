package com.mdframe.forge.plugin.generator.service;

import reactor.core.publisher.Flux;
import java.util.Map;

public interface AiClientAdapter {

    AiClientResult call(String agentCode, String message, Map<String, String> contextVars);

    AiClientResult call(String agentCode, String message, Map<String, String> contextVars, Integer timeoutSeconds);

    Flux<String> stream(String userInput,String agentCode, String message, Map<String, String> contextVars);

    Flux<String> stream(String userInput,String sessionId, String agentCode, String message, Map<String, String> contextVars);

    Flux<String> stream(String userInput, String sessionId, String agentCode, String message,
                        Map<String, String> contextVars, Long providerId, Long modelId,
                        Double temperature, Integer maxTokens);

    /**
     * 加载指定智能体的上下文 SPEC 配置，按 sort 升序拼接
     * @param agentCode 智能体编码
     * @return 全部 SPEC 内容拼接字符串，不存在则返回空字符串
     */
    default String loadContextSpec(String agentCode) {
        return "";
    }

    class AiClientResult {
        private String content;
        private boolean fallback;
        private String fallbackReason;

        public static AiClientResult success(String content) {
            AiClientResult r = new AiClientResult();
            r.setContent(content);
            r.setFallback(false);
            return r;
        }

        public static AiClientResult fallback(String reason) {
            AiClientResult r = new AiClientResult();
            r.setFallback(true);
            r.setFallbackReason(reason);
            return r;
        }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public boolean isFallback() { return fallback; }
        public void setFallback(boolean fallback) { this.fallback = fallback; }
        public String getFallbackReason() { return fallbackReason; }
        public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }
    }
}

package com.mdframe.forge.plugin.ai.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.ai.context.domain.AiContextConfig;
import com.mdframe.forge.plugin.ai.context.mapper.AiContextConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContextInjector {

    private static final int MAX_CONTEXT_LENGTH = 8000;
    private static final String CODE_STYLE_PATH = "ai-codegen/code-style.md";

    private final AiContextConfigMapper contextConfigMapper;

    public String injectContext(String systemPrompt, String agentCode) {
        StringBuilder contextBuilder = new StringBuilder();
        boolean hasContext = false;

        String codeStyle = readCodeStyle();
        if (StringUtils.hasText(codeStyle)) {
            contextBuilder.append("\n\n---\n【项目编码规范】\n").append(codeStyle);
            hasContext = true;
        }

        String specContext = readSpecContext(agentCode);
        if (StringUtils.hasText(specContext)) {
            contextBuilder.append("\n\n【SPEC上下文】\n").append(specContext);
            hasContext = true;
        }

        if (!hasContext) {
            return systemPrompt;
        }

        String fullContext = contextBuilder.toString();
        if (fullContext.length() > MAX_CONTEXT_LENGTH) {
            fullContext = fullContext.substring(0, MAX_CONTEXT_LENGTH);
            log.warn("[ContextInjector] 上下文总长度超限，已截断至{}字符, agentCode={}", MAX_CONTEXT_LENGTH, agentCode);
        }

        return systemPrompt + fullContext;
    }

    private String readCodeStyle() {
        try {
            ClassPathResource resource = new ClassPathResource(CODE_STYLE_PATH);
            if (resource.exists()) {
                return resource.getContentAsString(StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.debug("[ContextInjector] code-style.md 不存在或读取失败，跳过注入");
        }
        return null;
    }

    private String readSpecContext(String agentCode) {
        List<AiContextConfig> configs = contextConfigMapper.selectList(
                new LambdaQueryWrapper<AiContextConfig>()
                        .eq(AiContextConfig::getAgentCode, agentCode)
                        .eq(AiContextConfig::getStatus, "0")
                        .orderByAsc(AiContextConfig::getSort));
        if (configs.isEmpty()) {
            return null;
        }
        return configs.stream()
                .map(AiContextConfig::getConfigContent)
                .collect(Collectors.joining("\n"));
    }
}

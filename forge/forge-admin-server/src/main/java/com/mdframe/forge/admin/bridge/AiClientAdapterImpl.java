package com.mdframe.forge.admin.bridge;

import com.mdframe.forge.plugin.ai.client.AiClient;
import com.mdframe.forge.plugin.ai.client.dto.AiClientRequest;
import com.mdframe.forge.plugin.ai.client.dto.AiClientResponse;
import com.mdframe.forge.plugin.ai.context.service.AiContextConfigService;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.service.AiModelService;
import com.mdframe.forge.plugin.generator.service.AiClientAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiClientAdapterImpl implements AiClientAdapter {

    private final AiClient aiClient;
    private final AiContextConfigService contextConfigService;
    private final AiModelService modelService;

    @Override
    public AiClientResult call(String agentCode, String message, Map<String, String> contextVars) {
        return call(agentCode, message, contextVars, 30);
    }

    @Override
    public AiClientResult call(String agentCode, String message, Map<String, String> contextVars, Integer timeoutSeconds) {
        AiClientRequest request = new AiClientRequest();
        request.setAgentCode(agentCode);
        request.setMessage(message);
        request.setContextVars(contextVars);

        AiClientResponse response = aiClient.call(request);

        if (response.isFallback()) {
            return AiClientResult.fallback(response.getFallbackReason());
        }

        return AiClientResult.success(response.getContent());
    }

    @Override
    public Flux<String> stream(String userInput,String agentCode, String message, Map<String, String> contextVars) {
        return stream(userInput, null, agentCode, message, contextVars, null, null, null, null);
    }

    @Override
    public Flux<String> stream(String userInput,String sessionId, String agentCode, String message, Map<String, String> contextVars) {
        return stream(userInput, sessionId, agentCode, message, contextVars, null, null, null, null);
    }

    @Override
    public Flux<String> stream(String userInput, String sessionId, String agentCode, String message,
                               Map<String, String> contextVars, Long providerId, Long modelId,
                               Double temperature, Integer maxTokens) {
        AiClientRequest request = new AiClientRequest();
        request.setAgentCode(agentCode);
        request.setMessage(message);
        request.setUserInput(userInput);
        request.setContextVars(contextVars);
        if (sessionId != null) {
            request.setSessionId(sessionId);
        }
        request.setProviderId(providerId);
        request.setTemperature(temperature);
        request.setMaxTokens(maxTokens);
        applyModelSelection(request, modelId);
        return aiClient.stream(request);
    }

    private void applyModelSelection(AiClientRequest request, Long modelId) {
        if (modelId == null) {
            return;
        }
        AiModel model = modelService.getById(modelId);
        if (model == null) {
            return;
        }
        request.setProviderId(model.getProviderId());
        request.setModelName(model.getModelId());
        if (request.getMaxTokens() == null) {
            request.setMaxTokens(model.getMaxTokens());
        }
    }

    @Override
    public String loadContextSpec(String agentCode) {
        List<String> contents = contextConfigService.listByAgentCode(agentCode)
                .stream()
                .map(c -> c.getConfigContent())
                .collect(Collectors.toList());
        return String.join("\n", contents);
    }
}

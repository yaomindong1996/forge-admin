package com.mdframe.forge.plugin.ai.multimodal.voice;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.model.adapter.AiModelAdapterRegistry;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.service.AiModelService;
import com.mdframe.forge.plugin.ai.multimodal.voice.adapter.AiAsrModelAdapter;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * ASR（语音识别）服务。
 * 从 Agent 绑定的 asr_model_id 解析模型，调用适配器转写。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAsrService {

    private final AiModelAdapterRegistry adapterRegistry;
    private final AiModelService modelService;
    private final AiProviderMapper providerMapper;
    private final AiSecretCrypto aiSecretCrypto;
    private final AiAgentService agentService;

    /**
     * 语音识别：音频 → 文本
     *
     * @param audio   音频文件
     * @param agentId Agent ID（解析绑定的 ASR 模型）
     * @return 识别文本
     */
    public String transcribe(MultipartFile audio, Long agentId) {
        if (audio == null || audio.isEmpty()) {
            throw new BusinessException("音频文件不能为空");
        }

        // 解析 Agent 绑定的 ASR 模型
        AiModel model = resolveAsrModel(agentId);
        AiProvider provider = providerMapper.selectById(model.getProviderId());
        if (provider == null) {
            throw new BusinessException("供应商不存在: " + model.getProviderId());
        }

        // 解密 API Key
        String apiKey = provider.getApiKey();
        apiKey = aiSecretCrypto.resolvePlainSecret(apiKey);

        // 调用 ASR 适配器
        AiAsrModelAdapter adapter = adapterRegistry.getAsr(model.getModelId());
        try {
            byte[] audioBytes = audio.getBytes();
            String mimeType = audio.getContentType() != null ? audio.getContentType() : "audio/wav";
            return adapter.transcribe(provider.getBaseUrl(), apiKey, model.getModelId(), audioBytes, mimeType);
        } catch (IOException e) {
            throw new BusinessException("读取音频文件失败: " + e.getMessage());
        }
    }

    private AiModel resolveAsrModel(Long agentId) {
        if (agentId == null) {
            throw new BusinessException("Agent ID不能为空，语音识别需要Agent绑定ASR模型");
        }
        AiAgent agent = agentService.getById(agentId);
        if (agent == null) {
            throw new BusinessException("Agent不存在: " + agentId);
        }
        if (agent.getAsrModelId() == null) {
            throw new BusinessException("该Agent未绑定语音识别模型，请在Agent配置中设置ASR模型");
        }
        AiModel model = modelService.getById(agent.getAsrModelId());
        if (model == null) {
            throw new BusinessException("ASR模型不存在: " + agent.getAsrModelId());
        }
        return model;
    }
}

package com.mdframe.forge.plugin.ai.multimodal.voice;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.model.adapter.AiModelAdapterRegistry;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.service.AiModelService;
import com.mdframe.forge.plugin.ai.multimodal.voice.adapter.AiTtsModelAdapter;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.model.FileMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * TTS（语音合成）服务。
 * 从 Agent 绑定的 tts_model_id 解析模型，调用适配器合成，结果存文件系统。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTtsService {

    private final AiModelAdapterRegistry adapterRegistry;
    private final AiModelService modelService;
    private final AiProviderMapper providerMapper;
    private final AiSecretCrypto aiSecretCrypto;
    private final AiAgentService agentService;
    private final FileManager fileManager;

    /**
     * 语音合成：文本 → 语音文件
     *
     * @param text    待合成文本
     * @param agentId Agent ID（解析绑定的 TTS 模型）
     * @return 文件ID
     */
    public Long synthesize(String text, Long agentId) {
        if (text == null || text.isBlank()) {
            throw new BusinessException("合成文本不能为空");
        }

        // 解析 Agent 绑定的 TTS 模型
        AiModel model = resolveTtsModel(agentId);
        AiProvider provider = providerMapper.selectById(model.getProviderId());
        if (provider == null) {
            throw new BusinessException("供应商不存在: " + model.getProviderId());
        }

        // 解密 API Key
        String apiKey = provider.getApiKey();
        apiKey = aiSecretCrypto.resolvePlainSecret(apiKey);

        // 调用 TTS 适配器
        AiTtsModelAdapter adapter = adapterRegistry.getTts(model.getModelId());
        byte[] audioBytes = adapter.synthesize(provider.getBaseUrl(), apiKey, model.getModelId(), text);

        // 存储到文件系统
        try {
            InputStream is = new ByteArrayInputStream(audioBytes);
            FileMetadata metadata = fileManager.upload(is, "tts-output.mp3", "audio/mpeg",
                    "ai_tts", null);
            return Long.parseLong(metadata.getFileId());
        } catch (Exception e) {
            throw new BusinessException("语音文件存储失败: " + e.getMessage());
        }
    }

    private AiModel resolveTtsModel(Long agentId) {
        if (agentId == null) {
            throw new BusinessException("Agent ID不能为空，语音合成需要Agent绑定TTS模型");
        }
        AiAgent agent = agentService.getById(agentId);
        if (agent == null) {
            throw new BusinessException("Agent不存在: " + agentId);
        }
        if (agent.getTtsModelId() == null) {
            throw new BusinessException("该Agent未绑定语音合成模型，请在Agent配置中设置TTS模型");
        }
        AiModel model = modelService.getById(agent.getTtsModelId());
        if (model == null) {
            throw new BusinessException("TTS模型不存在: " + agent.getTtsModelId());
        }
        return model;
    }
}

package com.mdframe.forge.plugin.ai.multimodal.voice.adapter;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.plugin.ai.provider.adapter.OpenAiSdkSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.stereotype.Component;

/**
 * OpenAI 兼容协议的 TTS 模型适配器。
 * 覆盖 OpenAI / Azure / DashScope 兼容等供应商。
 */
@Slf4j
@Component
public class OpenAiCompatibleTtsModelAdapter implements AiTtsModelAdapter {

    @Override
    public String getSupportedProvider() {
        return "openai_compatible";
    }

    @Override
    public boolean supports(String modelKey) {
        if (modelKey == null) {
            return false;
        }
        String lower = modelKey.toLowerCase();
        return lower.startsWith("tts")
                || lower.contains("speech")
                || lower.contains("cosyvoice")
                || lower.contains("sambert");
    }

    @Override
    public byte[] synthesize(String baseUrl, String apiKey, String model, String text) {
        try {
            OpenAiAudioSpeechOptions options = OpenAiAudioSpeechOptions.builder()
                    .baseUrl(OpenAiSdkSupport.baseUrl(baseUrl)).apiKey(apiKey).model(model)
                    .timeout(OpenAiSdkSupport.REQUEST_TIMEOUT).maxRetries(0)
                    .input(text)
                    .build();

            OpenAiAudioSpeechModel speechModel = OpenAiAudioSpeechModel.builder().options(options).build();

            return speechModel.call(text);
        } catch (Exception e) {
            log.warn("[AI TTS] 语音合成失败, model={}, type={}", model, e.getClass().getSimpleName());
            throw new BusinessException("语音合成失败，请稍后重试");
        }
    }
}

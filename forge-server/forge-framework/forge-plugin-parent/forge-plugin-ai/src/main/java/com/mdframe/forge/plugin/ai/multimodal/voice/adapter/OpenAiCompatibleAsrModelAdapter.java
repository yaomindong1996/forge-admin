package com.mdframe.forge.plugin.ai.multimodal.voice.adapter;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.plugin.ai.provider.adapter.OpenAiSdkSupport;
import com.openai.models.audio.AudioResponseFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

/**
 * OpenAI 兼容协议的 ASR 模型适配器。
 * 覆盖 OpenAI / Azure / DashScope 兼容等供应商。
 */
@Slf4j
@Component
public class OpenAiCompatibleAsrModelAdapter implements AiAsrModelAdapter {

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
        return lower.startsWith("whisper")
                || lower.contains("asr")
                || lower.contains("speech-to-text")
                || lower.contains("paraformer");
    }

    @Override
    public String transcribe(String baseUrl, String apiKey, String model, byte[] audio, String mimeType) {
        try {
            OpenAiAudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
                    .baseUrl(OpenAiSdkSupport.baseUrl(baseUrl)).apiKey(apiKey).model(model)
                    .timeout(OpenAiSdkSupport.REQUEST_TIMEOUT).maxRetries(0)
                    .responseFormat(AudioResponseFormat.TEXT)
                    .build();

            OpenAiAudioTranscriptionModel transcriptionModel =
                    OpenAiAudioTranscriptionModel.builder().options(options).build();

            // 新 SDK 的 multipart 上传要求文件名；默认值只用于协议，不落盘。
            ByteArrayResource audioResource = new ByteArrayResource(audio) {
                @Override
                public String getFilename() {
                    return "audio.wav";
                }
            };
            AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, options);
            AudioTranscriptionResponse response = transcriptionModel.call(prompt);

            AudioTranscription transcript = response.getResult();
            if (transcript == null || transcript.getOutput() == null) {
                throw new BusinessException("语音识别返回空结果");
            }

            return transcript.getOutput();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[AI ASR] 语音识别失败, model={}, type={}", model, e.getClass().getSimpleName());
            throw new BusinessException("语音识别失败，请稍后重试");
        }
    }
}

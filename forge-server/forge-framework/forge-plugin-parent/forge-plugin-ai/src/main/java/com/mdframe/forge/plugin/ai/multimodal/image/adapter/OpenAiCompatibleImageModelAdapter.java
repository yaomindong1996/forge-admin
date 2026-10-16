package com.mdframe.forge.plugin.ai.multimodal.image.adapter;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.plugin.ai.provider.adapter.OpenAiSdkSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OpenAI 兼容协议的图片生成模型适配器。
 * 覆盖 OpenAI / Azure / DashScope 兼容 / 硅基流动等供应商。
 */
@Slf4j
@Component
public class OpenAiCompatibleImageModelAdapter implements AiImageModelAdapter {

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
        return lower.startsWith("dall-e")
                || lower.startsWith("gpt-image")
                || lower.contains("stable-diffusion")
                || lower.contains("qwen-image")
                || lower.contains("flux")
                || lower.contains("image");
    }

    @Override
    public String generate(String baseUrl, String apiKey, String model, String prompt,
                           String negativePrompt, String size) {
        try {
            OpenAiImageOptions.Builder optionsBuilder = OpenAiImageOptions.builder()
                    .baseUrl(OpenAiSdkSupport.baseUrl(baseUrl)).apiKey(apiKey).model(model)
                    .timeout(OpenAiSdkSupport.REQUEST_TIMEOUT).maxRetries(0);

            // 解析 size（如 "1024x1024"）为 width + height
            if (size != null && !size.isBlank() && size.contains("x")) {
                String[] parts = size.split("x");
                if (parts.length == 2) {
                    try {
                        optionsBuilder.width(Integer.parseInt(parts[0].trim()));
                        optionsBuilder.height(Integer.parseInt(parts[1].trim()));
                    } catch (NumberFormatException ignored) {
                        // 忽略无效尺寸
                    }
                }
            }

            OpenAiImageOptions options = optionsBuilder.build();
            OpenAiImageModel imageModel = OpenAiImageModel.builder().options(options).build();

            ImagePrompt imagePrompt = new ImagePrompt(prompt);
            ImageResponse response = imageModel.call(imagePrompt);

            List<ImageGeneration> generations = response.getResults();
            if (generations == null || generations.isEmpty()) {
                throw new BusinessException("图片生成返回空结果");
            }

            // 返回图片 URL 或 base64
            String url = generations.get(0).getOutput().getUrl();
            String b64 = generations.get(0).getOutput().getB64Json();

            if (url != null && !url.isBlank()) {
                return url;
            }
            if (b64 != null && !b64.isBlank()) {
                return "data:image/png;base64," + b64;
            }
            throw new BusinessException("图片生成返回结果中无URL或base64");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[AI Image] 图片生成失败, model={}, type={}", model, e.getClass().getSimpleName());
            throw new BusinessException("图片生成失败，请稍后重试");
        }
    }
}

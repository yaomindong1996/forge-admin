package com.mdframe.forge.plugin.ai.multimodal.image;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Base64;

/** URL 与内联图片使用同一存储校验，签名链接和正文不写入异常或日志。 */
@Component
@RequiredArgsConstructor
public class AiGeneratedImageStore {
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(3);
    private final SecureOutboundClient outbound;
    private final FileManager files;
    private final AiImageGenerationProperties properties;
    private final AiImageContentValidator validator;

    public Long store(String source) {
        if (source == null || source.isBlank()) {
            throw new BusinessException("模型没有返回图片");
        }
        ImageData data = source.startsWith("data:") ? inline(source) : download(source);
        String mime = validator.validate(data.bytes(), data.mime());
        String extension = "image/png".equals(mime) ? ".png" : ".jpg";
        try (var input = new ByteArrayInputStream(data.bytes())) {
            var metadata = files.upload(input, "ai-image" + extension, mime, "ai_image_generate", null,
                    null, true, (long) data.bytes().length);
            Long fileId = metadata == null ? null : Long.valueOf(metadata.getFileId());
            if (fileId == null || fileId <= 0) {
                throw new BusinessException("图片存储未返回有效文件");
            }
            return fileId;
        } catch (java.io.IOException | NumberFormatException ex) {
            throw new BusinessException("图片存储失败");
        }
    }

    private ImageData inline(String source) {
        // 在 substring/decode 之前限制整个数据 URI，避免解码前分配无界副本。
        long maxEncoded = 4L * ((properties.getMaxImageBytes() + 2L) / 3L);
        int comma = source.indexOf(',');
        if (source.length() > maxEncoded + 64 || comma < 0 || comma > 40) {
            throw new BusinessException("内联图片格式无效或超过大小上限");
        }
        String header = source.substring(0, comma);
        String mime = switch (header) {
            case "data:image/png;base64" -> "image/png";
            case "data:image/jpeg;base64" -> "image/jpeg";
            default -> throw new BusinessException("内联图片只支持 PNG/JPEG base64");
        };
        if (source.length() - comma - 1 > maxEncoded) {
            throw new BusinessException("内联图片超过大小上限");
        }
        try {
            return new ImageData(Base64.getDecoder().decode(source.substring(comma + 1)), mime);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("内联图片编码无效");
        }
    }

    private ImageData download(String source) {
        if (source.length() > 8192) {
            throw new BusinessException("图片下载地址过长");
        }
        var request = OutboundRequest.builder().scene(OutboundScenes.EXTERNAL_CONNECTOR)
                .url(source).method("GET").connectTimeout(DOWNLOAD_TIMEOUT).readTimeout(DOWNLOAD_TIMEOUT)
                .writeTimeout(DOWNLOAD_TIMEOUT).callTimeout(DOWNLOAD_TIMEOUT).build();
        var response = outbound.execute(request);
        if (response.getStatusCode() != 200) {
            throw new BusinessException("图片下载未返回成功状态");
        }
        return new ImageData(response.getBody(), response.firstHeader("Content-Type"));
    }

    private record ImageData(byte[] bytes, String mime) { }
}

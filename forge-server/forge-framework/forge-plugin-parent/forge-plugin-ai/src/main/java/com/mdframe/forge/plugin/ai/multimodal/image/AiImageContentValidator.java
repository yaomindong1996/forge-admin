package com.mdframe.forge.plugin.ai.multimodal.image;

import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

/** 只接受可完整解码的 PNG/JPEG；先读尺寸防止小文件触发巨大像素分配。 */
@Component
@RequiredArgsConstructor
public class AiImageContentValidator {
    private final AiImageGenerationProperties properties;

    public String validate(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0 || bytes.length > properties.getMaxImageBytes()) {
            throw new BusinessException("生成图片为空或超过大小上限");
        }
        String mime = normalizedMime(contentType);
        String format = switch (mime) {
            case "image/png" -> "png";
            case "image/jpeg" -> "JPEG";
            default -> throw new BusinessException("生成图片只支持 PNG 或 JPEG 格式");
        };
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BusinessException("生成图片内容无效");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (!format.equalsIgnoreCase(reader.getFormatName()) || width <= 0 || height <= 0
                        || (long) width * height > properties.getMaxImagePixels()) {
                    throw new BusinessException("图片类型不匹配或像素超过上限");
                }
                if (reader.read(0) == null) {
                    throw new BusinessException("生成图片无法解码");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new BusinessException("生成图片内容无效");
        }
        return mime;
    }

    private String normalizedMime(String type) {
        return type == null ? "" : type.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }
}

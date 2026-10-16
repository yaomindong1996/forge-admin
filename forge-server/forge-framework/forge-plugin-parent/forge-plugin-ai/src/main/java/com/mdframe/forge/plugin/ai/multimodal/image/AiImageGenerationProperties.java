package com.mdframe.forge.plugin.ai.multimodal.image;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "forge.ai.image")
public class AiImageGenerationProperties {
    @Min(1)
    @Max(32)
    private int corePoolSize = 2;
    @Min(1)
    @Max(32)
    private int maxPoolSize = 4;
    @Min(0)
    @Max(256)
    private int queueCapacity = 16;
    @Min(1024)
    @Max(16 * 1024 * 1024)
    private int maxImageBytes = 2 * 1024 * 1024;
    @Min(1)
    @Max(16 * 1024 * 1024)
    private int maxImagePixels = 16 * 1024 * 1024;
}

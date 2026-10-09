package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class PluginArtifactRegisterDTO {
    @NotNull @Pattern(regexp = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
    private String requestId;
    // 导入的原文本由严格解析器处理，不能交给全局Jackson的宽松未知字段策略。
    @NotNull @Size(min = 2, max = 65536) @ToString.Exclude
    private String metadataJson;
    @NotNull
    private Boolean localVerified;
    @NotNull
    private Boolean notDeployed;
    @NotNull @Size(min = 10, max = 1000) @ToString.Exclude
    private String note;
}

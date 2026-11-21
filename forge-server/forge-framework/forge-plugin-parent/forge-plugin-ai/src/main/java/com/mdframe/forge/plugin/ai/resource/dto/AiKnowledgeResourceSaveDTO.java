package com.mdframe.forge.plugin.ai.resource.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AiKnowledgeResourceSaveDTO {
    @NotNull
    @Positive
    private Long embeddingModelId;
    @Positive
    private Long rerankModelId;
    @NotNull
    @Positive
    private Long vectorStoreInstanceId;
    @NotNull
    @Min(1)
    @Max(65536)
    private Integer dimension;
    @NotNull
    @Min(32)
    @Max(2048)
    private Integer chunkSize;
    @NotNull
    @Min(0)
    private Integer revision;
}

package com.mdframe.forge.plugin.ai.multimodal.image.dto;

import com.mdframe.forge.plugin.ai.multimodal.image.domain.AiImageGenerateRecord;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 请求不接受主键、供应商、用户、租户、状态或结果文件。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiImageGenerateDTO {
    @NotNull
    @Positive
    private Long modelId;
    @NotBlank
    @Size(max = 6000)
    private String prompt;
    @Size(max = 3000)
    private String negativePrompt;
    @Size(max = 32)
    private String size;

    public AiImageGenerateRecord toRecord() {
        var record = new AiImageGenerateRecord();
        record.setModelId(modelId);
        record.setPrompt(prompt);
        record.setNegativePrompt(negativePrompt);
        record.setSize(size);
        return record;
    }
}

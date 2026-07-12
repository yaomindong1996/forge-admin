package com.mdframe.forge.plugin.capability.highrisk.publish;

import com.mdframe.forge.plugin.capability.secureaction.publish.BusinessActionCapabilityPublishDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HighRiskActionPublishDTO {
    @Valid @NotNull
    private BusinessActionCapabilityPublishDTO action;
    @NotBlank
    private String approvalCandidateGroup;
    @NotNull @Min(300) @Max(604800)
    private Integer expireSeconds = 86400;
}

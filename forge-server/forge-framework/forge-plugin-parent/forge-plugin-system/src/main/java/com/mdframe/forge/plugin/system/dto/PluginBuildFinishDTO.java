package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PluginBuildFinishDTO extends PluginBuildLeaseDTO {
    @Valid
    @NotNull
    private PluginBuildResultDTO result;
}

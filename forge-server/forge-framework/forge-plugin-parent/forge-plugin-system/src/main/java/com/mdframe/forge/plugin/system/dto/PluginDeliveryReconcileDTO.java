package com.mdframe.forge.plugin.system.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class PluginDeliveryReconcileDTO {
    @NotNull @AssertTrue private Boolean executorStopped;
    @NotNull @Size(min = 10, max = 1000) private String note;
}

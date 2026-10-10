package com.mdframe.forge.plugin.system.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper = true)
public class PluginDeliveryFinishDTO extends PluginDeliveryLeaseDTO {
    @NotNull @Pattern(regexp = "succeeded|failed|uncertain") private String status;
    @NotNull @Pattern(regexp = "[a-f0-9]{64}") private String artifactManifestSha256;
    @NotNull private Boolean cosVerified;
    @NotNull private Boolean runtimeVerified;
    @Pattern(regexp = "[A-Z][A-Z0-9_]{0,95}") private String failureCode;
}

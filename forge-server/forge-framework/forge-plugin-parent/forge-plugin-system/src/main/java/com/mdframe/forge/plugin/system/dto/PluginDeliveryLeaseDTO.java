package com.mdframe.forge.plugin.system.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
@Data
public class PluginDeliveryLeaseDTO {
    @NotNull @Pattern(regexp = "[a-f0-9]{64}") private String lease;
    @NotNull @Pattern(regexp = "[a-f0-9-]{36}") private String nonce;
}

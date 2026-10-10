package com.mdframe.forge.plugin.system.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class PluginDeliveryCreateDTO {
    @NotNull @Pattern(regexp = "[a-f0-9-]{36}") private String requestId;
    @NotNull @Pattern(regexp = "[a-f0-9-]{36}") private String taskId;
    @NotNull @Pattern(regexp = "[a-z][a-z0-9-]{0,63}") private String targetId;
    @NotNull @Pattern(regexp = "publish|deploy|restore") private String action;
    @NotNull @Pattern(regexp = "rel-[a-f0-9]{64}") private String releaseId;
    @NotNull @Size(min = 10, max = 1000) private String note;
    @Size(max = 128) @Pattern(regexp = "[a-zA-Z0-9_./:-]{1,128}") private String backupReference;
    @NotNull private Boolean migrationsReviewed;
    @NotNull private Boolean backwardCompatible;
}

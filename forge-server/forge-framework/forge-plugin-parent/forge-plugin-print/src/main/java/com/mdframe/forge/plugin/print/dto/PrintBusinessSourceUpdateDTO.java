package com.mdframe.forge.plugin.print.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PrintBusinessSourceUpdateDTO(
        @NotNull @Positive Long expectedRevision,
        @NotBlank @Size(max = 100) String sourceName,
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_.:-]{0,99}") String providerCode,
        @Positive Long datasetId,
        @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,99}") String objectCode,
        @Size(max = 65535) String parameterSchemaJson,
        @Size(max = 65535) String mappingJson) {
}

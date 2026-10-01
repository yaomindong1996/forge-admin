package com.mdframe.forge.plugin.print.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mdframe.forge.plugin.print.enums.PrintBusinessSourceType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PrintBusinessSourceCreateDTO(
        @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]{0,79}") String sourceCode,
        @NotBlank @Size(max = 100) String sourceName,
        @NotNull PrintBusinessSourceType sourceType,
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_.:-]{0,99}") String providerCode,
        @Positive Long datasetId,
        @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,99}") String objectCode,
        @Size(max = 65535) String parameterSchemaJson,
        @Size(max = 65535) String mappingJson) {

    @JsonIgnore
    @AssertTrue(message = "打印来源类型与配置不匹配")
    public boolean isSourceConfigValid() {
        if (sourceType == null) {
            return true;
        }
        return switch (sourceType) {
            case SERVICE -> providerCode != null && !providerCode.isBlank() && datasetId == null;
            case DATASET -> providerCode == null && datasetId != null;
            case API -> false;
        };
    }
}

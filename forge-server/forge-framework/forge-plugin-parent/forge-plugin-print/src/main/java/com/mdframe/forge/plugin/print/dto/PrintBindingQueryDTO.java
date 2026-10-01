package com.mdframe.forge.plugin.print.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import jakarta.validation.constraints.*;

public record PrintBindingQueryDTO(
        @Positive Long businessSourceId,
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]{0,79}") String sourceCode,
        @Positive Long applicationId,
        @NotNull PrintSourceType sourceType,
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}") String pageId,
        @Size(max = 128) String formKey,
        @NotBlank String objectCode,
        PrintScene scene) {

    public PrintBindingQueryDTO(Long applicationId, PrintSourceType sourceType, String pageId,
                                String formKey, String objectCode, PrintScene scene) {
        this(null, null, applicationId, sourceType, pageId, formKey, objectCode, scene);
    }

    public PrintSourceRequest source() {
        return new PrintSourceRequest(
                businessSourceId, sourceCode, applicationId, sourceType, pageId, formKey, objectCode);
    }

    @JsonIgnore
    @AssertTrue(message = "打印来源身份无效")
    public boolean isSourceValid() {
        return sourceType == null || source().isSourceValid();
    }
}

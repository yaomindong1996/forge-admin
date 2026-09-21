package com.mdframe.forge.plugin.print.dto;

import com.mdframe.forge.plugin.print.enums.*;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import jakarta.validation.constraints.*;

public record PrintBindingQueryDTO(@NotNull @Positive Long applicationId, @NotNull PrintSourceType sourceType, @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}") String pageId, @Size(max = 128) String formKey, @NotBlank String objectCode, PrintScene scene) {

    public PrintSourceRequest source() {
        return new PrintSourceRequest(applicationId, sourceType, pageId, formKey, objectCode);
    }
}

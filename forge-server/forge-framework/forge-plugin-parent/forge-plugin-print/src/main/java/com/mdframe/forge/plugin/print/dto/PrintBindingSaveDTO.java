package com.mdframe.forge.plugin.print.dto;

import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record PrintBindingSaveDTO(@Positive Long id, @Min(1) @Max(9223372036854775806L) Long expectedRevision, @Valid @NotNull PrintSourceRequest source, @NotNull @Positive Long templateId, @NotNull PrintScene scene, @NotNull Boolean isDefault, @NotNull @Min(0) @Max(10000) Integer sortOrder, @NotNull @Min(0) @Max(1) Integer status) {

    @JsonIgnore
    @AssertTrue
    public boolean isRevisionValid() {
        return id == null ? expectedRevision == null : expectedRevision != null;
    }
}

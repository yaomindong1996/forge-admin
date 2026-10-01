package com.mdframe.forge.plugin.print.dto;

import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record PrintBindingSaveDTO(
        @Positive Long id,
        @Min(1) @Max(9223372036854775806L) Long expectedRevision,
        @Valid @NotNull PrintSourceRequest source,
        @NotNull @Positive Long templateId,
        @Positive Long templateVersionId,
        @NotNull PrintScene scene,
        @NotNull Boolean isDefault,
        @NotNull @Min(0) @Max(10000) Integer sortOrder,
        @NotNull @Min(0) @Max(1) Integer status) {

    public PrintBindingSaveDTO(Long id, Long expectedRevision, PrintSourceRequest source,
                               Long templateId, PrintScene scene, Boolean isDefault,
                               Integer sortOrder, Integer status) {
        this(id, expectedRevision, source, templateId, null, scene, isDefault, sortOrder, status);
    }

    @JsonIgnore
    @AssertTrue
    public boolean isRevisionValid() {
        return id == null ? expectedRevision == null : expectedRevision != null;
    }

    @JsonIgnore
    @AssertTrue(message = "独立来源须固定模板版本，应用来源由发布快照固定版本")
    public boolean isVersionValid() {
        if (source == null || source.sourceType() == null) {
            return true;
        }
        return source.sourceType().isStandalone()
                ? templateVersionId != null
                : templateVersionId == null;
    }
}

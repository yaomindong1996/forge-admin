package com.mdframe.forge.plugin.print.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PrintBusinessSourceStatusDTO(
        @NotNull @Positive Long expectedRevision,
        @NotNull @Min(0) @Max(1) Integer status) {
}

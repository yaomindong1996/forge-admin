package com.mdframe.forge.plugin.ai.skill.pkg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SkillPackageActionDTO(
        @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String packageHash,
        @NotBlank String expectedStatus) {
}

package com.mdframe.forge.plugin.ai.skill.pkg.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SkillPackageBindDTO(
        @NotNull @Positive Long agentId,
        @NotNull @Positive @JsonAlias("skillVersionId") Long versionId) {
}

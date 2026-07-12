package com.mdframe.forge.plugin.capability.controlplane.service;

import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityPublishDTO;

import java.util.Objects;

public record CapabilityVersionFingerprint(
        String sourceType,
        String sourceKey,
        String sourceVersion,
        String behavior,
        String riskLevel,
        String visibility) {

    public CapabilityVersionFingerprint {
        Objects.requireNonNull(sourceType, "sourceType 不能为空");
        Objects.requireNonNull(sourceKey, "sourceKey 不能为空");
        Objects.requireNonNull(sourceVersion, "sourceVersion 不能为空");
        Objects.requireNonNull(behavior, "behavior 不能为空");
        Objects.requireNonNull(riskLevel, "riskLevel 不能为空");
        Objects.requireNonNull(visibility, "visibility 不能为空");
    }

    public static CapabilityVersionFingerprint from(CapabilityPublishDTO dto) {
        return new CapabilityVersionFingerprint(
                dto.sourceType(), dto.sourceKey(), dto.sourceVersion(),
                dto.behavior(), dto.riskLevel(), dto.visibility());
    }
}

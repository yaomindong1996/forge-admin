package com.mdframe.forge.plugin.system.enums;

import com.mdframe.forge.starter.core.exception.BusinessException;

public enum PluginBuildPhase {
    SOURCE_SNAPSHOT("source_snapshot"), PACKAGE_PREFLIGHT("package_preflight"),
    SOURCE_PREFLIGHT("source_preflight"), CONTAINER_BUILD("container_build"),
    ARTIFACT_VERIFICATION("artifact_verification");
    private final String code;

    PluginBuildPhase(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PluginBuildPhase from(String value) {
        for (var phase : values()) {
            if (phase.code.equals(value)) {
                return phase;
            }
        }
        throw new BusinessException(400, "构建阶段无效");
    }
}

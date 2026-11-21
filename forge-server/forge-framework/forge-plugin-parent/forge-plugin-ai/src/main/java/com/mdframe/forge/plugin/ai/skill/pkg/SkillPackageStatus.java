package com.mdframe.forge.plugin.ai.skill.pkg;

public enum SkillPackageStatus {
    IMPORTED, READY, RETIRED;

    public String getCode() {
        return name();
    }

    public boolean matches(String value) {
        return name().equals(value);
    }
}

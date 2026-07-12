package com.mdframe.forge.plugin.ai.health;
public record AiModelHealthKey(Long tenantId, Long providerPk, Long modelPk) {
    public AiModelHealthKey { if (tenantId == null || providerPk == null || modelPk == null) throw new IllegalArgumentException("模型健康键不能为空"); }
}

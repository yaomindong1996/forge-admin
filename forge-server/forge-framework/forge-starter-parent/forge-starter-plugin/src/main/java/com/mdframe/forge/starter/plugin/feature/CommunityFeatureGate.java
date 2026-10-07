package com.mdframe.forge.starter.plugin.feature;

import com.mdframe.forge.starter.plugin.descriptor.PluginEdition;

/** 社区默认规则；Bean 由自动配置提供，避免扫描顺序影响客户自定义实现。 */
public final class CommunityFeatureGate implements FeatureGate {

    @Override
    public boolean isEnabled(String featureCode) {
        return featureCode == null || !featureCode.strip().startsWith("ee.");
    }

    @Override
    public String edition() {
        return PluginEdition.COMMUNITY.getCode();
    }
}

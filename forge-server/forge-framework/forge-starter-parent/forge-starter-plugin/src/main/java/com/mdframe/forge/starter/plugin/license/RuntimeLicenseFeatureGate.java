package com.mdframe.forge.starter.plugin.license;

import com.mdframe.forge.starter.plugin.descriptor.PluginEdition;
import com.mdframe.forge.starter.plugin.feature.CommunityFeatureGate;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;

import java.time.Clock;
import java.util.List;

/** 启动验签，查询时判断期限；多节点使用同一项目标识，许可证更新需重启。 */
public final class RuntimeLicenseFeatureGate implements FeatureGate {
    private final CommunityFeatureGate community = new CommunityFeatureGate();
    private final LicensePayload.Binding binding;
    private final List<LicensePayload> licenses;
    private final Clock clock;

    public RuntimeLicenseFeatureGate(LicensePayload.Binding binding, List<LicensePayload> licenses, Clock clock) {
        this.binding = binding;
        this.licenses = List.copyOf(licenses);
        this.clock = clock;
    }

    @Override
    public boolean isEnabled(String featureCode) {
        if (community.isEnabled(featureCode)) {
            return true;
        }
        return licenses.stream().anyMatch(license -> usable(license)
                && license.scope().featureCodes().contains(featureCode.strip()));
    }

    @Override
    public String edition() {
        return licenses.stream().anyMatch(this::usable)
                ? PluginEdition.ENTERPRISE.getCode() : community.edition();
    }

    private boolean usable(LicensePayload license) {
        return binding != null && binding.equals(license.binding())
                && license.terms().allowsUse(clock.instant().getEpochSecond());
    }
}

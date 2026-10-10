package com.mdframe.forge.starter.plugin.license;

import com.mdframe.forge.starter.plugin.descriptor.PluginEdition;
import com.mdframe.forge.starter.plugin.feature.CommunityFeatureGate;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;

import java.time.Clock;
import java.util.List;
import java.util.stream.IntStream;

/** 启动验签，查询时判断期限；多节点使用同一项目标识，许可证更新需重启。 */
public final class RuntimeLicenseFeatureGate implements FeatureGate {
    private final CommunityFeatureGate community = new CommunityFeatureGate();
    private final LicensePayload.Binding binding;
    private final List<LoadedRuntimeLicense> licenses;
    private final Clock clock;
    private final RuntimeLicenseReport.Configuration configuration;

    public RuntimeLicenseFeatureGate(LicensePayload.Binding binding, List<LicensePayload> licenses, Clock clock) {
        this(binding, IntStream.range(0, licenses.size())
                        .mapToObj(index -> new LoadedRuntimeLicense(index + 1, licenses.get(index))).toList(), clock,
                new RuntimeLicenseReport.Configuration(binding, clock.instant().toString(), 0, 0, true));
    }

    RuntimeLicenseFeatureGate(LicensePayload.Binding binding, List<LoadedRuntimeLicense> licenses,
                              Clock clock, RuntimeLicenseReport.Configuration configuration) {
        this.binding = binding;
        this.licenses = List.copyOf(licenses);
        this.clock = clock;
        this.configuration = configuration;
    }

    @Override
    public boolean isEnabled(String featureCode) {
        if (community.isEnabled(featureCode)) {
            return true;
        }
        return licenses.stream().anyMatch(license -> usable(license)
                && license.payload().scope().featureCodes().contains(featureCode.strip()));
    }

    @Override
    public String edition() {
        return licenses.stream().anyMatch(this::usable)
                ? PluginEdition.ENTERPRISE.getCode() : community.edition();
    }

    /** 刷新诊断不重新读文件；期限由每次查询的同一个 UTC 时刻计算。 */
    public RuntimeLicenseReport report() {
        var now = clock.instant();
        var entries = licenses.stream().map(license -> license.view(binding, now.getEpochSecond())).toList();
        String currentEdition = entries.stream().anyMatch(entry -> RuntimeLicenseState.VALID.matches(entry.state()))
                ? PluginEdition.ENTERPRISE.getCode() : community.edition();
        return new RuntimeLicenseReport(configuration, now.toString(), currentEdition, entries);
    }

    private boolean usable(LoadedRuntimeLicense license) {
        return RuntimeLicenseState.evaluate(binding, license.payload(), clock.instant().getEpochSecond())
                == RuntimeLicenseState.VALID;
    }
}

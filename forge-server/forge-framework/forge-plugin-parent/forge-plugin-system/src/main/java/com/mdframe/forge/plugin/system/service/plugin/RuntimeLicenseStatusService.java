package com.mdframe.forge.plugin.system.service.plugin;

import com.mdframe.forge.plugin.system.enums.RuntimeLicenseMode;
import com.mdframe.forge.plugin.system.vo.RuntimeLicenseStatusVO;
import com.mdframe.forge.starter.plugin.feature.CommunityFeatureGate;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import com.mdframe.forge.starter.plugin.license.RuntimeLicenseFeatureGate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** 仅查询当前实例，不读取外部文件，也不主动启用许可证。 */
@Service
@RequiredArgsConstructor
public class RuntimeLicenseStatusService {
    private final ObjectProvider<FeatureGate> gates;

    public RuntimeLicenseStatusVO status() {
        FeatureGate gate = gates.getIfAvailable();
        if (gate == null) {
            return new RuntimeLicenseStatusVO(RuntimeLicenseMode.UNAVAILABLE.getCode(), null, null);
        }
        if (gate instanceof RuntimeLicenseFeatureGate licenseGate) {
            var report = licenseGate.report();
            return new RuntimeLicenseStatusVO(RuntimeLicenseMode.LICENSE.getCode(), report.edition(), report);
        }
        RuntimeLicenseMode mode = gate instanceof CommunityFeatureGate
                ? RuntimeLicenseMode.COMMUNITY : RuntimeLicenseMode.CUSTOM;
        return new RuntimeLicenseStatusVO(mode.getCode(), gate.edition(), null);
    }
}

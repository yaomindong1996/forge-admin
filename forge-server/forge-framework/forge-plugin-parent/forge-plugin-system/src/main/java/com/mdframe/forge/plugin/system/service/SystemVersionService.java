package com.mdframe.forge.plugin.system.service;

import com.mdframe.forge.plugin.system.vo.SystemVersionVO;
import com.mdframe.forge.starter.plugin.ForgeVersion;
import com.mdframe.forge.starter.plugin.feature.FeatureGate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemVersionService {
    private final ObjectProvider<BuildProperties> buildProperties;
    private final FeatureGate featureGate;

    public SystemVersionVO current() {
        BuildProperties build = buildProperties.getIfAvailable();
        // IDE 未生成宿主 build-info 时仅报告已知核心版本，不能用核心版本冒充宿主版本。
        if (build == null) {
            return new SystemVersionVO(null, ForgeVersion.CURRENT, featureGate.edition(), null);
        }
        String commit = build.get("commit");
        String safeCommit = commit != null && commit.matches("[a-fA-F0-9]{7,64}") ? commit : null;
        String time = build.getTime() == null ? null : build.getTime().toString();
        return new SystemVersionVO(build.getVersion(), ForgeVersion.CURRENT, featureGate.edition(),
                new SystemVersionVO.Build(build.getArtifact(), time, safeCommit));
    }
}

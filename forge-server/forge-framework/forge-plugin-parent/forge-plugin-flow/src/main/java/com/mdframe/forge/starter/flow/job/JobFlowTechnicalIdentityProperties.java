package com.mdframe.forge.starter.flow.job;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 定时任务启动流程时使用的服务端技术身份。
 */
@Data
@Component
@ConfigurationProperties(prefix = "forge.flow.job.technical-identity")
public class JobFlowTechnicalIdentityProperties {

    private Long tenantId;

    private String userId;

    private String userName;

    private Long activeOrgId;

    private String activeOrgName;

    public void requireConfigured() {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("未配置任务流程技术身份tenantId");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("未配置任务流程技术身份userId");
        }
        if (activeOrgId == null || activeOrgId <= 0) {
            throw new IllegalStateException("未配置任务流程技术身份activeOrgId");
        }
    }
}

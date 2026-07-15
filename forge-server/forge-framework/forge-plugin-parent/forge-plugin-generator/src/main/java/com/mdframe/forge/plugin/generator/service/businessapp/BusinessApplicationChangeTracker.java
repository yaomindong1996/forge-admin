package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 将受管资产变更传播为应用未发布变更，不反向依赖具体资产服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationChangeTracker {

    private final BusinessApplicationMapper applicationMapper;

    public void markApplicationChanged(Long applicationId) {
        if (applicationId != null) {
            applicationMapper.markChanged(resolveTenantId(), applicationId);
        }
    }

    public void markObjectChanged(Long objectId) {
        if (objectId != null) {
            applicationMapper.markChangedByObjectId(resolveTenantId(), objectId);
        }
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? 1L : tenantId;
        } catch (Exception e) {
            return 1L;
        }
    }
}

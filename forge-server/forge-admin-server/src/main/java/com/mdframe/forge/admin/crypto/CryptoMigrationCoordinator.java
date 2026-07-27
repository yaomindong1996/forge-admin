package com.mdframe.forge.admin.crypto;

import com.mdframe.forge.plugin.data.service.DataConnectionCryptoMigrationService;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeCryptoMigrationService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.crypto.migration.CryptoMigrationReport;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Admin 应用层迁移编排，避免 Starter 反向依赖业务插件。
 */
@Service
@RequiredArgsConstructor
public class CryptoMigrationCoordinator {

    private static final String SCOPE = "ALL";

    private final DataConnectionCryptoMigrationService dataConnectionMigrationService;
    private final LowcodeCryptoMigrationService lowcodeMigrationService;

    public CryptoMigrationReport inventory(CryptoMigrationRequest request) {
        CryptoMigrationRequest effective = request == null ? new CryptoMigrationRequest() : request;
        Long tenantId = currentTenantId();
        CryptoMigrationReport report = CryptoMigrationReport.of(tenantId, SCOPE);
        if (includeDataConnections(effective)) {
            report.merge(dataConnectionMigrationService.inventory(tenantId));
        }
        if (includeLowcode(effective)) {
            report.merge(lowcodeMigrationService.inventory(tenantId, effective.getConfigKeys()));
        }
        return report;
    }

    public CryptoMigrationReport execute(CryptoMigrationRequest request) {
        if (request == null) {
            throw new BusinessException("迁移请求不能为空");
        }
        if (StringUtils.isBlank(request.getExpectedActiveKeyId())) {
            throw new BusinessException("expectedActiveKeyId不能为空");
        }
        boolean dryRun = !Boolean.FALSE.equals(request.getDryRun());
        Long tenantId = currentTenantId();
        validateAllScopes(request);
        CryptoMigrationReport report = CryptoMigrationReport.of(tenantId, SCOPE);
        report.setExpectedActiveKeyId(request.getExpectedActiveKeyId());
        report.setDryRun(dryRun);
        report.setBatchSize(request.getBatchSize());
        if (includeDataConnections(request)) {
            report.merge(dataConnectionMigrationService.migrate(
                    tenantId, request.getExpectedActiveKeyId(), request.getBatchSize(), dryRun));
        }
        if (includeLowcode(request)) {
            List<String> configKeys = request.getConfigKeys();
            if (configKeys == null || configKeys.isEmpty()) {
                throw new BusinessException("低代码密文迁移必须显式指定configKeys；如只迁移数据连接，请设置includeLowcode=false");
            }
            report.merge(lowcodeMigrationService.migrate(
                    tenantId, configKeys, request.getExpectedActiveKeyId(), request.getBatchSize(), dryRun));
        }
        return report;
    }

    private void validateAllScopes(CryptoMigrationRequest request) {
        if (includeDataConnections(request)) {
            dataConnectionMigrationService.validateMigrationRequest(request.getExpectedActiveKeyId());
        }
        if (includeLowcode(request)) {
            lowcodeMigrationService.validateMigrationRequest(
                    request.getConfigKeys(), request.getExpectedActiveKeyId());
        }
    }

    private Long currentTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("当前租户上下文不能为空");
        }
        return tenantId;
    }

    private boolean includeDataConnections(CryptoMigrationRequest request) {
        return request.getIncludeDataConnections() == null || Boolean.TRUE.equals(request.getIncludeDataConnections());
    }

    private boolean includeLowcode(CryptoMigrationRequest request) {
        return request.getIncludeLowcode() == null || Boolean.TRUE.equals(request.getIncludeLowcode());
    }
}

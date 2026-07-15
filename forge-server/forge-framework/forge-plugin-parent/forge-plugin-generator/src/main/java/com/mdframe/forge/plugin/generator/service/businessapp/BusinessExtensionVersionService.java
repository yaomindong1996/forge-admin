package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionVersion;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionVersionDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionVersionMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessExtensionDiffVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessExtensionVersionVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 扩展不可变内容版本、差异和回滚服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessExtensionVersionService
        extends ServiceImpl<BusinessExtensionVersionMapper, AiBusinessExtensionVersion> {

    private final BusinessExtensionMapper extensionMapper;
    private final BusinessExtensionLockService lockService;
    private final ObjectMapper objectMapper;
    private final BusinessExtensionStateMachine stateMachine;
    private final BusinessApplicationChangeTracker applicationChangeTracker;

    public List<BusinessExtensionVersionVO> list(Long extensionId) {
        requireExtension(extensionId);
        return baseMapper.selectVersions(resolveTenantId(), extensionId).stream().map(this::toVO).toList();
    }

    public BusinessExtensionDiffVO diff(Long extensionId, Integer baseVersion, Integer targetVersion) {
        AiBusinessExtension extension = requireExtension(extensionId);
        int target = targetVersion == null ? extension.getDraftVersion() : targetVersion;
        int base = baseVersion == null ? Math.max(1, target - 1) : baseVersion;
        AiBusinessExtensionVersion baseRow = requireVersion(extensionId, base);
        AiBusinessExtensionVersion targetRow = requireVersion(extensionId, target);
        BusinessExtensionDiffVO vo = new BusinessExtensionDiffVO();
        vo.setBaseVersion(base);
        vo.setTargetVersion(target);
        vo.setBaseContent(baseRow.getContent());
        vo.setTargetContent(targetRow.getContent());
        vo.setBaseConfigJson(baseRow.getConfigJson());
        vo.setTargetConfigJson(targetRow.getConfigJson());
        vo.setChanged(!Objects.equals(baseRow.getContentHash(), targetRow.getContentHash()));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer saveDraft(Long extensionId, BusinessExtensionVersionDTO dto) {
        if (dto == null) {
            throw new BusinessException("扩展版本不能为空");
        }
        lockService.assertOwned(extensionId, dto.getLockToken());
        AiBusinessExtension extension = requireExtension(extensionId);
        Integer versionNo = nextVersionNo(extensionId);
        AiBusinessExtensionVersion version = buildVersion(extension, versionNo, dto.getContent(),
                dto.getProcessedContent(), dto.getConfigJson(), dto.getChangeSummary());
        save(version);
        String nextStatus = stateMachine.statusAfterContentChange(extension.getStatus());
        if (extensionMapper.updateDraftVersion(resolveTenantId(), extensionId, versionNo, nextStatus) == 0) {
            throw new BusinessException("扩展草稿版本更新失败");
        }
        applicationChangeTracker.markApplicationChanged(extension.getApplicationId());
        return versionNo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer rollback(Long extensionId, Integer versionNo, String lockToken) {
        lockService.assertOwned(extensionId, lockToken);
        AiBusinessExtension extension = requireExtension(extensionId);
        AiBusinessExtensionVersion source = requireVersion(extensionId, versionNo);
        Integer nextVersion = nextVersionNo(extensionId);
        AiBusinessExtensionVersion rollback = buildVersion(extension, nextVersion, source.getContent(),
                source.getProcessedContent(), source.getConfigJson(), "从 v" + versionNo + " 回滚生成新草稿");
        save(rollback);
        if (extensionMapper.updateDraftVersion(resolveTenantId(), extensionId, nextVersion,
                stateMachine.statusAfterContentChange(extension.getStatus())) == 0) {
            throw new BusinessException("扩展回滚草稿更新失败");
        }
        applicationChangeTracker.markApplicationChanged(extension.getApplicationId());
        return nextVersion;
    }

    public AiBusinessExtensionVersion requireVersion(Long extensionId, Integer versionNo) {
        if (versionNo == null || versionNo < 1) {
            throw new BusinessException("扩展版本号不正确");
        }
        AiBusinessExtensionVersion version = baseMapper.selectVersion(resolveTenantId(), extensionId, versionNo);
        if (version == null) {
            throw new BusinessException("扩展版本不存在: v" + versionNo);
        }
        return version;
    }

    private Integer nextVersionNo(Long extensionId) {
        Integer maxVersion = baseMapper.selectMaxVersionNo(resolveTenantId(), extensionId);
        return maxVersion == null ? 1 : maxVersion + 1;
    }

    private AiBusinessExtension requireExtension(Long extensionId) {
        if (extensionId == null) {
            throw new BusinessException("业务扩展ID不能为空");
        }
        AiBusinessExtension extension = extensionMapper.selectEntityById(resolveTenantId(), extensionId);
        if (extension == null) {
            throw new BusinessException("业务扩展不存在");
        }
        return extension;
    }

    private AiBusinessExtensionVersion buildVersion(AiBusinessExtension extension, Integer versionNo,
                                                      String content, String processedContent,
                                                      String configJson, String changeSummary) {
        String normalizedContent = BusinessExtensionSecurityPolicy.normalizeContent(content);
        String normalizedProcessed = BusinessExtensionSecurityPolicy.normalizeProcessedContent(processedContent);
        String normalizedConfig = BusinessExtensionSecurityPolicy.normalizeConfig(objectMapper, configJson);
        AiBusinessExtensionVersion version = new AiBusinessExtensionVersion();
        version.setTenantId(resolveTenantId());
        version.setExtensionId(extension.getId());
        version.setVersionNo(versionNo);
        version.setContent(normalizedContent);
        version.setProcessedContent(normalizedProcessed);
        version.setConfigJson(normalizedConfig);
        version.setContentHash(BusinessExtensionSecurityPolicy.contentHash(
                normalizedContent, normalizedProcessed, normalizedConfig));
        version.setValidationPassed(0);
        version.setTestPassed(0);
        version.setChangeSummary(StringUtils.defaultIfBlank(StringUtils.trimToNull(changeSummary), "保存扩展草稿"));
        return version;
    }

    private BusinessExtensionVersionVO toVO(AiBusinessExtensionVersion version) {
        BusinessExtensionVersionVO vo = new BusinessExtensionVersionVO();
        vo.setId(version.getId());
        vo.setExtensionId(version.getExtensionId());
        vo.setVersionNo(version.getVersionNo());
        vo.setContent(version.getContent());
        vo.setProcessedContent(version.getProcessedContent());
        vo.setConfigJson(version.getConfigJson());
        vo.setContentHash(version.getContentHash());
        vo.setValidationPassed(version.getValidationPassed());
        vo.setValidationSummary(version.getValidationSummary());
        vo.setTestPassed(version.getTestPassed());
        vo.setTestSummary(version.getTestSummary());
        vo.setChangeSummary(version.getChangeSummary());
        vo.setCreateBy(version.getCreateBy());
        vo.setCreateTime(version.getCreateTime());
        return vo;
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

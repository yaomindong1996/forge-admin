package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessExtensionHook;
import com.mdframe.forge.plugin.generator.constant.BusinessExtensionStatus;
import com.mdframe.forge.plugin.generator.constant.BusinessExtensionType;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionVersion;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionVersionMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessExtensionVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 业务扩展身份、归属和基础 CRUD 服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessExtensionService extends ServiceImpl<BusinessExtensionMapper, AiBusinessExtension> {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{1,63}$");
    private static final Set<String> SCOPE_TYPES = Set.of("APPLICATION", "OBJECT", "ENTRY", "PAGE", "COMPONENT");
    private static final Set<String> RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");

    private final BusinessExtensionVersionMapper versionMapper;
    private final ObjectMapper objectMapper;
    private final BusinessApplicationChangeTracker applicationChangeTracker;

    public Page<BusinessExtensionVO> page(Integer pageNum, Integer pageSize, BusinessExtensionQueryDTO query) {
        Page<BusinessExtensionVO> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return baseMapper.selectExtensionPage(page, resolveTenantId(), normalizeQuery(query));
    }

    public List<BusinessExtensionVO> list(BusinessExtensionQueryDTO query) {
        return baseMapper.selectExtensionList(resolveTenantId(), normalizeQuery(query));
    }

    public List<BusinessExtensionVO> listWorkspaceSummaries(Long applicationId) {
        if (applicationId == null) {
            throw new BusinessException("所属业务应用不能为空");
        }
        return baseMapper.selectWorkspaceSummaries(resolveTenantId(), applicationId);
    }

    public BusinessExtensionVO detail(Long id) {
        BusinessExtensionVO extension = baseMapper.selectExtensionDetail(resolveTenantId(), id);
        if (extension == null) {
            throw new BusinessException("业务扩展不存在");
        }
        return extension;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(BusinessExtensionDTO dto) {
        if (dto == null) {
            throw new BusinessException("业务扩展不能为空");
        }
        AiBusinessExtension extension = new AiBusinessExtension();
        copyMetadata(dto, extension, true);
        extension.setStatus(BusinessExtensionStatus.DRAFT);
        extension.setDraftVersion(1);
        extension.setEnabledVersion(null);
        save(extension);

        AiBusinessExtensionVersion version = buildVersion(extension, 1, dto.getContent(),
                dto.getProcessedContent(), dto.getConfigJson(), dto.getChangeSummary());
        versionMapper.insert(version);
        applicationChangeTracker.markApplicationChanged(extension.getApplicationId());
        return extension.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(BusinessExtensionDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("业务扩展ID不能为空");
        }
        AiBusinessExtension extension = requireEntity(dto.getId());
        copyMetadata(dto, extension, false);
        extension.setStatus(BusinessExtensionStatus.DRAFT);
        updateById(extension);
        applicationChangeTracker.markApplicationChanged(extension.getApplicationId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiBusinessExtension extension = requireEntity(id);
        if (BusinessExtensionStatus.ENABLED.equals(extension.getStatus())) {
            throw new BusinessException("已启用扩展不能删除，请先停用");
        }
        removeById(extension.getId());
        applicationChangeTracker.markApplicationChanged(extension.getApplicationId());
    }

    public AiBusinessExtension requireEntity(Long id) {
        if (id == null) {
            throw new BusinessException("业务扩展ID不能为空");
        }
        AiBusinessExtension extension = baseMapper.selectEntityById(resolveTenantId(), id);
        if (extension == null) {
            throw new BusinessException("业务扩展不存在");
        }
        return extension;
    }

    private void copyMetadata(BusinessExtensionDTO dto, AiBusinessExtension extension, boolean create) {
        Long tenantId = resolveTenantId();
        if (dto.getApplicationId() == null) {
            throw new BusinessException("所属业务应用不能为空");
        }
        if (!create && !dto.getApplicationId().equals(extension.getApplicationId())) {
            throw new BusinessException("扩展创建后不能移动到其他业务应用");
        }
        AiBusinessApplication application = baseMapper.selectApplicationById(tenantId, dto.getApplicationId());
        if (application == null) {
            throw new BusinessException("所属业务应用不存在或无权访问");
        }
        if (dto.getObjectId() != null
                && baseMapper.countApplicationObject(tenantId, dto.getApplicationId(), dto.getObjectId()) == 0L) {
            throw new BusinessException("业务对象不属于当前业务应用");
        }
        if (dto.getEntryId() != null
                && baseMapper.countApplicationEntry(tenantId, dto.getApplicationId(), dto.getEntryId()) == 0L) {
            throw new BusinessException("访问入口不属于当前业务应用");
        }

        String requestedCode = StringUtils.trimToNull(dto.getExtensionCode());
        String extensionCode = create ? requestedCode : extension.getExtensionCode();
        if (extensionCode == null || !CODE_PATTERN.matcher(extensionCode).matches()) {
            throw new BusinessException("扩展编码格式不正确（字母开头，仅含字母、数字和下划线，2-64字符）");
        }
        if (!create && requestedCode != null && !StringUtils.equals(requestedCode, extension.getExtensionCode())) {
            throw new BusinessException("扩展编码创建后不能修改");
        }
        if (baseMapper.countByExtensionCode(tenantId, dto.getApplicationId(), extensionCode,
                create ? null : extension.getId()) > 0L) {
            throw new BusinessException("扩展编码已存在: " + extensionCode);
        }

        String extensionName = StringUtils.trimToNull(dto.getExtensionName());
        if (extensionName == null) {
            throw new BusinessException("扩展名称不能为空");
        }
        String type = normalize(dto.getExtensionType());
        if (!BusinessExtensionType.ENABLED_TYPES.contains(type)) {
            throw new BusinessException("不支持的扩展类型: " + dto.getExtensionType());
        }
        if (!create && !type.equals(extension.getExtensionType())) {
            throw new BusinessException("扩展类型创建后不能修改，请新建扩展");
        }
        String hook = normalize(dto.getHookCode());
        if (!BusinessExtensionHook.ALL.contains(hook)) {
            throw new BusinessException("不支持的扩展钩子: " + dto.getHookCode());
        }
        if (!BusinessExtensionHook.allowedForType(type).contains(hook)) {
            throw new BusinessException("当前扩展类型不支持钩子: " + dto.getHookCode());
        }
        String scopeType = StringUtils.defaultIfBlank(normalize(dto.getScopeType()), defaultScope(dto));
        if (!SCOPE_TYPES.contains(scopeType)) {
            throw new BusinessException("不支持的扩展作用域: " + dto.getScopeType());
        }
        String riskLevel = StringUtils.defaultIfBlank(normalize(dto.getRiskLevel()), "MEDIUM");
        if (!RISK_LEVELS.contains(riskLevel)) {
            throw new BusinessException("不支持的扩展风险级别: " + dto.getRiskLevel());
        }
        String failurePolicy = StringUtils.defaultIfBlank(normalize(dto.getFailurePolicy()), "BLOCK");
        new BusinessExtensionStateMachine().validateFailurePolicy(hook, riskLevel, failurePolicy);
        BusinessExtensionSecurityPolicy.normalizeConfig(objectMapper, dto.getConfigJson());

        extension.setTenantId(tenantId);
        extension.setApplicationId(dto.getApplicationId());
        extension.setObjectId(dto.getObjectId());
        extension.setEntryId(dto.getEntryId());
        extension.setExtensionCode(extensionCode);
        extension.setExtensionName(extensionName);
        extension.setExtensionType(type);
        extension.setHookCode(hook);
        extension.setScopeType(scopeType);
        extension.setScopeKey(StringUtils.trimToNull(dto.getScopeKey()));
        extension.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        extension.setFailurePolicy(failurePolicy);
        extension.setRiskLevel(riskLevel);
        extension.setRemark(StringUtils.trimToNull(dto.getRemark()));
    }

    private AiBusinessExtensionVersion buildVersion(AiBusinessExtension extension, Integer versionNo,
                                                      String content, String processedContent,
                                                      String configJson, String changeSummary) {
        String normalizedContent = BusinessExtensionSecurityPolicy.normalizeContent(content);
        String normalizedProcessed = BusinessExtensionSecurityPolicy.normalizeProcessedContent(processedContent);
        String normalizedConfig = BusinessExtensionSecurityPolicy.normalizeConfig(objectMapper, configJson);
        AiBusinessExtensionVersion version = new AiBusinessExtensionVersion();
        version.setTenantId(extension.getTenantId());
        version.setExtensionId(extension.getId());
        version.setVersionNo(versionNo);
        version.setContent(normalizedContent);
        version.setProcessedContent(normalizedProcessed);
        version.setConfigJson(normalizedConfig);
        version.setContentHash(BusinessExtensionSecurityPolicy.contentHash(
                normalizedContent, normalizedProcessed, normalizedConfig));
        version.setValidationPassed(0);
        version.setTestPassed(0);
        version.setChangeSummary(StringUtils.defaultIfBlank(StringUtils.trimToNull(changeSummary), "创建扩展草稿"));
        return version;
    }

    private BusinessExtensionQueryDTO normalizeQuery(BusinessExtensionQueryDTO query) {
        BusinessExtensionQueryDTO result = query == null ? new BusinessExtensionQueryDTO() : query;
        result.setKeyword(StringUtils.trimToNull(result.getKeyword()));
        result.setExtensionType(StringUtils.trimToNull(normalize(result.getExtensionType())));
        result.setHookCode(StringUtils.trimToNull(normalize(result.getHookCode())));
        result.setStatus(StringUtils.trimToNull(normalize(result.getStatus())));
        if (result.getExtensionType() != null && !BusinessExtensionType.ENABLED_TYPES.contains(result.getExtensionType())) {
            throw new BusinessException("不支持的扩展类型: " + result.getExtensionType());
        }
        if (result.getHookCode() != null && !BusinessExtensionHook.ALL.contains(result.getHookCode())) {
            throw new BusinessException("不支持的扩展钩子: " + result.getHookCode());
        }
        if (result.getStatus() != null && !BusinessExtensionStatus.ALL.contains(result.getStatus())) {
            throw new BusinessException("不支持的扩展状态: " + result.getStatus());
        }
        return result;
    }

    private String defaultScope(BusinessExtensionDTO dto) {
        if (dto.getEntryId() != null) {
            return "ENTRY";
        }
        if (dto.getObjectId() != null) {
            return "OBJECT";
        }
        return "APPLICATION";
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum.longValue();
    }

    private long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 200);
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

package com.mdframe.forge.plugin.generator.service.lowcode;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeDomain;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigRenderVO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAppDraftDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeMoveDomainDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeObjectSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePolicySchema;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeAppDetailVO;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * 低代码应用草稿与详情服务。
 */
@Service
@RequiredArgsConstructor
public class LowcodeAppService {

    private static final Pattern CONFIG_KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");
    private static final Pattern OBJECT_CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,47}$");
    private static final String GENERAL_DOMAIN_CODE = "general";

    private final ObjectMapper objectMapper;
    private final AiCrudConfigService configService;
    private final AiCrudConfigMapper configMapper;
    private final LowcodeSchemaValidator schemaValidator;
    private final LowcodeDomainService domainService;
    private final LowcodePolicyService policyService;
    private final LowcodeModelSchemaNormalizer schemaNormalizer;

    public Page<LowcodeAppDetailVO> page(PageQuery pageQuery, String keyword, String publishStatus,
                                         Long domainId, String domainCode, Boolean generalDomain) {
        Page<AiCrudConfig> configPage = configMapper.selectLowcodePage(
                new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()),
                resolveTenantId(),
                StringUtils.trimToNull(keyword),
                StringUtils.trimToNull(publishStatus),
                domainId,
                StringUtils.trimToNull(domainCode),
                generalDomain);
        Page<LowcodeAppDetailVO> result = new Page<>(configPage.getCurrent(), configPage.getSize(), configPage.getTotal());
        result.setRecords(configPage.getRecords().stream().map(this::toDetailVO).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(LowcodeAppDraftDTO dto) {
        if (dto == null) {
            throw new BusinessException("草稿不能为空");
        }
        AiCrudConfig config = dto.getId() == null ? createDraft(dto) : updateDraft(dto);
        return config.getId();
    }

    public LowcodeAppDetailVO getDetail(Long id) {
        return toDetailVO(requireConfig(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireConfig(id);
        configService.deleteConfig(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void moveDomain(Long id, LowcodeMoveDomainDTO dto) {
        if (dto == null) {
            throw new BusinessException("迁移目标不能为空");
        }
        AiCrudConfig config = requireConfig(id);
        AiLowcodeDomain domain = domainService.requireEnabledDomain(dto.getDomainId());
        LowcodeModelSchema modelSchema = readModelSchema(config);
        String objectCode = resolveObjectCode(dto.getObjectCode(), modelSchema, config);
        String objectName = resolveObjectName(dto.getObjectName(), modelSchema, config);
        applyDomainFields(config, domain, objectCode, objectName);
        enrichModelSchema(config, modelSchema, domain, objectCode, objectName);
        config.setModelSchema(writeJson(modelSchema, "modelSchema"));
        configService.updateById(config);
    }

    public AiCrudConfigRenderVO preview(Long id, LowcodeAppDraftDTO draft) {
        AiCrudConfig existing = id == null ? null : requireConfig(id);
        String configKey = resolveConfigKey(existing, draft);
        DomainAssignment assignment = resolveDomainAssignment(existing, draft, false);
        LowcodeModelSchema modelSchema = resolveModelSchema(existing, draft);
        enrichModelSchema(existing, modelSchema, assignment.domain(), assignment.objectCode(), assignment.objectName());
        LowcodePageSchema pageSchema = resolvePageSchema(existing, draft, modelSchema);
        schemaValidator.validatePage(pageSchema, modelSchema);

        AiCrudConfig previewConfig = new AiCrudConfig();
        applyDomainFields(previewConfig, assignment.domain(), assignment.objectCode(), assignment.objectName());
        previewConfig.setConfigKey(configKey);
        previewConfig.setTableName(modelSchema.getTableName());
        previewConfig.setTableComment(modelSchema.getBusinessName());
        previewConfig.setAppName(resolveAppName(existing, draft, modelSchema));
        previewConfig.setMenuName(resolveMenuName(existing, draft, previewConfig.getAppName()));
        previewConfig.setMode("CONFIG");
        previewConfig.setBuildMode("LOWCODE");
        previewConfig.setStatus("0");
        previewConfig.setPublishStatus("DRAFT");
        previewConfig.setLayoutType(StringUtils.defaultIfBlank(pageSchema.getLayoutType(), "simple-crud"));
        previewConfig.setModelSchema(writeJson(modelSchema, "modelSchema"));
        previewConfig.setPageSchema(writeJson(pageSchema, "pageSchema"));
        return configService.buildRenderConfig(previewConfig);
    }

    private String resolveMenuName(AiCrudConfig existing, LowcodeAppDraftDTO draft, String fallback) {
        if (draft != null && StringUtils.isNotBlank(draft.getMenuName())) {
            return draft.getMenuName();
        }
        if (existing != null && StringUtils.isNotBlank(existing.getMenuName())) {
            return existing.getMenuName();
        }
        return fallback;
    }

    AiCrudConfig requireConfig(Long id) {
        if (id == null) {
            throw new BusinessException("低代码应用ID不能为空");
        }
        AiCrudConfig config = configService.getById(id);
        if (config == null || !"LOWCODE".equals(config.getBuildMode())) {
            throw new BusinessException("低代码应用不存在");
        }
        return config;
    }

    LowcodeModelSchema readModelSchema(AiCrudConfig config) {
        LowcodeModelSchema modelSchema = readJson(config.getModelSchema(), LowcodeModelSchema.class, "modelSchema");
        AiLowcodeDomain domain = resolveDomain(config.getDomainId(), config.getDomainCode(), false);
        String objectCode = resolveObjectCode(config.getObjectCode(), modelSchema, config);
        String objectName = resolveObjectName(config.getObjectName(), modelSchema, config);
        enrichModelSchema(config, modelSchema, domain, objectCode, objectName);
        return modelSchema;
    }

    LowcodePageSchema readPageSchema(AiCrudConfig config) {
        return readJson(config.getPageSchema(), LowcodePageSchema.class, "pageSchema");
    }

    LowcodePageSchema buildDefaultPageSchema(LowcodeModelSchema modelSchema) {
        LowcodePageSchema pageSchema = new LowcodePageSchema();
        String appType = StringUtils.defaultIfBlank(modelSchema.getAppType(), "SINGLE").toUpperCase(Locale.ROOT);
        boolean treeApp = "TREE".equals(appType);
        pageSchema.setLayoutType(treeApp ? "tree-crud" : "MASTER_DETAIL".equals(appType) ? "master-detail-crud" : "simple-crud");
        pageSchema.getZones().add(buildZone("search", "search-form", true,
                modelSchema.getFields().stream()
                        .filter(field -> Boolean.TRUE.equals(field.getSearchable()))
                        .map(LowcodeFieldSchema::getField)
                        .toList()));
        pageSchema.getZones().add(buildZone("table", "data-table", true,
                modelSchema.getFields().stream()
                        .filter(field -> field.getListVisible() == null || Boolean.TRUE.equals(field.getListVisible()))
                        .map(LowcodeFieldSchema::getField)
                        .toList()));
        if (treeApp) {
            LowcodePageZone tableZone = pageSchema.getZones().stream()
                    .filter(zone -> "table".equals(zone.getZoneKey()))
                    .findFirst()
                    .orElse(null);
            if (tableZone != null) {
                tableZone.getProps().put("treeConfig", buildDefaultTreeConfig(modelSchema));
            }
        }
        pageSchema.getZones().add(buildZone("edit", "edit-form", true,
                modelSchema.getFields().stream()
                        .filter(field -> field.getFormVisible() == null || Boolean.TRUE.equals(field.getFormVisible()))
                        .map(LowcodeFieldSchema::getField)
                        .toList()));
        return pageSchema;
    }

    private Map<String, Object> buildDefaultTreeConfig(LowcodeModelSchema modelSchema) {
        String parentField = modelSchema.getTreeConfig() != null
                ? modelSchema.getTreeConfig().getParentField()
                : null;
        String labelField = modelSchema.getTreeConfig() != null
                ? modelSchema.getTreeConfig().getLabelField()
                : null;
        parentField = StringUtils.defaultIfBlank(parentField, modelSchema.getFields().stream()
                .map(LowcodeFieldSchema::getField)
                .filter(field -> "parentId".equals(field) || "pid".equals(field) || "parentCode".equals(field))
                .findFirst()
                .orElse("parentId"));
        labelField = StringUtils.defaultIfBlank(labelField, modelSchema.getFields().stream()
                .map(LowcodeFieldSchema::getField)
                .filter(field -> "name".equals(field) || "title".equals(field) || "label".equals(field))
                .findFirst()
                .orElseGet(() -> modelSchema.getFields().isEmpty() ? "name" : modelSchema.getFields().get(0).getField()));
        return Map.of(
                "enabled", true,
                "keyField", "id",
                "parentField", parentField,
                "labelField", labelField,
                "filterField", parentField,
                "targetField", "id",
                "childrenField", "children",
                "treeTitle", StringUtils.defaultIfBlank(modelSchema.getBusinessName(), "树形导航"),
                "loadMode", "full"
        );
    }

    String writeJson(Object value, String fieldName) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(fieldName + "序列化失败");
        }
    }

    private AiCrudConfig createDraft(LowcodeAppDraftDTO dto) {
        validateConfigKey(dto.getConfigKey());
        if (configService.getByConfigKey(dto.getConfigKey()) != null) {
            throw new BusinessException("configKey已存在: " + dto.getConfigKey());
        }
        DomainAssignment assignment = resolveDomainAssignment(null, dto, true);
        LowcodeModelSchema modelSchema = resolveModelSchema(null, dto);
        enrichModelSchema(null, modelSchema, assignment.domain(), assignment.objectCode(), assignment.objectName());
        LowcodePageSchema pageSchema = resolvePageSchema(null, dto, modelSchema);
        schemaValidator.validatePage(pageSchema, modelSchema);

        AiCrudConfig config = new AiCrudConfig();
        config.setTenantId(resolveTenantId());
        applyDomainFields(config, assignment.domain(), assignment.objectCode(), assignment.objectName());
        config.setConfigKey(dto.getConfigKey());
        config.setTableName(modelSchema.getTableName());
        config.setTableComment(modelSchema.getBusinessName());
        config.setAppName(resolveAppName(null, dto, modelSchema));
        config.setMenuName(StringUtils.defaultIfBlank(dto.getMenuName(), config.getAppName()));
        config.setMenuParentId(dto.getMenuParentId());
        config.setMenuSort(dto.getMenuSort() != null ? dto.getMenuSort() : 0);
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setStatus("0");
        config.setPublishStatus("DRAFT");
        config.setDraftVersion(1);
        config.setPublishedVersion(0);
        config.setLayoutType(StringUtils.defaultIfBlank(pageSchema.getLayoutType(), "simple-crud"));
        config.setModelSchema(writeJson(modelSchema, "modelSchema"));
        config.setPageSchema(writeJson(pageSchema, "pageSchema"));
        configService.save(config);
        return config;
    }

    private AiCrudConfig updateDraft(LowcodeAppDraftDTO dto) {
        AiCrudConfig config = requireConfig(dto.getId());
        if (StringUtils.isNotBlank(dto.getConfigKey()) && !dto.getConfigKey().equals(config.getConfigKey())) {
            throw new BusinessException("configKey创建后不允许修改");
        }
        DomainAssignment assignment = resolveDomainAssignment(config, dto, true);
        LowcodeModelSchema modelSchema = resolveModelSchema(config, dto);
        enrichModelSchema(config, modelSchema, assignment.domain(), assignment.objectCode(), assignment.objectName());
        LowcodePageSchema pageSchema = resolvePageSchema(config, dto, modelSchema);
        schemaValidator.validatePage(pageSchema, modelSchema);

        applyDomainFields(config, assignment.domain(), assignment.objectCode(), assignment.objectName());
        config.setTableName(modelSchema.getTableName());
        config.setTableComment(modelSchema.getBusinessName());
        config.setAppName(resolveAppName(config, dto, modelSchema));
        if (dto.getMenuName() != null) {
            config.setMenuName(dto.getMenuName());
        }
        if (dto.getMenuParentId() != null) {
            config.setMenuParentId(dto.getMenuParentId());
        }
        if (dto.getMenuSort() != null) {
            config.setMenuSort(dto.getMenuSort());
        }
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setStatus("0");
        if (StringUtils.isBlank(config.getPublishStatus())) {
            config.setPublishStatus("DRAFT");
        }
        config.setDraftVersion((config.getDraftVersion() == null ? 0 : config.getDraftVersion()) + 1);
        config.setLayoutType(StringUtils.defaultIfBlank(pageSchema.getLayoutType(), "simple-crud"));
        config.setModelSchema(writeJson(modelSchema, "modelSchema"));
        config.setPageSchema(writeJson(pageSchema, "pageSchema"));
        configService.updateById(config);
        return config;
    }

    private LowcodeModelSchema resolveModelSchema(AiCrudConfig existing, LowcodeAppDraftDTO draft) {
        if (draft != null && draft.getModelSchema() != null) {
            return normalizeModelCollections(draft.getModelSchema());
        }
        if (existing != null && StringUtils.isNotBlank(existing.getModelSchema())) {
            return readModelSchema(existing);
        }
        throw new BusinessException("数据模型不能为空");
    }

    private LowcodePageSchema resolvePageSchema(AiCrudConfig existing, LowcodeAppDraftDTO draft,
                                                LowcodeModelSchema modelSchema) {
        if (draft != null && draft.getPageSchema() != null) {
            return draft.getPageSchema();
        }
        if (existing != null && StringUtils.isNotBlank(existing.getPageSchema())) {
            return readPageSchema(existing);
        }
        return buildDefaultPageSchema(modelSchema);
    }

    private String resolveConfigKey(AiCrudConfig existing, LowcodeAppDraftDTO draft) {
        String configKey = draft != null ? StringUtils.trimToNull(draft.getConfigKey()) : null;
        if (configKey == null && existing != null) {
            configKey = existing.getConfigKey();
        }
        validateConfigKey(configKey);
        return configKey;
    }

    private String resolveAppName(AiCrudConfig existing, LowcodeAppDraftDTO draft, LowcodeModelSchema modelSchema) {
        if (draft != null && StringUtils.isNotBlank(draft.getAppName())) {
            return draft.getAppName();
        }
        if (existing != null && StringUtils.isNotBlank(existing.getAppName())) {
            return existing.getAppName();
        }
        return StringUtils.defaultIfBlank(modelSchema.getBusinessName(), modelSchema.getTableName());
    }

    private void validateConfigKey(String configKey) {
        if (StringUtils.isBlank(configKey) || !CONFIG_KEY_PATTERN.matcher(configKey).matches()) {
            throw new BusinessException("configKey格式不正确（小写字母开头，仅含小写字母+数字+下划线，2-64字符）");
        }
    }

    private DomainAssignment resolveDomainAssignment(AiCrudConfig existing, LowcodeAppDraftDTO draft,
                                                     boolean requireEnabled) {
        Long domainId = draft != null ? draft.getDomainId() : null;
        String domainCode = draft != null ? StringUtils.trimToNull(draft.getDomainCode()) : null;
        if (domainId == null && existing != null) {
            domainId = existing.getDomainId();
        }
        if (domainCode == null && existing != null) {
            domainCode = existing.getDomainCode();
        }
        AiLowcodeDomain domain = resolveDomain(domainId, domainCode, requireEnabled);
        LowcodeModelSchema modelSchema = draft != null ? draft.getModelSchema() : null;
        String objectCode = draft != null ? StringUtils.trimToNull(draft.getObjectCode()) : null;
        String objectName = draft != null ? StringUtils.trimToNull(draft.getObjectName()) : null;
        objectCode = resolveObjectCode(objectCode, modelSchema, existing);
        objectName = resolveObjectName(objectName, modelSchema, existing);
        return new DomainAssignment(domain, objectCode, objectName);
    }

    private AiLowcodeDomain resolveDomain(Long domainId, String domainCode, boolean requireEnabled) {
        AiLowcodeDomain domain;
        if (domainId != null) {
            domain = requireEnabled ? domainService.requireEnabledDomain(domainId) : domainService.requireDomain(domainId);
        } else if (StringUtils.isNotBlank(domainCode)) {
            domain = domainService.getByCode(domainCode);
            if (domain == null) {
                throw new BusinessException("业务领域不存在: " + domainCode);
            }
            if (requireEnabled && !LowcodeDomainService.STATUS_ENABLED.equals(domain.getStatus())) {
                throw new BusinessException("业务领域已停用，不能创建或迁入应用");
            }
        } else {
            domain = domainService.getByCode(GENERAL_DOMAIN_CODE);
            if (domain == null) {
                throw new BusinessException("通用业务域不存在，请先执行低代码业务领域迁移脚本");
            }
        }
        return domain;
    }

    private String resolveObjectCode(String objectCode, LowcodeModelSchema modelSchema, AiCrudConfig existing) {
        String resolved = StringUtils.trimToNull(objectCode);
        if (resolved == null && modelSchema != null && modelSchema.getObject() != null) {
            resolved = StringUtils.trimToNull(modelSchema.getObject().getCode());
        }
        if (resolved == null && existing != null) {
            resolved = StringUtils.trimToNull(existing.getObjectCode());
        }
        if (resolved == null && existing != null) {
            resolved = StringUtils.trimToNull(existing.getConfigKey());
        }
        if (resolved == null && modelSchema != null) {
            resolved = StringUtils.trimToNull(modelSchema.getTableName());
        }
        resolved = normalizeObjectCode(resolved);
        if (StringUtils.isBlank(resolved) || !OBJECT_CODE_PATTERN.matcher(resolved).matches()) {
            throw new BusinessException("业务对象编码格式不正确（小写字母开头，仅含小写字母+数字+下划线，2-48字符）");
        }
        return resolved;
    }

    private String resolveObjectName(String objectName, LowcodeModelSchema modelSchema, AiCrudConfig existing) {
        String resolved = StringUtils.trimToNull(objectName);
        if (resolved == null && modelSchema != null && modelSchema.getObject() != null) {
            resolved = StringUtils.trimToNull(modelSchema.getObject().getName());
        }
        if (resolved == null && existing != null) {
            resolved = StringUtils.trimToNull(existing.getObjectName());
        }
        if (resolved == null && modelSchema != null) {
            resolved = StringUtils.trimToNull(modelSchema.getBusinessName());
        }
        if (resolved == null && existing != null) {
            resolved = StringUtils.firstNonBlank(existing.getAppName(), existing.getTableComment(), existing.getConfigKey());
        }
        if (StringUtils.isBlank(resolved)) {
            throw new BusinessException("业务对象名称不能为空");
        }
        return resolved;
    }

    private String normalizeObjectCode(String value) {
        String normalized = StringUtils.trimToEmpty(value)
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("_+", "_")
                .toLowerCase(Locale.ROOT)
                .replaceAll("^[^a-z]+", "")
                .replaceAll("_+$", "");
        if (normalized.length() > 48) {
            normalized = normalized.substring(0, 48).replaceAll("_+$", "");
        }
        return normalized;
    }

    private void applyDomainFields(AiCrudConfig config, AiLowcodeDomain domain, String objectCode, String objectName) {
        config.setDomainId(domain.getId());
        config.setDomainCode(domain.getDomainCode());
        config.setObjectCode(objectCode);
        config.setObjectName(objectName);
    }

    private void enrichModelSchema(AiCrudConfig existing, LowcodeModelSchema modelSchema,
                                   AiLowcodeDomain domain, String objectCode, String objectName) {
        normalizeModelCollections(modelSchema);
        modelSchema.setSchemaVersion(2);
        LowcodeDomainRef domainRef = modelSchema.getDomain() == null ? new LowcodeDomainRef() : modelSchema.getDomain();
        domainRef.setId(domain.getId());
        domainRef.setCode(domain.getDomainCode());
        domainRef.setName(domain.getDomainName());
        modelSchema.setDomain(domainRef);

        LowcodeObjectSchema object = modelSchema.getObject() == null ? new LowcodeObjectSchema() : modelSchema.getObject();
        object.setCode(objectCode);
        object.setName(objectName);
        if (StringUtils.isBlank(object.getDescription()) && existing != null) {
            object.setDescription(StringUtils.firstNonBlank(existing.getTableComment(), existing.getAppName()));
        }
        modelSchema.setObject(object);
        ensureTableName(existing, modelSchema, domain, objectCode);
        schemaNormalizer.normalizeModelFields(modelSchema, true);
        policyService.normalizeModelSchema(modelSchema);
    }

    private void ensureTableName(AiCrudConfig existing, LowcodeModelSchema modelSchema,
                                 AiLowcodeDomain domain, String objectCode) {
        if (schemaValidator.isValidTableName(modelSchema.getTableName())) {
            return;
        }
        String prefix = StringUtils.defaultIfBlank(domain.getTablePrefix(), "biz_");
        String base = StringUtils.firstNonBlank(
                objectCode,
                modelSchema.getObject() == null ? null : modelSchema.getObject().getCode(),
                existing == null ? null : existing.getConfigKey(),
                "runtime_model");
        modelSchema.setTableName(normalizeTableName(prefix + base));
    }

    private String normalizeTableName(String value) {
        String normalized = StringUtils.defaultString(value)
                .trim()
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .toLowerCase(Locale.ROOT)
                .replaceAll("^[^a-z]+", "")
                .replaceAll("_+$", "");
        if (StringUtils.isBlank(normalized)) {
            normalized = "biz_lowcode_model";
        }
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64).replaceAll("_+$", "");
        }
        return StringUtils.defaultIfBlank(normalized, "biz_lowcode_model");
    }

    private LowcodeModelSchema normalizeModelCollections(LowcodeModelSchema modelSchema) {
        if (modelSchema.getFields() == null) {
            modelSchema.setFields(new ArrayList<>());
        }
        if (modelSchema.getRelations() == null) {
            modelSchema.setRelations(new ArrayList<>());
        }
        if (modelSchema.getPolicies() == null) {
            modelSchema.setPolicies(new LowcodePolicySchema());
        }
        if (modelSchema.getChildren() == null) {
            modelSchema.setChildren(new ArrayList<>());
        }
        return modelSchema;
    }

    private LowcodePageZone buildZone(String zoneKey, String componentKey, boolean enabled, List<String> fieldRefs) {
        LowcodePageZone zone = new LowcodePageZone();
        zone.setZoneKey(zoneKey);
        zone.setComponentKey(componentKey);
        zone.setEnabled(enabled);
        zone.setFieldRefs(fieldRefs);
        return zone;
    }

    private LowcodeAppDetailVO toDetailVO(AiCrudConfig config) {
        LowcodeAppDetailVO vo = new LowcodeAppDetailVO();
        vo.setId(config.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setTableName(config.getTableName());
        vo.setTableComment(config.getTableComment());
        vo.setAppName(config.getAppName());
        vo.setDomainId(config.getDomainId());
        vo.setDomainCode(config.getDomainCode());
        vo.setObjectCode(config.getObjectCode());
        vo.setObjectName(config.getObjectName());
        vo.setMode(config.getMode());
        vo.setBuildMode(config.getBuildMode());
        vo.setStatus(config.getStatus());
        vo.setPublishStatus(config.getPublishStatus());
        vo.setMenuName(config.getMenuName());
        vo.setMenuParentId(config.getMenuParentId());
        vo.setMenuSort(config.getMenuSort());
        vo.setMenuResourceId(config.getMenuResourceId());
        vo.setLayoutType(config.getLayoutType());
        LowcodeModelSchema modelSchema = readModelSchema(config);
        vo.setDomainName(modelSchema.getDomain() == null ? null : modelSchema.getDomain().getName());
        vo.setModelSchema(modelSchema);
        vo.setPageSchema(readJsonObject(config.getPageSchema()));
        vo.setDraftVersion(config.getDraftVersion());
        vo.setPublishedVersion(config.getPublishedVersion());
        vo.setPublishTime(config.getPublishTime());
        vo.setPublishBy(config.getPublishBy());
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }

    private Object readJsonObject(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return readJson(json, Object.class, "JSON");
    }

    private <T> T readJson(String json, Class<T> type, String fieldName) {
        if (StringUtils.isBlank(json)) {
            throw new BusinessException(fieldName + "不能为空");
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new BusinessException(fieldName + "格式不正确");
        }
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }

    private record DomainAssignment(AiLowcodeDomain domain, String objectCode, String objectName) {
    }
}

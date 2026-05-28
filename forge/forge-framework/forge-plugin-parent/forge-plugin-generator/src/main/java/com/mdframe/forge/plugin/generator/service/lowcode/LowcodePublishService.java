package com.mdframe.forge.plugin.generator.service.lowcode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfigVersion;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeDomain;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeModel;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeObjectSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePolicySchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePublishDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigVersionMapper;
import com.mdframe.forge.plugin.generator.mapper.AiLowcodeModelMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeVersionVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 低代码应用发布、版本和回滚服务。
 */
@Service
@RequiredArgsConstructor
public class LowcodePublishService {

    private static final String DEPLOY_SKIP_DDL = "SKIP_DDL";
    private static final String DEPLOY_ONLINE_CREATE_TABLE = "ONLINE_CREATE_TABLE";
    private static final String DDL_PERMISSION = "ai:lowcode:deploy-ddl";
    private static final String GENERAL_DOMAIN_CODE = "general";

    private final ObjectMapper objectMapper;
    private final AiCrudConfigService configService;
    private final LowcodeAppService appService;
    private final LowcodeDomainService domainService;
    private final LowcodeRuntimeConfigBuilder runtimeConfigBuilder;
    private final LowcodeSchemaValidator schemaValidator;
    private final LowcodeDdlService ddlService;
    private final LowcodePolicyService policyService;
    private final MenuRegisterAdapter menuRegisterAdapter;
    private final AiCrudConfigVersionMapper versionMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessAppMapper businessAppMapper;
    private final AiLowcodeModelMapper lowcodeModelMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long id, LowcodePublishDTO dto) {
        AiCrudConfig config = appService.requireConfig(id);
        LowcodeModelSchema modelSchema = resolvePublishModel(config, dto);
        PublishDomainContext domainContext = resolvePublishDomainContext(config, modelSchema);
        applyDomainToModelSchema(modelSchema, domainContext, config.getConfigKey());
        LowcodePageSchema pageSchema = resolvePublishPage(config, dto, modelSchema);
        schemaValidator.validatePage(pageSchema, modelSchema);
        ensureTableReady(modelSchema, dto);
        policyService.validatePublishedPolicies(modelSchema, ddlService.listColumns(modelSchema.getTableName()));

        LowcodeRuntimeConfig runtimeConfig = runtimeConfigBuilder.buildRuntimeConfig(config.getConfigKey(), modelSchema, pageSchema);
        applyRuntimeConfig(config, modelSchema, pageSchema, runtimeConfig);
        applyDomainToConfig(config, domainContext);
        applyMenuConfig(config, dto);
        applyPublishMenuParent(config, dto, domainContext.domain());
        int versionNo = nextVersionNo(config);
        config.setPublishStatus("PUBLISHED");
        config.setPublishedVersion(versionNo);
        config.setPublishTime(LocalDateTime.now());
        config.setPublishBy(SessionHelper.getUserId());

        registerOrUpdateMenu(config);
        configService.updateById(config);
        syncBusinessRuntimeEntry(config, dto, domainContext);
        AiCrudConfigVersion version = createVersion(config, versionNo, "publish",
                dto != null ? dto.getRemark() : null);
        return version.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollback(Long id, Long versionId) {
        AiCrudConfig config = appService.requireConfig(id);
        AiCrudConfigVersion targetVersion = versionMapper.selectVersionById(
                resolveTenantId(config), config.getId(), versionId);
        if (targetVersion == null) {
            throw new BusinessException("版本不存在或不属于当前应用");
        }

        Map<String, Object> snapshot = readSnapshot(targetVersion.getPublishSnapshot());
        LowcodeModelSchema modelSchema = readVersionModel(targetVersion);
        PublishDomainContext domainContext = resolveVersionDomainContext(config, targetVersion, snapshot, modelSchema);
        applyDomainToModelSchema(modelSchema, domainContext, config.getConfigKey());
        LowcodePageSchema pageSchema = readVersionPage(targetVersion);
        LowcodeRuntimeConfig runtimeConfig = runtimeConfigBuilder.buildRuntimeConfig(config.getConfigKey(), modelSchema, pageSchema);
        applyRuntimeConfig(config, modelSchema, pageSchema, runtimeConfig);
        applyDomainToConfig(config, domainContext);
        applyVersionRuntimeFields(config, targetVersion, snapshot, runtimeConfig);
        applySnapshotMenuFields(config, snapshot);

        int versionNo = nextVersionNo(config);
        config.setPublishStatus("PUBLISHED");
        config.setPublishedVersion(versionNo);
        config.setPublishTime(LocalDateTime.now());
        config.setPublishBy(SessionHelper.getUserId());
        registerOrUpdateMenu(config);
        configService.updateById(config);
        syncBusinessRuntimeEntry(config, null, domainContext);
        createVersion(config, versionNo, "rollback", "回滚到版本 " + targetVersion.getVersionNo());
    }

    public List<LowcodeVersionVO> listVersions(Long id) {
        AiCrudConfig config = appService.requireConfig(id);
        return versionMapper.selectByConfigId(resolveTenantId(config), config.getId()).stream()
                .map(this::toVersionVO)
                .toList();
    }

    private LowcodeModelSchema resolvePublishModel(AiCrudConfig config, LowcodePublishDTO dto) {
        if (dto != null && dto.getModelSchema() != null) {
            return dto.getModelSchema();
        }
        return appService.readModelSchema(config);
    }

    private LowcodePageSchema resolvePublishPage(AiCrudConfig config, LowcodePublishDTO dto,
                                                 LowcodeModelSchema modelSchema) {
        if (dto != null && dto.getPageSchema() != null) {
            return dto.getPageSchema();
        }
        if (StringUtils.isNotBlank(config.getPageSchema())) {
            return appService.readPageSchema(config);
        }
        return appService.buildDefaultPageSchema(modelSchema);
    }

    private void ensureTableReady(LowcodeModelSchema modelSchema, LowcodePublishDTO dto) {
        String deployMode = dto != null && StringUtils.isNotBlank(dto.getDeployMode())
                ? dto.getDeployMode()
                : DEPLOY_SKIP_DDL;
        if (DEPLOY_ONLINE_CREATE_TABLE.equals(deployMode)) {
            if (!Boolean.TRUE.equals(dto.getConfirmOnlineDdl())) {
                throw new BusinessException("在线建表发布需要二次确认");
            }
            if (!SessionHelper.hasPermission(DDL_PERMISSION)) {
                throw new BusinessException("缺少在线建表发布权限: " + DDL_PERMISSION);
            }
            ddlService.executeCreateTable(modelSchema);
            return;
        }
        if (!ddlService.tableExists(modelSchema.getTableName())) {
            throw new BusinessException("数据表不存在，请先在数据模型页同步表结构");
        }
        if (!ddlService.hasAutoIncrementPrimaryId(modelSchema.getTableName())) {
            throw new BusinessException("数据表缺少 id 自增主键，请先在数据模型页修正表结构");
        }
    }

    private void applyRuntimeConfig(AiCrudConfig config,
                                    LowcodeModelSchema modelSchema,
                                    LowcodePageSchema pageSchema,
                                    LowcodeRuntimeConfig runtimeConfig) {
        config.setTableName(runtimeConfig.getTableName());
        config.setTableComment(runtimeConfig.getTableComment());
        config.setAppName(StringUtils.defaultIfBlank(config.getAppName(), runtimeConfig.getTableComment()));
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setStatus("0");
        config.setLayoutType(runtimeConfig.getLayoutType());
        config.setModelSchema(appService.writeJson(modelSchema, "modelSchema"));
        config.setPageSchema(appService.writeJson(pageSchema, "pageSchema"));
        config.setSearchSchema(runtimeConfig.getSearchSchema());
        config.setColumnsSchema(runtimeConfig.getColumnsSchema());
        config.setEditSchema(runtimeConfig.getEditSchema());
        config.setApiConfig(runtimeConfig.getApiConfig());
        config.setOptions(runtimeConfig.getOptions());
        config.setDictConfig(runtimeConfig.getDictConfig());
        config.setDesensitizeConfig(runtimeConfig.getDesensitizeConfig());
        config.setEncryptConfig(runtimeConfig.getEncryptConfig());
        config.setTransConfig(runtimeConfig.getTransConfig());
    }

    private void applyMenuConfig(AiCrudConfig config, LowcodePublishDTO dto) {
        if (dto == null) {
            return;
        }
        if (StringUtils.isNotBlank(dto.getMenuName())) {
            config.setMenuName(dto.getMenuName());
        }
        if (dto.getMenuParentId() != null) {
            config.setMenuParentId(dto.getMenuParentId());
        }
        if (dto.getMenuSort() != null) {
            config.setMenuSort(dto.getMenuSort());
        }
    }

    private void registerOrUpdateMenu(AiCrudConfig config) {
        String menuName = StringUtils.defaultIfBlank(config.getMenuName(),
                StringUtils.defaultIfBlank(config.getAppName(), config.getTableComment()));
        Long parentId = config.getMenuParentId() != null
                ? config.getMenuParentId()
                : resolveDomainMenuParentId(resolveDomainForConfig(config));
        if (parentId == null) {
            parentId = menuRegisterAdapter.resolveDefaultLowcodeParentId();
        }
        Integer sort = config.getMenuSort() != null ? config.getMenuSort() : 0;

        if (config.getMenuResourceId() == null) {
            Long menuResourceId = menuRegisterAdapter.registerMenu(menuName, parentId, config.getConfigKey(), sort);
            config.setMenuResourceId(menuResourceId);
        } else {
            menuRegisterAdapter.updateMenu(config.getMenuResourceId(), menuName, parentId, sort);
        }
        config.setMenuName(menuName);
        config.setMenuParentId(parentId);
        config.setMenuSort(sort);
    }

    private void syncBusinessRuntimeEntry(AiCrudConfig config, LowcodePublishDTO dto, PublishDomainContext context) {
        if (config == null || context == null || context.domain() == null || StringUtils.isBlank(config.getConfigKey())) {
            return;
        }
        Long tenantId = resolveTenantId(config);
        AiBusinessApp existingApp = businessAppMapper.selectByConfigKey(tenantId, config.getConfigKey());
        String suiteCode = StringUtils.firstNonBlank(dto != null ? dto.getBusinessSuiteCode() : null,
                existingApp == null ? null : existingApp.getSuiteCode(),
                context.domain().getDomainCode());
        String businessObjectCode = StringUtils.firstNonBlank(dto != null ? dto.getBusinessObjectCode() : null,
                existingApp == null ? null : existingApp.getObjectCode(),
                config.getObjectCode(),
                context.objectCode());
        if (StringUtils.isBlank(suiteCode) || StringUtils.isBlank(businessObjectCode)) {
            return;
        }
        AiBusinessObject businessObject = findBusinessObject(tenantId, suiteCode, businessObjectCode);
        if (businessObject == null) {
            return;
        }

        AiLowcodeModel model = StringUtils.isBlank(config.getObjectCode())
                ? null
                : lowcodeModelMapper.selectByCode(tenantId, context.domain().getId(), config.getObjectCode());
        businessObject.setModelId(model == null ? businessObject.getModelId() : model.getId());
        businessObject.setModelCode(StringUtils.defaultIfBlank(config.getObjectCode(), businessObject.getModelCode()));
        businessObjectMapper.updateById(businessObject);

        AiBusinessApp app = existingApp;
        if (app == null) {
            app = businessAppMapper.selectRuntimeAppByObject(tenantId, suiteCode, businessObject.getObjectCode());
        }
        boolean create = app == null;
        if (create) {
            app = new AiBusinessApp();
            app.setTenantId(tenantId);
            app.setAppCode(resolveRuntimeAppCode(tenantId, suiteCode, businessObject.getObjectCode(), config));
        }
        app.setAppName(resolveRuntimeAppName(config, dto, businessObject));
        app.setAppType("BUSINESS");
        app.setSuiteCode(suiteCode);
        app.setObjectCode(businessObject.getObjectCode());
        app.setEntryMode("RUNTIME");
        app.setEntryUrl("/ai/crud-page/" + config.getConfigKey());
        app.setConfigKey(config.getConfigKey());
        app.setIcon(StringUtils.defaultIfBlank(businessObject.getIcon(), "ionicons5:AppsOutline"));
        app.setDescription(StringUtils.defaultIfBlank(config.getTableComment(), "低代码发布生成的标准业务应用入口"));
        app.setStatus(1);
        app.setSortOrder(config.getMenuSort() == null ? 0 : config.getMenuSort());
        app.setOptions("{\"source\":\"lowcode_publish\"}");
        if (create) {
            businessAppMapper.insert(app);
        } else {
            businessAppMapper.updateById(app);
        }
    }

    private AiBusinessObject findBusinessObject(Long tenantId, String suiteCode, String objectCode) {
        AiBusinessObject object = businessObjectMapper.selectByObjectCode(tenantId, suiteCode, objectCode);
        if (object != null) {
            return object;
        }
        return businessObjectMapper.selectByObjectCode(tenantId, suiteCode, objectCode.toUpperCase(Locale.ROOT));
    }

    private String resolveRuntimeAppName(AiCrudConfig config, LowcodePublishDTO dto, AiBusinessObject businessObject) {
        return StringUtils.firstNonBlank(
                config.getMenuName(),
                config.getAppName(),
                dto != null ? dto.getBusinessObjectName() : null,
                businessObject.getObjectName(),
                config.getConfigKey()
        );
    }

    private String resolveRuntimeAppCode(Long tenantId, String suiteCode, String objectCode, AiCrudConfig config) {
        String base = normalizeAppCode(suiteCode + "_" + objectCode + "_RUNTIME");
        if (businessAppMapper.countByAppCode(tenantId, base, null) == 0) {
            return base;
        }
        String fallback = normalizeAppCode(base + "_" + config.getId());
        if (businessAppMapper.countByAppCode(tenantId, fallback, null) == 0) {
            return fallback;
        }
        return normalizeAppCode(base + "_" + System.currentTimeMillis());
    }

    private String normalizeAppCode(String value) {
        String normalized = StringUtils.defaultString(value)
                .replaceAll("[^A-Za-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .toUpperCase(Locale.ROOT)
                .replaceAll("^[^A-Z]+", "")
                .replaceAll("_+$", "");
        if (StringUtils.isBlank(normalized)) {
            normalized = "LOWCODE_RUNTIME_APP";
        }
        return normalized.length() > 64 ? normalized.substring(0, 64).replaceAll("_+$", "") : normalized;
    }

    private int nextVersionNo(AiCrudConfig config) {
        Integer maxVersionNo = versionMapper.selectMaxVersionNo(resolveTenantId(config), config.getId());
        return (maxVersionNo == null ? 0 : maxVersionNo) + 1;
    }

    private AiCrudConfigVersion createVersion(AiCrudConfig config, Integer versionNo, String versionType, String remark) {
        AiCrudConfigVersion version = new AiCrudConfigVersion();
        version.setTenantId(resolveTenantId(config));
        version.setConfigId(config.getId());
        version.setConfigKey(config.getConfigKey());
        version.setDomainId(config.getDomainId());
        version.setDomainCode(config.getDomainCode());
        version.setObjectCode(config.getObjectCode());
        version.setObjectName(config.getObjectName());
        version.setVersionNo(versionNo);
        version.setVersionType(versionType);
        version.setModelSchema(config.getModelSchema());
        version.setPageSchema(config.getPageSchema());
        version.setSearchSchema(config.getSearchSchema());
        version.setColumnsSchema(config.getColumnsSchema());
        version.setEditSchema(config.getEditSchema());
        version.setApiConfig(config.getApiConfig());
        version.setOptions(config.getOptions());
        version.setPublishSnapshot(writeSnapshot(config));
        version.setRemark(StringUtils.defaultIfBlank(remark, "发布低代码应用"));
        versionMapper.insert(version);
        return version;
    }

    private void applyVersionRuntimeFields(AiCrudConfig config,
                                           AiCrudConfigVersion version,
                                           Map<String, Object> snapshot,
                                           LowcodeRuntimeConfig fallback) {
        config.setSearchSchema(StringUtils.defaultIfBlank(version.getSearchSchema(), fallback.getSearchSchema()));
        config.setColumnsSchema(StringUtils.defaultIfBlank(version.getColumnsSchema(), fallback.getColumnsSchema()));
        config.setEditSchema(StringUtils.defaultIfBlank(version.getEditSchema(), fallback.getEditSchema()));
        config.setApiConfig(StringUtils.defaultIfBlank(version.getApiConfig(), fallback.getApiConfig()));
        config.setOptions(StringUtils.defaultIfBlank(version.getOptions(), fallback.getOptions()));
        config.setDictConfig(StringUtils.defaultIfBlank(text(snapshot.get("dictConfig")), fallback.getDictConfig()));
        config.setDesensitizeConfig(StringUtils.defaultIfBlank(text(snapshot.get("desensitizeConfig")), fallback.getDesensitizeConfig()));
        config.setEncryptConfig(StringUtils.defaultIfBlank(text(snapshot.get("encryptConfig")), fallback.getEncryptConfig()));
        config.setTransConfig(StringUtils.defaultIfBlank(text(snapshot.get("transConfig")), fallback.getTransConfig()));
        config.setLayoutType(StringUtils.defaultIfBlank(text(snapshot.get("layoutType")), fallback.getLayoutType()));
        config.setTableName(StringUtils.defaultIfBlank(text(snapshot.get("tableName")), fallback.getTableName()));
        config.setTableComment(StringUtils.defaultIfBlank(text(snapshot.get("tableComment")), fallback.getTableComment()));
        config.setAppName(StringUtils.defaultIfBlank(text(snapshot.get("appName")), config.getTableComment()));
    }

    private void applySnapshotMenuFields(AiCrudConfig config, Map<String, Object> snapshot) {
        config.setMenuName(StringUtils.defaultIfBlank(text(snapshot.get("menuName")),
                StringUtils.defaultIfBlank(config.getAppName(), config.getTableComment())));
        config.setMenuParentId(numberAsLong(snapshot.get("menuParentId"), config.getMenuParentId()));
        config.setMenuSort(numberAsInteger(snapshot.get("menuSort"), config.getMenuSort()));
    }

    private LowcodeModelSchema readVersionModel(AiCrudConfigVersion version) {
        return readJson(version.getModelSchema(), LowcodeModelSchema.class, "版本modelSchema");
    }

    private LowcodePageSchema readVersionPage(AiCrudConfigVersion version) {
        return readJson(version.getPageSchema(), LowcodePageSchema.class, "版本pageSchema");
    }

    private String writeSnapshot(AiCrudConfig config) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("configKey", config.getConfigKey());
        snapshot.put("tableName", config.getTableName());
        snapshot.put("tableComment", config.getTableComment());
        snapshot.put("appName", config.getAppName());
        snapshot.put("layoutType", config.getLayoutType());
        snapshot.put("domainId", config.getDomainId());
        snapshot.put("domainCode", config.getDomainCode());
        snapshot.put("objectCode", config.getObjectCode());
        snapshot.put("objectName", config.getObjectName());
        snapshot.put("domain", buildDomainSnapshot(config));
        snapshot.put("object", buildObjectSnapshot(config));
        snapshot.put("dictConfig", config.getDictConfig());
        snapshot.put("desensitizeConfig", config.getDesensitizeConfig());
        snapshot.put("encryptConfig", config.getEncryptConfig());
        snapshot.put("transConfig", config.getTransConfig());
        snapshot.put("menuName", config.getMenuName());
        snapshot.put("menuParentId", config.getMenuParentId());
        snapshot.put("menuSort", config.getMenuSort());
        snapshot.put("menuResourceId", config.getMenuResourceId());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new BusinessException("发布快照生成失败");
        }
    }

    private Map<String, Object> readSnapshot(String snapshotJson) {
        if (StringUtils.isBlank(snapshotJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(snapshotJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new BusinessException("版本快照格式不正确");
        }
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

    private LowcodeVersionVO toVersionVO(AiCrudConfigVersion version) {
        LowcodeVersionVO vo = new LowcodeVersionVO();
        vo.setId(version.getId());
        vo.setConfigId(version.getConfigId());
        vo.setConfigKey(version.getConfigKey());
        vo.setDomainId(version.getDomainId());
        vo.setDomainCode(version.getDomainCode());
        vo.setObjectCode(version.getObjectCode());
        vo.setObjectName(version.getObjectName());
        vo.setVersionNo(version.getVersionNo());
        vo.setVersionType(version.getVersionType());
        vo.setRemark(version.getRemark());
        vo.setCreateTime(version.getCreateTime());
        vo.setCreateBy(version.getCreateBy());
        return vo;
    }

    private Long resolveTenantId(AiCrudConfig config) {
        if (config.getTenantId() != null) {
            return config.getTenantId();
        }
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }

    private PublishDomainContext resolvePublishDomainContext(AiCrudConfig config, LowcodeModelSchema modelSchema) {
        LowcodeDomainRef schemaDomain = modelSchema != null ? modelSchema.getDomain() : null;
        LowcodeObjectSchema schemaObject = modelSchema != null ? modelSchema.getObject() : null;
        Long domainId = firstNonNull(config.getDomainId(), schemaDomain != null ? schemaDomain.getId() : null);
        String domainCode = StringUtils.firstNonBlank(config.getDomainCode(), schemaDomain != null ? schemaDomain.getCode() : null);
        AiLowcodeDomain domain = resolveDomain(domainId, domainCode);
        String objectCode = StringUtils.firstNonBlank(config.getObjectCode(),
                schemaObject != null ? schemaObject.getCode() : null,
                config.getConfigKey(),
                modelSchema != null ? modelSchema.getTableName() : null);
        String objectName = StringUtils.firstNonBlank(config.getObjectName(),
                schemaObject != null ? schemaObject.getName() : null,
                modelSchema != null ? modelSchema.getBusinessName() : null,
                config.getAppName(),
                config.getTableComment(),
                objectCode);
        return new PublishDomainContext(domain, objectCode, objectName);
    }

    private PublishDomainContext resolveVersionDomainContext(AiCrudConfig config,
                                                             AiCrudConfigVersion version,
                                                             Map<String, Object> snapshot,
                                                             LowcodeModelSchema modelSchema) {
        LowcodeDomainRef schemaDomain = modelSchema != null ? modelSchema.getDomain() : null;
        LowcodeObjectSchema schemaObject = modelSchema != null ? modelSchema.getObject() : null;
        Long domainId = firstNonNull(
                version.getDomainId(),
                numberAsLong(snapshot.get("domainId"), null),
                schemaDomain != null ? schemaDomain.getId() : null,
                config.getDomainId());
        String domainCode = StringUtils.firstNonBlank(
                version.getDomainCode(),
                text(snapshot.get("domainCode")),
                schemaDomain != null ? schemaDomain.getCode() : null,
                config.getDomainCode());
        AiLowcodeDomain domain = resolveDomain(domainId, domainCode);
        String objectCode = StringUtils.firstNonBlank(
                version.getObjectCode(),
                text(snapshot.get("objectCode")),
                schemaObject != null ? schemaObject.getCode() : null,
                config.getObjectCode(),
                config.getConfigKey());
        String objectName = StringUtils.firstNonBlank(
                version.getObjectName(),
                text(snapshot.get("objectName")),
                schemaObject != null ? schemaObject.getName() : null,
                config.getObjectName(),
                modelSchema != null ? modelSchema.getBusinessName() : null,
                config.getAppName(),
                config.getTableComment(),
                objectCode);
        return new PublishDomainContext(domain, objectCode, objectName);
    }

    private AiLowcodeDomain resolveDomainForConfig(AiCrudConfig config) {
        return resolveDomain(config.getDomainId(), config.getDomainCode());
    }

    private AiLowcodeDomain resolveDomain(Long domainId, String domainCode) {
        if (domainId != null) {
            try {
                return domainService.requireDomain(domainId);
            } catch (BusinessException ignored) {
                // 旧版本快照可能只保留编码，继续按编码和通用业务域兜底。
            }
        }
        if (StringUtils.isNotBlank(domainCode)) {
            AiLowcodeDomain domain = domainService.getByCode(domainCode);
            if (domain != null) {
                return domain;
            }
        }
        AiLowcodeDomain generalDomain = domainService.getByCode(GENERAL_DOMAIN_CODE);
        if (generalDomain == null) {
            throw new BusinessException("通用业务域不存在，请先执行低代码业务领域迁移脚本");
        }
        return generalDomain;
    }

    private void applyDomainToModelSchema(LowcodeModelSchema modelSchema, PublishDomainContext context, String configKey) {
        if (modelSchema == null || context == null || context.domain() == null) {
            return;
        }
        normalizeModelCollections(modelSchema);
        modelSchema.setSchemaVersion(2);

        LowcodeDomainRef domainRef = modelSchema.getDomain() == null ? new LowcodeDomainRef() : modelSchema.getDomain();
        domainRef.setId(context.domain().getId());
        domainRef.setCode(context.domain().getDomainCode());
        domainRef.setName(context.domain().getDomainName());
        modelSchema.setDomain(domainRef);

        LowcodeObjectSchema object = modelSchema.getObject() == null ? new LowcodeObjectSchema() : modelSchema.getObject();
        object.setCode(context.objectCode());
        object.setName(context.objectName());
        if (StringUtils.isBlank(object.getDescription())) {
            object.setDescription(StringUtils.defaultIfBlank(modelSchema.getBusinessName(), context.objectName()));
        }
        modelSchema.setObject(object);
        ensureTableName(modelSchema, context.domain(), context.objectCode(), configKey);
        policyService.normalizeModelSchema(modelSchema);
    }

    private void ensureTableName(LowcodeModelSchema modelSchema, AiLowcodeDomain domain, String objectCode, String configKey) {
        if (schemaValidator.isValidTableName(modelSchema.getTableName())) {
            return;
        }
        String prefix = StringUtils.defaultIfBlank(domain.getTablePrefix(), "biz_");
        String base = StringUtils.firstNonBlank(
                objectCode,
                modelSchema.getObject() == null ? null : modelSchema.getObject().getCode(),
                configKey,
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

    private void normalizeModelCollections(LowcodeModelSchema modelSchema) {
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
    }

    private void applyDomainToConfig(AiCrudConfig config, PublishDomainContext context) {
        if (context == null || context.domain() == null) {
            return;
        }
        config.setDomainId(context.domain().getId());
        config.setDomainCode(context.domain().getDomainCode());
        config.setObjectCode(context.objectCode());
        config.setObjectName(context.objectName());
    }

    private void applyPublishMenuParent(AiCrudConfig config, LowcodePublishDTO dto, AiLowcodeDomain domain) {
        if (dto != null && dto.getMenuParentId() != null) {
            config.setMenuParentId(dto.getMenuParentId());
            return;
        }
        Long parentId = resolveDomainMenuParentId(domain);
        config.setMenuParentId(parentId != null ? parentId : menuRegisterAdapter.resolveDefaultLowcodeParentId());
    }

    private Long resolveDomainMenuParentId(AiLowcodeDomain domain) {
        if (domain == null) {
            return null;
        }
        if (domain.getMenuParentId() != null) {
            return domain.getMenuParentId();
        }
        Long parentId = menuRegisterAdapter.resolveOrCreateDomainParentId(
                domain.getDomainCode(), domain.getDomainName(), domain.getSort());
        if (parentId != null) {
            domain.setMenuParentId(parentId);
            domainService.updateById(domain);
        }
        return parentId;
    }

    private Map<String, Object> buildDomainSnapshot(AiCrudConfig config) {
        Map<String, Object> domain = new LinkedHashMap<>();
        domain.put("id", config.getDomainId());
        domain.put("code", config.getDomainCode());
        return domain;
    }

    private Map<String, Object> buildObjectSnapshot(AiCrudConfig config) {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("code", config.getObjectCode());
        object.put("name", config.getObjectName());
        return object;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long numberAsLong(Object value, Long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            return Long.valueOf(text);
        }
        return defaultValue;
    }

    private Integer numberAsInteger(Object value, Integer defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            return Integer.valueOf(text);
        }
        return defaultValue;
    }

    private record PublishDomainContext(AiLowcodeDomain domain, String objectCode, String objectName) {
    }
}

package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeDomain;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeModel;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAuditStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeObjectSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeDatasourceSnapshot;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.AiLowcodeModelMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.service.IGenDatasourceService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDomainService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeModelSchemaNormalizer;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeSchemaValidator;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * 业务对象草稿 Schema 持久化网关。
 *
 * <p>封装模型/运行配置的加载、默认值补全、领域与数据源解析、校验和双表写入，
 * 使设计器 Facade 只负责编排事务与设计阶段。</p>
 */
final class BusinessObjectDraftSchemaGateway {

    private static final String GENERAL_DOMAIN_CODE = "general";
    private static final String FORM_DESIGNER_SCHEMA_OPTION_KEY = "formDesignerSchema";
    private static final String OBJECT_OPTION_RUNTIME_DATASOURCE_ID = "runtimeDatasourceId";
    private static final String OBJECT_OPTION_RUNTIME_DATASOURCE = "runtimeDatasource";

    private final ObjectMapper objectMapper;
    private final AiLowcodeModelMapper lowcodeModelMapper;
    private final AiCrudConfigMapper crudConfigMapper;
    private final BusinessAppMapper businessAppMapper;
    private final AiCrudConfigService crudConfigService;
    private final LowcodeDomainService domainService;
    private final LowcodeModelSchemaNormalizer schemaNormalizer;
    private final LowcodeSchemaValidator schemaValidator;
    private final IGenDatasourceService datasourceService;
    private final LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver;
    private final BusinessFieldSchemaService fieldSchemaService;
    private final LongSupplier tenantIdSupplier;

    BusinessObjectDraftSchemaGateway(ObjectMapper objectMapper,
                                     AiLowcodeModelMapper lowcodeModelMapper,
                                     AiCrudConfigMapper crudConfigMapper,
                                     BusinessAppMapper businessAppMapper,
                                     AiCrudConfigService crudConfigService,
                                     LowcodeDomainService domainService,
                                     LowcodeModelSchemaNormalizer schemaNormalizer,
                                     LowcodeSchemaValidator schemaValidator,
                                     IGenDatasourceService datasourceService,
                                     LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver,
                                     BusinessFieldSchemaService fieldSchemaService,
                                     LongSupplier tenantIdSupplier) {
        this.objectMapper = objectMapper;
        this.lowcodeModelMapper = lowcodeModelMapper;
        this.crudConfigMapper = crudConfigMapper;
        this.businessAppMapper = businessAppMapper;
        this.crudConfigService = crudConfigService;
        this.domainService = domainService;
        this.schemaNormalizer = schemaNormalizer;
        this.schemaValidator = schemaValidator;
        this.datasourceService = datasourceService;
        this.runtimeDataSourceResolver = runtimeDataSourceResolver;
        this.fieldSchemaService = fieldSchemaService;
        this.tenantIdSupplier = tenantIdSupplier;
    }

    AiLowcodeModel resolveModel(AiBusinessObject object, AiCrudConfig config) {
        Long tenantId = tenantIdSupplier.getAsLong();
        if (object.getModelId() != null) {
            AiLowcodeModel model = lowcodeModelMapper.selectModelById(tenantId, object.getModelId());
            if (model != null) {
                return model;
            }
        }
        if (StringUtils.isNotBlank(object.getModelCode())) {
            AiLowcodeModel model = lowcodeModelMapper.selectByModelCode(tenantId, object.getModelCode());
            if (model != null) {
                return model;
            }
        }
        if (config != null && StringUtils.isNotBlank(config.getObjectCode())) {
            return lowcodeModelMapper.selectByModelCode(tenantId, config.getObjectCode());
        }
        return null;
    }

    AiCrudConfig resolveConfig(AiBusinessObject object) {
        Long tenantId = tenantIdSupplier.getAsLong();
        if (StringUtils.isNotBlank(object.getConfigKey())) {
            AiCrudConfig config = crudConfigMapper.selectByConfigKey(tenantId, object.getConfigKey());
            if (config != null) {
                return config;
            }
        }
        AiBusinessApp app = businessAppMapper.selectRuntimeAppByObject(tenantId, object.getSuiteCode(), object.getObjectCode());
        if (app != null && StringUtils.isNotBlank(app.getConfigKey())) {
            AiCrudConfig config = crudConfigMapper.selectByConfigKey(tenantId, app.getConfigKey());
            if (config != null) {
                return config;
            }
        }
        if (StringUtils.isNotBlank(object.getModelCode())) {
            return crudConfigMapper.selectByConfigKey(tenantId, normalizeConfigKey(object.getModelCode()));
        }
        return null;
    }

    LowcodeModelSchema resolveModelSchema(AiBusinessObject object, AiLowcodeModel model, AiCrudConfig config) {
        LowcodeModelSchema schema = null;
        if (model != null && StringUtils.isNotBlank(model.getModelSchema())) {
            schema = readJson(model.getModelSchema(), LowcodeModelSchema.class, "modelSchema");
        } else if (config != null && StringUtils.isNotBlank(config.getModelSchema())) {
            schema = readJson(config.getModelSchema(), LowcodeModelSchema.class, "modelSchema");
        }
        if (schema == null) {
            schema = buildDefaultModelSchema(object);
        }
        return enrichModelSchema(object, schema);
    }

    LowcodePageSchema resolvePageSchema(AiCrudConfig config, LowcodeModelSchema modelSchema) {
        LowcodePageSchema pageSchema;
        if (config != null && StringUtils.isNotBlank(config.getPageSchema())) {
            pageSchema = ensurePageSchema(readJson(config.getPageSchema(), LowcodePageSchema.class, "pageSchema"), modelSchema);
        } else {
            pageSchema = ensurePageSchema(fieldSchemaService.buildDefaultPageSchema(modelSchema), modelSchema);
        }
        applyLegacyRuntimeSchemas(config, pageSchema, modelSchema);
        return pageSchema;
    }

    LowcodePageSchema ensurePageSchema(LowcodePageSchema pageSchema, LowcodeModelSchema modelSchema) {
        return legacyPageSchemaAdapter().ensurePageSchema(pageSchema, modelSchema);
    }

    private void applyLegacyRuntimeSchemas(AiCrudConfig config,
                                           LowcodePageSchema pageSchema,
                                           LowcodeModelSchema modelSchema) {
        legacyPageSchemaAdapter().applyLegacyRuntimeSchemas(config, pageSchema, modelSchema);
    }

    private LowcodeModelSchema buildDefaultModelSchema(AiBusinessObject object) {
        LowcodeModelSchema schema = new LowcodeModelSchema();
        schema.setSchemaVersion(2);
        schema.setAppType("SINGLE");
        schema.setTableMode("CREATE");
        schema.setTableName(normalizeTableName(resolveModelCode(object)));
        schema.setBusinessName(object.getObjectName());
        schema.setFields(new ArrayList<>());
        return schema;
    }

    LowcodeModelSchema enrichModelSchema(AiBusinessObject object, LowcodeModelSchema schema) {
        LowcodeModelSchema target = schema == null ? buildDefaultModelSchema(object) : schema;
        target.setSchemaVersion(2);
        target.setAppType(StringUtils.defaultIfBlank(target.getAppType(), "SINGLE").toUpperCase(Locale.ROOT));
        target.setTableMode(StringUtils.defaultIfBlank(target.getTableMode(), "CREATE").toUpperCase(Locale.ROOT));
        target.setTableName(StringUtils.defaultIfBlank(target.getTableName(), normalizeTableName(resolveModelCode(object))));
        target.setBusinessName(StringUtils.defaultIfBlank(target.getBusinessName(), object.getObjectName()));
        AiLowcodeDomain domain = resolveDomain(object, target);
        LowcodeDomainRef domainRef = target.getDomain() == null ? new LowcodeDomainRef() : target.getDomain();
        domainRef.setId(domain == null ? domainRef.getId() : domain.getId());
        domainRef.setCode(domain == null ? StringUtils.defaultIfBlank(domainRef.getCode(), object.getSuiteCode()) : domain.getDomainCode());
        domainRef.setName(domain == null ? StringUtils.defaultIfBlank(domainRef.getName(), object.getSuiteCode()) : domain.getDomainName());
        target.setDomain(domainRef);

        LowcodeObjectSchema objectSchema = target.getObject() == null ? new LowcodeObjectSchema() : target.getObject();
        objectSchema.setCode(resolveModelCode(object));
        objectSchema.setName(StringUtils.defaultIfBlank(objectSchema.getName(), object.getObjectName()));
        objectSchema.setDescription(StringUtils.defaultIfBlank(objectSchema.getDescription(), object.getDescription()));
        target.setObject(objectSchema);
        applyRuntimeDatasourceFromObjectOptions(object, target);
        if (target.getAuditStrategy() == null) {
            LowcodeAuditStrategy auditStrategy = new LowcodeAuditStrategy();
            auditStrategy.setMode("FORGE_COLUMNS");
            auditStrategy.setCreateByColumn("create_by");
            auditStrategy.setCreateTimeColumn("create_time");
            auditStrategy.setCreateDeptColumn("create_dept");
            auditStrategy.setUpdateByColumn("update_by");
            auditStrategy.setUpdateTimeColumn("update_time");
            target.setAuditStrategy(auditStrategy);
        }
        return schemaNormalizer.normalizeModelFields(target, true);
    }

    void validateDraft(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        if (!hasBusinessFields(modelSchema)) {
            return;
        }
        schemaValidator.validatePage(pageSchema, modelSchema);
    }

    AiLowcodeModel saveModelDraft(AiBusinessObject object, AiLowcodeModel model, LowcodeModelSchema modelSchema) {
        AiLowcodeDomain domain = resolveDomain(object, modelSchema);
        AiLowcodeModel target = model == null ? new AiLowcodeModel() : model;
        target.setTenantId(tenantIdSupplier.getAsLong());
        target.setDomainId(domain == null ? 1900000000000000001L : domain.getId());
        target.setDomainCode(domain == null ? GENERAL_DOMAIN_CODE : domain.getDomainCode());
        target.setModelCode(resolveModelCode(object));
        target.setModelName(object.getObjectName());
        target.setModelDesc(StringUtils.defaultIfBlank(object.getDescription(), object.getObjectName()));
        target.setStatus("ENABLED");
        target.setTenantEnabled(true);
        target.setMasterData("MASTER".equalsIgnoreCase(object.getObjectType()));
        LowcodeRuntimeDatasourceSnapshot runtimeDatasource = modelSchema.getRuntimeDatasource();
        target.setRuntimeDatasourceId(runtimeDatasource == null ? null : runtimeDatasource.getDatasourceId());
        target.setRuntimeDatasourceCode(runtimeDatasource == null ? null : runtimeDatasource.getDatasourceCode());
        target.setRuntimeTableName(StringUtils.defaultIfBlank(
                runtimeDatasource == null ? null : runtimeDatasource.getTableName(),
                modelSchema.getTableName()));
        target.setModelSchema(writeJson(modelSchema, "modelSchema"));
        if (target.getId() == null) {
            lowcodeModelMapper.insert(target);
        } else {
            lowcodeModelMapper.updateById(target);
        }
        return target;
    }

    AiCrudConfig saveRuntimeDraft(AiBusinessObject object, AiCrudConfig config,
                                          LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        AiCrudConfig target = config == null ? new AiCrudConfig() : config;
        target.setTenantId(tenantIdSupplier.getAsLong());
        target.setConfigKey(StringUtils.defaultIfBlank(target.getConfigKey(), resolveConfigKey(object)));
        target.setTableName(modelSchema.getTableName());
        target.setTableComment(StringUtils.defaultIfBlank(modelSchema.getBusinessName(), object.getObjectName()));
        target.setAppName(object.getObjectName());
        target.setMenuName(StringUtils.defaultIfBlank(target.getMenuName(), object.getObjectName()));
        target.setMenuSort(target.getMenuSort() == null ? object.getSortOrder() : target.getMenuSort());
        target.setMode("CONFIG");
        target.setBuildMode("LOWCODE");
        target.setStatus(EnableStatus.DISABLED.codeAsString());
        target.setPublishStatus(StringUtils.defaultIfBlank(target.getPublishStatus(), "DRAFT"));
        target.setLayoutType(StringUtils.defaultIfBlank(pageSchema.getLayoutType(), "simple-crud"));
        target.setDraftVersion((target.getDraftVersion() == null ? 0 : target.getDraftVersion()) + 1);
        if (target.getPublishedVersion() == null) {
            target.setPublishedVersion(0);
        }
        LowcodeDomainRef domain = modelSchema.getDomain();
        LowcodeObjectSchema lowcodeObject = modelSchema.getObject();
        LowcodeRuntimeDatasourceSnapshot runtimeDatasource = modelSchema.getRuntimeDatasource();
        target.setDomainId(domain == null ? target.getDomainId() : domain.getId());
        target.setDomainCode(domain == null ? target.getDomainCode() : domain.getCode());
        target.setObjectCode(lowcodeObject == null ? resolveModelCode(object) : lowcodeObject.getCode());
        target.setObjectName(object.getObjectName());
        target.setRuntimeDatasourceId(runtimeDatasource == null ? null : runtimeDatasource.getDatasourceId());
        target.setRuntimeDatasourceCode(runtimeDatasource == null ? null : runtimeDatasource.getDatasourceCode());
        target.setRuntimeDatasourceSnapshot(writeJson(runtimeDatasource, "runtimeDatasourceSnapshot"));
        target.setRuntimeTableName(StringUtils.defaultIfBlank(
                runtimeDatasource == null ? null : runtimeDatasource.getTableName(),
                modelSchema.getTableName()));
        target.setModelSchema(writeJson(modelSchema, "modelSchema"));
        target.setPageSchema(writeJson(pageSchema, "pageSchema"));
        // 导入已有表时 audit/tenant/logicDelete 写在 modelSchema 里；必须同步到配置列，
        // 否则运行时只读到 NULL/旧值，新增无法自动填充 create_by 等审计字段。
        target.setTenantStrategy(writeJson(modelSchema.getTenantStrategy(), "tenantStrategy"));
        target.setAuditStrategy(writeJson(modelSchema.getAuditStrategy(), "auditStrategy"));
        target.setLogicDeleteStrategy(writeJson(modelSchema.getLogicDeleteStrategy(), "logicDeleteStrategy"));
        // 同步重编译 columnsSchema/options：否则草稿预览若走存量字段会回落默认「编辑/删除」，自定义行按钮也不见
        syncCompiledRuntimeSchemas(target, pageSchema);
        if (target.getId() == null) {
            crudConfigService.save(target);
        } else {
            crudConfigService.updateById(target);
        }
        return target;
    }

    private void syncCompiledRuntimeSchemas(AiCrudConfig target, LowcodePageSchema pageSchema) {
        if (target == null) {
            return;
        }
        if (StringUtils.isBlank(target.getModelSchema()) || StringUtils.isBlank(target.getPageSchema())) {
            target.setOptions(mergeFormDesignerSchemaIntoRuntimeOptions(target.getOptions(), pageSchema));
            return;
        }
        try {
            AiCrudConfig compiled = crudConfigService.resolveDraftRuntimeConfig(target);
            if (compiled != null) {
                target.setSearchSchema(compiled.getSearchSchema());
                target.setColumnsSchema(compiled.getColumnsSchema());
                target.setEditSchema(compiled.getEditSchema());
                target.setApiConfig(compiled.getApiConfig());
                target.setDictConfig(compiled.getDictConfig());
                target.setDesensitizeConfig(compiled.getDesensitizeConfig());
                target.setEncryptConfig(compiled.getEncryptConfig());
                target.setTransConfig(compiled.getTransConfig());
                target.setOptions(mergeFormDesignerSchemaIntoRuntimeOptions(compiled.getOptions(), pageSchema));
                return;
            }
        } catch (Exception ignored) {
            // 编译失败时仍保存 pageSchema，预览链路会再次尝试 compile
        }
        target.setOptions(mergeFormDesignerSchemaIntoRuntimeOptions(target.getOptions(), pageSchema));
    }

    private void applyRuntimeDatasourceFromObjectOptions(AiBusinessObject object, LowcodeModelSchema schema) {
        if (object == null || schema == null || schema.getRuntimeDatasource() != null) {
            return;
        }
        Long datasourceId = resolveRuntimeDatasourceId(object);
        if (datasourceId == null) {
            return;
        }
        GenDatasource datasource = datasourceService.getById(datasourceId);
        if (datasource == null || !EnableStatus.ENABLED.matches(datasource.getIsEnabled())) {
            throw new BusinessException("运行数据源不存在或已禁用");
        }
        schema.setRuntimeDatasource(runtimeDataSourceResolver.buildSnapshot(
                datasource, schema.getTableName(), schema.getTableMode()));
    }

    private Long resolveRuntimeDatasourceId(AiBusinessObject object) {
        Map<String, Object> options = readMap(object.getOptions());
        Long datasourceId = numberAsLong(options.get(OBJECT_OPTION_RUNTIME_DATASOURCE_ID));
        if (datasourceId != null) {
            return datasourceId;
        }
        Map<String, Object> runtimeDatasource = mapValue(options.get(OBJECT_OPTION_RUNTIME_DATASOURCE));
        return numberAsLong(runtimeDatasource.get("datasourceId"));
    }

    private AiLowcodeDomain resolveDomain(AiBusinessObject object, LowcodeModelSchema schema) {
        LowcodeDomainRef domainRef = schema == null ? null : schema.getDomain();
        if (domainRef != null && domainRef.getId() != null) {
            try {
                return domainService.requireDomain(domainRef.getId());
            } catch (BusinessException ignored) {
                // 继续按编码兜底。
            }
        }
        String domainCode = StringUtils.firstNonBlank(
                domainRef == null ? null : domainRef.getCode(),
                object.getSuiteCode(),
                GENERAL_DOMAIN_CODE);
        AiLowcodeDomain domain = domainService.getByCode(domainCode);
        if (domain == null && StringUtils.isNotBlank(domainCode)) {
            domain = domainService.getByCode(domainCode.toLowerCase(Locale.ROOT));
        }
        if (domain == null) {
            domain = domainService.getByCode(GENERAL_DOMAIN_CODE);
        }
        return domain;
    }


    private boolean hasBusinessFields(LowcodeModelSchema modelSchema) {
        return modelSchema != null && modelSchema.getFields() != null
                && modelSchema.getFields().stream().anyMatch(field -> field != null && !Boolean.TRUE.equals(field.getSystemField()));
    }

    private String resolveModelCode(AiBusinessObject object) {
        return normalizeConfigKey(StringUtils.firstNonBlank(object.getModelCode(), object.getObjectCode(), "business_object"));
    }

    private String resolveConfigKey(AiBusinessObject object) {
        String preferred = StringUtils.firstNonBlank(object.getConfigKey(), object.getModelCode(),
                object.getSuiteCode() + "_" + object.getObjectCode());
        String base = normalizeConfigKey(preferred);
        AiCrudConfig existing = crudConfigMapper.selectByConfigKey(tenantIdSupplier.getAsLong(), base);
        if (existing == null || StringUtils.equals(existing.getObjectCode(), resolveModelCode(object))) {
            return base;
        }
        return normalizeConfigKey(base + "_" + object.getId());
    }

    private String normalizeConfigKey(String value) {
        String normalized = StringUtils.defaultString(value)
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .toLowerCase(Locale.ROOT)
                .replaceAll("^[^a-z]+", "")
                .replaceAll("_+$", "");
        if (StringUtils.isBlank(normalized)) {
            normalized = "business_object";
        }
        return StringUtils.left(normalized, 64);
    }

    private String normalizeTableName(String value) {
        String tableName = normalizeConfigKey(value);
        if (tableName.startsWith("sys_") || tableName.startsWith("ai_") || tableName.startsWith("gen_")
                || tableName.startsWith("flow_") || tableName.startsWith("data_")) {
            tableName = "biz_" + tableName;
        }
        return StringUtils.left(tableName, 64);
    }

    private BusinessObjectLegacyPageSchemaAdapter legacyPageSchemaAdapter() {
        return new BusinessObjectLegacyPageSchemaAdapter(objectMapper, fieldSchemaService);
    }

    private LowcodePageZone findZone(LowcodePageSchema pageSchema, String zoneKey) {
        if (pageSchema == null || pageSchema.getZones() == null) {
            return null;
        }
        return pageSchema.getZones().stream()
                .filter(zone -> zone != null && zoneKey.equals(zone.getZoneKey()))
                .findFirst()
                .orElse(null);
    }

    private String mergeFormDesignerSchemaIntoRuntimeOptions(String optionsJson, LowcodePageSchema pageSchema) {
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        Object formSchema = editZone == null || editZone.getProps() == null
                ? null : editZone.getProps().get(FORM_DESIGNER_SCHEMA_OPTION_KEY);
        if (formSchema == null) {
            return optionsJson;
        }
        Map<String, Object> options = readMap(optionsJson);
        options.put(FORM_DESIGNER_SCHEMA_OPTION_KEY, formSchema);
        return writeJson(options, "options");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> readMap(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private <T> T readJson(String json, Class<T> type, String fieldName) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new BusinessException(fieldName + "格式不正确");
        }
    }

    private String writeJson(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(fieldName + "序列化失败");
        }
    }

    private Long numberAsLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            return Long.valueOf(text);
        }
        return null;
    }
}

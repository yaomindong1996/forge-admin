package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessObjectDesignStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectRelation;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessSuite;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeDomain;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeModel;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectDesignVersion;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDesignerDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectRelationDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeObjectSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.AiLowcodeModelMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectDesignVersionMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectRelationMapper;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDomainService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeModelSchemaNormalizer;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeSchemaValidator;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectDesignerVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectRelationVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 业务对象设计器聚合服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessObjectDesignerService {

    private static final String GENERAL_DOMAIN_CODE = "general";

    private final ObjectMapper objectMapper;
    private final BusinessObjectService objectService;
    private final BusinessSuiteService suiteService;
    private final AiLowcodeModelMapper lowcodeModelMapper;
    private final AiCrudConfigMapper crudConfigMapper;
    private final BusinessAppMapper businessAppMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessObjectRelationMapper relationMapper;
    private final BusinessObjectDesignVersionMapper designVersionMapper;
    private final AiCrudConfigService crudConfigService;
    private final LowcodeDomainService domainService;
    private final LowcodeModelSchemaNormalizer schemaNormalizer;
    private final LowcodeSchemaValidator schemaValidator;
    private final BusinessFieldSchemaService fieldSchemaService;

    public BusinessObjectDesignerVO getDesigner(Long objectId) {
        DesignerContext context = loadContext(objectId);
        BusinessObjectDesignerVO vo = new BusinessObjectDesignerVO();
        BusinessObjectVO objectVO = context.getObjectVO();
        AiBusinessObject object = context.getObject();
        vo.setObjectId(object.getId());
        vo.setSuiteCode(object.getSuiteCode());
        vo.setSuiteName(objectVO == null ? null : objectVO.getSuiteName());
        vo.setObjectCode(object.getObjectCode());
        vo.setObjectName(object.getObjectName());
        vo.setObjectType(object.getObjectType());
        vo.setDisplayField(object.getDisplayField());
        vo.setIcon(object.getIcon());
        vo.setDescription(object.getDescription());
        vo.setStatus(object.getStatus());
        vo.setDesignStatus(resolveDesignStatus(object, context.getConfig()));
        vo.setPublishStatus(context.getConfig() == null ? "DRAFT" : context.getConfig().getPublishStatus());
        vo.setHasUnpublishedChanges(hasUnpublishedChanges(object, context.getConfig()));
        vo.setLastPublishVersion(object.getLastPublishVersion());
        vo.setLastPublishTime(object.getLastPublishTime());
        vo.setUpdateTime(object.getUpdateTime());
        vo.setModelSchema(context.getModelSchema());
        vo.setPageSchema(context.getPageSchema());
        vo.setFields(fieldSchemaService.toFieldVOList(context.getModelSchema()));
        vo.setRelations(context.getRelations());
        vo.setDesignerOptions(readMap(object.getDesignerOptions()));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveDesigner(Long objectId, BusinessObjectDesignerDTO dto) {
        DesignerContext context = loadContext(objectId);
        AiBusinessObject object = context.getObject();
        if (dto != null) {
            applyObjectFields(object, dto);
            if (dto.getModelSchema() != null) {
                context.setModelSchema(enrichModelSchema(object, dto.getModelSchema()));
            } else if (dto.getFields() != null && !dto.getFields().isEmpty()) {
                context.setModelSchema(rebuildModelFields(context.getModelSchema(), dto.getFields()));
            }
            if (dto.getPageSchema() != null) {
                context.setPageSchema(ensurePageSchema(dto.getPageSchema(), context.getModelSchema()));
            }
            if (dto.getDesignerOptions() != null && !dto.getDesignerOptions().isEmpty()) {
                object.setDesignerOptions(writeJson(dto.getDesignerOptions(), "designerOptions"));
            }
            if (dto.getRelations() != null && !dto.getRelations().isEmpty()) {
                saveSourceRelations(object, dto.getRelations());
                context.setRelations(relationMapper.selectRelationsByObject(
                        resolveTenantId(), object.getSuiteCode(), object.getObjectCode()));
                applyRelationsToModel(context);
            }
        }
        saveDraft(context, BusinessObjectDesignStatus.CHANGED);
    }

    public DesignerContext loadContext(Long objectId) {
        AiBusinessObject object = objectService.requireEntity(objectId);
        BusinessObjectVO objectVO = objectService.detail(objectId);
        AiCrudConfig config = resolveConfig(object);
        AiLowcodeModel model = resolveModel(object, config);
        LowcodeModelSchema modelSchema = resolveModelSchema(object, model, config);
        LowcodePageSchema pageSchema = resolvePageSchema(config, modelSchema);
        List<BusinessObjectRelationVO> relations = relationMapper.selectRelationsByObject(
                resolveTenantId(), object.getSuiteCode(), object.getObjectCode());

        DesignerContext context = new DesignerContext();
        context.setObject(object);
        context.setObjectVO(objectVO);
        context.setModel(model);
        context.setConfig(config);
        context.setModelSchema(modelSchema);
        context.setPageSchema(pageSchema);
        context.setRelations(relations);
        return context;
    }

    @Transactional(rollbackFor = Exception.class)
    public DesignerContext saveDraft(DesignerContext context, String designStatus) {
        if (context == null || context.getObject() == null) {
            throw new BusinessException("业务对象设计上下文不能为空");
        }
        AiBusinessObject object = context.getObject();
        LowcodeModelSchema modelSchema = enrichModelSchema(object, context.getModelSchema());
        LowcodePageSchema pageSchema = ensurePageSchema(context.getPageSchema(), modelSchema);
        validateDraft(modelSchema, pageSchema);
        AiLowcodeModel model = saveModelDraft(object, context.getModel(), modelSchema);
        AiCrudConfig config = saveRuntimeDraft(object, context.getConfig(), modelSchema, pageSchema);

        object.setModelId(model.getId());
        object.setModelCode(model.getModelCode());
        object.setConfigKey(config.getConfigKey());
        object.setDesignStatus(BusinessObjectDesignStatus.normalize(designStatus));
        if (StringUtils.isBlank(object.getDesignerOptions())) {
            object.setDesignerOptions("{}");
        }
        businessObjectMapper.updateById(object);

        context.setModel(model);
        context.setConfig(config);
        context.setModelSchema(modelSchema);
        context.setPageSchema(pageSchema);
        return context;
    }

    @Transactional(rollbackFor = Exception.class)
    public AiCrudConfig ensureRuntimeDraft(Long objectId) {
        return saveDraft(loadContext(objectId), BusinessObjectDesignStatus.CHANGED).getConfig();
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncModelRelations(Long objectId) {
        DesignerContext context = loadContext(objectId);
        applyRelationsToModel(context);
        saveDraft(context, BusinessObjectDesignStatus.CHANGED);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollbackDesignVersion(Long objectId, Long versionId) {
        DesignerContext context = loadContext(objectId);
        AiBusinessObjectDesignVersion version = designVersionMapper.selectVersionById(
                resolveTenantId(), objectId, versionId);
        if (version == null) {
            throw new BusinessException("设计版本不存在");
        }
        context.setModelSchema(readJson(version.getModelSnapshot(), LowcodeModelSchema.class, "modelSnapshot"));
        context.setPageSchema(readJson(version.getPageSnapshot(), LowcodePageSchema.class, "pageSnapshot"));
        restoreRelationsFromSnapshot(context.getObject(), version.getRelationSnapshot());
        saveDraft(context, BusinessObjectDesignStatus.CHANGED);
    }

    private AiLowcodeModel resolveModel(AiBusinessObject object, AiCrudConfig config) {
        Long tenantId = resolveTenantId();
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

    private AiCrudConfig resolveConfig(AiBusinessObject object) {
        Long tenantId = resolveTenantId();
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

    private LowcodeModelSchema resolveModelSchema(AiBusinessObject object, AiLowcodeModel model, AiCrudConfig config) {
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

    private LowcodePageSchema resolvePageSchema(AiCrudConfig config, LowcodeModelSchema modelSchema) {
        if (config != null && StringUtils.isNotBlank(config.getPageSchema())) {
            return ensurePageSchema(readJson(config.getPageSchema(), LowcodePageSchema.class, "pageSchema"), modelSchema);
        }
        return ensurePageSchema(fieldSchemaService.buildDefaultPageSchema(modelSchema), modelSchema);
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

    private LowcodeModelSchema enrichModelSchema(AiBusinessObject object, LowcodeModelSchema schema) {
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
        return schemaNormalizer.normalizeModelFields(target, true);
    }

    private LowcodePageSchema ensurePageSchema(LowcodePageSchema pageSchema, LowcodeModelSchema modelSchema) {
        LowcodePageSchema target = pageSchema == null ? fieldSchemaService.buildDefaultPageSchema(modelSchema) : pageSchema;
        if (StringUtils.isBlank(target.getLayoutType())) {
            target.setLayoutType("simple-crud");
        }
        if (target.getZones() == null) {
            target.setZones(new ArrayList<>());
        }
        Set<String> zoneKeys = new LinkedHashSet<>();
        target.getZones().forEach(zone -> {
            if (zone != null && StringUtils.isNotBlank(zone.getZoneKey())) {
                zoneKeys.add(zone.getZoneKey());
            }
        });
        LowcodePageSchema defaults = fieldSchemaService.buildDefaultPageSchema(modelSchema);
        defaults.getZones().stream()
                .filter(zone -> !zoneKeys.contains(zone.getZoneKey()))
                .forEach(target.getZones()::add);
        return target;
    }

    private void validateDraft(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        if (!hasBusinessFields(modelSchema)) {
            return;
        }
        schemaValidator.validatePage(pageSchema, modelSchema);
    }

    private AiLowcodeModel saveModelDraft(AiBusinessObject object, AiLowcodeModel model, LowcodeModelSchema modelSchema) {
        AiLowcodeDomain domain = resolveDomain(object, modelSchema);
        AiLowcodeModel target = model == null ? new AiLowcodeModel() : model;
        target.setTenantId(resolveTenantId());
        target.setDomainId(domain == null ? 1900000000000000001L : domain.getId());
        target.setDomainCode(domain == null ? GENERAL_DOMAIN_CODE : domain.getDomainCode());
        target.setModelCode(resolveModelCode(object));
        target.setModelName(object.getObjectName());
        target.setModelDesc(StringUtils.defaultIfBlank(object.getDescription(), object.getObjectName()));
        target.setStatus("ENABLED");
        target.setTenantEnabled(true);
        target.setMasterData("MASTER".equalsIgnoreCase(object.getObjectType()));
        target.setModelSchema(writeJson(modelSchema, "modelSchema"));
        if (target.getId() == null) {
            lowcodeModelMapper.insert(target);
        } else {
            lowcodeModelMapper.updateById(target);
        }
        return target;
    }

    private AiCrudConfig saveRuntimeDraft(AiBusinessObject object, AiCrudConfig config,
                                          LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        AiCrudConfig target = config == null ? new AiCrudConfig() : config;
        target.setTenantId(resolveTenantId());
        target.setConfigKey(StringUtils.defaultIfBlank(target.getConfigKey(), resolveConfigKey(object)));
        target.setTableName(modelSchema.getTableName());
        target.setTableComment(StringUtils.defaultIfBlank(modelSchema.getBusinessName(), object.getObjectName()));
        target.setAppName(object.getObjectName());
        target.setMenuName(StringUtils.defaultIfBlank(target.getMenuName(), object.getObjectName()));
        target.setMenuSort(target.getMenuSort() == null ? object.getSortOrder() : target.getMenuSort());
        target.setMode("CONFIG");
        target.setBuildMode("LOWCODE");
        target.setStatus("0");
        target.setPublishStatus(StringUtils.defaultIfBlank(target.getPublishStatus(), "DRAFT"));
        target.setLayoutType(StringUtils.defaultIfBlank(pageSchema.getLayoutType(), "simple-crud"));
        target.setDraftVersion((target.getDraftVersion() == null ? 0 : target.getDraftVersion()) + 1);
        if (target.getPublishedVersion() == null) {
            target.setPublishedVersion(0);
        }
        LowcodeDomainRef domain = modelSchema.getDomain();
        LowcodeObjectSchema lowcodeObject = modelSchema.getObject();
        target.setDomainId(domain == null ? target.getDomainId() : domain.getId());
        target.setDomainCode(domain == null ? target.getDomainCode() : domain.getCode());
        target.setObjectCode(lowcodeObject == null ? resolveModelCode(object) : lowcodeObject.getCode());
        target.setObjectName(object.getObjectName());
        target.setModelSchema(writeJson(modelSchema, "modelSchema"));
        target.setPageSchema(writeJson(pageSchema, "pageSchema"));
        if (target.getId() == null) {
            crudConfigService.save(target);
        } else {
            crudConfigService.updateById(target);
        }
        return target;
    }

    private void applyObjectFields(AiBusinessObject object, BusinessObjectDesignerDTO dto) {
        if (StringUtils.isNotBlank(dto.getObjectName())) {
            object.setObjectName(StringUtils.trim(dto.getObjectName()));
        }
        if (dto.getDescription() != null) {
            object.setDescription(StringUtils.trimToNull(dto.getDescription()));
        }
        if (dto.getIcon() != null) {
            object.setIcon(StringUtils.trimToNull(dto.getIcon()));
        }
        if (dto.getDisplayField() != null) {
            object.setDisplayField(StringUtils.trimToNull(dto.getDisplayField()));
        }
        if (dto.getStatus() != null) {
            object.setStatus(dto.getStatus());
        }
    }

    private LowcodeModelSchema rebuildModelFields(LowcodeModelSchema modelSchema, List<BusinessFieldDTO> fields) {
        LowcodeModelSchema target = modelSchema == null ? new LowcodeModelSchema() : modelSchema;
        List<LowcodeFieldSchema> newFields = new ArrayList<>();
        if (target.getFields() != null) {
            target.getFields().stream()
                    .filter(field -> field != null && Boolean.TRUE.equals(field.getSystemField()))
                    .forEach(newFields::add);
        }
        for (BusinessFieldDTO dto : fields) {
            if (dto != null) {
                newFields.add(fieldSchemaService.buildFieldSchema(dto));
            }
        }
        target.setFields(newFields);
        return schemaNormalizer.normalizeModelFields(target, true);
    }

    public void applyRelationsToModel(DesignerContext context) {
        if (context == null || context.getObject() == null || context.getModelSchema() == null) {
            return;
        }
        List<AiBusinessObjectRelation> relations = relationMapper.selectRuntimeRelationsBySource(
                resolveTenantId(), context.getObject().getSuiteCode(), context.getObject().getObjectCode());
        List<LowcodeRelationSchema> relationSchemas = relations.stream()
                .map(this::toLowcodeRelation)
                .toList();
        context.getModelSchema().setRelations(relationSchemas);
        syncInlineEditRelationsToPageSchema(context, relations);
    }

    private void syncInlineEditRelationsToPageSchema(DesignerContext context,
                                                     List<AiBusinessObjectRelation> relations) {
        LowcodePageSchema pageSchema = context.getPageSchema() == null ? new LowcodePageSchema() : context.getPageSchema();
        LowcodePageModelRef primaryRef = toPageModelRef(context.getObject(), context.getModel(), context.getModelSchema(), true);
        List<LowcodePageModelRef> refs = new ArrayList<>();
        refs.add(primaryRef);

        List<String> childFieldRefs = new ArrayList<>();
        for (AiBusinessObjectRelation relation : relations) {
            if (!isEmbeddedRelation(relation)) {
                continue;
            }
            AiBusinessObject target = businessObjectMapper.selectByObjectCode(
                    resolveTenantId(), relation.getSuiteCode(), relation.getTargetObjectCode());
            if (target == null) {
                continue;
            }
            DesignerContext targetContext = loadContext(target.getId());
            LowcodePageModelRef childRef = toPageModelRef(target, targetContext.getModel(),
                    targetContext.getModelSchema(), false);
            childRef.setRelations(List.of(toRelationToPrimary(relation, primaryRef.getModelCode())));
            childRef.setProps(toInlineRelationProps(relation));
            refs.add(childRef);
            childRef.getFields().stream()
                    .map(item -> text(item.get("fieldRef")))
                    .filter(StringUtils::isNotBlank)
                    .forEach(childFieldRefs::add);
        }

        pageSchema.setModelRefs(refs);
        pageSchema.setPrimaryModelId(primaryRef.getModelId());
        pageSchema.setPrimaryModelCode(primaryRef.getModelCode());
        if (refs.size() > 1) {
            pageSchema.setLayoutType("master-detail-crud");
        } else if ("master-detail-crud".equals(pageSchema.getLayoutType())) {
            pageSchema.setLayoutType("simple-crud");
        }
        syncInlineEditRefsToEditZone(pageSchema, primaryRef, childFieldRefs);
        context.setPageSchema(pageSchema);
    }

    private boolean isEmbeddedRelation(AiBusinessObjectRelation relation) {
        if (relation == null || Integer.valueOf(0).equals(relation.getStatus())) {
            return false;
        }
        String relationType = StringUtils.defaultString(relation.getRelationType()).toUpperCase(Locale.ROOT);
        if (!Set.of("CHILD_LIST", "DETAIL").contains(relationType)) {
            return false;
        }
        Map<String, Object> config = readMap(relation.getRelationConfig());
        return readBoolean(config.get("showInDetail"), true)
                || readBoolean(config.get("inlineCreateEnabled"), true)
                || readBoolean(config.get("inlineEditEnabled"), true);
    }

    private Map<String, Object> toInlineRelationProps(AiBusinessObjectRelation relation) {
        Map<String, Object> config = readMap(relation.getRelationConfig());
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("relationName", relation.getRelationName());
        props.put("tabTitle", StringUtils.firstNonBlank(text(config.get("detailTabTitle")),
                text(config.get("detailTab")), relation.getRelationName()));
        props.put("sourceObjectCode", relation.getSourceObjectCode());
        props.put("targetObjectCode", relation.getTargetObjectCode());
        props.put("businessObjectCode", relation.getTargetObjectCode());
        props.put("showInDetail", readBoolean(config.get("showInDetail"), true));
        props.put("inlineCreateEnabled", readBoolean(config.get("inlineCreateEnabled"), true));
        props.put("inlineEditEnabled", readBoolean(config.get("inlineEditEnabled"), true));
        if (StringUtils.isNotBlank(text(config.get("defaultFilter")))) {
            props.put("defaultFilter", text(config.get("defaultFilter")));
        }
        return props;
    }

    private LowcodeRelationSchema toRelationToPrimary(AiBusinessObjectRelation relation, String primaryObjectCode) {
        LowcodeRelationSchema schema = new LowcodeRelationSchema();
        schema.setRelationType(relation.getRelationType());
        schema.setTargetObjectCode(primaryObjectCode);
        schema.setSourceField(relation.getTargetFieldCode());
        schema.setTargetField(relation.getSourceFieldCode());
        schema.setDisplayField(text(readMap(relation.getRelationConfig()).get("displayField")));
        return schema;
    }

    private LowcodePageModelRef toPageModelRef(AiBusinessObject object,
                                               AiLowcodeModel model,
                                               LowcodeModelSchema schema,
                                               boolean primary) {
        LowcodePageModelRef ref = new LowcodePageModelRef();
        ref.setModelId(model == null ? null : model.getId());
        String modelCode = StringUtils.firstNonBlank(
                object.getModelCode(),
                schema == null || schema.getObject() == null ? null : schema.getObject().getCode(),
                resolveModelCode(object));
        modelCode = normalizeConfigKey(modelCode);
        ref.setModelCode(modelCode);
        ref.setModelName(StringUtils.defaultIfBlank(object.getObjectName(),
                schema == null ? modelCode : StringUtils.defaultIfBlank(schema.getBusinessName(), modelCode)));
        ref.setTableName(schema == null ? null : schema.getTableName());
        ref.setPrimary(primary);
        ref.setFields(toPageModelFields(modelCode, schema, primary));
        return ref;
    }

    private List<Map<String, Object>> toPageModelFields(String modelCode,
                                                        LowcodeModelSchema schema,
                                                        boolean primary) {
        if (schema == null || schema.getFields() == null) {
            return new ArrayList<>();
        }
        return schema.getFields().stream()
                .filter(field -> field != null)
                .map(field -> toPageModelField(modelCode, field, primary))
                .toList();
    }

    private Map<String, Object> toPageModelField(String modelCode,
                                                 LowcodeFieldSchema field,
                                                 boolean primary) {
        Map<String, Object> item = new LinkedHashMap<>();
        String fieldName = field.getField();
        item.put("field", fieldName);
        item.put("sourceField", fieldName);
        item.put("fieldRef", primary ? fieldName : safeModelKey(modelCode) + "__" + fieldName);
        item.put("rawLabel", StringUtils.defaultIfBlank(field.getLabel(), fieldName));
        item.put("label", StringUtils.defaultIfBlank(field.getLabel(), fieldName));
        item.put("columnName", field.getColumnName());
        item.put("dataType", field.getDataType());
        item.put("length", field.getLength());
        item.put("precision", field.getPrecision());
        item.put("required", field.getRequired());
        item.put("defaultValue", field.getDefaultValue());
        item.put("searchable", field.getSearchable());
        item.put("listVisible", field.getListVisible());
        item.put("formVisible", field.getFormVisible());
        item.put("componentType", field.getComponentType());
        item.put("queryType", field.getQueryType());
        item.put("dictType", field.getDictType());
        item.put("sensitiveType", field.getSensitiveType());
        item.put("encryptAlgorithm", field.getEncryptAlgorithm());
        item.put("sortable", field.getSortable());
        item.put("primaryKey", field.getPrimaryKey());
        item.put("systemField", field.getSystemField());
        item.put("readonly", field.getReadonly());
        item.put("fieldStatus", field.getFieldStatus());
        item.put("autoIncrement", field.getAutoIncrement());
        item.put("width", field.getWidth());
        item.put("remark", field.getRemark());
        return item;
    }

    private void syncInlineEditRefsToEditZone(LowcodePageSchema pageSchema,
                                              LowcodePageModelRef primaryRef,
                                              List<String> childFieldRefs) {
        if (pageSchema.getZones() == null) {
            pageSchema.setZones(new ArrayList<>());
        }
        LowcodePageZone editZone = pageSchema.getZones().stream()
                .filter(zone -> zone != null && "edit".equals(zone.getZoneKey()))
                .findFirst()
                .orElse(null);
        if (editZone == null) {
            editZone = new LowcodePageZone();
            editZone.setZoneKey("edit");
            editZone.setComponentKey("edit-form");
            editZone.setEnabled(true);
            editZone.setProps(new LinkedHashMap<>());
            pageSchema.getZones().add(editZone);
        }
        Set<String> primaryFields = primaryRef.getFields().stream()
                .map(item -> text(item.get("fieldRef")))
                .filter(StringUtils::isNotBlank)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        List<String> primaryRefs = editZone.getFieldRefs() == null
                ? new ArrayList<>()
                : editZone.getFieldRefs().stream()
                .filter(primaryFields::contains)
                .toList();
        if (primaryRefs.isEmpty()) {
            primaryRefs = primaryRef.getFields().stream()
                    .filter(item -> !Boolean.TRUE.equals(item.get("systemField")))
                    .filter(item -> !Boolean.FALSE.equals(item.get("formVisible")))
                    .map(item -> text(item.get("fieldRef")))
                    .filter(StringUtils::isNotBlank)
                    .toList();
        }
        Set<String> childFields = new LinkedHashSet<>(childFieldRefs);
        List<String> selectedChildRefs = editZone.getFieldRefs() == null
                ? new ArrayList<>()
                : editZone.getFieldRefs().stream()
                .filter(childFields::contains)
                .toList();
        Map<String, Object> props = editZone.getProps() == null ? Map.of() : editZone.getProps();
        boolean customRelationFields = "CUSTOM".equalsIgnoreCase(text(props.get("relationFieldSelectionMode")))
                || readBoolean(props.get("relationFieldSelectionTouched"), false);
        if (selectedChildRefs.isEmpty() && !customRelationFields) {
            selectedChildRefs = childFieldRefs;
        }
        LinkedHashSet<String> refs = new LinkedHashSet<>(primaryRefs);
        refs.addAll(selectedChildRefs);
        editZone.setFieldRefs(new ArrayList<>(refs));
    }

    private String safeModelKey(String value) {
        String key = StringUtils.defaultIfBlank(value, "model").replaceAll("[^A-Za-z0-9_]", "_");
        return StringUtils.defaultIfBlank(key, "model");
    }

    private LowcodeRelationSchema toLowcodeRelation(AiBusinessObjectRelation relation) {
        LowcodeRelationSchema schema = new LowcodeRelationSchema();
        schema.setRelationType(relation.getRelationType());
        schema.setTargetObjectCode(resolveRelationModelCode(relation.getSuiteCode(), relation.getTargetObjectCode()));
        schema.setSourceField(relation.getSourceFieldCode());
        schema.setTargetField(relation.getTargetFieldCode());
        schema.setDisplayField(text(readMap(relation.getRelationConfig()).get("displayField")));
        return schema;
    }

    private String resolveRelationModelCode(String suiteCode, String objectCode) {
        if (StringUtils.isBlank(objectCode)) {
            return objectCode;
        }
        AiBusinessObject object = businessObjectMapper.selectByObjectCode(resolveTenantId(), suiteCode, objectCode);
        if (object == null) {
            return normalizeConfigKey(objectCode);
        }
        return resolveModelCode(object);
    }

    private void saveSourceRelations(AiBusinessObject object, List<BusinessObjectRelationDTO> relations) {
        List<Long> savedIds = new ArrayList<>();
        for (BusinessObjectRelationDTO dto : relations) {
            if (dto == null) {
                continue;
            }
            AiBusinessObjectRelation relation = dto.getId() == null
                    ? new AiBusinessObjectRelation()
                    : relationMapper.selectRelationById(resolveTenantId(), dto.getId());
            if (relation == null) {
                relation = new AiBusinessObjectRelation();
            }
            relation.setTenantId(resolveTenantId());
            relation.setSuiteCode(object.getSuiteCode());
            relation.setSourceObjectCode(object.getObjectCode());
            relation.setTargetObjectCode(StringUtils.trimToNull(dto.getTargetObjectCode()));
            relation.setRelationType(StringUtils.defaultIfBlank(dto.getRelationType(), "REFERENCE").toUpperCase(Locale.ROOT));
            relation.setRelationName(StringUtils.defaultIfBlank(dto.getRelationName(), relation.getTargetObjectCode()));
            relation.setSourceFieldCode(StringUtils.trimToNull(dto.getSourceFieldCode()));
            relation.setTargetFieldCode(StringUtils.trimToNull(dto.getTargetFieldCode()));
            relation.setRelationConfig(StringUtils.trimToNull(dto.getRelationConfig()));
            relation.setDescription(StringUtils.trimToNull(dto.getDescription()));
            relation.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
            relation.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
            if (relation.getId() == null) {
                relationMapper.insert(relation);
            } else {
                relationMapper.updateById(relation);
            }
            savedIds.add(relation.getId());
        }
        relationMapper.deleteMissingRelations(resolveTenantId(), object.getSuiteCode(), object.getObjectCode(), savedIds);
    }

    @SuppressWarnings("unchecked")
    private void restoreRelationsFromSnapshot(AiBusinessObject object, String relationSnapshot) {
        if (StringUtils.isBlank(relationSnapshot)) {
            return;
        }
        List<Map<String, Object>> relations;
        try {
            relations = objectMapper.readValue(relationSnapshot, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new BusinessException("关系快照格式不正确");
        }
        List<BusinessObjectRelationDTO> dtoList = new ArrayList<>();
        for (Map<String, Object> item : relations) {
            if (item == null || !object.getObjectCode().equals(text(item.get("sourceObjectCode")))) {
                continue;
            }
            BusinessObjectRelationDTO dto = new BusinessObjectRelationDTO();
            dto.setId(numberAsLong(item.get("id")));
            dto.setTargetObjectCode(text(item.get("targetObjectCode")));
            dto.setRelationType(text(item.get("relationType")));
            dto.setRelationName(text(item.get("relationName")));
            dto.setSourceFieldCode(text(item.get("sourceFieldCode")));
            dto.setTargetFieldCode(text(item.get("targetFieldCode")));
            dto.setRelationConfig(text(item.get("relationConfig")));
            dto.setDescription(text(item.get("description")));
            dto.setStatus(numberAsInteger(item.get("status")));
            dto.setSortOrder(numberAsInteger(item.get("sortOrder")));
            dtoList.add(dto);
        }
        saveSourceRelations(object, dtoList);
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

    private String resolveDesignStatus(AiBusinessObject object, AiCrudConfig config) {
        if (StringUtils.isNotBlank(object.getDesignStatus())) {
            return object.getDesignStatus();
        }
        if (config != null && "PUBLISHED".equals(config.getPublishStatus())) {
            return BusinessObjectDesignStatus.PUBLISHED;
        }
        return BusinessObjectDesignStatus.DRAFT;
    }

    private boolean hasUnpublishedChanges(AiBusinessObject object, AiCrudConfig config) {
        String designStatus = BusinessObjectDesignStatus.normalize(object.getDesignStatus());
        if (BusinessObjectDesignStatus.CHANGED.equals(designStatus)
                || BusinessObjectDesignStatus.DESIGNING.equals(designStatus)
                || BusinessObjectDesignStatus.READY.equals(designStatus)) {
            return true;
        }
        if (config == null) {
            return true;
        }
        if (!"PUBLISHED".equals(config.getPublishStatus())) {
            return true;
        }
        if (BusinessObjectDesignStatus.PUBLISHED.equals(designStatus)) {
            return false;
        }
        return object.getLastPublishVersion() == null && config.getPublishedVersion() == null;
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
        AiCrudConfig existing = crudConfigMapper.selectByConfigKey(resolveTenantId(), base);
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

    private boolean readBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = StringUtils.trimToEmpty(String.valueOf(value));
        if (StringUtils.isBlank(text)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
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

    public String writeJson(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(fieldName + "序列化失败");
        }
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private Integer numberAsInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            return Integer.valueOf(text);
        }
        return null;
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

    @Data
    public static class DesignerContext {
        private AiBusinessObject object;
        private BusinessObjectVO objectVO;
        private AiLowcodeModel model;
        private AiCrudConfig config;
        private LowcodeModelSchema modelSchema;
        private LowcodePageSchema pageSchema;
        private List<BusinessObjectRelationVO> relations = new ArrayList<>();
    }
}

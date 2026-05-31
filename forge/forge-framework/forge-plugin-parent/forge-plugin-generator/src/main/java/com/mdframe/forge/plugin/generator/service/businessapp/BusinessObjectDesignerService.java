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
import com.mdframe.forge.plugin.generator.dto.businessapp.FormDesignerSchemaDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.LinkageSchemaDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.ViewSchemaDTO;
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
import java.util.Comparator;
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
    private static final String FORM_DESIGNER_SCHEMA_OPTION_KEY = "formDesignerSchema";
    private static final String VIEW_SCHEMA_OPTION_KEY = "viewSchema";
    private static final String LINKAGE_SCHEMA_OPTION_KEY = "linkageSchema";
    private static final String LINKAGE_SCHEMA_MANAGED_BY = "linkageSchema";
    private static final Set<String> FORM_FIELD_COMPONENT_KEYS = Set.of(
            "input", "textarea", "number", "integer", "money", "date", "datetime", "time",
            "switch", "select", "radio", "checkbox", "dictSelect", "cascader",
            "regionTreeSelect", "orgTreeSelect", "userSelect", "fileUpload", "imageUpload",
            "objectReference"
    );

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
        vo.setRelations(sourceRelations(object, context.getRelations()));
        Map<String, Object> designerOptions = readMap(object.getDesignerOptions());
        vo.setDesignerOptions(designerOptions);
        vo.setFormDesignerSchema(resolveFormDesignerSchema(object, context.getModelSchema(),
                context.getPageSchema(), designerOptions));
        vo.setViewSchema(resolveViewSchema(context.getModelSchema(), context.getPageSchema(), designerOptions));
        vo.setLinkageSchema(resolveLinkageSchema(designerOptions));
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
            Map<String, Object> designerOptions = readMap(object.getDesignerOptions());
            if (dto.getDesignerOptions() != null && !dto.getDesignerOptions().isEmpty()) {
                designerOptions.putAll(dto.getDesignerOptions());
            }
            if (dto.getFormDesignerSchema() != null) {
                designerOptions.put(FORM_DESIGNER_SCHEMA_OPTION_KEY, dto.getFormDesignerSchema());
            }
            if (dto.getViewSchema() != null) {
                designerOptions.put(VIEW_SCHEMA_OPTION_KEY, dto.getViewSchema());
            }
            if (dto.getLinkageSchema() != null) {
                designerOptions.put(LINKAGE_SCHEMA_OPTION_KEY, dto.getLinkageSchema());
            }
            if (!designerOptions.isEmpty()) {
                object.setDesignerOptions(writeJson(designerOptions, "designerOptions"));
            }
            if (dto.getRelations() != null) {
                saveSourceRelations(object, sourceRelationDTOs(object, dto.getRelations()));
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

    private List<BusinessObjectRelationVO> sourceRelations(AiBusinessObject object, List<BusinessObjectRelationVO> relations) {
        if (object == null || relations == null) {
            return new ArrayList<>();
        }
        return relations.stream()
                .filter(relation -> relation != null
                        && StringUtils.equals(object.getObjectCode(), relation.getSourceObjectCode()))
                .toList();
    }

    private List<BusinessObjectRelationDTO> sourceRelationDTOs(AiBusinessObject object,
                                                               List<BusinessObjectRelationDTO> relations) {
        if (object == null || relations == null) {
            return new ArrayList<>();
        }
        return relations.stream()
                .filter(relation -> relation != null
                        && (StringUtils.isBlank(relation.getSourceObjectCode())
                        || StringUtils.equals(object.getObjectCode(), relation.getSourceObjectCode())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public DesignerContext saveDraft(DesignerContext context, String designStatus) {
        if (context == null || context.getObject() == null) {
            throw new BusinessException("业务对象设计上下文不能为空");
        }
        AiBusinessObject object = context.getObject();
        LowcodeModelSchema modelSchema = enrichModelSchema(object, context.getModelSchema());
        LowcodePageSchema pageSchema = ensurePageSchema(context.getPageSchema(), modelSchema);
        context.setModelSchema(modelSchema);
        context.setPageSchema(pageSchema);
        compileFormFirstRuntimeSchema(context);
        modelSchema = enrichModelSchema(object, context.getModelSchema());
        pageSchema = ensurePageSchema(context.getPageSchema(), modelSchema);
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

    public DesignerContext compileFormFirstRuntimeSchema(DesignerContext context) {
        if (context == null || context.getObject() == null) {
            return context;
        }
        Map<String, Object> designerOptions = readMap(context.getObject().getDesignerOptions());
        boolean hasFormSchema = hasDesignerOption(designerOptions, FORM_DESIGNER_SCHEMA_OPTION_KEY);
        boolean hasViewSchema = hasDesignerOption(designerOptions, VIEW_SCHEMA_OPTION_KEY);
        boolean hasLinkageSchema = hasDesignerOption(designerOptions, LINKAGE_SCHEMA_OPTION_KEY);
        if (!hasFormSchema && !hasViewSchema && !hasLinkageSchema) {
            return context;
        }

        LowcodeModelSchema modelSchema = enrichModelSchema(context.getObject(), context.getModelSchema());
        LowcodePageSchema pageSchema = ensurePageSchema(context.getPageSchema(), modelSchema);
        FormDesignerSchemaDTO formSchema = hasFormSchema
                ? resolveFormDesignerSchema(context.getObject(), modelSchema, pageSchema, designerOptions)
                : null;
        ViewSchemaDTO viewSchema = hasViewSchema
                ? resolveViewSchema(modelSchema, pageSchema, designerOptions)
                : null;
        LinkageSchemaDTO linkageSchema = hasLinkageSchema ? resolveLinkageSchema(designerOptions) : null;

        if (formSchema != null) {
            applyFormDesignerSchemaToModel(modelSchema, formSchema);
        }
        if (linkageSchema != null) {
            applyLinkageSchemaToModel(modelSchema, linkageSchema);
        }
        modelSchema = schemaNormalizer.normalizeModelFields(modelSchema, true);
        pageSchema = ensurePageSchema(pageSchema, modelSchema);
        if (formSchema != null) {
            applyFormDesignerSchemaToEditZone(pageSchema, modelSchema, formSchema);
        }
        if (viewSchema != null) {
            applyViewSchemaToPageZones(pageSchema, modelSchema, viewSchema);
        }
        context.setModelSchema(modelSchema);
        context.setPageSchema(pageSchema);
        return context;
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
        restoreDesignerOptionsFromSnapshot(context.getObject(), version.getDesignerOptionsSnapshot());
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
        Set<String> addedModelCodes = new LinkedHashSet<>();
        refs.add(primaryRef);
        addedModelCodes.add(primaryRef.getModelCode());

        List<String> childFieldRefs = new ArrayList<>();
        boolean hasEmbeddedRelations = false;
        for (AiBusinessObjectRelation relation : relations) {
            AiBusinessObject target = businessObjectMapper.selectByObjectCode(
                    resolveTenantId(), relation.getSuiteCode(), relation.getTargetObjectCode());
            if (target == null) {
                continue;
            }
            DesignerContext targetContext = loadContext(target.getId());
            LowcodePageModelRef targetRef = toPageModelRef(target, targetContext.getModel(),
                    targetContext.getModelSchema(), false);
            if (isEmbeddedRelation(relation)) {
                targetRef.setRelations(List.of(toRelationToPrimary(relation, primaryRef.getModelCode())));
                targetRef.setProps(toInlineRelationProps(relation));
                hasEmbeddedRelations = true;
                addPageModelRef(refs, addedModelCodes, targetRef);
                targetRef.getFields().stream()
                        .map(item -> text(item.get("fieldRef")))
                        .filter(StringUtils::isNotBlank)
                        .forEach(childFieldRefs::add);
                continue;
            }
            if (isReferenceLookupRelation(relation)) {
                targetRef.setRelations(List.of(toLowcodeRelation(relation)));
                targetRef.setProps(toLookupRelationProps(relation));
                addPageModelRef(refs, addedModelCodes, targetRef);
            }
        }

        pageSchema.setModelRefs(refs);
        pageSchema.setPrimaryModelId(primaryRef.getModelId());
        pageSchema.setPrimaryModelCode(primaryRef.getModelCode());
        if (hasEmbeddedRelations) {
            pageSchema.setLayoutType("master-detail-crud");
        } else if ("master-detail-crud".equals(pageSchema.getLayoutType())) {
            pageSchema.setLayoutType("simple-crud");
        }
        syncInlineEditRefsToEditZone(pageSchema, primaryRef, childFieldRefs);
        context.setPageSchema(pageSchema);
    }

    private boolean addPageModelRef(List<LowcodePageModelRef> refs, Set<String> addedModelCodes, LowcodePageModelRef ref) {
        if (ref == null || StringUtils.isBlank(ref.getModelCode()) || addedModelCodes.contains(ref.getModelCode())) {
            return false;
        }
        refs.add(ref);
        addedModelCodes.add(ref.getModelCode());
        return true;
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

    private boolean isReferenceLookupRelation(AiBusinessObjectRelation relation) {
        if (relation == null || Integer.valueOf(0).equals(relation.getStatus())) {
            return false;
        }
        String relationType = StringUtils.defaultString(relation.getRelationType()).toUpperCase(Locale.ROOT);
        return "REFERENCE".equals(relationType)
                && StringUtils.isNotBlank(relation.getSourceFieldCode())
                && StringUtils.isNotBlank(relation.getTargetFieldCode());
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
        putIfNotBlank(props, "displayField", resolveRelationDisplayField(relation));
        return props;
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    private Map<String, Object> toLookupRelationProps(AiBusinessObjectRelation relation) {
        Map<String, Object> config = readMap(relation.getRelationConfig());
        Map<String, Object> props = new LinkedHashMap<>();
        AiBusinessObject target = businessObjectMapper.selectByObjectCode(
                resolveTenantId(), relation.getSuiteCode(), relation.getTargetObjectCode());
        props.put("relationName", relation.getRelationName());
        props.put("sourceObjectCode", relation.getSourceObjectCode());
        props.put("targetObjectCode", relation.getTargetObjectCode());
        props.put("sourceField", relation.getSourceFieldCode());
        props.put("targetField", relation.getTargetFieldCode());
        putIfNotBlank(props, "displayField", resolveRelationDisplayField(relation, target, config));
        putIfNotBlank(props, "targetConfigKey", target == null ? null : target.getConfigKey());
        putIfNotBlank(props, "targetDisplayField", target == null ? null : target.getDisplayField());
        return props;
    }

    private LowcodeRelationSchema toRelationToPrimary(AiBusinessObjectRelation relation, String primaryObjectCode) {
        LowcodeRelationSchema schema = new LowcodeRelationSchema();
        schema.setRelationType(relation.getRelationType());
        schema.setTargetObjectCode(primaryObjectCode);
        schema.setSourceField(relation.getTargetFieldCode());
        schema.setTargetField(relation.getSourceFieldCode());
        schema.setDisplayField(resolveRelationDisplayField(relation));
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
        schema.setDisplayField(resolveRelationDisplayField(relation));
        return schema;
    }

    private String resolveRelationDisplayField(AiBusinessObjectRelation relation) {
        if (relation == null) {
            return null;
        }
        Map<String, Object> config = readMap(relation.getRelationConfig());
        AiBusinessObject target = businessObjectMapper.selectByObjectCode(
                resolveTenantId(), relation.getSuiteCode(), relation.getTargetObjectCode());
        return resolveRelationDisplayField(relation, target, config);
    }

    private String resolveRelationDisplayField(AiBusinessObjectRelation relation,
                                               AiBusinessObject target,
                                               Map<String, Object> config) {
        String configured = config == null ? null : text(config.get("displayField"));
        return StringUtils.firstNonBlank(configured, target == null ? null : target.getDisplayField());
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

    private void restoreDesignerOptionsFromSnapshot(AiBusinessObject object, String designerOptionsSnapshot) {
        if (object == null) {
            return;
        }
        object.setDesignerOptions(StringUtils.isBlank(designerOptionsSnapshot) ? "{}" : designerOptionsSnapshot);
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

    private FormDesignerSchemaDTO resolveFormDesignerSchema(AiBusinessObject object, LowcodeModelSchema modelSchema,
                                                            LowcodePageSchema pageSchema,
                                                            Map<String, Object> designerOptions) {
        if (designerOptions != null && designerOptions.containsKey(FORM_DESIGNER_SCHEMA_OPTION_KEY)) {
            Object value = designerOptions.get(FORM_DESIGNER_SCHEMA_OPTION_KEY);
            try {
                if (value instanceof String text && StringUtils.isNotBlank(text)) {
                    return objectMapper.readValue(text, FormDesignerSchemaDTO.class);
                }
                if (value != null) {
                    return objectMapper.convertValue(value, FormDesignerSchemaDTO.class);
                }
            } catch (Exception ignored) {
                return buildDefaultFormDesignerSchema(object, modelSchema, pageSchema);
            }
        }
        FormDesignerSchemaDTO migrated = migrateFormDesignerSchemaFromPageSchema(object, modelSchema, pageSchema);
        if (migrated != null) {
            return migrated;
        }
        return buildDefaultFormDesignerSchema(object, modelSchema, pageSchema);
    }

    private FormDesignerSchemaDTO buildDefaultFormDesignerSchema(AiBusinessObject object, LowcodeModelSchema modelSchema,
                                                                 LowcodePageSchema pageSchema) {
        FormDesignerSchemaDTO schema = new FormDesignerSchemaDTO();
        String modelCode = resolveModelCode(object);
        schema.setFormKey(modelCode + "_default_form");
        schema.setFormName(StringUtils.defaultIfBlank(object.getObjectName(), modelCode) + "表单");
        Map<String, Object> layout = resolveFormDesignerLayout(pageSchema);
        schema.setLayout(layout);

        if (modelSchema == null || modelSchema.getFields() == null) {
            return schema;
        }
        List<Map<String, Object>> components = new ArrayList<>();
        int index = 0;
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (field == null
                    || Boolean.TRUE.equals(field.getSystemField())
                    || Boolean.TRUE.equals(field.getReadonly())
                    || Boolean.FALSE.equals(field.getFormVisible())) {
                continue;
            }
            components.add(buildDefaultFormComponent(field, index++));
        }
        schema.setComponents(components);
        return schema;
    }

    private FormDesignerSchemaDTO migrateFormDesignerSchemaFromPageSchema(AiBusinessObject object,
                                                                          LowcodeModelSchema modelSchema,
                                                                          LowcodePageSchema pageSchema) {
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        if (editZone == null || editZone.getProps() == null) {
            return null;
        }
        List<Map<String, Object>> formCreateRules = listOfMap(editZone.getProps().get("formCreateRule"));
        if (!formCreateRules.isEmpty()) {
            return migrateFormCreateRulesToFormDesignerSchema(object, modelSchema, pageSchema, formCreateRules);
        }
        if (editZone.getFieldRefs() == null || editZone.getFieldRefs().isEmpty()) {
            return null;
        }
        Map<String, LowcodeFieldSchema> fieldMap = lowcodeFieldMap(modelSchema);
        Map<String, Object> fieldSettings = mapValue(editZone.getProps().get("fieldSettings"));
        FormDesignerSchemaDTO schema = createBaseMigratedFormSchema(object, pageSchema, "pageSchema");
        List<Map<String, Object>> components = new ArrayList<>();
        int index = 0;
        for (String fieldRef : editZone.getFieldRefs()) {
            LowcodeFieldSchema field = fieldMap.get(fieldRef);
            if (field == null || Boolean.TRUE.equals(field.getSystemField())) {
                continue;
            }
            components.add(buildMigratedPageComponent(field, fieldSettings.get(fieldRef), index++));
        }
        if (components.isEmpty()) {
            return null;
        }
        schema.setComponents(components);
        return schema;
    }

    private FormDesignerSchemaDTO migrateFormCreateRulesToFormDesignerSchema(AiBusinessObject object,
                                                                             LowcodeModelSchema modelSchema,
                                                                             LowcodePageSchema pageSchema,
                                                                             List<Map<String, Object>> rules) {
        FormDesignerSchemaDTO schema = createBaseMigratedFormSchema(object, pageSchema, "formCreateRule");
        Map<String, LowcodeFieldSchema> fieldMap = lowcodeFieldMap(modelSchema);
        int gridColumns = integerValue(schema.getLayout().get("gridColumns"), 2);
        List<Map<String, Object>> components = new ArrayList<>();
        for (int index = 0; index < rules.size(); index++) {
            Map<String, Object> component = migrateFormCreateRule(rules.get(index), fieldMap, gridColumns, index);
            if (component != null) {
                components.add(component);
            }
        }
        if (components.isEmpty()) {
            return null;
        }
        schema.setComponents(components);
        return schema;
    }

    private FormDesignerSchemaDTO createBaseMigratedFormSchema(AiBusinessObject object, LowcodePageSchema pageSchema,
                                                               String source) {
        FormDesignerSchemaDTO schema = new FormDesignerSchemaDTO();
        String modelCode = resolveModelCode(object);
        schema.setFormKey(modelCode + "_default_form");
        schema.setFormName(StringUtils.defaultIfBlank(object.getObjectName(), modelCode) + "表单");
        schema.setLayout(resolveFormDesignerLayout(pageSchema));
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("migratedFrom", source);
        schema.setSettings(settings);
        return schema;
    }

    private Map<String, Object> buildMigratedPageComponent(LowcodeFieldSchema field, Object settingValue, int index) {
        Map<String, Object> component = buildDefaultFormComponent(field, index);
        Map<String, Object> setting = mapValue(settingValue);
        String componentType = text(setting.get("componentType"));
        if (StringUtils.isNotBlank(componentType)) {
            component.put("componentKey", normalizeFormComponentKey(componentType));
        }
        Map<String, Object> props = new LinkedHashMap<>(mapValue(component.get("props")));
        props.putAll(mapValue(setting.get("props")));
        putIfNotBlank(props, "dictType", text(setting.get("dictType")));
        if (setting.containsKey("defaultValue")) {
            props.put("defaultValue", setting.get("defaultValue"));
        }
        component.put("props", props);

        Map<String, Object> layout = new LinkedHashMap<>(mapValue(component.get("layout")));
        if (setting.containsKey("span")) {
            layout.put("span", integerValue(setting.get("span"), integerValue(layout.get("span"), 1)));
        }
        if (setting.containsKey("align")) {
            layout.put("align", normalizeAlign(text(setting.get("align"))));
        }
        if (setting.containsKey("labelWidth")) {
            layout.put("labelWidth", integerValue(setting.get("labelWidth"), 100));
        }
        component.put("layout", layout);

        Map<String, Object> validation = new LinkedHashMap<>(mapValue(component.get("validation")));
        if (setting.containsKey("required")) {
            validation.put("required", readBoolean(setting.get("required"), false));
        }
        component.put("validation", validation);

        Map<String, Object> visibility = new LinkedHashMap<>(mapValue(component.get("visibility")));
        if (setting.containsKey("readonly")) {
            visibility.put("readonly", readBoolean(setting.get("readonly"), false));
        }
        component.put("visibility", visibility);
        return component;
    }

    private Map<String, Object> migrateFormCreateRule(Map<String, Object> rule,
                                                      Map<String, LowcodeFieldSchema> fieldMap,
                                                      int gridColumns,
                                                      int index) {
        if (rule == null) {
            return null;
        }
        String fieldCode = resolveFormCreateFieldCode(rule);
        LowcodeFieldSchema field = fieldMap.get(fieldCode);
        String componentKey = resolveFormCreateComponentKey(rule, field);
        if (!FORM_FIELD_COMPONENT_KEYS.contains(componentKey) && StringUtils.isBlank(fieldCode)) {
            return null;
        }
        String label = StringUtils.firstNonBlank(text(rule.get("title")), text(rule.get("label")),
                field == null ? null : field.getLabel(), fieldCode, "字段");
        Map<String, Object> component = new LinkedHashMap<>();
        component.put("id", StringUtils.firstNonBlank(text(getNestedValue(rule, "_forge.id")),
                text(rule.get("id")), "cmp_" + StringUtils.defaultIfBlank(fieldCode, String.valueOf(index + 1))));
        component.put("componentKey", componentKey);
        component.put("label", label);

        Map<String, Object> binding = new LinkedHashMap<>(mapValue(getNestedValue(rule, "_forge.fieldBinding")));
        binding.putIfAbsent("mode", StringUtils.isBlank(fieldCode) ? "virtual" : "field");
        binding.putIfAbsent("fieldCode", fieldCode);
        binding.putIfAbsent("createIfMissing", false);
        binding.putIfAbsent("source", "migration");
        binding.putIfAbsent("locked", field == null ? false : Boolean.TRUE.equals(field.getReadonly()));
        component.put("fieldBinding", binding);

        component.put("props", buildMigratedRuleProps(rule));
        component.put("layout", buildMigratedRuleLayout(rule, gridColumns));
        component.put("validation", buildMigratedRuleValidation(rule, componentKey, label));
        component.put("visibility", buildMigratedRuleVisibility(rule));
        List<Map<String, Object>> children = new ArrayList<>();
        List<Map<String, Object>> childRules = listOfMap(rule.get("children"));
        for (int childIndex = 0; childIndex < childRules.size(); childIndex++) {
            Map<String, Object> child = migrateFormCreateRule(childRules.get(childIndex), fieldMap, gridColumns, childIndex);
            if (child != null) {
                children.add(child);
            }
        }
        component.put("children", children);
        return component;
    }

    private Map<String, Object> buildMigratedRuleProps(Map<String, Object> rule) {
        Map<String, Object> props = new LinkedHashMap<>(mapValue(rule.get("props")));
        props.putAll(mapValue(getNestedValue(rule, "_forge.props")));
        if (rule.containsKey("value")) {
            props.put("defaultValue", rule.get("value"));
        }
        if (rule.get("options") instanceof List<?> options) {
            props.put("options", options);
        }
        return props;
    }

    private Map<String, Object> buildMigratedRuleLayout(Map<String, Object> rule, int gridColumns) {
        Map<String, Object> layout = new LinkedHashMap<>(mapValue(getNestedValue(rule, "_forge.layout")));
        int span = integerValue(layout.get("span"), 1);
        Map<String, Object> col = mapValue(rule.get("col"));
        if (col.containsKey("span")) {
            int colSpan = integerValue(col.get("span"), 24);
            span = Math.max(1, Math.min(gridColumns, (int) Math.ceil(gridColumns * Math.min(24, colSpan) / 24.0)));
        }
        layout.put("span", span);
        layout.put("align", normalizeAlign(text(layout.get("align"))));
        return layout;
    }

    private Map<String, Object> buildMigratedRuleValidation(Map<String, Object> rule, String componentKey, String label) {
        Map<String, Object> validation = new LinkedHashMap<>();
        List<Map<String, Object>> rules = listOfMap(rule.get("validate"));
        boolean required = rules.stream().anyMatch(item -> readBoolean(item.get("required"), false));
        validation.put("required", required);
        validation.put("requiredMessage", required
                ? rules.stream()
                .map(item -> text(item.get("message")))
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(buildFormPlaceholder(componentKey, label))
                : "");
        validation.put("rules", rules);
        return validation;
    }

    private Map<String, Object> buildMigratedRuleVisibility(Map<String, Object> rule) {
        Map<String, Object> visibility = new LinkedHashMap<>(mapValue(getNestedValue(rule, "_forge.visibility")));
        visibility.put("hidden", readBoolean(rule.get("hidden"), readBoolean(visibility.get("hidden"), false)));
        visibility.put("readonly", readBoolean(getNestedValue(rule, "props.disabled"),
                readBoolean(visibility.get("readonly"), false)));
        return visibility;
    }

    private String resolveFormCreateFieldCode(Map<String, Object> rule) {
        String fieldCode = text(getNestedValue(rule, "_forge.fieldBinding.fieldCode"));
        return StringUtils.firstNonBlank(fieldCode, text(rule.get("field")), text(rule.get("name")));
    }

    private String resolveFormCreateComponentKey(Map<String, Object> rule, LowcodeFieldSchema field) {
        String componentKey = text(getNestedValue(rule, "_forge.componentKey"));
        if (StringUtils.isNotBlank(componentKey)) {
            return normalizeFormComponentKey(componentKey);
        }
        String dragTag = text(rule.get("_fc_drag_tag"));
        componentKey = switch (StringUtils.defaultString(dragTag)) {
            case "forgeDictSelect" -> "dictSelect";
            case "forgeRegionTreeSelect" -> "regionTreeSelect";
            case "forgeOrgTreeSelect" -> "orgTreeSelect";
            case "forgeUserSelect" -> "userSelect";
            case "forgeFileUpload" -> "fileUpload";
            case "forgeImageUpload" -> "imageUpload";
            case "forgeObjectReference" -> "objectReference";
            default -> null;
        };
        if (StringUtils.isNotBlank(componentKey)) {
            return componentKey;
        }
        String type = text(rule.get("type"));
        Map<String, Object> props = mapValue(rule.get("props"));
        if ("input".equals(type) && "textarea".equals(text(props.get("type")))) {
            return "textarea";
        }
        if ("inputNumber".equals(type)) {
            return "number";
        }
        if ("datePicker".equals(type)) {
            return "datetime".equals(text(props.get("type"))) ? "datetime" : "date";
        }
        if ("timePicker".equals(type)) {
            return "time";
        }
        if ("upload".equals(type)) {
            return "picture-card".equals(text(props.get("listType"))) || "image/*".equals(text(props.get("accept")))
                    ? "imageUpload" : "fileUpload";
        }
        if ("select".equals(type) && StringUtils.isNotBlank(text(props.get("dictType")))) {
            return "dictSelect";
        }
        Map<String, String> typeMap = Map.ofEntries(
                Map.entry("input", "input"),
                Map.entry("select", "select"),
                Map.entry("radio", "radio"),
                Map.entry("checkbox", "checkbox"),
                Map.entry("switch", "switch"),
                Map.entry("cascader", "cascader"),
                Map.entry("tree", "orgTreeSelect"),
                Map.entry("elTreeSelect", "orgTreeSelect")
        );
        return StringUtils.defaultIfBlank(typeMap.get(type), field == null ? "input" : resolveFormComponentKey(field));
    }

    private Map<String, Object> resolveFormDesignerLayout(LowcodePageSchema pageSchema) {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("labelPlacement", "left");
        layout.put("labelWidth", 100);
        layout.put("gridColumns", 2);
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        if (editZone == null || editZone.getProps() == null) {
            return layout;
        }
        Map<String, Object> props = editZone.getProps();
        layout.put("labelPlacement", StringUtils.defaultIfBlank(text(props.get("labelPlacement")), "left"));
        layout.put("labelWidth", integerValue(props.get("labelWidth"), 100));
        layout.put("gridColumns", clamp(integerValue(props.get("editGridCols"), 2), 1, 3));
        Map<String, Object> options = mapValue(props.get("formCreateOptions"));
        Map<String, Object> form = mapValue(options.get("form"));
        Map<String, Object> forge = mapValue(options.get("_forge"));
        String labelPosition = text(form.get("labelPosition"));
        if (StringUtils.isNotBlank(labelPosition)) {
            layout.put("labelPlacement", "top".equals(labelPosition) ? "top" : "left");
        }
        if (form.containsKey("labelWidth")) {
            layout.put("labelWidth", integerValue(form.get("labelWidth"), integerValue(layout.get("labelWidth"), 100)));
        }
        if (forge.containsKey("gridColumns")) {
            layout.put("gridColumns", clamp(integerValue(forge.get("gridColumns"), integerValue(layout.get("gridColumns"), 2)), 1, 3));
        }
        return layout;
    }

    private Map<String, Object> buildDefaultFormComponent(LowcodeFieldSchema field, int index) {
        Map<String, Object> component = new LinkedHashMap<>();
        String fieldCode = StringUtils.defaultIfBlank(field.getField(), "field" + index);
        String label = StringUtils.defaultIfBlank(field.getLabel(), fieldCode);
        String componentKey = resolveFormComponentKey(field);
        component.put("id", "cmp_" + fieldCode);
        component.put("componentKey", componentKey);
        component.put("label", label);

        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("mode", "field");
        binding.put("fieldCode", fieldCode);
        binding.put("createIfMissing", false);
        binding.put("source", "field_asset");
        binding.put("locked", Boolean.TRUE.equals(field.getReadonly()));
        component.put("fieldBinding", binding);

        Map<String, Object> props = new LinkedHashMap<>();
        if (field.getBasicProps() != null) {
            props.putAll(field.getBasicProps());
            props.remove("fieldBinding");
        }
        props.putIfAbsent("placeholder", buildFormPlaceholder(componentKey, label));
        props.putIfAbsent("clearable", true);
        if (StringUtils.isNotBlank(field.getDictType())) {
            props.put("dictType", field.getDictType());
        }
        if (StringUtils.isNotBlank(field.getReferenceObjectCode())) {
            props.put("referenceObjectCode", field.getReferenceObjectCode());
        }
        if (StringUtils.isNotBlank(field.getReferenceDisplayField())) {
            props.put("referenceDisplayField", field.getReferenceDisplayField());
        }
        if (field.getLength() != null) {
            props.put("maxlength", field.getLength());
        }
        component.put("props", props);

        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("span", Set.of("textarea", "fileUpload", "imageUpload", "subTable").contains(componentKey) ? 2 : 1);
        layout.put("align", "left");
        component.put("layout", layout);

        Map<String, Object> validation = new LinkedHashMap<>();
        validation.put("required", Boolean.TRUE.equals(field.getRequired()));
        validation.put("requiredMessage", Boolean.TRUE.equals(field.getRequired())
                ? buildFormPlaceholder(componentKey, label)
                : "");
        component.put("validation", validation);

        Map<String, Object> visibility = new LinkedHashMap<>();
        visibility.put("hidden", false);
        visibility.put("readonly", Boolean.TRUE.equals(field.getReadonly()));
        component.put("visibility", visibility);
        return component;
    }

    private String resolveFormComponentKey(LowcodeFieldSchema field) {
        String componentType = StringUtils.defaultString(field.getComponentType());
        String businessType = StringUtils.defaultString(field.getBusinessFieldType()).toUpperCase(Locale.ROOT);
        if ("textarea".equals(componentType) || "MULTILINE".equals(businessType)) {
            return "textarea";
        }
        if ("number".equals(componentType) || "NUMBER".equals(businessType)) {
            return "number";
        }
        if ("MONEY".equals(businessType)) {
            return "money";
        }
        if ("datetime".equals(componentType) || "DATETIME".equals(businessType)) {
            return "datetime";
        }
        if ("date".equals(componentType) || "DATE".equals(businessType)) {
            return "date";
        }
        if ("time".equals(componentType)) {
            return "time";
        }
        if ("switch".equals(componentType) || "SWITCH".equals(businessType)) {
            return "switch";
        }
        if ("radio".equals(componentType) || "RADIO".equals(businessType)) {
            return "radio";
        }
        if ("checkbox".equals(componentType) || Set.of("CHECKBOX", "MULTI_SELECT").contains(businessType)) {
            return "checkbox";
        }
        if (StringUtils.isNotBlank(field.getDictType()) || "dictSelect".equals(componentType)) {
            return "dictSelect";
        }
        if ("regionTreeSelect".equals(componentType) || "REGION".equals(businessType)) {
            return "regionTreeSelect";
        }
        if ("orgTreeSelect".equals(componentType) || "DEPT".equals(businessType)) {
            return "orgTreeSelect";
        }
        if ("userSelect".equals(componentType) || "USER".equals(businessType)) {
            return "userSelect";
        }
        if ("imageUpload".equals(componentType) || "IMAGE".equals(businessType)) {
            return "imageUpload";
        }
        if (Set.of("fileUpload", "upload").contains(componentType)
                || Set.of("FILE", "ATTACHMENT").contains(businessType)) {
            return "fileUpload";
        }
        if ("REFERENCE".equals(businessType)) {
            return "objectReference";
        }
        if ("select".equals(componentType) || Set.of("SELECT", "DICT").contains(businessType)) {
            return "select";
        }
        return "input";
    }

    private String buildFormPlaceholder(String componentKey, String label) {
        if (Set.of("select", "radio", "checkbox", "dictSelect", "date", "datetime", "time",
                "regionTreeSelect", "orgTreeSelect", "userSelect", "fileUpload", "imageUpload",
                "objectReference").contains(componentKey)) {
            return "请选择" + label;
        }
        return "请输入" + label;
    }

    private ViewSchemaDTO resolveViewSchema(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema,
                                            Map<String, Object> designerOptions) {
        if (designerOptions != null && designerOptions.containsKey(VIEW_SCHEMA_OPTION_KEY)) {
            Object value = designerOptions.get(VIEW_SCHEMA_OPTION_KEY);
            try {
                if (value instanceof String text && StringUtils.isNotBlank(text)) {
                    return objectMapper.readValue(text, ViewSchemaDTO.class);
                }
                if (value != null) {
                    return objectMapper.convertValue(value, ViewSchemaDTO.class);
                }
            } catch (Exception ignored) {
                return buildDefaultViewSchema(modelSchema, pageSchema);
            }
        }
        return buildDefaultViewSchema(modelSchema, pageSchema);
    }

    private ViewSchemaDTO buildDefaultViewSchema(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        ViewSchemaDTO schema = new ViewSchemaDTO();
        List<LowcodeFieldSchema> fields = modelSchema == null || modelSchema.getFields() == null
                ? new ArrayList<>()
                : modelSchema.getFields();
        schema.getSearch().put("fields", fields.stream()
                .filter(field -> field != null && Boolean.TRUE.equals(field.getSearchable()))
                .map(this::buildDefaultSearchField)
                .toList());
        schema.getList().put("columns", fields.stream()
                .filter(field -> field != null && !Boolean.FALSE.equals(field.getListVisible()))
                .map(this::buildDefaultListColumn)
                .toList());
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("sectionKey", "basic");
        section.put("title", "基础信息");
        section.put("fields", fields.stream()
                .filter(field -> field != null && !Boolean.FALSE.equals(field.getFormVisible()))
                .map(this::buildDefaultDetailField)
                .toList());
        schema.getDetail().put("sections", List.of(section));
        if (pageSchema != null && StringUtils.isNotBlank(pageSchema.getLayoutType())) {
            schema.getOverrides().put("layoutType", pageSchema.getLayoutType());
        }
        return schema;
    }

    private Map<String, Object> buildDefaultSearchField(LowcodeFieldSchema field) {
        Map<String, Object> item = buildDefaultViewField(field);
        item.put("componentKey", resolveFormComponentKey(field));
        item.put("matchMode", StringUtils.defaultIfBlank(field.getQueryType(), "eq"));
        item.put("collapsed", false);
        item.put("defaultValue", field.getDefaultValue());
        return item;
    }

    private Map<String, Object> buildDefaultListColumn(LowcodeFieldSchema field) {
        Map<String, Object> item = buildDefaultViewField(field);
        item.put("width", field.getWidth());
        item.put("fixed", null);
        item.put("sortable", Boolean.TRUE.equals(field.getSortable()));
        item.put("formatter", StringUtils.isNotBlank(field.getDictType()) ? "dictTag" : null);
        return item;
    }

    private Map<String, Object> buildDefaultDetailField(LowcodeFieldSchema field) {
        Map<String, Object> item = buildDefaultViewField(field);
        item.put("readonly", true);
        item.put("formatter", StringUtils.isNotBlank(field.getDictType()) ? "dictTag" : null);
        return item;
    }

    private Map<String, Object> buildDefaultViewField(LowcodeFieldSchema field) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fieldCode", field.getField());
        item.put("label", StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        item.put("visible", true);
        item.put("order", field.getSortOrder() == null ? 0 : field.getSortOrder());
        item.put("align", resolveDefaultViewAlign(field));
        return item;
    }

    private String resolveDefaultViewAlign(LowcodeFieldSchema field) {
        if (Set.of("int", "bigint", "decimal").contains(StringUtils.defaultString(field.getDataType()))
                || "number".equals(field.getComponentType())) {
            return "right";
        }
        if (Set.of("switch", "date", "datetime").contains(StringUtils.defaultString(field.getComponentType()))) {
            return "center";
        }
        return "left";
    }

    private LinkageSchemaDTO resolveLinkageSchema(Map<String, Object> designerOptions) {
        if (designerOptions != null && designerOptions.containsKey(LINKAGE_SCHEMA_OPTION_KEY)) {
            Object value = designerOptions.get(LINKAGE_SCHEMA_OPTION_KEY);
            try {
                if (value instanceof String text && StringUtils.isNotBlank(text)) {
                    return objectMapper.readValue(text, LinkageSchemaDTO.class);
                }
                if (value != null) {
                    return objectMapper.convertValue(value, LinkageSchemaDTO.class);
                }
            } catch (Exception ignored) {
                return new LinkageSchemaDTO();
            }
        }
        return new LinkageSchemaDTO();
    }

    private boolean hasDesignerOption(Map<String, Object> designerOptions, String key) {
        if (designerOptions == null || !designerOptions.containsKey(key)) {
            return false;
        }
        Object value = designerOptions.get(key);
        if (value instanceof String text) {
            return StringUtils.isNotBlank(text);
        }
        return value != null;
    }

    private void applyFormDesignerSchemaToModel(LowcodeModelSchema modelSchema, FormDesignerSchemaDTO formSchema) {
        if (modelSchema == null || formSchema == null || formSchema.getComponents() == null) {
            return;
        }
        Map<String, LowcodeFieldSchema> fieldMap = lowcodeFieldMap(modelSchema);
        int order = 1;
        for (Map<String, Object> component : flattenFormComponents(formSchema.getComponents())) {
            String componentKey = text(component.get("componentKey"));
            if (!FORM_FIELD_COMPONENT_KEYS.contains(componentKey)) {
                continue;
            }
            Map<String, Object> binding = mapValue(component.get("fieldBinding"));
            String bindingMode = StringUtils.defaultIfBlank(text(binding.get("mode")), "field");
            String fieldCode = text(binding.get("fieldCode"));
            if (!"field".equals(bindingMode) || StringUtils.isBlank(fieldCode)) {
                continue;
            }
            LowcodeFieldSchema field = fieldMap.get(fieldCode);
            if (field == null || Boolean.TRUE.equals(field.getSystemField())) {
                continue;
            }
            String label = text(component.get("label"));
            if (StringUtils.isNotBlank(label)) {
                field.setLabel(label);
            }
            String runtimeComponentType = normalizeRuntimeComponentType(componentKey);
            if (StringUtils.isNotBlank(runtimeComponentType)) {
                field.setComponentType(runtimeComponentType);
            }
            Map<String, Object> validation = mapValue(component.get("validation"));
            if (validation.containsKey("required")) {
                field.setRequired(readBoolean(validation.get("required"), false));
            }
            Map<String, Object> visibility = mapValue(component.get("visibility"));
            if (visibility.containsKey("hidden")) {
                field.setFormVisible(!readBoolean(visibility.get("hidden"), false));
            }
            if (visibility.containsKey("readonly")) {
                field.setReadonly(readBoolean(visibility.get("readonly"), false));
            }
            Map<String, Object> props = mapValue(component.get("props"));
            if (props.containsKey("defaultValue")) {
                field.setDefaultValue(props.get("defaultValue"));
            }
            String dictType = text(props.get("dictType"));
            if (StringUtils.isNotBlank(dictType)) {
                field.setDictType(dictType);
            }
            String referenceObjectCode = text(props.get("referenceObjectCode"));
            if (StringUtils.isNotBlank(referenceObjectCode)) {
                field.setReferenceObjectCode(referenceObjectCode);
            }
            String referenceDisplayField = text(props.get("referenceDisplayField"));
            if (StringUtils.isNotBlank(referenceDisplayField)) {
                field.setReferenceDisplayField(referenceDisplayField);
            }
            Map<String, Object> basicProps = field.getBasicProps() == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(field.getBasicProps());
            basicProps.putAll(props);
            basicProps.remove("fieldBinding");
            field.setBasicProps(basicProps);
            field.setSortOrder(order++);
        }
    }

    private void applyFormDesignerSchemaToEditZone(LowcodePageSchema pageSchema, LowcodeModelSchema modelSchema,
                                                   FormDesignerSchemaDTO formSchema) {
        if (pageSchema == null || modelSchema == null || formSchema == null) {
            return;
        }
        LowcodePageZone editZone = findOrCreateZone(pageSchema, "edit", "edit-form");
        Set<String> modelFields = lowcodeFieldMap(modelSchema).keySet();
        List<String> formFieldRefs = new ArrayList<>();
        Map<String, Object> compiledSettings = new LinkedHashMap<>();
        int gridColumns = clamp(integerValue(mapValue(formSchema.getLayout()).get("gridColumns"), 2), 1, 3);
        int defaultLabelWidth = integerValue(mapValue(formSchema.getLayout()).get("labelWidth"), 100);

        for (Map<String, Object> component : flattenFormComponents(formSchema.getComponents())) {
            String componentKey = text(component.get("componentKey"));
            if (!FORM_FIELD_COMPONENT_KEYS.contains(componentKey)) {
                continue;
            }
            Map<String, Object> binding = mapValue(component.get("fieldBinding"));
            String fieldCode = text(binding.get("fieldCode"));
            if (StringUtils.isBlank(fieldCode) || !modelFields.contains(fieldCode)) {
                continue;
            }
            Map<String, Object> visibility = mapValue(component.get("visibility"));
            if (readBoolean(visibility.get("hidden"), false)) {
                continue;
            }
            formFieldRefs.add(fieldCode);
            compiledSettings.put(fieldCode, buildFormFieldSetting(component, componentKey, gridColumns, defaultLabelWidth));
        }

        List<String> retainedRelationRefs = editZone.getFieldRefs() == null
                ? new ArrayList<>()
                : editZone.getFieldRefs().stream()
                .filter(ref -> StringUtils.isNotBlank(ref) && !modelFields.contains(ref))
                .toList();
        LinkedHashSet<String> refs = new LinkedHashSet<>(formFieldRefs);
        refs.addAll(retainedRelationRefs);
        editZone.setFieldRefs(new ArrayList<>(refs));

        Map<String, Object> props = editZone.getProps() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(editZone.getProps());
        replaceModelFieldSettings(props, modelFields, compiledSettings);
        props.put("editGridCols", gridColumns);
        props.put("labelPlacement", StringUtils.defaultIfBlank(text(mapValue(formSchema.getLayout()).get("labelPlacement")), "left"));
        props.put("labelWidth", defaultLabelWidth);
        props.put("compiledFrom", FORM_DESIGNER_SCHEMA_OPTION_KEY);
        props.remove("formCreateRule");
        props.remove("formCreateOptions");
        editZone.setProps(props);
    }

    private Map<String, Object> buildFormFieldSetting(Map<String, Object> component, String componentKey,
                                                      int gridColumns, int defaultLabelWidth) {
        Map<String, Object> setting = new LinkedHashMap<>();
        setting.put("componentType", normalizeRuntimeComponentType(componentKey));
        putIfNotBlank(setting, "label", text(component.get("label")));
        Map<String, Object> layout = mapValue(component.get("layout"));
        setting.put("align", normalizeAlign(text(layout.get("align"))));
        setting.put("span", clamp(integerValue(layout.get("span"), 1), 1, gridColumns));
        setting.put("labelWidth", integerValue(layout.get("labelWidth"), defaultLabelWidth));
        Map<String, Object> props = new LinkedHashMap<>(mapValue(component.get("props")));
        props.remove("fieldBinding");
        if (!props.isEmpty()) {
            setting.put("props", props);
        }
        Map<String, Object> validation = mapValue(component.get("validation"));
        if (validation.containsKey("required")) {
            setting.put("required", readBoolean(validation.get("required"), false));
        }
        Map<String, Object> visibility = mapValue(component.get("visibility"));
        if (visibility.containsKey("readonly")) {
            setting.put("readonly", readBoolean(visibility.get("readonly"), false));
        }
        putIfNotBlank(setting, "dictType", text(props.get("dictType")));
        if (props.containsKey("defaultValue")) {
            setting.put("defaultValue", props.get("defaultValue"));
        }
        return setting;
    }

    private void applyViewSchemaToPageZones(LowcodePageSchema pageSchema, LowcodeModelSchema modelSchema,
                                            ViewSchemaDTO viewSchema) {
        if (pageSchema == null || modelSchema == null || viewSchema == null) {
            return;
        }
        Set<String> modelFields = lowcodeFieldMap(modelSchema).keySet();
        applySearchViewZone(pageSchema, modelFields, viewSchema.getSearch());
        applyListViewZone(pageSchema, modelFields, viewSchema.getList());
        applyDetailViewZone(pageSchema, modelFields, viewSchema.getDetail());
        Map<String, Object> overrides = viewSchema.getOverrides() == null ? Map.of() : viewSchema.getOverrides();
        String layoutType = text(overrides.get("layoutType"));
        if (StringUtils.isNotBlank(layoutType)) {
            pageSchema.setLayoutType(layoutType);
        }
        String listLayoutMode = text(overrides.get("listLayoutMode"));
        if (StringUtils.isNotBlank(listLayoutMode)) {
            pageSchema.setListLayoutMode(listLayoutMode);
        }
        Map<String, Object> listGridLayout = mapValue(overrides.get("listGridLayout"));
        if (!listGridLayout.isEmpty()) {
            pageSchema.setListGridLayout(new LinkedHashMap<>(listGridLayout));
        }
    }

    private void applySearchViewZone(LowcodePageSchema pageSchema, Set<String> modelFields,
                                     Map<String, Object> searchSchema) {
        Map<String, Object> search = searchSchema == null ? Map.of() : searchSchema;
        List<Map<String, Object>> fields = visibleSortedItems(listOfMap(search.get("fields")));
        LowcodePageZone zone = findOrCreateZone(pageSchema, "search", "search-form");
        zone.setFieldRefs(fields.stream()
                .map(item -> StringUtils.defaultIfBlank(text(item.get("fieldCode")), text(item.get("field"))))
                .filter(modelFields::contains)
                .toList());
        Map<String, Object> props = zone.getProps() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(zone.getProps());
        props.putAll(mapValue(search.get("settings")));
        Map<String, Object> settings = new LinkedHashMap<>();
        for (Map<String, Object> item : fields) {
            String fieldCode = StringUtils.defaultIfBlank(text(item.get("fieldCode")), text(item.get("field")));
            if (!modelFields.contains(fieldCode)) {
                continue;
            }
            Map<String, Object> setting = new LinkedHashMap<>();
            setting.put("align", normalizeAlign(text(item.get("align"))));
            putIfNotBlank(setting, "componentType", text(item.get("componentKey")));
            putIfNotBlank(setting, "queryType", StringUtils.defaultIfBlank(text(item.get("matchMode")), text(item.get("queryType"))));
            if (item.containsKey("defaultValue")) {
                setting.put("defaultValue", item.get("defaultValue"));
            }
            setting.put("collapsed", readBoolean(item.get("collapsed"), false));
            settings.put(fieldCode, setting);
        }
        replaceModelFieldSettings(props, modelFields, settings);
        zone.setProps(props);
    }

    private void applyListViewZone(LowcodePageSchema pageSchema, Set<String> modelFields,
                                   Map<String, Object> listSchema) {
        Map<String, Object> list = listSchema == null ? Map.of() : listSchema;
        List<Map<String, Object>> columns = visibleSortedItems(listOfMap(list.get("columns")));
        LowcodePageZone zone = findOrCreateZone(pageSchema, "table", "data-table");
        zone.setFieldRefs(columns.stream()
                .map(item -> StringUtils.defaultIfBlank(text(item.get("fieldCode")), text(item.get("field"))))
                .filter(modelFields::contains)
                .toList());
        Map<String, Object> props = zone.getProps() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(zone.getProps());
        props.putAll(mapValue(list.get("settings")));
        Map<String, Object> settings = new LinkedHashMap<>();
        for (Map<String, Object> item : columns) {
            String fieldCode = StringUtils.defaultIfBlank(text(item.get("fieldCode")), text(item.get("field")));
            if (!modelFields.contains(fieldCode)) {
                continue;
            }
            Map<String, Object> setting = new LinkedHashMap<>();
            setting.put("align", normalizeAlign(text(item.get("align"))));
            putIfPresent(setting, "width", item.get("width"));
            putIfPresent(setting, "minWidth", item.get("minWidth"));
            putIfNotBlank(setting, "fixed", normalizeFixed(text(item.get("fixed"))));
            setting.put("sortable", readBoolean(item.get("sortable"), false));
            putIfNotBlank(setting, "renderType", text(item.get("formatter")));
            settings.put(fieldCode, setting);
        }
        replaceModelFieldSettings(props, modelFields, settings);
        zone.setProps(props);
    }

    private void applyDetailViewZone(LowcodePageSchema pageSchema, Set<String> modelFields,
                                     Map<String, Object> detailSchema) {
        Map<String, Object> detail = detailSchema == null ? Map.of() : detailSchema;
        List<Map<String, Object>> sections = visibleSortedItems(listOfMap(detail.get("sections")));
        LowcodePageZone zone = findOrCreateZone(pageSchema, "detail", "detail-view");
        List<String> fieldRefs = new ArrayList<>();
        List<Map<String, Object>> groups = new ArrayList<>();
        Map<String, Object> settings = new LinkedHashMap<>();
        for (int index = 0; index < sections.size(); index++) {
            Map<String, Object> section = sections.get(index);
            List<Map<String, Object>> fields = visibleSortedItems(listOfMap(section.get("fields")));
            List<Map<String, Object>> items = new ArrayList<>();
            for (Map<String, Object> field : fields) {
                String fieldCode = StringUtils.defaultIfBlank(text(field.get("fieldCode")), text(field.get("field")));
                if (!modelFields.contains(fieldCode)) {
                    continue;
                }
                fieldRefs.add(fieldCode);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("fieldRef", fieldCode);
                item.put("label", StringUtils.defaultIfBlank(text(field.get("label")), fieldCode));
                item.put("align", normalizeAlign(text(field.get("align"))));
                item.put("readonly", !isFalse(field.get("readonly")));
                items.add(item);

                Map<String, Object> setting = new LinkedHashMap<>();
                setting.put("align", normalizeAlign(text(field.get("align"))));
                putIfNotBlank(setting, "formatter", text(field.get("formatter")));
                settings.put(fieldCode, setting);
            }
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("key", StringUtils.defaultIfBlank(text(section.get("sectionKey")), "section_" + (index + 1)));
            group.put("title", StringUtils.defaultIfBlank(text(section.get("title")), "基础信息"));
            group.put("items", items);
            groups.add(group);
        }
        zone.setFieldRefs(new ArrayList<>(new LinkedHashSet<>(fieldRefs)));
        Map<String, Object> props = zone.getProps() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(zone.getProps());
        props.putAll(mapValue(detail.get("settings")));
        props.put("detailGroups", groups);
        replaceModelFieldSettings(props, modelFields, settings);
        zone.setProps(props);
    }

    private void applyLinkageSchemaToModel(LowcodeModelSchema modelSchema, LinkageSchemaDTO linkageSchema) {
        if (modelSchema == null || modelSchema.getFields() == null || linkageSchema == null) {
            return;
        }
        Map<String, Map<String, Object>> rulesByTarget = new LinkedHashMap<>();
        if (linkageSchema.getRules() != null) {
            for (Map<String, Object> rule : linkageSchema.getRules()) {
                if (rule == null || isFalse(rule.get("enabled"))) {
                    continue;
                }
                String targetField = text(rule.get("targetField"));
                if (StringUtils.isNotBlank(targetField) && !rulesByTarget.containsKey(targetField)) {
                    rulesByTarget.put(targetField, rule);
                }
            }
        }
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (field == null || StringUtils.isBlank(field.getField())) {
                continue;
            }
            Map<String, Object> basicProps = field.getBasicProps() == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(field.getBasicProps());
            Map<String, Object> rule = rulesByTarget.get(field.getField());
            if (rule == null) {
                Map<String, Object> cascade = mapValue(basicProps.get("cascade"));
                if (LINKAGE_SCHEMA_MANAGED_BY.equals(text(cascade.get("managedBy")))) {
                    basicProps.remove("cascade");
                }
                field.setBasicProps(basicProps);
                continue;
            }
            Map<String, Object> dictConfig = mapValue(rule.get("dictConfig"));
            Map<String, Object> objectConfig = mapValue(rule.get("objectConfig"));
            String targetDictType = text(dictConfig.get("targetDictType"));
            if (StringUtils.isNotBlank(targetDictType)) {
                field.setDictType(targetDictType);
            }
            String targetObjectCode = text(objectConfig.get("targetObjectCode"));
            if (StringUtils.isNotBlank(targetObjectCode)) {
                field.setReferenceObjectCode(targetObjectCode);
            }
            String displayField = text(objectConfig.get("displayField"));
            if (StringUtils.isNotBlank(displayField)) {
                field.setReferenceDisplayField(displayField);
            }
            basicProps.put("cascade", buildCascadeFromLinkageRule(rule, field));
            field.setBasicProps(basicProps);
        }
    }

    private Map<String, Object> buildCascadeFromLinkageRule(Map<String, Object> rule, LowcodeFieldSchema targetField) {
        String type = StringUtils.defaultIfBlank(text(rule.get("type")), text(rule.get("matchMode")));
        String dataSourceType = StringUtils.defaultIfBlank(text(rule.get("dataSourceType")), resolveLinkageDataSourceType(type));
        Map<String, Object> dictConfig = mapValue(rule.get("dictConfig"));
        Map<String, Object> remoteConfig = mapValue(rule.get("remoteConfig"));
        Map<String, Object> objectConfig = mapValue(rule.get("objectConfig"));
        Map<String, Object> orgConfig = mapValue(rule.get("orgConfig"));
        String mode = "dict".equals(dataSourceType) ? StringUtils.defaultIfBlank(text(rule.get("matchMode")), type) : "remoteParam";
        Map<String, Object> cascade = new LinkedHashMap<>();
        cascade.put("enabled", !isFalse(rule.get("enabled")));
        cascade.put("managedBy", LINKAGE_SCHEMA_MANAGED_BY);
        cascade.put("ruleId", text(rule.get("ruleId")));
        cascade.put("sourceField", text(rule.get("sourceField")));
        cascade.put("sourceDictType", text(dictConfig.get("sourceDictType")));
        cascade.put("targetDictType", StringUtils.defaultIfBlank(text(dictConfig.get("targetDictType")),
                targetField == null ? null : targetField.getDictType()));
        cascade.put("linkedDictType", StringUtils.firstNonBlank(text(dictConfig.get("linkedDictType")),
                text(dictConfig.get("sourceDictType"))));
        cascade.put("mode", mode);
        cascade.put("matchMode", mode);
        cascade.put("paramName", StringUtils.firstNonBlank(text(remoteConfig.get("paramName")),
                text(orgConfig.get("paramName")), text(rule.get("sourceField"))));
        cascade.put("emptyStrategy", StringUtils.defaultIfBlank(text(rule.get("emptyStrategy")), "empty"));
        cascade.put("clearOnParentChange", !isFalse(rule.get("clearOnSourceChange")));
        cascade.put("clearOnSourceChange", !isFalse(rule.get("clearOnSourceChange")));
        putIfNotBlank(cascade, "url", text(remoteConfig.get("url")));
        putIfNotBlank(cascade, "method", text(remoteConfig.get("method")));
        putIfNotBlank(cascade, "targetObjectCode", StringUtils.defaultIfBlank(text(objectConfig.get("targetObjectCode")),
                targetField == null ? null : targetField.getReferenceObjectCode()));
        putIfNotBlank(cascade, "displayField", StringUtils.defaultIfBlank(text(objectConfig.get("displayField")),
                targetField == null ? null : targetField.getReferenceDisplayField()));
        return cascade;
    }

    private String resolveLinkageDataSourceType(String type) {
        if ("parentDictCode".equals(type) || "linkedDict".equals(type)) {
            return "dict";
        }
        if ("orgScope".equals(type)) {
            return "org";
        }
        if ("objectReference".equals(type)) {
            return "object";
        }
        return "remote";
    }

    private LowcodePageZone findOrCreateZone(LowcodePageSchema pageSchema, String zoneKey, String componentKey) {
        if (pageSchema.getZones() == null) {
            pageSchema.setZones(new ArrayList<>());
        }
        LowcodePageZone zone = pageSchema.getZones().stream()
                .filter(item -> item != null && zoneKey.equals(item.getZoneKey()))
                .findFirst()
                .orElse(null);
        if (zone != null) {
            if (StringUtils.isBlank(zone.getComponentKey())) {
                zone.setComponentKey(componentKey);
            }
            if (zone.getProps() == null) {
                zone.setProps(new LinkedHashMap<>());
            }
            return zone;
        }
        zone = new LowcodePageZone();
        zone.setZoneKey(zoneKey);
        zone.setComponentKey(componentKey);
        zone.setEnabled(true);
        zone.setFieldRefs(new ArrayList<>());
        zone.setProps(new LinkedHashMap<>());
        pageSchema.getZones().add(zone);
        return zone;
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

    private void replaceModelFieldSettings(Map<String, Object> props, Set<String> modelFields,
                                           Map<String, Object> compiledSettings) {
        Map<String, Object> existing = new LinkedHashMap<>(mapValue(props.get("fieldSettings")));
        modelFields.forEach(existing::remove);
        existing.putAll(compiledSettings);
        props.put("fieldSettings", existing);
    }

    private List<Map<String, Object>> visibleSortedItems(List<Map<String, Object>> items) {
        return items.stream()
                .filter(item -> item != null && !isFalse(item.get("visible")))
                .sorted(Comparator.comparingInt(item -> integerValue(item.get("order"), 0)))
                .toList();
    }

    private List<Map<String, Object>> flattenFormComponents(List<Map<String, Object>> components) {
        List<Map<String, Object>> result = new ArrayList<>();
        collectFormComponents(components, result);
        return result;
    }

    private void collectFormComponents(List<Map<String, Object>> components, List<Map<String, Object>> result) {
        if (components == null) {
            return;
        }
        for (Map<String, Object> component : components) {
            if (component == null) {
                continue;
            }
            result.add(component);
            collectFormComponents(listOfMap(component.get("children")), result);
        }
    }

    private Map<String, LowcodeFieldSchema> lowcodeFieldMap(LowcodeModelSchema modelSchema) {
        Map<String, LowcodeFieldSchema> fields = new LinkedHashMap<>();
        if (modelSchema != null && modelSchema.getFields() != null) {
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field != null && StringUtils.isNotBlank(field.getField())) {
                    fields.put(field.getField(), field);
                }
            }
        }
        return fields;
    }

    private String normalizeRuntimeComponentType(String componentKey) {
        return switch (StringUtils.defaultString(componentKey)) {
            case "integer", "money" -> "number";
            default -> componentKey;
        };
    }

    private String normalizeFormComponentKey(String componentKey) {
        String normalized = StringUtils.defaultIfBlank(componentKey, "input");
        if ("inputNumber".equals(normalized)) {
            return "number";
        }
        return normalized;
    }

    private String normalizeAlign(String value) {
        String align = StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
        return Set.of("left", "center", "right").contains(align) ? align : "left";
    }

    private String normalizeFixed(String value) {
        String fixed = StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
        return Set.of("left", "right").contains(fixed) ? fixed : null;
    }

    private boolean isFalse(Object value) {
        return Boolean.FALSE.equals(value) || "false".equalsIgnoreCase(text(value)) || "0".equals(text(value));
    }

    private int integerValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                String digits = text.trim().replaceAll("[^0-9-]", "");
                if (StringUtils.isBlank(digits) || "-".equals(digits)) {
                    return defaultValue;
                }
                try {
                    return Integer.parseInt(digits);
                } catch (NumberFormatException ignoredAgain) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private Object getNestedValue(Map<String, Object> source, String path) {
        if (source == null || StringUtils.isBlank(path)) {
            return null;
        }
        Object current = source;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new LinkedHashMap<>();
    }

    private List<Map<String, Object>> listOfMap(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(this::mapValue)
                .toList();
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

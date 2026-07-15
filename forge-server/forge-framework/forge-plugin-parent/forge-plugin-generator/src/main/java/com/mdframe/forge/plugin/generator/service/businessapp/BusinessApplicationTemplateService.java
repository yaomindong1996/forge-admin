package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationObjectRole;
import com.mdframe.forge.plugin.generator.constant.BusinessObjectDesignStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationObjectDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationTemplateDetailDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationTemplateInitializeDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationTemplateObjectSourceDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDesignerDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectRelationDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTreeConfig;
import com.mdframe.forge.plugin.generator.service.IGenDatasourceService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectDesignerService.DesignerContext;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationTemplateResultVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectRelationVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 复用业务对象设计协议初始化常用应用模板。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationTemplateService {

    private static final String SINGLE_CRUD = "SINGLE_CRUD";
    private static final String TREE_TABLE = "TREE_TABLE";
    private static final String MASTER_DETAIL = "MASTER_DETAIL";
    private static final String SOURCE_DATABASE_TABLE = "DATABASE_TABLE";
    private static final String SOURCE_EXISTING_OBJECT = "EXISTING_OBJECT";
    private static final Set<String> SUPPORTED_TEMPLATES = Set.of(SINGLE_CRUD, TREE_TABLE, MASTER_DETAIL);

    private final BusinessApplicationService applicationService;
    private final BusinessApplicationObjectService applicationObjectService;
    private final BusinessObjectCreateService objectCreateService;
    private final BusinessObjectService objectService;
    private final BusinessObjectDesignerService designerService;
    private final BusinessNamingService namingService;
    private final IGenDatasourceService datasourceService;

    /**
     * 应用草稿已经单独创建；该事务只负责模板资产，失败时不遗留半套对象和关系。
     */
    @Transactional(rollbackFor = Exception.class)
    public BusinessApplicationTemplateResultVO initialize(
            Long applicationId, BusinessApplicationTemplateInitializeDTO dto) {
        AiBusinessApplication application = applicationService.requireEntity(applicationId);
        if (!applicationObjectService.list(applicationId).isEmpty()) {
            throw new BusinessException("当前应用已经包含数据对象，不能重复套用初始化模板");
        }
        String templateCode = normalizeTemplateCode(dto);
        CreatedObject primary = resolveObject(
                application,
                dto.getPrimarySource(),
                "MASTER",
                StringUtils.defaultIfBlank(StringUtils.trimToNull(dto.getPrimaryObjectName()), application.getApplicationName()),
                namingService.normalizeObjectCode(dto.getPrimaryObjectCode(), application.getApplicationCode()),
                primaryFields(templateCode, dto),
                "由" + templateLabel(templateCode) + "模板初始化");

        List<BusinessApplicationObjectDTO> applicationObjects = new ArrayList<>();
        applicationObjects.add(applicationObject(primary.id(), BusinessApplicationObjectRole.PRIMARY, 0, templateCode));

        if (TREE_TABLE.equals(templateCode)) {
            initializeTreeTemplate(application, primary, dto, applicationObjects);
        } else if (MASTER_DETAIL.equals(templateCode)) {
            initializeMasterDetailTemplate(application, primary, dto, applicationObjects);
        } else {
            configureLayout(primary.id(), "SINGLE", "simple-crud", null);
        }

        assertDistinctObjects(applicationObjects);
        applicationObjectService.replace(applicationId, applicationObjects);
        BusinessApplicationTemplateResultVO result = new BusinessApplicationTemplateResultVO();
        result.setApplicationId(applicationId);
        result.setTemplateCode(templateCode);
        result.setPrimaryObjectId(primary.id());
        result.setPrimaryObjectCode(primary.code());
        result.setObjects(applicationObjectService.list(applicationId));
        return result;
    }

    private void initializeTreeTemplate(
            AiBusinessApplication application,
            CreatedObject primary,
            BusinessApplicationTemplateInitializeDTO dto,
            List<BusinessApplicationObjectDTO> applicationObjects) {
        String legacyTreeName = StringUtils.defaultIfBlank(
                StringUtils.trimToNull(dto.getTreeObjectName()), primary.name() + "分类");
        String legacyTreeCode = namingService.normalizeObjectCode(
                dto.getTreeObjectCode(), primary.code() + "_category");
        CreatedObject tree = resolveObject(
                application,
                dto.getTreeSource(),
                "LOOKUP",
                legacyTreeName,
                legacyTreeCode,
                treeFields(legacyTreeName,
                        namingService.normalizeFieldCode(dto.getTreeLabelField(), "分类名称"),
                        namingService.normalizeFieldCode(dto.getTreeParentField(), "父级分类")),
                "左树右表模板的树形筛选对象");
        String keyField = requireField(tree, StringUtils.defaultIfBlank(dto.getTreeKeyField(), "id"), "树节点主键");
        String labelField = requireField(tree, dto.getTreeLabelField(), "树节点显示字段");
        String parentField = requireField(tree, dto.getTreeParentField(), "树父级字段");
        String filterField = requireField(primary, dto.getPrimaryTreeField(), "主表筛选字段");
        BusinessObjectRelationDTO relation = relation(
                primary.code(), tree.code(), "REFERENCE", tree.name(),
                filterField, keyField, Map.of("displayField", labelField));
        saveRelations(primary.id(), List.of(relation));

        LowcodeTreeConfig treeConfig = new LowcodeTreeConfig();
        treeConfig.setEnabled(true);
        treeConfig.setSourceModelCode(tree.modelCode());
        treeConfig.setSourceModelName(tree.name());
        treeConfig.setKeyField(keyField);
        treeConfig.setParentField(parentField);
        treeConfig.setLabelField(labelField);
        treeConfig.setFilterField(filterField);
        treeConfig.setTargetField(keyField);
        treeConfig.setChildrenField("children");
        treeConfig.setTreeTitle(tree.name());
        treeConfig.setLoadMode("full");
        configureLayout(primary.id(), "TREE", "tree-crud", treeConfig);

        applicationObjects.add(applicationObject(
                tree.id(), BusinessApplicationObjectRole.REFERENCE, applicationObjects.size(), TREE_TABLE));
    }

    private void initializeMasterDetailTemplate(
            AiBusinessApplication application,
            CreatedObject primary,
            BusinessApplicationTemplateInitializeDTO dto,
            List<BusinessApplicationObjectDTO> applicationObjects) {
        List<BusinessApplicationTemplateDetailDTO> details = normalizeDetails(primary, dto.getDetails());
        List<BusinessObjectRelationDTO> relations = new ArrayList<>();
        String primaryKeyField = requireField(
                primary, StringUtils.defaultIfBlank(dto.getPrimaryKeyField(), "id"), "主对象主键");
        int sortOrder = applicationObjects.size();
        for (BusinessApplicationTemplateDetailDTO detail : details) {
            String detailName = StringUtils.defaultIfBlank(
                    StringUtils.trimToNull(detail.getObjectName()), primary.name() + "明细");
            String detailCode = namingService.normalizeObjectCode(
                    detail.getObjectCode(), primary.code() + "_detail");
            String legacyForeignKey = namingService.normalizeFieldCode(
                    detail.getForeignKeyField(), primary.name() + "ID");
            CreatedObject child = resolveObject(
                    application,
                    detail.getSource(),
                    "DETAIL",
                    detailName,
                    detailCode,
                    detailFields(legacyForeignKey),
                    "主子表模板的明细对象");
            String foreignKeyField = requireField(child, detail.getForeignKeyField(), "子表外键");
            String relationName = StringUtils.defaultIfBlank(
                    StringUtils.trimToNull(detail.getRelationName()), child.name());
            relations.add(relation(primary.code(), child.code(), "CHILD_LIST", relationName,
                    primaryKeyField, foreignKeyField, inlineDetailConfig(relationName)));
            applicationObjects.add(applicationObject(
                    child.id(), BusinessApplicationObjectRole.DETAIL, sortOrder++, MASTER_DETAIL));
        }
        saveRelations(primary.id(), relations);
        configureLayout(primary.id(), "MASTER_DETAIL", "master-detail-crud", null);
    }

    private CreatedObject createObject(
            AiBusinessApplication application,
            String objectName,
            String objectCode,
            String objectType,
            List<BusinessFieldDTO> fields,
            String description) {
        BusinessObjectDTO object = new BusinessObjectDTO();
        object.setSuiteCode(application.getSuiteCode());
        object.setObjectName(StringUtils.trim(objectName));
        object.setObjectCode(objectCode);
        object.setModelCode(resolveModelCode(application.getSuiteCode(), objectCode));
        object.setObjectType(objectType);
        object.setCreateMode("BLANK");
        String displayField = resolveDisplayField(fields);
        object.setDisplayField(displayField);
        object.setDescription(description);
        object.setStatus(1);
        Long objectId = objectCreateService.create(object);

        BusinessObjectDesignerDTO designer = new BusinessObjectDesignerDTO();
        designer.setDisplayField(displayField);
        designer.setFields(fields);
        designerService.saveDesigner(objectId, designer);
        DesignerContext context = designerService.loadContext(objectId);
        context.setPageSchema(null);
        designerService.saveDraft(context, BusinessObjectDesignStatus.CHANGED);
        return new CreatedObject(
                objectId, objectCode, objectName, resolveModelCode(application.getSuiteCode(), objectCode));
    }

    private CreatedObject resolveObject(
            AiBusinessApplication application,
            BusinessApplicationTemplateObjectSourceDTO source,
            String objectType,
            String legacyObjectName,
            String legacyObjectCode,
            List<BusinessFieldDTO> legacyFields,
            String description) {
        if (source == null || StringUtils.isBlank(source.getSourceType())) {
            return createObject(application, legacyObjectName, legacyObjectCode, objectType, legacyFields, description);
        }
        String sourceType = StringUtils.trimToEmpty(source.getSourceType()).toUpperCase(Locale.ROOT);
        if (SOURCE_EXISTING_OBJECT.equals(sourceType)) {
            return resolveExistingObject(application, source.getObjectId());
        }
        if (SOURCE_DATABASE_TABLE.equals(sourceType)) {
            return importDatabaseTable(application, source, objectType, description);
        }
        throw new BusinessException("对象来源类型不正确");
    }

    private CreatedObject resolveExistingObject(AiBusinessApplication application, Long objectId) {
        if (objectId == null) {
            throw new BusinessException("请选择已有业务对象");
        }
        AiBusinessObject object = objectService.requireEntity(objectId);
        if (!StringUtils.equals(application.getSuiteCode(), object.getSuiteCode())) {
            throw new BusinessException("所选业务对象不属于当前应用业务域");
        }
        if (!Integer.valueOf(1).equals(object.getStatus())) {
            throw new BusinessException("所选业务对象已停用");
        }
        return new CreatedObject(
                object.getId(),
                object.getObjectCode(),
                object.getObjectName(),
                StringUtils.defaultIfBlank(object.getModelCode(),
                        resolveModelCode(application.getSuiteCode(), object.getObjectCode())));
    }

    private CreatedObject importDatabaseTable(
            AiBusinessApplication application,
            BusinessApplicationTemplateObjectSourceDTO source,
            String objectType,
            String description) {
        if (source.getDatasourceId() == null || StringUtils.isBlank(source.getTableName())) {
            throw new BusinessException("请选择数据源和数据表");
        }
        GenDatasource datasource = datasourceService.getById(source.getDatasourceId());
        if (datasource == null || !Integer.valueOf(1).equals(datasource.getIsEnabled())) {
            throw new BusinessException("所选数据源不存在或已停用");
        }
        GenTable table = datasourceService.selectDbTableByName(
                source.getDatasourceId(), StringUtils.trim(source.getTableName()));
        if (table == null) {
            throw new BusinessException("所选数据表不存在");
        }
        String objectName = StringUtils.defaultIfBlank(table.getTableComment(), table.getTableName());
        String objectCode = namingService.normalizeObjectCode(table.getTableName(), objectName);
        String modelCode = resolveModelCode(application.getSuiteCode(), objectCode);

        BusinessObjectDTO object = new BusinessObjectDTO();
        object.setSuiteCode(application.getSuiteCode());
        object.setObjectName(objectName);
        object.setObjectCode(objectCode);
        object.setModelCode(modelCode);
        object.setObjectType(objectType);
        object.setCreateMode("DB_IMPORT");
        object.setRuntimeDatasourceId(source.getDatasourceId());
        object.setImportDatasourceId(source.getDatasourceId());
        object.setImportTableName(table.getTableName());
        object.setDescription(description + "，来源表 " + table.getTableName());
        object.setOptions(databaseObjectOptions(datasource, table));
        object.setStatus(1);
        Long objectId = objectCreateService.create(object);
        return new CreatedObject(objectId, objectCode, objectName, modelCode);
    }

    private String databaseObjectOptions(GenDatasource datasource, GenTable table) {
        Map<String, Object> runtimeDatasource = new LinkedHashMap<>();
        runtimeDatasource.put("datasourceId", datasource.getDatasourceId());
        runtimeDatasource.put("datasourceCode", datasource.getDatasourceCode());
        runtimeDatasource.put("datasourceName", datasource.getDatasourceName());
        runtimeDatasource.put("dbType", datasource.getDbType());
        runtimeDatasource.put("usageScope", datasource.getUsageScope());
        runtimeDatasource.put("allowWrite", Integer.valueOf(1).equals(datasource.getAllowRuntimeWrite()));
        runtimeDatasource.put("allowDdl", Integer.valueOf(1).equals(datasource.getAllowRuntimeDdl()));
        runtimeDatasource.put("readonly", Integer.valueOf(1).equals(datasource.getReadonly()));
        runtimeDatasource.put("riskLevel", datasource.getRiskLevel());
        runtimeDatasource.put("tableMode", "EXISTING");

        Map<String, Object> sourceTable = new LinkedHashMap<>();
        sourceTable.put("datasourceId", datasource.getDatasourceId());
        sourceTable.put("tableName", table.getTableName());
        sourceTable.put("tableComment", table.getTableComment());

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("createMode", "DB_IMPORT");
        options.put("runtimeDatasourceId", datasource.getDatasourceId());
        options.put("runtimeDatasource", runtimeDatasource);
        options.put("sourceTable", sourceTable);
        return JSON.toJSONString(options);
    }

    private String requireField(CreatedObject object, String requestedField, String fieldLabel) {
        String field = StringUtils.trimToNull(requestedField);
        if (field == null) {
            throw new BusinessException("请选择" + fieldLabel);
        }
        DesignerContext context = designerService.loadContext(object.id());
        List<LowcodeFieldSchema> modelFields = context.getModelSchema().getFields() == null
                ? List.of() : context.getModelSchema().getFields();
        Set<String> fields = modelFields.stream()
                .filter(item -> item != null && StringUtils.isNotBlank(item.getField()))
                .map(LowcodeFieldSchema::getField)
                .collect(java.util.stream.Collectors.toSet());
        fields.add("id");
        if (!fields.contains(field)) {
            throw new BusinessException(fieldLabel + "不存在于对象「" + object.name() + "」中");
        }
        return field;
    }

    private String resolveDisplayField(List<BusinessFieldDTO> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        return fields.stream()
                .map(BusinessFieldDTO::getFieldCode)
                .filter(StringUtils::isNotBlank)
                .filter(code -> Set.of("name", "itemName", "title", "businessCode").contains(code))
                .findFirst()
                .orElse(fields.get(0).getFieldCode());
    }

    private void saveRelations(Long primaryObjectId, List<BusinessObjectRelationDTO> relations) {
        List<BusinessObjectRelationDTO> merged = designerService.getDesigner(primaryObjectId).getRelations().stream()
                .map(this::toRelationDTO)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (BusinessObjectRelationDTO relation : relations) {
            int existingIndex = -1;
            for (int index = 0; index < merged.size(); index++) {
                BusinessObjectRelationDTO existing = merged.get(index);
                if (StringUtils.equals(existing.getTargetObjectCode(), relation.getTargetObjectCode())
                        && StringUtils.equals(existing.getRelationType(), relation.getRelationType())) {
                    relation.setId(existing.getId());
                    existingIndex = index;
                    break;
                }
            }
            if (existingIndex >= 0) {
                merged.set(existingIndex, relation);
            } else {
                merged.add(relation);
            }
        }
        BusinessObjectDesignerDTO designer = new BusinessObjectDesignerDTO();
        designer.setRelations(merged);
        designerService.saveDesigner(primaryObjectId, designer);
    }

    private BusinessObjectRelationDTO toRelationDTO(BusinessObjectRelationVO source) {
        BusinessObjectRelationDTO target = new BusinessObjectRelationDTO();
        target.setId(source.getId());
        target.setSuiteCode(source.getSuiteCode());
        target.setSourceObjectCode(source.getSourceObjectCode());
        target.setTargetObjectCode(source.getTargetObjectCode());
        target.setRelationType(source.getRelationType());
        target.setRelationName(source.getRelationName());
        target.setSourceFieldCode(source.getSourceFieldCode());
        target.setTargetFieldCode(source.getTargetFieldCode());
        target.setRelationConfig(source.getRelationConfig());
        target.setDescription(source.getDescription());
        target.setStatus(source.getStatus());
        target.setSortOrder(source.getSortOrder());
        return target;
    }

    private void configureLayout(
            Long primaryObjectId, String appType, String layoutType, LowcodeTreeConfig treeConfig) {
        DesignerContext context = designerService.loadContext(primaryObjectId);
        LowcodeModelSchema modelSchema = context.getModelSchema();
        modelSchema.setAppType(appType);
        modelSchema.setTreeConfig(treeConfig);
        LowcodePageSchema pageSchema = context.getPageSchema();
        pageSchema.setLayoutType(layoutType);
        pageSchema.setListLayoutMode("grid");
        if (treeConfig != null) {
            LowcodePageZone tableZone = pageSchema.getZones().stream()
                    .filter(zone -> "table".equals(zone.getZoneKey()))
                    .findFirst()
                    .orElse(null);
            if (tableZone != null) {
                Map<String, Object> props = tableZone.getProps() == null
                        ? new LinkedHashMap<>() : new LinkedHashMap<>(tableZone.getProps());
                props.put("treeConfig", treeConfig);
                tableZone.setProps(props);
            }
        }
        context.setModelSchema(modelSchema);
        context.setPageSchema(pageSchema);
        designerService.saveDraft(context, BusinessObjectDesignStatus.CHANGED);
    }

    private List<BusinessFieldDTO> primaryFields(
            String templateCode, BusinessApplicationTemplateInitializeDTO dto) {
        List<BusinessFieldDTO> fields = new ArrayList<>();
        fields.add(textField("业务编码", "businessCode", true, true, 0));
        fields.add(textField("名称", "name", true, true, 1));
        if (TREE_TABLE.equals(templateCode)) {
            String filterField = namingService.normalizeFieldCode(dto.getPrimaryTreeField(), "分类ID");
            fields.add(numberField(StringUtils.defaultIfBlank(dto.getTreeObjectName(), "分类"),
                    filterField, "bigint", true, 2, "treeSelect"));
        }
        fields.add(switchField("启用状态", "enabled", fields.size()));
        fields.add(multilineField("备注", "remark", fields.size()));
        return fields;
    }

    private List<BusinessFieldDTO> treeFields(String treeName, String labelField, String parentField) {
        return List.of(
                textField(treeName + "名称", labelField, true, true, 0),
                numberField("上级" + treeName, parentField, "bigint", false, 1, "treeSelect"),
                numberField("排序", "sortOrder", "int", false, 2, "number"),
                switchField("启用状态", "enabled", 3)
        );
    }

    private List<BusinessFieldDTO> detailFields(String foreignKeyField) {
        return List.of(
                numberField("所属主记录", foreignKeyField, "bigint", true, 0, "number"),
                textField("明细名称", "itemName", true, true, 1),
                numberField("数量", "quantity", "int", true, 2, "number"),
                moneyField("金额", "amount", 3),
                multilineField("备注", "remark", 4)
        );
    }

    private BusinessFieldDTO textField(
            String label, String code, boolean required, boolean searchable, int sortOrder) {
        BusinessFieldDTO field = baseField(label, code, "TEXT", sortOrder);
        field.setRequired(required);
        field.setSearchable(searchable);
        field.setLength(128);
        return field;
    }

    private BusinessFieldDTO multilineField(String label, String code, int sortOrder) {
        BusinessFieldDTO field = baseField(label, code, "MULTILINE", sortOrder);
        field.setComponentType("textarea");
        field.setListVisible(false);
        return field;
    }

    private BusinessFieldDTO numberField(
            String label, String code, String dataType, boolean required, int sortOrder, String componentType) {
        BusinessFieldDTO field = baseField(label, code, "NUMBER", sortOrder);
        field.setDataType(dataType);
        field.setRequired(required);
        field.setComponentType(componentType);
        return field;
    }

    private BusinessFieldDTO moneyField(String label, String code, int sortOrder) {
        BusinessFieldDTO field = baseField(label, code, "MONEY", sortOrder);
        field.setDataType("decimal");
        field.setLength(18);
        field.setPrecision(2);
        return field;
    }

    private BusinessFieldDTO switchField(String label, String code, int sortOrder) {
        BusinessFieldDTO field = baseField(label, code, "SWITCH", sortOrder);
        field.setDefaultValue(1);
        field.setSearchable(true);
        return field;
    }

    private BusinessFieldDTO baseField(String label, String code, String type, int sortOrder) {
        BusinessFieldDTO field = new BusinessFieldDTO();
        field.setFieldName(label);
        field.setFieldCode(namingService.normalizeFieldCode(code, label));
        field.setFieldType(type);
        field.setRequired(false);
        field.setSearchable(false);
        field.setListVisible(true);
        field.setFormVisible(true);
        field.setImportable(true);
        field.setExportable(true);
        field.setSortable(false);
        field.setSystemField(false);
        field.setReadonly(false);
        field.setFieldStatus("ENABLED");
        field.setSortOrder(sortOrder);
        return field;
    }

    private List<BusinessApplicationTemplateDetailDTO> normalizeDetails(
            CreatedObject primary, List<BusinessApplicationTemplateDetailDTO> requested) {
        List<BusinessApplicationTemplateDetailDTO> details = requested == null
                ? new ArrayList<>() : requested.stream().filter(item -> item != null).toList();
        if (details.isEmpty()) {
            BusinessApplicationTemplateDetailDTO detail = new BusinessApplicationTemplateDetailDTO();
            detail.setObjectName(primary.name() + "明细");
            detail.setObjectCode(primary.code() + "_detail");
            detail.setForeignKeyField(primary.code() + "Id");
            detail.setRelationName("明细");
            details = List.of(detail);
        }
        for (BusinessApplicationTemplateDetailDTO detail : details) {
            if (detail.getSource() == null && StringUtils.isBlank(detail.getObjectName())) {
                throw new BusinessException("请填写子表名称");
            }
        }
        return details;
    }

    private void assertDistinctObjects(List<BusinessApplicationObjectDTO> applicationObjects) {
        Set<Long> objectIds = new java.util.HashSet<>();
        for (BusinessApplicationObjectDTO item : applicationObjects) {
            if (!objectIds.add(item.getObjectId())) {
                throw new BusinessException("主对象、树对象和子对象不能选择同一个业务对象或数据表");
            }
        }
    }

    private BusinessObjectRelationDTO relation(
            String sourceObjectCode,
            String targetObjectCode,
            String relationType,
            String relationName,
            String sourceField,
            String targetField,
            Map<String, Object> config) {
        BusinessObjectRelationDTO relation = new BusinessObjectRelationDTO();
        relation.setSourceObjectCode(sourceObjectCode);
        relation.setTargetObjectCode(targetObjectCode);
        relation.setRelationType(relationType);
        relation.setRelationName(relationName);
        relation.setSourceFieldCode(sourceField);
        relation.setTargetFieldCode(targetField);
        relation.setRelationConfig(JSON.toJSONString(config));
        relation.setStatus(1);
        relation.setSortOrder(0);
        return relation;
    }

    private Map<String, Object> inlineDetailConfig(String relationName) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("showInDetail", true);
        config.put("inlineCreateEnabled", true);
        config.put("inlineEditEnabled", true);
        config.put("saveMode", "replace");
        config.put("detailTabTitle", relationName);
        return config;
    }

    private BusinessApplicationObjectDTO applicationObject(
            Long objectId, String role, int sortOrder, String templateCode) {
        BusinessApplicationObjectDTO item = new BusinessApplicationObjectDTO();
        item.setObjectId(objectId);
        item.setObjectRole(role);
        item.setSortOrder(sortOrder);
        item.setOptions(JSON.toJSONString(Map.of("source", "APPLICATION_TEMPLATE", "templateCode", templateCode)));
        return item;
    }

    private String normalizeTemplateCode(BusinessApplicationTemplateInitializeDTO dto) {
        if (dto == null) {
            throw new BusinessException("请选择应用模板");
        }
        String code = StringUtils.defaultString(dto.getTemplateCode()).trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_TEMPLATES.contains(code)) {
            throw new BusinessException("应用模板不正确");
        }
        return code;
    }

    private String resolveModelCode(String suiteCode, String objectCode) {
        return namingService.buildModelCode(suiteCode, objectCode);
    }

    private String templateLabel(String templateCode) {
        return switch (templateCode) {
            case TREE_TABLE -> "左树右表";
            case MASTER_DETAIL -> "主子表";
            default -> "单表 CRUD";
        };
    }

    private record CreatedObject(Long id, String code, String name, String modelCode) {
    }
}

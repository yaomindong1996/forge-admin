package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationObjectRole;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationObjectDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPageDesignDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDesignerDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.FormDesignerSchemaDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.service.IGenDatasourceService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlService;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPageDesignVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectRelationVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/**
 * 页面形态、对象结构与应用导航的原子元数据保存服务。
 *
 * <p>MySQL DDL 存在隐式提交，因此数据表同步只在元数据事务提交后执行；
 * 同步失败时保留可重试的页面设计，不能把 DDL 伪装成数据库事务的一部分。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessApplicationPageDesignService {

    private static final BusinessApplicationTreePageProjector TREE_PAGE_PROJECTOR =
            new BusinessApplicationTreePageProjector();

    private static final String MANAGED_BY_PAGE_FORM = "PAGE_FORM";
    private static final String LOWCODE_RUNTIME = "LOWCODE_RUNTIME";
    private static final String CREATE_MODE_BLANK = "BLANK";
    private static final String CREATE_MODE_DB_IMPORT = "DB_IMPORT";
    private static final Set<String> PAGE_TYPES = Set.of("form", "list", "list-form", "tree-list", "tree-table", "custom");
    private static final Set<String> EMBEDDED_RELATION_TYPES = Set.of("CHILD_LIST", "DETAIL");

    private final ObjectMapper objectMapper;
    private final BusinessApplicationService applicationService;
    private final BusinessApplicationObjectService applicationObjectService;
    private final BusinessObjectService objectService;
    private final BusinessObjectCreateService objectCreateService;
    private final BusinessObjectDesignerService designerService;
    private final BusinessNamingService namingService;
    private final IGenDatasourceService datasourceService;
    private final BusinessObjectTableMappingService tableMappingService;
    private final LowcodeDdlService ddlService;
    private final LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver;
    private final DynamicCrudRepository dynamicCrudRepository;
    private final PlatformTransactionManager transactionManager;

    public BusinessApplicationPageDesignVO save(Long applicationId, BusinessApplicationPageDesignDTO request) {
        PageDesignRequest normalized = normalizeRequest(request);
        MetadataResult saved = new TransactionTemplate(transactionManager).execute(
                status -> saveMetadata(applicationId, normalized));
        if (saved == null) {
            throw new BusinessException("页面设计保存失败，请重试");
        }
        if (saved.objectId() != null && saved.shouldSyncDatabase()) {
            try {
                tableMappingService.syncManagedDatabase(
                        saved.objectId(), applicationId, normalized.formAssetId());
            } catch (RuntimeException error) {
                // DDL 硬失败不阻断保存：元数据已提交；安全变更由 syncManagedDatabase 静默处理，不向用户弹提示。
                log.warn("[页面设计保存] DDL 同步失败已忽略提示: objectId={}, formAssetId={}, error={}",
                        saved.objectId(), normalized.formAssetId(), error.getMessage());
            }
            syncChildManagedTables(saved.objectId());
        }
        return saved.result();
    }

    /**
     * 表单子表组件保存时会自动给子对象补外键字段；这里在元数据事务提交后同步
     * 页面表单托管的子对象数据表，避免运行时子表写入时外键列缺失。
     */
    private void syncChildManagedTables(Long objectId) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        AiBusinessObject object = context.getObject();
        List<BusinessObjectRelationVO> relations = context.getRelations();
        if (object == null || relations == null || relations.isEmpty()) {
            return;
        }
        Set<String> synced = new LinkedHashSet<>();
        for (BusinessObjectRelationVO relation : relations) {
            if (relation == null
                    || !object.getObjectCode().equals(relation.getSourceObjectCode())
                    || !EMBEDDED_RELATION_TYPES.contains(
                            StringUtils.defaultString(relation.getRelationType()).toUpperCase(Locale.ROOT))
                    || !EnableStatus.ENABLED.matches(relation.getStatus())
                    || StringUtils.isBlank(relation.getTargetObjectCode())
                    || !synced.add(relation.getTargetObjectCode())) {
                continue;
            }
            AiBusinessObject child;
            try {
                child = objectService.requireByCode(object.getSuiteCode(), relation.getTargetObjectCode());
            } catch (BusinessException missing) {
                continue;
            }
            JSONObject marker = readOptions(child.getOptions());
            Long childApplicationId = marker.getLong("sourceApplicationId");
            String childFormAssetId = marker.getString("sourceFormAssetId");
            if (!MANAGED_BY_PAGE_FORM.equals(marker.getString("managedBy"))
                    || childApplicationId == null || StringUtils.isBlank(childFormAssetId)) {
                continue;
            }
            try {
                tableMappingService.syncManagedDatabase(child.getId(), childApplicationId, childFormAssetId);
            } catch (RuntimeException error) {
                // 子表 DDL 同步失败同样不阻断主表保存流程
                log.warn("[页面设计保存] 子表 DDL 同步失败，已降级为警告: childObject={}, error={}",
                        child.getObjectCode(), error.getMessage());
            }
        }
    }

    private MetadataResult saveMetadata(Long applicationId, PageDesignRequest request) {
        AiBusinessApplication application = applicationService.requireEntity(applicationId);
        if ("custom".equals(request.pageType())) {
            Map<String, Object> builder = cloneBuilder(request.builder());
            saveApplicationBuilder(application, builder);
            return new MetadataResult(null, customResult(request, builder), false);
        }

        List<BusinessApplicationObjectVO> associations = applicationObjectService.list(applicationId);
        AiBusinessObject object = resolvePageObject(applicationId, associations, request);
        boolean created = object == null;
        if (created) {
            object = objectService.requireEntity(createPageObject(application, request));
        } else {
            updatePageObject(application, object, request);
            object = objectService.requireEntity(object.getId());
        }

        BusinessObjectDesignerService.DesignerContext current = designerService.loadContext(object.getId());
        LowcodeModelSchema currentModel = current.getModelSchema();
        // 数据量快照：守卫报错时附带真实条数与表名，用户能定位数据来源
        BusinessDataSnapshot businessData = inspectBusinessData(currentModel);
        java.util.function.Function<String, Boolean> columnDataChecker =
                createColumnDataChecker(currentModel, businessData);
        // 换子表等操作常漏传画布外字段；已有列数据的字段必须保留，避免误判为删除。
        List<BusinessFieldDTO> effectiveFields = retainExistingFieldsWithData(
                currentModel == null ? List.of() : currentModel.getFields(),
                request.fields(),
                columnDataChecker);
        BusinessApplicationPageFieldGuard.assertCompatible(
                businessData.count(),
                businessData.tableName(),
                currentModel == null ? List.of() : currentModel.getFields(),
                effectiveFields,
                columnDataChecker);

        FormDesignerSchemaDTO formSchema = request.formDesignerSchema();
        if (businessData.hasData()) {
            Object persistedFormSchema = current.getObject() == null
                    ? null
                    : readOptions(current.getObject().getDesignerOptions()).get("formDesignerSchema");
            Set<String> retainedFieldCodes = effectiveFields.stream()
                    .map(BusinessFieldDTO::getFieldCode)
                    .filter(StringUtils::isNotBlank)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            BusinessApplicationPageFieldGuard.assertLockedFormComponentsUnchanged(
                    persistedFormSchema,
                    objectMapper.convertValue(formSchema, new TypeReference<Map<String, Object>>() {
                    }),
                    currentModel == null ? List.of() : currentModel.getFields(),
                    businessData.count(),
                    businessData.tableName(),
                    columnDataChecker,
                    retainedFieldCodes);
            lockPersistedFieldBindings(formSchema);
        }
        BusinessObjectDesignerDTO designer = new BusinessObjectDesignerDTO();
        designer.setObjectId(object.getId());
        designer.setObjectName(request.objectName());
        designer.setDisplayField(resolveDisplayField(effectiveFields));
        designer.setFields(effectiveFields);
        designer.setFormDesignerSchema(formSchema);
        TREE_PAGE_PROJECTOR.applyDesignerPreset(
                designer,
                current.getModelSchema(),
                current.getPageSchema(),
                request.pageType(),
                request.objectName(),
                request.builder(),
                request.pageId());
        designerService.saveDesigner(object.getId(), designer);

        AiBusinessObject savedObject = objectService.requireEntity(object.getId());
        attachVisiblePageObject(applicationId, associations, savedObject.getId(), request);
        Map<String, Object> builder = cloneBuilder(request.builder());
        synchronizeBuilderFormAsset(builder, request.formAssetId(), formSchema);
        patchBuilderObjectReference(builder, request, savedObject, businessData.hasData());
        TREE_PAGE_PROJECTOR.patchBuilder(builder, request.pageId(), request.pageType());
        if (businessData.hasData()) {
            lockBuilderFormFields(builder, request.formAssetId());
        }
        saveApplicationBuilder(application, builder);

        BusinessApplicationPageDesignVO result = baseResult(request, builder);
        result.setObjectId(savedObject.getId());
        result.setObjectCode(savedObject.getObjectCode());
        result.setObjectName(savedObject.getObjectName());
        result.setConfigKey(savedObject.getConfigKey());
        result.setObjectCreated(created);
        result.setHasBusinessData(businessData.hasData());
        return new MetadataResult(
                savedObject.getId(),
                result,
                !(created && CREATE_MODE_DB_IMPORT.equals(request.createMode())));
    }

    private PageDesignRequest normalizeRequest(BusinessApplicationPageDesignDTO request) {
        if (request == null) {
            throw new BusinessException("页面设计不能为空");
        }
        String pageId = requiredText(request.getPageId(), "页面标识不能为空", 128);
        String pageType = StringUtils.lowerCase(StringUtils.trimToEmpty(request.getPageType()), Locale.ROOT);
        if (!PAGE_TYPES.contains(pageType)) {
            throw new BusinessException("页面形态不正确");
        }
        Map<String, Object> builder = request.getBuilder() == null ? Map.of() : request.getBuilder();
        assertBuilderContainsPage(builder, pageId);
        if ("custom".equals(pageType)) {
            return new PageDesignRequest(pageId, pageType, "", null, "", "",
                    List.of(), null, builder, null, CREATE_MODE_BLANK, null, null);
        }
        String formAssetId = requiredText(request.getFormAssetId(), "表单标识不能为空", 128);
        String objectName = requiredText(request.getObjectName(), "对象名称不能为空", 100);
        String objectCode = namingService.normalizeObjectCode(request.getObjectCode(), objectName);
        List<BusinessFieldDTO> fields = normalizeFields(request.getFields());
        if (fields.isEmpty()) {
            throw new BusinessException("请先添加至少一个字段组件");
        }
        if (request.getFormDesignerSchema() == null) {
            throw new BusinessException("表单设计不能为空");
        }
        assertBuilderContainsFormAsset(builder, pageId, formAssetId);
        String createMode = normalizeCreateMode(request.getCreateMode());
        String importTableName = StringUtils.trimToNull(request.getImportTableName());
        if (CREATE_MODE_DB_IMPORT.equals(createMode) && importTableName == null) {
            throw new BusinessException("请选择要引用的数据表");
        }
        return new PageDesignRequest(pageId, pageType, formAssetId, request.getObjectId(),
                objectCode, objectName, fields, request.getFormDesignerSchema(), builder,
                request.getRuntimeDatasourceId(), createMode,
                request.getImportDatasourceId(), importTableName);
    }

    private List<BusinessFieldDTO> normalizeFields(List<BusinessFieldDTO> fields) {
        List<BusinessFieldDTO> normalized = new ArrayList<>();
        Set<String> codes = new LinkedHashSet<>();
        for (BusinessFieldDTO field : (fields == null ? List.<BusinessFieldDTO>of() : fields)) {
            if (field == null || Boolean.TRUE.equals(field.getSystemField())
                    || StringUtils.isBlank(field.getFieldCode())) {
                continue;
            }
            String originalCode = StringUtils.trimToEmpty(field.getFieldCode());
            // 子表列引用（model__field）禁止写入主对象字段目录
            if (originalCode.contains("__")) {
                log.warn("[页面设计] 跳过子表合成字段，禁止写入主对象: {}", originalCode);
                continue;
            }
            field.setFieldCode(namingService.normalizeFieldCode(field.getFieldCode(), field.getFieldName()));
            field.setColumnName(namingService.camelToSnake(
                    StringUtils.defaultIfBlank(field.getColumnName(), field.getFieldCode())));
            // 前端历史脏数据会同时带上 entName + ent_name；归一化后等价，保留首次出现即可。
            // 真正语义冲突仍会在对象设计器/DDL 侧暴露，这里不要因 snake/camel 双胞胎阻断进设计器。
            if (!codes.add(field.getFieldCode())) {
                log.warn("[页面设计] 跳过归一化后重复的字段编码: {} (fieldName={})",
                        field.getFieldCode(), field.getFieldName());
                continue;
            }
            normalized.add(field);
        }
        return normalized;
    }

    private AiBusinessObject resolvePageObject(
            Long applicationId,
            List<BusinessApplicationObjectVO> associations,
            PageDesignRequest request) {
        if (request.objectId() != null) {
            boolean associated = safeAssociations(associations).stream()
                    .anyMatch(item -> request.objectId().equals(item.getObjectId()));
            if (!associated) {
                throw new BusinessException("当前页面绑定的对象不属于此应用");
            }
            return objectService.requireEntity(request.objectId());
        }
        return safeAssociations(associations).stream()
                .filter(item -> pageMarkerMatches(item.getOptions(), applicationId, request))
                .map(BusinessApplicationObjectVO::getObjectId)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .map(objectService::requireEntity)
                .orElse(null);
    }

    private Long createPageObject(AiBusinessApplication application, PageDesignRequest request) {
        boolean dbImport = CREATE_MODE_DB_IMPORT.equals(request.createMode());
        GenDatasource datasource = resolveRuntimeDatasource(request.runtimeDatasourceId(), !dbImport);
        BusinessObjectDTO object = new BusinessObjectDTO();
        object.setSuiteCode(application.getSuiteCode());
        object.setObjectCode(request.objectCode());
        object.setObjectName(request.objectName());
        object.setObjectType("MASTER");
        object.setCreateMode(dbImport ? CREATE_MODE_DB_IMPORT : CREATE_MODE_BLANK);
        object.setRuntimeDatasourceId(datasource.getDatasourceId());
        if (dbImport) {
            object.setImportDatasourceId(request.importDatasourceId() == null
                    ? datasource.getDatasourceId()
                    : request.importDatasourceId());
            object.setImportTableName(request.importTableName());
        }
        object.setModelCode(namingService.buildModelCode(application.getSuiteCode(), request.objectCode()));
        object.setDisplayField(resolveDisplayField(request.fields()));
        object.setDescription("由应用“" + application.getApplicationName() + "”中的页面自动管理");
        object.setStatus(EnableStatus.ENABLED.getCode());
        object.setOptions(buildObjectOptions(application.getId(), request, datasource));
        return objectCreateService.create(object);
    }

    private void updatePageObject(
            AiBusinessApplication application, AiBusinessObject existing, PageDesignRequest request) {
        JSONObject options = readOptions(existing.getOptions());
        options.putAll(pageMarker(application.getId(), request));
        BusinessObjectDTO object = new BusinessObjectDTO();
        object.setId(existing.getId());
        object.setSuiteCode(existing.getSuiteCode());
        object.setObjectCode(request.objectCode());
        object.setObjectName(request.objectName());
        object.setObjectType(existing.getObjectType());
        object.setModelId(existing.getModelId());
        object.setModelCode(existing.getModelCode());
        object.setDisplayField(resolveDisplayField(request.fields()));
        object.setIcon(existing.getIcon());
        object.setDescription(existing.getDescription());
        object.setStatus(existing.getStatus());
        object.setSortOrder(existing.getSortOrder());
        object.setOptions(options.toJSONString());
        objectService.update(object);
    }

    /** 业务数据量快照：count 为真实行数，tableName 为实际统计的数据表 */
    private record BusinessDataSnapshot(long count, String tableName) {
        boolean hasData() {
            return count > 0;
        }
    }

    private BusinessDataSnapshot inspectBusinessData(LowcodeModelSchema modelSchema) {
        if (modelSchema == null || StringUtils.isBlank(modelSchema.getTableName())
                || !hasBusinessFields(modelSchema)) {
            return new BusinessDataSnapshot(0, StringUtils.defaultString(
                    modelSchema == null ? null : modelSchema.getTableName()));
        }
        if (!Boolean.TRUE.equals(ddlService.previewCreateTable(modelSchema).getTableExists())) {
            return new BusinessDataSnapshot(0, modelSchema.getTableName());
        }
        LowcodeRuntimeDataSourceContext context = runtimeDataSourceResolver.resolve(modelSchema);
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored =
                     LowcodeRuntimeDataSourceContextHolder.use(context)) {
            return new BusinessDataSnapshot(
                    dynamicCrudRepository.countList(
                            context.getTableName(), Map.of(), Set.of(), Map.of(), Map.of(), null),
                    context.getTableName());
        }
    }

    /**
     * 创建字段级数据存在性检查器：按列名探测非空数据是否存在（LIMIT 1，大表安全）。
     * 物理列不存在时返回 false（无数据可丢失）；查询异常时降级返回 true（保守阻断）。
     */
    private java.util.function.Function<String, Boolean> createColumnDataChecker(
            LowcodeModelSchema modelSchema, BusinessDataSnapshot businessData) {
        if (modelSchema == null || StringUtils.isBlank(modelSchema.getTableName())
                || !businessData.hasData()) {
            return null;
        }
        LowcodeRuntimeDataSourceContext context;
        try {
            context = runtimeDataSourceResolver.resolve(modelSchema);
        } catch (RuntimeException e) {
            return null;
        }
        return columnName -> {
            if (StringUtils.isBlank(columnName)) {
                return true;
            }
            try (LowcodeRuntimeDataSourceContextHolder.Scope ignored =
                         LowcodeRuntimeDataSourceContextHolder.use(context)) {
                Set<String> columns = dynamicCrudRepository.getTableColumns(context.getTableName());
                if (!columns.contains(columnName.toLowerCase(Locale.ROOT))) {
                    // 物理列不存在，该字段无数据
                    return false;
                }
                return dynamicCrudRepository.hasColumnData(context.getTableName(), columnName);
            } catch (RuntimeException e) {
                log.warn("[字段守卫] 字段级数据探测失败，降级为保守阻断: column={}, error={}",
                        columnName, e.getMessage());
                return true;
            }
        };
    }

    /**
     * 请求字段列表漏传时，把“列上已有数据”的存量字段补回，避免换子表等保存被误判成删字段。
     * 列上无数据的字段仍可不传，以便真正清理空字段。
     */
    private List<BusinessFieldDTO> retainExistingFieldsWithData(
            List<LowcodeFieldSchema> existingFields,
            List<BusinessFieldDTO> requestedFields,
            java.util.function.Function<String, Boolean> columnDataChecker) {
        List<BusinessFieldDTO> requested = requestedFields == null
                ? new ArrayList<>()
                : new ArrayList<>(requestedFields);
        Set<String> requestedCodes = requested.stream()
                .map(BusinessFieldDTO::getFieldCode)
                .filter(StringUtils::isNotBlank)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        for (LowcodeFieldSchema existing : existingFields == null ? List.<LowcodeFieldSchema>of() : existingFields) {
            if (existing == null || Boolean.TRUE.equals(existing.getSystemField())
                    || StringUtils.isBlank(existing.getField())
                    || requestedCodes.contains(existing.getField())) {
                continue;
            }
            String columnName = StringUtils.defaultString(existing.getColumnName());
            boolean hasData = columnDataChecker == null || Boolean.TRUE.equals(columnDataChecker.apply(columnName));
            if (!hasData) {
                continue;
            }
            requested.add(toRetainedBusinessField(existing));
            requestedCodes.add(existing.getField());
            log.info("[页面设计保存] 补回已有数据字段，避免误删: field={}, label={}, column={}",
                    existing.getField(), existing.getLabel(), columnName);
        }
        return requested;
    }

    private BusinessFieldDTO toRetainedBusinessField(LowcodeFieldSchema existing) {
        BusinessFieldDTO field = new BusinessFieldDTO();
        field.setFieldCode(existing.getField());
        field.setFieldName(StringUtils.defaultIfBlank(existing.getLabel(), existing.getField()));
        field.setColumnName(existing.getColumnName());
        field.setFieldType(existing.getBusinessFieldType());
        field.setDataType(existing.getDataType());
        field.setLength(existing.getLength());
        field.setPrecision(existing.getPrecision());
        field.setRequired(Boolean.TRUE.equals(existing.getRequired()));
        field.setDefaultValue(existing.getDefaultValue() == null ? null : String.valueOf(existing.getDefaultValue()));
        field.setListVisible(existing.getListVisible() == null || Boolean.TRUE.equals(existing.getListVisible()));
        field.setFormVisible(existing.getFormVisible() == null || Boolean.TRUE.equals(existing.getFormVisible()));
        field.setComponentType(existing.getComponentType());
        field.setQueryType(existing.getQueryType());
        field.setDictType(existing.getDictType());
        field.setSystemField(false);
        field.setReadonly(Boolean.TRUE.equals(existing.getReadonly()));
        field.setFieldStatus(StringUtils.defaultIfBlank(existing.getFieldStatus(), "ENABLED"));
        field.setSortOrder(existing.getSortOrder());
        return field;
    }

    private boolean hasBusinessFields(LowcodeModelSchema modelSchema) {
        if (modelSchema.getFields() == null || modelSchema.getFields().isEmpty()) {
            return false;
        }
        return modelSchema.getFields().stream()
                .anyMatch(field -> field != null && !Boolean.TRUE.equals(field.getSystemField()));
    }

    private void attachVisiblePageObject(
            Long applicationId,
            List<BusinessApplicationObjectVO> current,
            Long objectId,
            PageDesignRequest request) {
        List<BusinessApplicationObjectDTO> next = new ArrayList<>();
        boolean found = false;
        for (BusinessApplicationObjectVO association : safeAssociations(current)) {
            BusinessApplicationObjectDTO item = new BusinessApplicationObjectDTO();
            item.setObjectId(association.getObjectId());
            item.setObjectRole(association.getObjectRole());
            item.setSortOrder(association.getSortOrder());
            item.setOptions(association.getOptions());
            if (objectId.equals(association.getObjectId())) {
                JSONObject options = readOptions(association.getOptions());
                options.putAll(pageMarker(applicationId, request));
                item.setOptions(options.toJSONString());
                found = true;
            }
            next.add(item);
        }
        if (!found) {
            BusinessApplicationObjectDTO managed = new BusinessApplicationObjectDTO();
            managed.setObjectId(objectId);
            managed.setObjectRole(next.isEmpty()
                    ? BusinessApplicationObjectRole.PRIMARY
                    : BusinessApplicationObjectRole.SHARED);
            managed.setSortOrder(next.size());
            managed.setOptions(pageMarker(applicationId, request).toJSONString());
            next.add(managed);
        }
        applicationObjectService.replace(applicationId, next);
    }

    private void saveApplicationBuilder(AiBusinessApplication application, Map<String, Object> builder) {
        JSONObject options = readOptions(application.getOptions());
        options.put("inAppBuilder", builder);
        BusinessApplicationDTO applicationDTO = new BusinessApplicationDTO();
        applicationDTO.setId(application.getId());
        applicationDTO.setApplicationCode(application.getApplicationCode());
        applicationDTO.setApplicationName(application.getApplicationName());
        applicationDTO.setSuiteCode(application.getSuiteCode());
        applicationDTO.setIcon(application.getIcon());
        applicationDTO.setDescription(application.getDescription());
        applicationDTO.setStatus(application.getStatus());
        applicationDTO.setOptions(options.toJSONString());
        applicationService.update(applicationDTO);
    }

    private void patchBuilderObjectReference(
            Map<String, Object> builder,
            PageDesignRequest request,
            AiBusinessObject object,
            boolean hasBusinessData) {
        Map<String, Object> objectRef = new LinkedHashMap<>();
        objectRef.put("objectId", String.valueOf(object.getId()));
        objectRef.put("objectCode", object.getObjectCode());
        objectRef.put("objectName", object.getObjectName());
        objectRef.put("configKey", StringUtils.defaultString(object.getConfigKey()));
        objectRef.put("pageKey", "form".equals(request.pageType()) ? "form" : "list");
        objectRef.put("pageMode", resolvePageMode(request.pageType()));
        objectRef.put("hasBusinessData", hasBusinessData);
        objectRef.put("valid", true);

        Object nodesValue = builder.get("nodes");
        if (nodesValue instanceof List<?> nodes) {
            for (Object value : nodes) {
                Map<String, Object> node = mutableMap(value);
                if (node != null && request.pageId().equals(String.valueOf(node.get("id")))) {
                    node.put("pageType", "object");
                    node.put("pageTemplate", request.pageType());
                    node.put("pageShape", request.pageType());
                    node.put("objectRef", new LinkedHashMap<>(objectRef));
                }
            }
        }
        Map<String, Object> pages = mutableMap(builder.get("pages"));
        Map<String, Object> page = pages == null ? null : mutableMap(pages.get(request.pageId()));
        patchObjectRefs(page, request.formAssetId(), objectRef);
    }

    private void patchObjectRefs(Object value, String formAssetId, Map<String, Object> objectRef) {
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> map = mutableMap(source);
            Map<String, Object> props = mutableMap(map.get("props"));
            if (props != null && StringUtils.equals(formAssetId, String.valueOf(props.get("formAssetId")))) {
                props.put("objectRef", new LinkedHashMap<>(objectRef));
            }
            map.values().forEach(child -> patchObjectRefs(child, formAssetId, objectRef));
        } else if (value instanceof List<?> list) {
            list.forEach(child -> patchObjectRefs(child, formAssetId, objectRef));
        }
    }

    private void lockBuilderFormFields(Map<String, Object> builder, String formAssetId) {
        Object assetsValue = builder.get("formAssets");
        if (!(assetsValue instanceof List<?> assets)) {
            return;
        }
        for (Object value : assets) {
            Map<String, Object> asset = mutableMap(value);
            if (asset != null && StringUtils.equals(formAssetId, String.valueOf(asset.get("id")))) {
                lockFieldBindings(asset.get("formDesignerSchema"));
            }
        }
    }

    private void synchronizeBuilderFormAsset(
            Map<String, Object> builder,
            String formAssetId,
            FormDesignerSchemaDTO formSchema) {
        Object assetsValue = builder.get("formAssets");
        if (!(assetsValue instanceof List<?> assets)) {
            return;
        }
        for (Object value : assets) {
            Map<String, Object> asset = mutableMap(value);
            if (asset != null && StringUtils.equals(formAssetId, String.valueOf(asset.get("id")))) {
                asset.put("formDesignerSchema", objectMapper.convertValue(
                        formSchema, new TypeReference<Map<String, Object>>() {
                        }));
                return;
            }
        }
    }

    private void lockPersistedFieldBindings(FormDesignerSchemaDTO schema) {
        if (schema != null) {
            lockFieldBindings(schema.getComponents());
            lockFieldBindings(schema.getForms());
        }
    }

    private void lockFieldBindings(Object value) {
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> map = mutableMap(source);
            Map<String, Object> fieldBinding = mutableMap(map.get("fieldBinding"));
            if (fieldBinding != null && StringUtils.isNotBlank(String.valueOf(fieldBinding.get("fieldCode")))) {
                fieldBinding.put("locked", true);
            }
            map.values().forEach(this::lockFieldBindings);
        } else if (value instanceof List<?> list) {
            list.forEach(this::lockFieldBindings);
        }
    }

    private String buildObjectOptions(
            Long applicationId, PageDesignRequest request, GenDatasource datasource) {
        JSONObject options = pageMarker(applicationId, request);
        boolean dbImport = CREATE_MODE_DB_IMPORT.equals(request.createMode());
        options.put("createMode", dbImport ? CREATE_MODE_DB_IMPORT : CREATE_MODE_BLANK);
        options.put("runtimeDatasourceId", datasource.getDatasourceId());
        JSONObject runtimeDatasource = new JSONObject();
        runtimeDatasource.put("datasourceId", datasource.getDatasourceId());
        runtimeDatasource.put("datasourceCode", datasource.getDatasourceCode());
        runtimeDatasource.put("datasourceName", datasource.getDatasourceName());
        runtimeDatasource.put("dbType", datasource.getDbType());
        runtimeDatasource.put("usageScope", datasource.getUsageScope());
        runtimeDatasource.put("allowWrite", Integer.valueOf(1).equals(datasource.getAllowRuntimeWrite()));
        runtimeDatasource.put("allowDdl", Integer.valueOf(1).equals(datasource.getAllowRuntimeDdl()));
        runtimeDatasource.put("readonly", Integer.valueOf(1).equals(datasource.getReadonly()));
        runtimeDatasource.put("riskLevel", datasource.getRiskLevel());
        runtimeDatasource.put("tableMode", dbImport ? "EXISTING" : "CREATE");
        options.put("runtimeDatasource", runtimeDatasource);
        if (dbImport) {
            JSONObject sourceTable = new JSONObject();
            sourceTable.put("datasourceId", request.importDatasourceId() == null
                    ? datasource.getDatasourceId()
                    : request.importDatasourceId());
            sourceTable.put("tableName", request.importTableName());
            options.put("sourceTable", sourceTable);
        }
        return options.toJSONString();
    }

    private JSONObject pageMarker(Long applicationId, PageDesignRequest request) {
        JSONObject marker = new JSONObject();
        marker.put("managedBy", MANAGED_BY_PAGE_FORM);
        marker.put("sourceApplicationId", applicationId);
        marker.put("sourcePageId", request.pageId());
        marker.put("sourcePageType", request.pageType());
        marker.put("sourceFormAssetId", request.formAssetId());
        marker.put("hiddenFromPrimaryFlow", false);
        return marker;
    }

    private boolean pageMarkerMatches(
            String options, Long applicationId, PageDesignRequest request) {
        JSONObject marker = readOptions(options);
        if (!MANAGED_BY_PAGE_FORM.equals(marker.getString("managedBy"))
                || !StringUtils.equals(String.valueOf(applicationId), marker.getString("sourceApplicationId"))) {
            return false;
        }
        // 页面 ID 和表单资产 ID 必须同时命中。中文页面名会退化成相同的 page_page / form，
        // 删除页面后对象仍留在应用里；只比其中一项会把新建页面绑回旧数据表。
        return StringUtils.equals(request.pageId(), marker.getString("sourcePageId"))
                && StringUtils.equals(request.formAssetId(), marker.getString("sourceFormAssetId"));
    }

    private GenDatasource resolveRuntimeDatasource(Long requestedId, boolean requireDdl) {
        List<GenDatasource> enabled = safeList(datasourceService.selectEnabledDatasources(LOWCODE_RUNTIME));
        if (requestedId != null) {
            GenDatasource selected = enabled.stream()
                    .filter(item -> requestedId.equals(item.getDatasourceId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("请选择有效的低代码运行数据源"));
            if (!isRuntimeWritable(selected)) {
                throw new BusinessException("当前数据源不允许写入业务数据");
            }
            if (requireDdl && !isWritableRuntimeDatasource(selected)) {
                throw new BusinessException("当前数据存储未允许自动建表，请在高级数据设置中开启自动建表");
            }
            return selected;
        }
        return enabled.stream()
                .filter(requireDdl ? this::isWritableRuntimeDatasource : this::isRuntimeWritable)
                .min(Comparator
                        .comparingInt((GenDatasource datasource) ->
                                Integer.valueOf(1).equals(datasource.getIsDefault()) ? 0 : 1)
                        .thenComparing(datasource -> datasource.getSort() == null
                                ? Integer.MAX_VALUE : datasource.getSort())
                        .thenComparing(datasource -> datasource.getDatasourceId() == null
                                ? Long.MAX_VALUE : datasource.getDatasourceId()))
                .orElseThrow(() -> new BusinessException(requireDdl
                        ? "当前数据存储未允许自动建表，请在高级数据设置中开启自动建表"
                        : "请选择有效的低代码运行数据源"));
    }

    private boolean isWritableRuntimeDatasource(GenDatasource datasource) {
        return isRuntimeWritable(datasource)
                && Integer.valueOf(1).equals(datasource.getAllowRuntimeDdl());
    }

    private boolean isRuntimeWritable(GenDatasource datasource) {
        return datasource != null
                && datasource.getDatasourceId() != null
                && Integer.valueOf(1).equals(datasource.getAllowRuntimeWrite())
                && !Integer.valueOf(1).equals(datasource.getReadonly());
    }

    private String normalizeCreateMode(String createMode) {
        if (CREATE_MODE_DB_IMPORT.equalsIgnoreCase(StringUtils.trimToEmpty(createMode))) {
            return CREATE_MODE_DB_IMPORT;
        }
        return CREATE_MODE_BLANK;
    }

    private void assertBuilderContainsPage(Map<String, Object> builder, String pageId) {
        Object nodesValue = builder.get("nodes");
        if (!(nodesValue instanceof List<?> nodes)
                || nodes.stream().map(this::mutableMap).filter(java.util.Objects::nonNull)
                .noneMatch(node -> pageId.equals(String.valueOf(node.get("id"))))) {
            throw new BusinessException("页面草稿中未找到当前页面");
        }
        Map<String, Object> pages = mutableMap(builder.get("pages"));
        if (pages == null || mutableMap(pages.get(pageId)) == null) {
            throw new BusinessException("页面草稿中缺少当前页面布局");
        }
    }

    private void assertBuilderContainsFormAsset(
            Map<String, Object> builder, String pageId, String formAssetId) {
        Object assetsValue = builder.get("formAssets");
        boolean assetExists = assetsValue instanceof List<?> assets
                && assets.stream()
                .map(this::mutableMap)
                .filter(java.util.Objects::nonNull)
                .anyMatch(asset -> formAssetId.equals(String.valueOf(asset.get("id"))));
        if (!assetExists) {
            throw new BusinessException("页面草稿中未找到当前表单资产");
        }
        Map<String, Object> pages = mutableMap(builder.get("pages"));
        Map<String, Object> page = pages == null ? null : mutableMap(pages.get(pageId));
        if (!containsFormAssetReference(page, formAssetId)) {
            throw new BusinessException("当前页面未绑定提交的表单资产");
        }
    }

    private boolean containsFormAssetReference(Object value, String formAssetId) {
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> map = mutableMap(source);
            Map<String, Object> props = mutableMap(map.get("props"));
            if (props != null && formAssetId.equals(String.valueOf(props.get("formAssetId")))) {
                return true;
            }
            return map.values().stream().anyMatch(child -> containsFormAssetReference(child, formAssetId));
        }
        if (value instanceof List<?> list) {
            return list.stream().anyMatch(child -> containsFormAssetReference(child, formAssetId));
        }
        return false;
    }

    private BusinessApplicationPageDesignVO customResult(
            PageDesignRequest request, Map<String, Object> builder) {
        BusinessApplicationPageDesignVO result = baseResult(request, builder);
        result.setObjectCreated(false);
        result.setHasBusinessData(false);
        return result;
    }

    private BusinessApplicationPageDesignVO baseResult(
            PageDesignRequest request, Map<String, Object> builder) {
        BusinessApplicationPageDesignVO result = new BusinessApplicationPageDesignVO();
        result.setPageId(request.pageId());
        result.setPageType(request.pageType());
        result.setFormAssetId(request.formAssetId());
        result.setBuilder(builder);
        return result;
    }

    private String resolveDisplayField(List<BusinessFieldDTO> fields) {
        return fields.stream()
                .filter(field -> field != null && StringUtils.isNotBlank(field.getFieldCode()))
                .findFirst()
                .map(BusinessFieldDTO::getFieldCode)
                .orElse(null);
    }

    private String resolvePageMode(String pageType) {
        return switch (pageType) {
            case "form" -> "form";
            case "list" -> "list";
            case "tree-table" -> "tree-table";
            case "tree-list" -> "tree-list";
            default -> "crud";
        };
    }

    private String requiredText(String value, String message, int maxLength) {
        String text = StringUtils.trimToNull(value);
        if (text == null || text.length() > maxLength) {
            throw new BusinessException(message);
        }
        return text;
    }

    private JSONObject readOptions(String options) {
        try {
            JSONObject result = JSON.parseObject(options);
            return result == null ? new JSONObject() : result;
        } catch (Exception error) {
            throw new BusinessException("应用或对象扩展配置不是合法 JSON 对象");
        }
    }

    private Map<String, Object> cloneBuilder(Map<String, Object> builder) {
        return objectMapper.convertValue(builder, new TypeReference<>() {
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mutableMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : null;
    }

    private List<BusinessApplicationObjectVO> safeAssociations(List<BusinessApplicationObjectVO> values) {
        return values == null ? List.of() : values;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record PageDesignRequest(
            String pageId,
            String pageType,
            String formAssetId,
            Long objectId,
            String objectCode,
            String objectName,
            List<BusinessFieldDTO> fields,
            FormDesignerSchemaDTO formDesignerSchema,
            Map<String, Object> builder,
            Long runtimeDatasourceId,
            String createMode,
            Long importDatasourceId,
            String importTableName) {
    }

    private record MetadataResult(Long objectId, BusinessApplicationPageDesignVO result, boolean shouldSyncDatabase) {
    }
}

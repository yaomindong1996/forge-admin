package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectRelation;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeModel;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectRelationMapper;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * 把持久化对象关系投影为运行时模型关系与页面 modelRefs。
 *
 * <p>这是关系域的投影策略：只负责读关系上下文并重建运行时视图，不负责关系写入。</p>
 */
final class BusinessObjectRelationProjector {

    private static final String FORM_DESIGNER_SCHEMA_OPTION_KEY = "formDesignerSchema";

    private final ObjectMapper objectMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessObjectRelationMapper relationMapper;
    private final LongSupplier tenantIdSupplier;
    private final Function<Long, BusinessObjectDesignerService.DesignerContext> contextLoader;

    BusinessObjectRelationProjector(ObjectMapper objectMapper,
                                    BusinessObjectMapper businessObjectMapper,
                                    BusinessObjectRelationMapper relationMapper,
                                    LongSupplier tenantIdSupplier,
                                    Function<Long, BusinessObjectDesignerService.DesignerContext> contextLoader) {
        this.objectMapper = objectMapper;
        this.businessObjectMapper = businessObjectMapper;
        this.relationMapper = relationMapper;
        this.tenantIdSupplier = tenantIdSupplier;
        this.contextLoader = contextLoader;
    }

    void applyRelationsToModel(BusinessObjectDesignerService.DesignerContext context) {
        if (context == null || context.getObject() == null || context.getModelSchema() == null) {
            return;
        }
        List<AiBusinessObjectRelation> relations = relationMapper.selectRuntimeRelationsBySource(
                tenantIdSupplier.getAsLong(), context.getObject().getSuiteCode(), context.getObject().getObjectCode());
        List<LowcodeRelationSchema> relationSchemas = relations.stream()
                .map(this::toLowcodeRelation)
                .toList();
        context.getModelSchema().setRelations(relationSchemas);
        syncInlineEditRelationsToPageSchema(context, relations);
    }

    private void syncInlineEditRelationsToPageSchema(BusinessObjectDesignerService.DesignerContext context,
                                                     List<AiBusinessObjectRelation> relations) {
        LowcodePageSchema pageSchema = context.getPageSchema() == null ? new LowcodePageSchema() : context.getPageSchema();
        Map<String, LowcodePageModelRef> existingRefs = indexPageModelRefs(pageSchema);
        LowcodePageModelRef primaryRef = toPageModelRef(context.getObject(), context.getModel(), context.getModelSchema(), true);
        List<LowcodePageModelRef> refs = new ArrayList<>();
        Set<String> addedModelCodes = new LinkedHashSet<>();
        refs.add(primaryRef);
        addedModelCodes.add(primaryRef.getModelCode());

        List<String> childFieldRefs = new ArrayList<>();
        boolean hasEmbeddedRelations = false;
        for (AiBusinessObjectRelation relation : relations) {
            AiBusinessObject target = businessObjectMapper.selectByObjectCode(
                    tenantIdSupplier.getAsLong(), relation.getSuiteCode(), relation.getTargetObjectCode());
            if (target == null) {
                continue;
            }
            BusinessObjectDesignerService.DesignerContext targetContext = contextLoader.apply(target.getId());
            LowcodePageModelRef targetRef = toPageModelRef(target, targetContext.getModel(),
                    targetContext.getModelSchema(), false);
            mergeExistingPageModelRef(targetRef, existingRefs.get(targetRef.getModelCode()));
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

        pageSchema.setModelRefs(sortModelRefsByFormSubTables(refs, context.getObject()));
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

    /**
     * 关系表按 sort_order/id（约等于创建顺序）返回，发布态子表顺序又取自 modelRefs；
     * 这里按表单设计器 subTable 组件的画布顺序重排，否则画布拖动后运行页/审批顺序会反。
     */
    private List<LowcodePageModelRef> sortModelRefsByFormSubTables(List<LowcodePageModelRef> refs,
                                                                   AiBusinessObject object) {
        List<String> order = collectFormSubTableOrder(object);
        if (order.isEmpty() || refs.size() < 3) {
            return refs;
        }
        List<LowcodePageModelRef> children = new ArrayList<>(refs.subList(1, refs.size()));
        children.sort(Comparator.comparingInt(ref -> formSubTableRank(ref, order)));
        List<LowcodePageModelRef> result = new ArrayList<>(refs.size());
        result.add(refs.get(0));
        result.addAll(children);
        return result;
    }

    private int formSubTableRank(LowcodePageModelRef ref, List<String> order) {
        String relationKey = ref.getProps() == null ? null : text(ref.getProps().get("relationKey"));
        int byRelation = StringUtils.isBlank(relationKey) ? -1 : order.indexOf(relationKey);
        if (byRelation >= 0) {
            return byRelation;
        }
        int byModel = order.indexOf(ref.getModelCode());
        return byModel >= 0 ? byModel : Integer.MAX_VALUE;
    }

    private List<String> collectFormSubTableOrder(AiBusinessObject object) {
        Map<String, Object> designerOptions = readMap(object == null ? null : object.getDesignerOptions());
        Object rawSchema = designerOptions.get(FORM_DESIGNER_SCHEMA_OPTION_KEY);
        Map<String, Object> formSchema = rawSchema instanceof String json ? readMap(json) : mapValue(rawSchema);
        List<String> order = new ArrayList<>();
        collectFormSubTableKeys(listOfMap(formSchema.get("components")), order);
        return order;
    }

    private void collectFormSubTableKeys(List<Map<String, Object>> components, List<String> order) {
        for (Map<String, Object> component : components) {
            String componentKey = StringUtils.firstNonBlank(text(component.get("componentKey")), text(component.get("type")));
            if ("subTable".equalsIgnoreCase(componentKey) || "childTable".equalsIgnoreCase(componentKey)) {
                Map<String, Object> props = mapValue(component.get("props"));
                for (String key : new String[]{text(props.get("relationKey")), text(props.get("modelCode"))}) {
                    if (StringUtils.isNotBlank(key)) {
                        order.add(key.trim());
                    }
                }
            }
            collectFormSubTableKeys(listOfMap(component.get("children")), order);
        }
    }

    private Map<String, LowcodePageModelRef> indexPageModelRefs(LowcodePageSchema pageSchema) {
        Map<String, LowcodePageModelRef> result = new LinkedHashMap<>();
        if (pageSchema == null || pageSchema.getModelRefs() == null) {
            return result;
        }
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref != null && StringUtils.isNotBlank(ref.getModelCode())) {
                result.put(ref.getModelCode(), ref);
            }
        }
        return result;
    }

    void mergeExistingPageModelRef(LowcodePageModelRef targetRef, LowcodePageModelRef existingRef) {
        if (targetRef == null || existingRef == null) {
            return;
        }
        Map<String, Object> mergedProps = new LinkedHashMap<>();
        if (targetRef.getProps() != null) {
            mergedProps.putAll(targetRef.getProps());
        }
        if (existingRef.getProps() != null) {
            mergedProps.putAll(existingRef.getProps());
        }
        targetRef.setProps(mergedProps);
        if (StringUtils.isNotBlank(existingRef.getModelName())) {
            targetRef.setModelName(existingRef.getModelName());
        }
        if (StringUtils.isNotBlank(text(existingRef.getProps().get("tabTitle")))) {
            targetRef.getProps().put("tabTitle", text(existingRef.getProps().get("tabTitle")));
        }
        if (StringUtils.isNotBlank(text(existingRef.getProps().get("relationName")))) {
            targetRef.getProps().put("relationName", text(existingRef.getProps().get("relationName")));
        }
        if (StringUtils.isBlank(text(targetRef.getProps().get("relationKey")))
                && StringUtils.isNotBlank(text(existingRef.getProps().get("relationKey")))) {
            targetRef.getProps().put("relationKey", text(existingRef.getProps().get("relationKey")));
        }
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
        if (relation == null || EnableStatus.DISABLED.matches(relation.getStatus())) {
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
        if (relation == null || EnableStatus.DISABLED.matches(relation.getStatus())) {
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
        props.put("relationKey", StringUtils.defaultIfBlank(
                text(config.get("relationKey")), defaultRelationKey(relation.getTargetObjectCode())));
        props.put("showInDetail", readBoolean(config.get("showInDetail"), true));
        props.put("inlineCreateEnabled", readBoolean(config.get("inlineCreateEnabled"), true));
        props.put("inlineEditEnabled", readBoolean(config.get("inlineEditEnabled"), true));
        props.put("saveMode", normalizeChildSaveMode(config.get("saveMode")));
        List<String> childFieldCodes = readStringList(config.get("childFieldCodes"));
        if (!childFieldCodes.isEmpty()) {
            props.put("childFieldCodes", childFieldCodes);
        }
        boolean allowSelectExisting = readBoolean(config.get("allowSelectExisting"), false);
        props.put("allowSelectExisting", allowSelectExisting);
        if (StringUtils.isNotBlank(text(config.get("displayMode")))) {
            props.put("displayMode", text(config.get("displayMode")));
        }
        if (StringUtils.isNotBlank(text(config.get("defaultFilter")))) {
            props.put("defaultFilter", text(config.get("defaultFilter")));
        }
        Object recordSelector = config.get("recordSelector");
        if (recordSelector instanceof Map<?, ?> selector && !selector.isEmpty()) {
            props.put("recordSelector", selector);
            // 面板读取扁平字段，这里反向拆解保证重新打开设计器时能回显
            props.put("selectorMultiple", readBoolean(selector.get("multiple"), true));
            List<String> displayFields = readStringList(selector.get("displayFields"));
            if (!displayFields.isEmpty()) {
                props.put("selectorDisplayFields", displayFields);
            }
            if (selector.get("filterFields") instanceof List<?> filters && !filters.isEmpty()) {
                props.put("selectorFilterFields", filters);
            }
        } else if (allowSelectExisting && StringUtils.isNotBlank(relation.getTargetObjectCode())) {
            Map<String, Object> defaultSelector = new LinkedHashMap<>();
            defaultSelector.put("objectCode", relation.getTargetObjectCode());
            defaultSelector.put("businessObjectCode", relation.getTargetObjectCode());
            defaultSelector.put("buttonText", "选择记录");
            props.put("recordSelector", defaultSelector);
        }
        Object rowActions = config.get("rowActions");
        if (rowActions instanceof List<?> actions && !actions.isEmpty()) {
            props.put("rowActions", actions);
        }
        putIfNotBlank(props, "displayField", resolveRelationDisplayField(relation));
        return props;
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    private String normalizeChildSaveMode(Object value) {
        return "merge".equalsIgnoreCase(text(value)) ? "merge" : "replace";
    }

    private String defaultRelationKey(String value) {
        return StringUtils.defaultString(value)
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "")
                .toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> toLookupRelationProps(AiBusinessObjectRelation relation) {
        Map<String, Object> config = readMap(relation.getRelationConfig());
        Map<String, Object> props = new LinkedHashMap<>();
        AiBusinessObject target = businessObjectMapper.selectByObjectCode(
                tenantIdSupplier.getAsLong(), relation.getSuiteCode(), relation.getTargetObjectCode());
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
        Map<String, Map<String, Object>> designerFieldProps = indexFormDesignerFieldProps(object);
        ref.setFields(toPageModelFields(modelCode, schema, primary, designerFieldProps));
        return ref;
    }

    private List<Map<String, Object>> toPageModelFields(String modelCode,
                                                        LowcodeModelSchema schema,
                                                        boolean primary,
                                                        Map<String, Map<String, Object>> designerFieldProps) {
        if (schema == null || schema.getFields() == null) {
            return new ArrayList<>();
        }
        return schema.getFields().stream()
                .filter(field -> field != null)
                .map(field -> toPageModelField(modelCode, field, primary, designerFieldProps))
                .toList();
    }

    private Map<String, Object> toPageModelField(String modelCode,
                                                 LowcodeFieldSchema field,
                                                 boolean primary,
                                                 Map<String, Map<String, Object>> designerFieldProps) {
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
        Map<String, Object> designerProps = designerFieldProps == null
                ? Map.of()
                : designerFieldProps.getOrDefault(fieldName, Map.of());
        String designerComponentType = text(designerProps.get("componentType"));
        item.put("componentType", StringUtils.defaultIfBlank(designerComponentType, field.getComponentType()));
        item.put("queryType", field.getQueryType());
        item.put("dictType", StringUtils.defaultIfBlank(text(designerProps.get("dictType")), field.getDictType()));
        item.put("sensitiveType", field.getSensitiveType());
        item.put("encryptAlgorithm", field.getEncryptAlgorithm());
        item.put("sortable", field.getSortable());
        item.put("primaryKey", field.getPrimaryKey());
        item.put("systemField", field.getSystemField());
        // 字段注册表只读 ∨ 表单设计器 visibility.readonly，主子表子列与独立填表一致
        item.put("readonly", Boolean.TRUE.equals(field.getReadonly())
                || Boolean.TRUE.equals(designerProps.get("readonly")));
        item.put("fieldStatus", field.getFieldStatus());
        item.put("autoIncrement", field.getAutoIncrement());
        item.put("width", field.getWidth());
        item.put("remark", field.getRemark());
        // 子表运行态控件依赖这些配置（选项源/引用对象/公式），快照缺失会让下拉、引用退化为输入框
        item.put("referenceObjectCode", StringUtils.defaultIfBlank(
                text(designerProps.get("referenceObjectCode")), field.getReferenceObjectCode()));
        item.put("referenceDisplayField", StringUtils.defaultIfBlank(
                text(designerProps.get("referenceDisplayField")), field.getReferenceDisplayField()));
        Map<String, Object> basicProps = field.getBasicProps() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(field.getBasicProps());
        // 明细页表单设计器上的 optionSource 是下拉事实源；子表引用时注册表 basicProps 可能未回写，这里补齐
        mergeMissingDesignerProps(basicProps, designerProps);
        item.put("basicProps", basicProps.isEmpty() ? null : basicProps);
        item.put("advancedProps", field.getAdvancedProps() == null ? null : new LinkedHashMap<>(field.getAdvancedProps()));
        item.put("formulaConfig", field.getFormulaConfig() == null ? null : new LinkedHashMap<>(field.getFormulaConfig()));
        return item;
    }

    /**
     * 从对象 designerOptions.formDesignerSchema 按 fieldCode 索引控件 props，
     * 供子表 masterDetailConfig 继承明细页下拉/引用配置。
     */
    private Map<String, Map<String, Object>> indexFormDesignerFieldProps(AiBusinessObject object) {
        Map<String, Object> designerOptions = readMap(object == null ? null : object.getDesignerOptions());
        Object rawSchema = designerOptions.get(FORM_DESIGNER_SCHEMA_OPTION_KEY);
        Map<String, Object> formSchema = rawSchema instanceof String json ? readMap(json) : mapValue(rawSchema);
        Map<String, Map<String, Object>> byCode = new LinkedHashMap<>();
        collectFormDesignerFieldProps(listOfMap(formSchema.get("components")), byCode);
        return byCode;
    }

    private void collectFormDesignerFieldProps(List<Map<String, Object>> components,
                                               Map<String, Map<String, Object>> byCode) {
        for (Map<String, Object> component : components) {
            if (component == null) {
                continue;
            }
            Map<String, Object> binding = mapValue(component.get("fieldBinding"));
            String fieldCode = StringUtils.firstNonBlank(text(binding.get("fieldCode")), text(component.get("field")));
            String componentKey = StringUtils.firstNonBlank(text(component.get("componentKey")), text(component.get("type")));
            if (StringUtils.isNotBlank(fieldCode) && !"virtual".equalsIgnoreCase(text(binding.get("mode")))) {
                Map<String, Object> props = new LinkedHashMap<>(mapValue(component.get("props")));
                props.remove("__fc");
                props.remove("__fcType");
                props.remove("fieldBinding");
                if (StringUtils.isNotBlank(componentKey)) {
                    props.putIfAbsent("componentType", componentKey);
                }
                if (StringUtils.isNotBlank(text(props.get("dictType")))) {
                    props.putIfAbsent("dictType", text(props.get("dictType")));
                }
                // 表单设计器 visibility.readonly 可能尚未回写字段注册表，投影时一并带上
                Map<String, Object> visibility = mapValue(component.get("visibility"));
                if (Boolean.TRUE.equals(visibility.get("readonly"))) {
                    props.put("readonly", true);
                }
                byCode.putIfAbsent(fieldCode, props);
            }
            collectFormDesignerFieldProps(listOfMap(component.get("children")), byCode);
        }
    }

    private void mergeMissingDesignerProps(Map<String, Object> basicProps, Map<String, Object> designerProps) {
        if (basicProps == null || designerProps == null || designerProps.isEmpty()) {
            return;
        }
        for (String key : List.of(
                "optionSource", "options", "dictType", "labelValueField", "fieldMappings", "mappings",
                "cascade", "cascadeConfig", "referenceObjectCode", "referenceDisplayField", "referenceValueField",
                "recordSelector", "multiple", "clearable", "filterable", "placeholder", "defaultValue",
                "checkedValue", "uncheckedValue", "runtimeRules")) {
            if (basicProps.get(key) == null && designerProps.get(key) != null) {
                basicProps.put(key, designerProps.get(key));
            }
        }
        if (basicProps.get("optionSource") instanceof Map<?, ?>) {
            basicProps.remove("options");
        }
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
                tenantIdSupplier.getAsLong(), relation.getSuiteCode(), relation.getTargetObjectCode());
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
        AiBusinessObject object = businessObjectMapper.selectByObjectCode(tenantIdSupplier.getAsLong(), suiteCode, objectCode);
        if (object == null) {
            return normalizeConfigKey(objectCode);
        }
        return resolveModelCode(object);
    }


    private String resolveModelCode(AiBusinessObject object) {
        return normalizeConfigKey(StringUtils.firstNonBlank(
                object.getModelCode(), object.getObjectCode(), "business_object"));
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
        return list.stream().filter(Map.class::isInstance).map(this::mapValue).toList();
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

    private List<String> readStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(item -> StringUtils.trimToNull(text(item)))
                .filter(item -> item != null)
                .toList();
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}


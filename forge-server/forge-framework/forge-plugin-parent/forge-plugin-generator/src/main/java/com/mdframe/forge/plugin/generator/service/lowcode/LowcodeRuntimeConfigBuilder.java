package com.mdframe.forge.plugin.generator.service.lowcode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 将低代码业务协议转换为 AiCrudPage 运行时配置。
 */
@Service
@RequiredArgsConstructor
public class LowcodeRuntimeConfigBuilder {

    private static final String MASTER_DETAIL_LAYOUT = "master-detail-crud";
    private static final Set<String> SYSTEM_FIELD_NAMES = Set.of(
            "id", "tenantId", "createBy", "createTime", "createDept", "updateBy", "updateTime", "delFlag"
    );
    private static final Set<String> SYSTEM_COLUMN_NAMES = Set.of(
            "id", "tenant_id", "create_by", "create_time", "create_dept", "update_by", "update_time", "del_flag"
    );

    private final ObjectMapper objectMapper;
    private final LowcodeSchemaValidator schemaValidator;
    private final LowcodePolicyService policyService;

    private record RelationLookupMeta(String modelCode,
                                      String modelName,
                                      String configKey,
                                      String sourceField,
                                      String targetField,
                                      String displayField) {
    }

    public LowcodeRuntimeConfig buildRuntimeConfig(String configKey,
                                                   LowcodeModelSchema modelSchema,
                                                   LowcodePageSchema pageSchema) {
        if (StringUtils.isBlank(configKey)) {
            throw new BusinessException("configKey不能为空");
        }
        policyService.normalizeModelSchema(modelSchema);
        schemaValidator.validatePage(pageSchema, modelSchema);

        LowcodeRuntimeConfig runtimeConfig = new LowcodeRuntimeConfig();
        runtimeConfig.setConfigKey(configKey);
        runtimeConfig.setTableName(modelSchema.getTableName());
        runtimeConfig.setTableComment(modelSchema.getBusinessName());
        runtimeConfig.setLayoutType(StringUtils.defaultIfBlank(pageSchema.getLayoutType(), "simple-crud"));

        try {
            runtimeConfig.setSearchSchema(objectMapper.writeValueAsString(buildSearchSchema(configKey, modelSchema, pageSchema)));
            runtimeConfig.setColumnsSchema(objectMapper.writeValueAsString(buildColumnsSchema(modelSchema, pageSchema)));
            runtimeConfig.setEditSchema(objectMapper.writeValueAsString(buildEditSchema(configKey, modelSchema, pageSchema)));
            runtimeConfig.setApiConfig(objectMapper.writeValueAsString(buildApiConfig(configKey, modelSchema, pageSchema)));
            runtimeConfig.setOptions(objectMapper.writeValueAsString(buildOptions(modelSchema, pageSchema)));
            runtimeConfig.setDictConfig(objectMapper.writeValueAsString(buildDictConfig(modelSchema)));
            runtimeConfig.setDesensitizeConfig(objectMapper.writeValueAsString(buildDesensitizeConfig(modelSchema)));
            runtimeConfig.setEncryptConfig(objectMapper.writeValueAsString(buildEncryptConfig(modelSchema)));
            runtimeConfig.setTransConfig(objectMapper.writeValueAsString(buildTransConfig(modelSchema, pageSchema)));
            return runtimeConfig;
        } catch (Exception e) {
            throw new BusinessException("低代码运行时配置生成失败: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> buildSearchSchema(String configKey, LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        List<Map<String, Object>> fields = resolveFields(modelSchema, pageSchema, "search", field -> Boolean.TRUE.equals(field.getSearchable()))
                .stream()
                .map(field -> buildSearchField(field, resolveRuntimeFieldSetting(pageSchema, "search", field.getField()), modelSchema, pageSchema))
                .collect(Collectors.toCollection(ArrayList::new));
        appendTreeRuntimeField(fields, modelSchema, pageSchema, "search");
        decorateTreeRuntimeFields(fields, configKey, modelSchema, pageSchema);
        return fields;
    }

    private List<Map<String, Object>> buildColumnsSchema(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        List<Map<String, Object>> columns = resolveFields(modelSchema, pageSchema, "table",
                field -> field.getListVisible() == null || Boolean.TRUE.equals(field.getListVisible()))
                .stream()
                .map(field -> buildTableColumn(field, resolveRuntimeFieldSetting(pageSchema, "table", field.getField()),
                        modelSchema, pageSchema))
                .collect(Collectors.toCollection(ArrayList::new));

        Map<String, Object> actions = new LinkedHashMap<>();
        actions.put("key", "actions");
        actions.put("title", "操作");
        actions.put("dataIndex", "actions");
        List<Map<String, Object>> rowActions = buildRowActions(pageSchema, isEmbeddedTreeTableRuntime(modelSchema, pageSchema));
        actions.put("width", Math.max(180, rowActions.size() * 58));
        actions.put("fixed", "right");
        actions.put("actions", rowActions);
        actions.put("maxActionButtons", 3);
        columns.add(actions);
        return columns;
    }

    private List<Map<String, Object>> buildEditSchema(String configKey, LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Set<String> childFieldRefs = isMasterDetailRuntime(pageSchema) ? buildChildFieldRefs(pageSchema) : Set.of();
        List<LowcodeFieldSchema> orderedFields = sortByCanvasOrder(
                resolveFields(modelSchema, pageSchema, "edit",
                        field -> field.getFormVisible() == null || Boolean.TRUE.equals(field.getFormVisible())),
                pageSchema,
                "edit"
        );
        List<Map<String, Object>> fields = orderedFields
                .stream()
                .filter(field -> !childFieldRefs.contains(field.getField()))
                .map(field -> buildEditField(field, resolveEditFieldSetting(pageSchema, field.getField()),
                        modelSchema, pageSchema))
                .collect(Collectors.toCollection(ArrayList::new));
        appendTreeRuntimeField(fields, modelSchema, pageSchema, "edit");
        decorateTreeRuntimeFields(fields, configKey, modelSchema, pageSchema);
        return fields;
    }

    private Map<String, String> buildApiConfig(String configKey, LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Map<String, String> apiConfig = new LinkedHashMap<>();
        apiConfig.put("list", "get@/ai/crud/" + configKey + "/page");
        if (isTreeRuntime(modelSchema, pageSchema)) {
            apiConfig.put("tree", "get@/ai/crud/" + configKey + "/tree");
        }
        apiConfig.put("detail", "get@/ai/crud/" + configKey + "/:id");
        apiConfig.put("create", "post@/ai/crud/" + configKey);
        apiConfig.put("update", "put@/ai/crud/" + configKey);
        apiConfig.put("delete", "delete@/ai/crud/" + configKey + "/:id");
        apiConfig.put("import", "post@/ai/crud/" + configKey + "/import");
        apiConfig.put("export", "post@/ai/crud/" + configKey + "/export");
        apiConfig.put("importTemplate", "get@/ai/crud/" + configKey + "/import-template");
        return apiConfig;
    }

    private boolean isEmbeddedTreeTableRuntime(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        return isTreeRuntime(modelSchema, pageSchema) && !"tree-crud".equals(StringUtils.defaultIfBlank(
                pageSchema == null ? null : pageSchema.getLayoutType(), "simple-crud"));
    }

    private Map<String, Object> buildOptions(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Map<String, Object> options = new LinkedHashMap<>();
        boolean masterDetailRuntime = isMasterDetailRuntime(pageSchema);
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        Map<String, Object> editProps = editZone == null || editZone.getProps() == null ? Map.of() : editZone.getProps();
        Map<String, Object> crudBlockProps = resolveGridBlockProps(pageSchema, List.of("AiCrudPage"));
        options.put("modalType", resolveModalType(firstPresent(editProps.get("modalType"), crudBlockProps.get("modalType"))));
        int editGridCols = resolveEditGridCols(pageSchema);
        options.put("modalWidth", StringUtils.defaultIfBlank(text(editProps.get("modalWidth")),
                StringUtils.defaultIfBlank(text(crudBlockProps.get("modalWidth")),
                        resolveDefaultModalWidth(masterDetailRuntime, editGridCols))));
        options.put("searchGridCols", integerValue(crudBlockProps.get("searchGridCols")) == null
                ? 4
                : integerValue(crudBlockProps.get("searchGridCols")));
        options.put("editGridCols", editGridCols);
        options.put("editLabelPlacement", StringUtils.defaultIfBlank(text(editProps.get("labelPlacement")),
                StringUtils.defaultIfBlank(text(crudBlockProps.get("editLabelPlacement")), "left")));
        options.put("editLabelAlign", StringUtils.defaultIfBlank(text(editProps.get("labelAlign")),
                StringUtils.defaultIfBlank(text(crudBlockProps.get("editLabelAlign")), "right")));
        options.put("editLabelWidth", editProps.getOrDefault("labelWidth",
                crudBlockProps.getOrDefault("editLabelWidth", "auto")));
        options.put("editSize", normalizeRuntimeFormSize(StringUtils.defaultIfBlank(text(editProps.get("size")),
                text(crudBlockProps.get("editSize")))));
        options.put("editShowFeedback", booleanWithDefault(firstPresent(editProps.get("showFeedback"),
                crudBlockProps.get("editShowFeedback")), true));
        copyOption(editProps, options, "editFormClass");
        copyOption(editProps, options, "editFormStyle");
        options.put("editXGap", intValue(editProps.get("columnGap"), 16));
        options.put("editYGap", intValue(editProps.get("rowGap"), 16));
        Object formLayout = editProps.get("formLayout");
        if (formLayout instanceof List<?> layout && !layout.isEmpty()) {
            options.put("editFormLayout", layout);
        }
        Object formDesignerSchema = editProps.get("formDesignerSchema");
        if (formDesignerSchema != null) {
            options.put("formDesignerSchema", formDesignerSchema);
        }

        LowcodePageZone tableZone = findZone(pageSchema, "table");
        Map<String, Object> tableProps = new LinkedHashMap<>();
        if (tableZone != null && tableZone.getProps() != null) {
            tableProps.putAll(tableZone.getProps());
        }
        tableProps.putAll(resolveGridBlockProps(pageSchema, List.of("data-table", "AiCrudPage", "AiTable")));
        if (!tableProps.isEmpty()) {
            copyOption(tableProps, options, "showImport");
            copyOption(tableProps, options, "showExport");
            copyOption(tableProps, options, "showPagination");
            copyOption(tableProps, options, "hideAdd");
            copyOption(tableProps, options, "hideToolbar");
            copyOption(tableProps, options, "hideSelection");
            copyOption(tableProps, options, "hideBatchDelete");
            copyOption(tableProps, options, "enableCustomQuery");
            copyOption(tableProps, options, "showRenderModeSwitch");
            copyOption(tableProps, options, "renderMode");
            copyOption(tableProps, options, "tableSize");
            copyOption(tableProps, options, "bordered");
            copyOption(tableProps, options, "striped");
            options.put("tableRowGap", intValue(tableProps.get("rowGap"), 8));
        }
        Set<String> toolbarActions = resolveToolbarStandardActions(pageSchema);
        if (!toolbarActions.isEmpty()) {
            options.put("hideAdd", !toolbarActions.contains("add"));
            options.put("showImport", toolbarActions.contains("import"));
            options.put("showExport", toolbarActions.contains("export"));
            options.put("hideBatchDelete", !toolbarActions.contains("batch-delete"));
            options.put("enableCustomQuery", toolbarActions.contains("custom-query"));
        }
        options.put("toolbarActions", resolveCustomActions(pageSchema, "toolbar"));
        options.put("rowActions", resolveCustomActions(pageSchema, "row"));
        options.put("defaultSort", buildDefaultSort(modelSchema, pageSchema));
        options.put("joinConfig", buildJoinConfig(modelSchema, pageSchema));
        if (masterDetailRuntime) {
            options.put("masterDetailConfig", buildMasterDetailConfig(modelSchema, pageSchema));
        }
        if (isTreeRuntime(modelSchema, pageSchema)) {
            options.put("treeConfig", buildTreeConfig(modelSchema, pageSchema, extractTreeConfigOverrides(pageSchema)));
        }
        return options;
    }

    private String resolveDefaultModalWidth(boolean masterDetailRuntime, int editGridCols) {
        if (masterDetailRuntime) {
            return "1080px";
        }
        if (editGridCols >= 3) {
            return "1180px";
        }
        return editGridCols > 1 ? "1040px" : "800px";
    }

    private String resolveModalType(Object value) {
        String modalType = StringUtils.defaultIfBlank(text(value), "modal").toLowerCase(Locale.ROOT);
        return Set.of("modal", "drawer").contains(modalType) ? modalType : "modal";
    }

    private Set<String> resolveToolbarStandardActions(LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getListGridLayout() == null) {
            return Set.of();
        }
        Object items = pageSchema.getListGridLayout().get("items");
        if (!(items instanceof List<?> list)) {
            return Set.of();
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> block) || !"toolbar".equals(text(block.get("blockType")))) {
                continue;
            }
            Object props = block.get("props");
            if (!(props instanceof Map<?, ?> propsMap)) {
                return Set.of();
            }
            Object actions = propsMap.get("actions");
            if (!(actions instanceof List<?> actionList)) {
                return Set.of();
            }
            return actionList.stream()
                    .map(this::text)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return Set.of();
    }

    private int resolveEditGridCols(LowcodePageSchema pageSchema) {
        int cols = 1;
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        if (editZone != null && editZone.getProps() != null) {
            Integer configuredCols = integerValue(editZone.getProps().get("editGridCols"));
            if (configuredCols != null && configuredCols > 0) {
                return Math.max(1, Math.min(3, configuredCols));
            }
            cols = Math.max(cols, resolveCanvasGridCols(editZone));
        }
        for (Map<String, Object> rule : extractFormRules(pageSchema)) {
            Object col = rule.get("col");
            if (!(col instanceof Map<?, ?> colMap)) {
                continue;
            }
            Integer span = integerValue(colMap.get("span"));
            if (span == null || span <= 0 || span >= 24) {
                continue;
            }
            cols = Math.max(cols, Math.min(3, Math.max(1, (int) Math.ceil(24.0 / span))));
        }
        return cols;
    }

    @SuppressWarnings("unchecked")
    private int resolveCanvasGridCols(LowcodePageZone editZone) {
        List<Map<String, Object>> items = extractCanvasItems(editZone);
        if (items.isEmpty()) {
            return 1;
        }
        List<Integer> columns = new ArrayList<>();
        items.stream()
                .filter(item -> StringUtils.isNotBlank(text(item.get("fieldRef"))))
                .sorted(Comparator.comparingInt(item -> intValue(item.get("x"), 0)))
                .forEach(item -> {
                    int x = intValue(item.get("x"), 0);
                    boolean exists = columns.stream().anyMatch(columnX -> Math.abs(columnX - x) < 80);
                    if (!exists) {
                        columns.add(x);
                    }
                });
        return Math.max(1, Math.min(3, columns.isEmpty() ? 1 : columns.size()));
    }

    private List<LowcodeFieldSchema> sortByCanvasOrder(List<LowcodeFieldSchema> fields,
                                                       LowcodePageSchema pageSchema,
                                                       String zoneKey) {
        if (fields == null || fields.size() <= 1) {
            return fields == null ? List.of() : fields;
        }
        LowcodePageZone zone = findZone(pageSchema, zoneKey);
        if (zone != null && zone.getProps() != null
                && "formDesignerSchema".equals(text(zone.getProps().get("compiledFrom")))) {
            return fields;
        }
        List<Map<String, Object>> items = extractCanvasItems(zone);
        if (items.isEmpty()) {
            return fields;
        }
        Map<String, LowcodeFieldSchema> fieldMap = fields.stream()
                .collect(Collectors.toMap(
                        LowcodeFieldSchema::getField,
                        field -> field,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<LowcodeFieldSchema> ordered = new ArrayList<>();
        items.stream()
                .sorted(this::compareCanvasItemPosition)
                .map(item -> text(item.get("fieldRef")))
                .filter(StringUtils::isNotBlank)
                .distinct()
                .forEach(fieldRef -> {
                    LowcodeFieldSchema field = fieldMap.remove(fieldRef);
                    if (field != null) {
                        ordered.add(field);
                    }
                });
        ordered.addAll(fieldMap.values());
        return ordered;
    }

    private int compareCanvasItemPosition(Map<String, Object> left, Map<String, Object> right) {
        int leftRow = Math.round(intValue(left.get("y"), 0) / 16.0f);
        int rightRow = Math.round(intValue(right.get("y"), 0) / 16.0f);
        if (leftRow != rightRow) {
            return Integer.compare(leftRow, rightRow);
        }
        int xCompare = Integer.compare(intValue(left.get("x"), 0), intValue(right.get("x"), 0));
        if (xCompare != 0) {
            return xCompare;
        }
        return Integer.compare(intValue(left.get("zIndex"), 0), intValue(right.get("zIndex"), 0));
    }

    private List<Map<String, Object>> buildJoinConfig(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().size() <= 1) {
            return List.of();
        }
        LowcodePageModelRef primaryRef = pageSchema.getModelRefs().stream()
                .filter(ref -> Boolean.TRUE.equals(ref.getPrimary()))
                .findFirst()
                .orElse(null);
        String fallbackPrimaryCode = primaryRef == null
                ? modelSchema.getObject() == null ? null : modelSchema.getObject().getCode()
                : primaryRef.getModelCode();
        String primaryModelCode = StringUtils.defaultIfBlank(pageSchema.getPrimaryModelCode(), fallbackPrimaryCode);
        if (StringUtils.isBlank(primaryModelCode)) {
            return List.of();
        }
        List<LowcodeRelationSchema> primaryRelations = primaryRef != null && primaryRef.getRelations() != null
                ? primaryRef.getRelations()
                : modelSchema.getRelations();
        List<Map<String, Object>> result = new ArrayList<>();
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getModelCode())) {
                continue;
            }
            LowcodeRelationSchema relation = resolveRuntimeRelation(primaryModelCode, ref, primaryRelations);
            if (relation == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("modelCode", ref.getModelCode());
            item.put("modelName", ref.getModelName());
            item.put("tableName", ref.getTableName());
            item.put("sourceField", relation.getSourceField());
            item.put("targetField", relation.getTargetField());
            item.put("targetObjectCode", relation.getTargetObjectCode());
            item.put("relationType", StringUtils.defaultIfBlank(relation.getRelationType(), "REFERENCE"));
            LowcodeRelationSchema primaryRelation = findRelationFromPrimary(primaryRelations, ref.getModelCode());
            if (primaryRelation != null) {
                String displaySourceField = resolveRelationDisplayField(ref, primaryRelation);
                String relationSourceField = normalizePrimaryFieldName(modelSchema, primaryRelation.getSourceField());
                putIfNotBlank(item, "displayField", displaySourceField);
                putIfNotBlank(item, "displayAlias", buildRelationDisplayAlias(relationSourceField));
            }
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> buildMasterDetailConfig(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Map<String, Object> config = new LinkedHashMap<>();
        LowcodePageModelRef primaryRef = resolvePrimaryRef(modelSchema, pageSchema);
        String primaryModelCode = primaryRef == null
                ? modelSchema.getObject() == null ? null : modelSchema.getObject().getCode()
                : primaryRef.getModelCode();
        primaryModelCode = StringUtils.defaultIfBlank(pageSchema.getPrimaryModelCode(), primaryModelCode);

        Map<String, Object> primary = new LinkedHashMap<>();
        primary.put("modelCode", primaryModelCode);
        primary.put("modelName", primaryRef == null ? modelSchema.getBusinessName() : primaryRef.getModelName());
        primary.put("tableName", modelSchema.getTableName());
        primary.put("keyField", "id");
        config.put("primary", primary);

        List<Map<String, Object>> children = new ArrayList<>();
        if (StringUtils.isBlank(primaryModelCode) || pageSchema.getModelRefs() == null) {
            config.put("children", children);
            return config;
        }

        List<LowcodeRelationSchema> primaryRelations = primaryRef != null && primaryRef.getRelations() != null
                ? primaryRef.getRelations()
                : modelSchema.getRelations();
        List<String> selectedEditRefs = resolveSelectedEditRefs(pageSchema);
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getModelCode())) {
                continue;
            }
            LowcodeRelationSchema relation = resolveRuntimeRelation(primaryModelCode, ref, primaryRelations);
            if (relation == null) {
                continue;
            }
            String childFkField = resolveChildRelationField(primaryModelCode, relation);
            List<Map<String, Object>> childFields = buildMasterDetailChildFields(ref, selectedEditRefs, childFkField);
            if (childFields.isEmpty()) {
                continue;
            }
            Map<String, Object> child = new LinkedHashMap<>();
            Map<String, Object> refProps = ref.getProps() == null ? Map.of() : ref.getProps();
            child.put("key", ref.getModelCode());
            child.put("modelCode", ref.getModelCode());
            child.put("modelName", ref.getModelName());
            child.put("tableName", ref.getTableName());
            child.put("relationType", StringUtils.defaultIfBlank(relation.getRelationType(), "ONE_TO_MANY"));
            child.put("sourceField", childFkField);
            child.put("targetField", resolveMainRelationField(primaryModelCode, relation));
            child.put("showInCreate", booleanWithDefault(refProps.get("inlineCreateEnabled"), true));
            child.put("showInEdit", booleanWithDefault(refProps.get("inlineEditEnabled"), true));
            child.put("showInDetail", booleanWithDefault(refProps.get("showInDetail"), true));
            putIfNotBlank(child, "tabTitle", text(refProps.get("tabTitle")));
            putIfNotBlank(child, "relationName", text(refProps.get("relationName")));
            child.put("fields", childFields);
            children.add(child);
        }
        config.put("children", children);
        return config;
    }

    private LowcodePageModelRef resolvePrimaryRef(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().isEmpty()) {
            return null;
        }
        return pageSchema.getModelRefs().stream()
                .filter(ref -> Boolean.TRUE.equals(ref.getPrimary()))
                .findFirst()
                .orElseGet(() -> {
                    String modelCode = modelSchema.getObject() == null ? null : modelSchema.getObject().getCode();
                    return pageSchema.getModelRefs().stream()
                            .filter(ref -> StringUtils.equals(ref.getModelCode(), pageSchema.getPrimaryModelCode())
                                    || StringUtils.equals(ref.getModelCode(), modelCode))
                            .findFirst()
                            .orElse(pageSchema.getModelRefs().get(0));
                });
    }

    private List<Map<String, Object>> buildMasterDetailChildFields(LowcodePageModelRef ref,
                                                                   List<String> selectedEditRefs,
                                                                   String childFkField) {
        if (ref.getFields() == null) {
            return List.of();
        }
        Set<String> selectedEditRefSet = new LinkedHashSet<>(selectedEditRefs);
        boolean hasSelectedChildRefs = ref.getFields().stream()
                .map(source -> StringUtils.defaultIfBlank(text(source.get("fieldRef")),
                        safeKey(ref.getModelCode()) + "__"
                                + StringUtils.defaultIfBlank(text(source.get("sourceField")), text(source.get("field")))))
                .anyMatch(selectedEditRefSet::contains);
        Map<String, Integer> selectedOrder = new LinkedHashMap<>();
        for (int i = 0; i < selectedEditRefs.size(); i++) {
            selectedOrder.putIfAbsent(selectedEditRefs.get(i), i);
        }
        List<Map<String, Object>> fields = new ArrayList<>();
        for (Map<String, Object> source : ref.getFields()) {
            LowcodeFieldSchema field = buildMasterDetailChildField(ref, source);
            if (field == null) {
                continue;
            }
            String fieldRef = StringUtils.defaultIfBlank(text(source.get("fieldRef")),
                    safeKey(ref.getModelCode()) + "__" + field.getField());
            if (hasSelectedChildRefs && !selectedEditRefSet.contains(fieldRef)) {
                continue;
            }
            if (!isChildEditFieldAllowed(field, childFkField)) {
                continue;
            }
            Map<String, Object> item = buildEditField(field);
            item.put("sourceField", field.getField());
            item.put("fieldRef", fieldRef);
            item.put("columnName", field.getColumnName());
            item.put("modelCode", ref.getModelCode());
            item.put("modelName", ref.getModelName());
            fields.add(item);
        }
        if (!selectedOrder.isEmpty()) {
            fields.sort(Comparator.comparingInt(item ->
                    selectedOrder.getOrDefault(text(item.get("fieldRef")), Integer.MAX_VALUE)));
        }
        return fields;
    }

    private LowcodeFieldSchema buildMasterDetailChildField(LowcodePageModelRef ref, Map<String, Object> source) {
        LowcodeFieldSchema field = buildPageRefField(ref, source);
        if (field == null) {
            return null;
        }
        String sourceField = StringUtils.defaultIfBlank(text(source.get("sourceField")), text(source.get("field")));
        field.setField(sourceField);
        field.setLabel(StringUtils.defaultIfBlank(text(source.get("rawLabel")),
                StringUtils.defaultIfBlank(text(source.get("label")), sourceField)));
        return field;
    }

    private boolean isChildEditFieldAllowed(LowcodeFieldSchema field, String childFkField) {
        if (field == null) {
            return false;
        }
        String fieldName = field.getField();
        String columnName = field.getColumnName();
        if (StringUtils.equals(fieldName, childFkField) || StringUtils.equals(columnName, childFkField)
                || StringUtils.equals(columnName, camelToSnake(childFkField))) {
            return false;
        }
        return !isSystemField(field)
                && isActiveField(field)
                && !SYSTEM_FIELD_NAMES.contains(fieldName)
                && !SYSTEM_COLUMN_NAMES.contains(columnName)
                && !Boolean.TRUE.equals(field.getReadonly())
                && !Boolean.TRUE.equals(field.getPrimaryKey())
                && (field.getFormVisible() == null || Boolean.TRUE.equals(field.getFormVisible()));
    }

    private List<String> resolveSelectedEditRefs(LowcodePageSchema pageSchema) {
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        if (editZone == null || Boolean.FALSE.equals(editZone.getEnabled()) || editZone.getFieldRefs() == null) {
            return List.of();
        }
        return editZone.getFieldRefs().stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    private String resolveChildRelationField(String primaryModelCode, LowcodeRelationSchema relation) {
        if (relation == null) {
            return null;
        }
        return primaryModelCode.equals(relation.getTargetObjectCode())
                ? relation.getSourceField()
                : relation.getTargetField();
    }

    private String resolveMainRelationField(String primaryModelCode, LowcodeRelationSchema relation) {
        if (relation == null) {
            return null;
        }
        return primaryModelCode.equals(relation.getTargetObjectCode())
                ? relation.getTargetField()
                : relation.getSourceField();
    }

    private LowcodeRelationSchema resolveRuntimeRelation(String primaryModelCode,
                                                         LowcodePageModelRef ref,
                                                         List<LowcodeRelationSchema> primaryRelations) {
        if (primaryRelations != null) {
            for (LowcodeRelationSchema relation : primaryRelations) {
                if (relation != null && ref.getModelCode().equals(relation.getTargetObjectCode())) {
                    return relation;
                }
            }
        }
        if (ref.getRelations() != null) {
            for (LowcodeRelationSchema relation : ref.getRelations()) {
                if (relation != null && primaryModelCode.equals(relation.getTargetObjectCode())) {
                    return relation;
                }
            }
        }
        return inferRuntimeRelation(primaryModelCode, ref);
    }

    private LowcodeRelationSchema findRelationFromPrimary(List<LowcodeRelationSchema> relations, String targetModelCode) {
        if (relations == null || StringUtils.isBlank(targetModelCode)) {
            return null;
        }
        return relations.stream()
                .filter(relation -> relation != null && targetModelCode.equals(relation.getTargetObjectCode()))
                .findFirst()
                .orElse(null);
    }

    private LowcodeRelationSchema inferRuntimeRelation(String primaryModelCode, LowcodePageModelRef ref) {
        if (StringUtils.isBlank(primaryModelCode) || ref == null || ref.getFields() == null) {
            return null;
        }
        String expectedCamel = snakeToCamel(primaryModelCode) + "Id";
        String expectedSnake = camelToSnake(primaryModelCode) + "_id";
        for (Map<String, Object> field : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String columnName = StringUtils.defaultIfBlank(text(field.get("columnName")), sourceField);
            if (expectedCamel.equals(sourceField) || expectedSnake.equals(columnName)) {
                LowcodeRelationSchema relation = new LowcodeRelationSchema();
                relation.setRelationType("ONE_TO_MANY");
                relation.setSourceField(sourceField);
                relation.setTargetObjectCode(primaryModelCode);
                relation.setTargetField("id");
                return relation;
            }
        }
        return null;
    }

    private boolean isTreeRuntime(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        String appType = StringUtils.defaultIfBlank(modelSchema.getAppType(), "SINGLE").toUpperCase(Locale.ROOT);
        return "TREE".equals(appType)
                || (modelSchema.getTreeConfig() != null && Boolean.TRUE.equals(modelSchema.getTreeConfig().getEnabled()))
                || (pageSchema != null && "tree-crud".equals(pageSchema.getLayoutType()))
                || extractTreeConfigOverrides(pageSchema) instanceof Map<?, ?>;
    }

    private boolean isMasterDetailRuntime(LowcodePageSchema pageSchema) {
        return pageSchema != null && MASTER_DETAIL_LAYOUT.equals(pageSchema.getLayoutType());
    }

    private Set<String> buildChildFieldRefs(LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getModelRefs() == null) {
            return Set.of();
        }
        Set<String> refs = new LinkedHashSet<>();
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || ref.getFields() == null) {
                continue;
            }
            for (Map<String, Object> field : ref.getFields()) {
                String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
                String fieldRef = StringUtils.defaultIfBlank(text(field.get("fieldRef")),
                        safeKey(ref.getModelCode()) + "__" + sourceField);
                if (StringUtils.isNotBlank(fieldRef)) {
                    refs.add(fieldRef);
                }
            }
        }
        return refs;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildTreeConfig(LowcodeModelSchema modelSchema,
                                                LowcodePageSchema pageSchema,
                                                Object overrides) {
        Map<String, Object> treeConfig = new LinkedHashMap<>();
        if (modelSchema.getTreeConfig() != null) {
            putIfNotBlank(treeConfig, "sourceModelCode", modelSchema.getTreeConfig().getSourceModelCode());
            putIfNotBlank(treeConfig, "sourceModelName", modelSchema.getTreeConfig().getSourceModelName());
            putIfNotBlank(treeConfig, "sourceTableName", modelSchema.getTreeConfig().getSourceTableName());
            putIfNotBlank(treeConfig, "keyField", modelSchema.getTreeConfig().getKeyField());
            putIfNotBlank(treeConfig, "parentField", modelSchema.getTreeConfig().getParentField());
            putIfNotBlank(treeConfig, "labelField", modelSchema.getTreeConfig().getLabelField());
            putIfNotBlank(treeConfig, "filterField", modelSchema.getTreeConfig().getFilterField());
            putIfNotBlank(treeConfig, "targetField", modelSchema.getTreeConfig().getTargetField());
            putIfNotBlank(treeConfig, "childrenField", modelSchema.getTreeConfig().getChildrenField());
            putIfNotBlank(treeConfig, "treeTitle", modelSchema.getTreeConfig().getTreeTitle());
            putIfNotBlank(treeConfig, "loadMode", modelSchema.getTreeConfig().getLoadMode());
            if (modelSchema.getTreeConfig().getEnabled() != null) {
                treeConfig.put("enabled", modelSchema.getTreeConfig().getEnabled());
            }
        }
        if (overrides instanceof Map<?, ?> map) {
            putIfNotBlank(treeConfig, "sourceModelCode", text(map.get("sourceModelCode")));
            putIfNotBlank(treeConfig, "sourceModelName", text(map.get("sourceModelName")));
            putIfNotBlank(treeConfig, "sourceTableName", text(map.get("sourceTableName")));
            putIfNotBlank(treeConfig, "keyField", text(map.get("keyField")));
            putIfNotBlank(treeConfig, "parentField", text(map.get("parentField")));
            putIfNotBlank(treeConfig, "labelField", text(map.get("labelField")));
            putIfNotBlank(treeConfig, "filterField", text(map.get("filterField")));
            putIfNotBlank(treeConfig, "targetField", text(map.get("targetField")));
            putIfNotBlank(treeConfig, "childrenField", text(map.get("childrenField")));
            putIfNotBlank(treeConfig, "treeTitle", text(map.get("treeTitle")));
            putIfNotBlank(treeConfig, "loadMode", text(map.get("loadMode")));
            if (StringUtils.isBlank(text(treeConfig.get("loadMode")))
                    && map.get("lazy") instanceof Boolean lazy
                    && lazy) {
                treeConfig.put("loadMode", "lazy");
            }
            if (map.get("enabled") instanceof Boolean enabled) {
                treeConfig.put("enabled", enabled);
            }
        }
        LowcodePageModelRef sourceRef = resolveTreeSourceRef(pageSchema, text(treeConfig.get("sourceModelCode")));
        if (sourceRef != null) {
            putIfNotBlank(treeConfig, "sourceModelCode", sourceRef.getModelCode());
            putIfNotBlank(treeConfig, "sourceModelName", sourceRef.getModelName());
            putIfNotBlank(treeConfig, "sourceTableName", sourceRef.getTableName());
            normalizeTreeSourceField(treeConfig, "keyField", sourceRef);
            normalizeTreeSourceField(treeConfig, "parentField", sourceRef);
            normalizeTreeSourceField(treeConfig, "labelField", sourceRef);
            normalizeTreeSourceField(treeConfig, "targetField", sourceRef);
        }
        treeConfig.putIfAbsent("keyField", "id");
        treeConfig.putIfAbsent("parentField", inferTreeParentField(modelSchema, sourceRef));
        treeConfig.putIfAbsent("labelField", inferTreeLabelField(modelSchema, sourceRef));
        LowcodeRelationSchema relation = sourceRef == null || Boolean.TRUE.equals(sourceRef.getPrimary())
                ? null
                : resolveRuntimeRelation(resolveTreePrimaryModelCode(modelSchema, pageSchema),
                sourceRef,
                resolvePrimaryTreeRelations(modelSchema, pageSchema));
        treeConfig.putIfAbsent("filterField", relation == null
                ? text(treeConfig.get("parentField"))
                : relation.getSourceField());
        treeConfig.putIfAbsent("targetField", relation == null
                ? text(treeConfig.get("keyField"))
                : relation.getTargetField());
        normalizePrimaryTreeField(treeConfig, "filterField", modelSchema);
        treeConfig.putIfAbsent("childrenField", "children");
        treeConfig.putIfAbsent("loadMode", "full");
        String defaultTreeTitle = StringUtils.defaultIfBlank(text(treeConfig.get("sourceModelName")), modelSchema.getBusinessName());
        treeConfig.putIfAbsent("treeTitle", StringUtils.isBlank(defaultTreeTitle) ? "树形导航" : defaultTreeTitle + "树");
        return treeConfig;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String inferTreeParentField(LowcodeModelSchema modelSchema, LowcodePageModelRef sourceRef) {
        return treeSourceFields(modelSchema, sourceRef).stream()
                .filter(field -> "parentId".equals(field) || "pid".equals(field) || "parentCode".equals(field))
                .findFirst()
                .orElse("parentId");
    }

    private String inferTreeLabelField(LowcodeModelSchema modelSchema, LowcodePageModelRef sourceRef) {
        List<String> fields = treeSourceFields(modelSchema, sourceRef);
        return fields.stream()
                .filter(field -> "name".equals(field) || "title".equals(field) || "label".equals(field))
                .findFirst()
                .orElseGet(() -> fields.isEmpty() ? "name" : fields.get(0));
    }

    private LowcodePageModelRef resolveTreeSourceRef(LowcodePageSchema pageSchema, String sourceModelCode) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().isEmpty()) {
            return null;
        }
        if (StringUtils.isNotBlank(sourceModelCode)) {
            for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
                if (ref != null && sourceModelCode.equals(ref.getModelCode())) {
                    return ref;
                }
            }
        }
        return pageSchema.getModelRefs().stream()
                .filter(ref -> ref != null && !Boolean.TRUE.equals(ref.getPrimary()))
                .findFirst()
                .orElseGet(() -> pageSchema.getModelRefs().stream()
                        .filter(ref -> ref != null && Boolean.TRUE.equals(ref.getPrimary()))
                        .findFirst()
                        .orElse(pageSchema.getModelRefs().get(0)));
    }

    private List<String> treeSourceFields(LowcodeModelSchema modelSchema, LowcodePageModelRef sourceRef) {
        if (sourceRef != null && sourceRef.getFields() != null && !sourceRef.getFields().isEmpty()) {
            return sourceRef.getFields().stream()
                    .map(field -> StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field"))))
                    .filter(StringUtils::isNotBlank)
                    .toList();
        }
        return modelSchema.getFields().stream()
                .map(LowcodeFieldSchema::getField)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    private void normalizeTreeSourceField(Map<String, Object> treeConfig, String key, LowcodePageModelRef sourceRef) {
        String value = text(treeConfig.get(key));
        if (StringUtils.isBlank(value) || sourceRef == null || sourceRef.getFields() == null) {
            return;
        }
        String normalized = resolveRefSourceField(sourceRef, value);
        if (StringUtils.isNotBlank(normalized)) {
            treeConfig.put(key, normalized);
        }
    }

    private void normalizePrimaryTreeField(Map<String, Object> treeConfig, String key, LowcodeModelSchema modelSchema) {
        String value = text(treeConfig.get(key));
        if (StringUtils.isBlank(value)) {
            return;
        }
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (value.equals(field.getField()) || value.equals(field.getColumnName())) {
                treeConfig.put(key, field.getField());
                return;
            }
        }
    }

    private String resolveRefSourceField(LowcodePageModelRef ref, String value) {
        if (ref == null || ref.getFields() == null || StringUtils.isBlank(value)) {
            return value;
        }
        for (Map<String, Object> field : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String fieldRef = StringUtils.defaultIfBlank(text(field.get("fieldRef")), sourceField);
            String columnName = text(field.get("columnName"));
            if (value.equals(sourceField)
                    || value.equals(fieldRef)
                    || value.equals(columnName)
                    || value.equals(ref.getModelCode() + "__" + sourceField)
                    || value.equals(ref.getModelCode() + "." + sourceField)) {
                return sourceField;
            }
        }
        return value;
    }

    private String resolveTreePrimaryModelCode(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        String code = pageSchema == null ? null : pageSchema.getPrimaryModelCode();
        if (StringUtils.isNotBlank(code)) {
            return code;
        }
        LowcodePageModelRef primaryRef = resolvePrimaryRef(modelSchema, pageSchema);
        if (primaryRef != null && StringUtils.isNotBlank(primaryRef.getModelCode())) {
            return primaryRef.getModelCode();
        }
        return modelSchema.getObject() == null ? null : modelSchema.getObject().getCode();
    }

    private List<LowcodeRelationSchema> resolvePrimaryTreeRelations(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        LowcodePageModelRef primaryRef = resolvePrimaryRef(modelSchema, pageSchema);
        if (primaryRef != null && primaryRef.getRelations() != null && !primaryRef.getRelations().isEmpty()) {
            return primaryRef.getRelations();
        }
        return modelSchema.getRelations();
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    private void appendTreeRuntimeField(List<Map<String, Object>> fields,
                                        LowcodeModelSchema modelSchema,
                                        LowcodePageSchema pageSchema,
                                        String zoneKey) {
        if (!isTreeRuntime(modelSchema, pageSchema)) {
            return;
        }
        String filterField = String.valueOf(buildTreeConfig(modelSchema, pageSchema, extractTreeConfigOverrides(pageSchema)).get("filterField"));
        boolean exists = fields.stream().anyMatch(item -> filterField.equals(item.get("field"))
                || filterField.equals(item.get("prop"))
                || filterField.equals(item.get("dataIndex"))
                || filterField.equals(item.get("key")));
        if (exists) {
            return;
        }
        LowcodeFieldSchema fieldSchema = findField(modelSchema, filterField);
        if (fieldSchema == null) {
            return;
        }
        Map<String, Object> runtimeField = "edit".equals(zoneKey) ? buildEditField(fieldSchema) : buildSearchField(fieldSchema);
        if (!"edit".equals(zoneKey)) {
            runtimeField.put("hidden", true);
        }
        runtimeField.put("queryType", "eq");
        runtimeField.put("required", false);
        fields.add(runtimeField);
    }

    @SuppressWarnings("unchecked")
    private void decorateTreeRuntimeFields(List<Map<String, Object>> fields,
                                           String configKey,
                                           LowcodeModelSchema modelSchema,
                                           LowcodePageSchema pageSchema) {
        if (!isTreeRuntime(modelSchema, pageSchema) || StringUtils.isBlank(configKey)) {
            return;
        }
        Map<String, Object> treeConfig = buildTreeConfig(modelSchema, pageSchema, extractTreeConfigOverrides(pageSchema));
        String filterField = text(treeConfig.get("filterField"));
        if (StringUtils.isBlank(filterField)) {
            return;
        }
        for (Map<String, Object> item : fields) {
            if (!filterField.equals(text(item.get("field")))) {
                continue;
            }
            String label = StringUtils.defaultIfBlank(text(item.get("label")), filterField);
            item.put("type", "treeSelect");
            item.put("queryType", "eq");
            Map<String, Object> props = new LinkedHashMap<>();
            Object sourceProps = item.get("props");
            if (sourceProps instanceof Map<?, ?> sourcePropsMap) {
                props.putAll((Map<String, Object>) sourcePropsMap);
            }
            props.putIfAbsent("placeholder", "请选择" + label);
            props.putIfAbsent("clearable", true);
            props.putIfAbsent("filterable", true);
            props.put("optionSource", buildTreeOptionSource(configKey, treeConfig));
            item.put("props", props);
        }
    }

    private Map<String, Object> buildTreeOptionSource(String configKey, Map<String, Object> treeConfig) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("type", "tree");
        source.put("api", "get@/ai/crud/" + configKey + "/tree");
        source.put("keyField", "key");
        source.put("valueField", "targetValue");
        source.put("labelField", "label");
        source.put("childrenField", StringUtils.defaultIfBlank(text(treeConfig.get("childrenField")), "children"));
        source.put("params", Map.of("loadMode", "full"));
        return source;
    }

    private Object extractTreeConfigOverrides(LowcodePageSchema pageSchema) {
        LowcodePageZone tableZone = findZone(pageSchema, "table");
        if (tableZone != null && tableZone.getProps() != null && tableZone.getProps().get("treeConfig") != null) {
            return tableZone.getProps().get("treeConfig");
        }
        return extractGridTreeConfigOverrides(pageSchema);
    }

    private Object extractGridTreeConfigOverrides(LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getListGridLayout() == null) {
            return null;
        }
        Object items = pageSchema.getListGridLayout().get("items");
        if (!(items instanceof List<?> itemList)) {
            return null;
        }
        for (Object item : itemList) {
            if (!(item instanceof Map<?, ?> block)) {
                continue;
            }
            if (!"tree-panel".equals(String.valueOf(block.get("blockType")))) {
                continue;
            }
            Object props = block.get("props");
            if (props instanceof Map<?, ?>) {
                return props;
            }
        }
        return null;
    }

    private LowcodeFieldSchema findField(LowcodeModelSchema modelSchema, String fieldName) {
        if (modelSchema == null || modelSchema.getFields() == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        return modelSchema.getFields().stream()
                .filter(field -> fieldName.equals(field.getField()))
                .findFirst()
                .orElse(null);
    }

    private List<Map<String, Object>> buildDictConfig(LowcodeModelSchema modelSchema) {
        Map<String, String> dictMap = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (StringUtils.isNotBlank(field.getDictType())) {
                dictMap.putIfAbsent(field.getDictType(), StringUtils.defaultIfBlank(field.getLabel(), field.getDictType()));
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : dictMap.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("dictType", entry.getKey());
            item.put("dictName", entry.getValue());
            item.put("isNew", false);
            item.put("items", List.of());
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> buildDesensitizeConfig(LowcodeModelSchema modelSchema) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            String sensitiveType = StringUtils.defaultIfBlank(field.getSensitiveType(), "NONE").toUpperCase(Locale.ROOT);
            if ("NONE".equals(sensitiveType)) {
                continue;
            }
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("type", sensitiveType);
            rule.put("label", StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
            result.put(field.getField(), rule);
        }
        return result;
    }

    private Map<String, Object> buildEncryptConfig(LowcodeModelSchema modelSchema) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (StringUtils.isBlank(field.getEncryptAlgorithm())) {
                continue;
            }
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("algorithm", field.getEncryptAlgorithm());
            result.put(field.getField(), rule);
        }
        return result;
    }

    private Map<String, Object> buildTransConfig(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            String componentType = StringUtils.defaultIfBlank(field.getComponentType(), "input");
            if (StringUtils.isNotBlank(field.getDictType())) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("dictType", field.getDictType());
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            } else if ("orgTreeSelect".equals(componentType)) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("type", "orgName");
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            } else if ("userSelect".equals(componentType)) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("type", "userName");
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            } else if ("regionTreeSelect".equals(componentType)) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("type", "regionName");
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            } else if ("fileUpload".equals(componentType) || "imageUpload".equals(componentType)) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("type", componentType);
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            }
        }
        appendRelationDisplayTransConfig(result, modelSchema, pageSchema);
        return result;
    }

    private void appendRelationDisplayTransConfig(Map<String, Object> transConfig,
                                                  LowcodeModelSchema modelSchema,
                                                  LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().size() <= 1) {
            return;
        }
        LowcodePageModelRef primaryRef = resolvePrimaryRef(modelSchema, pageSchema);
        if (primaryRef == null) {
            return;
        }
        List<LowcodeRelationSchema> primaryRelations = primaryRef.getRelations() != null && !primaryRef.getRelations().isEmpty()
                ? primaryRef.getRelations()
                : modelSchema.getRelations();
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getModelCode())) {
                continue;
            }
            LowcodeRelationSchema relation = findRelationFromPrimary(primaryRelations, ref.getModelCode());
            if (relation == null) {
                continue;
            }
            String sourceField = normalizePrimaryFieldName(modelSchema, relation.getSourceField());
            if (StringUtils.isBlank(sourceField) || findField(modelSchema, sourceField) == null || transConfig.containsKey(sourceField)) {
                continue;
            }
            String displayField = resolveRelationDisplayField(ref, relation);
            if (StringUtils.isBlank(displayField)) {
                continue;
            }
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("type", "relationName");
            rule.put("targetField", buildRelationDisplayAlias(sourceField));
            rule.put("relationModelCode", ref.getModelCode());
            rule.put("displayField", displayField);
            transConfig.put(sourceField, rule);
        }
    }

    private String resolveRelationDisplayField(LowcodePageModelRef ref, LowcodeRelationSchema relation) {
        if (ref == null || ref.getFields() == null || ref.getFields().isEmpty()) {
            return null;
        }
        String configured = resolveRefSourceField(ref, relation == null ? null : relation.getDisplayField());
        if (hasRefSourceField(ref, configured)) {
            return configured;
        }
        Set<String> excluded = new LinkedHashSet<>();
        excluded.add(resolveRefSourceField(ref, relation == null ? null : relation.getTargetField()));
        excluded.add(resolveRefSourceField(ref, relation == null ? null : relation.getSourceField()));

        String matched = pickRefDisplayField(ref, excluded, Set.of("name", "title", "label", "orgName", "deptName"));
        if (StringUtils.isNotBlank(matched)) {
            return matched;
        }
        return pickRefDisplayField(ref, excluded, Set.of());
    }

    private String pickRefDisplayField(LowcodePageModelRef ref, Set<String> excluded, Set<String> preferredNames) {
        if (ref == null || ref.getFields() == null) {
            return null;
        }
        for (Map<String, Object> field : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String columnName = text(field.get("columnName"));
            if (StringUtils.isBlank(sourceField)
                    || excluded.contains(sourceField)
                    || excluded.contains(columnName)
                    || SYSTEM_FIELD_NAMES.contains(sourceField)
                    || SYSTEM_COLUMN_NAMES.contains(columnName)) {
                continue;
            }
            if (!preferredNames.isEmpty() && !preferredNames.contains(sourceField)) {
                continue;
            }
            return sourceField;
        }
        return null;
    }

    private boolean hasRefSourceField(LowcodePageModelRef ref, String sourceField) {
        if (ref == null || ref.getFields() == null || StringUtils.isBlank(sourceField)) {
            return false;
        }
        for (Map<String, Object> field : ref.getFields()) {
            String candidate = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String columnName = text(field.get("columnName"));
            if (sourceField.equals(candidate) || sourceField.equals(columnName)) {
                return true;
            }
        }
        return false;
    }

    private String normalizePrimaryFieldName(LowcodeModelSchema modelSchema, String value) {
        if (StringUtils.isBlank(value) || modelSchema == null || modelSchema.getFields() == null) {
            return value;
        }
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (value.equals(field.getField()) || value.equals(field.getColumnName())) {
                return field.getField();
            }
        }
        return value;
    }

    private String buildRelationDisplayAlias(String sourceField) {
        return StringUtils.isBlank(sourceField) ? null : sourceField + "Name";
    }

    private RelationLookupMeta resolveRelationLookup(LowcodeModelSchema modelSchema,
                                                     LowcodePageSchema pageSchema,
                                                     String fieldName) {
        if (StringUtils.isBlank(fieldName) || modelSchema == null || pageSchema == null
                || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().size() <= 1) {
            return null;
        }
        LowcodePageModelRef primaryRef = resolvePrimaryRef(modelSchema, pageSchema);
        if (primaryRef == null) {
            return null;
        }
        List<LowcodeRelationSchema> primaryRelations = primaryRef.getRelations() != null && !primaryRef.getRelations().isEmpty()
                ? primaryRef.getRelations()
                : modelSchema.getRelations();
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getModelCode())) {
                continue;
            }
            LowcodeRelationSchema relation = findRelationFromPrimary(primaryRelations, ref.getModelCode());
            if (relation == null || !"REFERENCE".equalsIgnoreCase(StringUtils.defaultString(relation.getRelationType()))) {
                continue;
            }
            String sourceField = normalizePrimaryFieldName(modelSchema, relation.getSourceField());
            if (!fieldName.equals(sourceField)) {
                continue;
            }
            String displayField = resolveRelationDisplayField(ref, relation);
            Map<String, Object> props = ref.getProps() == null ? Map.of() : ref.getProps();
            return new RelationLookupMeta(
                    ref.getModelCode(),
                    ref.getModelName(),
                    text(props.get("targetConfigKey")),
                    sourceField,
                    StringUtils.defaultIfBlank(relation.getTargetField(), text(props.get("targetField"))),
                    displayField
            );
        }
        return null;
    }

    private Map<String, Object> buildRelationLookupConfig(RelationLookupMeta lookupMeta) {
        Map<String, Object> config = new LinkedHashMap<>();
        if (lookupMeta == null) {
            return config;
        }
        putIfNotBlank(config, "modelCode", lookupMeta.modelCode());
        putIfNotBlank(config, "modelName", lookupMeta.modelName());
        putIfNotBlank(config, "configKey", lookupMeta.configKey());
        putIfNotBlank(config, "sourceField", lookupMeta.sourceField());
        putIfNotBlank(config, "targetField", lookupMeta.targetField());
        putIfNotBlank(config, "displayField", lookupMeta.displayField());
        putIfNotBlank(config, "targetFieldAlias", buildRelationDisplayAlias(lookupMeta.sourceField()));
        return config;
    }

    @SuppressWarnings("unchecked")
    private void applyRelationLookupProps(Map<String, Object> item,
                                          RelationLookupMeta lookupMeta,
                                          String label) {
        if (item == null || lookupMeta == null) {
            return;
        }
        Map<String, Object> props = item.get("props") instanceof Map<?, ?> propsMap
                ? new LinkedHashMap<>((Map<String, Object>) propsMap)
                : new LinkedHashMap<>();
        props.putIfAbsent("clearable", true);
        props.putIfAbsent("filterable", true);
        props.putIfAbsent("placeholder", "请选择" + StringUtils.defaultIfBlank(label, "关联数据"));
        putIfNotBlank(props, "labelValueField", buildRelationDisplayAlias(lookupMeta.sourceField()));
        Map<String, Object> optionSource = buildRelationOptionSource(lookupMeta);
        if (!optionSource.isEmpty()) {
            item.put("optionSource", optionSource);
            props.put("optionSource", optionSource);
        }
        item.put("props", props);
    }

    private Map<String, Object> buildRelationOptionSource(RelationLookupMeta lookupMeta) {
        if (lookupMeta == null || StringUtils.isBlank(lookupMeta.configKey())) {
            return Map.of();
        }
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("type", "list");
        source.put("api", "get@/ai/crud/" + lookupMeta.configKey() + "/page");
        source.put("recordsField", "records");
        source.put("valueField", StringUtils.defaultIfBlank(lookupMeta.targetField(), "id"));
        source.put("keyField", StringUtils.defaultIfBlank(lookupMeta.targetField(), "id"));
        source.put("labelField", StringUtils.defaultIfBlank(lookupMeta.displayField(), "name"));
        source.put("fallbackLabelFields", List.of(
                StringUtils.defaultIfBlank(lookupMeta.displayField(), "name"),
                "name", "title", "label", "customerName", "contactName", "objectName"
        ));
        source.put("params", Map.of(
                "pageNum", 1,
                "pageSize", 50
        ));
        return source;
    }

    private Map<String, Object> buildSearchField(LowcodeFieldSchema field) {
        return buildSearchField(field, Map.of(), null, null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSearchField(LowcodeFieldSchema field,
                                                 Map<String, Object> pageSetting,
                                                 LowcodeModelSchema modelSchema,
                                                 LowcodePageSchema pageSchema) {
        Map<String, Object> item = new LinkedHashMap<>();
        String configuredQueryFieldName = StringUtils.defaultIfBlank(text(pageSetting.get("queryField")), field.getField());
        LowcodeFieldSchema queryField = findRuntimeField(modelSchema, pageSchema, configuredQueryFieldName);
        String queryFieldName = queryField == null ? field.getField() : configuredQueryFieldName;
        LowcodeFieldSchema effectiveField = queryField == null ? field : queryField;
        String queryType = StringUtils.defaultIfBlank(text(pageSetting.get("queryType")),
                StringUtils.defaultIfBlank(effectiveField.getQueryType(), StringUtils.defaultIfBlank(field.getQueryType(), "eq")))
                .toLowerCase(Locale.ROOT);
        item.put("field", queryFieldName);
        item.put("label", StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        String componentType = resolveSearchComponentType(effectiveField, queryType, pageSetting);
        item.put("type", componentType);
        item.put("queryType", queryType);
        applyAlignment(item, pageSetting);
        if (pageSetting.containsKey("defaultValue")) {
            item.put("defaultValue", pageSetting.get("defaultValue"));
        }
        if (pageSetting.containsKey("collapsed")) {
            item.put("collapsed", booleanWithDefault(pageSetting.get("collapsed"), false));
        }
        RelationLookupMeta lookupMeta = resolveRelationLookup(modelSchema, pageSchema, field.getField());
        if (lookupMeta != null) {
            item.put("type", "select");
            item.put("queryType", "eq");
            item.put("relationLookup", buildRelationLookupConfig(lookupMeta));
        }
        if ("daterange".equals(componentType) || "datetimerange".equals(componentType) || "timerange".equals(componentType)) {
            item.put("startPlaceholder", "开始" + StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
            item.put("endPlaceholder", "结束" + StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        }
        String dictType = StringUtils.defaultIfBlank(text(pageSetting.get("dictType")), effectiveField.getDictType());
        if (StringUtils.isNotBlank(dictType)) {
            item.put("dictType", dictType);
        }
        Map<String, Object> props = sanitizeFieldBasicProps(field);
        Object designerProps = pageSetting.get("props");
        if (designerProps instanceof Map<?, ?> designerPropsMap) {
            props.putAll((Map<String, Object>) designerPropsMap);
        }
        if (!props.isEmpty()) {
            item.put("props", props);
        }
        if (lookupMeta != null) {
            applyRelationLookupProps(item, lookupMeta, StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        }
        return item;
    }

    private LowcodeFieldSchema findRuntimeField(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema, String fieldName) {
        if (StringUtils.isBlank(fieldName) || modelSchema == null) {
            return null;
        }
        LowcodeFieldSchema field = findField(modelSchema, fieldName);
        if (field != null) {
            return field;
        }
        Map<String, LowcodeFieldSchema> fieldMap = buildRuntimeFieldMap(modelSchema, pageSchema);
        return fieldMap.get(fieldName);
    }

    private Map<String, Object> buildTableColumn(LowcodeFieldSchema field) {
        return buildTableColumn(field, Map.of());
    }

    private Map<String, Object> buildTableColumn(LowcodeFieldSchema field, Map<String, Object> pageSetting) {
        return buildTableColumn(field, pageSetting, null, null);
    }

    private Map<String, Object> buildTableColumn(LowcodeFieldSchema field,
                                                 Map<String, Object> pageSetting,
                                                 LowcodeModelSchema modelSchema,
                                                 LowcodePageSchema pageSchema) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", field.getField());
        item.put("title", StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        item.put("dataIndex", field.getField());
        applyAlignment(item, pageSetting);
        Integer settingWidth = integerValue(pageSetting.get("width"));
        if (settingWidth != null && settingWidth > 0) {
            item.put("width", settingWidth);
        } else if (field.getWidth() != null && field.getWidth() > 0) {
            item.put("width", field.getWidth());
        }
        String fixed = normalizeFixed(text(pageSetting.get("fixed")));
        if (StringUtils.isNotBlank(fixed)) {
            item.put("fixed", fixed);
        }
        Object sortable = pageSetting.get("sortable");
        if (Boolean.TRUE.equals(sortable) || Boolean.TRUE.equals(field.getSortable())) {
            item.put("sorter", true);
        }
        String componentType = StringUtils.defaultIfBlank(text(pageSetting.get("componentType")), field.getComponentType());
        componentType = StringUtils.defaultIfBlank(componentType, "input");
        String renderType = StringUtils.defaultIfBlank(text(pageSetting.get("renderType")),
                resolveDefaultRenderType(field, componentType));
        String targetField = StringUtils.defaultIfBlank(text(pageSetting.get("targetField")), field.getField() + "Name");
        RelationLookupMeta lookupMeta = resolveRelationLookup(modelSchema, pageSchema, field.getField());
        if (lookupMeta != null) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "relationName");
            render.put("targetField", buildRelationDisplayAlias(field.getField()));
            render.put("relationModelCode", lookupMeta.modelCode());
            render.put("displayField", lookupMeta.displayField());
            item.put("render", render);
        } else if ("dictTag".equals(renderType) || (StringUtils.isBlank(renderType) && StringUtils.isNotBlank(field.getDictType()))) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "dictTag");
            render.put("dictType", field.getDictType());
            item.put("render", render);
        } else if ("orgName".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "orgName");
            render.put("targetField", targetField);
            item.put("render", render);
        } else if ("userName".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "userName");
            render.put("targetField", targetField);
            item.put("render", render);
        } else if ("regionName".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "regionName");
            render.put("targetField", targetField);
            item.put("render", render);
        } else if ("fileUpload".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "fileUpload");
            render.put("targetField", targetField);
            item.put("render", render);
        } else if ("imageUpload".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "imageUpload");
            render.put("targetField", targetField);
            item.put("render", render);
        }
        copyTableColumnDesignerSettings(item, pageSetting);
        return item;
    }

    private void copyTableColumnDesignerSettings(Map<String, Object> item, Map<String, Object> pageSetting) {
        putIfNotBlank(item, "renderType", text(pageSetting.get("renderType")));
        putIfNotBlank(item, "targetField", text(pageSetting.get("targetField")));
        putIfNotBlank(item, "textColor", text(pageSetting.get("textColor")));
        String clickAction = StringUtils.defaultIfBlank(text(pageSetting.get("clickAction")), "none");
        if (!"none".equals(clickAction)) {
            item.put("clickAction", clickAction);
            putIfNotBlank(item, "targetPageKey", text(pageSetting.get("targetPageKey")));
            putIfNotBlank(item, "targetFormKey", text(pageSetting.get("targetFormKey")));
            putIfNotBlank(item, "targetParamName", text(pageSetting.get("targetParamName")));
            putIfNotBlank(item, "targetParamField", text(pageSetting.get("targetParamField")));
        }
    }

    private String resolveDefaultRenderType(LowcodeFieldSchema field, String componentType) {
        if (field != null && StringUtils.isNotBlank(field.getDictType())) {
            return "dictTag";
        }
        return switch (StringUtils.defaultString(componentType)) {
            case "orgTreeSelect" -> "orgName";
            case "userSelect" -> "userName";
            case "regionTreeSelect" -> "regionName";
            case "fileUpload", "imageUpload" -> componentType;
            default -> "";
        };
    }

    private void applyAlignment(Map<String, Object> item, Map<String, Object> pageSetting) {
        String align = normalizeAlign(StringUtils.defaultIfBlank(text(pageSetting.get("align")),
                text(pageSetting.get("textAlign"))));
        if (StringUtils.isNotBlank(align)) {
            item.put("align", align);
        }
    }

    private String normalizeAlign(String value) {
        String align = StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
        return Set.of("left", "center", "right").contains(align) ? align : null;
    }

    private String normalizeFixed(String value) {
        String fixed = StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
        return Set.of("left", "right").contains(fixed) ? fixed : null;
    }

    private Map<String, Object> sanitizeFieldBasicProps(LowcodeFieldSchema field) {
        if (field == null || field.getBasicProps() == null || field.getBasicProps().isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> props = new LinkedHashMap<>();
        copyBasicProp(field.getBasicProps(), props, "placeholder");
        copyBasicProp(field.getBasicProps(), props, "cascade");
        copyBasicProp(field.getBasicProps(), props, "cascadeConfig");
        copyBasicProp(field.getBasicProps(), props, "sourceField");
        copyBasicProp(field.getBasicProps(), props, "sourceDictType");
        copyBasicProp(field.getBasicProps(), props, "linkedDictType");
        copyBasicProp(field.getBasicProps(), props, "linkedDictValue");
        copyBasicProp(field.getBasicProps(), props, "parentDictCode");
        copyBasicProp(field.getBasicProps(), props, "matchMode");
        copyBasicProp(field.getBasicProps(), props, "emptyStrategy");
        copyBasicProp(field.getBasicProps(), props, "clearOnSourceChange");
        copyBasicProp(field.getBasicProps(), props, "clearable");
        copyBasicProp(field.getBasicProps(), props, "filterable");
        copyBasicProp(field.getBasicProps(), props, "multiple");
        copyBasicProp(field.getBasicProps(), props, "optionSource");
        copyBasicProp(field.getBasicProps(), props, "labelValueField");
        copyBasicProp(field.getBasicProps(), props, "targetField");
        copyBasicProp(field.getBasicProps(), props, "rootCode");
        copyBasicProp(field.getBasicProps(), props, "dataRight");
        copyBasicProp(field.getBasicProps(), props, "virtualDisabled");
        copyBasicProp(field.getBasicProps(), props, "limit");
        copyBasicProp(field.getBasicProps(), props, "fileSize");
        copyBasicProp(field.getBasicProps(), props, "fileType");
        copyBasicProp(field.getBasicProps(), props, "storageType");
        copyBasicProp(field.getBasicProps(), props, "valueType");
        copyBasicProp(field.getBasicProps(), props, "showTip");
        copyBasicProp(field.getBasicProps(), props, "showFileList");
        copyBasicProp(field.getBasicProps(), props, "uploadButtonText");
        copyBasicProp(field.getBasicProps(), props, "businessType");
        copyBasicProp(field.getBasicProps(), props, "businessId");
        copyBasicProp(field.getBasicProps(), props, "referenceObjectCode");
        copyBasicProp(field.getBasicProps(), props, "referenceDisplayField");
        copyBasicProp(field.getBasicProps(), props, "targetObjectCode");
        copyBasicProp(field.getBasicProps(), props, "relationKey");
        copyBasicProp(field.getBasicProps(), props, "inlineCreateEnabled");
        copyBasicProp(field.getBasicProps(), props, "showInDetail");
        return props;
    }

    private void copyBasicProp(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveFieldSetting(LowcodePageSchema pageSchema, String zoneKey, String fieldName) {
        LowcodePageZone zone = findZone(pageSchema, zoneKey);
        if (zone == null || zone.getProps() == null || StringUtils.isBlank(fieldName)) {
            return Map.of();
        }
        Object settings = zone.getProps().get("fieldSettings");
        if (!(settings instanceof Map<?, ?> settingsMap)) {
            return Map.of();
        }
        Object value = settingsMap.get(fieldName);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private Map<String, Object> resolveRuntimeFieldSetting(LowcodePageSchema pageSchema, String zoneKey, String fieldName) {
        Map<String, Object> result = new LinkedHashMap<>(resolveFieldSetting(pageSchema, zoneKey, fieldName));
        Map<String, Object> gridSetting = resolveGridFieldSetting(pageSchema, zoneKey, fieldName);
        result.putAll(gridSetting);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveGridFieldSetting(LowcodePageSchema pageSchema, String zoneKey, String fieldName) {
        if (pageSchema == null || pageSchema.getListGridLayout() == null || StringUtils.isBlank(fieldName)) {
            return Map.of();
        }
        Object items = pageSchema.getListGridLayout().get("items");
        if (!(items instanceof List<?> list)) {
            return Map.of();
        }
        for (String blockType : runtimeSettingBlockTypes(zoneKey)) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> block)) {
                    continue;
                }
                if (!blockType.equals(text(block.get("blockType")))) {
                    continue;
                }
                Object propsValue = block.get("props");
                if (!(propsValue instanceof Map<?, ?> props)) {
                    continue;
                }
                Object settingsValue = props.get("fieldSettings");
                if (!(settingsValue instanceof Map<?, ?> settings)) {
                    continue;
                }
                Object value = settings.get(fieldName);
                if (value instanceof Map<?, ?> map) {
                    return (Map<String, Object>) map;
                }
            }
        }
        return Map.of();
    }

    private List<String> runtimeSettingBlockTypes(String zoneKey) {
        if ("search".equals(zoneKey)) {
            return List.of("search-form", "AiCrudPage");
        }
        if ("table".equals(zoneKey)) {
            return List.of("data-table", "AiCrudPage", "AiTable");
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveGridBlockProps(LowcodePageSchema pageSchema, List<String> blockTypes) {
        if (pageSchema == null || pageSchema.getListGridLayout() == null || blockTypes == null || blockTypes.isEmpty()) {
            return Map.of();
        }
        Object items = pageSchema.getListGridLayout().get("items");
        if (!(items instanceof List<?> list)) {
            return Map.of();
        }
        for (String blockType : blockTypes) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> block)) {
                    continue;
                }
                if (!blockType.equals(text(block.get("blockType")))) {
                    continue;
                }
                Object props = block.get("props");
                if (props instanceof Map<?, ?> map) {
                    return new LinkedHashMap<>((Map<String, Object>) map);
                }
            }
        }
        return Map.of();
    }

    private Map<String, Object> resolveEditFieldSetting(LowcodePageSchema pageSchema, String fieldName) {
        Map<String, Object> setting = new LinkedHashMap<>(resolveFieldSetting(pageSchema, "edit", fieldName));
        Map<String, Object> designerSetting = resolveFormRuleSetting(pageSchema, fieldName);
        setting.putAll(designerSetting);
        Map<String, Object> canvasSetting = resolveCanvasFieldSetting(pageSchema, fieldName);
        setting.putAll(canvasSetting);
        return setting;
    }

    private Map<String, Object> resolveCanvasFieldSetting(LowcodePageSchema pageSchema, String fieldName) {
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        if (editZone == null || StringUtils.isBlank(fieldName)) {
            return Map.of();
        }
        int gridCols = Math.max(1, resolveEditGridCols(pageSchema));
        int canvasWidth = 1040;
        if (editZone.getProps() != null) {
            Object canvas = editZone.getProps().get("canvas");
            if (canvas instanceof Map<?, ?> canvasMap) {
                canvasWidth = intValue(canvasMap.get("width"), canvasWidth);
            }
        }
        int colWidth = Math.max(1, (canvasWidth - 64) / gridCols);
        for (Map<String, Object> item : extractCanvasItems(editZone)) {
            if (!fieldName.equals(text(item.get("fieldRef")))) {
                continue;
            }
            Map<String, Object> setting = new LinkedHashMap<>();
            int itemWidth = intValue(item.get("w"), 280);
            int span = Math.max(1, Math.min(gridCols, Math.round((float) itemWidth / colWidth)));
            setting.put("span", span);
            Object style = item.get("style");
            if (style instanceof Map<?, ?> styleMap && styleMap.get("labelWidth") != null) {
                setting.put("labelWidth", styleMap.get("labelWidth"));
            }
            return setting;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveFormRuleSetting(LowcodePageSchema pageSchema, String fieldName) {
        if (StringUtils.isBlank(fieldName)) {
            return Map.of();
        }
        for (Map<String, Object> rule : extractFormRules(pageSchema)) {
            if (!fieldName.equals(text(rule.get("field")))) {
                continue;
            }
            Map<String, Object> setting = new LinkedHashMap<>();
            Object props = rule.get("props");
            if (props instanceof Map<?, ?> propsMap) {
                setting.put("props", new LinkedHashMap<>((Map<String, Object>) propsMap));
            }
            String componentType = extractForgeComponentType(rule);
            if (StringUtils.isNotBlank(componentType)) {
                setting.put("componentType", componentType);
            }
            String dictType = text(getNestedValue(rule, "props.dictType"));
            if (StringUtils.isNotBlank(dictType)) {
                setting.put("dictType", dictType);
            }
            Object requiredSwitch = rule.get("$required");
            List<Map<String, Object>> validationRules = copyRuleList(rule.get("validate"));
            boolean requiredFromSwitch = isRequiredSwitchEnabled(requiredSwitch);
            boolean required = requiredFromSwitch
                    || validationRules.stream().anyMatch(item -> booleanWithDefault(item.get("required"), false));
            if (required) {
                setting.put("required", true);
                String requiredMessage = requiredFromSwitch && requiredSwitch instanceof String message && StringUtils.isNotBlank(message)
                        ? message
                        : validationRules.stream()
                        .filter(item -> booleanWithDefault(item.get("required"), false))
                        .map(item -> text(item.get("message")))
                        .filter(StringUtils::isNotBlank)
                        .findFirst()
                        .orElse("");
                if (StringUtils.isNotBlank(requiredMessage)) {
                    setting.put("requiredMessage", requiredMessage);
                }
                if (requiredFromSwitch && validationRules.stream().noneMatch(item -> booleanWithDefault(item.get("required"), false))) {
                    Map<String, Object> requiredRule = new LinkedHashMap<>();
                    requiredRule.put("required", true);
                    requiredRule.put("message", StringUtils.defaultIfBlank(requiredMessage, "该字段为必填项"));
                    requiredRule.put("trigger", List.of("blur", "change"));
                    validationRules.add(0, requiredRule);
                }
            } else if (requiredSwitch != null) {
                setting.put("required", false);
                validationRules.removeIf(item -> booleanWithDefault(item.get("required"), false));
            }
            if (!validationRules.isEmpty()) {
                setting.put("rules", validationRules);
            }
            Object style = rule.get("style");
            if (style != null) {
                setting.put("componentStyle", style);
            }
            Object className = firstPresent(rule.get("className"), rule.get("class"));
            if (className != null) {
                setting.put("formItemClass", className);
            }
            Object forgeLayout = getNestedValue(rule, "_forge.layout");
            if (forgeLayout instanceof Map<?, ?> layoutMap) {
                Object align = layoutMap.get("align");
                if (align != null) {
                    setting.put("align", align);
                }
                Object forgeLabelWidth = layoutMap.get("labelWidth");
                if (forgeLabelWidth != null) {
                    setting.put("labelWidth", forgeLabelWidth);
                }
            }
            Object col = rule.get("col");
            if (col instanceof Map<?, ?> colMap) {
                int gridCols = resolveEditGridCols(pageSchema);
                Integer span = integerValue(colMap.get("span"));
                if (span != null && span > 0) {
                    int gridSpan = (int) Math.ceil(gridCols * Math.min(24, span) / 24.0);
                    setting.put("span", Math.max(1, Math.min(gridCols, gridSpan)));
                }
                Object gridStyle = colMap.get("style");
                if (gridStyle != null) {
                    setting.put("gridStyle", gridStyle);
                }
            }
            Object labelWidth = rule.get("labelWidth");
            if (labelWidth != null) {
                setting.put("labelWidth", labelWidth);
            }
            return setting;
        }
        return Map.of();
    }

    private String extractForgeComponentType(Map<String, Object> rule) {
        String componentKey = text(getNestedValue(rule, "_forge.componentKey"));
        if (StringUtils.isNotBlank(componentKey)) {
            return componentKey;
        }
        String dragTag = text(rule.get("_fc_drag_tag"));
        if (StringUtils.isBlank(dragTag)) {
            return null;
        }
        return switch (dragTag) {
            case "forgeDictSelect" -> "dictSelect";
            case "forgeRegionTreeSelect" -> "regionTreeSelect";
            case "forgeOrgTreeSelect" -> "orgTreeSelect";
            case "forgeUserSelect" -> "userSelect";
            case "forgeFileUpload" -> "fileUpload";
            case "forgeImageUpload" -> "imageUpload";
            case "forgeObjectReference" -> "objectReference";
            case "forgeSubTable" -> "subTable";
            default -> null;
        };
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractFormRules(LowcodePageSchema pageSchema) {
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        if (editZone == null || editZone.getProps() == null) {
            return List.of();
        }
        Object rules = editZone.getProps().get("formCreateRule");
        if (!(rules instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        collectFormRules(list, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractCanvasItems(LowcodePageZone zone) {
        if (zone == null || zone.getProps() == null) {
            return List.of();
        }
        Object canvas = zone.getProps().get("canvas");
        if (!(canvas instanceof Map<?, ?> canvasMap)) {
            return List.of();
        }
        Object items = canvasMap.get("items");
        if (!(items instanceof List<?> itemList)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : itemList) {
            if (item instanceof Map<?, ?> itemMap) {
                result.add((Map<String, Object>) itemMap);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void collectFormRules(List<?> rules, List<Map<String, Object>> result) {
        for (Object item : rules) {
            if (!(item instanceof Map<?, ?> rule)) {
                continue;
            }
            Map<String, Object> typedRule = (Map<String, Object>) rule;
            result.add(typedRule);
            Object children = typedRule.get("children");
            if (children instanceof List<?> childRules) {
                collectFormRules(childRules, result);
            }
        }
    }

    private Map<String, Object> buildEditField(LowcodeFieldSchema field) {
        return buildEditField(field, Map.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildEditField(LowcodeFieldSchema field, Map<String, Object> pageSetting) {
        return buildEditField(field, pageSetting, null, null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildEditField(LowcodeFieldSchema field,
                                               Map<String, Object> pageSetting,
                                               LowcodeModelSchema modelSchema,
                                               LowcodePageSchema pageSchema) {
        Map<String, Object> item = new LinkedHashMap<>();
        String label = StringUtils.defaultIfBlank(text(pageSetting.get("label")),
                StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        RelationLookupMeta lookupMeta = resolveRelationLookup(modelSchema, pageSchema, field.getField());
        String componentType = resolveEditComponentType(field, pageSetting);
        if (lookupMeta != null) {
            componentType = "select";
        }
        Map<String, Object> formulaConfig = field.getFormulaConfig();
        boolean formulaField = formulaConfig != null && !formulaConfig.isEmpty();
        item.put("field", field.getField());
        item.put("label", label);
        item.put("type", componentType);
        applyAlignment(item, pageSetting);
        boolean required = pageSetting.containsKey("required")
                ? booleanWithDefault(pageSetting.get("required"), false)
                : Boolean.TRUE.equals(field.getRequired());
        List<Map<String, Object>> validationRules = resolveRuntimeValidationRules(pageSetting);
        if (!pageSetting.containsKey("required")
                && validationRules.stream().anyMatch(rule -> booleanWithDefault(rule.get("required"), false))) {
            required = true;
        }
        boolean readonly = pageSetting.containsKey("readonly")
                ? booleanWithDefault(pageSetting.get("readonly"), false)
                : Boolean.TRUE.equals(field.getReadonly());
        if (formulaField) {
            readonly = true;
            required = false;
            validationRules.removeIf(rule -> booleanWithDefault(rule.get("required"), false));
        }
        if (!required) {
            validationRules.removeIf(rule -> booleanWithDefault(rule.get("required"), false));
        }
        item.put("required", !isSystemField(field) && required);
        String requiredMessage = StringUtils.defaultIfBlank(text(pageSetting.get("requiredMessage")),
                resolveRequiredRuleMessage(validationRules));
        if (StringUtils.isNotBlank(requiredMessage)) {
            item.put("requiredMessage", requiredMessage);
        }
        Object trigger = StringUtils.isNotBlank(text(pageSetting.get("trigger")))
                ? pageSetting.get("trigger")
                : resolveRequiredRuleTrigger(validationRules);
        if (trigger != null) {
            item.put("trigger", trigger);
        }
        if (isSystemField(field) || readonly) {
            item.put("disabled", true);
            item.put("readonly", true);
        }
        if (formulaField) {
            item.put("formulaConfig", new LinkedHashMap<>(formulaConfig));
        }
        String dictType = StringUtils.defaultIfBlank(text(pageSetting.get("dictType")), field.getDictType());
        if (StringUtils.isNotBlank(dictType)) {
            item.put("dictType", dictType);
        }
        if (pageSetting.containsKey("defaultValue")) {
            item.put("defaultValue", pageSetting.get("defaultValue"));
        } else if (field.getDefaultValue() != null) {
            item.put("defaultValue", field.getDefaultValue());
        }
        Object span = pageSetting.get("span");
        if (span != null) {
            item.put("span", span);
        }
        Object formItemStyle = pageSetting.get("formItemStyle");
        if (formItemStyle != null) {
            item.put("formItemStyle", formItemStyle);
        }
        Object gridStyle = pageSetting.get("gridStyle");
        if (gridStyle != null) {
            item.put("gridStyle", gridStyle);
        }
        Object labelWidth = pageSetting.get("labelWidth");
        if (labelWidth != null) {
            item.put("labelWidth", labelWidth);
        }
        copyRuntimeSetting(item, pageSetting, "componentStyle");
        copyRuntimeSetting(item, pageSetting, "componentClass");
        copyRuntimeSetting(item, pageSetting, "formItemClass");
        copyRuntimeSetting(item, pageSetting, "showFeedback");
        copyRuntimeSetting(item, pageSetting, "showLabel");

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("placeholder", buildPlaceholder(componentType, label));
        if (isSystemField(field) || readonly) {
            props.put("disabled", true);
            props.put("readonly", true);
        }
        if (field.getLength() != null && field.getLength() > 0 && isTextComponent(componentType)) {
            props.put("maxlength", field.getLength());
        }
        if (field.getPrecision() != null && field.getPrecision() >= 0 && "number".equals(componentType)) {
            props.put("precision", field.getPrecision());
        }
        props.putAll(sanitizeFieldBasicProps(field));
        Object designerProps = pageSetting.get("props");
        if (designerProps instanceof Map<?, ?> designerPropsMap) {
            Map<String, Object> sanitizedDesignerProps = new LinkedHashMap<>((Map<String, Object>) designerPropsMap);
            Map<String, Object> formCreateMeta = mapValue(sanitizedDesignerProps.get("__fc"));
            sanitizedDesignerProps.remove("__fc");
            sanitizedDesignerProps.remove("__fcType");
            sanitizedDesignerProps.remove("fieldBinding");
            props.putAll(sanitizedDesignerProps);
            applyFormCreateMeta(item, formCreateMeta, props);
        }
        copyRuntimePropsToField(item, props);
        applySelectionLabelProps(props, field.getField(), componentType);
        if (isSystemField(field) || readonly) {
            props.put("disabled", true);
            props.put("readonly", true);
        }
        item.put("props", props);
        if (lookupMeta != null) {
            item.put("relationLookup", buildRelationLookupConfig(lookupMeta));
            applyRelationLookupProps(item, lookupMeta, label);
        }

        if (required) {
            String message = StringUtils.defaultIfBlank(requiredMessage, buildPlaceholder(componentType, label));
            if (validationRules.stream().noneMatch(rule -> booleanWithDefault(rule.get("required"), false))) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("required", true);
                rule.put("message", message);
                rule.put("trigger", trigger == null ? List.of("blur", "change") : trigger);
                validationRules.add(0, rule);
            } else {
                validationRules.forEach(rule -> {
                    if (booleanWithDefault(rule.get("required"), false) && StringUtils.isBlank(text(rule.get("message")))) {
                        rule.put("message", message);
                    }
                });
            }
            item.put("requiredMessage", message);
        }
        if (!validationRules.isEmpty()) {
            item.put("rules", validationRules);
        }
        return item;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveRuntimeValidationRules(Map<String, Object> pageSetting) {
        Object source = pageSetting.get("rules");
        if (!(source instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> rules = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                rules.add(new LinkedHashMap<>((Map<String, Object>) map));
            }
        }
        return rules;
    }

    private String resolveRequiredRuleMessage(List<Map<String, Object>> validationRules) {
        return validationRules.stream()
                .filter(rule -> booleanWithDefault(rule.get("required"), false))
                .map(rule -> text(rule.get("message")))
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse("");
    }

    private Object resolveRequiredRuleTrigger(List<Map<String, Object>> validationRules) {
        return validationRules.stream()
                .filter(rule -> booleanWithDefault(rule.get("required"), false))
                .map(rule -> rule.get("trigger"))
                .filter(value -> value != null && StringUtils.isNotBlank(String.valueOf(value)))
                .findFirst()
                .orElse(null);
    }

    private void copyRuntimeSetting(Map<String, Object> item, Map<String, Object> pageSetting, String key) {
        if (pageSetting.containsKey(key)) {
            item.put(key, pageSetting.get(key));
        }
    }

    private void copyRuntimePropsToField(Map<String, Object> item, Map<String, Object> props) {
        List.of("placeholder", "clearable", "filterable", "multiple", "size", "maxlength", "showCount",
                        "rows", "autosize", "min", "max", "step", "precision", "showButton",
                        "checkedValue", "uncheckedValue", "checkedText", "uncheckedText", "format",
                        "valueFormat", "startPlaceholder", "endPlaceholder", "showFeedback", "showLabel")
                .forEach(key -> {
                    if (props.containsKey(key)) {
                        item.put(key, props.get(key));
                    }
                });
    }

    private void applyFormCreateMeta(Map<String, Object> item, Map<String, Object> formCreateMeta, Map<String, Object> props) {
        if (formCreateMeta == null || formCreateMeta.isEmpty()) {
            return;
        }
        Object style = firstPresent(formCreateMeta.get("style"), props.get("style"));
        if (style != null) {
            item.put("componentStyle", style);
        }
        Object componentClass = firstPresent(props.get("className"), props.get("class"));
        if (componentClass != null) {
            item.put("componentClass", componentClass);
        }
        Object formItemClass = firstPresent(formCreateMeta.get("className"), formCreateMeta.get("class"));
        if (formItemClass != null) {
            item.put("formItemClass", formItemClass);
        }
        Map<String, Object> wrap = mapValue(formCreateMeta.get("wrap"));
        if (wrap.get("style") != null) {
            item.put("formItemStyle", wrap.get("style"));
        }
        if (wrap.containsKey("labelWidth")) {
            item.put("labelWidth", wrap.get("labelWidth"));
        }
        if (wrap.containsKey("show") && !booleanWithDefault(wrap.get("show"), true)) {
            item.put("showLabel", false);
        }
    }

    private Object firstPresent(Object primary, Object fallback) {
        return primary != null ? primary : fallback;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> copyRuleList(Object source) {
        if (!(source instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add(new LinkedHashMap<>((Map<String, Object>) map));
            }
        }
        return result;
    }

    private boolean isRequiredSwitchEnabled(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)
                && !"false".equalsIgnoreCase(text) && !"0".equals(text)) {
            return true;
        }
        return booleanWithDefault(value, false);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> buildDefaultSort(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        LowcodePageZone tableZone = findZone(pageSchema, "table");
        Map<String, Object> props = new LinkedHashMap<>();
        if (tableZone != null && tableZone.getProps() != null) {
            props.putAll(tableZone.getProps());
        }
        props.putAll(resolveGridBlockProps(pageSchema, List.of("data-table", "AiCrudPage", "AiTable")));
        Object defaultSort = props.get("defaultSort");
        String sortField = text(props.get("defaultSortField"));
        String sortOrder = text(props.get("defaultSortOrder"));
        if (defaultSort instanceof Map<?, ?> defaultSortMap) {
            sortField = StringUtils.defaultIfBlank(sortField,
                    StringUtils.defaultIfBlank(text(defaultSortMap.get("orderByColumn")), text(defaultSortMap.get("field"))));
            sortOrder = StringUtils.defaultIfBlank(sortOrder,
                    StringUtils.defaultIfBlank(text(defaultSortMap.get("isAsc")), text(defaultSortMap.get("order"))));
        }

        Set<String> allowedFields = modelSchema == null || modelSchema.getFields() == null
                ? Set.of("id")
                : modelSchema.getFields().stream()
                .map(LowcodeFieldSchema::getField)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String orderByColumn = StringUtils.defaultIfBlank(sortField, "id");
        if (!allowedFields.contains(orderByColumn)) {
            orderByColumn = "id";
        }

        String isAsc = "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderByColumn", orderByColumn);
        result.put("isAsc", isAsc);
        return result;
    }

    private List<Map<String, Object>> buildRowActions(LowcodePageSchema pageSchema, boolean treeRuntime) {
        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(defaultAction("edit", "编辑", "primary"));
        actions.add(defaultAction("detail", "查看详情", "info"));
        if (treeRuntime) {
            actions.add(defaultAction("addChild", "添加下级", "success"));
        }
        actions.add(defaultAction("delete", "删除", "error"));
        List<Map<String, Object>> customActions = resolveCustomActions(pageSchema, "row");
        Set<String> existingKeys = actions.stream()
                .map(action -> text(action.get("key")))
                .collect(Collectors.toSet());
        customActions.stream()
                .filter(action -> !existingKeys.contains(text(action.get("key"))))
                .forEach(actions::add);
        return actions;
    }

    private Map<String, Object> defaultAction(String key, String label, String type) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("key", key);
        action.put("label", label);
        action.put("type", type);
        action.put("position", "row");
        return action;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveCustomActions(LowcodePageSchema pageSchema, String position) {
        LowcodePageZone tableZone = findZone(pageSchema, "table");
        Map<String, Object> props = new LinkedHashMap<>();
        if (tableZone != null && tableZone.getProps() != null) {
            props.putAll(tableZone.getProps());
        }
        props.putAll(resolveGridBlockProps(pageSchema, List.of("data-table", "AiCrudPage", "AiTable")));
        Object value = props.get("customActions");
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> source)) {
                continue;
            }
            String actionPosition = StringUtils.defaultIfBlank(text(source.get("position")), "toolbar");
            if (!position.equals(actionPosition)) {
                continue;
            }
            String key = StringUtils.defaultIfBlank(text(source.get("key")), "custom_" + result.size());
            String label = StringUtils.defaultIfBlank(text(source.get("label")), "自定义按钮");
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("key", key);
            action.put("label", label);
            action.put("type", StringUtils.defaultIfBlank(text(source.get("type")), "default"));
            action.put("position", position);
            action.put("actionType", StringUtils.defaultIfBlank(text(source.get("actionType")), "route"));
            putIfNotBlank(action, "routePath", text(source.get("routePath")));
            putIfNotBlank(action, "targetFormKey", text(source.get("targetFormKey")));
            putIfNotBlank(action, "openTarget", StringUtils.defaultIfBlank(text(source.get("openTarget")), "_self"));
            putIfNotBlank(action, "permissionCode", text(source.get("permissionCode")));
            putIfNotBlank(action, "confirmText", text(source.get("confirmText")));
            putIfNotBlank(action, "displayCondition", text(source.get("displayCondition")));
            putIfNotBlank(action, "successBehavior", text(source.get("successBehavior")));
            List<Map<String, Object>> params = resolveActionParams(source.get("params"));
            if (!params.isEmpty()) {
                action.put("params", params);
            }
            result.add(action);
        }
        return result;
    }

    private List<Map<String, Object>> resolveActionParams(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> source)) {
                continue;
            }
            String name = text(source.get("name"));
            if (StringUtils.isBlank(name)) {
                continue;
            }
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("name", name);
            putIfNotBlank(param, "sourceType", text(source.get("sourceType")));
            putIfNotBlank(param, "sourceField", text(source.get("sourceField")));
            param.put("value", StringUtils.defaultString(text(source.get("value"))));
            result.add(param);
        }
        return result;
    }

    private boolean isSystemField(LowcodeFieldSchema field) {
        return field != null && Boolean.TRUE.equals(field.getSystemField());
    }

    private List<LowcodeFieldSchema> resolveFields(LowcodeModelSchema modelSchema,
                                                   LowcodePageSchema pageSchema,
                                                   String zoneKey,
                                                   Predicate<LowcodeFieldSchema> fallbackPredicate) {
        Map<String, LowcodeFieldSchema> fieldMap = buildRuntimeFieldMap(modelSchema, pageSchema);
        LowcodePageZone zone = findZone(pageSchema, zoneKey);
        if (zone == null || Boolean.FALSE.equals(zone.getEnabled()) || zone.getFieldRefs() == null || zone.getFieldRefs().isEmpty()) {
            return fieldMap.values().stream()
                    .filter(this::isActiveField)
                    .filter(fallbackPredicate)
                    .toList();
        }

        Set<String> refs = new LinkedHashSet<>(zone.getFieldRefs());
        List<LowcodeFieldSchema> selectedFields = refs.stream()
                .map(fieldMap::get)
                .filter(field -> field != null)
                .filter(field -> isZoneFieldAllowed(field, zoneKey, fallbackPredicate))
                .toList();
        if (selectedFields.isEmpty()) {
            return fieldMap.values().stream()
                    .filter(this::isActiveField)
                    .filter(fallbackPredicate)
                    .toList();
        }
        return selectedFields;
    }

    private Map<String, LowcodeFieldSchema> buildRuntimeFieldMap(LowcodeModelSchema modelSchema,
                                                                  LowcodePageSchema pageSchema) {
        Map<String, LowcodeFieldSchema> fieldMap = modelSchema.getFields().stream()
                .collect(Collectors.toMap(
                        LowcodeFieldSchema::getField,
                        field -> field,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        if (pageSchema == null || pageSchema.getModelRefs() == null) {
            return fieldMap;
        }
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || ref.getFields() == null) {
                continue;
            }
            for (Map<String, Object> source : ref.getFields()) {
                LowcodeFieldSchema field = buildPageRefField(ref, source);
                if (field != null) {
                    fieldMap.putIfAbsent(field.getField(), field);
                }
            }
        }
        return fieldMap;
    }

    private LowcodeFieldSchema buildPageRefField(LowcodePageModelRef ref, Map<String, Object> source) {
        if (source == null) {
            return null;
        }
        String sourceField = StringUtils.defaultIfBlank(text(source.get("sourceField")), text(source.get("field")));
        if (StringUtils.isBlank(sourceField)) {
            return null;
        }
        boolean primary = Boolean.TRUE.equals(ref.getPrimary());
        String fieldRef = StringUtils.defaultIfBlank(text(source.get("fieldRef")),
                primary ? sourceField : safeKey(ref.getModelCode()) + "__" + sourceField);
        if (StringUtils.isBlank(fieldRef)) {
            return null;
        }

        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField(fieldRef);
        field.setColumnName(StringUtils.defaultIfBlank(text(source.get("columnName")), sourceField));
        String rawLabel = StringUtils.defaultIfBlank(text(source.get("rawLabel")),
                StringUtils.defaultIfBlank(text(source.get("label")), sourceField));
        field.setLabel(rawLabel);
        field.setDataType(StringUtils.defaultIfBlank(text(source.get("dataType")), "varchar"));
        field.setLength(integerValue(source.get("length")));
        field.setPrecision(integerValue(source.get("precision")));
        field.setRequired(Boolean.TRUE.equals(booleanValue(source.get("required"))));
        field.setDefaultValue(source.get("defaultValue"));
        field.setSearchable(Boolean.TRUE.equals(booleanValue(source.get("searchable"))));
        field.setListVisible(booleanValue(source.get("listVisible")) == null || Boolean.TRUE.equals(booleanValue(source.get("listVisible"))));
        field.setFormVisible(booleanValue(source.get("formVisible")) == null || Boolean.TRUE.equals(booleanValue(source.get("formVisible"))));
        field.setComponentType(StringUtils.defaultIfBlank(text(source.get("componentType")), "input"));
        field.setQueryType(StringUtils.defaultIfBlank(text(source.get("queryType")), "eq"));
        field.setDictType(text(source.get("dictType")));
        field.setSensitiveType(StringUtils.defaultIfBlank(text(source.get("sensitiveType")), "NONE"));
        field.setEncryptAlgorithm(text(source.get("encryptAlgorithm")));
        field.setSortable(Boolean.TRUE.equals(booleanValue(source.get("sortable"))));
        field.setPrimaryKey(Boolean.TRUE.equals(booleanValue(source.get("primaryKey"))));
        field.setSystemField(Boolean.TRUE.equals(booleanValue(source.get("systemField"))));
        field.setReadonly(Boolean.TRUE.equals(booleanValue(source.get("readonly"))));
        field.setFieldStatus(StringUtils.defaultIfBlank(text(source.get("fieldStatus")), "ENABLED"));
        field.setAutoIncrement(Boolean.TRUE.equals(booleanValue(source.get("autoIncrement"))));
        field.setWidth(integerValue(source.get("width")));
        field.setRemark(text(source.get("remark")));
        Object basicProps = source.get("basicProps");
        if (basicProps instanceof Map<?, ?> basicPropsMap) {
            Map<String, Object> props = new LinkedHashMap<>();
            basicPropsMap.forEach((key, value) -> {
                if (key != null) {
                    props.put(String.valueOf(key), value);
                }
            });
            field.setBasicProps(props);
        }
        return field;
    }

    private Boolean booleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private boolean booleanWithDefault(Object value, boolean defaultValue) {
        Boolean bool = booleanValue(value);
        return bool == null ? defaultValue : bool;
    }

    private boolean isZoneFieldAllowed(LowcodeFieldSchema field,
                                       String zoneKey,
                                       Predicate<LowcodeFieldSchema> fallbackPredicate) {
        if (!isActiveField(field)) {
            return false;
        }
        if ("search".equals(zoneKey)) {
            return !isSystemField(field);
        }
        if ("edit".equals(zoneKey)) {
            return !isSystemField(field)
                    && !Boolean.TRUE.equals(field.getReadonly())
                    && (field.getFormVisible() == null || Boolean.TRUE.equals(field.getFormVisible()));
        }
        return fallbackPredicate.test(field);
    }

    private boolean isActiveField(LowcodeFieldSchema field) {
        String status = StringUtils.defaultString(field == null ? null : field.getFieldStatus());
        return !"DISABLED".equalsIgnoreCase(status) && !"HIDDEN".equalsIgnoreCase(status);
    }

    private Integer integerValue(Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int intValue(Object value, int defaultValue) {
        Integer parsed = integerValue(value);
        return parsed == null ? defaultValue : parsed;
    }

    private String safeKey(String value) {
        String key = StringUtils.defaultIfBlank(value, "model").replaceAll("[^A-Za-z0-9_]", "_");
        return StringUtils.defaultIfBlank(key, "model");
    }

    private String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private String snakeToCamel(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }

    private LowcodePageZone findZone(LowcodePageSchema pageSchema, String zoneKey) {
        if (pageSchema == null || pageSchema.getZones() == null) {
            return null;
        }
        return pageSchema.getZones().stream()
                .filter(zone -> zoneKey.equals(zone.getZoneKey()))
                .findFirst()
                .orElse(null);
    }

    private void copyOption(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private String resolveSearchComponentType(LowcodeFieldSchema field, String queryType) {
        return resolveSearchComponentType(field, queryType, Map.of());
    }

    private String resolveSearchComponentType(LowcodeFieldSchema field, String queryType, Map<String, Object> pageSetting) {
        String configuredType = StringUtils.defaultIfBlank(text(pageSetting.get("componentType")), text(pageSetting.get("type")));
        if (StringUtils.isNotBlank(configuredType)) {
            return normalizeEditComponentType(configuredType);
        }
        String componentType = StringUtils.defaultIfBlank(field.getComponentType(), field.getDataType());
        componentType = StringUtils.defaultIfBlank(componentType, "input");
        if (isBusinessSelectComponent(componentType)) {
            return componentType;
        }
        if (StringUtils.isNotBlank(field.getDictType())) {
            return "select";
        }
        if ("between".equals(queryType)) {
            if ("datetime".equals(componentType)) {
                return "datetimerange";
            }
            if ("date".equals(componentType)) {
                return "daterange";
            }
            if ("time".equals(componentType)) {
                return "timerange";
            }
        }
        if ("number".equals(componentType)) {
            return "number";
        }
        if ("date".equals(componentType)) {
            return "date";
        }
        if ("datetime".equals(componentType)) {
            return "datetime";
        }
        if ("time".equals(componentType)) {
            return "time";
        }
        if ("treeSelect".equals(componentType)) {
            return "treeSelect";
        }
        if ("cascader".equals(componentType)) {
            return "cascader";
        }
        return "input";
    }

    private String resolveEditComponentType(LowcodeFieldSchema field) {
        return normalizeEditComponentType(StringUtils.defaultIfBlank(field.getComponentType(), "input"));
    }

    private String resolveEditComponentType(LowcodeFieldSchema field, Map<String, Object> pageSetting) {
        String componentType = StringUtils.defaultIfBlank(text(pageSetting.get("componentType")), text(pageSetting.get("type")));
        componentType = StringUtils.defaultIfBlank(componentType, field.getComponentType());
        return normalizeEditComponentType(StringUtils.defaultIfBlank(componentType, "input"));
    }

    private String normalizeEditComponentType(String componentType) {
        return switch (StringUtils.defaultString(componentType)) {
            case "inputNumber" -> "number";
            case "orgSelect", "organizationSelect", "departmentSelect", "deptSelect",
                    "departmentTreeSelect", "deptTreeSelect", "elTreeSelect", "orgName", "deptName",
                    "forgeOrgTreeSelect" -> "orgTreeSelect";
            case "userPicker", "user", "userName", "sysUserSelect", "forgeUserSelect" -> "userSelect";
            default -> componentType;
        };
    }

    private String normalizeRuntimeFormSize(String value) {
        String size = StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
        if ("default".equals(size) || "medium".equals(size)) {
            return "medium";
        }
        return Set.of("small", "large").contains(size) ? size : "medium";
    }

    private void applySelectionLabelProps(Map<String, Object> props, String fieldName, String componentType) {
        if (StringUtils.isBlank(fieldName)
                || (!"orgTreeSelect".equals(componentType) && !"userSelect".equals(componentType))) {
            return;
        }
        if (StringUtils.isBlank(text(props.get("labelValueField")))) {
            props.put("labelValueField", fieldName + "Name");
        }
        if (StringUtils.isBlank(text(props.get("targetField")))) {
            props.put("targetField", fieldName + "Name");
        }
    }

    private boolean isBusinessSelectComponent(String componentType) {
        return "dictSelect".equals(componentType)
                || "treeSelect".equals(componentType)
                || "orgTreeSelect".equals(componentType)
                || "userSelect".equals(componentType)
                || "regionTreeSelect".equals(componentType)
                || "cascader".equals(componentType)
                || "objectReference".equals(componentType);
    }

    private String buildPlaceholder(String componentType, String label) {
        if ("select".equals(componentType) || "radio".equals(componentType) || "checkbox".equals(componentType)
                || "date".equals(componentType) || "datetime".equals(componentType) || "time".equals(componentType)
                || "daterange".equals(componentType) || "datetimerange".equals(componentType) || "timerange".equals(componentType)
                || "dictSelect".equals(componentType) || "treeSelect".equals(componentType) || "orgTreeSelect".equals(componentType)
                || "userSelect".equals(componentType) || "regionTreeSelect".equals(componentType) || "cascader".equals(componentType)
                || "objectReference".equals(componentType) || "fileUpload".equals(componentType)
                || "imageUpload".equals(componentType) || "upload".equals(componentType)) {
            return "请选择" + label;
        }
        return "请输入" + label;
    }

    private boolean isTextComponent(String componentType) {
        return "input".equals(componentType) || "textarea".equals(componentType);
    }
}

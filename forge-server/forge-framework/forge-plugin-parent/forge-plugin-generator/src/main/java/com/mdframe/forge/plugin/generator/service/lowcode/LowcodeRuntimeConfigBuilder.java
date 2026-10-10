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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimePageRelationResolver.findRelationFromPrimary;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimePageRelationResolver.resolveChildRelationField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimePageRelationResolver.resolveMainRelationField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimePageRelationResolver.resolvePrimaryRef;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimePageRelationResolver.resolveRuntimeRelation;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponentResolver.isBusinessSelectComponent;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponentResolver.normalizeEditComponentType;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponentResolver.normalizeRuntimeFormSize;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponentResolver.resolveSearchComponentType;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerLayoutReader.extractCanvasItems;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerLayoutReader.extractFormRules;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerLayoutReader.findZone;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerLayoutReader.normalizeAlign;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerLayoutReader.resolveGridBlockProps;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerLayoutReader.resolveGridFieldSetting;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerLayoutReader.runtimeSettingBlockTypes;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerLayoutReader.text;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFormRuleSettingResolver.booleanWithDefault;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFormRuleSettingResolver.firstPresent;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFormRuleSettingResolver.integerValue;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFormRuleSettingResolver.intValue;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFormRuleSettingResolver.resolveFormRuleSetting;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldPresentationSupport.applyAlignment;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldPresentationSupport.isSystemField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldPresentationSupport.putIfNotBlank;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldPresentationSupport.sanitizeFieldBasicProps;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeEditFieldCompiler.buildEditField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeEditFieldCompiler.mapValue;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimePageRefFieldFactory.safeKey;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalogResolver.buildChildFieldRefs;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalogResolver.buildRuntimeFieldMap;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalogResolver.isActiveField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalogResolver.isManagedBusinessFlowStatusField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeTreeConfigBuilder.buildTreeConfig;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeTreeConfigBuilder.buildTreeOptionSource;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeTreeConfigBuilder.extractTreeConfigOverrides;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeTreeConfigBuilder.hasTreePanelBlock;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeTreeConfigBuilder.isLeftTreeRightTableLayout;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeTreeConfigBuilder.resolveTreeApiConfigKey;

/**
 * 将低代码业务协议转换为 AiCrudPage 运行时配置。
 */
@Service
@RequiredArgsConstructor
public class LowcodeRuntimeConfigBuilder {

    private static final Logger log = LoggerFactory.getLogger(LowcodeRuntimeConfigBuilder.class);
    private static final String MASTER_DETAIL_LAYOUT = "master-detail-crud";
    private final ObjectMapper objectMapper;
    private final LowcodeSchemaValidator schemaValidator;
    private final LowcodePolicyService policyService;
    private final RuntimeFieldMetadataCompiler fieldMetadataCompiler = new RuntimeFieldMetadataCompiler();
    private final RuntimeTableColumnCompiler tableColumnCompiler = new RuntimeTableColumnCompiler(fieldMetadataCompiler);

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
        runtimeConfig.setObjectCode(StringUtils.firstNonBlank(
                modelSchema.getObject() == null ? null : modelSchema.getObject().getCode(),
                configKey,
                modelSchema.getTableName()));
        runtimeConfig.setTableName(modelSchema.getTableName());
        runtimeConfig.setTableComment(modelSchema.getBusinessName());
        String layoutType = StringUtils.defaultIfBlank(pageSchema.getLayoutType(), "simple-crud");
        if (hasTreePanelBlock(pageSchema)) {
            layoutType = "tree-crud";
        }
        runtimeConfig.setLayoutType(layoutType);

        try {
            runtimeConfig.setSearchSchema(objectMapper.writeValueAsString(buildSearchSchema(configKey, modelSchema, pageSchema)));
            runtimeConfig.setColumnsSchema(objectMapper.writeValueAsString(buildColumnsSchema(modelSchema, pageSchema)));
            runtimeConfig.setEditSchema(objectMapper.writeValueAsString(buildEditSchema(configKey, modelSchema, pageSchema)));
            runtimeConfig.setApiConfig(objectMapper.writeValueAsString(buildApiConfig(configKey, modelSchema, pageSchema)));
            runtimeConfig.setOptions(objectMapper.writeValueAsString(buildOptions(modelSchema, pageSchema)));
            runtimeConfig.setDictConfig(objectMapper.writeValueAsString(fieldMetadataCompiler.dictConfig(modelSchema)));
            runtimeConfig.setDesensitizeConfig(objectMapper.writeValueAsString(fieldMetadataCompiler.desensitizeConfig(modelSchema)));
            runtimeConfig.setEncryptConfig(objectMapper.writeValueAsString(fieldMetadataCompiler.encryptConfig(modelSchema)));
            runtimeConfig.setTransConfig(objectMapper.writeValueAsString(buildTransConfig(modelSchema, pageSchema)));
            return runtimeConfig;
        } catch (Exception e) {
            throw new BusinessException("低代码运行时配置生成失败: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> buildSearchSchema(String configKey, LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        List<Map<String, Object>> fields = resolveFields(modelSchema, pageSchema, "search", field -> Boolean.TRUE.equals(field.getSearchable()))
                .stream()
                .map(field -> buildSearchField(field, resolveRuntimeFieldSetting(pageSchema, "search", field.getField()),
                        modelSchema, pageSchema, configKey))
                .collect(Collectors.toCollection(ArrayList::new));
        appendTreeRuntimeField(fields, modelSchema, pageSchema, "search");
        RuntimeTreeFieldDecorator.decorate(fields, configKey, modelSchema, pageSchema,
                isTreeRuntime(modelSchema, pageSchema), isEmbeddedTreeTableRuntime(modelSchema, pageSchema));
        return fields;
    }

    private List<Map<String, Object>> buildColumnsSchema(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        List<Map<String, Object>> columns = resolveFields(modelSchema, pageSchema, "table",
                field -> field.getListVisible() == null || Boolean.TRUE.equals(field.getListVisible()))
                .stream()
                .map(field -> tableColumnCompiler.buildTableColumn(field,
                        resolveTableColumnSetting(pageSchema, field.getField()),
                        modelSchema, pageSchema))
                .collect(Collectors.toCollection(ArrayList::new));

        Map<String, Object> actions = new LinkedHashMap<>();
        actions.put("key", "actions");
        actions.put("title", "操作");
        actions.put("dataIndex", "actions");
        List<Map<String, Object>> rowActions = RuntimeActionCompiler.rowActions(
                resolveTableProps(pageSchema), isEmbeddedTreeTableRuntime(modelSchema, pageSchema));
        actions.put("width", Math.max(180, rowActions.size() * 58));
        actions.put("fixed", "right");
        actions.put("actions", rowActions);
        actions.put("maxActionButtons", 3);
        columns.add(actions);
        return columns;
    }

    private List<Map<String, Object>> buildEditSchema(String configKey, LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Set<String> childFieldRefs = buildChildFieldRefs(pageSchema);
        List<LowcodeFieldSchema> orderedFields = sortByCanvasOrder(
                resolveFields(modelSchema, pageSchema, "edit",
                        field -> isEditFieldVisibleAtDesignTime(pageSchema, field)),
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
        RuntimeTreeFieldDecorator.decorate(fields, configKey, modelSchema, pageSchema,
                isTreeRuntime(modelSchema, pageSchema), isEmbeddedTreeTableRuntime(modelSchema, pageSchema));
        return fields;
    }

    private Map<String, String> buildApiConfig(String configKey, LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Map<String, String> apiConfig = new LinkedHashMap<>();
        apiConfig.put("list", "get@/ai/crud/" + configKey + "/page");
        if (isTreeRuntime(modelSchema, pageSchema)) {
            apiConfig.put("tree", "get@/ai/crud/" + resolveTreeApiConfigKey(configKey, pageSchema) + "/tree");
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
        String formOpenMode = RuntimeFormContainerOptionsCompiler.formOpenMode(firstNonBlank(
                editProps.get("formOpenMode"),
                crudBlockProps.get("formOpenMode"),
                editProps.get("modalType"),
                crudBlockProps.get("modalType")));
        options.put("formOpenMode", formOpenMode);
        options.put("modalType", RuntimeFormContainerOptionsCompiler.modalType(firstNonBlank(
                editProps.get("modalType"),
                crudBlockProps.get("modalType"),
                formOpenMode)));
        options.put("tabWorkspace", RuntimeFormContainerOptionsCompiler.tabWorkspaceOptions(editProps, crudBlockProps));
        int editGridCols = resolveEditGridCols(pageSchema);
        options.put("modalWidth", RuntimeFormContainerOptionsCompiler.runtimeModalWidth(editProps, crudBlockProps,
                RuntimeFormContainerOptionsCompiler.defaultModalWidth(masterDetailRuntime, editGridCols)));
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
        options.put("editEnableCollapse", booleanWithDefault(editProps.get("enableCollapse"), false));
        Integer editMaxVisibleFields = integerValue(editProps.get("maxVisibleFields"));
        if (editMaxVisibleFields != null && editMaxVisibleFields > 0) {
            options.put("editMaxVisibleFields", editMaxVisibleFields);
        }
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

        Map<String, Object> tableProps = resolveTableProps(pageSchema);
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
            copyOption(tableProps, options, "drawerPlacement");
            // 左树右表右表是平铺列表：强制关闭「添加下级」，忽略表区/区块残留配置
            if (isLeftTreeRightTableLayout(pageSchema)) {
                options.put("enableTreeAddChild", Boolean.FALSE);
            } else {
                copyOption(tableProps, options, "enableTreeAddChild");
            }
            // 表单设计器布局里配置的抽屉方向优先于列表/表格区设置
            copyOption(editProps, options, "drawerPlacement");
            copyOption(tableProps, options, "tabWorkspace");
            options.put("tableRowGap", intValue(tableProps.get("rowGap"), 8));
        }
        formOpenMode = RuntimeFormContainerOptionsCompiler.formOpenMode(firstNonBlank(
                editProps.get("formOpenMode"),
                crudBlockProps.get("formOpenMode"),
                tableProps.get("formOpenMode"),
                editProps.get("modalType"),
                crudBlockProps.get("modalType"),
                tableProps.get("modalType"),
                options.get("formOpenMode")));
        options.put("formOpenMode", formOpenMode);
        options.put("modalType", RuntimeFormContainerOptionsCompiler.modalType(firstNonBlank(
                Set.of("modal", "drawer").contains(formOpenMode) ? formOpenMode : null,
                editProps.get("modalType"),
                crudBlockProps.get("modalType"),
                tableProps.get("modalType"),
                options.get("modalType"))));
        Set<String> toolbarActions = resolveToolbarStandardActions(pageSchema);
        if (!toolbarActions.isEmpty()) {
            options.put("hideAdd", !toolbarActions.contains("add"));
            options.put("showImport", toolbarActions.contains("import"));
            options.put("showExport", toolbarActions.contains("export"));
            options.put("hideBatchDelete", !toolbarActions.contains("batch-delete"));
            options.put("enableCustomQuery", toolbarActions.contains("custom-query"));
        }
        options.put("toolbarActions", RuntimeActionCompiler.customActions(tableProps, "toolbar"));
        options.put("rowActions", RuntimeActionCompiler.customActions(tableProps, "row"));
        options.put("detailActions", RuntimeActionCompiler.customActions(tableProps, "detail"));
        options.put("formActions", RuntimeActionCompiler.customActions(tableProps, "form"));
        options.put("defaultSort", buildDefaultSort(modelSchema, pageSchema));
        options.put("childListDisplayMode", normalizeChildListDisplayMode(tableProps.get("childListDisplayMode")));
        options.put("joinConfig", buildJoinConfig(modelSchema, pageSchema));
        if (masterDetailRuntime) {
            options.put("masterDetailConfig", RuntimeChildTableCompiler.buildMasterDetailConfig(
                    modelSchema, pageSchema,
                    (ref, selectedRefs, childFk) -> RuntimeChildFieldCompiler.compile(
                            ref, selectedRefs, childFk, RuntimeEditFieldCompiler::buildEditField)));
        }
        LowcodePageZone detailZone = findZone(pageSchema, "detail");
        Map<String, Object> detailProps = detailZone == null || detailZone.getProps() == null ? Map.of() : detailZone.getProps();
        Object quantityPanels = detailProps.get("quantityPanels");
        if (quantityPanels instanceof List<?> panels && !panels.isEmpty()) {
            options.put("detailPanels", panels);
        }
        if (Boolean.TRUE.equals(detailProps.get("showDataChangeLog"))
                || readDesignerLayoutFlag(formDesignerSchema, "showDataChangeLog")) {
            options.put("showDataChangeLog", true);
        }
        if (isTreeRuntime(modelSchema, pageSchema)) {
            options.put("treeConfig", buildTreeConfig(modelSchema, pageSchema, extractTreeConfigOverrides(pageSchema)));
            // 嵌入式树表全量拉取，分页必须关掉（区块默认 true 不能盖掉）
            if (isEmbeddedTreeTableRuntime(modelSchema, pageSchema)) {
                options.put("showPagination", Boolean.FALSE);
            }
        }
        return options;
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
                    .map(RuntimeDesignerLayoutReader::text)
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
                String displaySourceField = RuntimeRelationLookupCompiler.resolveDisplayField(ref, primaryRelation);
                String relationSourceField = RuntimeRelationLookupCompiler.normalizePrimaryFieldName(
                        modelSchema, primaryRelation.getSourceField());
                putIfNotBlank(item, "displayField", displaySourceField);
                putIfNotBlank(item, "displayAlias", RuntimeRelationLookupCompiler.displayAlias(relationSourceField));
            }
            result.add(item);
        }
        return result;
    }




    private boolean isTreeRuntime(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        if (modelSchema != null) {
            String appType = StringUtils.defaultIfBlank(modelSchema.getAppType(), "SINGLE").toUpperCase(Locale.ROOT);
            if ("TREE".equals(appType)
                    || (modelSchema.getTreeConfig() != null && Boolean.TRUE.equals(modelSchema.getTreeConfig().getEnabled()))) {
                return true;
            }
        }
        // 仅布局 / 真实 tree-panel 算树运行时。
        // table zone 残留的空 treeConfig（无 source、未 enabled）不能再挂 /tree，
        // 否则普通列表预览会报「树形父级字段不存在: parentId」。
        return (pageSchema != null && "tree-crud".equals(pageSchema.getLayoutType()))
                || hasTreePanelBlock(pageSchema)
                || isEnabledTreeOverride(extractTreeConfigOverrides(pageSchema));
    }

    private boolean isEnabledTreeOverride(Object overrides) {
        if (!(overrides instanceof Map<?, ?> map) || map.isEmpty()) {
            return false;
        }
        Object enabled = map.get("enabled");
        if (Boolean.TRUE.equals(enabled) || Integer.valueOf(1).equals(enabled)
                || "true".equalsIgnoreCase(String.valueOf(enabled)) || "1".equals(String.valueOf(enabled))) {
            return true;
        }
        // 外部树源（左树绑分类对象）也算有效树配置
        return StringUtils.isNotBlank(text(map.get("sourceConfigKey")))
                || StringUtils.isNotBlank(text(map.get("sourceModelCode")));
    }

    private boolean isMasterDetailRuntime(LowcodePageSchema pageSchema) {
        return pageSchema != null && MASTER_DETAIL_LAYOUT.equals(pageSchema.getLayoutType());
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
        Map<String, Object> runtimeField = "edit".equals(zoneKey)
                ? buildEditField(fieldSchema, Map.of(), modelSchema, pageSchema)
                // 隐藏筛选项不传 runtimeConfigKey，避免外部树源误挂本表 defaultSort
                : buildSearchField(fieldSchema, Map.of(), modelSchema, pageSchema, null);
        if (!"edit".equals(zoneKey)) {
            runtimeField.put("hidden", true);
        }
        runtimeField.put("queryType", "eq");
        runtimeField.put("required", false);
        fields.add(runtimeField);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
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

    private Map<String, Object> buildTransConfig(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Map<String, Object> result = fieldMetadataCompiler.translationConfig(modelSchema);
        RuntimeRelationLookupCompiler.appendDisplayTranslations(result, modelSchema, pageSchema);
        return result;
    }

    private Map<String, Object> buildSearchField(LowcodeFieldSchema field) {
        return buildSearchField(field, Map.of(), null, null, null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSearchField(LowcodeFieldSchema field,
                                                 Map<String, Object> pageSetting,
                                                 LowcodeModelSchema modelSchema,
                                                 LowcodePageSchema pageSchema) {
        return buildSearchField(field, pageSetting, modelSchema, pageSchema, null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSearchField(LowcodeFieldSchema field,
                                                 Map<String, Object> pageSetting,
                                                 LowcodeModelSchema modelSchema,
                                                 LowcodePageSchema pageSchema,
                                                 String runtimeConfigKey) {
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
        RuntimeRelationLookupCompiler.RelationLookupMeta lookupMeta = RuntimeRelationLookupCompiler.resolve(
                modelSchema, pageSchema, field.getField());
        if (lookupMeta != null) {
            item.put("type", "select");
            item.put("queryType", "eq");
            item.put("relationLookup", RuntimeRelationLookupCompiler.buildConfig(lookupMeta));
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
        // 查询区选项源与表单字段保持一致：优先用表单设计器 props / 模型 basicProps
        if (!RuntimeTreeFieldDecorator.hasEffectiveOptionSource(props.get("optionSource"))
                && !RuntimeTreeFieldDecorator.hasEffectiveOptionSource(item.get("optionSource"))) {
            Map<String, Object> editSetting = resolveEditFieldSetting(pageSchema, field.getField());
            Object editPropsValue = editSetting.get("props");
            if (editPropsValue instanceof Map<?, ?> editProps) {
                Object editOptionSource = editProps.get("optionSource");
                if (RuntimeTreeFieldDecorator.hasEffectiveOptionSource(editOptionSource)) {
                    props.put("optionSource", editOptionSource);
                    item.put("optionSource", editOptionSource);
                }
            }
        }
        if (RuntimeTreeFieldDecorator.hasEffectiveOptionSource(props.get("optionSource"))
                && !RuntimeTreeFieldDecorator.hasEffectiveOptionSource(item.get("optionSource"))) {
            item.put("optionSource", props.get("optionSource"));
        }
        if (!props.isEmpty()) {
            item.put("props", props);
        }
        if (lookupMeta != null) {
            RuntimeRelationLookupCompiler.applyProps(
                    item, lookupMeta, StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        }
        // 查询区树形默认支持本级+子集；选项源优先对齐左树（同一 tree API / 同一排序）
        if ("treeSelect".equals(text(item.get("type"))) || "orgTreeSelect".equals(text(item.get("type")))) {
            String searchTreeConfigKey = StringUtils.defaultIfBlank(
                    configKeyForSearchTree(pageSchema, modelSchema),
                    runtimeConfigKey);
            applySearchTreeSelectDefaults(item, props, modelSchema, pageSchema, searchTreeConfigKey);
        }
        return item;
    }

    private String configKeyForSearchTree(LowcodePageSchema pageSchema, LowcodeModelSchema modelSchema) {
        Object overrides = extractTreeConfigOverrides(pageSchema);
        if (overrides instanceof Map<?, ?> map) {
            String sourceConfigKey = text(map.get("sourceConfigKey"));
            if (StringUtils.isNotBlank(sourceConfigKey)) {
                return sourceConfigKey;
            }
        }
        if (modelSchema != null && modelSchema.getTreeConfig() != null
                && StringUtils.isNotBlank(modelSchema.getTreeConfig().getSourceConfigKey())) {
            return modelSchema.getTreeConfig().getSourceConfigKey();
        }
        return null;
    }

    private void applySearchTreeSelectDefaults(Map<String, Object> item,
                                               Map<String, Object> props,
                                               LowcodeModelSchema modelSchema,
                                               LowcodePageSchema pageSchema,
                                               String leftTreeConfigKey) {
        if (item == null) {
            return;
        }
        Map<String, Object> nextProps = props == null ? new LinkedHashMap<>() : props;
        // 与左树一致：默认本级+下级；仅显式 false 时关闭
        if (!nextProps.containsKey("includeChildren")) {
            nextProps.put("includeChildren", Boolean.TRUE);
        }
        item.put("includeChildren", nextProps.get("includeChildren"));
        String fieldName = text(item.get("field"));
        Map<String, Object> treeConfig = buildTreeConfig(modelSchema, pageSchema, extractTreeConfigOverrides(pageSchema));
        String filterField = firstNonBlank(text(treeConfig.get("filterField")), text(treeConfig.get("parentField")));
        boolean alignWithLeftTree = StringUtils.isNotBlank(leftTreeConfigKey)
                && StringUtils.isNotBlank(filterField)
                && filterField.equals(fieldName);
        // 与左树筛选字段相同时，查询树强制共用左树 tree API（节点序用树接口默认序，不用列表 defaultSort）
        if (alignWithLeftTree) {
            Map<String, Object> optionSource = buildTreeOptionSource(leftTreeConfigKey, treeConfig, Map.of());
            nextProps.put("optionSource", optionSource);
            item.put("optionSource", optionSource);
        }
        if (!nextProps.isEmpty()) {
            item.put("props", nextProps);
        }
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

    /**
     * 列表列设置：以 table zone 为主，并从 edit 区补齐静态 options（选项只配在表单设计里）。
     */
    private Map<String, Object> resolveTableColumnSetting(LowcodePageSchema pageSchema, String fieldName) {
        Map<String, Object> result = new LinkedHashMap<>(resolveRuntimeFieldSetting(pageSchema, "table", fieldName));
        Map<String, Object> editSetting = resolveEditFieldSetting(pageSchema, fieldName);
        Object editPropsValue = editSetting.get("props");
        if (editPropsValue instanceof Map<?, ?> editProps) {
            Object options = editProps.get("options");
            if (options instanceof List<?> list && !list.isEmpty()) {
                Map<String, Object> props = mapValue(result.get("props"));
                if (!(props.get("options") instanceof List<?> existing) || existing.isEmpty()) {
                    props.put("options", options);
                }
                Object optionSource = editProps.get("optionSource");
                if (optionSource != null && props.get("optionSource") == null) {
                    props.put("optionSource", optionSource);
                }
                result.put("props", props);
            }
        }
        return result;
    }

    private Map<String, Object> resolveEditFieldSetting(LowcodePageSchema pageSchema, String fieldName) {
        Map<String, Object> setting = new LinkedHashMap<>(resolveFieldSetting(pageSchema, "edit", fieldName));
        Map<String, Object> designerSetting = resolveFormRuleSetting(
                pageSchema, fieldName, () -> resolveEditGridCols(pageSchema));
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

    private Object firstNonBlank(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (StringUtils.isNotBlank(text(value))) {
                return value;
            }
        }
        return null;
    }

    private String normalizeChildListDisplayMode(Object value) {
        return "expand".equalsIgnoreCase(text(value)) ? "expand" : "aggregate";
    }

    private Map<String, Object> buildDefaultSort(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Map<String, Object> props = resolveTableProps(pageSchema);
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

    private Map<String, Object> resolveTableProps(LowcodePageSchema pageSchema) {
        LowcodePageZone tableZone = findZone(pageSchema, "table");
        Map<String, Object> props = new LinkedHashMap<>();
        if (tableZone != null && tableZone.getProps() != null) {
            props.putAll(tableZone.getProps());
        }
        props.putAll(resolveGridBlockProps(pageSchema, List.of("data-table", "AiCrudPage", "AiTable")));
        return props;
    }

    private List<LowcodeFieldSchema> resolveFields(LowcodeModelSchema modelSchema,
                                                   LowcodePageSchema pageSchema,
                                                   String zoneKey,
                                                   Predicate<LowcodeFieldSchema> fallbackPredicate) {
        return RuntimeFieldCatalogResolver.resolveFields(modelSchema, pageSchema, zoneKey,
                fallbackPredicate, this::isTableFieldExplicitlyHidden);
    }

    /**
     * 已发布快照早于流程托管字段时，按发布同一规则生成该字段的列表列；
     * 非托管字段、已停用、列表不可见或在列表设计里显式隐藏时返回 null。
     */
    public Map<String, Object> buildManagedFlowStatusColumn(LowcodeModelSchema modelSchema,
                                                            LowcodePageSchema pageSchema,
                                                            LowcodeFieldSchema field) {
        if (!isManagedBusinessFlowStatusField(field) || !isActiveField(field)
                || (field.getListVisible() != null && !Boolean.TRUE.equals(field.getListVisible()))
                || isTableFieldExplicitlyHidden(pageSchema, field.getField())) {
            return null;
        }
        return tableColumnCompiler.buildTableColumn(field, resolveRuntimeFieldSetting(pageSchema, "table", field.getField()),
                modelSchema, pageSchema);
    }

    private boolean isTableFieldExplicitlyHidden(LowcodePageSchema pageSchema, String fieldCode) {
        if (pageSchema == null || StringUtils.isBlank(fieldCode)) {
            return false;
        }
        Map<String, Object> setting = resolveRuntimeFieldSetting(pageSchema, "table", fieldCode);
        return setting != null && Boolean.FALSE.equals(setting.get("visible"));
    }

    private boolean readDesignerLayoutFlag(Object formDesignerSchema, String key) {
        Object schema = formDesignerSchema;
        if (schema instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                schema = objectMapper.readValue(text, Map.class);
            } catch (Exception ignored) {
                return false;
            }
        }
        if (!(schema instanceof Map<?, ?> schemaMap)) {
            return false;
        }
        Object layout = schemaMap.get("layout");
        if (!(layout instanceof Map<?, ?> layoutMap)) {
            return false;
        }
        return Boolean.TRUE.equals(layoutMap.get(key));
    }

    private boolean isEditFieldVisibleAtDesignTime(LowcodePageSchema pageSchema,
                                                   LowcodeFieldSchema field) {
        if (field == null || Boolean.TRUE.equals(field.getSystemField())
                || Boolean.TRUE.equals(field.getReadonly())) {
            return false;
        }
        if (field.getFormVisible() == null || Boolean.TRUE.equals(field.getFormVisible())) {
            return true;
        }
        Map<String, Object> setting = resolveEditFieldSetting(pageSchema, field.getField());
        return containsVisibilityRuntimeRules(setting.get("runtimeRules"))
                || containsVisibilityRuntimeRules(mapValue(setting.get("props")).get("runtimeRules"));
    }

    private boolean containsVisibilityRuntimeRules(Object value) {
        if (!(value instanceof List<?> rules)) {
            return false;
        }
        return rules.stream()
                .filter(item -> item instanceof Map<?, ?>)
                .map(item -> (Map<?, ?>) item)
                .anyMatch(rule -> {
                    Object effectValue = rule.get("effect");
                    if (effectValue instanceof Map<?, ?> effect) {
                        return effect.containsKey("visible") || effect.containsKey("hidden");
                    }
                    return rule.containsKey("visible") || rule.containsKey("hidden");
                });
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

    private void copyOption(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

}

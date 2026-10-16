package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.businessapp.ViewSchemaDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
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

/**
 * 业务对象视图 Schema 投影器。
 *
 * <p>采用 Projector 模式统一搜索、列表、详情视图的默认组装、字段清洗及页面区域投影。</p>
 */
final class BusinessObjectViewSchemaProjector {

    private static final String VIEW_SCHEMA_OPTION_KEY = "viewSchema";

    private final ObjectMapper objectMapper;
    private final Function<LowcodeFieldSchema, String> componentResolver;

    BusinessObjectViewSchemaProjector(
            ObjectMapper objectMapper,
            Function<LowcodeFieldSchema, String> componentResolver) {
        this.objectMapper = objectMapper;
        this.componentResolver = componentResolver;
    }

    ViewSchemaDTO resolveViewSchema(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema,
                                            Map<String, Object> designerOptions) {
        ViewSchemaDTO schema = null;
        if (designerOptions != null && designerOptions.containsKey(VIEW_SCHEMA_OPTION_KEY)) {
            Object value = designerOptions.get(VIEW_SCHEMA_OPTION_KEY);
            try {
                if (value instanceof String text && StringUtils.isNotBlank(text)) {
                    schema = objectMapper.readValue(text, ViewSchemaDTO.class);
                } else if (value != null) {
                    schema = objectMapper.convertValue(value, ViewSchemaDTO.class);
                }
            } catch (Exception ignored) {
                schema = buildDefaultViewSchema(modelSchema, pageSchema);
            }
        }
        if (schema == null) {
            schema = buildDefaultViewSchema(modelSchema, pageSchema);
        }
        return sanitizeViewSchemaFieldRefs(schema, modelSchema, pageSchema);
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

    ViewSchemaDTO sanitizeViewSchemaFieldRefs(ViewSchemaDTO schema, LowcodeModelSchema modelSchema,
                                                      LowcodePageSchema pageSchema) {
        if (schema == null) {
            return null;
        }
        Set<String> modelFields = resolvePageViewFieldRefs(modelSchema, pageSchema);
        if (modelFields.isEmpty()) {
            return schema;
        }
        if (schema.getSearch() == null) {
            schema.setSearch(new LinkedHashMap<>());
        }
        if (schema.getList() == null) {
            schema.setList(new LinkedHashMap<>());
        }
        if (schema.getDetail() == null) {
            schema.setDetail(new LinkedHashMap<>());
        }
        schema.getSearch().put("fields", filterViewFieldRefs(listOfMap(schema.getSearch().get("fields")), modelFields));
        schema.getList().put("columns", filterViewFieldRefs(listOfMap(schema.getList().get("columns")), modelFields));
        List<Map<String, Object>> sections = listOfMap(schema.getDetail().get("sections")).stream()
                .map(section -> {
                    Map<String, Object> next = new LinkedHashMap<>(section);
                    next.put("fields", filterViewFieldRefs(listOfMap(section.get("fields")), modelFields));
                    return next;
                })
                .toList();
        schema.getDetail().put("sections", sections);
        return schema;
    }

    private List<Map<String, Object>> filterViewFieldRefs(List<Map<String, Object>> refs, Set<String> modelFields) {
        return refs.stream()
                .filter(item -> modelFields.contains(viewFieldCode(item)))
                .toList();
    }

    private String viewFieldCode(Map<String, Object> item) {
        return StringUtils.defaultIfBlank(text(item.get("fieldCode")), text(item.get("field")));
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

    void applyViewSchemaToPageZones(LowcodePageSchema pageSchema, LowcodeModelSchema modelSchema,
                                            ViewSchemaDTO viewSchema) {
        if (pageSchema == null || modelSchema == null || viewSchema == null) {
            return;
        }
        Set<String> modelFields = resolvePageViewFieldRefs(modelSchema, pageSchema);
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

    /**
     * viewSchema 同时承载主表字段和列表中显式选择的子表字段。只按主模型清洗会在保存时
     * 把 modelCode__field 子表引用从 table zone 删除，导致发布结果与列表画布不一致。
     */
    private Set<String> resolvePageViewFieldRefs(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Set<String> fieldRefs = new LinkedHashSet<>(lowcodeFieldMap(modelSchema).keySet());
        if (pageSchema == null || pageSchema.getModelRefs() == null) {
            return fieldRefs;
        }
        for (LowcodePageModelRef modelRef : pageSchema.getModelRefs()) {
            if (modelRef == null || modelRef.getFields() == null) {
                continue;
            }
            boolean primary = Boolean.TRUE.equals(modelRef.getPrimary());
            for (Map<String, Object> field : modelRef.getFields()) {
                String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
                if (StringUtils.isBlank(sourceField)) {
                    continue;
                }
                String fieldRef = StringUtils.defaultIfBlank(text(field.get("fieldRef")),
                        primary ? sourceField : safeModelKey(modelRef.getModelCode()) + "__" + sourceField);
                if (StringUtils.isNotBlank(fieldRef)) {
                    fieldRefs.add(fieldRef);
                }
            }
        }
        return fieldRefs;
    }

    private void applySearchViewZone(LowcodePageSchema pageSchema, Set<String> modelFields,
                                     Map<String, Object> searchSchema) {
        Map<String, Object> search = searchSchema == null ? Map.of() : searchSchema;
        List<Map<String, Object>> fields = visibleSortedItems(listOfMap(search.get("fields")));
        LowcodePageZone zone = findOrCreateZone(pageSchema, "search", "search-form");
        List<String> fieldRefs = fields.stream()
                .map(item -> StringUtils.defaultIfBlank(text(item.get("fieldCode")), text(item.get("field"))))
                .filter(modelFields::contains)
                .toList();
        zone.setFieldRefs(fieldRefs);
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
            // 保留查询区自定义标签，避免 saveDesigner 投影回 pageSchema 时冲掉
            putIfNotBlank(setting, "label", text(item.get("label")));
            putIfNotBlank(setting, "componentType", text(item.get("componentKey")));
            putIfNotBlank(setting, "queryType", StringUtils.defaultIfBlank(text(item.get("matchMode")), text(item.get("queryType"))));
            putIfNotBlank(setting, "dictType", text(item.get("dictType")));
            putIfNotBlank(setting, "queryField", text(item.get("queryField")));
            if (item.containsKey("defaultValue")) {
                setting.put("defaultValue", item.get("defaultValue"));
            }
            setting.put("collapsed", readBoolean(item.get("collapsed"), false));
            settings.put(fieldCode, setting);
        }
        replaceModelFieldSettings(props, modelFields, settings);
        zone.setProps(props);
        // 列表网格 AiCrudPage 以 searchFieldRefs 为查询条件事实来源，必须与 search zone 同步
        syncListGridSearchFieldRefs(pageSchema, fieldRefs, settings);
    }

    @SuppressWarnings("unchecked")
    private void syncListGridSearchFieldRefs(LowcodePageSchema pageSchema,
                                             List<String> fieldRefs,
                                             Map<String, Object> fieldSettings) {
        if (pageSchema == null) {
            return;
        }
        syncGridSearchFieldRefs(pageSchema.getListGridLayout(), fieldRefs, fieldSettings);
        if (pageSchema.getPages() == null) {
            return;
        }
        for (Map<String, Object> page : pageSchema.getPages()) {
            if (page == null || !"list".equals(text(page.get("pageKey")))) {
                continue;
            }
            Object grid = page.get("gridLayout");
            if (grid instanceof Map<?, ?> gridMap) {
                Map<String, Object> mutable = new LinkedHashMap<>((Map<String, Object>) gridMap);
                syncGridSearchFieldRefs(mutable, fieldRefs, fieldSettings);
                page.put("gridLayout", mutable);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void syncGridSearchFieldRefs(Map<String, Object> gridLayout,
                                         List<String> fieldRefs,
                                         Map<String, Object> fieldSettings) {
        if (gridLayout == null || gridLayout.isEmpty()) {
            return;
        }
        Object itemsValue = gridLayout.get("items");
        if (!(itemsValue instanceof List<?> items)) {
            return;
        }
        List<Object> nextItems = new ArrayList<>(items.size());
        boolean changed = false;
        for (Object itemValue : items) {
            if (!(itemValue instanceof Map<?, ?> rawItem)
                    || !"AiCrudPage".equals(text(rawItem.get("blockType")))) {
                nextItems.add(itemValue);
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>((Map<String, Object>) rawItem);
            Map<String, Object> props = item.get("props") instanceof Map<?, ?> propsMap
                    ? new LinkedHashMap<>((Map<String, Object>) propsMap)
                    : new LinkedHashMap<>();
            props.put("searchFieldRefs", new ArrayList<>(fieldRefs == null ? List.of() : fieldRefs));
            if (fieldSettings != null && !fieldSettings.isEmpty()) {
                // 与已有 searchFieldSettings 合并，避免投影漏字段冲掉 dictType 等扩展项
                props.put("searchFieldSettings", mergeFieldSettings(
                        mapValue(props.get("searchFieldSettings")), fieldSettings));
            }
            item.put("props", props);
            nextItems.add(item);
            changed = true;
        }
        if (changed) {
            gridLayout.put("items", nextItems);
        }
    }

    @SuppressWarnings("unchecked")
    private void syncListGridTableFieldSettings(LowcodePageSchema pageSchema,
                                                List<String> fieldRefs,
                                                Map<String, Object> fieldSettings) {
        if (pageSchema == null) {
            return;
        }
        syncGridTableFieldSettings(pageSchema.getListGridLayout(), fieldRefs, fieldSettings);
        if (pageSchema.getPages() == null) {
            return;
        }
        for (Map<String, Object> page : pageSchema.getPages()) {
            if (page == null || !"list".equals(text(page.get("pageKey")))) {
                continue;
            }
            Object grid = page.get("gridLayout");
            if (grid instanceof Map<?, ?> gridMap) {
                Map<String, Object> mutable = new LinkedHashMap<>((Map<String, Object>) gridMap);
                syncGridTableFieldSettings(mutable, fieldRefs, fieldSettings);
                page.put("gridLayout", mutable);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void syncGridTableFieldSettings(Map<String, Object> gridLayout,
                                            List<String> fieldRefs,
                                            Map<String, Object> fieldSettings) {
        if (gridLayout == null || gridLayout.isEmpty() || fieldSettings == null || fieldSettings.isEmpty()) {
            return;
        }
        Object itemsValue = gridLayout.get("items");
        if (!(itemsValue instanceof List<?> items)) {
            return;
        }
        List<Object> nextItems = new ArrayList<>(items.size());
        boolean changed = false;
        for (Object itemValue : items) {
            if (!(itemValue instanceof Map<?, ?> rawItem)
                    || !"AiCrudPage".equals(text(rawItem.get("blockType")))) {
                nextItems.add(itemValue);
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>((Map<String, Object>) rawItem);
            Map<String, Object> props = item.get("props") instanceof Map<?, ?> propsMap
                    ? new LinkedHashMap<>((Map<String, Object>) propsMap)
                    : new LinkedHashMap<>();
            if (fieldRefs != null) {
                item.put("fieldRefs", new ArrayList<>(fieldRefs));
            }
            props.put("fieldSettings", mergeFieldSettings(mapValue(props.get("fieldSettings")), fieldSettings));
            item.put("props", props);
            nextItems.add(item);
            changed = true;
        }
        if (changed) {
            gridLayout.put("items", nextItems);
        }
    }

    /**
     * 按字段合并设置：compiled 覆盖同名字段的已知键，保留 existing 中未投影的扩展键。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeFieldSettings(Map<String, Object> existing,
                                                   Map<String, Object> compiled) {
        Map<String, Object> merged = new LinkedHashMap<>(existing == null ? Map.of() : existing);
        if (compiled == null || compiled.isEmpty()) {
            return merged;
        }
        for (Map.Entry<String, Object> entry : compiled.entrySet()) {
            String fieldCode = entry.getKey();
            if (StringUtils.isBlank(fieldCode)) {
                continue;
            }
            Map<String, Object> prev = mapValue(merged.get(fieldCode));
            Map<String, Object> next = new LinkedHashMap<>(prev);
            Map<String, Object> patch = entry.getValue() instanceof Map<?, ?> patchMap
                    ? (Map<String, Object>) patchMap
                    : Map.of();
            next.putAll(patch);
            merged.put(fieldCode, next);
        }
        return merged;
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
            // 列标题：viewSchema.columns.label → fieldSettings.title（画布/运行态读取 title|label）
            putIfNotBlank(setting, "title", text(item.get("label")));
            putIfNotBlank(setting, "label", text(item.get("label")));
            putIfPresent(setting, "width", item.get("width"));
            putIfPresent(setting, "minWidth", item.get("minWidth"));
            putIfNotBlank(setting, "fixed", normalizeFixed(text(item.get("fixed"))));
            setting.put("sortable", readBoolean(item.get("sortable"), false));
            putIfNotBlank(setting, "renderType", text(item.get("formatter")));
            settings.put(fieldCode, setting);
        }
        replaceModelFieldSettings(props, modelFields, settings);
        zone.setProps(props);
        // 同步到 AiCrudPage.fieldSettings，避免仅 table zone 有标题、网格块丢标题
        syncListGridTableFieldSettings(pageSchema, zone.getFieldRefs(), settings);
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

    private String resolveFormComponentKey(LowcodeFieldSchema field) {
        return componentResolver == null ? "input" : componentResolver.apply(field);
    }

    private String safeModelKey(String value) {
        String key = StringUtils.defaultIfBlank(value, "model").replaceAll("[^A-Za-z0-9_]", "_");
        return StringUtils.defaultIfBlank(key, "model");
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

    private void replaceModelFieldSettings(Map<String, Object> props,
                                           Set<String> modelFields,
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

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
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

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}


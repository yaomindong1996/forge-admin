package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessCodeAppFormAssetMerger.mergeNonNull;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowJsonReader.readBooleanValue;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowJsonReader.readMapList;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowJsonReader.readNestedArray;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowJsonReader.readNestedObject;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowJsonReader.textValue;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowTaskFormControlTypes.firstStrongTaskFormControlType;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowTaskFormControlTypes.normalizeTaskFormFieldType;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowTaskFormControlTypes.resolveTaskFormControlType;

/**
 * 审批任务主子表装配器。
 * <p>
 * 以表单设计器的子表清单和顺序为准，用发布态关系配置与字段注册表补齐运行元数据，
 * 再应用 BPMN 节点的子表权限。该组件只组装协议，不读写业务记录或流程状态。
 */
@Slf4j
final class BusinessFlowTaskChildAssembler {

    private final BusinessFlowTaskChildPolicy childPolicy;
    private final BusinessFlowTaskFormPolicy formPolicy;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessFieldDesignService businessFieldDesignService;
    private final BusinessFlowFormAssetAssembler formAssetAssembler;
    private final Supplier<Long> tenantIdSupplier;

    BusinessFlowTaskChildAssembler(BusinessFlowTaskChildPolicy childPolicy,
                                   BusinessFlowTaskFormPolicy formPolicy,
                                   BusinessObjectMapper businessObjectMapper,
                                   BusinessFieldDesignService businessFieldDesignService,
                                   BusinessFlowFormAssetAssembler formAssetAssembler,
                                   Supplier<Long> tenantIdSupplier) {
        this.childPolicy = childPolicy;
        this.formPolicy = formPolicy;
        this.businessObjectMapper = businessObjectMapper;
        this.businessFieldDesignService = businessFieldDesignService;
        this.formAssetAssembler = formAssetAssembler;
        this.tenantIdSupplier = tenantIdSupplier;
    }

    List<Map<String, Object>> resolveBusinessTaskChildrenConfig(String configKey,
                                                                        JSONObject nodeForm,
                                                                        JSONObject runtimeOptions,
                                                                        JSONObject formSchema) {
        if (StringUtils.isBlank(configKey)) {
            return List.of();
        }
        try {
            JSONObject options = runtimeOptions == null ? new JSONObject() : runtimeOptions;
            JSONObject masterDetailConfig = readNestedObject(options.get("masterDetailConfig"));
            List<Map<String, Object>> rawChildren = readMapList(readNestedArray(masterDetailConfig.get("children"))).stream()
                    .filter(childPolicy::isDetailChild)
                    .toList();
            Map<String, Map<String, Object>> publishedByKey = indexTaskChildrenByKey(rawChildren);
            // 表单设计器里的子表组件决定「审批展示哪些子表」；发布态 masterDetail 只负责补齐字段元数据
            List<Map<String, Object>> formChildren = collectFormDesignerSubTables(formSchema);
            List<Map<String, Object>> sourceChildren = !formChildren.isEmpty()
                    ? mergeFormDesignerChildrenWithPublished(formChildren, publishedByKey)
                    : rawChildren;
            // 设计器子表解析失败时回落发布态，避免审批页整块子表消失
            if (sourceChildren.isEmpty() && !rawChildren.isEmpty()) {
                sourceChildren = rawChildren;
            }
            Map<String, Map<String, Object>> childPermissions = childPolicy.normalizeChildPermissionMap(nodeForm);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> rawChild : sourceChildren) {
                String childKey = childPolicy.resolveChildKey(rawChild);
                Map<String, Object> childPermission = childPolicy.findChildPermission(childPermissions, childKey);
                if (childPermission != null && !readBooleanValue(childPermission.get("readable"), true)) {
                    continue;
                }
                Map<String, Object> child = new LinkedHashMap<>(rawChild);
                boolean fieldWritable = childPolicy.hasWritableField(rawChild, nodeForm, childKey);
                // 设计器/发布态默认允许新增；节点 childPermissions 显式配置时以节点为准
                boolean designerAllowCreate = readBooleanValue(rawChild.get("allowCreate"),
                        readBooleanValue(rawChild.get("inlineCreateEnabled"), true));
                if (childPermission != null) {
                    child.put("allowCreate", readBooleanValue(childPermission.get("allowCreate"), false));
                    // 字段勾了可编辑，已有行也要能改。行级「修改」只是额外开关，不能把字段权限盖掉。
                    child.put("allowUpdate", fieldWritable
                            || readBooleanValue(childPermission.get("allowUpdate"), false));
                    child.put("allowDelete", readBooleanValue(childPermission.get("allowDelete"), false));
                } else {
                    child.put("allowCreate", designerAllowCreate);
                    child.put("allowUpdate", fieldWritable
                            || readBooleanValue(rawChild.get("allowUpdate"), true));
                    child.put("allowDelete", readBooleanValue(rawChild.get("allowDelete"), false));
                }
                child.put("readable", true);
                ensureChildSelectExistingConfig(child);
                List<Map<String, Object>> visibleFields = childPolicy.applyFieldPermissions(
                        readMapList(readNestedArray(rawChild.get("fields"))), nodeForm, childKey);
                child.put("fields", visibleFields);
                if (!visibleFields.isEmpty()) {
                    result.add(child);
                }
            }
            attachTaskChildFieldEvents(result, formSchema);
            return result;
        } catch (Exception e) {
            log.warn("读取业务表单子表配置失败: configKey={}, error={}", configKey, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 把表单治理里的字段自动查询挂到子表，审批 ChildTableEditor 按行触发联动。
     */
    private void attachTaskChildFieldEvents(List<Map<String, Object>> children, JSONObject formSchema) {
        if (children == null || children.isEmpty()) {
            return;
        }
        List<Map<String, Object>> events = collectFormFieldEvents(formSchema);
        if (events.isEmpty()) {
            return;
        }
        for (Map<String, Object> child : children) {
            if (child == null) {
                continue;
            }
            if (!(child.get("fieldEvents") instanceof List<?> existing) || existing.isEmpty()) {
                child.put("fieldEvents", events);
            }
        }
    }

    private List<Map<String, Object>> collectFormFieldEvents(JSONObject formSchema) {
        if (formSchema == null || formSchema.isEmpty()) {
            return List.of();
        }
        JSONObject settings = readNestedObject(formSchema.get("settings"));
        JSONObject governance = readNestedObject(settings.get("governance"));
        List<Map<String, Object>> events = readMapList(readNestedArray(governance.get("fieldEvents")));
        if (!events.isEmpty()) {
            return events;
        }
        return readMapList(readNestedArray(settings.get("fieldEvents")));
    }

    private Map<String, Map<String, Object>> indexTaskChildrenByKey(List<Map<String, Object>> children) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> child : children) {
            String key = childPolicy.resolveChildKey(child);
            if (key != null) {
                result.putIfAbsent(key, child);
                // 同子表可能同时有 relationKey / 带前缀 modelCode，互为别名
                String relationKey = StringUtils.trimToNull(textValue(child.get("relationKey")));
                String modelCode = StringUtils.trimToNull(textValue(child.get("modelCode")));
                if (relationKey != null) {
                    result.putIfAbsent(relationKey, child);
                }
                if (modelCode != null) {
                    result.putIfAbsent(modelCode, child);
                }
            }
        }
        return result;
    }

    /**
     * 从表单设计器 subTable 组件收集子表清单（顺序与画布一致）。
     */
    private List<Map<String, Object>> collectFormDesignerSubTables(JSONObject formSchema) {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        collectFormDesignerSubTableComponents(readNestedArray(formSchema == null ? null : formSchema.get("components")), result, seen);
        if (result.isEmpty() && formSchema != null) {
            JSONObject settings = readNestedObject(formSchema.get("settings"));
            collectFormDesignerSubTableComponents(readNestedArray(settings.get("components")), result, seen);
        }
        return result;
    }

    private void collectFormDesignerSubTableComponents(JSONArray components,
                                                       List<Map<String, Object>> result,
                                                       Set<String> seen) {
        if (components == null) {
            return;
        }
        for (int i = 0; i < components.size(); i++) {
            JSONObject component = components.getJSONObject(i);
            if (component == null) {
                continue;
            }
            String componentKey = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(component.getString("componentKey")),
                    StringUtils.trimToNull(component.getString("type")));
            JSONObject props = readNestedObject(component.get("props"));
            if ("subTable".equalsIgnoreCase(componentKey) || "childTable".equalsIgnoreCase(componentKey)) {
                String childKey = StringUtils.firstNonBlank(
                        StringUtils.trimToNull(props.getString("modelCode")),
                        StringUtils.trimToNull(props.getString("relationKey")),
                        StringUtils.trimToNull(component.getString("modelCode")),
                        StringUtils.trimToNull(component.getString("relationKey")));
                if (childKey == null || !seen.add(childKey)) {
                    collectFormDesignerSubTableComponents(readNestedArray(component.get("children")), result, seen);
                    continue;
                }
                Map<String, Object> child = new LinkedHashMap<>();
                child.put("modelCode", StringUtils.firstNonBlank(
                        StringUtils.trimToNull(props.getString("modelCode")), childKey));
                child.put("relationKey", StringUtils.firstNonBlank(
                        StringUtils.trimToNull(props.getString("relationKey")), childKey));
                child.put("key", childKey);
                child.put("label", StringUtils.firstNonBlank(
                        StringUtils.trimToNull(props.getString("header")),
                        StringUtils.trimToNull(props.getString("relationName")),
                        StringUtils.trimToNull(component.getString("label")),
                        childKey));
                child.put("relationName", child.get("label"));
                child.put("tabTitle", child.get("label"));
                child.put("relationType", StringUtils.defaultIfBlank(props.getString("relationType"), "ONE_TO_MANY"));
                child.put("showInDetail", props.get("showInDetail") == null || Boolean.TRUE.equals(props.getBoolean("showInDetail")));
                boolean allowCreate = readBooleanValue(props.get("allowCreate"), true);
                child.put("allowCreate", allowCreate);
                child.put("inlineCreateEnabled", readBooleanValue(props.get("inlineCreateEnabled"), allowCreate));
                child.put("showInCreate", readBooleanValue(props.get("showInCreate"), allowCreate));
                boolean allowSelectExisting = readBooleanValue(props.get("allowSelectExisting"), false);
                child.put("allowSelectExisting", allowSelectExisting);
                if (props.containsKey("selectorMultiple")) {
                    child.put("selectorMultiple", readBooleanValue(props.get("selectorMultiple"), true));
                }
                JSONArray selectorDisplayFields = readNestedArray(props.get("selectorDisplayFields"));
                if (!selectorDisplayFields.isEmpty()) {
                    child.put("selectorDisplayFields", selectorDisplayFields);
                }
                JSONArray selectorFilterFields = readNestedArray(props.get("selectorFilterFields"));
                if (!selectorFilterFields.isEmpty()) {
                    child.put("selectorFilterFields", selectorFilterFields);
                }
                Object recordSelector = props.get("recordSelector");
                if (recordSelector instanceof Map<?, ?> selector && !selector.isEmpty()) {
                    Map<String, Object> selectorMap = new LinkedHashMap<>();
                    selector.forEach((key, value) -> {
                        if (key != null) {
                            selectorMap.put(String.valueOf(key), value);
                        }
                    });
                    child.put("recordSelector", selectorMap);
                }
                ensureChildSelectExistingConfig(child);
                JSONArray columns = readNestedArray(props.get("columns"));
                if (columns.isEmpty()) {
                    columns = readNestedArray(props.get("fields"));
                }
                List<Map<String, Object>> fields = new ArrayList<>();
                for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                    Object rawColumn = columns.get(columnIndex);
                    JSONObject column = rawColumn instanceof JSONObject jsonColumn
                            ? jsonColumn
                            : rawColumn instanceof Map<?, ?> ? readNestedObject(rawColumn) : null;
                    String fieldCode = column == null
                            ? StringUtils.trimToNull(textValue(rawColumn))
                            : StringUtils.firstNonBlank(
                                    StringUtils.trimToNull(column.getString("fieldCode")),
                                    StringUtils.trimToNull(column.getString("field")),
                                    StringUtils.trimToNull(column.getString("sourceField")));
                    if (fieldCode == null) {
                        continue;
                    }
                    Map<String, Object> field = column == null ? new LinkedHashMap<>() : new LinkedHashMap<>(column);
                    field.put("field", fieldCode);
                    field.put("fieldCode", fieldCode);
                    field.putIfAbsent("label", StringUtils.firstNonBlank(
                            column == null ? null : StringUtils.trimToNull(column.getString("fieldLabel")),
                            column == null ? null : StringUtils.trimToNull(column.getString("label")),
                            fieldCode));
                    fields.add(field);
                }
                child.put("fields", fields);
                result.add(child);
            }
            collectFormDesignerSubTableComponents(readNestedArray(component.get("children")), result, seen);
        }
    }

    List<Map<String, Object>> mergeFormDesignerChildrenWithPublished(
            List<Map<String, Object>> formChildren,
            Map<String, Map<String, Object>> publishedByKey) {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Map<String, Object>> usedPublished = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<String, Map<String, Map<String, Object>>> registryCache = new HashMap<>();
        for (Map<String, Object> formChild : formChildren) {
            Map<String, Object> published = findPublishedChild(publishedByKey, formChild, usedPublished);
            if (published == null) {
                if (!readMapList(readNestedArray(formChild.get("fields"))).isEmpty()) {
                    Map<String, Object> standalone = new LinkedHashMap<>(formChild);
                    enrichTaskChildFieldControls(standalone, childPolicy.childKeyCandidates(formChild), registryCache);
                    ensureChildSelectExistingConfig(standalone);
                    result.add(standalone);
                }
                continue;
            }
            usedPublished.add(published);
            Map<String, Object> merged = new LinkedHashMap<>(published);
            // 展示名/页签顺序以设计器为准；数据键保留发布态 modelCode，才能对上详情 children
            String designerLabel = StringUtils.firstNonBlank(
                    textValue(formChild.get("label")),
                    textValue(formChild.get("relationName")),
                    textValue(published.get("relationName")),
                    textValue(published.get("tabTitle")),
                    textValue(published.get("modelName")));
            if (designerLabel != null) {
                merged.put("label", designerLabel);
                merged.put("relationName", designerLabel);
                merged.put("tabTitle", designerLabel);
            }
            // 发布态 modelCode 是 DynamicCrud 装载 children 的键，禁止被设计器短键覆盖
            if (published.get("modelCode") != null) {
                merged.put("modelCode", published.get("modelCode"));
            }
            if (published.get("key") != null) {
                merged.put("key", published.get("key"));
            }
            overlayFormChildBehavior(merged, formChild);
            List<Map<String, Object>> formFields = readMapList(readNestedArray(formChild.get("fields")));
            if (!formFields.isEmpty()) {
                // 列集合以设计器子表列为准，发布态字段定义做补齐
                Map<String, Map<String, Object>> publishedFields = new LinkedHashMap<>();
                for (Map<String, Object> field : readMapList(readNestedArray(published.get("fields")))) {
                    String code = StringUtils.firstNonBlank(
                            textValue(field.get("field")), textValue(field.get("fieldCode")), textValue(field.get("sourceField")));
                    if (code != null) {
                        publishedFields.putIfAbsent(code, field);
                    }
                }
                List<Map<String, Object>> fields = new ArrayList<>();
                for (Map<String, Object> formField : formFields) {
                    String code = StringUtils.firstNonBlank(
                            textValue(formField.get("field")), textValue(formField.get("fieldCode")));
                    Map<String, Object> next = new LinkedHashMap<>(formField);
                    Map<String, Object> publishedField = code == null ? null : findPublishedChildField(publishedFields, code);
                    if (publishedField != null) {
                        mergeNonNull(next, publishedField);
                        next.put("field", code);
                        next.put("fieldCode", code);
                    }
                    fields.add(next);
                }
                merged.put("fields", fields);
            }
            List<String> objectCodes = new ArrayList<>(childPolicy.childKeyCandidates(formChild));
            childPolicy.childKeyCandidates(published).forEach(code -> {
                if (!objectCodes.contains(code)) {
                    objectCodes.add(code);
                }
            });
            enrichTaskChildFieldControls(merged, objectCodes, registryCache);
            ensureChildSelectExistingConfig(merged);
            result.add(merged);
        }
        return result;
    }

    /**
     * 设计器子表列只存 fieldCode/fieldLabel；发布态缺列或类型过期时，用子表对象实时字段注册表补齐控件，
     * 与设计器读取的字段来源保持一致，避免审批端下拉/人员等退化为输入框。
     */
    private void enrichTaskChildFieldControls(Map<String, Object> child,
                                              List<String> objectCodes,
                                              Map<String, Map<String, Map<String, Object>>> registryCache) {
        List<Map<String, Object>> fields = readMapList(readNestedArray(child.get("fields")));
        if (fields.isEmpty()) {
            return;
        }
        Map<String, Map<String, Object>> registry = loadChildFieldRegistry(objectCodes, registryCache);
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Map<String, Object> source : fields) {
            Map<String, Object> field = new LinkedHashMap<>(source);
            String code = StringUtils.firstNonBlank(
                    textValue(field.get("field")), textValue(field.get("fieldCode")), textValue(field.get("sourceField")));
            applyFieldRegistryMetadata(field, findPublishedChildField(registry, code), true);
            String controlType = firstStrongTaskFormControlType(field);
            if (controlType == null) {
                controlType = resolveTaskFormControlType(field);
            }
            field.put("type", normalizeTaskFormFieldType(controlType));
            enriched.add(field);
        }
        child.put("fields", enriched);
    }

    /**
     * 设计器画布预览会把字段注册表（referenceObjectCode / dictType / basicProps 等）合进组件；
     * 审批主表只拷贝组件 props，引用/字典下拉因此缺配置拉不到选项，这里按同一来源补齐。
     */
    void enrichTaskMainFieldsFromObjectRegistry(List<Map<String, Object>> fields, BusinessObjectVO object) {
        if (fields == null || fields.isEmpty() || object == null || object.getId() == null) {
            return;
        }
        Map<String, Map<String, Object>> registry = buildObjectFieldRegistry(object.getId());
        if (registry.isEmpty()) {
            return;
        }
        for (Map<String, Object> field : fields) {
            if (field == null || formPolicy.isChildField(field)) {
                continue;
            }
            String code = StringUtils.firstNonBlank(textValue(field.get("field")), textValue(field.get("fieldCode")));
            applyFieldRegistryMetadata(field, findPublishedChildField(registry, code), false);
        }
    }

    /**
     * 注册表只补缺：引用与字典元数据只填空值，props 以现有配置为准。
     * fillWeakType=true 时（子表列无控件配置）才用注册表类型替换空/input；主表控件以设计器 componentKey 为准。
     */
    private void applyFieldRegistryMetadata(Map<String, Object> field,
                                            Map<String, Object> registryField,
                                            boolean fillWeakType) {
        if (field == null || registryField == null) {
            return;
        }
        String registryType = firstStrongTaskFormControlType(registryField);
        if (fillWeakType && firstStrongTaskFormControlType(field) == null && registryType != null) {
            field.put("type", normalizeTaskFormFieldType(registryType));
            field.put("componentType", registryType);
            if (field.containsKey("componentKey")) {
                field.put("componentKey", registryType);
            }
        }
        Map<String, Object> ownProps = readNestedObject(field.get("props"));
        Map<String, Object> registryProps = new LinkedHashMap<>(readNestedObject(registryField.get("basicProps")));
        registryProps.remove("fieldBinding");
        if (!fillWeakType) {
            // 主表设计器已决定控件与选项源，只补引用配置；字典仅补给缺 dictType 的字典下拉
            fillBlank(field, registryField, "referenceObjectCode", "referenceDisplayField");
            if ("dictSelect".equals(normalizeTaskFormFieldType(firstStrongTaskFormControlType(field)))
                    && StringUtils.isBlank(textValue(ownProps.get("dictType")))) {
                fillBlank(field, registryField, "dictType");
            }
            registryProps.keySet().retainAll(Set.of("referenceObjectCode", "referenceDisplayField",
                    "referenceValueField", "recordSelector", "referenceConfig"));
        } else {
            fillBlank(field, registryField, "dictType", "dataType", "referenceObjectCode", "referenceDisplayField",
                    "precision", "length", "formulaConfig", "advancedProps", "basicProps");
            // 注册表 basicProps 常含 optionSource；发布快照若只有弱 props，这里补进 props
            if (!(ownProps.get("optionSource") instanceof Map<?, ?>)) {
                Object registryOptionSource = registryProps.get("optionSource");
                if (registryOptionSource instanceof Map<?, ?>) {
                    ownProps.put("optionSource", registryOptionSource);
                }
            }
            for (String key : List.of("fieldMappings", "mappings", "labelValueField", "targetField",
                    "cascade", "cascadeConfig", "checkedValue", "uncheckedValue", "runtimeRules")) {
                if (ownProps.get(key) == null && registryProps.get(key) != null) {
                    ownProps.put(key, registryProps.get(key));
                }
            }
        }
        if (ownProps.get("optionSource") instanceof Map<?, ?> os) {
            Object rawType = os.get("type");
            String type = rawType == null ? "" : String.valueOf(rawType).trim();
            // 动态选项源下不能混入注册表静态 options；STATIC 保留 options
            if (!type.isEmpty() && !"STATIC".equalsIgnoreCase(type.replace('-', '_'))) {
                registryProps.remove("options");
            }
        }
        if (registryProps.isEmpty()) {
            return;
        }
        registryProps.putAll(ownProps);
        field.put("props", registryProps);
    }

    private void fillBlank(Map<String, Object> target, Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object current = target.get(key);
            Object value = source.get(key);
            if (value != null && (current == null || (current instanceof String text && StringUtils.isBlank(text)))) {
                target.put(key, value);
            }
        }
    }

    private Map<String, Map<String, Object>> loadChildFieldRegistry(
            List<String> objectCodes,
            Map<String, Map<String, Map<String, Object>>> registryCache) {
        Long tenantId = tenantIdSupplier.get();
        for (String objectCode : objectCodes) {
            if (StringUtils.isBlank(objectCode)) {
                continue;
            }
            Map<String, Map<String, Object>> cached = registryCache.computeIfAbsent(objectCode, code -> {
                AiBusinessObject object = businessObjectMapper.selectFirstByObjectCode(tenantId, code);
                return object == null || object.getId() == null ? Map.of() : buildObjectFieldRegistry(object.getId());
            });
            if (!cached.isEmpty()) {
                return cached;
            }
        }
        return Map.of();
    }

    private Map<String, Map<String, Object>> buildObjectFieldRegistry(Long objectId) {
        try {
            Map<String, Map<String, Object>> byCode = new LinkedHashMap<>();
            businessFieldDesignService.listFields(objectId).forEach(vo -> {
                Map<String, Object> raw = new LinkedHashMap<>(
                        JSON.parseObject(JSON.toJSONString(vo), JSONObject.class));
                Map<String, Object> normalized = formAssetAssembler.normalizeRuntimeCrudFormField(raw);
                String fieldCode = normalized == null ? null : textValue(normalized.get("field"));
                if (fieldCode != null) {
                    byCode.putIfAbsent(fieldCode, normalized);
                }
            });
            return byCode;
        } catch (Exception e) {
            log.debug("读取业务对象字段注册表失败: objectId={}, error={}", objectId, e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> findPublishedChildField(Map<String, Map<String, Object>> publishedFields, String code) {
        if (publishedFields == null || StringUtils.isBlank(code)) {
            return null;
        }
        Map<String, Object> direct = publishedFields.get(code);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, Map<String, Object>> entry : publishedFields.entrySet()) {
            if (childPolicy.sameFieldName(entry.getKey(), code)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<String, Object> findPublishedChild(Map<String, Map<String, Object>> publishedByKey,
                                                    Map<String, Object> formChild,
                                                    Set<Map<String, Object>> usedPublished) {
        if (publishedByKey == null || publishedByKey.isEmpty() || formChild == null) {
            return null;
        }
        for (String candidate : childPolicy.childKeyCandidates(formChild)) {
            Map<String, Object> direct = publishedByKey.get(candidate);
            if (direct != null && (usedPublished == null || !usedPublished.contains(direct))) {
                return direct;
            }
        }
        Map<String, Object> best = null;
        int bestDelta = Integer.MAX_VALUE;
        for (String candidate : childPolicy.childKeyCandidates(formChild)) {
            for (Map.Entry<String, Map<String, Object>> entry : publishedByKey.entrySet()) {
                Map<String, Object> published = entry.getValue();
                if (published == null || (usedPublished != null && usedPublished.contains(published))) {
                    continue;
                }
                if (!childPolicy.sameChildTableKey(entry.getKey(), candidate)) {
                    continue;
                }
                int delta = Math.abs(StringUtils.length(entry.getKey()) - StringUtils.length(candidate));
                if (delta < bestDelta) {
                    bestDelta = delta;
                    best = published;
                }
            }
        }
        return best;
    }

    private Map<String, Object> findPublishedChild(Map<String, Map<String, Object>> publishedByKey, String childKey) {
        if (publishedByKey == null || StringUtils.isBlank(childKey)) {
            return null;
        }
        Map<String, Object> probe = new LinkedHashMap<>();
        probe.put("modelCode", childKey);
        return findPublishedChild(publishedByKey, probe, null);
    }

    /**
     * 表单设计器子表行为（新增/选择已有）覆盖发布态同名字段。
     */
    private void overlayFormChildBehavior(Map<String, Object> target, Map<String, Object> formChild) {
        if (target == null || formChild == null) {
            return;
        }
        copyIfPresent(target, formChild, "allowCreate");
        copyIfPresent(target, formChild, "inlineCreateEnabled");
        copyIfPresent(target, formChild, "showInCreate");
        copyIfPresent(target, formChild, "allowSelectExisting");
        copyIfPresent(target, formChild, "selectorMultiple");
        copyIfPresent(target, formChild, "selectorDisplayFields");
        copyIfPresent(target, formChild, "selectorFilterFields");
        copyIfPresent(target, formChild, "recordSelector");
        // 设计器关掉「选择已有」时清掉选择器，避免前端仍按 recordSelector 出按钮
        if (formChild.containsKey("allowSelectExisting")
                && !readBooleanValue(formChild.get("allowSelectExisting"), false)) {
            target.remove("recordSelector");
        }
    }

    private void copyIfPresent(Map<String, Object> target, Map<String, Object> source, String key) {
        if (source != null && source.containsKey(key) && source.get(key) != null) {
            target.put(key, source.get(key));
        }
    }

    /**
     * 设计器开启「选择已有」但未写 recordSelector 时，用子表 modelCode 合成选择器配置。
     */
    private void ensureChildSelectExistingConfig(Map<String, Object> child) {
        if (child == null) {
            return;
        }
        Object existingSelector = child.get("recordSelector");
        boolean hasSelector = existingSelector instanceof Map<?, ?> map && !map.isEmpty();
        boolean allowSelectExisting = readBooleanValue(child.get("allowSelectExisting"), hasSelector);
        if (!allowSelectExisting) {
            return;
        }
        child.put("allowSelectExisting", true);
        if (hasSelector) {
            return;
        }
        String modelCode = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(child.get("modelCode"))),
                StringUtils.trimToNull(textValue(child.get("relationKey"))),
                StringUtils.trimToNull(textValue(child.get("key"))));
        if (modelCode == null) {
            return;
        }
        Map<String, Object> selector = new LinkedHashMap<>();
        selector.put("objectCode", modelCode);
        selector.put("businessObjectCode", modelCode);
        selector.put("multiple", readBooleanValue(child.get("selectorMultiple"), true));
        JSONArray displayFields = readNestedArray(child.get("selectorDisplayFields"));
        if (!displayFields.isEmpty()) {
            selector.put("displayFields", displayFields);
            selector.put("keywordFields", displayFields);
        }
        JSONArray filterFields = readNestedArray(child.get("selectorFilterFields"));
        if (!filterFields.isEmpty()) {
            selector.put("filterFields", filterFields);
        }
        child.put("recordSelector", selector);
    }

}

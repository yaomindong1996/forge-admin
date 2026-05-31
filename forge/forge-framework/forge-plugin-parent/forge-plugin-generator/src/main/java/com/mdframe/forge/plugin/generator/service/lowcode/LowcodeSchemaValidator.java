package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeIndexSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 低代码单表协议校验。
 */
@Service
public class LowcodeSchemaValidator {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");
    private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");
    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("^[a-z][A-Za-z0-9]{1,63}$");

    private static final Set<String> RESERVED_TABLE_PREFIXES = Set.of(
            "sys_", "ai_", "gen_", "flow_", "qrtz_", "data_"
    );
    private static final Set<String> BASE_FIELDS = Set.of(
            "id", "tenantId", "createBy", "createTime", "createDept", "updateBy", "updateTime", "delFlag"
    );
    private static final Set<String> BASE_COLUMNS = Set.of(
            "id", "tenant_id", "create_by", "create_time", "create_dept", "update_by", "update_time", "del_flag"
    );
    private static final Set<String> TABLE_MODES = Set.of("EXISTING", "CREATE");
    private static final Set<String> APP_TYPES = Set.of("SINGLE", "TREE", "MASTER_DETAIL");
    private static final Set<String> DATA_TYPES = Set.of(
            "varchar", "char", "text", "longtext", "int", "bigint", "decimal", "date", "datetime", "time", "tinyint"
    );
    private static final Set<String> COMPONENT_TYPES = Set.of(
            "input", "textarea", "select", "radio", "checkbox", "switch", "date", "datetime", "time",
            "number", "upload", "imageUpload", "fileUpload", "cascader", "treeSelect",
            "dictSelect", "orgTreeSelect", "userSelect", "regionTreeSelect", "objectReference"
    );
    private static final Set<String> QUERY_TYPES = Set.of(
            "eq", "ne", "like", "left_like", "right_like", "gt", "ge", "gte", "lt", "le", "lte", "in", "between"
    );
    private static final Set<String> ZONE_KEYS = Set.of("search", "table", "edit", "detail", "toolbar");
    private static final Set<String> SENSITIVE_TYPES = Set.of(
            "NONE", "PHONE", "ID_CARD", "EMAIL", "BANK_CARD", "NAME", "ADDRESS", "PASSWORD", "CUSTOM"
    );

    public void validateModel(LowcodeModelSchema modelSchema) {
        if (modelSchema == null) {
            throw new BusinessException("数据模型不能为空");
        }
        String tableMode = StringUtils.defaultIfBlank(modelSchema.getTableMode(), "EXISTING").toUpperCase(Locale.ROOT);
        if (!TABLE_MODES.contains(tableMode)) {
            throw new BusinessException("不支持的数据表模式: " + modelSchema.getTableMode());
        }
        String appType = StringUtils.defaultIfBlank(modelSchema.getAppType(), "SINGLE").toUpperCase(Locale.ROOT);
        if (!APP_TYPES.contains(appType)) {
            throw new BusinessException("不支持的应用类型: " + modelSchema.getAppType());
        }
        validateTableName(modelSchema.getTableName());
        if (modelSchema.getFields() == null || modelSchema.getFields().isEmpty()) {
            throw new BusinessException("数据模型至少需要一个业务字段");
        }

        Set<String> fields = new HashSet<>();
        Set<String> columns = new HashSet<>();
        int businessFieldCount = 0;
        for (LowcodeFieldSchema fieldSchema : modelSchema.getFields()) {
            validateField(fieldSchema);
            String field = fieldSchema.getField();
            String column = fieldSchema.getColumnName();
            if (!fields.add(field)) {
                throw new BusinessException("字段名重复: " + field);
            }
            if (!columns.add(column)) {
                throw new BusinessException("数据库列名重复: " + column);
            }
            if (!isSystemField(fieldSchema)) {
                businessFieldCount++;
            }
        }
        if (businessFieldCount == 0) {
            throw new BusinessException("数据模型至少需要一个业务字段");
        }
        if ("TREE".equals(appType) && modelSchema.getTreeConfig() != null
                && StringUtils.isNotBlank(modelSchema.getTreeConfig().getParentField())
                && !fields.contains(modelSchema.getTreeConfig().getParentField())
                && !columns.contains(modelSchema.getTreeConfig().getParentField())) {
            throw new BusinessException("树形父级字段不存在: " + modelSchema.getTreeConfig().getParentField());
        }
        validateIndexes(modelSchema, fields);
    }

    public void validatePage(LowcodePageSchema pageSchema, LowcodeModelSchema modelSchema) {
        validateModel(modelSchema);
        if (pageSchema == null) {
            throw new BusinessException("页面配置不能为空");
        }
        if (pageSchema.getZones() == null) {
            return;
        }

        Set<String> modelFields = modelSchema.getFields().stream()
                .map(LowcodeFieldSchema::getField)
                .collect(Collectors.toSet());
        Set<String> modelColumns = modelSchema.getFields().stream()
                .map(LowcodeFieldSchema::getColumnName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        Set<String> pageFields = buildAllowedPageRefs(modelFields, pageSchema);
        for (LowcodePageZone zone : pageSchema.getZones()) {
            validateZone(zone, pageFields);
        }
        validateTreeRuntime(modelSchema, pageSchema, modelFields, modelColumns, pageFields);
    }

    private void validateTableName(String tableName) {
        if (!isValidTableName(tableName)) {
            throw new BusinessException("表名格式不正确，仅允许小写字母、数字、下划线，且必须以小写字母开头");
        }
        for (String prefix : RESERVED_TABLE_PREFIXES) {
            if (tableName.startsWith(prefix)) {
                throw new BusinessException("业务表名不能使用系统保留前缀: " + prefix);
            }
        }
    }

    public boolean isValidTableName(String tableName) {
        return StringUtils.isNotBlank(tableName) && TABLE_NAME_PATTERN.matcher(tableName).matches();
    }

    private void validateField(LowcodeFieldSchema fieldSchema) {
        if (fieldSchema == null) {
            throw new BusinessException("字段配置不能为空");
        }
        if (isSystemField(fieldSchema)) {
            validateSystemField(fieldSchema);
            return;
        }
        if (StringUtils.isBlank(fieldSchema.getField()) || !FIELD_NAME_PATTERN.matcher(fieldSchema.getField()).matches()) {
            throw new BusinessException("字段名格式不正确: " + fieldSchema.getField());
        }
        if (BASE_FIELDS.contains(fieldSchema.getField())) {
            throw new BusinessException("基础审计字段不能作为业务字段: " + fieldSchema.getField());
        }
        if (StringUtils.isBlank(fieldSchema.getColumnName()) || !COLUMN_NAME_PATTERN.matcher(fieldSchema.getColumnName()).matches()) {
            throw new BusinessException("数据库列名格式不正确: " + fieldSchema.getColumnName());
        }
        if (BASE_COLUMNS.contains(fieldSchema.getColumnName())) {
            throw new BusinessException("基础审计列不能作为业务字段: " + fieldSchema.getColumnName());
        }
        String dataType = StringUtils.defaultIfBlank(fieldSchema.getDataType(), "varchar").toLowerCase(Locale.ROOT);
        if (!DATA_TYPES.contains(dataType)) {
            throw new BusinessException("不支持的数据类型: " + fieldSchema.getDataType());
        }
        String componentType = StringUtils.defaultIfBlank(fieldSchema.getComponentType(), "input");
        if (!COMPONENT_TYPES.contains(componentType)) {
            throw new BusinessException("不支持的控件类型: " + componentType);
        }
        String queryType = StringUtils.defaultIfBlank(fieldSchema.getQueryType(), "eq").toLowerCase(Locale.ROOT);
        if (!QUERY_TYPES.contains(queryType)) {
            throw new BusinessException("不支持的查询方式: " + fieldSchema.getQueryType());
        }
        String sensitiveType = StringUtils.defaultIfBlank(fieldSchema.getSensitiveType(), "NONE").toUpperCase(Locale.ROOT);
        if (!SENSITIVE_TYPES.contains(sensitiveType)) {
            throw new BusinessException("不支持的敏感类型: " + fieldSchema.getSensitiveType());
        }
    }

    private boolean isSystemField(LowcodeFieldSchema fieldSchema) {
        if (fieldSchema == null) {
            return false;
        }
        return Boolean.TRUE.equals(fieldSchema.getSystemField())
                || BASE_FIELDS.contains(fieldSchema.getField())
                || BASE_COLUMNS.contains(fieldSchema.getColumnName());
    }

    private void validateSystemField(LowcodeFieldSchema fieldSchema) {
        if (!BASE_FIELDS.contains(fieldSchema.getField()) || !BASE_COLUMNS.contains(fieldSchema.getColumnName())) {
            throw new BusinessException("系统字段配置不正确: " + fieldSchema.getField());
        }
        if ("id".equals(fieldSchema.getField())) {
            if (!"id".equals(fieldSchema.getColumnName())
                    || !Boolean.TRUE.equals(fieldSchema.getPrimaryKey())
                    || !Boolean.TRUE.equals(fieldSchema.getAutoIncrement())) {
                throw new BusinessException("低代码业务表必须使用 id 自增主键");
            }
        }
        if (!Boolean.TRUE.equals(fieldSchema.getReadonly())) {
            throw new BusinessException("系统字段必须为只读字段: " + fieldSchema.getField());
        }
    }

    private void validateIndexes(LowcodeModelSchema modelSchema, Set<String> modelFields) {
        if (modelSchema.getIndexes() == null) {
            return;
        }
        Set<String> indexNames = new HashSet<>();
        for (LowcodeIndexSchema index : modelSchema.getIndexes()) {
            if (index == null || index.getFields() == null || index.getFields().isEmpty()) {
                continue;
            }
            String indexType = StringUtils.defaultIfBlank(index.getIndexType(), "NORMAL").toUpperCase(Locale.ROOT);
            if (!Set.of("NORMAL", "UNIQUE").contains(indexType)) {
                throw new BusinessException("不支持的索引类型: " + index.getIndexType());
            }
            if (StringUtils.isNotBlank(index.getIndexName()) && !COLUMN_NAME_PATTERN.matcher(index.getIndexName()).matches()) {
                throw new BusinessException("索引名格式不正确: " + index.getIndexName());
            }
            if (StringUtils.isNotBlank(index.getIndexName()) && !indexNames.add(index.getIndexName())) {
                throw new BusinessException("索引名重复: " + index.getIndexName());
            }
            for (String field : index.getFields()) {
                if (!modelFields.contains(field)) {
                    throw new BusinessException("索引引用了不存在的字段: " + field);
                }
                if (BASE_FIELDS.contains(field)) {
                    throw new BusinessException("系统字段不允许作为自定义索引字段: " + field);
                }
            }
        }
    }

    private void validateZone(LowcodePageZone zone, Set<String> modelFields) {
        if (zone == null) {
            throw new BusinessException("页面区域配置不能为空");
        }
        if (StringUtils.isBlank(zone.getZoneKey()) || !ZONE_KEYS.contains(zone.getZoneKey())) {
            throw new BusinessException("不支持的页面区域: " + zone.getZoneKey());
        }
        List<String> refs = zone.getFieldRefs();
        if (refs == null) {
            return;
        }
        for (String ref : refs) {
            if (!modelFields.contains(ref)) {
                throw new BusinessException("页面区域引用了不存在的字段: " + ref);
            }
        }
    }

    private Set<String> buildAllowedPageRefs(Set<String> modelFields, LowcodePageSchema pageSchema) {
        Set<String> pageFields = new HashSet<>(modelFields);
        if (pageSchema == null || pageSchema.getModelRefs() == null) {
            return pageFields;
        }
        for (var modelRef : pageSchema.getModelRefs()) {
            if (modelRef == null || modelRef.getFields() == null) {
                continue;
            }
            String modelCode = StringUtils.trimToEmpty(modelRef.getModelCode());
            for (Map<String, Object> field : modelRef.getFields()) {
                String sourceField = readFieldText(field, "sourceField");
                if (StringUtils.isBlank(sourceField)) {
                    sourceField = readFieldText(field, "field");
                }
                String fieldRef = readFieldText(field, "fieldRef");
                if (StringUtils.isNotBlank(fieldRef)) {
                    pageFields.add(fieldRef);
                }
                if (StringUtils.isNotBlank(sourceField)) {
                    pageFields.add(sourceField);
                }
                String columnName = readFieldText(field, "columnName");
                if (StringUtils.isNotBlank(columnName)) {
                    pageFields.add(columnName);
                }
                if (StringUtils.isNotBlank(modelCode) && StringUtils.isNotBlank(sourceField)) {
                    pageFields.add(modelCode + "." + sourceField);
                    pageFields.add(modelCode + "__" + sourceField);
                }
            }
        }
        return pageFields;
    }

    private String readFieldText(Map<String, Object> field, String key) {
        if (field == null || field.get(key) == null) {
            return null;
        }
        return String.valueOf(field.get(key));
    }

    private void validateTreeRuntime(LowcodeModelSchema modelSchema,
                                     LowcodePageSchema pageSchema,
                                     Set<String> modelFields,
                                     Set<String> modelColumns,
                                     Set<String> pageFields) {
        if (!isTreeRuntime(modelSchema, pageSchema)) {
            return;
        }
        String parentField = resolveTreeParentField(modelSchema, pageSchema);
        if (StringUtils.isBlank(parentField) || !isValidTreeField(parentField, modelFields, modelColumns, pageFields)) {
            throw new BusinessException("树形表必须配置父级字段，请先添加 parentId/pid 等字段或在树形配置中指定父级字段");
        }
        String labelField = resolveTreeLabelField(modelSchema, pageSchema);
        if (StringUtils.isNotBlank(labelField) && !isValidTreeField(labelField, modelFields, modelColumns, pageFields)) {
            throw new BusinessException("树形显示字段不存在: " + labelField);
        }
    }

    private boolean isValidTreeField(String field, Set<String> modelFields, Set<String> modelColumns, Set<String> pageFields) {
        return modelFields.contains(field) || modelColumns.contains(field) || pageFields.contains(field);
    }

    private boolean isTreeRuntime(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        String appType = StringUtils.defaultIfBlank(modelSchema.getAppType(), "SINGLE").toUpperCase(Locale.ROOT);
        return "TREE".equals(appType)
                || (modelSchema.getTreeConfig() != null && Boolean.TRUE.equals(modelSchema.getTreeConfig().getEnabled()))
                || (pageSchema != null && "tree-crud".equals(pageSchema.getLayoutType()))
                || hasPageTreeConfig(pageSchema);
    }

    private boolean hasPageTreeConfig(LowcodePageSchema pageSchema) {
        if (pageSchema == null) {
            return false;
        }
        boolean zoneTree = pageSchema.getZones() != null && pageSchema.getZones().stream()
                .filter(zone -> "table".equals(zone.getZoneKey()))
                .map(LowcodePageZone::getProps)
                .anyMatch(props -> props != null && props.get("treeConfig") instanceof Map<?, ?>);
        if (zoneTree) {
            return true;
        }
        if (pageSchema.getListGridLayout() == null) {
            return false;
        }
        Object items = pageSchema.getListGridLayout().get("items");
        if (!(items instanceof List<?> itemList)) {
            return false;
        }
        return itemList.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .anyMatch(block -> "tree-panel".equals(String.valueOf(block.get("blockType"))));
    }

    private String resolveTreeParentField(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        String parentField = readPageTreeConfig(pageSchema, "parentField");
        parentField = StringUtils.defaultIfBlank(parentField,
                modelSchema.getTreeConfig() != null ? modelSchema.getTreeConfig().getParentField() : null);
        if (StringUtils.isNotBlank(parentField)) {
            return parentField;
        }
        return modelSchema.getFields().stream()
                .map(LowcodeFieldSchema::getField)
                .filter(field -> "parentId".equals(field) || "pid".equals(field) || "parentCode".equals(field))
                .findFirst()
                .orElse("parentId");
    }

    private String resolveTreeLabelField(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        String labelField = readPageTreeConfig(pageSchema, "labelField");
        labelField = StringUtils.defaultIfBlank(labelField,
                modelSchema.getTreeConfig() != null ? modelSchema.getTreeConfig().getLabelField() : null);
        if (StringUtils.isNotBlank(labelField)) {
            return labelField;
        }
        return modelSchema.getFields().stream()
                .map(LowcodeFieldSchema::getField)
                .filter(field -> "name".equals(field) || "title".equals(field) || "label".equals(field))
                .findFirst()
                .orElse(null);
    }

    private String readPageTreeConfig(LowcodePageSchema pageSchema, String key) {
        if (pageSchema == null) {
            return null;
        }
        String zoneValue = pageSchema.getZones() == null ? null : pageSchema.getZones().stream()
                .filter(zone -> "table".equals(zone.getZoneKey()))
                .map(LowcodePageZone::getProps)
                .filter(props -> props != null && props.get("treeConfig") instanceof java.util.Map<?, ?>)
                .map(props -> (java.util.Map<?, ?>) props.get("treeConfig"))
                .map(treeConfig -> treeConfig.get(key))
                .filter(value -> value != null && StringUtils.isNotBlank(String.valueOf(value)))
                .map(String::valueOf)
                .findFirst()
                .orElse(null);
        return StringUtils.defaultIfBlank(zoneValue, readGridTreeConfig(pageSchema, key));
    }

    private String readGridTreeConfig(LowcodePageSchema pageSchema, String key) {
        if (pageSchema == null || pageSchema.getListGridLayout() == null) {
            return null;
        }
        Object items = pageSchema.getListGridLayout().get("items");
        if (!(items instanceof List<?> itemList)) {
            return null;
        }
        for (Object item : itemList) {
            if (!(item instanceof java.util.Map<?, ?> block)) {
                continue;
            }
            if (!"tree-panel".equals(String.valueOf(block.get("blockType")))) {
                continue;
            }
            Object props = block.get("props");
            if (!(props instanceof java.util.Map<?, ?> propMap)) {
                continue;
            }
            Object value = propMap.get(key);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }
}

package com.mdframe.forge.plugin.generator.dto.lowcode;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 单表低代码字段协议。
 */
@Data
public class LowcodeFieldSchema {

    private String field;

    private String columnName;

    private String label;

    private String dataType;

    private Integer length;

    private Integer precision;

    private Boolean required;

    private Object defaultValue;

    private Boolean searchable;

    private Boolean listVisible;

    private Boolean formVisible;

    private String componentType;

    private String queryType;

    private String dictType;

    /** 敏感类型：NONE/PHONE/ID_CARD/EMAIL/BANK_CARD/NAME/ADDRESS/PASSWORD/CUSTOM */
    private String sensitiveType;

    private String encryptAlgorithm;

    private Boolean sortable;

    /** 是否主键字段；低代码业务表固定为 id。 */
    private Boolean primaryKey;

    /** 是否系统字段，系统字段只读展示，不参与业务字段 DDL 追加。 */
    private Boolean systemField;

    /** 是否只读字段，只读字段不能由用户在运行态表单中修改。 */
    private Boolean readonly;

    /** 是否自增字段，当前仅 id 字段固定启用。 */
    private Boolean autoIncrement;

    private Integer width;

    private String remark;

    /** 业务字段类型：TEXT/MONEY/DATE/DICT/REFERENCE 等，面向对象设计器。 */
    private String businessFieldType;

    /** 字段状态：ENABLED/HIDDEN/DISABLED。 */
    private String fieldStatus;

    /** 字段在业务设计器中的排序。 */
    private Integer sortOrder;

    /** 是否允许导入。 */
    private Boolean importable;

    /** 是否允许导出。 */
    private Boolean exportable;

    /** 引用对象编码，仅引用对象字段使用。 */
    private String referenceObjectCode;

    /** 引用对象回显字段，仅引用对象字段使用。 */
    private String referenceDisplayField;

    /** 面向业务设计器的基础组件属性，例如 placeholder、cascade。 */
    private Map<String, Object> basicProps = new LinkedHashMap<>();

    /** 面向开发者模式的扩展属性。 */
    private Map<String, Object> advancedProps = new LinkedHashMap<>();

    /**
     * 公式配置（JSON对象），包含 type / mode / expression / dependsOn / aggregate / condition。
     *
     * 当此字段非空时，表示该字段值由公式计算得出。
     * 发布时由 FormulaPublishValidator 校验；
     * 运行时由 StoredFormulaRuntime / VirtualFormulaRuntime 执行。
     */
    private Map<String, Object> formulaConfig;

    /**
     * 是否引用类字段（对象引用/记录选择器）：选中记录 ID 存主列，
     * 显示名称冗余写入伴随列（{@link #referenceDisplayColumnName()}），回显零关联查询。
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isReferenceField() {
        return (columnName != null && !columnName.isBlank())
                && ((referenceObjectCode != null && !referenceObjectCode.isBlank())
                || "objectReference".equals(componentType)
                || "recordSelector".equals(componentType));
    }

    /**
     * 引用字段的显示名称伴随列：主列名 + "_name"，列名上限 64 字符，超长时截断基础列名。
     */
    public String referenceDisplayColumnName() {
        if (columnName == null || columnName.isBlank()) {
            return null;
        }
        String base = columnName.length() > 59 ? columnName.substring(0, 59) : columnName;
        return base + "_name";
    }

    /**
     * 引用字段伴随列对应的业务字段编码：field + "Name"，与运行时 relationName 渲染约定一致。
     */
    public String referenceDisplayFieldName() {
        if (field == null || field.isBlank()) {
            return null;
        }
        return field + "Name";
    }

    private static final Set<String> SELECTION_LABEL_COMPONENT_TYPES = Set.of(
            "userSelect", "userPicker", "orgTreeSelect", "orgSelect", "departmentSelect",
            "departmentTreeSelect", "deptSelect", "deptTreeSelect"
    );
    /** 配置了动态选项来源时，同样需要冗余保存显示名称的选择类组件 */
    private static final Set<String> OPTION_SOURCE_SELECTION_COMPONENT_TYPES = Set.of(
            "select", "radio", "radioButton", "checkbox", "cascader", "treeSelect", "transfer"
    );
    private static final Set<String> MULTI_SELECT_COMPONENT_TYPES = Set.of(
            "select", "dictSelect", "userSelect", "orgTreeSelect", "objectReference", "recordSelector"
    );
    private static final Set<String> NUMERIC_SELECTION_TYPES = Set.of("bigint", "int", "tinyint");
    public static final int MULTI_SELECT_VARCHAR_LENGTH = 1024;

    /**
     * 需要把显示名称冗余写入伴随列的选择类字段：引用/记录选择器，人员/部门，
     * 以及配置了非静态 optionSource 的下拉等（值存主列，label 存 fieldName，回显零关联查询）。
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isSelectionLabelField() {
        if (isReferenceField()) {
            return true;
        }
        if (columnName != null && !columnName.isBlank()
                && componentType != null
                && SELECTION_LABEL_COMPONENT_TYPES.contains(componentType)) {
            return true;
        }
        return hasDynamicOptionSource()
                && columnName != null && !columnName.isBlank()
                && componentType != null
                && OPTION_SOURCE_SELECTION_COMPONENT_TYPES.contains(componentType);
    }

    /**
     * 是否配置了需要查源才能拿到 label 的动态选项来源（非 STATIC）。
     */
    public boolean hasDynamicOptionSource() {
        if (basicProps == null || basicProps.isEmpty()) {
            return false;
        }
        Object source = basicProps.get("optionSource");
        if (!(source instanceof Map<?, ?> map)) {
            return false;
        }
        Object rawType = map.get("type");
        if (rawType == null) {
            return false;
        }
        String type = String.valueOf(rawType).trim();
        if (type.isEmpty()) {
            return false;
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        // currentChildren / CURRENT_CHILDREN / QUERY_SOURCE / REMOTE 等均视为动态来源
        return !"STATIC".equals(normalized);
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isMultipleSelection() {
        if (booleanProp(basicProps, "multiple")) {
            return true;
        }
        Object selector = basicProps == null ? null : basicProps.get("recordSelector");
        if (selector instanceof Map<?, ?> map) {
            return booleanProp(map, "multiple");
        }
        return false;
    }

    /**
     * 多选统一改为 varchar 逗号分隔存储，避免 bigint 列无法写入多个 ID。
     */
    public void applyMultipleSelectionStorage() {
        if (!isMultipleSelection() || !MULTI_SELECT_COMPONENT_TYPES.contains(String.valueOf(componentType))) {
            return;
        }
        String type = dataType == null ? "" : dataType.toLowerCase(Locale.ROOT);
        if (NUMERIC_SELECTION_TYPES.contains(type) || type.isBlank()) {
            setDataType("varchar");
            if (length == null || length < MULTI_SELECT_VARCHAR_LENGTH) {
                setLength(MULTI_SELECT_VARCHAR_LENGTH);
            }
            return;
        }
        if ("varchar".equals(type) && (length == null || length < MULTI_SELECT_VARCHAR_LENGTH)) {
            setLength(MULTI_SELECT_VARCHAR_LENGTH);
        }
    }

    private static boolean booleanProp(Map<?, ?> source, String key) {
        if (source == null) {
            return false;
        }
        Object value = source.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value instanceof String text && "true".equalsIgnoreCase(text.trim());
    }

    @JsonProperty("fieldCode")
    public void setLegacyFieldCode(String fieldCode) {
        this.columnName = fieldCode;
        if (this.field == null && fieldCode != null) {
            this.field = snakeToCamel(fieldCode);
        }
    }

    @JsonProperty("fieldName")
    public void setLegacyFieldName(String fieldName) {
        this.label = fieldName;
    }

    @JsonProperty("fieldType")
    public void setLegacyFieldType(String fieldType) {
        this.dataType = fieldType == null ? null : fieldType.toLowerCase(Locale.ROOT);
    }

    @JsonProperty("fieldLength")
    public void setLegacyFieldLength(Object fieldLength) {
        if (fieldLength == null) {
            return;
        }
        String value = String.valueOf(fieldLength).trim();
        if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
            return;
        }
        if (value.contains(",")) {
            String[] parts = value.split(",", 2);
            this.length = parseInteger(parts[0], this.length);
            this.precision = parseInteger(parts[1], this.precision);
            return;
        }
        this.length = parseInteger(value, this.length);
    }

    @JsonProperty("displayInList")
    public void setLegacyDisplayInList(Boolean displayInList) {
        this.listVisible = displayInList;
    }

    @JsonProperty("displayInForm")
    public void setLegacyDisplayInForm(Boolean displayInForm) {
        this.formVisible = displayInForm;
    }

    private Integer parseInteger(String value, Integer fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String snakeToCamel(String value) {
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
}

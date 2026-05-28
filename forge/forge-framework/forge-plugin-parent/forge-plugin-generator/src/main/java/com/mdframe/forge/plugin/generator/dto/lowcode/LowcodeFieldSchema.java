package com.mdframe.forge.plugin.generator.dto.lowcode;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Locale;

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

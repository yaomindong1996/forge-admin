package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeIndexSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePolicySchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 低代码模型协议归一化。
 */
@Service
public class LowcodeModelSchemaNormalizer {

    private static final Set<String> SYSTEM_FIELDS = Set.of(
            "id", "tenantId", "createBy", "createTime", "createDept", "updateBy", "updateTime", "delFlag"
    );
    private static final Set<String> SYSTEM_COLUMNS = Set.of(
            "id", "tenant_id", "create_by", "create_time", "create_dept", "update_by", "update_time", "del_flag"
    );
    private static final Set<String> TEXT_TYPES = Set.of("varchar", "char", "text", "longtext");

    public LowcodeModelSchema normalizeModelFields(LowcodeModelSchema modelSchema) {
        return normalizeModelFields(modelSchema, true);
    }

    public LowcodeModelSchema normalizeModelFields(LowcodeModelSchema modelSchema, boolean tenantEnabled) {
        if (modelSchema == null) {
            return null;
        }
        List<LowcodeFieldSchema> businessFields = new ArrayList<>();
        if (modelSchema.getFields() != null) {
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field == null || isSystemField(field)) {
                    continue;
                }
                businessFields.add(normalizeBusinessField(field));
            }
        }

        List<LowcodeFieldSchema> fields = new ArrayList<>();
        fields.add(systemField("id", "id", "ID", "bigint", "number", true, true, true, true, true, false, 100,
                "自增主键，系统生成"));
        if (tenantEnabled) {
            fields.add(systemField("tenantId", "tenant_id", "租户ID", "bigint", "number", true, false, true, false,
                    false, false, 120, "租户隔离字段，系统写入"));
        }
        fields.addAll(businessFields);
        fields.add(systemField("createBy", "create_by", "创建人", "bigint", "number", false, false, true, false,
                false, false, 120, "审计字段，系统写入"));
        fields.add(systemField("createTime", "create_time", "创建时间", "datetime", "datetime", true, false, true, false,
                true, false, 180, "审计字段，系统写入"));
        fields.add(systemField("createDept", "create_dept", "创建部门", "bigint", "number", false, false, true, false,
                false, false, 120, "审计字段，系统写入"));
        fields.add(systemField("updateBy", "update_by", "更新人", "bigint", "number", false, false, true, false,
                false, false, 120, "审计字段，系统写入"));
        fields.add(systemField("updateTime", "update_time", "更新时间", "datetime", "datetime", true, false, true, false,
                true, false, 180, "审计字段，系统写入"));
        fields.add(systemField("delFlag", "del_flag", "删除标志", "char", "input", true, false, true, false,
                false, false, 100, "逻辑删除字段，系统维护"));
        modelSchema.setFields(fields);
        normalizeCollections(modelSchema);
        return modelSchema;
    }

    public boolean isSystemField(LowcodeFieldSchema field) {
        if (field == null) {
            return false;
        }
        String fieldName = field.getField();
        String columnName = field.getColumnName();
        return Boolean.TRUE.equals(field.getSystemField())
                || (fieldName != null && SYSTEM_FIELDS.contains(fieldName))
                || (columnName != null && SYSTEM_COLUMNS.contains(columnName));
    }

    private LowcodeFieldSchema normalizeBusinessField(LowcodeFieldSchema field) {
        String fieldName = StringUtils.trimToNull(field.getField());
        String columnName = StringUtils.trimToNull(field.getColumnName());
        if (StringUtils.isBlank(fieldName) && StringUtils.isNotBlank(columnName)) {
            fieldName = snakeToCamel(columnName);
        }
        if (StringUtils.isBlank(columnName) && StringUtils.isNotBlank(fieldName)) {
            columnName = camelToSnake(fieldName);
        }
        field.setField(fieldName);
        field.setColumnName(columnName);
        field.setLabel(StringUtils.defaultIfBlank(field.getLabel(), StringUtils.defaultIfBlank(fieldName, columnName)));

        String dataType = StringUtils.defaultIfBlank(field.getDataType(), "varchar").toLowerCase(Locale.ROOT);
        field.setDataType(dataType);
        if ("varchar".equals(dataType) && field.getLength() == null) {
            field.setLength(128);
        } else if ("char".equals(dataType) && field.getLength() == null) {
            field.setLength(1);
        } else if ("decimal".equals(dataType) && field.getLength() == null) {
            field.setLength(18);
        }
        if (field.getPrecision() == null) {
            field.setPrecision(2);
        }
        if (field.getRequired() == null) {
            field.setRequired(false);
        }
        if (field.getSearchable() == null) {
            field.setSearchable(false);
        }
        if (field.getListVisible() == null) {
            field.setListVisible(true);
        }
        if (field.getFormVisible() == null) {
            field.setFormVisible(true);
        }
        field.setComponentType(StringUtils.defaultIfBlank(field.getComponentType(), resolveComponentType(dataType)));
        field.setQueryType(StringUtils.defaultIfBlank(field.getQueryType(), TEXT_TYPES.contains(dataType) ? "like" : "eq"));
        field.setDictType(StringUtils.defaultString(field.getDictType()));
        field.setSensitiveType(StringUtils.defaultIfBlank(field.getSensitiveType(), "NONE").toUpperCase(Locale.ROOT));
        field.setEncryptAlgorithm(StringUtils.defaultString(field.getEncryptAlgorithm()));
        if (field.getSortable() == null) {
            field.setSortable(false);
        }
        field.setPrimaryKey(false);
        field.setSystemField(false);
        field.setReadonly(false);
        field.setAutoIncrement(false);
        if (field.getWidth() == null) {
            field.setWidth("datetime".equals(dataType) ? 180 : "text".equals(dataType) || "longtext".equals(dataType) ? 220 : 160);
        }
        field.setRemark(StringUtils.defaultString(field.getRemark()));
        return field;
    }

    private LowcodeFieldSchema systemField(String field, String columnName, String label, String dataType,
                                           String componentType, boolean required, boolean primaryKey,
                                           boolean readonly, boolean searchable, boolean listVisible,
                                           boolean formVisible, int width, String remark) {
        LowcodeFieldSchema schema = new LowcodeFieldSchema();
        schema.setField(field);
        schema.setColumnName(columnName);
        schema.setLabel(label);
        schema.setDataType(dataType);
        schema.setLength("char".equals(dataType) ? 1 : null);
        schema.setPrecision(null);
        schema.setRequired(required);
        schema.setDefaultValue(null);
        schema.setSearchable(searchable);
        schema.setListVisible(listVisible);
        schema.setFormVisible(formVisible);
        schema.setComponentType(componentType);
        schema.setQueryType("eq");
        schema.setDictType(null);
        schema.setSensitiveType("NONE");
        schema.setEncryptAlgorithm(null);
        schema.setPrimaryKey(primaryKey);
        schema.setSystemField(true);
        schema.setReadonly(readonly);
        schema.setAutoIncrement(primaryKey);
        schema.setSortable("id".equals(field) || field.endsWith("Time"));
        schema.setWidth(width);
        schema.setRemark(remark);
        return schema;
    }

    private void normalizeCollections(LowcodeModelSchema modelSchema) {
        if (modelSchema.getRelations() == null) {
            modelSchema.setRelations(new ArrayList<LowcodeRelationSchema>());
        }
        if (modelSchema.getIndexes() == null) {
            modelSchema.setIndexes(new ArrayList<LowcodeIndexSchema>());
        }
        if (modelSchema.getPolicies() == null) {
            modelSchema.setPolicies(new LowcodePolicySchema());
        }
        if (modelSchema.getChildren() == null) {
            modelSchema.setChildren(new ArrayList<>());
        }
    }

    private String resolveComponentType(String dataType) {
        return switch (dataType) {
            case "int", "bigint", "decimal" -> "number";
            case "date", "datetime", "time" -> dataType;
            case "tinyint" -> "switch";
            case "text", "longtext" -> "textarea";
            default -> "input";
        };
    }

    private String snakeToCamel(String value) {
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : StringUtils.defaultString(value).toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }

    private String camelToSnake(String value) {
        return StringUtils.defaultString(value)
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .toLowerCase(Locale.ROOT);
    }
}

package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeModelSchemaNormalizer;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFieldVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 业务字段到低代码字段协议的转换服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessFieldSchemaService {

    private static final Set<String> DICT_FIELD_TYPES = Set.of("DICT", "SELECT", "RADIO", "CHECKBOX", "MULTI_SELECT");
    private static final Set<String> SYSTEM_FIELDS = Set.of(
            "id", "tenantId", "createBy", "createTime", "createDept", "updateBy", "updateTime", "delFlag"
    );

    private static final Map<String, FieldDefaults> FIELD_DEFAULTS = Map.ofEntries(
            Map.entry("TEXT", new FieldDefaults("varchar", "input", 128, 2, "like", 160)),
            Map.entry("MULTILINE", new FieldDefaults("text", "textarea", null, 2, "like", 220)),
            Map.entry("NUMBER", new FieldDefaults("int", "number", 11, 0, "eq", 120)),
            Map.entry("MONEY", new FieldDefaults("decimal", "number", 18, 2, "eq", 140)),
            Map.entry("DATE", new FieldDefaults("date", "date", null, null, "eq", 140)),
            Map.entry("DATETIME", new FieldDefaults("datetime", "datetime", null, null, "eq", 180)),
            Map.entry("DICT", new FieldDefaults("varchar", "select", 64, 2, "eq", 140)),
            Map.entry("SELECT", new FieldDefaults("varchar", "select", 64, 2, "eq", 140)),
            Map.entry("RADIO", new FieldDefaults("varchar", "radio", 64, 2, "eq", 140)),
            Map.entry("CHECKBOX", new FieldDefaults("varchar", "checkbox", 255, 2, "in", 180)),
            Map.entry("MULTI_SELECT", new FieldDefaults("varchar", "checkbox", 255, 2, "in", 180)),
            Map.entry("SWITCH", new FieldDefaults("tinyint", "switch", 1, 0, "eq", 100)),
            Map.entry("FILE", new FieldDefaults("varchar", "fileUpload", 512, 2, "eq", 180)),
            Map.entry("ATTACHMENT", new FieldDefaults("varchar", "fileUpload", 512, 2, "eq", 180)),
            Map.entry("IMAGE", new FieldDefaults("varchar", "imageUpload", 512, 2, "eq", 140)),
            Map.entry("PHONE", new FieldDefaults("varchar", "input", 32, 2, "like", 140)),
            Map.entry("USER", new FieldDefaults("bigint", "userSelect", null, null, "eq", 140)),
            Map.entry("DEPT", new FieldDefaults("bigint", "orgTreeSelect", null, null, "eq", 140)),
            Map.entry("REGION", new FieldDefaults("varchar", "regionTreeSelect", 32, 2, "eq", 160)),
            Map.entry("REFERENCE", new FieldDefaults("bigint", "objectReference", null, null, "eq", 160))
    );

    private final LowcodeModelSchemaNormalizer schemaNormalizer;
    private final BusinessNamingService businessNamingService;

    public LowcodeFieldSchema buildFieldSchema(BusinessFieldDTO dto) {
        if (dto == null) {
            throw new BusinessException("业务字段不能为空");
        }
        String fieldName = StringUtils.trimToNull(dto.getFieldName());
        if (StringUtils.isBlank(fieldName) && StringUtils.isNotBlank(dto.getFieldCode())) {
            fieldName = dto.getFieldCode();
        }
        if (StringUtils.isBlank(fieldName)) {
            throw new BusinessException("字段名称不能为空");
        }

        Map<String, Object> dtoFieldBinding = copyProps(dto.getFieldBinding());
        String bindingFieldCode = mapText(dtoFieldBinding, "fieldCode");
        String bindingColumnName = mapText(dtoFieldBinding, "columnName");
        String fieldType = normalizeFieldType(dto.getFieldType(), fieldName);
        validateFieldTypeOptions(fieldType, dto, dtoFieldBinding);
        FieldDefaults defaults = FIELD_DEFAULTS.getOrDefault(fieldType, FIELD_DEFAULTS.get("TEXT"));

        String fieldCode = normalizeFieldCode(StringUtils.defaultIfBlank(
                StringUtils.defaultIfBlank(dto.getFieldCode(), bindingFieldCode),
                generateFieldCode(fieldName)));
        String columnName = normalizeColumnName(StringUtils.defaultIfBlank(
                StringUtils.defaultIfBlank(dto.getColumnName(), bindingColumnName),
                camelToSnake(fieldCode)));

        LowcodeFieldSchema schema = new LowcodeFieldSchema();
        schema.setField(fieldCode);
        schema.setColumnName(columnName);
        schema.setLabel(fieldName);
        schema.setBusinessFieldType(fieldType);
        schema.setDataType(StringUtils.defaultIfBlank(dto.getDataType(), defaults.dataType()));
        schema.setLength(dto.getLength() == null ? defaults.length() : dto.getLength());
        schema.setPrecision(dto.getPrecision() == null ? defaults.precision() : dto.getPrecision());
        schema.setRequired(Boolean.TRUE.equals(dto.getRequired()));
        schema.setDefaultValue(dto.getDefaultValue());
        schema.setSearchable(Boolean.TRUE.equals(dto.getSearchable()));
        schema.setListVisible(dto.getListVisible() == null || Boolean.TRUE.equals(dto.getListVisible()));
        schema.setFormVisible(dto.getFormVisible() == null || Boolean.TRUE.equals(dto.getFormVisible()));
        schema.setComponentType(StringUtils.defaultIfBlank(dto.getComponentType(), defaults.componentType()));
        schema.setQueryType(StringUtils.defaultIfBlank(dto.getQueryType(), defaults.queryType()));
        schema.setDictType(StringUtils.defaultString(dto.getDictType()));
        schema.setSensitiveType(resolveSensitiveType(fieldType, dto.getSensitiveType()));
        schema.setEncryptAlgorithm(StringUtils.defaultString(dto.getEncryptAlgorithm()));
        schema.setSortable(Boolean.TRUE.equals(dto.getSortable()));
        schema.setPrimaryKey(false);
        schema.setSystemField(Boolean.TRUE.equals(dto.getSystemField()));
        schema.setReadonly(Boolean.TRUE.equals(dto.getReadonly()));
        schema.setAutoIncrement(false);
        schema.setWidth(defaults.width());
        schema.setRemark(StringUtils.defaultIfBlank(dto.getRemark(), fieldName));
        schema.setFieldStatus(StringUtils.defaultIfBlank(dto.getFieldStatus(), "ENABLED").toUpperCase(Locale.ROOT));
        schema.setImportable(dto.getImportable() == null || Boolean.TRUE.equals(dto.getImportable()));
        schema.setExportable(dto.getExportable() == null || Boolean.TRUE.equals(dto.getExportable()));
        schema.setReferenceObjectCode(StringUtils.trimToNull(dto.getReferenceObjectCode()));
        schema.setReferenceDisplayField(StringUtils.trimToNull(dto.getReferenceDisplayField()));
        schema.setSortOrder(dto.getSortOrder());
        schema.setBasicProps(copyProps(dto.getBasicProps()));
        schema.setAdvancedProps(copyProps(dto.getAdvancedProps()));
        schema.getBasicProps().put("fieldBinding", normalizeFieldBinding(dtoFieldBinding, schema, "designer"));
        if (StringUtils.isNotBlank(dto.getPlaceholder())) {
            schema.getBasicProps().put("placeholder", dto.getPlaceholder());
        }
        return schema;
    }

    public LowcodeModelSchema appendField(LowcodeModelSchema modelSchema, BusinessFieldDTO dto) {
        LowcodeModelSchema target = modelSchema == null ? new LowcodeModelSchema() : modelSchema;
        if (target.getFields() == null) {
            target.setFields(new ArrayList<>());
        }
        LowcodeFieldSchema field = buildFieldSchema(dto);
        field.setField(nextUniqueFieldCode(target.getFields(), field.getField()));
        field.setColumnName(nextUniqueColumnName(target.getFields(), field.getColumnName()));
        target.getFields().add(field);
        return schemaNormalizer.normalizeModelFields(target, target.getPolicies() == null
                || StringUtils.isNotBlank(target.getPolicies().getTenantField()));
    }

    public List<BusinessFieldVO> toFieldVOList(LowcodeModelSchema modelSchema) {
        if (modelSchema == null || modelSchema.getFields() == null) {
            return new ArrayList<>();
        }
        return modelSchema.getFields().stream()
                .filter(item -> item != null)
                .sorted(Comparator.comparing(item -> item.getSortOrder() == null ? Integer.MAX_VALUE : item.getSortOrder()))
                .map(this::toFieldVO)
                .toList();
    }

    public BusinessFieldVO toFieldVO(LowcodeFieldSchema schema) {
        BusinessFieldVO vo = new BusinessFieldVO();
        if (schema == null) {
            return vo;
        }
        vo.setFieldName(schema.getLabel());
        vo.setFieldCode(schema.getField());
        vo.setColumnName(schema.getColumnName());
        vo.setFieldType(StringUtils.defaultIfBlank(schema.getBusinessFieldType(), inferFieldType(schema)));
        vo.setDataType(schema.getDataType());
        vo.setLength(schema.getLength());
        vo.setPrecision(schema.getPrecision());
        vo.setRequired(schema.getRequired());
        vo.setDefaultValue(schema.getDefaultValue());
        vo.setSearchable(schema.getSearchable());
        vo.setListVisible(schema.getListVisible());
        vo.setFormVisible(schema.getFormVisible());
        vo.setImportable(schema.getImportable());
        vo.setExportable(schema.getExportable());
        vo.setComponentType(schema.getComponentType());
        vo.setQueryType(schema.getQueryType());
        vo.setDictType(schema.getDictType());
        vo.setSensitiveType(schema.getSensitiveType());
        vo.setEncryptAlgorithm(schema.getEncryptAlgorithm());
        vo.setSortable(schema.getSortable());
        vo.setSystemField(schema.getSystemField());
        vo.setReadonly(schema.getReadonly());
        vo.setFieldStatus(StringUtils.defaultIfBlank(schema.getFieldStatus(), "ENABLED"));
        vo.setReferenceObjectCode(schema.getReferenceObjectCode());
        vo.setReferenceDisplayField(schema.getReferenceDisplayField());
        vo.setSortOrder(schema.getSortOrder());
        vo.setWidth(schema.getWidth());
        vo.setRemark(schema.getRemark());
        vo.setCanDelete(!Boolean.TRUE.equals(schema.getSystemField()) && !SYSTEM_FIELDS.contains(schema.getField()));
        vo.setReferenceStatus("NORMAL");
        vo.setFieldBinding(resolveFieldBinding(schema));
        fillProps(vo, schema);
        return vo;
    }

    public LowcodePageSchema buildDefaultPageSchema(LowcodeModelSchema modelSchema) {
        LowcodePageSchema pageSchema = new LowcodePageSchema();
        pageSchema.setLayoutType("SINGLE");
        pageSchema.setListLayoutMode("standard");
        List<LowcodeFieldSchema> fields = modelSchema == null || modelSchema.getFields() == null
                ? new ArrayList<>()
                : modelSchema.getFields();
        pageSchema.setZones(List.of(
                zone("search", "search-form", enabledFieldRefs(fields, LowcodeFieldSchema::getSearchable)),
                zone("table", "data-table", enabledFieldRefs(fields, LowcodeFieldSchema::getListVisible)),
                zone("edit", "edit-form", enabledFieldRefs(fields, LowcodeFieldSchema::getFormVisible)),
                zone("detail", "detail-view", enabledFieldRefs(fields, LowcodeFieldSchema::getListVisible)),
                zone("toolbar", "table-toolbar", new ArrayList<>())
        ));
        return pageSchema;
    }

    public String generateFieldCode(String fieldName) {
        return businessNamingService.generateFieldCode(fieldName);
    }

    public String camelToSnake(String value) {
        return businessNamingService.camelToSnake(value);
    }

    public String normalizeBusinessFieldCode(String value) {
        return normalizeFieldCode(value);
    }

    public String normalizeBusinessColumnName(String value) {
        return normalizeColumnName(value);
    }

    private void validateFieldTypeOptions(String fieldType, BusinessFieldDTO dto, Map<String, Object> fieldBinding) {
        boolean designerDraftField = "designer".equalsIgnoreCase(StringUtils.defaultString(mapText(fieldBinding, "source")))
                && readBoolean(fieldBinding.get("createIfMissing"), false);
        if (DICT_FIELD_TYPES.contains(fieldType) && StringUtils.isBlank(dto.getDictType()) && !designerDraftField) {
            throw new BusinessException("字典字段必须配置字典类型");
        }
        if ("REFERENCE".equals(fieldType)
                && (StringUtils.isBlank(dto.getReferenceObjectCode())
                || StringUtils.isBlank(dto.getReferenceDisplayField()))
                && !designerDraftField) {
            throw new BusinessException("引用对象字段必须配置目标对象和回显字段");
        }
    }

    private String normalizeFieldType(String fieldType, String fieldName) {
        String normalized = StringUtils.defaultIfBlank(fieldType, "TEXT").trim().toUpperCase(Locale.ROOT);
        if ("TEXTAREA".equals(normalized) || "MULTI_LINE".equals(normalized)) {
            return "MULTILINE";
        }
        if ("SELECT".equals(normalized) || "RADIO".equals(normalized) || "CHECKBOX".equals(normalized)) {
            return normalized;
        }
        if ("客户等级".equals(fieldName) || fieldName.endsWith("状态") || fieldName.endsWith("类型")) {
            return "DICT";
        }
        if ("金额".equals(fieldName) || fieldName.endsWith("金额")) {
            return "MONEY";
        }
        if ("联系电话".equals(fieldName) || fieldName.endsWith("手机号") || fieldName.endsWith("电话")) {
            return "PHONE";
        }
        if ("所属地区".equals(fieldName) || fieldName.endsWith("地区") || fieldName.endsWith("区域")) {
            return "REGION";
        }
        return FIELD_DEFAULTS.containsKey(normalized) ? normalized : "TEXT";
    }

    private String normalizeFieldCode(String value) {
        return businessNamingService.normalizeFieldCode(value, value);
    }

    private String normalizeColumnName(String value) {
        String cleaned = camelToSnake(value);
        if (StringUtils.isBlank(cleaned)) {
            cleaned = "field";
        }
        if (!Character.isLowerCase(cleaned.charAt(0))) {
            cleaned = "field_" + cleaned;
        }
        return StringUtils.left(cleaned, 64);
    }

    private String nextUniqueFieldCode(List<LowcodeFieldSchema> fields, String fieldCode) {
        Set<String> used = new HashSet<>();
        for (LowcodeFieldSchema field : fields) {
            if (field != null && StringUtils.isNotBlank(field.getField())) {
                used.add(field.getField());
            }
        }
        if (!used.contains(fieldCode)) {
            return fieldCode;
        }
        for (int i = 2; i < 1000; i++) {
            String candidate = StringUtils.left(fieldCode, Math.max(1, 64 - String.valueOf(i).length())) + i;
            if (!used.contains(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("字段编码生成失败，请手动调整字段编码");
    }

    private String nextUniqueColumnName(List<LowcodeFieldSchema> fields, String columnName) {
        Set<String> used = new HashSet<>();
        for (LowcodeFieldSchema field : fields) {
            if (field != null && StringUtils.isNotBlank(field.getColumnName())) {
                used.add(field.getColumnName());
            }
        }
        if (!used.contains(columnName)) {
            return columnName;
        }
        for (int i = 2; i < 1000; i++) {
            String suffix = "_" + i;
            String candidate = StringUtils.left(columnName, Math.max(1, 64 - suffix.length())) + suffix;
            if (!used.contains(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("数据库列名生成失败，请手动调整字段编码");
    }

    private LowcodePageZone zone(String zoneKey, String componentKey, List<String> fieldRefs) {
        LowcodePageZone zone = new LowcodePageZone();
        zone.setZoneKey(zoneKey);
        zone.setComponentKey(componentKey);
        zone.setEnabled(true);
        zone.setFieldRefs(fieldRefs);
        zone.setProps(new LinkedHashMap<>());
        return zone;
    }

    private List<String> enabledFieldRefs(List<LowcodeFieldSchema> fields,
                                          java.util.function.Function<LowcodeFieldSchema, Boolean> getter) {
        return fields.stream()
                .filter(item -> item != null && !Boolean.TRUE.equals(item.getSystemField()))
                .filter(item -> !"DISABLED".equalsIgnoreCase(StringUtils.defaultString(item.getFieldStatus())))
                .filter(item -> !"HIDDEN".equalsIgnoreCase(StringUtils.defaultString(item.getFieldStatus())))
                .filter(item -> Boolean.TRUE.equals(getter.apply(item)))
                .map(LowcodeFieldSchema::getField)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    private String resolveSensitiveType(String fieldType, String sensitiveType) {
        if (StringUtils.isNotBlank(sensitiveType)) {
            return sensitiveType.toUpperCase(Locale.ROOT);
        }
        return "PHONE".equals(fieldType) ? "PHONE" : "NONE";
    }

    private String inferFieldType(LowcodeFieldSchema schema) {
        if (StringUtils.isNotBlank(schema.getReferenceObjectCode())) {
            return "REFERENCE";
        }
        if (StringUtils.isNotBlank(schema.getDictType())) {
            return "DICT";
        }
        String component = StringUtils.defaultString(schema.getComponentType()).toLowerCase(Locale.ROOT);
        String dataType = StringUtils.defaultString(schema.getDataType()).toLowerCase(Locale.ROOT);
        if ("textarea".equals(component)) {
            return "MULTILINE";
        }
        if ("decimal".equals(dataType)) {
            return "MONEY";
        }
        if ("date".equals(component)) {
            return "DATE";
        }
        if ("datetime".equals(component)) {
            return "DATETIME";
        }
        if ("regiontreeselect".equals(component)) {
            return "REGION";
        }
        if ("userselect".equals(component)) {
            return "USER";
        }
        if ("orgtreeselect".equals(component)) {
            return "DEPT";
        }
        return "TEXT";
    }

    private void fillProps(BusinessFieldVO vo, LowcodeFieldSchema schema) {
        Map<String, Object> basicProps = vo.getBasicProps();
        basicProps.putAll(copyProps(schema.getBasicProps()));
        basicProps.put("required", schema.getRequired());
        basicProps.put("defaultValue", schema.getDefaultValue());
        basicProps.put("searchable", schema.getSearchable());
        basicProps.put("listVisible", schema.getListVisible());
        basicProps.put("formVisible", schema.getFormVisible());
        basicProps.put("importable", schema.getImportable());
        basicProps.put("exportable", schema.getExportable());

        Map<String, Object> advancedProps = vo.getAdvancedProps();
        advancedProps.putAll(copyProps(schema.getAdvancedProps()));
        advancedProps.put("fieldCode", schema.getField());
        advancedProps.put("columnName", schema.getColumnName());
        advancedProps.put("dataType", schema.getDataType());
        advancedProps.put("length", schema.getLength());
        advancedProps.put("precision", schema.getPrecision());
        advancedProps.put("dictType", schema.getDictType());
        advancedProps.put("sensitiveType", schema.getSensitiveType());
        advancedProps.put("encryptAlgorithm", schema.getEncryptAlgorithm());
    }

    private Map<String, Object> resolveFieldBinding(LowcodeFieldSchema schema) {
        Map<String, Object> basicProps = schema.getBasicProps();
        Object binding = basicProps == null ? null : basicProps.get("fieldBinding");
        if (binding instanceof Map<?, ?> map) {
            Map<String, Object> source = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                if (key != null) {
                    source.put(String.valueOf(key), value);
                }
            });
            return normalizeFieldBinding(source, schema, StringUtils.defaultIfBlank(mapText(source, "source"), "field_asset"));
        }
        return normalizeFieldBinding(new LinkedHashMap<>(), schema, "field_asset");
    }

    private Map<String, Object> normalizeFieldBinding(Map<String, Object> source,
                                                      LowcodeFieldSchema schema,
                                                      String defaultSource) {
        Map<String, Object> binding = new LinkedHashMap<>();
        String fieldCode = StringUtils.defaultIfBlank(mapText(source, "fieldCode"), schema.getField());
        binding.put("mode", StringUtils.defaultIfBlank(mapText(source, "mode"), "field"));
        binding.put("fieldCode", fieldCode);
        binding.put("columnName", StringUtils.defaultIfBlank(mapText(source, "columnName"), schema.getColumnName()));
        binding.put("createIfMissing", readBoolean(source.get("createIfMissing"), false));
        binding.put("source", StringUtils.defaultIfBlank(mapText(source, "source"), defaultSource));
        binding.put("locked", readBoolean(source.get("locked"),
                Boolean.TRUE.equals(schema.getReadonly()) || Boolean.TRUE.equals(schema.getSystemField())));
        return binding;
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

    private String mapText(Map<String, Object> source, String key) {
        if (source == null || !source.containsKey(key) || source.get(key) == null) {
            return null;
        }
        return StringUtils.trimToNull(String.valueOf(source.get(key)));
    }

    private Map<String, Object> copyProps(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private record FieldDefaults(String dataType, String componentType, Integer length,
                                 Integer precision, String queryType, Integer width) {
    }
}

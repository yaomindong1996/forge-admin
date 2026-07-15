package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeDomain;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDataModelDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAuditStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeLogicDeleteStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelImportRequest;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeObjectSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeSourceTableRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTenantStrategy;
import com.mdframe.forge.plugin.generator.service.IGenDatasourceService;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 低代码模型导入服务。新流程只读取数据源表结构，不从旧 GenTable 选择。
 */
@Service
@RequiredArgsConstructor
public class LowcodeModelImportService {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,63}$");
    private static final Pattern LENGTH_PATTERN = Pattern.compile("\\((\\d+)");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("\\((\\d+)\\s*,\\s*(\\d+)\\)");
    private static final Set<String> AUDIT_COLUMNS = Set.of(
            "id", "tenant_id", "create_by", "create_time", "create_dept", "update_by", "update_time", "del_flag"
    );

    private final IGenDatasourceService datasourceService;
    private final LowcodeDomainService domainService;
    private final LowcodeDataModelService modelService;
    private final LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver;

    public LowcodeModelSchema previewDbTableModel(LowcodeModelImportRequest request) {
        ImportContext context = resolveContext(request);
        LowcodeModelSchema schema = new LowcodeModelSchema();
        schema.setSchemaVersion(2);
        schema.setAppType("SINGLE");
        schema.setTableMode("EXISTING");
        schema.setTableName(context.table().getTableName());
        schema.setBusinessName(context.modelName());
        schema.setDomain(buildDomainRef(context.domain()));
        schema.setObject(buildObject(context.modelCode(), context.modelName(), context.request().getModelDesc()));
        schema.setSourceTable(buildSourceTable(context.datasource(), context.table()));
        schema.setRuntimeDatasource(runtimeDataSourceResolver.buildSnapshot(
                context.datasource(), context.table().getTableName(), "EXISTING"));
        schema.setPrimaryKey(buildPrimaryKey(context.columns()));
        schema.setTenantStrategy(buildTenantStrategy(context.columns()));
        schema.setAuditStrategy(buildAuditStrategy(context.columns()));
        schema.setLogicDeleteStrategy(buildLogicDeleteStrategy(context.columns()));
        schema.setFields(context.columns().stream()
                .filter(column -> !isAuditColumn(column.getColumnName()))
                .map(this::toFieldSchema)
                .toList());
        return schema;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long importDbTableModel(LowcodeModelImportRequest request) {
        ImportContext context = resolveContext(request);
        LowcodeModelSchema schema = previewDbTableModel(request);
        LowcodeDataModelDTO dto = new LowcodeDataModelDTO();
        dto.setDomainId(context.domain().getId());
        dto.setModelCode(context.modelCode());
        dto.setModelName(context.modelName());
        dto.setModelDesc(StringUtils.defaultIfBlank(request.getModelDesc(), context.table().getTableComment()));
        dto.setStatus("ENABLED");
        dto.setTenantEnabled(request.getTenantEnabled() == null || request.getTenantEnabled());
        dto.setMasterData(Boolean.TRUE.equals(request.getMasterData()));
        dto.setModelSchema(schema);
        dto.setSyncDdl(false);
        dto.setConfirmSyncDdl(false);
        return modelService.saveModel(dto);
    }

    private ImportContext resolveContext(LowcodeModelImportRequest request) {
        if (request == null) {
            throw new BusinessException("导入请求不能为空");
        }
        if (request.getDatasourceId() == null) {
            throw new BusinessException("数据源不能为空");
        }
        String tableName = StringUtils.trimToNull(request.getTableName());
        if (tableName == null || !TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new BusinessException("数据表名格式不正确");
        }
        AiLowcodeDomain domain = domainService.requireEnabledDomain(request.getDomainId());
        GenDatasource datasource = datasourceService.getById(request.getDatasourceId());
        if (datasource == null || datasource.getIsEnabled() == null || datasource.getIsEnabled() != 1) {
            throw new BusinessException("数据源不存在或已禁用");
        }
        GenTable table = datasourceService.selectDbTableByName(request.getDatasourceId(), tableName);
        if (table == null) {
            throw new BusinessException("数据表不存在: " + tableName);
        }
        List<GenTableColumn> columns = datasourceService.selectDbTableColumnsByName(request.getDatasourceId(), tableName);
        if (columns == null || columns.isEmpty()) {
            throw new BusinessException("数据表没有可导入字段: " + tableName);
        }
        String modelCode = normalizeObjectCode(StringUtils.defaultIfBlank(request.getModelCode(), tableName));
        String modelName = StringUtils.defaultIfBlank(request.getModelName(),
                StringUtils.defaultIfBlank(table.getTableComment(), tableName));
        return new ImportContext(request, domain, datasource, table, columns, modelCode, modelName);
    }

    private LowcodeDomainRef buildDomainRef(AiLowcodeDomain domain) {
        LowcodeDomainRef ref = new LowcodeDomainRef();
        ref.setId(domain.getId());
        ref.setCode(domain.getDomainCode());
        ref.setName(domain.getDomainName());
        return ref;
    }

    private LowcodeObjectSchema buildObject(String modelCode, String modelName, String desc) {
        LowcodeObjectSchema object = new LowcodeObjectSchema();
        object.setCode(modelCode);
        object.setName(modelName);
        object.setDescription(StringUtils.trimToNull(desc));
        return object;
    }

    private LowcodeSourceTableRef buildSourceTable(GenDatasource datasource, GenTable table) {
        LowcodeSourceTableRef ref = new LowcodeSourceTableRef();
        ref.setDatasourceId(datasource.getDatasourceId());
        ref.setDatasourceCode(datasource.getDatasourceCode());
        ref.setDatasourceName(datasource.getDatasourceName());
        ref.setDbType(datasource.getDbType());
        ref.setTableName(table.getTableName());
        ref.setTableComment(table.getTableComment());
        return ref;
    }

    private LowcodePrimaryKeyStrategy buildPrimaryKey(List<GenTableColumn> columns) {
        List<GenTableColumn> primaryKeys = columns.stream()
                .filter(column -> column.getIsPk() != null && column.getIsPk() == 1)
                .toList();
        if (primaryKeys.isEmpty()) {
            throw new BusinessException("导入低代码模型要求数据表存在单字段主键");
        }
        if (primaryKeys.size() > 1) {
            throw new BusinessException("暂不支持复合主键表作为可写低代码模型");
        }
        GenTableColumn column = primaryKeys.get(0);
        LowcodePrimaryKeyStrategy primaryKey = new LowcodePrimaryKeyStrategy();
        primaryKey.setField(normalizeFieldName(StringUtils.defaultIfBlank(column.getJavaField(), column.getColumnName())));
        primaryKey.setColumnName(column.getColumnName());
        primaryKey.setDataType(mapDataType(column.getColumnType(), column.getJavaType()));
        primaryKey.setAutoIncrement(column.getIsIncrement() != null && column.getIsIncrement() == 1);
        return primaryKey;
    }

    private LowcodeTenantStrategy buildTenantStrategy(List<GenTableColumn> columns) {
        LowcodeTenantStrategy strategy = new LowcodeTenantStrategy();
        if (hasColumn(columns, "tenant_id")) {
            strategy.setMode("FORGE_TENANT_ID");
            strategy.setColumnName("tenant_id");
        } else {
            strategy.setMode("NONE");
        }
        return strategy;
    }

    private LowcodeAuditStrategy buildAuditStrategy(List<GenTableColumn> columns) {
        LowcodeAuditStrategy strategy = new LowcodeAuditStrategy();
        if (hasColumn(columns, "create_by")
                && hasColumn(columns, "create_time")
                && hasColumn(columns, "create_dept")
                && hasColumn(columns, "update_by")
                && hasColumn(columns, "update_time")) {
            strategy.setMode("FORGE_COLUMNS");
            strategy.setCreateByColumn("create_by");
            strategy.setCreateTimeColumn("create_time");
            strategy.setCreateDeptColumn("create_dept");
            strategy.setUpdateByColumn("update_by");
            strategy.setUpdateTimeColumn("update_time");
        } else {
            strategy.setMode("NONE");
        }
        return strategy;
    }

    private LowcodeLogicDeleteStrategy buildLogicDeleteStrategy(List<GenTableColumn> columns) {
        LowcodeLogicDeleteStrategy strategy = new LowcodeLogicDeleteStrategy();
        if (hasColumn(columns, "del_flag")) {
            strategy.setMode("DEL_FLAG");
            strategy.setColumnName("del_flag");
            strategy.setActiveValue("0");
            strategy.setDeletedValue("1");
        } else {
            strategy.setMode("NONE");
        }
        return strategy;
    }

    private boolean hasColumn(List<GenTableColumn> columns, String columnName) {
        return columns.stream()
                .anyMatch(column -> StringUtils.equalsIgnoreCase(column.getColumnName(), columnName));
    }

    private LowcodeFieldSchema toFieldSchema(GenTableColumn column) {
        String fieldName = normalizeFieldName(StringUtils.defaultIfBlank(column.getJavaField(), column.getColumnName()));
        String dataType = mapDataType(column.getColumnType(), column.getJavaType());
        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField(fieldName);
        field.setColumnName(column.getColumnName());
        field.setLabel(StringUtils.defaultIfBlank(column.getColumnComment(), fieldName));
        field.setBusinessFieldType(inferBusinessFieldType(dataType));
        field.setDataType(dataType);
        field.setLength(resolveLength(column.getColumnType(), dataType));
        field.setPrecision(resolvePrecision(column.getColumnType(), dataType));
        field.setRequired(column.getIsRequired() != null && column.getIsRequired() == 1);
        field.setSearchable(Boolean.TRUE.equals(isSearchField(fieldName, column.getColumnName())));
        field.setListVisible(true);
        field.setFormVisible(column.getIsPk() == null || column.getIsPk() != 1);
        field.setComponentType(inferComponent(fieldName, column.getColumnName(), column.getColumnComment(), dataType));
        field.setQueryType("varchar".equals(dataType) || "text".equals(dataType) ? "like" : "eq");
        field.setDictType(StringUtils.trimToEmpty(column.getDictType()));
        field.setSensitiveType(inferSensitiveType(fieldName, column.getColumnName()));
        field.setEncryptAlgorithm(null);
        field.setSortable(fieldName.endsWith("Time") || "id".equals(fieldName));
        field.setPrimaryKey(column.getIsPk() != null && column.getIsPk() == 1);
        field.setSystemField(false);
        field.setReadonly(column.getIsPk() != null && column.getIsPk() == 1);
        field.setAutoIncrement(column.getIsIncrement() != null && column.getIsIncrement() == 1);
        field.setWidth(defaultWidth(dataType));
        field.setRemark(column.getColumnComment());
        return field;
    }

    private boolean isAuditColumn(String columnName) {
        return AUDIT_COLUMNS.contains(StringUtils.defaultString(columnName).toLowerCase(Locale.ROOT));
    }

    private Boolean isSearchField(String fieldName, String columnName) {
        String name = (StringUtils.defaultString(fieldName) + " " + StringUtils.defaultString(columnName)).toLowerCase(Locale.ROOT);
        return name.contains("name") || name.contains("code") || name.contains("status") || name.contains("type");
    }

    private String mapDataType(String columnType, String javaType) {
        String type = StringUtils.defaultString(columnType).toLowerCase(Locale.ROOT);
        if (type.startsWith("bigint")) {
            return "bigint";
        }
        if (type.startsWith("int") || type.startsWith("smallint") || type.startsWith("mediumint")) {
            return "int";
        }
        if (type.startsWith("tinyint")) {
            return "tinyint";
        }
        if (type.startsWith("char")) {
            return "char";
        }
        if (type.startsWith("varchar") || type.startsWith("nvarchar") || type.startsWith("varchar2")) {
            return "varchar";
        }
        if (type.startsWith("decimal") || type.startsWith("numeric") || type.startsWith("number")
                || type.startsWith("double") || type.startsWith("float")) {
            return "decimal";
        }
        if (type.startsWith("datetime") || type.startsWith("timestamp")) {
            return "datetime";
        }
        if (type.startsWith("date")) {
            return "date";
        }
        if (type.startsWith("time")) {
            return "time";
        }
        if (type.contains("longtext")) {
            return "longtext";
        }
        if (type.contains("text")) {
            return "text";
        }
        if ("Long".equals(javaType)) {
            return "bigint";
        }
        if ("Integer".equals(javaType)) {
            return "int";
        }
        return "varchar";
    }

    private Integer resolveLength(String columnType, String dataType) {
        Matcher matcher = LENGTH_PATTERN.matcher(StringUtils.defaultString(columnType));
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        if ("varchar".equals(dataType)) {
            return 128;
        }
        if ("decimal".equals(dataType)) {
            return 18;
        }
        return null;
    }

    private Integer resolvePrecision(String columnType, String dataType) {
        if (!"decimal".equals(dataType)) {
            return null;
        }
        Matcher matcher = DECIMAL_PATTERN.matcher(StringUtils.defaultString(columnType));
        return matcher.find() ? Integer.parseInt(matcher.group(2)) : 2;
    }

    private String inferBusinessFieldType(String dataType) {
        return switch (StringUtils.defaultString(dataType)) {
            case "decimal" -> "MONEY";
            case "int", "bigint" -> "NUMBER";
            case "tinyint" -> "SWITCH";
            case "date" -> "DATE";
            case "datetime", "time" -> "DATETIME";
            case "text", "longtext" -> "MULTILINE";
            default -> "TEXT";
        };
    }

    private String inferComponent(String fieldName, String columnName, String comment, String dataType) {
        String text = (StringUtils.defaultString(fieldName) + " "
                + StringUtils.defaultString(columnName) + " "
                + StringUtils.defaultString(comment)).toLowerCase(Locale.ROOT);
        if (text.contains("图片") || text.contains("头像")) {
            return "imageUpload";
        }
        if (text.contains("附件") || text.contains("文件")) {
            return "fileUpload";
        }
        if ("date".equals(dataType)) {
            return "date";
        }
        if ("datetime".equals(dataType)) {
            return "datetime";
        }
        if ("int".equals(dataType) || "bigint".equals(dataType) || "decimal".equals(dataType)) {
            return "number";
        }
        if ("tinyint".equals(dataType)) {
            return "switch";
        }
        if ("text".equals(dataType) || text.contains("备注") || text.contains("描述")) {
            return "textarea";
        }
        return "input";
    }

    private String inferSensitiveType(String fieldName, String columnName) {
        String text = (StringUtils.defaultString(fieldName) + " " + StringUtils.defaultString(columnName)).toLowerCase(Locale.ROOT);
        if (text.contains("phone") || text.contains("mobile")) {
            return "PHONE";
        }
        if (text.contains("email")) {
            return "EMAIL";
        }
        if (text.contains("id_card") || text.contains("idcard")) {
            return "ID_CARD";
        }
        return "NONE";
    }

    private Integer defaultWidth(String dataType) {
        if ("datetime".equals(dataType)) {
            return 180;
        }
        if ("date".equals(dataType)) {
            return 140;
        }
        if ("int".equals(dataType) || "bigint".equals(dataType) || "decimal".equals(dataType)) {
            return 120;
        }
        return 160;
    }

    private String normalizeFieldName(String value) {
        String normalized = StringUtils.defaultString(value)
                .replaceAll("[^A-Za-z0-9_]", "_")
                .replaceAll("_+", "_");
        if (normalized.contains("_")) {
            String[] parts = normalized.toLowerCase(Locale.ROOT).split("_");
            StringBuilder builder = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                if (parts[i].isEmpty()) {
                    continue;
                }
                builder.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            }
            normalized = builder.toString();
        }
        if (StringUtils.isBlank(normalized)) {
            normalized = "field";
        }
        if (!Character.isLetter(normalized.charAt(0))) {
            normalized = "field" + normalized;
        }
        return normalized;
    }

    private String normalizeObjectCode(String value) {
        String normalized = StringUtils.defaultString(value)
                .trim()
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .toLowerCase(Locale.ROOT)
                .replaceAll("^[^a-z]+", "")
                .replaceAll("_+$", "");
        if (StringUtils.isBlank(normalized)) {
            normalized = "model";
        }
        if (normalized.length() > 48) {
            normalized = normalized.substring(0, 48).replaceAll("_+$", "");
        }
        return normalized;
    }

    private record ImportContext(LowcodeModelImportRequest request,
                                 AiLowcodeDomain domain,
                                 GenDatasource datasource,
                                 GenTable table,
                                 List<GenTableColumn> columns,
                                 String modelCode,
                                 String modelName) {
    }
}

package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeIndexSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialect.DdlColumn;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialect.IndexDefinition;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialectFactory;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDdlPreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 低代码单表受控 DDL 生成与执行。
 */
@Service
@RequiredArgsConstructor
public class LowcodeDdlService {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$");
    private static final Set<String> INDEXABLE_TYPES = Set.of(
            "varchar", "char", "int", "bigint", "decimal", "date", "datetime", "time", "tinyint"
    );

    private final LowcodeSchemaValidator schemaValidator;
    private final LowcodeDdlRepository ddlRepository;
    private final DynamicCrudRepository dynamicCrudRepository;
    private final LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver;
    private final RuntimeDatabaseDialectFactory dialectFactory;

    public LowcodeDdlPreviewVO previewCreateTable(LowcodeModelSchema modelSchema) {
        schemaValidator.validateModel(modelSchema);
        LowcodeRuntimeDataSourceContext context = runtimeDataSourceResolver.resolve(modelSchema);
        RuntimeDatabaseDialect dialect = dialectFactory.resolve(context);
        LowcodeDdlPreviewVO preview = new LowcodeDdlPreviewVO();
        preview.setTableName(context.getTableName());

        boolean tableExists = ddlRepository.tableExists(context, context.getTableName());
        preview.setTableExists(tableExists);
        if (!tableExists) {
            if (!context.isAllowDdl()) {
                preview.setExecutable(false);
                preview.getWarnings().add("运行数据源不允许在线DDL，请先在目标库创建数据表");
                preview.getDdlStatements().addAll(buildCreateTableSql(context, modelSchema, dialect, preview.getWarnings()));
                return preview;
            }
            preview.getDdlStatements().addAll(buildCreateTableSql(context, modelSchema, dialect, preview.getWarnings()));
        } else {
            if (!ddlRepository.hasSinglePrimaryKey(context, context.getTableName())) {
                preview.setExecutable(false);
                preview.getWarnings().add("已有表必须包含单字段主键后才能绑定为可写低代码模型");
                return preview;
            }
            preview.getDdlStatements().addAll(buildAddColumnSql(context, modelSchema, dialect, preview.getWarnings()));
            if (!preview.getDdlStatements().isEmpty() && !context.isAllowDdl()) {
                preview.setExecutable(false);
                preview.getWarnings().add("运行数据源不允许在线DDL，请由数据库管理员手工同步表结构");
                return preview;
            }
        }
        preview.setExecutable(true);
        if (preview.getDdlStatements().isEmpty()) {
            preview.getWarnings().add("数据表已存在，且没有需要追加的业务字段");
        }
        return preview;
    }

    @Transactional(rollbackFor = Exception.class)
    public void executeCreateTable(LowcodeModelSchema modelSchema) {
        LowcodeDdlPreviewVO preview = previewCreateTable(modelSchema);
        if (!Boolean.TRUE.equals(preview.getExecutable())) {
            throw new BusinessException("DDL预览不可执行");
        }
        LowcodeRuntimeDataSourceContext context = runtimeDataSourceResolver.resolve(modelSchema);
        if (!context.isAllowDdl()) {
            throw new BusinessException("运行数据源不允许在线DDL");
        }
        for (String ddl : preview.getDdlStatements()) {
            assertSafeDdl(ddl);
        }
        for (String ddl : preview.getDdlStatements()) {
            ddlRepository.executeDdl(context, ddl);
        }
        dynamicCrudRepository.clearTableMetadataCache(context, context.getTableName());
    }

    /**
     * 判断预览结果是否包含在线发布不允许执行的非追加式 DDL。
     *
     * <p>该策略与最终执行白名单共用同一判断，避免发布检查允许、执行阶段又拒绝。</p>
     */
    public boolean containsUnsafeOnlineDdl(List<String> statements) {
        if (statements == null) {
            return false;
        }
        return statements.stream()
                .filter(StringUtils::isNotBlank)
                .anyMatch(statement -> !isSafeOnlineDdl(statement));
    }

    public boolean tableExists(String tableName) {
        validateIdentifier(tableName, "表名");
        return ddlRepository.tableExists(tableName);
    }

    public boolean tableExists(LowcodeModelSchema modelSchema) {
        LowcodeRuntimeDataSourceContext context = runtimeDataSourceResolver.resolve(modelSchema);
        validateIdentifier(context.getTableName(), "表名");
        return ddlRepository.tableExists(context, context.getTableName());
    }

    public boolean hasAutoIncrementPrimaryId(String tableName) {
        validateIdentifier(tableName, "表名");
        return ddlRepository.hasAutoIncrementPrimaryId(tableName);
    }

    public boolean hasSinglePrimaryKey(LowcodeModelSchema modelSchema) {
        LowcodeRuntimeDataSourceContext context = runtimeDataSourceResolver.resolve(modelSchema);
        validateIdentifier(context.getTableName(), "表名");
        return ddlRepository.hasSinglePrimaryKey(context, context.getTableName());
    }

    public Set<String> listColumns(String tableName) {
        validateIdentifier(tableName, "表名");
        return ddlRepository.listColumns(tableName);
    }

    public Set<String> listColumns(LowcodeModelSchema modelSchema) {
        LowcodeRuntimeDataSourceContext context = runtimeDataSourceResolver.resolve(modelSchema);
        validateIdentifier(context.getTableName(), "表名");
        return ddlRepository.listColumns(context, context.getTableName());
    }

    /**
     * 查询模型绑定表的完整列元数据，供对象设计器展示字段映射差异。
     *
     * <p>该方法只读取数据库结构，不生成或执行 DDL。</p>
     */
    public Map<String, LowcodeDdlRepository.ColumnMetadata> listColumnMetadata(LowcodeModelSchema modelSchema) {
        LowcodeRuntimeDataSourceContext context = runtimeDataSourceResolver.resolve(modelSchema);
        validateIdentifier(context.getTableName(), "表名");
        return ddlRepository.listColumnMetadata(context, context.getTableName());
    }

    /**
     * 查询模型绑定表的索引名称，供对象设计器展示同步状态。
     *
     * <p>该方法只读取数据库结构，不生成或执行 DDL。</p>
     */
    public Set<String> listIndexes(LowcodeModelSchema modelSchema) {
        LowcodeRuntimeDataSourceContext context = runtimeDataSourceResolver.resolve(modelSchema);
        validateIdentifier(context.getTableName(), "表名");
        return ddlRepository.listIndexes(context, context.getTableName());
    }

    private List<String> buildCreateTableSql(LowcodeRuntimeDataSourceContext context,
                                             LowcodeModelSchema modelSchema,
                                             RuntimeDatabaseDialect dialect,
                                             List<String> warnings) {
        String tableName = context.getTableName();
        String tableComment = StringUtils.defaultIfBlank(modelSchema.getBusinessName(), tableName);
        List<DdlColumn> columns = new ArrayList<>();
        List<String> definitions = new ArrayList<>();
        DdlColumn primaryColumn = new DdlColumn("id", dialect.resolveSqlType("bigint", 19, "19,0"),
                true, null, null, "主键ID", true);
        columns.add(primaryColumn);
        definitions.add(dialect.columnDefinition(primaryColumn));
        appendSystemColumn(columns, definitions, dialect, "tenant_id", "bigint", true, 1, "租户编号");
        for (LowcodeFieldSchema field : businessFields(modelSchema)) {
            DdlColumn column = buildColumn(field, false, dialect);
            columns.add(column);
            definitions.add(dialect.columnDefinition(column));
        }
        appendSystemColumn(columns, definitions, dialect, "del_flag", "char", true, "0", "删除标志");
        appendSystemColumn(columns, definitions, dialect, "create_by", "bigint", false, null, "创建人ID");
        appendSystemColumn(columns, definitions, dialect, "create_time", "datetime", true, "CURRENT_TIMESTAMP", "创建时间");
        appendSystemColumn(columns, definitions, dialect, "create_dept", "bigint", false, null, "创建部门ID");
        appendSystemColumn(columns, definitions, dialect, "update_by", "bigint", false, null, "更新人ID");
        appendSystemColumn(columns, definitions, dialect, "update_time", "datetime", true, "CURRENT_TIMESTAMP",
                "ON UPDATE CURRENT_TIMESTAMP", "更新时间");
        definitions.add(dialect.primaryKeyConstraint(tableName, "id"));

        List<IndexDefinition> indexes = buildIndexDefinitions(modelSchema, warnings, dialect);
        if (indexes.isEmpty()) {
            warnings.add("未配置显式二级索引；系统不会根据查询字段、关联字段或系统字段自动创建索引");
        }
        for (IndexDefinition index : indexes) {
            String inline = dialect.inlineIndexDefinition(index);
            if (StringUtils.isNotBlank(inline)) {
                definitions.add(inline);
            }
        }

        List<String> ddlList = new ArrayList<>();
        ddlList.add(dialect.createTableSql(tableName, definitions, tableComment));
        ddlList.addAll(dialect.afterCreateTableSql(tableName, tableComment, columns, indexes));
        return ddlList;
    }

    private List<String> buildAddColumnSql(LowcodeRuntimeDataSourceContext context,
                                           LowcodeModelSchema modelSchema,
                                           RuntimeDatabaseDialect dialect,
                                           List<String> warnings) {
        Map<String, LowcodeDdlRepository.ColumnMetadata> columnMetadata =
                ddlRepository.listColumnMetadata(context, context.getTableName());
        Set<String> existingColumns = columnMetadata.keySet();
        List<String> ddlList = new ArrayList<>();
        List<String> addedColumns = new ArrayList<>();
        appendMissingSystemColumns(modelSchema, context.getTableName(), existingColumns, ddlList, dialect);
        for (LowcodeFieldSchema field : businessFields(modelSchema)) {
            if (existingColumns.contains(field.getColumnName())) {
                continue;
            }
            DdlColumn column = buildColumn(field, false, dialect);
            ddlList.add(dialect.addColumnSql(context.getTableName(), column));
            ddlList.addAll(dialect.afterAddColumnSql(context.getTableName(), column));
            addedColumns.add(field.getColumnName());
        }
        if (!addedColumns.isEmpty()) {
            warnings.add("新增字段发布时将追加数据表列: " + String.join("、", addedColumns));
        }
        appendExistingColumnChanges(context.getTableName(), modelSchema, columnMetadata, ddlList, warnings, dialect);
        appendMissingIndexes(modelSchema, context.getTableName(),
                ddlRepository.listIndexes(context, context.getTableName()), ddlList, warnings, dialect);
        if (indexList(modelSchema).isEmpty()) {
            warnings.add("未配置显式二级索引；数据库现有索引保持不变，系统不会自动新增索引");
        }
        warnings.add("已有表在线变更仅追加缺失字段、同步字段长度/类型/是否必填和显式配置的索引，不会删除或重命名字段");
        warnings.add("导入字段会保留数据库现有 NOT NULL；新增列或把可空列收紧为必填时，只有配置默认值才同步为 NOT NULL");
        return ddlList;
    }

    private void appendExistingColumnChanges(String tableName,
                                             LowcodeModelSchema modelSchema,
                                             Map<String, LowcodeDdlRepository.ColumnMetadata> columnMetadata,
                                             List<String> ddlList,
                                             List<String> warnings,
                                             RuntimeDatabaseDialect dialect) {
        for (LowcodeFieldSchema field : businessFields(modelSchema)) {
            validateIdentifier(field.getColumnName(), "字段列名");
            LowcodeDdlRepository.ColumnMetadata metadata = columnMetadata.get(field.getColumnName());
            if (metadata == null) {
                continue;
            }
            if (StringUtils.isNotBlank(metadata.generationExpression())) {
                warnings.add("字段 " + field.getLabel() + " 是生成列，跳过字段长度/类型/是否必填同步");
                continue;
            }
            String dataType = normalizeDataType(field);
            String expectedSqlType = resolveSqlType(field, dataType, dialect);
            boolean typeChanged = !dialect.sameSqlType(expectedSqlType, metadata.columnType());
            boolean currentRequired = "NO".equalsIgnoreCase(metadata.isNullable());
            boolean expectedRequired = shouldUseNotNull(field, dataType)
                    || (currentRequired && Boolean.TRUE.equals(field.getRequired()));
            boolean requiredChanged = currentRequired != expectedRequired;
            if (!typeChanged && !requiredChanged) {
                continue;
            }
            if (typeChanged) {
                warnings.add("字段 " + fieldLabel(field) + " 数据库类型将从 "
                        + metadata.columnType() + " 调整为 " + expectedSqlType);
                if (isPotentialLengthShrink(metadata.columnType(), expectedSqlType)) {
                    warnings.add("字段 " + fieldLabel(field) + " 长度变小，若历史数据超长数据库可能拒绝执行");
                }
            }
            DdlColumn column = typeChanged
                    ? buildColumn(field, false, dialect)
                    : buildExistingColumn(metadata, field, expectedRequired);
            ddlList.addAll(dialect.modifyColumnSql(tableName, column));
        }
    }

    private void appendMissingSystemColumns(LowcodeModelSchema modelSchema, String tableName,
                                            Set<String> existingColumns, List<String> ddlList,
                                            RuntimeDatabaseDialect dialect) {
        if (usesForgeTenant(modelSchema)) {
            appendMissingColumn(tableName, existingColumns, ddlList, tenantColumn(modelSchema),
                    systemColumn(dialect, tenantColumn(modelSchema), "bigint", true, 1, "租户编号"),
                    dialect);
        }
        if (usesLogicDelete(modelSchema)) {
            appendMissingColumn(tableName, existingColumns, ddlList, logicDeleteColumn(modelSchema),
                    systemColumn(dialect, logicDeleteColumn(modelSchema), "char", true, "0", "删除标志"),
                    dialect);
        }
        if (usesForgeAudit(modelSchema)) {
            appendMissingColumn(tableName, existingColumns, ddlList, "create_by",
                    systemColumn(dialect, "create_by", "bigint", false, null, "创建人ID"),
                    dialect);
            appendMissingColumn(tableName, existingColumns, ddlList, "create_time",
                    systemColumn(dialect, "create_time", "datetime", true, "CURRENT_TIMESTAMP", "创建时间"),
                    dialect);
            appendMissingColumn(tableName, existingColumns, ddlList, "create_dept",
                    systemColumn(dialect, "create_dept", "bigint", false, null, "创建部门ID"),
                    dialect);
            appendMissingColumn(tableName, existingColumns, ddlList, "update_by",
                    systemColumn(dialect, "update_by", "bigint", false, null, "更新人ID"),
                    dialect);
            appendMissingColumn(tableName, existingColumns, ddlList, "update_time",
                    systemColumn(dialect, "update_time", "datetime", true, "CURRENT_TIMESTAMP",
                            "ON UPDATE CURRENT_TIMESTAMP", "更新时间"),
                    dialect);
        }
    }

    private void appendMissingColumn(String tableName, Set<String> existingColumns, List<String> ddlList,
                                     String columnName, DdlColumn column, RuntimeDatabaseDialect dialect) {
        if (!existingColumns.contains(columnName)) {
            ddlList.add(dialect.addColumnSql(tableName, column));
            ddlList.addAll(dialect.afterAddColumnSql(tableName, column));
        }
    }

    private boolean usesForgeTenant(LowcodeModelSchema modelSchema) {
        String mode = modelSchema.getTenantStrategy() == null ? null : modelSchema.getTenantStrategy().getMode();
        return "FORGE_TENANT_ID".equalsIgnoreCase(StringUtils.defaultIfBlank(mode, "FORGE_TENANT_ID"));
    }

    private String tenantColumn(LowcodeModelSchema modelSchema) {
        return modelSchema.getTenantStrategy() == null
                ? "tenant_id"
                : StringUtils.defaultIfBlank(modelSchema.getTenantStrategy().getColumnName(), "tenant_id");
    }

    private boolean usesForgeAudit(LowcodeModelSchema modelSchema) {
        String mode = modelSchema.getAuditStrategy() == null ? null : modelSchema.getAuditStrategy().getMode();
        return "FORGE_COLUMNS".equalsIgnoreCase(StringUtils.defaultIfBlank(mode, "FORGE_COLUMNS"));
    }

    private boolean usesLogicDelete(LowcodeModelSchema modelSchema) {
        String mode = modelSchema.getLogicDeleteStrategy() == null ? null : modelSchema.getLogicDeleteStrategy().getMode();
        return "DEL_FLAG".equalsIgnoreCase(StringUtils.defaultIfBlank(mode, "DEL_FLAG"));
    }

    private String logicDeleteColumn(LowcodeModelSchema modelSchema) {
        return modelSchema.getLogicDeleteStrategy() == null
                ? "del_flag"
                : StringUtils.defaultIfBlank(modelSchema.getLogicDeleteStrategy().getColumnName(), "del_flag");
    }

    private List<IndexDefinition> buildIndexDefinitions(LowcodeModelSchema modelSchema,
                                                        List<String> warnings,
                                                        RuntimeDatabaseDialect dialect) {
        Set<String> indexNames = new HashSet<>();
        List<IndexDefinition> definitions = new ArrayList<>();
        for (LowcodeIndexSchema index : indexList(modelSchema)) {
            if (index == null || index.getFields() == null || index.getFields().isEmpty()) {
                continue;
            }
            appendIndexDefinition(definitions, warnings, indexNames, modelSchema, index.getIndexName(),
                    index.getFields(), isUniqueIndex(index), dialect);
        }
        return definitions;
    }

    private void appendMissingIndexes(LowcodeModelSchema modelSchema, String tableName, Set<String> existingIndexNames,
                                      List<String> ddlList, List<String> warnings,
                                      RuntimeDatabaseDialect dialect) {
        Set<String> emitted = new HashSet<>(existingIndexNames);
        for (IndexDefinition index : buildIndexDefinitions(modelSchema, warnings, dialect)) {
            String indexName = index.indexName();
            if (StringUtils.isBlank(indexName) || emitted.contains(indexName.toLowerCase(Locale.ROOT))) {
                continue;
            }
            ddlList.add(dialect.addIndexSql(tableName, index));
            emitted.add(indexName.toLowerCase(Locale.ROOT));
        }
    }

    private void appendIndexDefinition(List<IndexDefinition> definitions, List<String> warnings, Set<String> indexNames,
                                       LowcodeModelSchema modelSchema, String preferredName,
                                       List<String> fieldNames, boolean unique,
                                       RuntimeDatabaseDialect dialect) {
        if (StringUtils.isBlank(preferredName)) {
            warnings.add("索引配置缺少索引名称，已跳过；请在数据结构中显式填写索引名称");
            return;
        }
        List<String> columns = resolveIndexColumns(modelSchema, fieldNames, warnings);
        if (columns.isEmpty()) {
            return;
        }
        String indexName = normalizeIndexName(preferredName, dialect);
        if (!indexNames.add(indexName)) {
            return;
        }
        definitions.add(new IndexDefinition(indexName, columns, unique));
    }

    private List<String> resolveIndexColumns(LowcodeModelSchema modelSchema, List<String> fieldNames, List<String> warnings) {
        Map<String, LowcodeFieldSchema> fieldMap = buildFieldMap(modelSchema);
        Set<String> columns = new LinkedHashSet<>();
        for (String fieldName : fieldNames) {
            LowcodeFieldSchema field = fieldMap.get(fieldName);
            if (field == null || Boolean.TRUE.equals(field.getSystemField())) {
                continue;
            }
            String dataType = normalizeDataType(field);
            if (!INDEXABLE_TYPES.contains(dataType)) {
                warnings.add("字段 " + field.getLabel() + " 类型不适合创建索引");
                continue;
            }
            if ("varchar".equals(dataType) && normalizeLength(field, 255, 1, 2048) > 191) {
                warnings.add("字段 " + field.getLabel() + " 长度超过191，不创建索引");
                continue;
            }
            columns.add(field.getColumnName());
        }
        return new ArrayList<>(columns);
    }

    private Map<String, LowcodeFieldSchema> buildFieldMap(LowcodeModelSchema modelSchema) {
        Map<String, LowcodeFieldSchema> fieldMap = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : businessFields(modelSchema)) {
            fieldMap.put(field.getField(), field);
            fieldMap.put(field.getColumnName(), field);
        }
        return fieldMap;
    }

    private boolean isUniqueIndex(LowcodeIndexSchema index) {
        return Boolean.TRUE.equals(index.getUnique())
                || "UNIQUE".equalsIgnoreCase(StringUtils.defaultString(index.getIndexType()));
    }

    private List<LowcodeIndexSchema> indexList(LowcodeModelSchema modelSchema) {
        if (modelSchema.getIndexes() == null) {
            return List.of();
        }
        return modelSchema.getIndexes().stream()
                .filter(index -> index != null && !Boolean.TRUE.equals(index.getAuto()))
                .toList();
    }

    private String normalizeIndexName(String indexName, RuntimeDatabaseDialect dialect) {
        String normalized = StringUtils.defaultIfBlank(indexName, "idx_lowcode")
                .replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("_+", "_")
                .toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("idx_") && !normalized.startsWith("uk_")) {
            normalized = "idx_" + normalized;
        }
        int maxLength = Math.max(1, dialect.maxIdentifierLength());
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private void appendSystemColumn(List<DdlColumn> columns,
                                    List<String> definitions,
                                    RuntimeDatabaseDialect dialect,
                                    String columnName,
                                    String dataType,
                                    boolean required,
                                    Object defaultValue,
                                    String comment) {
        appendSystemColumn(columns, definitions, dialect, columnName, dataType, required, defaultValue, null, comment);
    }

    private void appendSystemColumn(List<DdlColumn> columns,
                                    List<String> definitions,
                                    RuntimeDatabaseDialect dialect,
                                    String columnName,
                                    String dataType,
                                    boolean required,
                                    Object defaultValue,
                                    String extra,
                                    String comment) {
        DdlColumn column = systemColumn(dialect, columnName, dataType, required, defaultValue, extra, comment);
        columns.add(column);
        definitions.add(dialect.columnDefinition(column));
    }

    private DdlColumn systemColumn(RuntimeDatabaseDialect dialect,
                                   String columnName,
                                   String dataType,
                                   boolean required,
                                   Object defaultValue,
                                   String comment) {
        return systemColumn(dialect, columnName, dataType, required, defaultValue, null, comment);
    }

    private DdlColumn systemColumn(RuntimeDatabaseDialect dialect,
                                   String columnName,
                                   String dataType,
                                   boolean required,
                                   Object defaultValue,
                                   String extra,
                                   String comment) {
        validateIdentifier(columnName, "字段列名");
        int length = "char".equals(dataType) ? 1 : 255;
        String sqlType = dialect.resolveSqlType(dataType, length, "18,2");
        return new DdlColumn(columnName, sqlType, required, defaultValue, extra, comment, false);
    }

    private DdlColumn buildColumn(LowcodeFieldSchema field, boolean forceNullable, RuntimeDatabaseDialect dialect) {
        validateIdentifier(field.getColumnName(), "字段列名");
        String dataType = normalizeDataType(field);
        String sqlType = resolveSqlType(field, dataType, dialect);
        boolean required = !forceNullable && shouldUseNotNull(field, dataType);
        return new DdlColumn(
                field.getColumnName(),
                sqlType,
                required,
                required ? field.getDefaultValue() : null,
                null,
                StringUtils.defaultIfBlank(field.getLabel(), field.getColumnName()),
                false
        );
    }

    private DdlColumn buildExistingColumn(LowcodeDdlRepository.ColumnMetadata metadata,
                                          LowcodeFieldSchema field,
                                          boolean required) {
        validateIdentifier(metadata.columnName(), "字段列名");
        return new DdlColumn(
                metadata.columnName(),
                metadata.columnType(),
                required,
                required ? field.getDefaultValue() : metadata.columnDefault(),
                metadata.extra(),
                metadata.columnComment(),
                false
        );
    }

    private boolean shouldUseNotNull(LowcodeFieldSchema field, String dataType) {
        return Boolean.TRUE.equals(field.getRequired()) && hasUsableDefaultValue(field.getDefaultValue(), dataType);
    }

    private boolean hasUsableDefaultValue(Object defaultValue, String dataType) {
        if (defaultValue == null || Set.of("text", "longtext").contains(dataType)) {
            return false;
        }
        if (defaultValue instanceof String text) {
            return StringUtils.isNotBlank(text);
        }
        return true;
    }

    private String resolveSqlType(LowcodeFieldSchema field, String dataType, RuntimeDatabaseDialect dialect) {
        int length = switch (dataType) {
            case "varchar" -> normalizeLength(field, 255, 1, 2048);
            case "char" -> normalizeLength(field, 1, 1, 255);
            default -> field.getLength() == null ? 0 : field.getLength();
        };
        String decimalPrecision = "decimal".equals(dataType) ? normalizeDecimalPrecision(field) : "18,2";
        try {
            return dialect.resolveSqlType(dataType, length, decimalPrecision);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    private boolean isPotentialLengthShrink(String currentType, String expectedType) {
        Integer currentLength = firstTypeNumber(currentType);
        Integer expectedLength = firstTypeNumber(expectedType);
        return currentLength != null && expectedLength != null && expectedLength < currentLength;
    }

    private Integer firstTypeNumber(String columnType) {
        String value = StringUtils.defaultString(columnType);
        int start = value.indexOf('(');
        int end = value.indexOf(')', start + 1);
        if (start < 0 || end <= start) {
            return null;
        }
        String number = value.substring(start + 1, end).split(",", 2)[0].trim();
        try {
            return Integer.valueOf(number);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeDataType(LowcodeFieldSchema field) {
        return StringUtils.defaultIfBlank(field.getDataType(), "varchar").toLowerCase(Locale.ROOT);
    }

    private int normalizeLength(LowcodeFieldSchema field, int defaultLength, int min, int max) {
        Integer length = field.getLength();
        if (length == null) {
            return defaultLength;
        }
        if (length < min || length > max) {
            throw new BusinessException("字段长度超出允许范围: " + field.getLabel());
        }
        return length;
    }

    private String normalizeDecimalPrecision(LowcodeFieldSchema field) {
        int length = field.getLength() != null ? field.getLength() : 18;
        int precision = field.getPrecision() != null ? field.getPrecision() : 2;
        if (length < 1 || length > 65 || precision < 0 || precision >= length) {
            throw new BusinessException("decimal精度配置不正确: " + field.getLabel());
        }
        return length + "," + precision;
    }

    private void validateIdentifier(String identifier, String label) {
        if (StringUtils.isBlank(identifier) || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new BusinessException(label + "格式不正确: " + identifier);
        }
    }

    private String fieldLabel(LowcodeFieldSchema field) {
        return StringUtils.defaultIfBlank(field.getLabel(), field.getColumnName());
    }

    private void assertSafeDdl(String ddl) {
        if (isSafeOnlineDdl(ddl)) {
            return;
        }
        throw new BusinessException("数据库差异包含高风险或非追加式 DDL（如字段类型、长度或必填属性调整），"
                + "仅允许预览和导出脚本后人工审核执行");
    }

    private boolean isSafeOnlineDdl(String ddl) {
        if (StringUtils.isBlank(ddl)) {
            return false;
        }
        String normalized = ddl.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("CREATE TABLE IF NOT EXISTS") || normalized.startsWith("CREATE TABLE ")) {
            return true;
        }
        if (normalized.startsWith("ALTER TABLE") && normalized.contains(" ADD COLUMN ")) {
            return true;
        }
        if (normalized.startsWith("ALTER TABLE") && normalized.contains(" ADD (")) {
            return true;
        }
        if (normalized.startsWith("ALTER TABLE")
                && (normalized.contains(" ADD KEY ") || normalized.contains(" ADD UNIQUE KEY "))) {
            return true;
        }
        if (normalized.startsWith("CREATE INDEX ") || normalized.startsWith("CREATE UNIQUE INDEX ")) {
            return true;
        }
        if (normalized.startsWith("COMMENT ON TABLE ") || normalized.startsWith("COMMENT ON COLUMN ")) {
            return true;
        }
        return false;
    }

    private List<LowcodeFieldSchema> businessFields(LowcodeModelSchema modelSchema) {
        if (modelSchema.getFields() == null) {
            return List.of();
        }
        return modelSchema.getFields().stream()
                .filter(field -> field != null && !Boolean.TRUE.equals(field.getSystemField()))
                .filter(field -> !"DISABLED".equalsIgnoreCase(StringUtils.defaultString(field.getFieldStatus())))
                .filter(field -> !"HIDDEN".equalsIgnoreCase(StringUtils.defaultString(field.getFieldStatus())))
                .filter(field -> !"id".equals(field.getColumnName())
                        && !"tenant_id".equals(field.getColumnName())
                        && !"create_by".equals(field.getColumnName())
                        && !"create_time".equals(field.getColumnName())
                        && !"create_dept".equals(field.getColumnName())
                        && !"update_by".equals(field.getColumnName())
                        && !"update_time".equals(field.getColumnName())
                        && !"del_flag".equals(field.getColumnName()))
                .toList();
    }
}

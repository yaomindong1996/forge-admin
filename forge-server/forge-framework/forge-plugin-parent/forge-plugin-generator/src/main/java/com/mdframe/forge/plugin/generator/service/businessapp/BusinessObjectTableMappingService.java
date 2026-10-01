package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeIndexSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeDatasourceSnapshot;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlRepository.ColumnMetadata;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectTableFieldMappingVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectTableMappingVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDdlPreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 业务对象表映射、差异预览和显式数据库同步编排。
 */
@Service
@RequiredArgsConstructor
public class BusinessObjectTableMappingService {

    private static final String DDL_PERMISSION = "ai:lowcode:deploy-ddl";
    private static final String DATABASE_SYNC_OPTION_KEY = "databaseSync";
    private static final String MANAGED_BY_PAGE_FORM = "PAGE_FORM";
    private static final Set<String> SYSTEM_COLUMNS = Set.of(
            "id", "tenant_id", "del_flag", "create_by", "create_time",
            "create_dept", "update_by", "update_time"
    );
    private static final Set<String> LOGIC_DELETE_STORAGE_TYPES = Set.of(
            "char", "varchar", "tinyint", "int", "bigint"
    );

    private final BusinessObjectDesignContextProvider contextProvider;
    private final LowcodeDdlService ddlService;
    private final BusinessApplicationObjectMapper applicationObjectMapper;
    private final BusinessObjectMapper objectMapper;

    public BusinessObjectTableMappingVO getTableMapping(Long objectId) {
        BusinessObjectDesignerService.DesignerContext context = contextProvider.loadContext(objectId);
        LowcodeModelSchema modelSchema = requireModelSchema(context);
        BusinessObjectTableMappingVO mapping = baseMapping(context, modelSchema);
        applyLastSync(mapping, context.getObject());
        try {
            LowcodeDdlPreviewVO preview = ddlService.previewCreateTable(modelSchema);
            mapping.setPendingDdlCount(ddlCount(preview));
            boolean tableExists = Boolean.TRUE.equals(preview.getTableExists());
            mapping.setTableExists(tableExists);
            Map<String, ColumnMetadata> columns = tableExists
                    ? safeMap(ddlService.listColumnMetadata(modelSchema)) : Map.of();
            Set<String> indexes = tableExists
                    ? safeSet(ddlService.listIndexes(modelSchema)) : Set.of();
            mapping.setFields(buildFieldMappings(modelSchema, columns, indexes));
            int unsyncedCount = (int) mapping.getFields().stream()
                    .filter(field -> Boolean.TRUE.equals(field.getBlockingDifference()))
                    .count();
            mapping.setUnsyncedChangeCount(unsyncedCount);
            if (!tableExists) {
                mapping.setSyncStatus("TABLE_MISSING");
            } else if (unsyncedCount > 0 || hasDdl(preview)) {
                mapping.setSyncStatus("OUT_OF_SYNC");
            } else {
                mapping.setSyncStatus("IN_SYNC");
            }
        } catch (RuntimeException e) {
            mapping.setTableExists(null);
            mapping.setSyncStatus("CHECK_FAILED");
            mapping.setLastSyncMessage(safeMessage(e));
            mapping.setFields(buildFieldMappings(modelSchema, Map.of(), Set.of()));
            mapping.setUnsyncedChangeCount(mapping.getFields().size());
            mapping.setPendingDdlCount(0);
        }
        return mapping;
    }

    public LowcodeDdlPreviewVO previewDatabaseDiff(Long objectId, Integer designVersion) {
        BusinessObjectDesignerService.DesignerContext context = contextProvider.loadContext(objectId);
        assertDesignVersion(context, designVersion);
        return ddlService.previewCreateTable(requireModelSchema(context));
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncForDebugBundleImport(Long objectId) {
        BusinessObjectDesignerService.DesignerContext context = contextProvider.loadContext(objectId);
        LowcodeModelSchema modelSchema = requireModelSchema(context);
        assertDatasourceAllowsDdl(modelSchema);
        LowcodeDdlPreviewVO preview = ddlService.previewCreateTable(modelSchema);
        if (!Boolean.TRUE.equals(preview.getExecutable())) {
            throw new BusinessException("当前数据源未允许自动建表，请在数据源设置中开启 DDL");
        }
        if (!hasDdl(preview)) {
            persistSyncResult(context, "IN_SYNC", "调试包导入：数据表结构已是最新版本", 0);
            return;
        }
        if (!Boolean.TRUE.equals(preview.getTableExists())) {
            try {
                ddlService.executeCreateTable(modelSchema);
                persistSyncResult(context, "IN_SYNC", "调试包导入：数据表创建成功",
                        preview.getDdlStatements() == null ? 0 : preview.getDdlStatements().size());
            } catch (RuntimeException e) {
                persistSyncResult(context, "FAILED", safeMessage(e),
                        preview.getDdlStatements() == null ? 0 : preview.getDdlStatements().size());
                throw e;
            }
            return;
        }
        int totalDdl = preview.getDdlStatements() == null ? 0 : preview.getDdlStatements().size();
        try {
            int executed = ddlService.executeSafeDdlOnly(modelSchema);
            persistSyncResult(context, executed >= totalDdl ? "IN_SYNC" : "PARTIAL",
                    "调试包导入：已同步 " + executed + " 项安全 DDL", executed);
        } catch (RuntimeException e) {
            persistSyncFailure(context, e, totalDdl);
            throw e;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncDatabase(Long objectId, Integer designVersion, boolean confirmOnlineDdl) {
        BusinessObjectDesignerService.DesignerContext context = contextProvider.loadContext(objectId);
        assertDesignVersion(context, designVersion);
        if (!confirmOnlineDdl) {
            throw new BusinessException("同步数据库需要显式二次确认");
        }
        if (!hasDdlPermission()) {
            throw new BusinessException("缺少同步数据库权限: " + DDL_PERMISSION);
        }

        LowcodeModelSchema modelSchema = requireModelSchema(context);
        assertDatasourceAllowsDdl(modelSchema);
        LowcodeDdlPreviewVO preview = ddlService.previewCreateTable(modelSchema);
        if (!Boolean.TRUE.equals(preview.getExecutable())) {
            throw new BusinessException("当前数据库差异不允许在线执行，请导出迁移脚本后人工审核");
        }
        if (ddlService.containsUnsafeOnlineDdl(preview.getDdlStatements())) {
            throw new BusinessException("数据库差异包含高风险或非追加式 DDL，仅允许预览和导出脚本");
        }
        if (!hasDdl(preview)) {
            persistSyncResult(context, "IN_SYNC", "数据库结构已是最新版本", 0);
            return;
        }
        try {
            ddlService.executeCreateTable(modelSchema);
            persistSyncResult(context, "IN_SYNC", "数据库结构同步成功", preview.getDdlStatements().size());
        } catch (RuntimeException e) {
            persistSyncResult(context, "FAILED", safeMessage(e), preview.getDdlStatements().size());
            throw e;
        }
    }

    /**
     * 为页面表单自动托管的数据存储执行安全追加式同步。
     *
     * <p>该入口只供应用表单聚合服务在元数据事务提交后调用。普通对象仍必须走
     * {@link #syncDatabase(Long, Integer, boolean)} 的权限、版本和二次确认门禁。</p>
     */
    public void syncManagedDatabase(Long objectId, Long applicationId, String formAssetId) {
        BusinessObjectDesignerService.DesignerContext context = contextProvider.loadContext(objectId);
        syncManagedDatabase(context, applicationId, formAssetId);
    }

    /**
     * 复用已加载的 DesignerContext 执行托管数据表同步，避免重复查询数据库元数据。
     * 当调用方（如 provision 流程）已经持有完整 context 时优先使用此方法。
     */
    public void syncManagedDatabase(
            BusinessObjectDesignerService.DesignerContext context,
            Long applicationId, String formAssetId) {
        assertManagedPageForm(context, applicationId, formAssetId);
        LowcodeModelSchema modelSchema = requireModelSchema(context);
        LowcodeDdlPreviewVO preview = ddlService.previewCreateTable(modelSchema);
        if (!Boolean.TRUE.equals(preview.getExecutable())) {
            throw new BusinessException("当前数据存储未允许自动建表，请在高级数据设置中开启自动建表");
        }
        if (!hasDdl(preview)) {
            persistSyncResult(context, "IN_SYNC", "数据表结构已是最新版本", 0);
            return;
        }
        int totalDdl = preview.getDdlStatements().size();
        int executed;
        try {
            // 先执行安全追加式 DDL（ADD COLUMN 等），非追加式语句单独报告，
            // 避免一条 MODIFY 阻断全部新增字段的自动同步。
            executed = ddlService.executeSafeDdlOnly(modelSchema);
        } catch (RuntimeException e) {
            persistSyncFailure(context, e, totalDdl);
            throw e;
        }
        int unsafeCount = totalDdl - executed;
        if (unsafeCount > 0) {
            String detail = describeUnsafeColumnChanges(preview);
            String summary = "已自动同步 " + executed + " 项安全变更；另有 " + unsafeCount
                    + " 项高风险差异已跳过（缩长度/改类型/收紧必填等）"
                    + (StringUtils.isBlank(detail) ? "" : "：" + detail);
            // 页面表单自动同步：安全变更已落地即可，剩余差异留给高级数据设置，不阻断保存、不弹提示。
            persistSyncResult(context, "PARTIAL", summary, executed);
            return;
        }
        persistSyncResult(context, "IN_SYNC", "页面表单数据表同步成功", executed);
    }

    /**
     * 从 DDL 预览里抽出“已有列需调整”的说明。加子表/保存草稿只会触发全量对比，
     * 容易把主表上早已存在的模型与库不一致误当成“我改了字段类型”。
     */
    private String describeUnsafeColumnChanges(LowcodeDdlPreviewVO preview) {
        if (preview == null) {
            return "";
        }
        List<String> details = new ArrayList<>();
        if (preview.getWarnings() != null) {
            for (String warning : preview.getWarnings()) {
                if (StringUtils.isBlank(warning)) {
                    continue;
                }
                if (warning.contains("数据库类型将从") || warning.contains("长度变小")) {
                    details.add(warning);
                }
            }
        }
        if (!details.isEmpty()) {
            return String.join("；", details);
        }
        if (preview.getDdlStatements() == null) {
            return "";
        }
        List<String> columns = new ArrayList<>();
        for (String ddl : preview.getDdlStatements()) {
            String normalized = StringUtils.upperCase(StringUtils.defaultString(ddl), Locale.ROOT);
            if (!normalized.contains(" MODIFY COLUMN ") && !normalized.contains(" ALTER COLUMN ")) {
                continue;
            }
            String column = extractModifyColumnName(ddl);
            if (StringUtils.isNotBlank(column)) {
                columns.add(column);
            }
        }
        return columns.isEmpty() ? "" : "涉及列 " + String.join("、", columns);
    }

    private String extractModifyColumnName(String ddl) {
        String text = StringUtils.defaultString(ddl);
        int modifyIdx = StringUtils.indexOfIgnoreCase(text, "MODIFY COLUMN ");
        if (modifyIdx >= 0) {
            return firstIdentifier(text.substring(modifyIdx + "MODIFY COLUMN ".length()));
        }
        int alterIdx = StringUtils.indexOfIgnoreCase(text, "ALTER COLUMN ");
        if (alterIdx >= 0) {
            return firstIdentifier(text.substring(alterIdx + "ALTER COLUMN ".length()));
        }
        return null;
    }

    private String firstIdentifier(String fragment) {
        String trimmed = StringUtils.trimToEmpty(fragment);
        if (trimmed.isEmpty()) {
            return null;
        }
        char quote = trimmed.charAt(0);
        if (quote == '`' || quote == '"' || quote == '[') {
            char end = quote == '[' ? ']' : quote;
            int close = trimmed.indexOf(end, 1);
            if (close > 1) {
                return trimmed.substring(1, close);
            }
        }
        int end = 0;
        while (end < trimmed.length()) {
            char ch = trimmed.charAt(end);
            if (!(Character.isLetterOrDigit(ch) || ch == '_')) {
                break;
            }
            end++;
        }
        return end > 0 ? trimmed.substring(0, end) : null;
    }

    protected boolean hasDdlPermission() {
        try {
            return SessionHelper.hasPermission(DDL_PERMISSION);
        } catch (Exception e) {
            return false;
        }
    }

    private BusinessObjectTableMappingVO baseMapping(
            BusinessObjectDesignerService.DesignerContext context, LowcodeModelSchema modelSchema) {
        AiBusinessObject object = context.getObject();
        AiCrudConfig config = context.getConfig();
        LowcodeRuntimeDatasourceSnapshot datasource = modelSchema.getRuntimeDatasource();
        BusinessObjectTableMappingVO mapping = new BusinessObjectTableMappingVO();
        mapping.setObjectId(object.getId());
        mapping.setObjectCode(object.getObjectCode());
        mapping.setObjectName(object.getObjectName());
        mapping.setDatasourceId(datasource == null ? null : datasource.getDatasourceId());
        mapping.setDatasourceCode(firstNotBlank(
                datasource == null ? null : datasource.getDatasourceCode(),
                config == null ? null : config.getRuntimeDatasourceCode()));
        mapping.setDatasourceName(datasource == null ? null : datasource.getDatasourceName());
        mapping.setDbType(datasource == null ? null : datasource.getDbType());
        mapping.setTableName(firstNotBlank(modelSchema.getTableName(),
                datasource == null ? null : datasource.getTableName(),
                config == null ? null : config.getRuntimeTableName()));
        mapping.setTableMode(firstNotBlank(modelSchema.getTableMode(),
                datasource == null ? null : datasource.getTableMode()));
        mapping.setAllowDdl(datasource == null ? null : datasource.getAllowDdl());
        mapping.setReadonly(datasource == null ? null : datasource.getReadonly());
        mapping.setDesignVersion(config == null ? 0 : defaultInteger(config.getDraftVersion()));
        Long sharedCount = applicationObjectMapper.countByObjectId(resolveTenantId(), object.getId());
        mapping.setSharedApplicationCount(sharedCount == null ? 0L : sharedCount);
        return mapping;
    }

    private List<BusinessObjectTableFieldMappingVO> buildFieldMappings(
            LowcodeModelSchema modelSchema, Map<String, ColumnMetadata> columns, Set<String> indexes) {
        List<BusinessObjectTableFieldMappingVO> fields = new ArrayList<>();
        Set<String> mappedColumns = new LinkedHashSet<>();
        for (LowcodeFieldSchema field : safeFields(modelSchema)) {
            String columnName = normalizeIdentifier(field.getColumnName());
            if (columnName != null) {
                mappedColumns.add(columnName);
            }
            ColumnMetadata metadata = columnName == null ? null : columns.get(columnName);
            fields.add(toFieldMapping(modelSchema, field, metadata, indexes));
        }
        for (Map.Entry<String, ColumnMetadata> entry : columns.entrySet()) {
            if (mappedColumns.contains(normalizeIdentifier(entry.getKey()))) {
                continue;
            }
            ColumnMetadata metadata = entry.getValue();
            BusinessObjectTableFieldMappingVO field = new BusinessObjectTableFieldMappingVO();
            field.setBusinessName(StringUtils.defaultIfBlank(metadata.columnComment(), metadata.columnName()));
            field.setColumnName(metadata.columnName());
            boolean systemColumn = SYSTEM_COLUMNS.contains(normalizeIdentifier(metadata.columnName()));
            field.setSystemField(systemColumn);
            field.setReadonly(true);
            applyDatabaseMetadata(field, metadata, indexes);
            field.setSyncStatus(systemColumn ? "IN_SYNC" : "UNMAPPED_DATABASE_COLUMN");
            field.setBlockingDifference(!systemColumn && isUnmappedColumnBlocking(metadata));
            fields.add(field);
        }
        return fields;
    }

    private BusinessObjectTableFieldMappingVO toFieldMapping(
            LowcodeModelSchema modelSchema, LowcodeFieldSchema source,
            ColumnMetadata metadata, Set<String> indexes) {
        BusinessObjectTableFieldMappingVO field = new BusinessObjectTableFieldMappingVO();
        field.setBusinessName(source.getLabel());
        field.setFieldCode(source.getField());
        field.setColumnName(source.getColumnName());
        field.setDataType(source.getDataType());
        field.setLength(source.getLength());
        field.setPrecision(source.getPrecision());
        field.setRequired(Boolean.TRUE.equals(source.getRequired()));
        field.setDefaultValue(source.getDefaultValue());
        field.setComponentType(source.getComponentType());
        field.setSystemField(Boolean.TRUE.equals(source.getSystemField())
                || SYSTEM_COLUMNS.contains(normalizeIdentifier(source.getColumnName())));
        field.setReadonly(Boolean.TRUE.equals(source.getReadonly()) || Boolean.TRUE.equals(field.getSystemField()));
        field.setConfiguredIndex(isConfiguredIndex(modelSchema, source));
        if (metadata == null) {
            field.setDatabaseIndexed(false);
            field.setSyncStatus("MISSING_DATABASE_COLUMN");
            field.setBlockingDifference(true);
            return field;
        }
        applyDatabaseMetadata(field, metadata, indexes);
        boolean sameType = sameType(source, metadata);
        field.setSyncStatus(sameType ? "IN_SYNC" : "TYPE_MISMATCH");
        field.setBlockingDifference(!sameType);
        return field;
    }

    private void applyDatabaseMetadata(
            BusinessObjectTableFieldMappingVO field, ColumnMetadata metadata, Set<String> indexes) {
        field.setDatabaseType(metadata.columnType());
        field.setDatabaseNullable(!"NO".equalsIgnoreCase(metadata.isNullable()));
        field.setDatabaseDefaultValue(metadata.columnDefault());
        field.setDatabaseIndexed(isDatabaseIndexed(metadata.columnName(), indexes));
    }

    private boolean sameType(LowcodeFieldSchema field, ColumnMetadata metadata) {
        String configuredType = normalizeType(field.getDataType());
        String databaseType = normalizeType(metadata.columnType());
        if (isLogicDeleteField(field)
                && LOGIC_DELETE_STORAGE_TYPES.contains(baseType(configuredType))
                && LOGIC_DELETE_STORAGE_TYPES.contains(baseType(databaseType))) {
            return true;
        }
        if (!baseType(configuredType).equals(baseType(databaseType))) {
            return false;
        }
        if (Set.of("varchar", "char").contains(baseType(configuredType)) && field.getLength() != null) {
            return databaseType.startsWith(baseType(configuredType) + "(" + field.getLength() + ")");
        }
        if ("decimal".equals(baseType(configuredType)) && field.getLength() != null) {
            int scale = field.getPrecision() == null ? 2 : field.getPrecision();
            return databaseType.startsWith("decimal(" + field.getLength() + "," + scale + ")");
        }
        return true;
    }

    private boolean isLogicDeleteField(LowcodeFieldSchema field) {
        if (!Boolean.TRUE.equals(field.getSystemField())) {
            return false;
        }
        String fieldCode = normalizeIdentifier(field.getField());
        String columnName = normalizeIdentifier(field.getColumnName());
        return "del_flag".equals(columnName)
                || "delflag".equals(fieldCode)
                || "del_flag".equals(fieldCode);
    }

    private boolean isUnmappedColumnBlocking(ColumnMetadata metadata) {
        if (!"NO".equalsIgnoreCase(metadata.isNullable()) || metadata.columnDefault() != null) {
            return false;
        }
        String extra = StringUtils.defaultString(metadata.extra()).toLowerCase(Locale.ROOT);
        return !extra.contains("auto_increment")
                && !extra.contains("generated")
                && StringUtils.isBlank(metadata.generationExpression());
    }

    private boolean isConfiguredIndex(LowcodeModelSchema modelSchema, LowcodeFieldSchema field) {
        String fieldCode = StringUtils.defaultString(field.getField());
        String columnName = StringUtils.defaultString(field.getColumnName());
        for (LowcodeIndexSchema index : safeIndexes(modelSchema)) {
            if (index.getFields() != null && index.getFields().stream()
                    .anyMatch(item -> fieldCode.equals(item) || columnName.equals(item))) {
                return true;
            }
        }
        return false;
    }

    private boolean isDatabaseIndexed(String columnName, Set<String> indexes) {
        String normalizedColumn = normalizeIdentifier(columnName);
        return normalizedColumn != null && indexes.stream()
                .map(this::normalizeIdentifier)
                .filter(java.util.Objects::nonNull)
                .anyMatch(index -> index.contains(normalizedColumn));
    }

    private void assertDesignVersion(
            BusinessObjectDesignerService.DesignerContext context, Integer designVersion) {
        int currentVersion = context.getConfig() == null
                ? 0 : defaultInteger(context.getConfig().getDraftVersion());
        if (designVersion == null || designVersion != currentVersion) {
            throw new BusinessException("业务对象设计版本已变化，请刷新后重新预览数据库差异");
        }
    }

    private void assertDatasourceAllowsDdl(LowcodeModelSchema modelSchema) {
        LowcodeRuntimeDatasourceSnapshot datasource = modelSchema.getRuntimeDatasource();
        if (datasource != null && Boolean.FALSE.equals(datasource.getAllowDdl())) {
            throw new BusinessException("运行数据源禁止在线 DDL，请导出迁移脚本后人工执行");
        }
        if (datasource != null && Boolean.TRUE.equals(datasource.getReadonly())) {
            throw new BusinessException("只读运行数据源不能执行在线 DDL");
        }
    }

    private void assertManagedPageForm(
            BusinessObjectDesignerService.DesignerContext context,
            Long applicationId,
            String formAssetId) {
        AiBusinessObject object = context == null ? null : context.getObject();
        JSONObject options = object == null ? null : readObject(object.getOptions());
        if (options == null
                || !MANAGED_BY_PAGE_FORM.equals(options.getString("managedBy"))
                || !StringUtils.equals(String.valueOf(applicationId), options.getString("sourceApplicationId"))
                || !StringUtils.equals(formAssetId, options.getString("sourceFormAssetId"))) {
            throw new BusinessException("当前对象不是页面表单自动管理的数据存储，不能自动调整数据库");
        }
    }

    private void persistSyncFailure(
            BusinessObjectDesignerService.DesignerContext context,
            RuntimeException error,
            int ddlCount) {
        try {
            persistSyncResult(context, "FAILED", safeMessage(error), ddlCount);
        } catch (RuntimeException ignored) {
            // 保留原始 DDL 异常，避免同步状态写回失败覆盖真正原因。
        }
    }

    private void persistSyncResult(
            BusinessObjectDesignerService.DesignerContext context,
            String status, String message, int ddlCount) {
        AiBusinessObject object = context.getObject();
        JSONObject options = readObject(object.getDesignerOptions());
        JSONObject sync = new JSONObject();
        sync.put("status", status);
        sync.put("designVersion", context.getConfig() == null
                ? 0 : defaultInteger(context.getConfig().getDraftVersion()));
        sync.put("syncTime", LocalDateTime.now().toString());
        sync.put("message", StringUtils.abbreviate(StringUtils.defaultString(message), 500));
        sync.put("ddlCount", ddlCount);
        options.put(DATABASE_SYNC_OPTION_KEY, sync);
        object.setDesignerOptions(options.toJSONString());
        objectMapper.updateById(object);
    }

    private void applyLastSync(BusinessObjectTableMappingVO mapping, AiBusinessObject object) {
        JSONObject sync = readObject(object.getDesignerOptions()).getJSONObject(DATABASE_SYNC_OPTION_KEY);
        if (sync == null) {
            return;
        }
        mapping.setLastSyncStatus(sync.getString("status"));
        mapping.setLastSyncMessage(sync.getString("message"));
        String syncTime = sync.getString("syncTime");
        if (StringUtils.isNotBlank(syncTime)) {
            try {
                mapping.setLastSyncTime(LocalDateTime.parse(syncTime));
            } catch (Exception ignored) {
                mapping.setLastSyncTime(null);
            }
        }
    }

    private LowcodeModelSchema requireModelSchema(BusinessObjectDesignerService.DesignerContext context) {
        if (context == null || context.getObject() == null || context.getModelSchema() == null
                || StringUtils.isBlank(context.getModelSchema().getTableName())) {
            throw new BusinessException("业务对象尚未配置运行数据表");
        }
        return context.getModelSchema();
    }

    private JSONObject readObject(String value) {
        if (StringUtils.isBlank(value)) {
            return new JSONObject();
        }
        try {
            JSONObject result = JSON.parseObject(value);
            return result == null ? new JSONObject() : result;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private String safeMessage(Throwable error) {
        return StringUtils.abbreviate(StringUtils.defaultIfBlank(error.getMessage(), "数据库结构同步失败"), 500);
    }

    private List<LowcodeFieldSchema> safeFields(LowcodeModelSchema modelSchema) {
        return modelSchema.getFields() == null ? List.of() : modelSchema.getFields();
    }

    private List<LowcodeIndexSchema> safeIndexes(LowcodeModelSchema modelSchema) {
        if (modelSchema.getIndexes() == null) {
            return List.of();
        }
        return modelSchema.getIndexes().stream()
                .filter(index -> index != null && !Boolean.TRUE.equals(index.getAuto()))
                .toList();
    }

    private Map<String, ColumnMetadata> safeMap(Map<String, ColumnMetadata> columns) {
        if (columns == null || columns.isEmpty()) {
            return Map.of();
        }
        Map<String, ColumnMetadata> normalized = new LinkedHashMap<>();
        columns.forEach((key, value) -> normalized.put(normalizeIdentifier(key), value));
        return normalized;
    }

    private Set<String> safeSet(Set<String> indexes) {
        return indexes == null ? Set.of() : indexes;
    }

    private boolean hasDdl(LowcodeDdlPreviewVO preview) {
        return preview != null && preview.getDdlStatements() != null && !preview.getDdlStatements().isEmpty();
    }

    private int ddlCount(LowcodeDdlPreviewVO preview) {
        return preview == null || preview.getDdlStatements() == null
                ? 0 : preview.getDdlStatements().size();
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String normalizeIdentifier(String value) {
        return StringUtils.isBlank(value) ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeType(String value) {
        return StringUtils.defaultIfBlank(value, "varchar")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private String baseType(String value) {
        int bracket = value.indexOf('(');
        return bracket < 0 ? value : value.substring(0, bracket);
    }

    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? 1L : tenantId;
        } catch (Exception e) {
            return 1L;
        }
    }
}

package com.mdframe.forge.plugin.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.data.dto.DataDatasetFieldDTO;
import com.mdframe.forge.plugin.data.dto.DataDatasetPreviewDTO;
import com.mdframe.forge.plugin.data.dto.DataDatasetSaveDTO;
import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.plugin.data.entity.DataDataset;
import com.mdframe.forge.plugin.data.entity.DataDatasetCategory;
import com.mdframe.forge.plugin.data.entity.DataDatasetField;
import com.mdframe.forge.plugin.data.enums.DataDatasetAccessLevelEnum;
import com.mdframe.forge.plugin.data.enums.DataDatasetAccessModeEnum;
import com.mdframe.forge.plugin.data.enums.DatasetPublishStatusEnum;
import com.mdframe.forge.plugin.data.service.DataConnectionService;
import com.mdframe.forge.plugin.data.service.DataDatasetAccessService;
import com.mdframe.forge.plugin.data.service.DataDatasetCategoryService;
import com.mdframe.forge.plugin.data.service.DataDatasetFieldService;
import com.mdframe.forge.plugin.data.service.DataDatasetRowScopeService;
import com.mdframe.forge.plugin.data.service.DataDatasetService;
import com.mdframe.forge.plugin.data.support.DatasetParamSchemaParser;
import com.mdframe.forge.plugin.data.support.DataDatasetFieldViewAssembler;
import com.mdframe.forge.plugin.data.support.DbDialect;
import com.mdframe.forge.plugin.data.support.DbDialectFactory;
import com.mdframe.forge.plugin.data.support.JdbcDataSourceProvider;
import com.mdframe.forge.plugin.data.support.SqlParameterBinder;
import com.mdframe.forge.plugin.data.support.SqlSafetyValidator;
import com.mdframe.forge.plugin.data.vo.DataConnectionFieldVO;
import com.mdframe.forge.plugin.data.vo.DataDatasetDetailVO;
import com.mdframe.forge.plugin.data.vo.DataDatasetFieldVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/data/dataset")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class DataDatasetController {

    private final DataDatasetService datasetService;
    private final DataDatasetAccessService datasetAccessService;
    private final DataDatasetRowScopeService datasetRowScopeService;
    private final DataDatasetFieldService fieldService;
    private final DataConnectionService connectionService;
    private final DataDatasetCategoryService categoryService;
    private final JdbcDataSourceProvider dataSourceProvider;
    private final DbDialectFactory dialectFactory;
    private final SqlSafetyValidator sqlSafetyValidator;
    private final SqlParameterBinder parameterBinder;
    private final DatasetParamSchemaParser datasetParamSchemaParser;
    private final DataDatasetFieldViewAssembler fieldViewAssembler;

    @GetMapping("/page")
    public RespInfo<IPage<DataDataset>> page(
        @RequestParam(required = false) String datasetName,
        @RequestParam(required = false) Long connectionId,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Boolean uncategorized,
        @RequestParam(required = false) String datasetType,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) Integer publishStatus,
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<DataDataset> page = datasetService.page(datasetName, connectionId, datasetType, status, publishStatus,
            categoryId, uncategorized, pageNum, pageSize);
        return RespInfo.success(page);
    }

    @GetMapping("/list")
    public RespInfo<List<DataDataset>> list(@RequestParam(required = false) Long connectionId) {
        return RespInfo.success(datasetService.listByConnectionId(connectionId));
    }

    @GetMapping("/{id}")
    public RespInfo<DataDatasetDetailVO> getById(@PathVariable Long id) {
        DataDataset dataset = requireDataset(id);
        datasetAccessService.requireAccess(dataset, DataDatasetAccessLevelEnum.VIEW);
        DataConnection connection = connectionService.getById(dataset.getConnectionId());
        DataDatasetCategory category = dataset.getCategoryId() != null ? categoryService.getById(dataset.getCategoryId()) : null;
        List<DataDatasetField> fields = fieldService.listByDatasetId(id);
        return RespInfo.success(convertToDetailVO(dataset, connection, category, fields));
    }

    @PostMapping
    @OperationLog(module = "数据资产", desc = "新增数据集：{{#dto.datasetName}}")
    public RespInfo<Void> add(@Validated @RequestBody DataDatasetSaveDTO dto) {
        validateDatasetDefinition(dto.getDatasetCode(), dto.getDatasetName(), dto.getConnectionId(), dto.getCategoryId(),
            dto.getDatasetType(), dto.getTableName(), dto.getSqlText(), dto.getParamSchemaJson());
        DataDataset entity = convertToEntity(dto, null);
        entity.setPublishStatus(DatasetPublishStatusEnum.DRAFT.getCode());
        datasetService.save(entity);
        if (dto.getAclItems() != null) {
            datasetAccessService.saveDatasetAcl(entity.getId(), dto.getAclItems());
        }
        if (dto.getRowScope() != null) {
            datasetRowScopeService.saveDatasetRowScope(entity.getId(), dto.getRowScope());
        }
        if (dto.getFields() != null && !dto.getFields().isEmpty()) {
            saveFields(entity.getId(), dto.getFields());
        }
        return RespInfo.success();
    }

    @PutMapping
    @OperationLog(module = "数据资产", desc = "修改数据集：{{#dto.datasetName}}")
    public RespInfo<Void> edit(@Validated @RequestBody DataDatasetSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("数据集ID不能为空");
        }
        DataDataset existing = requireDataset(dto.getId());
        requireManageAccessIfPrivate(existing);
        assertEditable(existing, "修改");
        validateDatasetDefinition(dto.getDatasetCode(), dto.getDatasetName(), dto.getConnectionId(), dto.getCategoryId(),
            dto.getDatasetType(), dto.getTableName(), dto.getSqlText(), dto.getParamSchemaJson());
        DataDataset entity = convertToEntity(dto, existing);
        datasetService.updateById(entity);
        if (dto.getAclItems() != null) {
            datasetAccessService.saveDatasetAcl(entity.getId(), dto.getAclItems());
        }
        if (dto.getRowScope() != null) {
            datasetRowScopeService.saveDatasetRowScope(entity.getId(), dto.getRowScope());
        }
        if (dto.getFields() != null) {
            saveFields(entity.getId(), dto.getFields());
        }
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "数据资产", desc = "删除数据集")
    public RespInfo<Void> remove(@PathVariable Long id) {
        DataDataset dataset = requireDataset(id);
        requireManageAccessIfPrivate(dataset);
        assertEditable(dataset, "删除");
        fieldService.deleteByDatasetId(id);
        datasetAccessService.deleteDatasetAcl(id);
        datasetRowScopeService.deleteDatasetRowScope(id);
        datasetService.removeById(id);
        return RespInfo.success();
    }

    @PostMapping("/{id}/publish")
    @OperationLog(module = "数据资产", desc = "发布数据集")
    public RespInfo<Void> publish(@PathVariable Long id) {
        DataDataset dataset = requireDataset(id);
        requireManageAccessIfPrivate(dataset);
        if (DatasetPublishStatusEnum.isPublished(dataset.getPublishStatus())) {
            throw new BusinessException("数据集已发布，无需重复操作");
        }
        validateDatasetDefinition(dataset.getDatasetCode(), dataset.getDatasetName(), dataset.getConnectionId(),
            dataset.getCategoryId(), dataset.getDatasetType(), dataset.getTableName(), dataset.getSqlText(),
            dataset.getParamSchemaJson());

        List<DataDatasetField> fields = fieldService.listByDatasetId(id);
        if (fields == null || fields.isEmpty()) {
            DataConnection connection = requireEnabledConnection(dataset.getConnectionId());
            fieldService.saveBatchFields(id, extractFieldsFromDataset(dataset, connection));
        }

        updatePublishStatus(id, DatasetPublishStatusEnum.PUBLISHED.getCode());
        return RespInfo.success();
    }

    @PostMapping("/{id}/offline")
    @OperationLog(module = "数据资产", desc = "下架数据集")
    public RespInfo<Void> offline(@PathVariable Long id) {
        DataDataset dataset = requireDataset(id);
        requireManageAccessIfPrivate(dataset);
        if (!DatasetPublishStatusEnum.isPublished(dataset.getPublishStatus())) {
            throw new BusinessException("只有已发布的数据集才能下架");
        }
        updatePublishStatus(id, DatasetPublishStatusEnum.OFFLINE.getCode());
        return RespInfo.success();
    }

    @PostMapping("/{id}/sync-fields")
    @OperationLog(module = "数据资产", desc = "同步数据集字段")
    public RespInfo<List<DataDatasetFieldVO>> syncFields(@PathVariable Long id) {
        DataDataset dataset = requireDataset(id);
        requireManageAccessIfPrivate(dataset);
        assertEditable(dataset, "同步字段");
        DataConnection connection = requireEnabledConnection(dataset.getConnectionId());
        List<DataDatasetField> fields = extractFieldsFromDataset(dataset, connection);
        fieldService.saveBatchFields(id, fields);
        return RespInfo.success(convertToFieldVOList(fields));
    }

    @PutMapping("/{id}/fields")
    @OperationLog(module = "数据资产", desc = "保存数据集字段配置")
    public RespInfo<Void> saveFields(@PathVariable Long id, @RequestBody List<DataDatasetFieldDTO> fieldDTOs) {
        DataDataset dataset = requireDataset(id);
        requireManageAccessIfPrivate(dataset);
        assertEditable(dataset, "保存字段配置");
        List<DataDatasetField> fields = fieldDTOs.stream()
            .map(this::convertFieldDTOToEntity)
            .collect(Collectors.toList());
        fieldService.saveBatchFields(id, fields);
        return RespInfo.success();
    }

    @PostMapping("/{id}/preview")
    @OperationLog(module = "数据资产", desc = "预览数据集")
    public RespInfo<Map<String, Object>> preview(@PathVariable Long id, @RequestBody DataDatasetPreviewDTO dto) {
        DataDataset dataset = requireDataset(id);
        datasetAccessService.requireAccess(dataset, DataDatasetAccessLevelEnum.QUERY);
        ensureDatasetUsable(dataset);
        DataConnection connection = requireEnabledConnection(dataset.getConnectionId());
        return RespInfo.success(executePreview(dataset, connection, dto));
    }

    @PostMapping("/preview-sql")
    @OperationLog(module = "数据资产", desc = "预览SQL数据集")
    public RespInfo<Map<String, Object>> previewSql(@RequestBody DataDatasetSaveDTO dto) {
        if (dto.getConnectionId() == null) {
            throw new BusinessException("数据连接不能为空");
        }
        if (dto.getSqlText() == null || dto.getSqlText().isEmpty()) {
            throw new BusinessException("SQL不能为空");
        }
        DataConnection connection = requireEnabledConnection(dto.getConnectionId());

        DataDataset dataset = new DataDataset();
        dataset.setConnectionId(dto.getConnectionId());
        dataset.setDatasetType("SQL");
        dataset.setSqlText(dto.getSqlText());
        Map<String, Object> result = executePreviewSqlOrThrow(dataset, connection, 10);
        return RespInfo.success(result);
    }

    private DataDataset requireDataset(Long id) {
        DataDataset dataset = datasetService.getById(id);
        if (dataset == null) {
            throw new BusinessException("数据集不存在或已删除");
        }
        return dataset;
    }

    private DataConnection requireEnabledConnection(Long connectionId) {
        DataConnection connection = connectionService.getById(connectionId);
        if (connection == null) {
            throw new BusinessException("数据连接不存在或已删除");
        }
        if (connection.getStatus() != 1) {
            throw new BusinessException("数据连接已禁用");
        }
        return connection;
    }

    private void validateDatasetDefinition(String datasetCode, String datasetName, Long connectionId, Long categoryId,
        String datasetType, String tableName, String sqlText, String paramSchemaJson) {
        if (datasetCode == null || datasetCode.isEmpty()) {
            throw new BusinessException("数据集编码不能为空");
        }
        if (datasetName == null || datasetName.isEmpty()) {
            throw new BusinessException("数据集名称不能为空");
        }
        if (connectionId == null) {
            throw new BusinessException("数据连接不能为空");
        }
        if (datasetType == null || datasetType.isEmpty()) {
            throw new BusinessException("数据集类型不能为空");
        }

        requireEnabledConnection(connectionId);

        if (categoryId != null) {
            DataDatasetCategory category = categoryService.getById(categoryId);
            if (category == null) {
                throw new BusinessException("数据集分类不存在或已删除");
            }
        }

        if ("TABLE".equals(datasetType)) {
            if (tableName == null || tableName.isEmpty()) {
                throw new BusinessException("表名不能为空");
            }
        }

        if ("SQL".equals(datasetType)) {
            if (sqlText == null || sqlText.isEmpty()) {
                throw new BusinessException("SQL不能为空");
            }
            sqlSafetyValidator.validate(sqlText);
        }

        if (paramSchemaJson != null && !paramSchemaJson.trim().isEmpty()) {
            try {
                datasetParamSchemaParser.validate(paramSchemaJson, "TABLE".equals(datasetType));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(e.getMessage());
            }
        }
    }

    private DataDataset convertToEntity(DataDatasetSaveDTO dto, DataDataset existing) {
        DataDataset entity = new DataDataset();
        entity.setId(dto.getId());
        entity.setTenantId(existing != null ? existing.getTenantId() : SessionHelper.getTenantId());
        entity.setDatasetCode(dto.getDatasetCode());
        entity.setDatasetName(dto.getDatasetName());
        entity.setConnectionId(dto.getConnectionId());
        entity.setCategoryId(dto.getCategoryId());
        entity.setDatasetType(dto.getDatasetType());
        entity.setTableName(dto.getTableName());
        entity.setSqlText(dto.getSqlText());
        entity.setParamSchemaJson(dto.getParamSchemaJson());
        entity.setDefaultOrderJson(dto.getDefaultOrderJson());
        entity.setMaxRows(dto.getMaxRows() != null ? dto.getMaxRows() : existing != null ? existing.getMaxRows() : 1000);
        entity.setTimeoutSeconds(dto.getTimeoutSeconds() != null ? dto.getTimeoutSeconds() : existing != null ? existing.getTimeoutSeconds() : 15);
        entity.setCacheEnabled(dto.getCacheEnabled() != null ? dto.getCacheEnabled() : existing != null ? existing.getCacheEnabled() : 0);
        entity.setCacheTtlSeconds(dto.getCacheTtlSeconds());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : existing != null ? existing.getStatus() : 1);
        entity.setAccessMode(DataDatasetAccessModeEnum.normalize(
            dto.getAccessMode() != null ? dto.getAccessMode() : existing != null ? existing.getAccessMode() : null));
        entity.setDescription(dto.getDescription());
        return entity;
    }

    private void assertEditable(DataDataset dataset, String action) {
        if (DatasetPublishStatusEnum.isPublished(dataset.getPublishStatus())) {
            throw new BusinessException("数据集已发布，请先下架后再" + action);
        }
    }

    private void requireManageAccessIfPrivate(DataDataset dataset) {
        if (DataDatasetAccessModeEnum.isPrivate(dataset.getAccessMode())) {
            datasetAccessService.requireAccess(dataset, DataDatasetAccessLevelEnum.MANAGE);
        }
    }

    private void updatePublishStatus(Long id, Integer publishStatus) {
        boolean updated = datasetService.lambdaUpdate()
            .eq(DataDataset::getId, id)
            .set(DataDataset::getPublishStatus, publishStatus)
            .update();
        if (!updated) {
            throw new BusinessException("数据集发布状态更新失败");
        }
    }

    private void ensureDatasetUsable(DataDataset dataset) {
        if (!DatasetPublishStatusEnum.isPublished(dataset.getPublishStatus())) {
            throw new BusinessException("数据集未发布，暂不可使用");
        }
        if (dataset.getStatus() != 1) {
            throw new BusinessException("数据集已禁用");
        }
    }

    private DataDatasetDetailVO convertToDetailVO(DataDataset dataset, DataConnection connection, DataDatasetCategory category,
        List<DataDatasetField> fields) {
        DataDatasetDetailVO vo = new DataDatasetDetailVO();
        vo.setId(dataset.getId());
        vo.setDatasetCode(dataset.getDatasetCode());
        vo.setDatasetName(dataset.getDatasetName());
        vo.setConnectionId(dataset.getConnectionId());
        vo.setCategoryId(dataset.getCategoryId());
        vo.setConnectionName(connection != null ? connection.getConnectionName() : null);
        vo.setCategoryCode(category != null ? category.getCategoryCode() : null);
        vo.setCategoryName(category != null ? category.getCategoryName() : null);
        vo.setDatasetType(dataset.getDatasetType());
        vo.setTableName(dataset.getTableName());
        vo.setSqlText(dataset.getSqlText());
        vo.setParamSchemaJson(dataset.getParamSchemaJson());
        vo.setDefaultOrderJson(dataset.getDefaultOrderJson());
        vo.setMaxRows(dataset.getMaxRows());
        vo.setTimeoutSeconds(dataset.getTimeoutSeconds());
        vo.setCacheEnabled(dataset.getCacheEnabled());
        vo.setCacheTtlSeconds(dataset.getCacheTtlSeconds());
        vo.setStatus(dataset.getStatus());
        vo.setPublishStatus(dataset.getPublishStatus());
        vo.setAccessMode(DataDatasetAccessModeEnum.normalize(dataset.getAccessMode()));
        vo.setDescription(dataset.getDescription());
        vo.setCreateTime(dataset.getCreateTime());
        vo.setUpdateTime(dataset.getUpdateTime());
        vo.setFields(convertToFieldVOList(fields));
        vo.setAclItems(datasetAccessService.canAccess(dataset, DataDatasetAccessLevelEnum.MANAGE)
            ? datasetAccessService.listDatasetAcl(dataset.getId())
            : Collections.emptyList());
        vo.setRowScope(datasetAccessService.canAccess(dataset, DataDatasetAccessLevelEnum.MANAGE)
            ? datasetRowScopeService.getByDatasetId(dataset.getId())
            : null);
        return vo;
    }

    private List<DataDatasetFieldVO> convertToFieldVOList(List<DataDatasetField> fields) {
        return fieldViewAssembler.toVOList(fields);
    }

    private DataDatasetField convertFieldDTOToEntity(DataDatasetFieldDTO dto) {
        DataDatasetField entity = new DataDatasetField();
        entity.setId(dto.getId());
        entity.setFieldName(dto.getFieldName());
        entity.setFieldLabel(dto.getFieldLabel());
        entity.setSourceColumn(dto.getSourceColumn());
        entity.setDbType(dto.getDbType());
        entity.setDataType(dto.getDataType() != null ? dto.getDataType() : "STRING");
        entity.setFieldRole(dto.getFieldRole() != null ? dto.getFieldRole() : "DIMENSION");
        entity.setDefaultAgg(dto.getDefaultAgg());
        entity.setQueryEnabled(dto.getQueryEnabled() != null ? dto.getQueryEnabled() : 1);
        entity.setDisplayEnabled(dto.getDisplayEnabled() != null ? dto.getDisplayEnabled() : 1);
        entity.setSensitiveLevel(dto.getSensitiveLevel() != null ? dto.getSensitiveLevel() : "NONE");
        entity.setMaskRule(dto.getMaskRule());
        entity.setDictType(dto.getDictType());
        entity.setDateFormat(dto.getDateFormat());
        entity.setDataUnit(dto.getDataUnit());
        entity.setDimensionId(dto.getDimensionId());
        entity.setSort(dto.getSort() != null ? dto.getSort() : 0);
        entity.setDescription(dto.getDescription());
        return entity;
    }

    @SuppressWarnings("unchecked")
    private List<DataDatasetField> extractFieldsFromDataset(DataDataset dataset, DataConnection connection) {
        List<DataDatasetField> fields = new ArrayList<>();
        if ("TABLE".equals(dataset.getDatasetType())) {
            List<DataConnectionFieldVO> columnVOs = queryTableFields(connection, dataset.getTableName());
            int sort = 0;
            for (DataConnectionFieldVO col : columnVOs) {
                DataDatasetField field = new DataDatasetField();
                field.setFieldName(col.getColumnName());
                field.setFieldLabel(col.getColumnComment());
                field.setSourceColumn(col.getColumnName());
                field.setDbType(col.getColumnType());
                field.setDataType(mapDataType(col.getColumnType()));
                field.setFieldRole(isNumericType(col.getColumnType()) ? "MEASURE" : "DIMENSION");
                field.setQueryEnabled(1);
                field.setDisplayEnabled(1);
                field.setSensitiveLevel("NONE");
                field.setSort(sort++);
                fields.add(field);
            }
        } else if ("SQL".equals(dataset.getDatasetType())) {
            Map<String, Object> previewResult = executePreviewSql(dataset, connection, dataset.getMaxRows());
            List<Map<String, Object>> rows = (List<Map<String, Object>>) previewResult.get("rows");
            if (rows != null && !rows.isEmpty()) {
                Map<String, Object> firstRow = rows.get(0);
                int sort = 0;
                for (String key : firstRow.keySet()) {
                    DataDatasetField field = new DataDatasetField();
                    field.setFieldName(key);
                    field.setFieldLabel(key);
                    field.setDataType(mapValueDataType(firstRow.get(key)));
                    field.setFieldRole(firstRow.get(key) instanceof Number ? "MEASURE" : "DIMENSION");
                    field.setQueryEnabled(1);
                    field.setDisplayEnabled(1);
                    field.setSensitiveLevel("NONE");
                    field.setSort(sort++);
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    private List<DataConnectionFieldVO> queryTableFields(DataConnection connection, String tableName) {
        List<DataConnectionFieldVO> fields = new ArrayList<>();
        String schemaName = connection.getSchemaName();
        if (schemaName == null || schemaName.isEmpty()) {
            schemaName = extractSchemaFromUrl(connection.getJdbcUrl());
        }
        try {
            Connection conn = dataSourceProvider.getConnection(connection);
            try {
                String sql = dialectFactory.getDialect(connection.getDbType()).getColumnQuerySql(schemaName, tableName);
                PreparedStatement ps = conn.prepareStatement(sql);
                try {
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        DataConnectionFieldVO field = new DataConnectionFieldVO();
                        field.setColumnName(rs.getString("columnName"));
                        field.setColumnType(rs.getString("columnType"));
                        field.setColumnComment(rs.getString("columnComment"));
                        field.setNullable("YES".equalsIgnoreCase(rs.getString("nullable")));
                        field.setPrimaryKey("PRI".equalsIgnoreCase(rs.getString("primaryKey")));
                        fields.add(field);
                    }
                    rs.close();
                } finally {
                    ps.close();
                }
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            log.warn("Query fields failed: {}", e.getMessage());
        }
        return fields;
    }

    private Map<String, Object> executePreview(DataDataset dataset, DataConnection connection, DataDatasetPreviewDTO dto) {
        int maxRows = dto.getMaxRows() != null ? Math.min(dto.getMaxRows(), dataset.getMaxRows()) : dataset.getMaxRows();
        if ("TABLE".equals(dataset.getDatasetType())) {
            return executePreviewTable(dataset, connection, maxRows);
        }
        return executePreviewSql(dataset, connection, maxRows);
    }

    private Map<String, Object> executePreviewTable(DataDataset dataset, DataConnection connection, int maxRows) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        try {
            Connection conn = dataSourceProvider.getConnection(connection);
            try {
                DbDialect dialect = dialectFactory.getDialect(connection.getDbType());
                String sql = "SELECT * FROM " + dialect.quoteIdentifier(dataset.getTableName());
                sql = dialect.buildLimitSql(sql, maxRows);
                PreparedStatement ps = conn.prepareStatement(sql);
                try {
                    ResultSet rs = ps.executeQuery();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        columns.add(metaData.getColumnLabel(i));
                    }
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(metaData.getColumnLabel(i), rs.getObject(i));
                        }
                        rows.add(row);
                    }
                    rs.close();
                } finally {
                    ps.close();
                }
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            log.warn("Preview table failed: {}", e.getMessage());
        }
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("total", rows.size());
        return result;
    }

    private Map<String, Object> executePreviewSql(DataDataset dataset, DataConnection connection, int maxRows) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        try {
            return executePreviewSqlOrThrow(dataset, connection, maxRows);
        } catch (Exception e) {
            log.warn("Preview SQL failed: {}", e.getMessage());
        }
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("total", rows.size());
        return result;
    }

    private Map<String, Object> executePreviewSqlOrThrow(DataDataset dataset, DataConnection connection, int maxRows) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        try {
            Connection conn = dataSourceProvider.getConnection(connection);
            try {
                DbDialect dialect = dialectFactory.getDialect(connection.getDbType());
                String validatedSql = dataset.getSqlText();
                sqlSafetyValidator.validate(validatedSql);
                String wrappedSql = "SELECT * FROM (" + validatedSql + ") t";
                wrappedSql = dialect.buildLimitSql(wrappedSql, maxRows);
                String preparedSql = parameterBinder.convertToPreparedStatement(wrappedSql);
                PreparedStatement ps = conn.prepareStatement(preparedSql);
                try {
                    Map<Integer, Object> paramIndexMap = parameterBinder.buildParamIndexMap(wrappedSql, Map.of());
                    for (Map.Entry<Integer, Object> entry : paramIndexMap.entrySet()) {
                        ps.setObject(entry.getKey(), entry.getValue());
                    }
                    ResultSet rs = ps.executeQuery();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        columns.add(metaData.getColumnLabel(i));
                    }
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(metaData.getColumnLabel(i), rs.getObject(i));
                        }
                        rows.add(row);
                    }
                    rs.close();
                } finally {
                    ps.close();
                }
            } finally {
                conn.close();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Preview SQL failed: {}", e.getMessage());
            throw new BusinessException("SQL预览失败：" + e.getMessage());
        }
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("total", rows.size());
        return result;
    }

    private String mapDataType(String dbType) {
        if (dbType == null) {
            return "STRING";
        }
        String upper = dbType.toUpperCase();
        if (upper.contains("INT") || upper.contains("DECIMAL") || upper.contains("FLOAT")
            || upper.contains("DOUBLE") || upper.contains("NUMERIC")) {
            return "NUMBER";
        }
        if (upper.contains("DATE") && !upper.contains("TIME")) {
            return "DATE";
        }
        if (upper.contains("DATETIME") || upper.contains("TIMESTAMP")) {
            return "DATETIME";
        }
        if (upper.contains("BOOL") || upper.contains("TINYINT(1)")) {
            return "BOOLEAN";
        }
        return "STRING";
    }

    private String mapValueDataType(Object value) {
        if (value == null) {
            return "STRING";
        }
        if (value instanceof Number) {
            return "NUMBER";
        }
        if (value instanceof java.sql.Date) {
            return "DATE";
        }
        if (value instanceof java.sql.Timestamp || value instanceof java.util.Date) {
            return "DATETIME";
        }
        if (value instanceof Boolean) {
            return "BOOLEAN";
        }
        return "STRING";
    }

    private boolean isNumericType(String dbType) {
        if (dbType == null) {
            return false;
        }
        String upper = dbType.toUpperCase();
        return upper.contains("INT") || upper.contains("DECIMAL") || upper.contains("FLOAT")
            || upper.contains("DOUBLE") || upper.contains("NUMERIC");
    }

    private String extractSchemaFromUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        int start = jdbcUrl.lastIndexOf('/');
        if (start < 0) {
            return null;
        }
        int end = jdbcUrl.indexOf('?');
        if (end < 0) {
            end = jdbcUrl.length();
        }
        return jdbcUrl.substring(start + 1, end);
    }
}

package com.mdframe.forge.plugin.generator.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.constant.GenDatasourceRuntime;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.mapper.GenDatasourceMapper;
import com.mdframe.forge.plugin.generator.service.IGenDatasourceService;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialectFactory;
import com.mdframe.forge.plugin.generator.util.DynamicDataSourceUtil;
import com.mdframe.forge.plugin.generator.util.GenDatasourcePasswordCodec;
import com.mdframe.forge.plugin.generator.util.GenUtils;
import com.mdframe.forge.starter.core.domain.PageQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * 代码生成器数据源配置Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenDatasourceServiceImpl extends ServiceImpl<GenDatasourceMapper, GenDatasource> implements IGenDatasourceService {

    private final GenDatasourceMapper genDatasourceMapper;
    private final RuntimeDatabaseDialectFactory dialectFactory;
    private final LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver;

    @Override
    public Page<GenDatasource> selectDatasourcePage(PageQuery pageQuery, String datasourceName, String usageScope) {
        return genDatasourceMapper.selectDatasourcePage(
                pageQuery.toPage(), datasourceName, normalizeUsageScopeFilter(usageScope));
    }

    @Override
    public List<GenDatasource> selectEnabledDatasources(String usageScope) {
        return genDatasourceMapper.selectEnabledDatasources(normalizeUsageScopeFilter(usageScope));
    }

    @Override
    public boolean save(GenDatasource entity) {
        applyRuntimeDefaults(entity, null);
        // 保存前加密密码
        if (StrUtil.isNotBlank(entity.getPassword())) {
            entity.setPassword(GenDatasourcePasswordCodec.encrypt(entity.getPassword()));
        }
        boolean saved = super.save(entity);
        if (saved) {
            runtimeDataSourceResolver.evictDatasource(entity.getDatasourceId(), entity.getDatasourceCode());
        }
        return saved;
    }

    @Override
    public boolean updateById(GenDatasource entity) {
        GenDatasource existing = entity.getDatasourceId() == null ? null : genDatasourceMapper.selectById(entity.getDatasourceId());
        applyRuntimeDefaults(entity, existing);
        // 更新时如果密码不为空，则加密
        if (StrUtil.isNotBlank(entity.getPassword())) {
            entity.setPassword(GenDatasourcePasswordCodec.encrypt(entity.getPassword()));
        }
        boolean updated = super.updateById(entity);
        if (updated && entity.getDatasourceId() != null) {
            DynamicDataSourceUtil.removeDataSource(entity.getDatasourceId());
            runtimeDataSourceResolver.evictDatasource(entity.getDatasourceId(),
                    existing == null ? null : existing.getDatasourceCode());
            runtimeDataSourceResolver.evictDatasource(entity.getDatasourceId(), entity.getDatasourceCode());
        }
        return updated;
    }

    @Override
    public boolean removeById(Serializable id) {
        GenDatasource existing = id == null ? null : genDatasourceMapper.selectById(id);
        boolean removed = super.removeById(id);
        if (removed && id != null) {
            Long datasourceId = Long.valueOf(String.valueOf(id));
            DynamicDataSourceUtil.removeDataSource(datasourceId);
            runtimeDataSourceResolver.evictDatasource(datasourceId, existing == null ? null : existing.getDatasourceCode());
        }
        return removed;
    }

    @Override
    public boolean testConnection(Long datasourceId) {
        GenDatasource datasource = genDatasourceMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new RuntimeException("数据源不存在");
        }
        // 解密密码用于测试连接
        if (StrUtil.isNotBlank(datasource.getPassword())) {
            datasource.setPassword(GenDatasourcePasswordCodec.decrypt(datasource.getPassword()));
        }
        return DynamicDataSourceUtil.testConnection(datasource);
    }

    @Override
    public List<GenTable> selectDbTableList(Long datasourceId) {
        GenDatasource datasource = getDatasourceById(datasourceId);
        RuntimeDatabaseDialect dialect = dialect(datasource);
        try {
            return jdbcTemplate(datasource).query(dialect.listImportTablesSql(),
                    (rs, rowNum) -> mapGenTable(rs, datasourceId));
        } catch (Exception e) {
            log.error("查询数据源表列表失败: datasourceId={}", datasourceId, e);
            throw new RuntimeException("查询表列表失败: " + e.getMessage());
        }
    }

    @Override
    public GenTable selectDbTableByName(Long datasourceId, String tableName) {
        GenDatasource datasource = getDatasourceById(datasourceId);
        RuntimeDatabaseDialect dialect = dialect(datasource);
        try {
            List<GenTable> tables = jdbcTemplate(datasource).query(dialect.importTableInfoSql(),
                    (rs, rowNum) -> mapGenTable(rs, datasourceId), tableName);
            return tables.isEmpty() ? null : tables.get(0);
        } catch (Exception e) {
            log.error("查询表信息失败: datasourceId={}, tableName={}", datasourceId, tableName, e);
            throw new RuntimeException("查询表信息失败: " + e.getMessage());
        }
    }

    @Override
    public List<GenTableColumn> selectDbTableColumnsByName(Long datasourceId, String tableName) {
        GenDatasource datasource = getDatasourceById(datasourceId);
        RuntimeDatabaseDialect dialect = dialect(datasource);
        try {
            Object[] args = tableNameArgs(tableName, dialect.listImportColumnsTableNameParameterCount());
            return jdbcTemplate(datasource).query(dialect.listImportColumnsSql(),
                    (rs, rowNum) -> mapGenTableColumn(rs), args);
        } catch (Exception e) {
            log.error("查询表字段信息失败: datasourceId={}, tableName={}", datasourceId, tableName, e);
            throw new RuntimeException("查询表字段信息失败: " + e.getMessage());
        }
    }

    @Override
    public GenDatasource getDefaultDatasource() {
        return genDatasourceMapper.selectDefaultDatasource();
    }

    private String normalizeUsageScopeFilter(String usageScope) {
        String normalized = GenDatasourceRuntime.normalizeUsageScopeFilter(usageScope);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    private void applyRuntimeDefaults(GenDatasource entity, GenDatasource existing) {
        String usageScope = StrUtil.blankToDefault(entity.getUsageScope(), existing == null ? null : existing.getUsageScope());
        entity.setUsageScope(GenDatasourceRuntime.normalizeUsageScope(usageScope));

        String dbType = StrUtil.blankToDefault(entity.getDbType(), existing == null ? null : existing.getDbType());
        if (StrUtil.isNotBlank(dbType)) {
            entity.setDbType(dbType);
            applyTestQueryDefault(entity, existing, dbType);
        }

        String requestedRiskLevel = entity.getRiskLevel();
        String riskLevel = StrUtil.blankToDefault(requestedRiskLevel, existing == null ? null : existing.getRiskLevel());
        entity.setRiskLevel(GenDatasourceRuntime.normalizeRiskLevel(riskLevel));

        boolean highRisk = GenDatasourceRuntime.RISK_HIGH.equals(entity.getRiskLevel());
        boolean explicitlyChangedToHighRisk = StrUtil.isNotBlank(requestedRiskLevel) && highRisk;

        if (entity.getReadonly() == null) {
            if (explicitlyChangedToHighRisk || existing == null || existing.getReadonly() == null) {
                entity.setReadonly(highRisk ? 1 : 0);
            } else {
                entity.setReadonly(existing.getReadonly());
            }
        }

        if (entity.getAllowRuntimeDdl() == null) {
            if (!explicitlyChangedToHighRisk && existing != null && existing.getAllowRuntimeDdl() != null) {
                entity.setAllowRuntimeDdl(existing.getAllowRuntimeDdl());
            } else {
                entity.setAllowRuntimeDdl(0);
            }
        }

        if (entity.getAllowRuntimeWrite() == null) {
            if (existing != null && existing.getAllowRuntimeWrite() != null && !Integer.valueOf(1).equals(entity.getReadonly())) {
                entity.setAllowRuntimeWrite(existing.getAllowRuntimeWrite());
            } else {
                entity.setAllowRuntimeWrite(Integer.valueOf(1).equals(entity.getReadonly()) ? 0 : 1);
            }
        }

        if (Integer.valueOf(1).equals(entity.getReadonly())) {
            entity.setAllowRuntimeWrite(0);
            entity.setAllowRuntimeDdl(0);
        }
    }

    private void applyTestQueryDefault(GenDatasource entity, GenDatasource existing, String dbType) {
        String defaultTestQuery = defaultTestQuery(dbType);
        String requested = entity.getTestQuery();
        if (StrUtil.isBlank(requested)) {
            entity.setTestQuery(existing != null && StrUtil.isNotBlank(existing.getTestQuery())
                    ? existing.getTestQuery()
                    : defaultTestQuery);
            return;
        }
        boolean dbTypeChanged = existing != null
                && (existing.getDbType() == null || !dbType.equalsIgnoreCase(existing.getDbType()));
        if (isKnownDefaultTestQuery(requested)
                && (existing == null || dbTypeChanged || !requested.equals(existing.getTestQuery()))) {
            entity.setTestQuery(defaultTestQuery);
        }
    }

    private String defaultTestQuery(String dbType) {
        try {
            return dialectFactory.resolve(dbType).defaultTestQuery();
        } catch (Exception e) {
            return "SELECT 1";
        }
    }

    private boolean isKnownDefaultTestQuery(String testQuery) {
        String normalized = StrUtil.trim(testQuery);
        return "SELECT 1".equalsIgnoreCase(normalized)
                || "SELECT 1 FROM DUAL".equalsIgnoreCase(normalized);
    }

    private RuntimeDatabaseDialect dialect(GenDatasource datasource) {
        return dialectFactory.resolve(datasource == null ? null : datasource.getDbType());
    }

    private JdbcTemplate jdbcTemplate(GenDatasource datasource) {
        return new JdbcTemplate(DynamicDataSourceUtil.getDataSource(datasource));
    }

    private Object[] tableNameArgs(String tableName, int count) {
        Object[] args = new Object[Math.max(1, count)];
        for (int i = 0; i < args.length; i++) {
            args[i] = tableName;
        }
        return args;
    }

    private GenTable mapGenTable(ResultSet rs, Long datasourceId) throws SQLException {
        GenTable table = new GenTable();
        table.setDatasourceId(datasourceId);
        table.setTableName(rs.getString("table_name"));
        table.setTableComment(rs.getString("table_comment"));
        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            table.setCreateTime(createTime.toLocalDateTime());
        }
        Timestamp updateTime = rs.getTimestamp("update_time");
        if (updateTime != null) {
            table.setUpdateTime(updateTime.toLocalDateTime());
        }
        return table;
    }

    private GenTableColumn mapGenTableColumn(ResultSet rs) throws SQLException {
        GenTableColumn column = new GenTableColumn();
        column.setColumnName(rs.getString("column_name"));
        column.setColumnComment(rs.getString("column_comment"));
        column.setColumnType(rs.getString("column_type"));
        column.setIsPk(rs.getInt("is_pk"));
        column.setIsIncrement(rs.getInt("is_increment"));
        column.setIsRequired(rs.getInt("is_required"));
        GenUtils.initColumnField(column);
        return column;
    }

    /**
     * 根据ID获取数据源
     */
    private GenDatasource getDatasourceById(Long datasourceId) {
        GenDatasource datasource = genDatasourceMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new RuntimeException("数据源不存在: datasourceId=" + datasourceId);
        }
        if (datasource.getIsEnabled() == 0) {
            throw new RuntimeException("数据源已禁用: " + datasource.getDatasourceName());
        }
        // 解密密码用于连接
        if (StrUtil.isNotBlank(datasource.getPassword())) {
            datasource.setPassword(GenDatasourcePasswordCodec.decrypt(datasource.getPassword()));
        }
        return datasource;
    }
}

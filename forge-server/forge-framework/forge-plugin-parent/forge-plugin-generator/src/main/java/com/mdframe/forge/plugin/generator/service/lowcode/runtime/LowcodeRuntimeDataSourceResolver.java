package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAuditStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeLogicDeleteStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeDatasourceSnapshot;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeSourceTableRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTenantStrategy;
import com.mdframe.forge.plugin.generator.mapper.GenDatasourceMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 低代码运行时数据源解析器。
 */
@Service
@RequiredArgsConstructor
public class LowcodeRuntimeDataSourceResolver {

    private static final long DATASOURCE_CACHE_TTL_MILLIS = TimeUnit.SECONDS.toMillis(30);
    private static final int DATASOURCE_CACHE_MAX_SIZE = 512;
    private static final String DEFAULT_DB_TYPE = "MySQL";
    private static final String DEFAULT_PRIMARY_KEY = "id";
    private static final String TENANT_MODE_FORGE = "FORGE_TENANT_ID";
    private static final String AUDIT_MODE_FORGE_COLUMNS = "FORGE_COLUMNS";
    private static final String LOGIC_MODE_DEL_FLAG = "DEL_FLAG";

    private final ObjectMapper objectMapper;
    private final GenDatasourceMapper datasourceMapper;
    private final Map<Long, CacheEntry<GenDatasource>> datasourceByIdCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<GenDatasource>> datasourceByCodeCache = new ConcurrentHashMap<>();

    public LowcodeRuntimeDataSourceContext resolve(LowcodeModelSchema modelSchema) {
        if (modelSchema == null) {
            throw new BusinessException("低代码模型协议不能为空");
        }
        LowcodeRuntimeDatasourceSnapshot snapshot = resolveModelSnapshot(modelSchema);
        String tableName = StringUtils.firstNonBlank(
            snapshot == null ? null : snapshot.getTableName(),
            modelSchema.getTableName(),
            modelSchema.getSourceTable() == null ? null : modelSchema.getSourceTable().getTableName()
        );
        LowcodeRuntimeDataSourceContext context = resolveBySnapshot(snapshot, tableName, modelSchema.getTableMode());
        context.setPrimaryKey(defaultPrimaryKey(modelSchema.getPrimaryKey()));
        context.setTenantStrategy(defaultTenantStrategy(modelSchema.getTenantStrategy()));
        context.setAuditStrategy(defaultAuditStrategy(modelSchema.getAuditStrategy()));
        context.setLogicDeleteStrategy(defaultLogicDeleteStrategy(modelSchema.getLogicDeleteStrategy()));
        return context;
    }

    public LowcodeRuntimeDataSourceContext resolve(AiCrudConfig config) {
        if (config == null) {
            throw new BusinessException("低代码应用配置不能为空");
        }
        LowcodeModelSchema modelSchema = readModelSchema(config);
        LowcodeRuntimeDatasourceSnapshot snapshot = readJson(
            config.getRuntimeDatasourceSnapshot(), LowcodeRuntimeDatasourceSnapshot.class, "runtimeDatasourceSnapshot");
        if (snapshot == null && modelSchema != null) {
            snapshot = resolveModelSnapshot(modelSchema);
        }
        if (snapshot == null && (config.getRuntimeDatasourceId() != null
            || StringUtils.isNotBlank(config.getRuntimeDatasourceCode()))) {
            snapshot = new LowcodeRuntimeDatasourceSnapshot();
            snapshot.setDatasourceId(config.getRuntimeDatasourceId());
            snapshot.setDatasourceCode(config.getRuntimeDatasourceCode());
        }
        String tableName = StringUtils.firstNonBlank(
            config.getRuntimeTableName(),
            snapshot == null ? null : snapshot.getTableName(),
            config.getTableName(),
            modelSchema == null ? null : modelSchema.getTableName()
        );
        LowcodeRuntimeDataSourceContext context = resolveBySnapshot(snapshot, tableName,
            modelSchema == null ? null : modelSchema.getTableMode());
        context.setPrimaryKey(defaultPrimaryKey(resolveConfigPrimaryKey(config, modelSchema)));
        context.setTenantStrategy(defaultTenantStrategy(readJson(
            config.getTenantStrategy(), LowcodeTenantStrategy.class, "tenantStrategy",
            modelSchema == null ? null : modelSchema.getTenantStrategy())));
        context.setAuditStrategy(defaultAuditStrategy(readJson(
            config.getAuditStrategy(), LowcodeAuditStrategy.class, "auditStrategy",
            modelSchema == null ? null : modelSchema.getAuditStrategy())));
        context.setLogicDeleteStrategy(defaultLogicDeleteStrategy(readJson(
            config.getLogicDeleteStrategy(), LowcodeLogicDeleteStrategy.class, "logicDeleteStrategy",
            modelSchema == null ? null : modelSchema.getLogicDeleteStrategy())));
        return context;
    }

    public LowcodeRuntimeDatasourceSnapshot buildSnapshot(GenDatasource datasource, String tableName, String tableMode) {
        if (datasource == null) {
            return null;
        }
        LowcodeRuntimeDatasourceSnapshot snapshot = new LowcodeRuntimeDatasourceSnapshot();
        snapshot.setDatasourceId(datasource.getDatasourceId());
        snapshot.setDatasourceCode(datasource.getDatasourceCode());
        snapshot.setDatasourceName(datasource.getDatasourceName());
        snapshot.setDbType(StringUtils.defaultIfBlank(datasource.getDbType(), DEFAULT_DB_TYPE));
        snapshot.setTableName(tableName);
        snapshot.setTableMode(tableMode);
        snapshot.setUsageScope(datasource.getUsageScope());
        snapshot.setReadonly(Integer.valueOf(1).equals(datasource.getReadonly()));
        snapshot.setAllowWrite(!Boolean.TRUE.equals(snapshot.getReadonly())
            && Integer.valueOf(1).equals(datasource.getAllowRuntimeWrite()));
        snapshot.setAllowDdl(!Boolean.TRUE.equals(snapshot.getReadonly())
            && Integer.valueOf(1).equals(datasource.getAllowRuntimeDdl()));
        snapshot.setRiskLevel(datasource.getRiskLevel());
        return snapshot;
    }

    public GenDatasource getDatasourceById(Long datasourceId) {
        if (datasourceId == null) {
            return null;
        }
        return getCachedDatasourceById(datasourceId);
    }

    public void evictDatasource(Long datasourceId, String datasourceCode) {
        if (datasourceId != null) {
            datasourceByIdCache.remove(datasourceId);
        }
        String codeKey = normalizeDatasourceCode(datasourceCode);
        if (StringUtils.isNotBlank(codeKey)) {
            datasourceByCodeCache.remove(codeKey);
        }
    }

    public void clearDatasourceCache() {
        datasourceByIdCache.clear();
        datasourceByCodeCache.clear();
    }

    private LowcodeRuntimeDatasourceSnapshot resolveModelSnapshot(LowcodeModelSchema modelSchema) {
        if (modelSchema.getRuntimeDatasource() != null) {
            return modelSchema.getRuntimeDatasource();
        }
        LowcodeSourceTableRef sourceTable = modelSchema.getSourceTable();
        if (sourceTable == null || (sourceTable.getDatasourceId() == null
            && StringUtils.isBlank(sourceTable.getDatasourceCode()))) {
            return null;
        }
        LowcodeRuntimeDatasourceSnapshot snapshot = new LowcodeRuntimeDatasourceSnapshot();
        snapshot.setDatasourceId(sourceTable.getDatasourceId());
        snapshot.setDatasourceCode(sourceTable.getDatasourceCode());
        snapshot.setDatasourceName(sourceTable.getDatasourceName());
        snapshot.setDbType(sourceTable.getDbType());
        snapshot.setTableName(StringUtils.firstNonBlank(sourceTable.getTableName(), modelSchema.getTableName()));
        snapshot.setTableMode(StringUtils.defaultIfBlank(modelSchema.getTableMode(), "EXISTING"));
        return snapshot;
    }

    private LowcodeRuntimeDataSourceContext resolveBySnapshot(LowcodeRuntimeDatasourceSnapshot snapshot,
                                                             String tableName,
                                                             String tableMode) {
        if (snapshot == null || (snapshot.getDatasourceId() == null
            && StringUtils.isBlank(snapshot.getDatasourceCode()))) {
            LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContext.master(tableName);
            context.setTableMode(tableMode);
            context.setSnapshot(null);
            return context;
        }
        GenDatasource datasource = findDatasource(snapshot);
        if (datasource == null) {
            throw new BusinessException("运行数据源不存在: " + StringUtils.firstNonBlank(
                snapshot.getDatasourceCode(),
                snapshot.getDatasourceId() == null ? null : String.valueOf(snapshot.getDatasourceId())));
        }
        if (EnableStatus.DISABLED.matches(datasource.getIsEnabled())) {
            throw new BusinessException("运行数据源已禁用: " + datasource.getDatasourceName());
        }
        String effectiveTableName = StringUtils.firstNonBlank(tableName, snapshot.getTableName());
        LowcodeRuntimeDatasourceSnapshot freshSnapshot = buildSnapshot(datasource, effectiveTableName,
            StringUtils.firstNonBlank(tableMode, snapshot.getTableMode()));

        LowcodeRuntimeDataSourceContext context = new LowcodeRuntimeDataSourceContext();
        context.setMaster(false);
        context.setDatasourceId(datasource.getDatasourceId());
        context.setDatasourceCode(datasource.getDatasourceCode());
        context.setDatasourceName(datasource.getDatasourceName());
        context.setDbType(StringUtils.defaultIfBlank(datasource.getDbType(), DEFAULT_DB_TYPE));
        context.setTableName(effectiveTableName);
        context.setTableMode(freshSnapshot.getTableMode());
        context.setAllowWrite(Boolean.TRUE.equals(freshSnapshot.getAllowWrite()));
        context.setAllowDdl(Boolean.TRUE.equals(freshSnapshot.getAllowDdl()));
        context.setReadonly(Boolean.TRUE.equals(freshSnapshot.getReadonly()));
        context.setRiskLevel(datasource.getRiskLevel());
        context.setSnapshot(freshSnapshot);
        return context;
    }

    private GenDatasource findDatasource(LowcodeRuntimeDatasourceSnapshot snapshot) {
        // ID 优先；库被重建/手工改过后 ID 可能失效，再按编码回退，避免整站低代码报“运行数据源不存在”
        if (snapshot.getDatasourceId() != null) {
            GenDatasource byId = getCachedDatasourceById(snapshot.getDatasourceId());
            if (byId != null) {
                return byId;
            }
        }
        if (StringUtils.isBlank(snapshot.getDatasourceCode())) {
            return null;
        }
        return getCachedDatasourceByCode(snapshot.getDatasourceCode());
    }

    private GenDatasource getCachedDatasourceById(Long datasourceId) {
        long now = System.currentTimeMillis();
        CacheEntry<GenDatasource> cached = datasourceByIdCache.get(datasourceId);
        if (cached != null && cached.expiresAt() > now) {
            return copyDatasource(cached.value());
        }
        GenDatasource datasource = datasourceMapper.selectById(datasourceId);
        cacheDatasource(datasource, now);
        return copyDatasource(datasource);
    }

    private GenDatasource getCachedDatasourceByCode(String datasourceCode) {
        String codeKey = normalizeDatasourceCode(datasourceCode);
        if (StringUtils.isBlank(codeKey)) {
            return null;
        }
        long now = System.currentTimeMillis();
        CacheEntry<GenDatasource> cached = datasourceByCodeCache.get(codeKey);
        if (cached != null && cached.expiresAt() > now) {
            return copyDatasource(cached.value());
        }
        GenDatasource datasource = datasourceMapper.selectOne(new LambdaQueryWrapper<GenDatasource>()
            .eq(GenDatasource::getDatasourceCode, datasourceCode)
            .last("LIMIT 1"));
        cacheDatasource(datasource, now);
        return copyDatasource(datasource);
    }

    private void cacheDatasource(GenDatasource datasource, long now) {
        if (datasource == null) {
            return;
        }
        trimExpiredCacheIfNeeded(now);
        CacheEntry<GenDatasource> entry = new CacheEntry<>(
            copyDatasource(datasource), now + DATASOURCE_CACHE_TTL_MILLIS);
        if (datasource.getDatasourceId() != null) {
            datasourceByIdCache.put(datasource.getDatasourceId(), entry);
        }
        String codeKey = normalizeDatasourceCode(datasource.getDatasourceCode());
        if (StringUtils.isNotBlank(codeKey)) {
            datasourceByCodeCache.put(codeKey, entry);
        }
    }

    private void trimExpiredCacheIfNeeded(long now) {
        if (datasourceByIdCache.size() > DATASOURCE_CACHE_MAX_SIZE) {
            datasourceByIdCache.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        }
        if (datasourceByCodeCache.size() > DATASOURCE_CACHE_MAX_SIZE) {
            datasourceByCodeCache.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        }
    }

    private GenDatasource copyDatasource(GenDatasource datasource) {
        return datasource == null ? null : BeanUtil.copyProperties(datasource, GenDatasource.class);
    }

    private String normalizeDatasourceCode(String datasourceCode) {
        return StringUtils.trimToEmpty(datasourceCode);
    }

    private LowcodeModelSchema readModelSchema(AiCrudConfig config) {
        if (StringUtils.isBlank(config.getModelSchema())) {
            return null;
        }
        return readJson(config.getModelSchema(), LowcodeModelSchema.class, "modelSchema");
    }

    private LowcodePrimaryKeyStrategy resolveConfigPrimaryKey(AiCrudConfig config, LowcodeModelSchema modelSchema) {
        if (StringUtils.isNotBlank(config.getPrimaryKeyField()) || StringUtils.isNotBlank(config.getPrimaryKeyColumn())) {
            LowcodePrimaryKeyStrategy primaryKey = new LowcodePrimaryKeyStrategy();
            primaryKey.setField(config.getPrimaryKeyField());
            primaryKey.setColumnName(config.getPrimaryKeyColumn());
            primaryKey.setDataType(config.getPrimaryKeyType());
            primaryKey.setAutoIncrement(true);
            return primaryKey;
        }
        return modelSchema == null ? null : modelSchema.getPrimaryKey();
    }

    private LowcodePrimaryKeyStrategy defaultPrimaryKey(LowcodePrimaryKeyStrategy primaryKey) {
        LowcodePrimaryKeyStrategy value = primaryKey == null ? new LowcodePrimaryKeyStrategy() : primaryKey;
        value.setField(StringUtils.defaultIfBlank(value.getField(), DEFAULT_PRIMARY_KEY));
        value.setColumnName(StringUtils.defaultIfBlank(value.getColumnName(), DEFAULT_PRIMARY_KEY));
        value.setDataType(StringUtils.defaultIfBlank(value.getDataType(), "bigint"));
        value.setAutoIncrement(value.getAutoIncrement() == null || value.getAutoIncrement());
        return value;
    }

    private LowcodeTenantStrategy defaultTenantStrategy(LowcodeTenantStrategy tenantStrategy) {
        LowcodeTenantStrategy value = tenantStrategy == null ? new LowcodeTenantStrategy() : tenantStrategy;
        value.setMode(StringUtils.defaultIfBlank(value.getMode(), TENANT_MODE_FORGE));
        value.setColumnName(StringUtils.defaultIfBlank(value.getColumnName(), "tenant_id"));
        return value;
    }

    private LowcodeAuditStrategy defaultAuditStrategy(LowcodeAuditStrategy auditStrategy) {
        LowcodeAuditStrategy value = auditStrategy == null ? new LowcodeAuditStrategy() : auditStrategy;
        value.setMode(StringUtils.defaultIfBlank(value.getMode(), AUDIT_MODE_FORGE_COLUMNS));
        value.setCreateByColumn(StringUtils.defaultIfBlank(value.getCreateByColumn(), "create_by"));
        value.setCreateTimeColumn(StringUtils.defaultIfBlank(value.getCreateTimeColumn(), "create_time"));
        value.setCreateDeptColumn(StringUtils.defaultIfBlank(value.getCreateDeptColumn(), "create_dept"));
        value.setUpdateByColumn(StringUtils.defaultIfBlank(value.getUpdateByColumn(), "update_by"));
        value.setUpdateTimeColumn(StringUtils.defaultIfBlank(value.getUpdateTimeColumn(), "update_time"));
        return value;
    }

    private LowcodeLogicDeleteStrategy defaultLogicDeleteStrategy(LowcodeLogicDeleteStrategy logicDeleteStrategy) {
        LowcodeLogicDeleteStrategy value = logicDeleteStrategy == null ? new LowcodeLogicDeleteStrategy() : logicDeleteStrategy;
        value.setMode(StringUtils.defaultIfBlank(value.getMode(), LOGIC_MODE_DEL_FLAG));
        value.setColumnName(StringUtils.defaultIfBlank(value.getColumnName(), "del_flag"));
        value.setActiveValue(StringUtils.defaultIfBlank(value.getActiveValue(), "0"));
        value.setDeletedValue(StringUtils.defaultIfBlank(value.getDeletedValue(), "1"));
        return value;
    }

    private <T> T readJson(String json, Class<T> type, String fieldName) {
        return readJson(json, type, fieldName, null);
    }

    private <T> T readJson(String json, Class<T> type, String fieldName, T defaultValue) {
        if (StringUtils.isBlank(json)) {
            return defaultValue;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new BusinessException(fieldName + "格式不正确");
        }
    }

    private record CacheEntry<T>(T value, long expiresAt) {
    }
}

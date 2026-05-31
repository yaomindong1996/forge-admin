package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.CustomQueryConditionDTO;
import com.mdframe.forge.plugin.generator.dto.CustomQueryExecuteDTO;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTreeConfig;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.crypto.Encryptor;
import com.mdframe.forge.starter.crypto.crypto.EncryptorFactory;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategy;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategyFactory;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeType;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态CRUD服务
 * 基于DynamicCrudRepository实现，支持配置驱动的通用CRUD操作
 * 
 * @author forge
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicCrudService {

    private static final int MAX_EXPORT_ROWS = 10000;
    private static final int MAX_TREE_ROWS = 5000;
    private static final String MASTER_DETAIL_LAYOUT = "master-detail-crud";
    private static final String TREE_LAYOUT = "tree-crud";
    private static final Set<String> IMMUTABLE_WRITE_FIELDS = Set.of(
            "id", "tenantId", "tenant_id", "createBy", "create_by", "createTime", "create_time",
            "createDept", "create_dept", "updateBy", "update_by", "updateTime", "update_time", "delFlag", "del_flag"
    );

    private record RuntimeFieldRef(String fieldName,
                                   String modelCode,
                                   String sourceField,
                                   String columnName,
                                   String tableName,
                                   String tableAlias,
                                   boolean primary) {
    }

    private record RuntimeChildRelation(String modelCode,
                                        String tableName,
                                        String tableAlias,
                                        String childFkColumn,
                                        String mainColumn,
                                        Map<String, RuntimeFieldRef> fields) {
    }

    private record RuntimeJoinContext(Map<String, RuntimeFieldRef> fields,
                                      List<RuntimeChildRelation> childRelations,
                                      List<DynamicCrudRepository.JoinField> selectFields,
                                      List<DynamicCrudRepository.JoinSpec> joins,
                                      Map<String, String> fieldColumnMapping,
                                      Map<String, String> relationDisplayAliases) {
    }

    private record ExportQueryContext(AiCrudConfig config,
                                      String tableName,
                                      Map<String, String> columnMapping,
                                      Set<String> allowedSearchFields,
                                      Map<String, String> searchTypeMap,
                                      Map<String, Object> searchParams,
                                      RuntimeJoinContext joinContext) {
    }

    private final DynamicCrudRepository repository;
    private final AiCrudConfigService configService;
    private final ObjectMapper objectMapper;
    private final DictValueProvider dictValueProvider;
    private final DesensitizeStrategyFactory desensitizeStrategyFactory;
    private final EncryptorFactory encryptorFactory;
    private final DynamicDataScopeService dynamicDataScopeService;

    // ==================== 查询操作 ====================

    /**
     * 分页查询
     */
    public Page<Map<String, Object>> selectPage(String configKey, PageQuery pageQuery, DynamicCrudQuery query) {
        // 1. 加载配置
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 2. 获取字段映射
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        
        // 3. 解析搜索配置
        Set<String> allowedSearchFields = buildAllowedSearchFields(config);
        Map<String, String> searchTypeMap = DynamicQueryGenerator.extractSearchTypeMap(config.getSearchSchema(), objectMapper);
        
        // 4. 构建搜索条件
        Map<String, Object> searchParams = (query != null) ? query.getSearchParams() : null;
        searchParams = expandIncludeChildrenParams(searchParams, config, tableName, allowedSearchFields, searchTypeMap);

        RuntimeJoinContext joinContext = buildRuntimeJoinContext(config);
        if (joinContext != null && requiresJoinedPageQuery(config, pageQuery, searchParams, joinContext)) {
            DynamicCrudRepository.SqlCondition dataScopeCondition = buildDataScopeCondition(config, tableName, "t0");
            Page<Map<String, Object>> page = repository.selectJoinedPage(
                    tableName,
                    buildRuntimeSelectFields(joinContext, DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), true),
                    joinContext.joins(),
                    pageQuery.getPageNum(),
                    pageQuery.getPageSize(),
                    searchParams,
                    allowedSearchFields,
                    searchTypeMap,
                    joinContext.fieldColumnMapping(),
                    buildJoinOrderBy(pageQuery.getOrderByColumn(), pageQuery.getIsAsc(), joinContext),
                    dataScopeCondition
            );
            applyReadPipeline(page.getRecords(), config);
            return page;
        }
        
        // 5. 构建排序
        String orderBy = DynamicQueryGenerator.buildOrderByClause(
                pageQuery.getOrderByColumn(), pageQuery.getIsAsc(), columnMapping);
        
        // 6. 执行分页查询
        Page<Map<String, Object>> page = repository.selectPage(
                tableName,
                pageQuery.getPageNum(),
                pageQuery.getPageSize(),
                searchParams,
                allowedSearchFields,
                searchTypeMap,
                columnMapping,
                orderBy,
                buildDataScopeCondition(config, tableName, null)
        );
        
        // 7. 转换字段名为camelCase
        List<Map<String, Object>> camelCaseRecords = DynamicQueryGenerator.convertListToCamelCase(page.getRecords());
        
        // 8. 读取链路统一先解密，再做翻译和脱敏，避免密文参与展示处理
        applyReadPipeline(camelCaseRecords, config);
        
        page.setRecords(camelCaseRecords);
        return page;
    }

    /**
     * 查询动态导出数据，复用动态 CRUD 的字段白名单、解密、字典翻译和脱敏链路。
     */
    public List<Map<String, Object>> selectExportRows(String configKey,
                                                      DynamicCrudQuery query,
                                                      Integer maxRows) {
        int limit = normalizeExportLimit(maxRows);
        return selectExportPageRows(configKey, query, 1, limit, null);
    }

    /**
     * 统计动态导出数据量，供同步/异步导出决策使用。
     */
    public long countExportRows(String configKey,
                                DynamicCrudQuery query,
                                DataScopeContext dataScopeContext) {
        ExportQueryContext context = buildExportQueryContext(configKey, query);
        RuntimeJoinContext joinContext = context.joinContext();
        if (joinContext != null && requiresJoinedExportQuery(context.config(), context.searchParams(), joinContext)) {
            DynamicCrudRepository.SqlCondition dataScopeCondition = buildDataScopeCondition(
                    context.config(), context.tableName(), "t0", dataScopeContext);
            return repository.countJoined(
                    context.tableName(),
                    buildRuntimeSelectFields(joinContext,
                            DynamicQueryGenerator.extractFieldNames(context.config().getColumnsSchema(), objectMapper), true),
                    joinContext.joins(),
                    context.searchParams(),
                    context.allowedSearchFields(),
                    context.searchTypeMap(),
                    joinContext.fieldColumnMapping(),
                    dataScopeCondition
            );
        }

        return repository.countList(
                context.tableName(),
                context.searchParams(),
                context.allowedSearchFields(),
                context.searchTypeMap(),
                context.columnMapping(),
                buildDataScopeCondition(context.config(), context.tableName(), null, dataScopeContext)
        );
    }

    /**
     * 分页查询动态导出数据，不重复统计 count，供异步导出分批写入使用。
     */
    public List<Map<String, Object>> selectExportPageRows(String configKey,
                                                          DynamicCrudQuery query,
                                                          Integer pageNum,
                                                          Integer pageSize,
                                                          DataScopeContext dataScopeContext) {
        ExportQueryContext context = buildExportQueryContext(configKey, query);
        int current = normalizePageNum(pageNum);
        int size = normalizeExportPageSize(pageSize);
        RuntimeJoinContext joinContext = context.joinContext();
        if (joinContext != null && requiresJoinedExportQuery(context.config(), context.searchParams(), joinContext)) {
            List<Map<String, Object>> rows = repository.selectJoinedPageRecords(
                    context.tableName(),
                    buildRuntimeSelectFields(joinContext,
                            DynamicQueryGenerator.extractFieldNames(context.config().getColumnsSchema(), objectMapper), true),
                    joinContext.joins(),
                    current,
                    size,
                    context.searchParams(),
                    context.allowedSearchFields(),
                    context.searchTypeMap(),
                    joinContext.fieldColumnMapping(),
                    "t0.id DESC",
                    buildDataScopeCondition(context.config(), context.tableName(), "t0", dataScopeContext)
            );
            applyReadPipeline(rows, context.config());
            return rows;
        }

        List<Map<String, Object>> rows = repository.selectPageRecords(
                context.tableName(),
                current,
                size,
                context.searchParams(),
                context.allowedSearchFields(),
                context.searchTypeMap(),
                context.columnMapping(),
                "id DESC",
                buildDataScopeCondition(context.config(), context.tableName(), null, dataScopeContext)
        );

        List<Map<String, Object>> camelCaseRows = DynamicQueryGenerator.convertListToCamelCase(rows);
        applyReadPipeline(camelCaseRows, context.config());
        return camelCaseRows;
    }

    /**
     * 查询树形导航数据，供树形单表模板左侧树使用。
     */
    public List<Map<String, Object>> selectTree(String configKey) {
        return selectTree(configKey, null, null, null);
    }

    /**
     * 查询树形数据。loadMode=full 返回完整树，loadMode=lazy 按 parentValue 返回一层子节点。
     */
    public List<Map<String, Object>> selectTree(String configKey, String parentValue, String parentId, String loadMode) {
        return selectTree(configKey, parentValue, parentId, loadMode, null, null);
    }

    /**
     * 查询树形数据。loadMode=full 返回完整树，loadMode=lazy 按 parentValue 返回一层子节点。
     */
    public List<Map<String, Object>> selectTree(String configKey,
                                                String parentValue,
                                                String parentId,
                                                String loadMode,
                                                String orderByColumn,
                                                String isAsc) {
        AiCrudConfig config = getConfig(configKey);
        LowcodeTreeConfig treeConfig = resolveTreeConfig(config);
        String tableName = StringUtils.defaultIfBlank(treeConfig.getSourceTableName(), config.getTableName());
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        String keyColumn = columnMapping.getOrDefault(treeConfig.getKeyField(),
                DynamicQueryGenerator.camelToSnake(treeConfig.getKeyField()));
        String parentColumn = columnMapping.getOrDefault(treeConfig.getParentField(),
                DynamicQueryGenerator.camelToSnake(treeConfig.getParentField()));
        String orderColumn = keyColumn;
        if (!repository.getTableColumns(tableName).contains(keyColumn)) {
            throw new BusinessException("树形主键字段不存在: " + treeConfig.getKeyField());
        }
        if (!repository.getTableColumns(tableName).contains(parentColumn)) {
            throw new BusinessException("树形父级字段不存在: " + treeConfig.getParentField());
        }
        if (!repository.getTableColumns(tableName).contains(orderColumn)) {
            orderColumn = repository.getTableColumns(tableName).contains("id") ? "id" : parentColumn;
        }
        String orderBy = StringUtils.isBlank(orderByColumn)
                ? orderColumn + " ASC"
                : DynamicQueryGenerator.buildOrderByClause(orderByColumn, isAsc, columnMapping);
        DynamicCrudRepository.SqlCondition dataScopeCondition = buildDataScopeCondition(config, tableName, null);

        String effectiveLoadMode = StringUtils.defaultIfBlank(loadMode, treeConfig.getLoadMode());
        boolean lazyLoad = "lazy".equalsIgnoreCase(effectiveLoadMode);
        if (lazyLoad) {
            String effectiveParentValue = StringUtils.defaultIfBlank(parentValue, parentId);
            List<Map<String, Object>> rows = repository.selectTreeChildren(
                    tableName,
                    parentColumn,
                    effectiveParentValue,
                    orderBy,
                    MAX_TREE_ROWS,
                    dataScopeCondition
            );
            List<Map<String, Object>> camelCaseRows = DynamicQueryGenerator.convertListToCamelCase(rows);
            applyReadPipeline(camelCaseRows, config);
            return buildLazyTreeNodes(camelCaseRows, tableName, parentColumn, treeConfig, dataScopeCondition);
        }

        List<Map<String, Object>> rows = repository.selectList(
                tableName,
                null,
                Collections.emptySet(),
                Collections.emptyMap(),
                columnMapping,
                orderBy,
                MAX_TREE_ROWS,
                dataScopeCondition
        );

        Set<String> ancestorKeys = new LinkedHashSet<>();
        rows = appendTreeAncestorRows(rows, tableName, keyColumn, parentColumn, ancestorKeys);
        List<Map<String, Object>> camelCaseRows = DynamicQueryGenerator.convertListToCamelCase(rows);
        markTreeAncestorRows(camelCaseRows, treeConfig, ancestorKeys);
        applyReadPipeline(camelCaseRows, config);
        return buildTree(camelCaseRows, treeConfig);
    }

    /**
     * 自定义分页查询。
     */
    public Page<Map<String, Object>> selectCustomPage(String configKey, CustomQueryExecuteDTO request) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        Set<String> allowedFields = buildAllowedCustomFields(config);

        RuntimeJoinContext joinContext = buildRuntimeJoinContext(config);
        if (joinContext != null) {
            allowedFields.addAll(joinContext.fields().keySet());
        }
        List<CustomQueryConditionDTO> customConditions = expandCustomIncludeChildrenConditions(
                request.getConditions(), config, tableName, allowedFields);
        if (joinContext != null) {
            if (requiresJoinedCustomQuery(request, joinContext)) {
                DynamicCrudRepository.SqlCondition dataScopeCondition = buildDataScopeCondition(config, tableName, "t0");
                Page<Map<String, Object>> page = repository.selectJoinedCustomPage(
                        tableName,
                        buildRuntimeSelectFields(joinContext, request.getFields(), true),
                        joinContext.joins(),
                        normalizePageNum(request.getPageNum()),
                        normalizePageSize(request.getPageSize()),
                        customConditions,
                        allowedFields,
                        joinContext.fieldColumnMapping(),
                        buildJoinOrderBy(request.getOrderByColumn(), request.getIsAsc(), joinContext),
                        dataScopeCondition
                );
                applyReadPipeline(page.getRecords(), config);
                return page;
            }
        }

        String orderBy = DynamicQueryGenerator.buildOrderByClause(
                request.getOrderByColumn(), request.getIsAsc(), columnMapping);

        Page<Map<String, Object>> page = repository.selectCustomPage(
                tableName,
                normalizePageNum(request.getPageNum()),
                normalizePageSize(request.getPageSize()),
                request.getFields(),
                customConditions,
                allowedFields,
                columnMapping,
                orderBy,
                buildDataScopeCondition(config, tableName, null)
        );

        List<Map<String, Object>> camelCaseRecords = DynamicQueryGenerator.convertListToCamelCase(page.getRecords());
        applyReadPipeline(camelCaseRecords, config);
        page.setRecords(camelCaseRecords);
        return page;
    }

    /**
     * 根据ID查询
     */
    public Map<String, Object> selectById(String configKey, Long id) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();

        RuntimeJoinContext joinContext = buildRuntimeJoinContext(config);
        if (isMasterDetailRuntime(config) && joinContext != null) {
            return selectMasterDetailById(config, id, joinContext);
        }
        if (joinContext != null) {
            Map<String, Object> record = repository.selectJoinedById(
                    tableName,
                    id,
                    joinContext.selectFields(),
                    joinContext.joins(),
                    buildDataScopeCondition(config, tableName, "t0"));
            if (record == null) {
                return null;
            }
            applyReadPipeline(Collections.singletonList(record), config);
            return record;
        }
        
        Map<String, Object> record = repository.selectById(tableName, id, buildDataScopeCondition(config, tableName, null));
        if (record == null) {
            return null;
        }
        
        // 转换为camelCase
        Map<String, Object> camelCaseRecord = DynamicQueryGenerator.convertMapToCamelCase(record);
        
        // 单条读取同样遵循“解密 -> 翻译 -> 脱敏”顺序
        applyReadPipeline(Collections.singletonList(camelCaseRecord), config);
        
        return camelCaseRecord;
    }

    // ==================== 新增操作 ====================

    /**
     * 新增
     */
    @Transactional(rollbackFor = Exception.class)
    public void insert(String configKey, Map<String, Object> data) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 获取字段映射
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        
        // 获取允许写入的字段
        Set<String> allowedFields = DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper);

        RuntimeJoinContext joinContext = buildRuntimeJoinContext(config);
        if (isMasterDetailRuntime(config) && joinContext != null) {
            insertMasterDetailData(config, data, allowedFields, joinContext);
            return;
        }
        if (joinContext != null) {
            insertJoinedData(config, data, allowedFields, joinContext);
            return;
        }
        
        // 过滤并转换字段名
        Map<String, Object> filteredData = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (isImmutableWriteField(entry.getKey())) {
                continue;
            }
            if (allowedFields.contains(entry.getKey())) {
                String columnName = columnMapping.getOrDefault(entry.getKey(), DynamicQueryGenerator.camelToSnake(entry.getKey()));
                filteredData.put(columnName, entry.getValue());
            }
        }
        
        if (filteredData.isEmpty()) {
            throw new BusinessException("没有可写入的字段");
        }
        
        // 应用加密
        applyEncrypt(filteredData, config.getEncryptConfig());
        
        // 执行插入
        repository.insert(tableName, filteredData);
    }

    // ==================== 更新操作 ====================

    /**
     * 更新
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateById(String configKey, Map<String, Object> data) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 获取ID
        Object idValue = resolvePayloadId(data);
        if (idValue == null) {
            throw new BusinessException("更新操作缺少id");
        }
        Long id = Long.valueOf(idValue.toString());
        
        // 获取字段映射
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        
        // 获取允许写入的字段
        Set<String> allowedFields = DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper);

        RuntimeJoinContext joinContext = buildRuntimeJoinContext(config);
        if (isMasterDetailRuntime(config) && joinContext != null) {
            updateMasterDetailData(config, id, data, allowedFields, joinContext);
            return;
        }
        if (joinContext != null) {
            updateJoinedData(config, id, data, allowedFields, joinContext);
            return;
        }
        
        // 过滤并转换字段名
        Map<String, Object> filteredData = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (isImmutableWriteField(key)) {
                continue;
            }
            if (allowedFields.contains(key)) {
                String columnName = columnMapping.getOrDefault(key, DynamicQueryGenerator.camelToSnake(key));
                filteredData.put(columnName, entry.getValue());
            }
        }
        removeMaskedDesensitizedWriteColumns(filteredData, config, tableName);
        
        if (filteredData.isEmpty()) {
            throw new BusinessException("没有可更新的字段");
        }
        
        // 应用加密
        applyEncrypt(filteredData, config.getEncryptConfig());
        
        // 执行更新
        int affected = repository.updateById(tableName, id, filteredData, buildDataScopeCondition(config, tableName, null));
        if (affected <= 0) {
            throw new BusinessException("无权限更新该数据或数据不存在");
        }
    }

    private Map<String, Object> selectMasterDetailById(AiCrudConfig config,
                                                       Long id,
                                                       RuntimeJoinContext joinContext) {
        Map<String, Object> record = repository.selectById(
                config.getTableName(),
                id,
                buildDataScopeCondition(config, config.getTableName(), null));
        if (record == null) {
            return null;
        }
        Map<String, Object> main = DynamicQueryGenerator.convertMapToCamelCase(record);
        applyReadPipeline(Collections.singletonList(main), config);

        Map<String, Object> children = new LinkedHashMap<>();
        for (RuntimeChildRelation relation : joinContext.childRelations()) {
            Object relationValue = resolveMainRelationValue(relation, record, id, record);
            if (relationValue == null) {
                children.put(relation.modelCode(), List.of());
                continue;
            }
            List<Map<String, Object>> rows = repository.selectListByColumn(
                    relation.tableName(), relation.childFkColumn(), relationValue);
            children.put(relation.modelCode(), DynamicQueryGenerator.convertListToCamelCase(rows));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("main", main);
        result.put("children", children);
        return result;
    }

    private void insertMasterDetailData(AiCrudConfig config,
                                        Map<String, Object> data,
                                        Set<String> allowedFields,
                                        RuntimeJoinContext joinContext) {
        Map<String, Object> mainPayload = extractMainPayload(data);
        Map<String, Object> primaryData = filterPrimaryWriteData(mainPayload, allowedFields, joinContext);
        if (primaryData.isEmpty()) {
            throw new BusinessException("没有可写入的主表字段");
        }

        applyEncrypt(primaryData, config.getEncryptConfig());
        Long mainId = repository.insertReturningId(config.getTableName(), primaryData);
        Map<String, Object> currentMainRecord = null;
        Map<String, Object> childrenPayload = extractChildrenPayload(data);
        for (RuntimeChildRelation relation : joinContext.childRelations()) {
            List<Map<String, Object>> childRows = normalizeChildRows(childrenPayload.get(relation.modelCode()));
            if (childRows.isEmpty()) {
                continue;
            }
            if (currentMainRecord == null && !"id".equals(relation.mainColumn()) && !primaryData.containsKey(relation.mainColumn())) {
                currentMainRecord = repository.selectById(config.getTableName(), mainId);
            }
            Object relationValue = resolveMainRelationValue(relation, primaryData, mainId, currentMainRecord);
            if (relationValue == null) {
                continue;
            }
            for (Map<String, Object> row : childRows) {
                Map<String, Object> childData = filterChildWriteData(row, relation);
                if (!hasWritableChildData(childData)) {
                    continue;
                }
                childData.put(relation.childFkColumn(), relationValue);
                repository.insert(relation.tableName(), childData);
            }
        }
    }

    private void updateMasterDetailData(AiCrudConfig config,
                                        Long id,
                                        Map<String, Object> data,
                                        Set<String> allowedFields,
                                        RuntimeJoinContext joinContext) {
        DynamicCrudRepository.SqlCondition dataScopeCondition = buildDataScopeCondition(config, config.getTableName(), null);
        Map<String, Object> authorizedMainRecord = repository.selectById(config.getTableName(), id, dataScopeCondition);
        if (authorizedMainRecord == null) {
            throw new BusinessException("无权限更新该数据或数据不存在");
        }
        Map<String, Object> mainPayload = extractMainPayload(data);
        Map<String, Object> primaryData = filterPrimaryWriteData(mainPayload, allowedFields, joinContext);
        removeMaskedDesensitizedWriteColumns(primaryData, config, config.getTableName());
        if (!primaryData.isEmpty()) {
            applyEncrypt(primaryData, config.getEncryptConfig());
            int affected = repository.updateById(config.getTableName(), id, primaryData, dataScopeCondition);
            if (affected <= 0) {
                throw new BusinessException("无权限更新该数据或数据不存在");
            }
        }

        Map<String, Object> childrenPayload = extractChildrenPayload(data);
        Map<String, Object> currentMainRecord = authorizedMainRecord;
        boolean childrenChanged = false;
        for (RuntimeChildRelation relation : joinContext.childRelations()) {
            if (!childrenPayload.containsKey(relation.modelCode())) {
                continue;
            }
            if (currentMainRecord == null && !"id".equals(relation.mainColumn()) && !primaryData.containsKey(relation.mainColumn())) {
                currentMainRecord = repository.selectById(config.getTableName(), id);
            }
            Object relationValue = resolveMainRelationValue(relation, primaryData, id, currentMainRecord);
            if (relationValue == null) {
                continue;
            }
            childrenChanged = true;
            repository.deleteByColumn(
                    relation.tableName(),
                    relation.childFkColumn(),
                    relationValue,
                    repository.hasDelFlag(relation.tableName())
            );
            for (Map<String, Object> row : normalizeChildRows(childrenPayload.get(relation.modelCode()))) {
                Map<String, Object> childData = filterChildWriteData(row, relation);
                if (!hasWritableChildData(childData)) {
                    continue;
                }
                childData.put(relation.childFkColumn(), relationValue);
                repository.insert(relation.tableName(), childData);
            }
        }

        if (primaryData.isEmpty() && !childrenChanged) {
            throw new BusinessException("没有可更新的字段");
        }
    }

    private Map<String, Object> filterPrimaryWriteData(Map<String, Object> data,
                                                       Set<String> allowedFields,
                                                       RuntimeJoinContext joinContext) {
        Map<String, Object> primaryData = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            return primaryData;
        }
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (isImmutableWriteField(key) || !allowedFields.contains(key)) {
                continue;
            }
            RuntimeFieldRef fieldRef = joinContext.fields().get(key);
            if (fieldRef == null || !fieldRef.primary()) {
                continue;
            }
            primaryData.put(fieldRef.columnName(), entry.getValue());
        }
        return primaryData;
    }

    private Map<String, Object> filterChildWriteData(Map<String, Object> data, RuntimeChildRelation relation) {
        Map<String, Object> childData = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            return childData;
        }
        for (RuntimeFieldRef fieldRef : relation.fields().values()) {
            if (fieldRef.primary() || isImmutableWriteField(fieldRef.fieldName()) || isImmutableWriteField(fieldRef.sourceField())) {
                continue;
            }
            if (fieldRef.columnName().equals(relation.childFkColumn())) {
                continue;
            }
            Object value = firstPresent(data, fieldRef.sourceField(), fieldRef.fieldName(), fieldRef.columnName());
            if (value != null) {
                childData.put(fieldRef.columnName(), value);
            }
        }
        return childData;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMainPayload(Map<String, Object> data) {
        if (data != null && data.get("main") instanceof Map<?, ?> main) {
            return (Map<String, Object>) main;
        }
        return data == null ? Map.of() : data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractChildrenPayload(Map<String, Object> data) {
        if (data != null && data.get("children") instanceof Map<?, ?> children) {
            return (Map<String, Object>) children;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeChildRows(Object value) {
        if (value instanceof List<?> rows) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object row : rows) {
                if (row instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        }
        if (value instanceof Map<?, ?> map) {
            return List.of((Map<String, Object>) map);
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Object resolvePayloadId(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Object idValue = data.get("id");
        if (idValue != null) {
            return idValue;
        }
        Object main = data.get("main");
        if (main instanceof Map<?, ?> map) {
            return ((Map<String, Object>) map).get("id");
        }
        return null;
    }

    private Object firstPresent(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            if (StringUtils.isNotBlank(key) && data.containsKey(key)) {
                return data.get(key);
            }
        }
        return null;
    }

    private void insertJoinedData(AiCrudConfig config,
                                  Map<String, Object> data,
                                  Set<String> allowedFields,
                                  RuntimeJoinContext joinContext) {
        Map<String, Object> primaryData = new LinkedHashMap<>();
        Map<String, Map<String, Object>> childDataMap = new LinkedHashMap<>();
        splitRuntimeWriteData(data, allowedFields, joinContext, primaryData, childDataMap);
        if (primaryData.isEmpty()) {
            throw new BusinessException("没有可写入的主表字段");
        }
        applyEncrypt(primaryData, config.getEncryptConfig());
        Long mainId = repository.insertReturningId(config.getTableName(), primaryData);
        for (RuntimeChildRelation relation : joinContext.childRelations()) {
            Map<String, Object> childData = childDataMap.get(relation.modelCode());
            if (!hasWritableChildData(childData)) {
                continue;
            }
            Object relationValue = resolveMainRelationValue(relation, primaryData, mainId, null);
            if (relationValue == null) {
                continue;
            }
            childData.put(relation.childFkColumn(), relationValue);
            repository.insert(relation.tableName(), childData);
        }
    }

    private void updateJoinedData(AiCrudConfig config,
                                  Long id,
                                  Map<String, Object> data,
                                  Set<String> allowedFields,
                                  RuntimeJoinContext joinContext) {
        DynamicCrudRepository.SqlCondition dataScopeCondition = buildDataScopeCondition(config, config.getTableName(), null);
        Map<String, Object> authorizedMainRecord = repository.selectById(config.getTableName(), id, dataScopeCondition);
        if (authorizedMainRecord == null) {
            throw new BusinessException("无权限更新该数据或数据不存在");
        }
        Map<String, Object> primaryData = new LinkedHashMap<>();
        Map<String, Map<String, Object>> childDataMap = new LinkedHashMap<>();
        splitRuntimeWriteData(data, allowedFields, joinContext, primaryData, childDataMap);
        removeMaskedDesensitizedWriteColumns(primaryData, config, config.getTableName());

        if (!primaryData.isEmpty()) {
            applyEncrypt(primaryData, config.getEncryptConfig());
            int affected = repository.updateById(config.getTableName(), id, primaryData, dataScopeCondition);
            if (affected <= 0) {
                throw new BusinessException("无权限更新该数据或数据不存在");
            }
        }

        Map<String, Object> currentMainRecord = authorizedMainRecord;
        for (RuntimeChildRelation relation : joinContext.childRelations()) {
            Map<String, Object> childData = childDataMap.get(relation.modelCode());
            if (!hasWritableChildData(childData)) {
                continue;
            }
            if (currentMainRecord == null && !"id".equals(relation.mainColumn()) && !primaryData.containsKey(relation.mainColumn())) {
                currentMainRecord = repository.selectById(config.getTableName(), id);
            }
            Object relationValue = resolveMainRelationValue(relation, primaryData, id, currentMainRecord);
            if (relationValue == null) {
                continue;
            }
            childData.put(relation.childFkColumn(), relationValue);
            Long childId = repository.selectFirstIdByColumn(relation.tableName(), relation.childFkColumn(), relationValue);
            if (childId == null) {
                repository.insert(relation.tableName(), childData);
            } else {
                repository.updateById(relation.tableName(), childId, childData);
            }
        }

        if (primaryData.isEmpty() && childDataMap.values().stream().noneMatch(this::hasWritableChildData)) {
            throw new BusinessException("没有可更新的字段");
        }
    }

    private void splitRuntimeWriteData(Map<String, Object> data,
                                       Set<String> allowedFields,
                                       RuntimeJoinContext joinContext,
                                       Map<String, Object> primaryData,
                                       Map<String, Map<String, Object>> childDataMap) {
        if (data == null || data.isEmpty()) {
            return;
        }
        Map<String, RuntimeChildRelation> relationMap = joinContext.childRelations().stream()
                .collect(Collectors.toMap(RuntimeChildRelation::modelCode, relation -> relation, (left, right) -> left, LinkedHashMap::new));
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (isImmutableWriteField(key) || !allowedFields.contains(key)) {
                continue;
            }
            RuntimeFieldRef fieldRef = joinContext.fields().get(key);
            if (fieldRef == null) {
                continue;
            }
            if (fieldRef.primary()) {
                primaryData.put(fieldRef.columnName(), entry.getValue());
                continue;
            }
            RuntimeChildRelation relation = relationMap.get(fieldRef.modelCode());
            if (relation == null) {
                continue;
            }
            childDataMap.computeIfAbsent(fieldRef.modelCode(), ignored -> new LinkedHashMap<>())
                    .put(fieldRef.columnName(), entry.getValue());
        }
    }

    private boolean hasWritableChildData(Map<String, Object> childData) {
        if (childData == null || childData.isEmpty()) {
            return false;
        }
        return childData.values().stream().anyMatch(value -> value != null && !(value instanceof String text && StringUtils.isBlank(text)));
    }

    private Object resolveMainRelationValue(RuntimeChildRelation relation,
                                            Map<String, Object> primaryData,
                                            Long mainId,
                                            Map<String, Object> currentMainRecord) {
        if ("id".equals(relation.mainColumn())) {
            return mainId;
        }
        if (primaryData.containsKey(relation.mainColumn())) {
            return primaryData.get(relation.mainColumn());
        }
        if (currentMainRecord != null) {
            return currentMainRecord.get(relation.mainColumn());
        }
        return null;
    }

    private boolean isImmutableWriteField(String key) {
        return IMMUTABLE_WRITE_FIELDS.contains(key);
    }

    // ==================== 删除操作 ====================

    /**
     * 删除
     */
    public void deleteById(String configKey, Long id) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        
        // 判断是否逻辑删除
        boolean logicDelete = repository.hasDelFlag(tableName);
        
        // 执行删除
        int affected = repository.deleteById(tableName, id, logicDelete, buildDataScopeCondition(config, tableName, null));
        if (affected <= 0) {
            throw new BusinessException("无权限删除该数据或数据不存在");
        }
    }

    /**
     * 暴露运行时配置给动态导入导出服务，仍统一走发布态校验。
     */
    public AiCrudConfig getRuntimeConfig(String configKey) {
        return getConfig(configKey);
    }

    private RuntimeJoinContext buildRuntimeJoinContext(AiCrudConfig config) {
        if (!"LOWCODE".equals(config.getBuildMode())
                || StringUtils.isBlank(config.getPageSchema())
                || StringUtils.isBlank(config.getModelSchema())) {
            return null;
        }
        LowcodePageSchema pageSchema = readPageSchema(config);
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().size() <= 1) {
            return null;
        }
        LowcodeModelSchema modelSchema = readModelSchema(config);
        if (modelSchema == null) {
            return null;
        }

        LowcodePageModelRef primaryRef = pageSchema.getModelRefs().stream()
                .filter(ref -> Boolean.TRUE.equals(ref.getPrimary()))
                .findFirst()
                .orElse(pageSchema.getModelRefs().get(0));
        String primaryModelCode = StringUtils.defaultIfBlank(pageSchema.getPrimaryModelCode(), primaryRef.getModelCode());
        if (StringUtils.isBlank(primaryModelCode)) {
            primaryModelCode = modelSchema.getObject() == null ? null : modelSchema.getObject().getCode();
        }
        if (StringUtils.isBlank(primaryModelCode)) {
            return null;
        }

        Map<String, RuntimeFieldRef> fields = new LinkedHashMap<>();
        Map<String, String> fieldColumnMapping = new LinkedHashMap<>();
        List<DynamicCrudRepository.JoinField> selectFields = new ArrayList<>();
        Map<String, String> relationDisplayAliases = new LinkedHashMap<>();

        addPrimaryRuntimeFields(config.getTableName(), primaryModelCode, fields, fieldColumnMapping, selectFields);

        List<RuntimeChildRelation> childRelations = new ArrayList<>();
        List<DynamicCrudRepository.JoinSpec> joins = new ArrayList<>();
        List<LowcodeRelationSchema> primaryRelations = mergeRelations(modelSchema.getRelations(), primaryRef.getRelations());
        Map<String, String> primaryColumnMapping = repository.getColumnMapping(config.getTableName());
        int aliasIndex = 1;
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getModelCode())) {
                continue;
            }
            if (StringUtils.isBlank(ref.getTableName()) || !repository.tableExists(ref.getTableName())) {
                log.warn("[DynamicCrudService] 引用模型缺少可用表名，跳过左连接, configKey={}, modelCode={}",
                        config.getConfigKey(), ref.getModelCode());
                continue;
            }
            RuntimeChildRelation relation = buildChildRelation(primaryModelCode, ref, primaryRelations,
                    "t" + aliasIndex, primaryColumnMapping, fields, fieldColumnMapping, selectFields);
            if (relation == null) {
                log.warn("[DynamicCrudService] 未找到引用模型关联关系，跳过左连接, configKey={}, modelCode={}",
                        config.getConfigKey(), ref.getModelCode());
                continue;
            }
            childRelations.add(relation);
            joins.add(new DynamicCrudRepository.JoinSpec(
                    relation.tableName(), relation.tableAlias(), relation.childFkColumn(), relation.mainColumn()));
            addRelationDisplayField(modelSchema, primaryRelations, ref, relation,
                    fields, fieldColumnMapping, selectFields, relationDisplayAliases);
            aliasIndex++;
        }

        if (childRelations.isEmpty()) {
            return null;
        }
        return new RuntimeJoinContext(fields, childRelations, selectFields, joins, fieldColumnMapping, relationDisplayAliases);
    }

    private boolean isMasterDetailRuntime(AiCrudConfig config) {
        return config != null && MASTER_DETAIL_LAYOUT.equals(config.getLayoutType());
    }

    private boolean isTreeRuntime(AiCrudConfig config) {
        if (config == null) {
            return false;
        }
        if (TREE_LAYOUT.equals(config.getLayoutType())) {
            return true;
        }
        if (StringUtils.isBlank(config.getOptions())) {
            return false;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(config.getOptions()).get("treeConfig");
            return treeNode != null && treeNode.isObject();
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 判断树形运行时失败, configKey={}", config.getConfigKey(), e);
            return false;
        }
    }

    private void addPrimaryRuntimeFields(String tableName,
                                         String modelCode,
                                         Map<String, RuntimeFieldRef> fields,
                                         Map<String, String> fieldColumnMapping,
                                         List<DynamicCrudRepository.JoinField> selectFields) {
        List<String> columns = new ArrayList<>(repository.getTableColumns(tableName));
        columns.sort(Comparator.naturalOrder());
        for (String column : columns) {
            String fieldName = DynamicQueryGenerator.snakeToCamel(column);
            RuntimeFieldRef fieldRef = new RuntimeFieldRef(fieldName, modelCode, fieldName, column, tableName, "t0", true);
            fields.putIfAbsent(fieldName, fieldRef);
            fieldColumnMapping.put(fieldName, "t0." + column);
            selectFields.add(new DynamicCrudRepository.JoinField(fieldName, "t0", column));
        }
    }

    private RuntimeChildRelation buildChildRelation(String primaryModelCode,
                                                    LowcodePageModelRef ref,
                                                    List<LowcodeRelationSchema> primaryRelations,
                                                    String tableAlias,
                                                    Map<String, String> primaryColumnMapping,
                                                    Map<String, RuntimeFieldRef> fields,
                                                    Map<String, String> fieldColumnMapping,
                                                    List<DynamicCrudRepository.JoinField> selectFields) {
        Map<String, String> childColumnMapping = repository.getColumnMapping(ref.getTableName());
        LowcodeRelationSchema relation = findRelationToPrimary(ref.getRelations(), primaryModelCode);
        boolean relationFromPrimary = false;
        if (relation == null) {
            LowcodeRelationSchema primaryRelation = findRelationFromPrimary(primaryRelations, ref.getModelCode());
            LowcodeRelationSchema inferredRelation = inferRelation(primaryModelCode, ref);
            if (shouldPreferInferredChildRelation(primaryRelation, inferredRelation, childColumnMapping)) {
                relation = inferredRelation;
            } else {
                relation = primaryRelation != null ? primaryRelation : inferredRelation;
                relationFromPrimary = primaryRelation != null && relation == primaryRelation;
            }
        }
        if (relation == null) {
            return null;
        }

        String mainField = relationFromPrimary ? relation.getSourceField() : relation.getTargetField();
        String childField = relationFromPrimary ? relation.getTargetField() : relation.getSourceField();
        String mainColumn = resolvePrimaryColumn(mainField, primaryColumnMapping);
        String childColumn = resolveChildColumn(childField, childColumnMapping);
        if (StringUtils.isBlank(mainColumn) || StringUtils.isBlank(childColumn)) {
            return null;
        }

        Map<String, RuntimeFieldRef> childFields = new LinkedHashMap<>();
        for (Map<String, Object> source : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(source.get("sourceField")), text(source.get("field")));
            if (StringUtils.isBlank(sourceField)) {
                continue;
            }
            String fieldName = StringUtils.defaultIfBlank(text(source.get("fieldRef")), safeKey(ref.getModelCode()) + "__" + sourceField);
            String columnName = resolveChildColumn(StringUtils.defaultIfBlank(text(source.get("columnName")), sourceField), childColumnMapping);
            if (StringUtils.isBlank(columnName)) {
                continue;
            }
            RuntimeFieldRef fieldRef = new RuntimeFieldRef(
                    fieldName, ref.getModelCode(), sourceField, columnName, ref.getTableName(), tableAlias, false);
            fields.putIfAbsent(fieldName, fieldRef);
            childFields.put(fieldName, fieldRef);
            fieldColumnMapping.put(fieldName, tableAlias + "." + columnName);
            selectFields.add(new DynamicCrudRepository.JoinField(fieldName, tableAlias, columnName));
        }
        if (childFields.isEmpty()) {
            return null;
        }
        return new RuntimeChildRelation(ref.getModelCode(), ref.getTableName(), tableAlias, childColumn, mainColumn, childFields);
    }

    private boolean shouldPreferInferredChildRelation(LowcodeRelationSchema primaryRelation,
                                                      LowcodeRelationSchema inferredRelation,
                                                      Map<String, String> childColumnMapping) {
        if (primaryRelation == null || inferredRelation == null) {
            return false;
        }
        String configuredChildColumn = resolveChildColumn(primaryRelation.getTargetField(), childColumnMapping);
        String inferredChildColumn = resolveChildColumn(inferredRelation.getSourceField(), childColumnMapping);
        return "id".equals(configuredChildColumn) && StringUtils.isNotBlank(inferredChildColumn)
                && !"id".equals(inferredChildColumn);
    }

    private String resolvePrimaryColumn(String fieldName, Map<String, String> primaryColumnMapping) {
        if (StringUtils.isBlank(fieldName)) {
            return null;
        }
        String column = primaryColumnMapping.getOrDefault(fieldName, DynamicQueryGenerator.camelToSnake(fieldName));
        return primaryColumnMapping.containsValue(column) ? column : null;
    }

    private String resolveChildColumn(String fieldName, Map<String, String> childColumnMapping) {
        if (StringUtils.isBlank(fieldName)) {
            return null;
        }
        String column = childColumnMapping.getOrDefault(fieldName, DynamicQueryGenerator.camelToSnake(fieldName));
        return childColumnMapping.containsValue(column) ? column : null;
    }

    private LowcodeRelationSchema findRelationFromPrimary(List<LowcodeRelationSchema> relations, String targetModelCode) {
        if (relations == null) {
            return null;
        }
        return relations.stream()
                .filter(relation -> relation != null && targetModelCode.equals(relation.getTargetObjectCode()))
                .findFirst()
                .orElse(null);
    }

    private LowcodeRelationSchema findRelationToPrimary(List<LowcodeRelationSchema> relations, String primaryModelCode) {
        if (relations == null) {
            return null;
        }
        return relations.stream()
                .filter(relation -> relation != null && primaryModelCode.equals(relation.getTargetObjectCode()))
                .findFirst()
                .orElse(null);
    }

    private void addRelationDisplayField(LowcodeModelSchema modelSchema,
                                         List<LowcodeRelationSchema> primaryRelations,
                                         LowcodePageModelRef ref,
                                         RuntimeChildRelation relation,
                                         Map<String, RuntimeFieldRef> fields,
                                         Map<String, String> fieldColumnMapping,
                                         List<DynamicCrudRepository.JoinField> selectFields,
                                         Map<String, String> relationDisplayAliases) {
        if (ref == null || relation == null) {
            return;
        }
        LowcodeRelationSchema primaryRelation = findRelationFromPrimary(primaryRelations, ref.getModelCode());
        if (primaryRelation == null) {
            return;
        }
        String sourceField = normalizeModelFieldName(modelSchema, primaryRelation.getSourceField());
        if (StringUtils.isBlank(sourceField)) {
            return;
        }
        String displayField = resolveRelationDisplayField(ref, primaryRelation);
        if (StringUtils.isBlank(displayField)) {
            return;
        }
        String aliasField = sourceField + "Name";
        if (fields.containsKey(aliasField)) {
            relationDisplayAliases.putIfAbsent(sourceField, aliasField);
            return;
        }
        Map<String, String> childColumnMapping = repository.getColumnMapping(ref.getTableName());
        String displayColumn = resolveChildColumn(displayField, childColumnMapping);
        if (StringUtils.isBlank(displayColumn)) {
            return;
        }
        RuntimeFieldRef displayRef = new RuntimeFieldRef(
                aliasField,
                ref.getModelCode(),
                displayField,
                displayColumn,
                ref.getTableName(),
                relation.tableAlias(),
                false
        );
        fields.put(aliasField, displayRef);
        fieldColumnMapping.put(aliasField, relation.tableAlias() + "." + displayColumn);
        selectFields.add(new DynamicCrudRepository.JoinField(aliasField, relation.tableAlias(), displayColumn));
        relationDisplayAliases.put(sourceField, aliasField);
    }

    private LowcodeRelationSchema inferRelation(String primaryModelCode, LowcodePageModelRef ref) {
        String expectedCamel = DynamicQueryGenerator.snakeToCamel(primaryModelCode) + "Id";
        String expectedSnake = DynamicQueryGenerator.camelToSnake(primaryModelCode) + "_id";
        for (Map<String, Object> field : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String columnName = StringUtils.defaultIfBlank(text(field.get("columnName")), sourceField);
            if (expectedCamel.equals(sourceField) || expectedSnake.equals(columnName)) {
                LowcodeRelationSchema relation = new LowcodeRelationSchema();
                relation.setRelationType("ONE_TO_MANY");
                relation.setSourceField(sourceField);
                relation.setTargetObjectCode(primaryModelCode);
                relation.setTargetField("id");
                return relation;
            }
        }
        return null;
    }

    private List<LowcodeRelationSchema> mergeRelations(List<LowcodeRelationSchema> first, List<LowcodeRelationSchema> second) {
        List<LowcodeRelationSchema> result = new ArrayList<>();
        if (first != null) {
            result.addAll(first);
        }
        if (second != null) {
            result.addAll(second);
        }
        return result;
    }

    private String buildJoinOrderBy(String orderByColumn, String isAsc, RuntimeJoinContext joinContext) {
        if (StringUtils.isBlank(orderByColumn)) {
            return "t0.id DESC";
        }
        List<String> columns = Arrays.stream(orderByColumn.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(column -> joinContext.fieldColumnMapping().get(column))
                .filter(StringUtils::isNotBlank)
                .toList();
        if (columns.isEmpty()) {
            return "t0.id DESC";
        }
        String direction = "asc".equalsIgnoreCase(isAsc) ? "ASC" : "DESC";
        return String.join(", ", columns) + " " + direction;
    }

    private boolean requiresJoinedPageQuery(AiCrudConfig config,
                                            PageQuery pageQuery,
                                            Map<String, Object> searchParams,
                                            RuntimeJoinContext joinContext) {
        return containsChildField(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), joinContext)
                || containsRelationDisplayField(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), joinContext)
                || containsActiveChildSearchField(searchParams, joinContext)
                || containsChildField(splitFields(pageQuery.getOrderByColumn()), joinContext);
    }

    private boolean requiresJoinedExportQuery(AiCrudConfig config,
                                              Map<String, Object> searchParams,
                                              RuntimeJoinContext joinContext) {
        return containsChildField(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), joinContext)
                || containsRelationDisplayField(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), joinContext)
                || containsActiveChildSearchField(searchParams, joinContext);
    }

    private boolean requiresJoinedCustomQuery(CustomQueryExecuteDTO request, RuntimeJoinContext joinContext) {
        if (request == null) {
            return false;
        }
        if (containsChildField(request.getFields(), joinContext)) {
            return true;
        }
        if (containsRelationDisplayField(request.getFields(), joinContext)) {
            return true;
        }
        if (containsChildField(splitFields(request.getOrderByColumn()), joinContext)) {
            return true;
        }
        if (request.getConditions() == null) {
            return false;
        }
        for (var condition : request.getConditions()) {
            if (condition != null && isChildRuntimeField(condition.getField(), joinContext)) {
                return true;
            }
        }
        return false;
    }

    private List<DynamicCrudRepository.JoinField> buildRuntimeSelectFields(RuntimeJoinContext joinContext,
                                                                           Collection<String> requestedFields,
                                                                           boolean defaultPrimaryFields) {
        LinkedHashSet<String> fieldNames = new LinkedHashSet<>();
        fieldNames.add("id");
        if (requestedFields != null) {
            requestedFields.stream()
                    .filter(StringUtils::isNotBlank)
                    .forEach(fieldNames::add);
        }
        if (fieldNames.size() == 1 && defaultPrimaryFields) {
            joinContext.fields().values().stream()
                    .filter(RuntimeFieldRef::primary)
                    .map(RuntimeFieldRef::fieldName)
                    .forEach(fieldNames::add);
        }
        List<String> relationAliases = fieldNames.stream()
                .map(fieldName -> joinContext.relationDisplayAliases().get(fieldName))
                .filter(StringUtils::isNotBlank)
                .toList();
        fieldNames.addAll(relationAliases);

        List<DynamicCrudRepository.JoinField> selectFields = new ArrayList<>();
        for (String fieldName : fieldNames) {
            RuntimeFieldRef fieldRef = joinContext.fields().get(fieldName);
            if (fieldRef == null) {
                continue;
            }
            selectFields.add(new DynamicCrudRepository.JoinField(
                    fieldRef.fieldName(), fieldRef.tableAlias(), fieldRef.columnName()));
        }
        if (selectFields.isEmpty()) {
            joinContext.fields().values().stream()
                    .filter(RuntimeFieldRef::primary)
                    .map(fieldRef -> new DynamicCrudRepository.JoinField(
                            fieldRef.fieldName(), fieldRef.tableAlias(), fieldRef.columnName()))
                    .forEach(selectFields::add);
        }
        return selectFields;
    }

    private boolean containsChildField(Collection<String> fieldNames, RuntimeJoinContext joinContext) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            return false;
        }
        for (String fieldName : fieldNames) {
            if (isChildRuntimeField(fieldName, joinContext)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsRelationDisplayField(Collection<String> fieldNames, RuntimeJoinContext joinContext) {
        if (fieldNames == null || fieldNames.isEmpty() || joinContext == null || joinContext.relationDisplayAliases().isEmpty()) {
            return false;
        }
        for (String fieldName : fieldNames) {
            if (joinContext.relationDisplayAliases().containsKey(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsActiveChildSearchField(Map<String, Object> searchParams, RuntimeJoinContext joinContext) {
        if (searchParams == null || searchParams.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, Object> entry : searchParams.entrySet()) {
            if (hasQueryValue(entry.getValue()) && isChildRuntimeField(entry.getKey(), joinContext)) {
                return true;
            }
        }
        return false;
    }

    private boolean isChildRuntimeField(String fieldName, RuntimeJoinContext joinContext) {
        if (StringUtils.isBlank(fieldName) || joinContext == null) {
            return false;
        }
        RuntimeFieldRef fieldRef = joinContext.fields().get(fieldName);
        return fieldRef != null && !fieldRef.primary();
    }

    private List<String> splitFields(String fields) {
        if (StringUtils.isBlank(fields)) {
            return List.of();
        }
        return Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    private boolean hasQueryValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return StringUtils.isNotBlank(text);
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        return true;
    }

    private LowcodePageSchema readPageSchema(AiCrudConfig config) {
        try {
            return objectMapper.readValue(config.getPageSchema(), LowcodePageSchema.class);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析pageSchema失败, configKey={}", config.getConfigKey(), e);
            return null;
        }
    }

    private LowcodeModelSchema readModelSchema(AiCrudConfig config) {
        try {
            return objectMapper.readValue(config.getModelSchema(), LowcodeModelSchema.class);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析modelSchema失败, configKey={}", config.getConfigKey(), e);
            return null;
        }
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String safeKey(String value) {
        String key = StringUtils.defaultIfBlank(value, "model").replaceAll("[^A-Za-z0-9_]", "_");
        return StringUtils.defaultIfBlank(key, "model");
    }

    private void applyReadPipeline(List<Map<String, Object>> rows, AiCrudConfig config) {
        applyDecrypt(rows, config.getEncryptConfig());
        applyDictTranslation(rows, buildEffectiveTransConfig(config));
        applyDesensitize(rows, config.getDesensitizeConfig());
    }

    private LowcodeTreeConfig resolveTreeConfig(AiCrudConfig config) {
        LowcodeTreeConfig treeConfig = new LowcodeTreeConfig();
        applyTreeConfigFromModel(config, treeConfig);
        applyTreeConfigFromOptions(config, treeConfig);
        if (StringUtils.isBlank(treeConfig.getKeyField())) {
            treeConfig.setKeyField("id");
        }
        if (StringUtils.isBlank(treeConfig.getParentField())) {
            treeConfig.setParentField("parentId");
        }
        if (StringUtils.isBlank(treeConfig.getLabelField())) {
            treeConfig.setLabelField("name");
        }
        if (StringUtils.isBlank(treeConfig.getFilterField())) {
            treeConfig.setFilterField(treeConfig.getParentField());
        }
        if (StringUtils.isBlank(treeConfig.getTargetField())) {
            treeConfig.setTargetField(treeConfig.getKeyField());
        }
        if (StringUtils.isBlank(treeConfig.getChildrenField())) {
            treeConfig.setChildrenField("children");
        }
        if (StringUtils.isBlank(treeConfig.getLoadMode())) {
            treeConfig.setLoadMode("full");
        }
        return treeConfig;
    }

    private void applyTreeConfigFromModel(AiCrudConfig config, LowcodeTreeConfig target) {
        if (StringUtils.isBlank(config.getModelSchema())) {
            return;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(config.getModelSchema()).get("treeConfig");
            applyTreeConfigNode(treeNode, target);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析modelSchema.treeConfig失败, configKey={}", config.getConfigKey(), e);
        }
    }

    private void applyTreeConfigFromOptions(AiCrudConfig config, LowcodeTreeConfig target) {
        if (StringUtils.isBlank(config.getOptions())) {
            return;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(config.getOptions()).get("treeConfig");
            applyTreeConfigNode(treeNode, target);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析options.treeConfig失败, configKey={}", config.getConfigKey(), e);
        }
    }

    private void applyTreeConfigNode(JsonNode treeNode, LowcodeTreeConfig target) {
        if (treeNode == null || !treeNode.isObject()) {
            return;
        }
        if (StringUtils.isNotBlank(text(treeNode, "keyField"))) {
            target.setKeyField(text(treeNode, "keyField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "sourceModelCode"))) {
            target.setSourceModelCode(text(treeNode, "sourceModelCode"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "sourceModelName"))) {
            target.setSourceModelName(text(treeNode, "sourceModelName"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "sourceTableName"))) {
            target.setSourceTableName(text(treeNode, "sourceTableName"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "parentField"))) {
            target.setParentField(text(treeNode, "parentField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "labelField"))) {
            target.setLabelField(text(treeNode, "labelField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "filterField"))) {
            target.setFilterField(text(treeNode, "filterField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "targetField"))) {
            target.setTargetField(text(treeNode, "targetField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "childrenField"))) {
            target.setChildrenField(text(treeNode, "childrenField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "treeTitle"))) {
            target.setTreeTitle(text(treeNode, "treeTitle"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "loadMode"))) {
            target.setLoadMode(text(treeNode, "loadMode"));
        } else if (treeNode.has("lazy") && treeNode.get("lazy").asBoolean(false)) {
            target.setLoadMode("lazy");
        }
        if (treeNode.has("enabled") && !treeNode.get("enabled").isNull()) {
            target.setEnabled(treeNode.get("enabled").asBoolean(false));
        }
    }

    private List<Map<String, Object>> appendTreeAncestorRows(List<Map<String, Object>> rows,
                                                             String tableName,
                                                             String keyColumn,
                                                             String parentColumn,
                                                             Set<String> ancestorKeys) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
        List<Map<String, Object>> result = new ArrayList<>(rows);
        Set<String> knownKeys = result.stream()
                .map(row -> normalizeTreeKey(row.get(keyColumn)))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Deque<Object> parentQueue = new ArrayDeque<>();
        for (Map<String, Object> row : result) {
            Object parentValue = row.get(parentColumn);
            String parentKey = normalizeTreeKey(parentValue);
            if (StringUtils.isNotBlank(parentKey) && !isRootParent(parentKey) && !knownKeys.contains(parentKey)) {
                parentQueue.add(parentValue);
            }
        }

        int guard = 0;
        while (!parentQueue.isEmpty() && guard < MAX_TREE_ROWS) {
            guard++;
            Object parentValue = parentQueue.removeFirst();
            String parentKey = normalizeTreeKey(parentValue);
            if (StringUtils.isBlank(parentKey) || isRootParent(parentKey) || knownKeys.contains(parentKey)) {
                continue;
            }
            List<Map<String, Object>> parents = repository.selectListByColumn(tableName, keyColumn, parentValue);
            if (parents.isEmpty()) {
                continue;
            }
            Map<String, Object> parent = parents.get(0);
            String key = normalizeTreeKey(parent.get(keyColumn));
            if (StringUtils.isBlank(key) || !knownKeys.add(key)) {
                continue;
            }
            ancestorKeys.add(key);
            result.add(parent);
            String nextParentKey = normalizeTreeKey(parent.get(parentColumn));
            if (StringUtils.isNotBlank(nextParentKey) && !isRootParent(nextParentKey) && !knownKeys.contains(nextParentKey)) {
                parentQueue.add(parent.get(parentColumn));
            }
        }
        return result;
    }

    private void markTreeAncestorRows(List<Map<String, Object>> rows,
                                      LowcodeTreeConfig treeConfig,
                                      Set<String> ancestorKeys) {
        if (rows == null || rows.isEmpty() || ancestorKeys == null || ancestorKeys.isEmpty()) {
            return;
        }
        String keyField = treeConfig.getKeyField();
        for (Map<String, Object> row : rows) {
            String key = normalizeTreeKey(row.get(keyField));
            boolean ancestor = ancestorKeys.contains(key);
            row.put("_scopeAncestor", ancestor);
            row.put("_dataScopeWritable", !ancestor);
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private List<Map<String, Object>> buildLazyTreeNodes(List<Map<String, Object>> rows,
                                                         String tableName,
                                                         String parentColumn,
                                                         LowcodeTreeConfig treeConfig,
                                                         DynamicCrudRepository.SqlCondition dataScopeCondition) {
        String keyField = treeConfig.getKeyField();
        String labelField = treeConfig.getLabelField();
        String targetField = treeConfig.getTargetField();
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String key = normalizeTreeKey(row.get(keyField));
            if (StringUtils.isBlank(key)) {
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>(row);
            node.putIfAbsent("key", row.get(keyField));
            node.putIfAbsent("label", resolveTreeLabel(row, labelField, keyField));
            node.putIfAbsent("targetValue", row.get(targetField));
            boolean hasChildren = repository.existsByColumn(tableName, parentColumn, row.get(keyField), dataScopeCondition);
            if (hasChildren) {
                node.put("isLeaf", false);
            } else {
                node.put("isLeaf", true);
            }
            nodes.add(node);
        }
        return nodes;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildTree(List<Map<String, Object>> rows, LowcodeTreeConfig treeConfig) {
        Map<String, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        String keyField = treeConfig.getKeyField();
        String parentField = treeConfig.getParentField();
        String labelField = treeConfig.getLabelField();
        String targetField = treeConfig.getTargetField();
        String childrenField = treeConfig.getChildrenField();

        for (Map<String, Object> row : rows) {
            String key = normalizeTreeKey(row.get(keyField));
            if (StringUtils.isBlank(key)) {
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>(row);
            node.putIfAbsent("key", row.get(keyField));
            node.putIfAbsent("label", resolveTreeLabel(row, labelField, keyField));
            node.putIfAbsent("targetValue", row.get(targetField));
            node.put(childrenField, new ArrayList<Map<String, Object>>());
            nodeMap.put(key, node);
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> node : nodeMap.values()) {
            String key = normalizeTreeKey(node.get(keyField));
            String parentKey = normalizeTreeKey(node.get(parentField));
            Map<String, Object> parent = nodeMap.get(parentKey);
            if (isRootParent(parentKey) || parent == null || key.equals(parentKey)) {
                roots.add(node);
                continue;
            }
            Object children = parent.get(childrenField);
            if (children instanceof List<?> childList) {
                ((List<Map<String, Object>>) childList).add(node);
            }
        }
        normalizeFullTreeLeafState(roots, childrenField);
        return roots;
    }

    @SuppressWarnings("unchecked")
    private void normalizeFullTreeLeafState(List<Map<String, Object>> nodes, String childrenField) {
        for (Map<String, Object> node : nodes) {
            Object children = node.get(childrenField);
            if (children instanceof List<?> childList && !childList.isEmpty()) {
                node.put("isLeaf", false);
                normalizeFullTreeLeafState((List<Map<String, Object>>) childList, childrenField);
                continue;
            }
            node.put("isLeaf", true);
            node.remove(childrenField);
        }
    }

    private Object resolveTreeLabel(Map<String, Object> row, String labelField, String keyField) {
        List<String> candidates = Arrays.asList(labelField, "label", "name", "title", "treeName", "deptName", "orgName", keyField);
        for (String candidate : candidates) {
            if (StringUtils.isBlank(candidate)) {
                continue;
            }
            Object value = row.get(candidate);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return value;
            }
        }
        return "未命名节点";
    }

    private boolean isRootParent(String parentKey) {
        return StringUtils.isBlank(parentKey) || "0".equals(parentKey) || "null".equalsIgnoreCase(parentKey);
    }

    private String normalizeTreeKey(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.isBlank(text) ? null : text;
    }

    // ==================== 加解密处理 ====================

    /**
     * 应用加密（写入时）
     * encryptConfig格式示例：
     * {
     *   "phone": {"algorithm": "SM4"},
     *   "idCard": {"algorithm": "AES"},
     *   "email": {"algorithm": "SM4"}
     * }
     */
    private void applyEncrypt(Map<String, Object> data, String encryptConfigJson) {
        if (StringUtils.isBlank(encryptConfigJson) || data == null || data.isEmpty()) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(encryptConfigJson);
            if (!configNode.isObject()) return;

            for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                String fieldName = entry.getKey(); // camelCase字段名
                JsonNode ruleNode = entry.getValue();

                // 转换为snake_case（数据库列名）
                String snakeFieldName = DynamicQueryGenerator.camelToSnake(fieldName);

                if (!data.containsKey(snakeFieldName) || data.get(snakeFieldName) == null) {
                    continue;
                }

                // 获取加密算法配置
                String algorithm = ruleNode.has("algorithm") ? ruleNode.get("algorithm").asText() : "";
                if (StringUtils.isBlank(algorithm)) {
                    continue;
                }

                // 获取加密器
                Encryptor encryptor = encryptorFactory.getEncryptor(algorithm);
                if (encryptor == null) {
                    log.warn("[DynamicCrudService] 未找到加密器, algorithm={}", algorithm);
                    continue;
                }

                // 加密字段值
                Object value = data.get(snakeFieldName);
                if (value instanceof String) {
                    String plainText = (String) value;
                    if (StringUtils.isNotBlank(plainText)) {
                        String encryptedValue = encryptor.encrypt(plainText);
                        data.put(snakeFieldName, encryptedValue);
                        log.debug("[DynamicCrudService] 加密字段: {}, algorithm: {}", fieldName, algorithm);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 加密处理失败", e);
        }
    }

    /**
     * 应用解密（读取时）
     * encryptConfig格式示例：
     * {
     *   "phone": {"algorithm": "SM4"},
     *   "idCard": {"algorithm": "AES"},
     *   "email": {"algorithm": "SM4"}
     * }
     */
    private void applyDecrypt(List<Map<String, Object>> rows, String encryptConfigJson) {
        if (StringUtils.isBlank(encryptConfigJson) || rows == null || rows.isEmpty()) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(encryptConfigJson);
            if (!configNode.isObject()) return;

            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                    String fieldName = entry.getKey(); // camelCase字段名
                    JsonNode ruleNode = entry.getValue();

                    if (!row.containsKey(fieldName) || row.get(fieldName) == null) {
                        continue;
                    }

                    // 获取加密算法配置
                    String algorithm = ruleNode.has("algorithm") ? ruleNode.get("algorithm").asText() : "";
                    if (StringUtils.isBlank(algorithm)) {
                        continue;
                    }

                    // 获取加密器
                    Encryptor encryptor = encryptorFactory.getEncryptor(algorithm);
                    if (encryptor == null) {
                        log.warn("[DynamicCrudService] 未找到加密器, algorithm={}", algorithm);
                        continue;
                    }

                    // 解密字段值
                    Object value = row.get(fieldName);
                    if (value instanceof String) {
                        String cipherText = (String) value;
                        if (StringUtils.isNotBlank(cipherText)) {
                            try {
                                String decryptedValue = encryptor.decrypt(cipherText);
                                row.put(fieldName, decryptedValue);
                                log.debug("[DynamicCrudService] 解密字段: {}, algorithm: {}", fieldName, algorithm);
                            } catch (Exception decryptException) {
                                log.warn("[DynamicCrudService] 解密字段失败: {}, 可能是明文数据", fieldName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解密处理失败", e);
        }
    }

    // ==================== 脱敏处理 ====================

    /**
     * 应用字段脱敏
     */
    private void applyDesensitize(List<Map<String, Object>> rows, String desensitizeConfigJson) {
        if (StringUtils.isBlank(desensitizeConfigJson) || rows == null || rows.isEmpty()) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(desensitizeConfigJson);
            if (!configNode.isObject()) return;

            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                    String fieldName = entry.getKey(); // camelCase字段名
                    JsonNode ruleNode = entry.getValue();
                    if (!row.containsKey(fieldName) || row.get(fieldName) == null) continue;

                    String typeStr = ruleNode.has("type") ? ruleNode.get("type").asText("CUSTOM") : "CUSTOM";
                    DesensitizeType type = DesensitizeType.valueOf(typeStr);
                    DesensitizeStrategy strategy = desensitizeStrategyFactory.getStrategy(type);
                    if (strategy != null) {
                        String originalValue = String.valueOf(row.get(fieldName));
                        row.put(fieldName, strategy.desensitize(originalValue));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 脱敏处理失败", e);
        }
    }

    private void removeMaskedDesensitizedWriteColumns(Map<String, Object> data, AiCrudConfig config, String tableName) {
        if (data == null || data.isEmpty() || config == null || StringUtils.isBlank(config.getDesensitizeConfig())) {
            return;
        }
        Set<String> sensitiveColumns = resolveDesensitizedColumns(config.getDesensitizeConfig(), tableName);
        if (sensitiveColumns.isEmpty()) {
            return;
        }
        data.entrySet().removeIf(entry -> sensitiveColumns.contains(entry.getKey()) && isMaskedValue(entry.getValue()));
    }

    private Set<String> resolveDesensitizedColumns(String desensitizeConfigJson, String tableName) {
        Set<String> columns = new HashSet<>();
        try {
            JsonNode configNode = objectMapper.readTree(desensitizeConfigJson);
            if (!configNode.isObject()) {
                return columns;
            }
            Map<String, String> columnMapping = repository.getColumnMapping(tableName);
            for (String fieldName : iterableFieldNames(configNode)) {
                String columnName = columnMapping.getOrDefault(fieldName, DynamicQueryGenerator.camelToSnake(fieldName));
                columns.add(columnName);
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析脱敏写入字段失败", e);
        }
        return columns;
    }

    private List<String> iterableFieldNames(JsonNode node) {
        List<String> fields = new ArrayList<>();
        node.fieldNames().forEachRemaining(fields::add);
        return fields;
    }

    private boolean isMaskedValue(Object value) {
        return value instanceof String text && text.contains("*");
    }

    // ==================== 字典翻译 ====================

    private String buildEffectiveTransConfig(AiCrudConfig config) {
        if (config == null) {
            return null;
        }
        Map<String, Object> rules = new LinkedHashMap<>();
        mergeTransConfig(rules, config.getTransConfig());
        mergeTransRulesFromSchema(rules, config.getColumnsSchema(), true);
        mergeTransRulesFromSchema(rules, config.getSearchSchema(), false);
        mergeTransRulesFromSchema(rules, config.getEditSchema(), false);
        if (rules.isEmpty()) {
            return config.getTransConfig();
        }
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 合并翻译配置失败", e);
            return config.getTransConfig();
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeTransConfig(Map<String, Object> rules, String transConfigJson) {
        if (StringUtils.isBlank(transConfigJson)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(transConfigJson);
            if (!node.isObject()) {
                return;
            }
            Map<String, Object> source = objectMapper.convertValue(node, Map.class);
            rules.putAll(source);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析翻译配置失败", e);
        }
    }

    private void mergeTransRulesFromSchema(Map<String, Object> rules, String schemaJson, boolean overrideExisting) {
        if (StringUtils.isBlank(schemaJson)) {
            return;
        }
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            if (!schemaNode.isArray()) {
                return;
            }
            for (JsonNode item : schemaNode) {
                mergeTransRuleFromField(rules, item, overrideExisting);
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 从Schema推导翻译配置失败", e);
        }
    }

    private void mergeTransRuleFromField(Map<String, Object> rules, JsonNode item, boolean overrideExisting) {
        String fieldName = firstText(item, "field", "prop", "key", "dataIndex");
        if (StringUtils.isBlank(fieldName) || (!overrideExisting && rules.containsKey(fieldName))) {
            return;
        }
        String dictType = firstText(item, "dictType");
        JsonNode renderNode = item.get("render");
        if (StringUtils.isBlank(dictType) && renderNode != null && renderNode.isObject()) {
            dictType = firstText(renderNode, "dictType");
        }
        if (StringUtils.isNotBlank(dictType)) {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("dictType", dictType);
            rule.put("targetField", fieldName + "Name");
            rules.put(fieldName, rule);
            return;
        }

        String renderType = renderNode != null && renderNode.isObject() ? firstText(renderNode, "type") : "";
        String componentType = StringUtils.defaultIfBlank(firstText(item, "type", "componentType"), renderType);
        String transType = resolveTransType(componentType);
        if (StringUtils.isBlank(transType)) {
            return;
        }
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("type", transType);
        String targetField = renderNode != null && renderNode.isObject()
                ? StringUtils.defaultIfBlank(firstText(renderNode, "targetField"), fieldName + "Name")
                : fieldName + "Name";
        rule.put("targetField", targetField);
        rules.put(fieldName, rule);
    }

    private String resolveTransType(String componentType) {
        return switch (StringUtils.defaultString(componentType)) {
            case "orgTreeSelect", "orgName" -> "orgName";
            case "userSelect", "userName" -> "userName";
            case "regionTreeSelect", "regionName" -> "regionName";
            case "fileUpload", "imageUpload" -> componentType;
            default -> "";
        };
    }

    private String firstText(JsonNode node, String... fieldNames) {
        if (node == null) {
            return "";
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull() && StringUtils.isNotBlank(value.asText())) {
                return value.asText();
            }
        }
        return "";
    }

    private Map<String, Object> expandIncludeChildrenParams(Map<String, Object> searchParams,
                                                            AiCrudConfig config,
                                                            String tableName,
                                                            Set<String> allowedSearchFields,
                                                            Map<String, String> searchTypeMap) {
        if (searchParams == null || searchParams.isEmpty()) {
            return searchParams;
        }
        Map<String, Object> expanded = new LinkedHashMap<>(searchParams);
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, Object> entry : searchParams.entrySet()) {
            String key = entry.getKey();
            if (!key.endsWith("_includeChildren")) continue;
            if (!isTruthy(entry.getValue())) continue;
            String baseField = key.substring(0, key.length() - "_includeChildren".length());
            if (!allowedSearchFields.contains(baseField)) continue;
            Object baseValue = searchParams.get(baseField);
            if (baseValue == null) continue;
            keysToRemove.add(key);
            List<Object> values = normalizeIncludeChildrenValues(baseValue);
            if (values == null) {
                values = resolveIncludeChildrenValues(config, tableName, baseField, baseValue);
            }
            if (values == null || values.isEmpty()) {
                continue;
            }
            expanded.put(baseField, values);
            if (searchTypeMap != null) {
                searchTypeMap.put(baseField, "in");
            }
        }
        for (String key : keysToRemove) {
            expanded.remove(key);
        }
        return expanded;
    }

    private List<CustomQueryConditionDTO> expandCustomIncludeChildrenConditions(List<CustomQueryConditionDTO> conditions,
                                                                                AiCrudConfig config,
                                                                                String tableName,
                                                                                Set<String> allowedFields) {
        if (conditions == null || conditions.isEmpty()) {
            return conditions;
        }
        List<CustomQueryConditionDTO> expanded = new ArrayList<>(conditions.size());
        boolean changed = false;
        for (CustomQueryConditionDTO condition : conditions) {
            if (condition == null || !Boolean.TRUE.equals(condition.getIncludeChildren())
                    || StringUtils.isBlank(condition.getField()) || !allowedFields.contains(condition.getField())) {
                expanded.add(condition);
                continue;
            }
            List<Object> values = normalizeIncludeChildrenValues(condition.getValue());
            if (values == null) {
                values = resolveIncludeChildrenValues(config, tableName, condition.getField(), condition.getValue());
            }
            if (values == null || values.isEmpty()) {
                expanded.add(condition);
                continue;
            }
            CustomQueryConditionDTO next = new CustomQueryConditionDTO();
            next.setRelation(condition.getRelation());
            next.setField(condition.getField());
            next.setOperator("in");
            next.setValue(values);
            next.setValueEnd(null);
            next.setIncludeChildren(true);
            expanded.add(next);
            changed = true;
        }
        return changed ? expanded : conditions;
    }

    private boolean isTruthy(Object value) {
        if (Boolean.TRUE.equals(value)) {
            return true;
        }
        if (value instanceof String text) {
            return "true".equalsIgnoreCase(text) || "1".equals(text);
        }
        if (value instanceof Number number) {
            return number.intValue() == 1;
        }
        return false;
    }

    private List<Object> normalizeIncludeChildrenValues(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> !(item instanceof String text) || StringUtils.isNotBlank(text))
                    .distinct()
                    .map(item -> (Object) item)
                    .toList();
        }
        if (value instanceof Object[] array) {
            return Arrays.stream(array)
                    .filter(Objects::nonNull)
                    .filter(item -> !(item instanceof String text) || StringUtils.isNotBlank(text))
                    .distinct()
                    .map(item -> (Object) item)
                    .toList();
        }
        if (value instanceof String text && text.contains(",")) {
            return Arrays.stream(text.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .map(item -> (Object) item)
                    .toList();
        }
        return null;
    }

    private List<Object> resolveIncludeChildrenValues(AiCrudConfig config,
                                                      String tableName,
                                                      String baseField,
                                                      Object baseValue) {
        if (baseValue == null) {
            return List.of();
        }
        Object normalizedBaseValue = normalizeIncludeChildrenBaseValue(baseValue);
        LowcodeTreeConfig treeConfig = resolveIncludeChildrenTreeConfig(config, baseField);
        String sourceTable = treeConfig != null && StringUtils.isNotBlank(treeConfig.getSourceTableName())
                ? treeConfig.getSourceTableName()
                : tableName;
        if (StringUtils.isBlank(sourceTable) || !repository.tableExists(sourceTable)) {
            return List.of(normalizedBaseValue);
        }
        Map<String, String> columnMapping = repository.getColumnMapping(sourceTable);
        String keyField = treeConfig == null ? "id" : StringUtils.defaultIfBlank(treeConfig.getKeyField(), "id");
        String parentField = treeConfig == null
                ? DynamicQueryGenerator.camelToSnake(baseField)
                : StringUtils.defaultIfBlank(treeConfig.getParentField(), "parentId");
        String targetField = treeConfig == null
                ? "id"
                : StringUtils.defaultIfBlank(treeConfig.getTargetField(), keyField);
        String keyColumn = resolveColumnName(keyField, columnMapping);
        String parentColumn = resolveColumnName(parentField, columnMapping);
        String targetColumn = resolveColumnName(targetField, columnMapping);
        if (StringUtils.isBlank(keyColumn) || StringUtils.isBlank(parentColumn) || StringUtils.isBlank(targetColumn)) {
            return List.of(normalizedBaseValue);
        }

        LinkedHashSet<Object> resultValues = new LinkedHashSet<>();
        Deque<Object> queue = new ArrayDeque<>();
        LinkedHashSet<String> visitedKeys = new LinkedHashSet<>();
        if (targetColumn.equals(keyColumn)) {
            resultValues.add(normalizedBaseValue);
            queue.add(normalizedBaseValue);
            visitedKeys.add(String.valueOf(normalizedBaseValue));
        } else {
            List<Map<String, Object>> seeds = repository.selectListByColumn(sourceTable, targetColumn, normalizedBaseValue);
            if (seeds.isEmpty()) {
                return List.of(normalizedBaseValue);
            }
            for (Map<String, Object> seed : seeds) {
                Object keyValue = seed.get(keyColumn);
                if (keyValue == null) {
                    continue;
                }
                queue.add(keyValue);
                visitedKeys.add(String.valueOf(keyValue));
                Object targetValue = seed.get(targetColumn);
                resultValues.add(targetValue != null ? targetValue : normalizedBaseValue);
            }
        }

        while (!queue.isEmpty()) {
            Object currentKey = queue.removeFirst();
            List<Map<String, Object>> children = repository.selectListByColumn(sourceTable, parentColumn, currentKey);
            for (Map<String, Object> child : children) {
                Object childKey = child.get(keyColumn);
                if (childKey != null && visitedKeys.add(String.valueOf(childKey))) {
                    queue.addLast(childKey);
                }
                Object targetValue = child.get(targetColumn);
                if (targetValue != null) {
                    resultValues.add(targetValue);
                }
            }
        }
        return resultValues.isEmpty() ? List.of(normalizedBaseValue) : new ArrayList<>(resultValues);
    }

    private LowcodeTreeConfig resolveIncludeChildrenTreeConfig(AiCrudConfig config, String baseField) {
        if (config == null || StringUtils.isBlank(baseField)) {
            return null;
        }
        LowcodeTreeConfig treeConfig = resolveTreeConfig(config);
        if (isTreeRuntime(config) && StringUtils.equals(baseField, treeConfig.getFilterField())) {
            return treeConfig;
        }
        LowcodeTreeConfig systemTreeConfig = resolveSystemTreeConfig(config, baseField);
        return systemTreeConfig != null ? systemTreeConfig : null;
    }

    private Object normalizeIncludeChildrenBaseValue(Object baseValue) {
        if (baseValue instanceof String text && text.endsWith("ALL")) {
            return text.substring(0, text.length() - 3);
        }
        return baseValue;
    }

    private LowcodeTreeConfig resolveSystemTreeConfig(AiCrudConfig config, String baseField) {
        JsonNode fieldNode = findSchemaField(config.getSearchSchema(), baseField);
        if (fieldNode == null) {
            fieldNode = findSchemaField(config.getEditSchema(), baseField);
        }
        if (fieldNode == null) {
            return null;
        }
        String fieldType = firstText(fieldNode, "type", "componentType");
        if ("orgTreeSelect".equals(fieldType)) {
            return buildStaticTreeConfig("sys_org", "id", "parentId", "id", baseField);
        }
        if ("regionTreeSelect".equals(fieldType)) {
            return buildStaticTreeConfig("sys_region_code", "code", "parentCode", "code", baseField);
        }
        if ("treeSelect".equals(fieldType)) {
            JsonNode optionSource = fieldNode.path("props").path("optionSource");
            String api = firstText(optionSource, "api");
            if (StringUtils.contains(api, "/ai/crud/")) {
                return resolveTreeConfig(config);
            }
        }
        return null;
    }

    private LowcodeTreeConfig buildStaticTreeConfig(String sourceTable,
                                                    String keyField,
                                                    String parentField,
                                                    String targetField,
                                                    String filterField) {
        LowcodeTreeConfig treeConfig = new LowcodeTreeConfig();
        treeConfig.setSourceTableName(sourceTable);
        treeConfig.setKeyField(keyField);
        treeConfig.setParentField(parentField);
        treeConfig.setTargetField(targetField);
        treeConfig.setFilterField(filterField);
        treeConfig.setChildrenField("children");
        return treeConfig;
    }

    private JsonNode findSchemaField(String schemaJson, String fieldName) {
        if (StringUtils.isBlank(schemaJson) || StringUtils.isBlank(fieldName)) {
            return null;
        }
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            if (!schemaNode.isArray()) {
                return null;
            }
            for (JsonNode item : schemaNode) {
                String currentField = firstText(item, "field", "prop", "key", "dataIndex");
                if (fieldName.equals(currentField)) {
                    return item;
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 查找树形查询字段失败, field={}", fieldName, e);
        }
        return null;
    }

    private String resolveColumnName(String fieldName, Map<String, String> columnMapping) {
        if (StringUtils.isBlank(fieldName)) {
            return null;
        }
        String column = columnMapping.getOrDefault(fieldName, DynamicQueryGenerator.camelToSnake(fieldName));
        return columnMapping.containsValue(column) ? column : null;
    }

    private String normalizeModelFieldName(LowcodeModelSchema modelSchema, String fieldName) {
        if (StringUtils.isBlank(fieldName) || modelSchema == null || modelSchema.getFields() == null) {
            return fieldName;
        }
        return modelSchema.getFields().stream()
                .filter(field -> fieldName.equals(field.getField()) || fieldName.equals(field.getColumnName()))
                .map(com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema::getField)
                .findFirst()
                .orElse(fieldName);
    }

    private String resolveRelationDisplayField(LowcodePageModelRef ref, LowcodeRelationSchema relation) {
        if (ref == null || ref.getFields() == null || ref.getFields().isEmpty()) {
            return null;
        }
        String configured = resolveRefSourceField(ref, relation == null ? null : relation.getDisplayField());
        if (hasRefSourceField(ref, configured)) {
            return configured;
        }
        Set<String> excluded = new LinkedHashSet<>();
        excluded.add(resolveRefSourceField(ref, relation == null ? null : relation.getTargetField()));
        excluded.add(resolveRefSourceField(ref, relation == null ? null : relation.getSourceField()));
        String preferred = pickRelationDisplayField(ref, excluded, Set.of("name", "title", "label", "orgName", "deptName"));
        if (StringUtils.isNotBlank(preferred)) {
            return preferred;
        }
        return pickRelationDisplayField(ref, excluded, Set.of());
    }

    private String pickRelationDisplayField(LowcodePageModelRef ref, Set<String> excluded, Set<String> preferredNames) {
        if (ref == null || ref.getFields() == null) {
            return null;
        }
        for (Map<String, Object> field : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String columnName = text(field.get("columnName"));
            if (StringUtils.isBlank(sourceField)
                    || excluded.contains(sourceField)
                    || excluded.contains(columnName)
                    || isSystemFieldName(sourceField)
                    || isSystemFieldName(columnName)) {
                continue;
            }
            if (!preferredNames.isEmpty() && !preferredNames.contains(sourceField)) {
                continue;
            }
            return sourceField;
        }
        return null;
    }

    private boolean hasRefSourceField(LowcodePageModelRef ref, String sourceField) {
        if (ref == null || ref.getFields() == null || StringUtils.isBlank(sourceField)) {
            return false;
        }
        for (Map<String, Object> field : ref.getFields()) {
            String candidate = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String columnName = text(field.get("columnName"));
            if (sourceField.equals(candidate) || sourceField.equals(columnName)) {
                return true;
            }
        }
        return false;
    }

    private String resolveRefSourceField(LowcodePageModelRef ref, String value) {
        if (ref == null || ref.getFields() == null || StringUtils.isBlank(value)) {
            return value;
        }
        for (Map<String, Object> field : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String columnName = text(field.get("columnName"));
            if (value.equals(sourceField) || value.equals(columnName)) {
                return sourceField;
            }
        }
        return value;
    }

    private boolean isSystemFieldName(String fieldName) {
        return "id".equals(fieldName)
                || "tenantId".equals(fieldName)
                || "tenant_id".equals(fieldName)
                || "createBy".equals(fieldName)
                || "create_by".equals(fieldName)
                || "createTime".equals(fieldName)
                || "create_time".equals(fieldName)
                || "createDept".equals(fieldName)
                || "create_dept".equals(fieldName)
                || "updateBy".equals(fieldName)
                || "update_by".equals(fieldName)
                || "updateTime".equals(fieldName)
                || "update_time".equals(fieldName)
                || "delFlag".equals(fieldName)
                || "del_flag".equals(fieldName);
    }

    /**
     * 应用字典翻译
     */
    private void applyDictTranslation(List<Map<String, Object>> rows, String transConfigJson) {
        if (StringUtils.isBlank(transConfigJson) || rows == null || rows.isEmpty() || dictValueProvider == null) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(transConfigJson);
            if (!configNode.isObject()) return;

            Map<String, List<String>> orgIdBuckets = new LinkedHashMap<>();
            Map<String, List<String>> userIdBuckets = new LinkedHashMap<>();
            Map<String, List<String>> fileIdBuckets = new LinkedHashMap<>();
            Map<String, List<String>> regionCodeBuckets = new LinkedHashMap<>();
            Map<String, String> targetFieldMap = new LinkedHashMap<>();

            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                    String sourceField = entry.getKey();
                    JsonNode ruleNode = entry.getValue();
                    if (!row.containsKey(sourceField) || row.get(sourceField) == null) continue;

                    String transType = ruleNode.has("type") ? ruleNode.get("type").asText("") : "";
                    String dictType = ruleNode.has("dictType") ? ruleNode.get("dictType").asText("") : "";
                    String targetField = ruleNode.has("targetField") ? ruleNode.get("targetField").asText()
                            : sourceField + "Name";

                    if (StringUtils.isNotBlank(dictType)) {
                        String key = String.valueOf(row.get(sourceField));
                        String label = dictValueProvider.getLabel(dictType, key);
                        if (label != null) {
                            row.put(targetField, label);
                        }
                    } else if ("orgName".equals(transType)) {
                        String orgId = String.valueOf(row.get(sourceField));
                        orgIdBuckets.computeIfAbsent(sourceField, k -> new ArrayList<>()).add(orgId);
                        targetFieldMap.put(sourceField, targetField);
                    } else if ("userName".equals(transType)) {
                        String userId = String.valueOf(row.get(sourceField));
                        userIdBuckets.computeIfAbsent(sourceField, k -> new ArrayList<>()).add(userId);
                        targetFieldMap.put(sourceField, targetField);
                    } else if ("regionName".equals(transType)) {
                        String regionCode = String.valueOf(row.get(sourceField));
                        regionCodeBuckets.computeIfAbsent(sourceField, k -> new ArrayList<>()).add(regionCode);
                        targetFieldMap.put(sourceField, targetField);
                    } else if ("fileUpload".equals(transType) || "imageUpload".equals(transType)) {
                        String fileId = String.valueOf(row.get(sourceField));
                        fileIdBuckets.computeIfAbsent(sourceField, k -> new ArrayList<>()).add(fileId);
                        targetFieldMap.put(sourceField, targetField);
                    }
                }
            }

            applyBatchTranslation(rows, orgIdBuckets, "orgName",
                    (ids) -> dictValueProvider.batchGetOrgNames(ids), targetFieldMap);
            applyBatchTranslation(rows, userIdBuckets, "userName",
                    (ids) -> dictValueProvider.batchGetUserNames(ids), targetFieldMap);
            applyBatchTranslation(rows, regionCodeBuckets, "regionName",
                    (ids) -> dictValueProvider.batchGetRegionNames(ids), targetFieldMap);
            applyBatchTranslation(rows, fileIdBuckets, "fileUpload",
                    (ids) -> dictValueProvider.batchGetFileNames(ids), targetFieldMap);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 翻译处理失败", e);
        }
    }

    private void applyBatchTranslation(List<Map<String, Object>> rows,
                                       Map<String, List<String>> fieldBuckets,
                                       String transType,
                                       java.util.function.Function<List<String>, Map<String, String>> batchLoader,
                                       Map<String, String> targetFieldMap) {
        for (Map.Entry<String, List<String>> bucket : fieldBuckets.entrySet()) {
            String sourceField = bucket.getKey();
            List<String> ids = bucket.getValue().stream().distinct().toList();
            if (ids.isEmpty()) continue;
            Map<String, String> nameMap;
            try {
                nameMap = batchLoader.apply(ids);
            } catch (Exception e) {
                log.warn("[DynamicCrudService] 批量翻译失败, type={}, field={}", transType, sourceField, e);
                continue;
            }
            if (nameMap == null || nameMap.isEmpty()) continue;
            String targetField = targetFieldMap.getOrDefault(sourceField, sourceField + "Name");
            for (Map<String, Object> row : rows) {
                Object value = row.get(sourceField);
                if (value == null) continue;
                String key = String.valueOf(value);
                String name = nameMap.get(key);
                if (name != null) {
                    row.put(targetField, name);
                }
            }
        }
    }

    // ==================== 配置加载 ====================

    /**
     * 获取配置
     */
    private AiCrudConfig getConfig(String configKey) {
        AiCrudConfig config = configService.getByConfigKey(configKey);
        if (config == null || "1".equals(config.getStatus())) {
            throw new BusinessException("CRUD配置不存在或已停用: " + configKey);
        }
        if (!"CONFIG".equals(config.getMode())) {
            throw new BusinessException("该配置不是配置驱动模式: " + configKey);
        }
        if ("LOWCODE".equals(config.getBuildMode()) && !"PUBLISHED".equals(config.getPublishStatus())) {
            throw new BusinessException("低代码应用尚未发布: " + configKey);
        }
        return config;
    }

    private Set<String> buildAllowedCustomFields(AiCrudConfig config) {
        Set<String> fields = new HashSet<>();
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getSearchSchema(), objectMapper));
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper));
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper));
        fields.add("id");
        return fields;
    }

    private Set<String> buildAllowedSearchFields(AiCrudConfig config) {
        Set<String> fields = new HashSet<>(DynamicQueryGenerator.extractFieldNames(config.getSearchSchema(), objectMapper));
        if (isTreeRuntime(config)) {
            LowcodeTreeConfig treeConfig = resolveTreeConfig(config);
            if (StringUtils.isNotBlank(treeConfig.getFilterField())) {
                fields.add(treeConfig.getFilterField());
            }
        }
        return fields;
    }

    private ExportQueryContext buildExportQueryContext(String configKey, DynamicCrudQuery query) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        Map<String, String> columnMapping = repository.getColumnMapping(tableName);
        Set<String> allowedSearchFields = buildAllowedSearchFields(config);
        Map<String, String> searchTypeMap = DynamicQueryGenerator.extractSearchTypeMap(config.getSearchSchema(), objectMapper);
        Map<String, Object> searchParams = query != null ? query.getSearchParams() : null;
        searchParams = expandIncludeChildrenParams(searchParams, config, tableName, allowedSearchFields, searchTypeMap);
        RuntimeJoinContext joinContext = buildRuntimeJoinContext(config);
        return new ExportQueryContext(config, tableName, columnMapping, allowedSearchFields, searchTypeMap,
                searchParams, joinContext);
    }

    private DynamicCrudRepository.SqlCondition buildDataScopeCondition(AiCrudConfig config, String tableName, String tableAlias) {
        return dynamicDataScopeService.buildCondition(config, tableName, tableAlias);
    }

    private DynamicCrudRepository.SqlCondition buildDataScopeCondition(AiCrudConfig config,
                                                                       String tableName,
                                                                       String tableAlias,
                                                                       DataScopeContext dataScopeContext) {
        return dynamicDataScopeService.buildCondition(config, tableName, tableAlias, dataScopeContext);
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private int normalizeExportLimit(Integer maxRows) {
        if (maxRows == null || maxRows < 1) {
            return MAX_EXPORT_ROWS;
        }
        return Math.min(maxRows, MAX_EXPORT_ROWS);
    }

    private int normalizeExportPageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 1000;
        }
        return Math.min(pageSize, 5000);
    }
}

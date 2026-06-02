package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessStatsMetricQueryDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.mapper.BusinessStatsMapper;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessStatsMetricVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 业务对象统计服务。
 * <p>
 * 提供业务数据的聚合统计能力，支持按字段分组计数、时间趋势统计等。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessStatsService {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$");

    private final AiCrudConfigService configService;
    private final BusinessStatsMapper statsMapper;
    private final ObjectMapper objectMapper;

    /**
     * 业务对象概览统计：总记录数 + 今日新增 + 本月新增
     */
    public Map<String, Object> overview(String configKey) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = config.getTableName();
        validateIdentifier(tableName);
        Map<String, Object> result = statsMapper.selectOverview(tableName, TenantContextHolder.getTenantId());
        return result == null ? new LinkedHashMap<>() : result;
    }

    /**
     * 按字段分组计数统计
     *
     * @param configKey  CRUD配置键
     * @param groupField 分组字段名（camelCase 或 snake_case）
     * @return 每组的名称和计数
     */
    public List<Map<String, Object>> groupCount(String configKey, String groupField) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = validateTable(config);
        String columnName = resolveModelColumn(config, groupField);
        return statsMapper.selectGroupCount(tableName, columnName, TenantContextHolder.getTenantId());
    }

    /**
     * 时间趋势统计（按天、按周、按月）
     *
     * @param configKey  CRUD配置键
     * @param period     统计粒度：day / week / month
     * @param days       查看最近多少天的数据
     * @return 时间段和对应计数
     */
    public List<Map<String, Object>> trend(String configKey, String period, Integer days) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = validateTable(config);

        int lookbackDays = (days != null && days > 0) ? Math.min(days, 365) : 30;
        return statsMapper.selectTrend(tableName, TenantContextHolder.getTenantId(), normalizePeriod(period), lookbackDays);
    }

    public List<BusinessStatsMetricVO> metrics(String configKey, BusinessStatsMetricQueryDTO query) {
        AiCrudConfig config = getConfig(configKey);
        String tableName = validateTable(config);
        BusinessStatsMetricQueryDTO actualQuery = query == null ? new BusinessStatsMetricQueryDTO() : query;
        Set<String> metricTypes = normalizeMetricTypes(actualQuery.getMetricTypes());
        boolean all = metricTypes.isEmpty();

        List<BusinessStatsMetricVO> metrics = new ArrayList<>();
        if (all || metricTypes.contains("OVERVIEW")) {
            Map<String, Object> overview = statsMapper.selectOverview(tableName, TenantContextHolder.getTenantId());
            metrics.add(valueMetric("TOTAL", "记录总数", "COUNT", value(overview, "total"), "条"));
            metrics.add(valueMetric("TODAY", "今日新增", "COUNT", value(overview, "today"), "条"));
            metrics.add(valueMetric("MONTH", "本月新增", "COUNT", value(overview, "month"), "条"));
        }
        if (StringUtils.isNotBlank(actualQuery.getGroupField())) {
            metrics.add(groupMetric("GROUP_" + actualQuery.getGroupField(), "字段分布", actualQuery.getGroupField(),
                    statsMapper.selectGroupCount(tableName, resolveModelColumn(config, actualQuery.getGroupField()),
                            TenantContextHolder.getTenantId())));
        }
        if (StringUtils.isNotBlank(actualQuery.getStatusField())) {
            metrics.add(groupMetric("STATUS_DISTRIBUTION", "状态分布", actualQuery.getStatusField(),
                    statsMapper.selectGroupCount(tableName, resolveModelColumn(config, actualQuery.getStatusField()),
                            TenantContextHolder.getTenantId())));
        }
        if (StringUtils.isNotBlank(actualQuery.getStageField())) {
            metrics.add(groupMetric("STAGE_DISTRIBUTION", "阶段分布", actualQuery.getStageField(),
                    statsMapper.selectGroupCount(tableName, resolveModelColumn(config, actualQuery.getStageField()),
                            TenantContextHolder.getTenantId())));
        }
        if (StringUtils.isNotBlank(actualQuery.getAmountField()) && (all || metricTypes.contains("SUM"))) {
            metrics.add(sumAmount(config, actualQuery.getAmountField()));
        }
        if (Boolean.TRUE.equals(actualQuery.getIncludeFlowResult()) || metricTypes.contains("FLOW_RESULT")) {
            metrics.add(groupMetric("FLOW_RESULT_DISTRIBUTION", "流程结果分布", "flowResult",
                    statsMapper.selectFlowResultDistribution(TenantContextHolder.getTenantId(), config.getObjectCode())));
        }
        if (metricTypes.contains("TREND")) {
            BusinessStatsMetricVO trend = groupMetric("CREATE_TREND", "新增趋势", "createTime",
                    statsMapper.selectTrend(tableName, TenantContextHolder.getTenantId(),
                            normalizePeriod(actualQuery.getPeriod()),
                            actualQuery.getDays() == null ? 30 : Math.min(Math.max(actualQuery.getDays(), 1), 365)));
            trend.setMetricType("TREND");
            metrics.add(trend);
        }
        return metrics;
    }

    public BusinessStatsMetricVO sumAmount(String configKey, String amountField) {
        return sumAmount(getConfig(configKey), amountField);
    }

    public BusinessStatsMetricVO sumAmount(AiCrudConfig config, String amountField) {
        String tableName = validateTable(config);
        String columnName = resolveModelColumn(config, amountField);
        BusinessStatsMetricVO vo = valueMetric("SUM_" + amountField, "金额汇总", "SUM",
                statsMapper.selectSum(tableName, columnName, TenantContextHolder.getTenantId()), "分");
        vo.setField(amountField);
        return vo;
    }

    // ==================== Private ====================

    private AiCrudConfig getConfig(String configKey) {
        AiCrudConfig config = configService.getByConfigKey(configKey);
        if (config == null) {
            throw new BusinessException("未找到配置: " + configKey);
        }
        return config;
    }

    private String validateTable(AiCrudConfig config) {
        String tableName = config.getTableName();
        validateIdentifier(tableName);
        return tableName;
    }

    private void validateIdentifier(String name) {
        if (StringUtils.isBlank(name) || !SAFE_IDENTIFIER.matcher(name).matches()) {
            throw new BusinessException("非法标识符: " + name);
        }
    }

    private String resolveModelColumn(AiCrudConfig config, String field) {
        if (StringUtils.isBlank(field)) {
            throw new BusinessException("指标字段不能为空");
        }
        String normalized = field.trim();
        validateIdentifier(normalized);
        Map<String, String> columns = collectModelColumns(config);
        String columnName = columns.get(normalized);
        if (StringUtils.isBlank(columnName)) {
            columnName = columns.get(DynamicQueryGenerator.camelToSnake(normalized));
        }
        if (StringUtils.isBlank(columnName)) {
            columnName = columns.get(DynamicQueryGenerator.snakeToCamel(normalized));
        }
        if (StringUtils.isBlank(columnName)) {
            throw new BusinessException("指标字段不在模型中: " + field);
        }
        validateIdentifier(columnName);
        return columnName;
    }

    private Map<String, String> collectModelColumns(AiCrudConfig config) {
        Map<String, String> columns = new LinkedHashMap<>();
        addColumnAlias(columns, "id", "id");
        addColumnAlias(columns, "createTime", "create_time");
        addColumnAlias(columns, "create_time", "create_time");
        LowcodeModelSchema schema = readModelSchema(config);
        if (schema != null && schema.getFields() != null) {
            for (LowcodeFieldSchema field : schema.getFields()) {
                if (field == null) {
                    continue;
                }
                String fieldName = StringUtils.defaultIfBlank(field.getField(), field.getColumnName());
                String columnName = StringUtils.defaultIfBlank(field.getColumnName(),
                        DynamicQueryGenerator.camelToSnake(fieldName));
                addColumnAlias(columns, fieldName, columnName);
                addColumnAlias(columns, columnName, columnName);
            }
        }
        return columns;
    }

    private void addColumnAlias(Map<String, String> columns, String field, String columnName) {
        if (StringUtils.isBlank(field) || StringUtils.isBlank(columnName)) {
            return;
        }
        columns.put(field, columnName);
        columns.put(DynamicQueryGenerator.camelToSnake(field), columnName);
        columns.put(DynamicQueryGenerator.snakeToCamel(field), columnName);
    }

    private LowcodeModelSchema readModelSchema(AiCrudConfig config) {
        if (StringUtils.isBlank(config.getModelSchema())) {
            return null;
        }
        try {
            return objectMapper.readValue(config.getModelSchema(), LowcodeModelSchema.class);
        } catch (Exception e) {
            throw new BusinessException("模型 Schema 解析失败");
        }
    }

    private Set<String> normalizeMetricTypes(List<String> metricTypes) {
        Set<String> result = new LinkedHashSet<>();
        if (metricTypes == null) {
            return result;
        }
        for (String metricType : metricTypes) {
            if (StringUtils.isNotBlank(metricType)) {
                result.add(metricType.trim().toUpperCase(Locale.ROOT));
            }
        }
        return result;
    }

    private String normalizePeriod(String period) {
        String normalized = StringUtils.defaultIfBlank(period, "day").toLowerCase(Locale.ROOT);
        if (!Set.of("day", "week", "month").contains(normalized)) {
            return "day";
        }
        return normalized;
    }

    private BusinessStatsMetricVO valueMetric(String code, String name, String type, Object value, String unit) {
        BusinessStatsMetricVO vo = new BusinessStatsMetricVO();
        vo.setMetricCode(code);
        vo.setMetricName(name);
        vo.setMetricType(type);
        vo.setValue(value == null ? 0L : value);
        vo.setUnit(unit);
        return vo;
    }

    private BusinessStatsMetricVO groupMetric(String code, String name, String field, List<Map<String, Object>> items) {
        BusinessStatsMetricVO vo = new BusinessStatsMetricVO();
        vo.setMetricCode(code);
        vo.setMetricName(name);
        vo.setMetricType("GROUP");
        vo.setField(field);
        vo.setItems(items == null ? new ArrayList<>() : items);
        return vo;
    }

    private Object value(Map<String, Object> map, String key) {
        if (map == null || map.isEmpty()) {
            return 0L;
        }
        Object value = map.get(key);
        if (value != null) {
            return value;
        }
        value = map.get(key.toUpperCase(Locale.ROOT));
        if (value != null) {
            return value;
        }
        return map.getOrDefault(key.toLowerCase(Locale.ROOT), 0L);
    }
}

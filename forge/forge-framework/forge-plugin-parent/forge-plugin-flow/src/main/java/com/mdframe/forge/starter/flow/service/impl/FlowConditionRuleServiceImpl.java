package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.flow.entity.FlowConditionItem;
import com.mdframe.forge.starter.flow.entity.FlowConditionRule;
import com.mdframe.forge.starter.flow.mapper.FlowConditionItemMapper;
import com.mdframe.forge.starter.flow.mapper.FlowConditionRuleMapper;
import com.mdframe.forge.starter.flow.service.FlowConditionRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程条件规则服务实现
 *
 * @author forge
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowConditionRuleServiceImpl extends ServiceImpl<FlowConditionRuleMapper, FlowConditionRule>
        implements FlowConditionRuleService {

    private final FlowConditionItemMapper conditionItemMapper;
    private final ObjectMapper objectMapper;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Override
    public List<FlowConditionRule> getByModelId(String modelId) {
        return list(new LambdaQueryWrapper<FlowConditionRule>()
                .eq(FlowConditionRule::getModelId, modelId)
                .orderByAsc(FlowConditionRule::getPriority));
    }

    @Override
    public FlowConditionRule getBySequenceFlowId(String sequenceFlowId) {
        return getOne(new LambdaQueryWrapper<FlowConditionRule>()
                .eq(FlowConditionRule::getSequenceFlowId, sequenceFlowId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRule(FlowConditionRule rule, List<FlowConditionItem> items) {
        // 保存规则
        boolean result = save(rule);
        
        // 保存条件项
        if (result && items != null && !items.isEmpty()) {
            for (FlowConditionItem item : items) {
                item.setRuleId(rule.getId());
                item.setCreateTime(LocalDateTime.now());
                item.setUpdateTime(LocalDateTime.now());
                conditionItemMapper.insert(item);
            }
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRule(String id) {
        // 删除条件项
        conditionItemMapper.delete(new LambdaQueryWrapper<FlowConditionItem>()
                .eq(FlowConditionItem::getRuleId, id));
        
        // 删除规则
        return removeById(id);
    }

    @Override
    public List<FlowConditionItem> getConditionItems(String ruleId) {
        return conditionItemMapper.selectList(new LambdaQueryWrapper<FlowConditionItem>()
                .eq(FlowConditionItem::getRuleId, ruleId)
                .orderByAsc(FlowConditionItem::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveConditionItems(String ruleId, List<FlowConditionItem> items) {
        // 先删除旧的条件项
        conditionItemMapper.delete(new LambdaQueryWrapper<FlowConditionItem>()
                .eq(FlowConditionItem::getRuleId, ruleId));
        
        // 保存新的条件项
        if (items != null && !items.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (FlowConditionItem item : items) {
                item.setId(null);
                item.setRuleId(ruleId);
                item.setCreateTime(now);
                item.setUpdateTime(now);
                conditionItemMapper.insert(item);
            }
        }
        
        return true;
    }

    @Override
    public boolean evaluateRule(String ruleId, Map<String, Object> variables) {
        FlowConditionRule rule = getById(ruleId);
        if (rule == null) {
            return false;
        }
        
        // 如果是默认路径，直接返回true
        if (Boolean.TRUE.equals(rule.getIsDefault())) {
            return true;
        }
        
        // 根据条件类型评估
        String conditionType = rule.getConditionType();
        if ("script".equals(conditionType)) {
            // 脚本类型，直接执行表达式
            return evaluateExpression(rule.getConditionExpression(), variables);
        } else {
            // 简单条件或组合条件，从条件项构建并评估
            List<FlowConditionItem> items = getConditionItems(ruleId);
            return evaluateConditionItems(items, variables);
        }
    }

    @Override
    public String toBpmnExpression(String ruleId) {
        FlowConditionRule rule = getById(ruleId);
        if (rule == null) {
            return null;
        }
        
        // 如果是默认路径，返回空
        if (Boolean.TRUE.equals(rule.getIsDefault())) {
            return null;
        }
        
        // 如果是脚本类型，直接返回表达式
        if ("script".equals(rule.getConditionType())) {
            return rule.getConditionExpression();
        }
        
        // 从条件项构建表达式
        List<FlowConditionItem> items = getConditionItems(ruleId);
        return buildExpressionFromItems(items);
    }

    @Override
    public FlowConditionRule parseFromBpmnExpression(String expression, String sequenceFlowId) {
        if (expression == null || expression.isEmpty()) {
            // 创建默认规则
            FlowConditionRule rule = new FlowConditionRule();
            rule.setSequenceFlowId(sequenceFlowId);
            rule.setConditionType("simple");
            rule.setIsDefault(true);
            return rule;
        }
        
        FlowConditionRule rule = new FlowConditionRule();
        rule.setSequenceFlowId(sequenceFlowId);
        rule.setConditionType("script");
        rule.setConditionExpression(expression);
        rule.setIsDefault(false);
        
        return rule;
    }

    @Override
    public String validateRule(FlowConditionRule rule, List<FlowConditionItem> items) {
        if (rule == null) {
            return "规则不能为空";
        }
        
        if (rule.getRuleName() == null || rule.getRuleName().isEmpty()) {
            return "规则名称不能为空";
        }
        
        // 如果不是默认路径，检查条件配置
        if (!Boolean.TRUE.equals(rule.getIsDefault())) {
            if ("simple".equals(rule.getConditionType()) || "composite".equals(rule.getConditionType())) {
                if (items == null || items.isEmpty()) {
                    return "条件项不能为空";
                }
                
                for (FlowConditionItem item : items) {
                    if (item.getFieldName() == null || item.getFieldName().isEmpty()) {
                        return "条件项字段名不能为空";
                    }
                    if (item.getOperator() == null || item.getOperator().isEmpty()) {
                        return "条件项操作符不能为空";
                    }
                }
            } else if ("script".equals(rule.getConditionType())) {
                if (rule.getConditionExpression() == null || rule.getConditionExpression().isEmpty()) {
                    return "脚本表达式不能为空";
                }
            }
        }
        
        return null;
    }

    /**
     * 评估条件项列表
     */
    private boolean evaluateConditionItems(List<FlowConditionItem> items, Map<String, Object> variables) {
        if (items == null || items.isEmpty()) {
            return true;
        }
        
        // 按分组组织条件项
        Map<String, List<FlowConditionItem>> groupedItems = items.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getGroupId() != null ? item.getGroupId() : "default",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        
        // 评估每个分组
        List<Boolean> groupResults = new ArrayList<>();
        for (Map.Entry<String, List<FlowConditionItem>> entry : groupedItems.entrySet()) {
            boolean groupResult = evaluateGroup(entry.getValue(), variables);
            groupResults.add(groupResult);
        }
        
        // 如果只有一个分组，直接返回结果
        if (groupResults.size() == 1) {
            return groupResults.get(0);
        }
        
        // 多个分组之间使用OR连接
        return groupResults.stream().anyMatch(r -> r);
    }

    /**
     * 评估单个分组内的条件
     */
    private boolean evaluateGroup(List<FlowConditionItem> items, Map<String, Object> variables) {
        if (items == null || items.isEmpty()) {
            return true;
        }
        
        boolean result = true;
        String currentConnector = "and";
        
        for (FlowConditionItem item : items) {
            boolean itemResult = evaluateSingleCondition(item, variables);
            
            if ("and".equals(currentConnector)) {
                result = result && itemResult;
            } else {
                result = result || itemResult;
            }
            
            // 更新下一个连接符
            currentConnector = item.getLogicConnector() != null ? item.getLogicConnector() : "and";
        }
        
        return result;
    }

    /**
     * 评估单个条件
     */
    private boolean evaluateSingleCondition(FlowConditionItem item, Map<String, Object> variables) {
        String fieldName = item.getFieldName();
        String operator = item.getOperator();
        Object fieldValue = variables.get(fieldName);
        Object compareValue = parseValue(item.getValue(), item.getFieldType());
        
        if (fieldValue == null) {
            return "isEmpty".equals(operator);
        }
        
        switch (operator) {
            case "eq":
                return Objects.equals(fieldValue, compareValue);
            case "ne":
                return !Objects.equals(fieldValue, compareValue);
            case "gt":
                return compareNumbers(fieldValue, compareValue) > 0;
            case "lt":
                return compareNumbers(fieldValue, compareValue) < 0;
            case "ge":
                return compareNumbers(fieldValue, compareValue) >= 0;
            case "le":
                return compareNumbers(fieldValue, compareValue) <= 0;
            case "contains":
                return fieldValue.toString().contains(compareValue.toString());
            case "startsWith":
                return fieldValue.toString().startsWith(compareValue.toString());
            case "endsWith":
                return fieldValue.toString().endsWith(compareValue.toString());
            case "in":
                List<?> inList = parseListValue(item.getValue());
                return inList.contains(fieldValue);
            case "notIn":
                List<?> notInList = parseListValue(item.getValue());
                return !notInList.contains(fieldValue);
            case "isEmpty":
                return fieldValue == null || fieldValue.toString().isEmpty();
            case "isNotEmpty":
                return fieldValue != null && !fieldValue.toString().isEmpty();
            default:
                return false;
        }
    }

    /**
     * 评估表达式
     */
    private boolean evaluateExpression(String expression, Map<String, Object> variables) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            variables.forEach(context::setVariable);
            
            Boolean result = expressionParser.parseExpression(expression)
                    .getValue(context, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("评估条件表达式失败: {}", expression, e);
            return false;
        }
    }

    /**
     * 从条件项构建表达式
     */
    private String buildExpressionFromItems(List<FlowConditionItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("${");
        
        for (int i = 0; i < items.size(); i++) {
            FlowConditionItem item = items.get(i);
            
            if (i > 0) {
                String connector = item.getLogicConnector() != null ? item.getLogicConnector() : "and";
                sb.append(" ").append(connector).append(" ");
            }
            
            sb.append(buildSingleConditionExpression(item));
        }
        
        sb.append("}");
        return sb.toString();
    }

    /**
     * 构建单个条件的表达式
     */
    private String buildSingleConditionExpression(FlowConditionItem item) {
        String fieldName = item.getFieldName();
        String operator = item.getOperator();
        String value = item.getValue();
        
        switch (operator) {
            case "eq":
                return String.format("%s == %s", fieldName, formatValue(value, item.getFieldType()));
            case "ne":
                return String.format("%s != %s", fieldName, formatValue(value, item.getFieldType()));
            case "gt":
                return String.format("%s > %s", fieldName, value);
            case "lt":
                return String.format("%s < %s", fieldName, value);
            case "ge":
                return String.format("%s >= %s", fieldName, value);
            case "le":
                return String.format("%s <= %s", fieldName, value);
            case "contains":
                return String.format("%s.contains('%s')", fieldName, value);
            case "isEmpty":
                return String.format("%s == null || %s.isEmpty()", fieldName, fieldName);
            case "isNotEmpty":
                return String.format("%s != null && !%s.isEmpty()", fieldName, fieldName);
            default:
                return String.format("%s == %s", fieldName, formatValue(value, item.getFieldType()));
        }
    }

    /**
     * 解析值
     */
    private Object parseValue(String value, String fieldType) {
        if (value == null) {
            return null;
        }
        
        try {
            switch (fieldType) {
                case "number":
                    return new java.math.BigDecimal(value);
                case "boolean":
                    return Boolean.parseBoolean(value);
                case "date":
                    return java.time.LocalDate.parse(value);
                default:
                    return value;
            }
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * 解析列表值
     */
    private List<?> parseListValue(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            return objectMapper.readValue(value, new TypeReference<List<Object>>() {});
        } catch (Exception e) {
            log.error("解析列表值失败: {}", value, e);
            return Collections.emptyList();
        }
    }

    /**
     * 比较数字
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compareNumbers(Object value1, Object value2) {
        if (value1 instanceof Comparable && value2 instanceof Comparable) {
            try {
                return ((Comparable) value1).compareTo(value2);
            } catch (ClassCastException e) {
                // 类型不匹配，尝试转换为BigDecimal比较
                java.math.BigDecimal bd1 = new java.math.BigDecimal(value1.toString());
                java.math.BigDecimal bd2 = new java.math.BigDecimal(value2.toString());
                return bd1.compareTo(bd2);
            }
        }
        return 0;
    }

    /**
     * 格式化值
     */
    private String formatValue(String value, String fieldType) {
        if ("string".equals(fieldType) || "date".equals(fieldType)) {
            return "'" + value + "'";
        }
        return value;
    }
}
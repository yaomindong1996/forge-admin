package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.flow.entity.FlowApprovalLevel;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.entity.FlowNodeOperation;
import com.mdframe.forge.starter.flow.mapper.FlowApprovalLevelMapper;
import com.mdframe.forge.starter.flow.mapper.FlowNodeConfigMapper;
import com.mdframe.forge.starter.flow.mapper.FlowNodeOperationMapper;
import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
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
 * 流程审批节点配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowNodeConfigServiceImpl extends ServiceImpl<FlowNodeConfigMapper, FlowNodeConfig>
        implements FlowNodeConfigService {

    private final FlowApprovalLevelMapper approvalLevelMapper;
    private final FlowNodeOperationMapper nodeOperationMapper;
    private final ObjectMapper objectMapper;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final FlowOrgIntegrationService orgIntegrationService;

    @Override
    public List<FlowNodeConfig> getByModelId(String modelId) {
        return list(new LambdaQueryWrapper<FlowNodeConfig>()
                .eq(FlowNodeConfig::getModelId, modelId)
                .orderByAsc(FlowNodeConfig::getCreateTime));
    }

    @Override
    public FlowNodeConfig getByModelAndNode(String modelId, String nodeId) {
        return getOne(new LambdaQueryWrapper<FlowNodeConfig>()
                .eq(FlowNodeConfig::getModelId, modelId)
                .eq(FlowNodeConfig::getNodeId, nodeId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveNodeConfig(FlowNodeConfig nodeConfig, List<FlowApprovalLevel> levels) {
        // 保存节点配置
        boolean result = save(nodeConfig);
        
        // 保存层级配置
        if (result && levels != null && !levels.isEmpty()) {
            for (FlowApprovalLevel level : levels) {
                level.setNodeConfigId(nodeConfig.getId());
                approvalLevelMapper.insert(level);
            }
        }
        
        return result;
    }

    @Override
    public boolean updateNodeConfig(FlowNodeConfig nodeConfig) {
        return updateById(nodeConfig);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveNodeConfig(List<FlowNodeConfig> nodeConfigs) {
        if (nodeConfigs == null || nodeConfigs.isEmpty()) {
            return true;
        }
        return saveBatch(nodeConfigs);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNodeConfig(String id) {
        // 删除层级配置
        approvalLevelMapper.delete(new LambdaQueryWrapper<FlowApprovalLevel>()
                .eq(FlowApprovalLevel::getNodeConfigId, id));
        
        // 删除操作权限配置
        nodeOperationMapper.delete(new LambdaQueryWrapper<FlowNodeOperation>()
                .eq(FlowNodeOperation::getNodeConfigId, id));
        
        // 删除节点配置
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByModelId(String modelId) {
        // 获取该模型下所有节点配置
        List<FlowNodeConfig> configs = getByModelId(modelId);
        if (configs == null || configs.isEmpty()) {
            return true;
        }
        
        // 删除所有相关的层级配置和操作权限配置
        for (FlowNodeConfig config : configs) {
            approvalLevelMapper.delete(new LambdaQueryWrapper<FlowApprovalLevel>()
                    .eq(FlowApprovalLevel::getNodeConfigId, config.getId()));
            nodeOperationMapper.delete(new LambdaQueryWrapper<FlowNodeOperation>()
                    .eq(FlowNodeOperation::getNodeConfigId, config.getId()));
        }
        
        // 删除节点配置
        return remove(new LambdaQueryWrapper<FlowNodeConfig>()
                .eq(FlowNodeConfig::getModelId, modelId));
    }

    @Override
    public List<FlowApprovalLevel> getApprovalLevels(String nodeConfigId) {
        return approvalLevelMapper.selectList(new LambdaQueryWrapper<FlowApprovalLevel>()
                .eq(FlowApprovalLevel::getNodeConfigId, nodeConfigId)
                .orderByAsc(FlowApprovalLevel::getLevelIndex));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveApprovalLevels(String nodeConfigId, List<FlowApprovalLevel> levels) {
        // 先删除旧的层级配置
        approvalLevelMapper.delete(new LambdaQueryWrapper<FlowApprovalLevel>()
                .eq(FlowApprovalLevel::getNodeConfigId, nodeConfigId));
        
        // 保存新的层级配置
        if (levels != null && !levels.isEmpty()) {
            for (FlowApprovalLevel level : levels) {
                level.setId(null);
                level.setNodeConfigId(nodeConfigId);
                approvalLevelMapper.insert(level);
            }
        }
        
        return true;
    }

    @Override
    public List<String> calculateApprovers(String nodeConfigId, Map<String, Object> variables) {
        FlowNodeConfig nodeConfig = getById(nodeConfigId);
        if (nodeConfig == null) {
            return Collections.emptyList();
        }
        
        return calculateApproversByConfig(nodeConfig, variables);
    }

    @Override
    public List<String> calculateLevelApprovers(String nodeConfigId, int levelIndex, Map<String, Object> variables) {
        List<FlowApprovalLevel> levels = getApprovalLevels(nodeConfigId);
        if (levels == null || levels.isEmpty() || levelIndex > levels.size()) {
            return Collections.emptyList();
        }
        
        FlowApprovalLevel level = levels.get(levelIndex - 1);
        return calculateApproversByLevel(level, variables);
    }

    @Override
    public boolean hasNextLevel(String nodeConfigId, int currentLevel, Map<String, Object> variables) {
        List<FlowApprovalLevel> levels = getApprovalLevels(nodeConfigId);
        if (levels == null || levels.isEmpty()) {
            return false;
        }
        
        // 检查是否还有下一层级
        if (currentLevel >= levels.size()) {
            return false;
        }
        
        // 检查下一层级的条件是否满足
        FlowApprovalLevel nextLevel = levels.get(currentLevel);
        if (nextLevel.getConditionExpr() != null && !nextLevel.getConditionExpr().isEmpty()) {
            try {
                StandardEvaluationContext context = new StandardEvaluationContext();
                variables.forEach(context::setVariable);
                Boolean result = expressionParser.parseExpression(nextLevel.getConditionExpr())
                        .getValue(context, Boolean.class);
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                log.error("评估层级条件失败: {}", nextLevel.getConditionExpr(), e);
                return false;
            }
        }
        
        return true;
    }

    @Override
    public boolean hasNextLevel(String nodeConfigId, Integer currentLevelIndex) {
        List<FlowApprovalLevel> levels = getApprovalLevels(nodeConfigId);
        if (levels == null || levels.isEmpty()) {
            return false;
        }
        // 检查是否还有下一层级
        return currentLevelIndex < levels.size();
    }

    @Override
    public List<FlowNodeOperation> getNodeOperations(String nodeConfigId) {
        return nodeOperationMapper.selectList(new LambdaQueryWrapper<FlowNodeOperation>()
                .eq(FlowNodeOperation::getNodeConfigId, nodeConfigId)
                .orderByAsc(FlowNodeOperation::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveNodeOperations(String nodeConfigId, List<FlowNodeOperation> operations) {
        // 先删除旧的操作权限配置
        nodeOperationMapper.delete(new LambdaQueryWrapper<FlowNodeOperation>()
                .eq(FlowNodeOperation::getNodeConfigId, nodeConfigId));
        
        // 保存新的操作权限配置
        if (operations != null && !operations.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (FlowNodeOperation operation : operations) {
                operation.setId(null);
                operation.setNodeConfigId(nodeConfigId);
                operation.setCreateTime(now);
                operation.setUpdateTime(now);
                nodeOperationMapper.insert(operation);
            }
        }
        
        return true;
    }

    @Override
    public Long getTimeoutMillis(String nodeConfigId) {
        FlowNodeConfig nodeConfig = getById(nodeConfigId);
        if (nodeConfig == null) {
            return null;
        }
        
        long millis = 0;
        if (nodeConfig.getDueDateDays() != null && nodeConfig.getDueDateDays() > 0) {
            millis += nodeConfig.getDueDateDays() * 24L * 60 * 60 * 1000;
        }
        if (nodeConfig.getDueDateHours() != null && nodeConfig.getDueDateHours() > 0) {
            millis += nodeConfig.getDueDateHours() * 60L * 60 * 1000;
        }
        
        return millis > 0 ? millis : null;
    }

    /**
     * 根据节点配置计算审批人
     */
    private List<String> calculateApproversByConfig(FlowNodeConfig nodeConfig, Map<String, Object> variables) {
        String assigneeType = nodeConfig.getAssigneeType();
        String assigneeValue = nodeConfig.getAssigneeValue();
        
        if (assigneeType == null) {
            return Collections.emptyList();
        }
        
        switch (assigneeType) {
            case "user":
                return parseUserIds(assigneeValue);
            case "role":
                return getUserIdsByRole(assigneeValue);
            case "dept":
                return getUserIdsByDept(assigneeValue);
            case "post":
                return getUserIdsByPost(assigneeValue);
            case "leader":
                // 支持层级参数，从变量中获取 leaderLevel
                int leaderLevel = getLevelFromVariables(variables, "leaderLevel", 1);
                return getLeaderUserIds(variables, leaderLevel);
            case "deptManager":
                // 支持层级参数，从变量中获取 deptManagerLevel
                int deptManagerLevel = getLevelFromVariables(variables, "deptManagerLevel", 1);
                return getDeptManagerUserIds(variables, deptManagerLevel);
            case "initiator":
                return getInitiatorUserIds(variables);
            case "expr":
                return evaluateExpression(nodeConfig.getAssigneeExpr(), variables);
            case "deptUser":
                // 指定部门用户
                return getUserIdsByDept(assigneeValue);
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 根据层级配置计算审批人
     */
    private List<String> calculateApproversByLevel(FlowApprovalLevel level, Map<String, Object> variables) {
        String assigneeType = level.getAssigneeType();
        String assigneeValue = level.getAssigneeValue();
        
        if (assigneeType == null) {
            return Collections.emptyList();
        }
        
        switch (assigneeType) {
            case "user":
                return parseUserIds(assigneeValue);
            case "role":
                return getUserIdsByRole(assigneeValue);
            case "dept":
                return getUserIdsByDept(assigneeValue);
            case "post":
                return getUserIdsByPost(assigneeValue);
            case "leader":
                return getLeaderUserIds(variables);
            case "deptManager":
                return getDeptManagerUserIds(variables);
            case "expr":
                return evaluateExpression(level.getAssigneeValue(), variables);
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 解析用户ID列表
     */
    private List<String> parseUserIds(String jsonValue) {
        if (jsonValue == null || jsonValue.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            return objectMapper.readValue(jsonValue, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("解析用户ID失败: {}", jsonValue, e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据角色获取用户ID列表
     * TODO: 需要集成组织架构服务
     */
    private List<String> getUserIdsByRole(String roleValue) {
        // 调用组织架构服务获取角色下的用户
        // roleValue可以是角色编码或角色ID
        List<String> userIds = orgIntegrationService.getUserIdsByRoleCode(roleValue);
        if (userIds.isEmpty()) {
            // 尝试作为角色ID查询
            userIds = orgIntegrationService.getUserIdsByRoleId(roleValue);
        }
        return userIds;
    }

    /**
     * 根据部门获取用户ID列表
     */
    private List<String> getUserIdsByDept(String deptValue) {
        // 调用组织架构服务获取部门下的用户
        return orgIntegrationService.getUserIdsByDeptId(deptValue);
    }

    /**
     * 根据岗位获取用户ID列表
     */
    private List<String> getUserIdsByPost(String postValue) {
        // 调用组织架构服务获取岗位下的用户
        return orgIntegrationService.getUserIdsByPostId(postValue);
    }

    /**
     * 从变量中获取层级参数
     */
    private int getLevelFromVariables(Map<String, Object> variables, String key, int defaultValue) {
        Object level = variables.get(key);
        if (level == null) {
            return defaultValue;
        }
        if (level instanceof Number) {
            return ((Number) level).intValue();
        }
        try {
            return Integer.parseInt(level.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取上级领导用户ID列表
     */
    private List<String> getLeaderUserIds(Map<String, Object> variables) {
        return getLeaderUserIds(variables, 1);
    }

    /**
     * 获取指定层级的上级领导用户ID列表
     */
    private List<String> getLeaderUserIds(Map<String, Object> variables, int level) {
        // 从流程变量中获取发起人，然后获取其上级领导
        String initiator = (String) variables.get("initiator");
        if (initiator == null) {
            return Collections.emptyList();
        }
        // 调用组织架构服务获取指定层级的上级领导
        String leaderId = orgIntegrationService.getLeaderUserIdByLevel(initiator, level);
        if (leaderId != null) {
            return Collections.singletonList(leaderId);
        }
        // 如果指定层级没有找到，尝试获取默认上级领导
        return orgIntegrationService.getLeaderUserIds(initiator);
    }

    /**
     * 获取部门负责人用户ID列表
     */
    private List<String> getDeptManagerUserIds(Map<String, Object> variables) {
        return getDeptManagerUserIds(variables, 1);
    }

    /**
     * 获取指定层级的部门负责人用户ID列表
     */
    private List<String> getDeptManagerUserIds(Map<String, Object> variables, int level) {
        // 从流程变量中获取发起人所在部门，然后获取部门负责人
        String initiator = (String) variables.get("initiator");
        if (initiator == null) {
            return Collections.emptyList();
        }
        // 调用组织架构服务获取指定层级的部门负责人
        String managerId = orgIntegrationService.getDeptManagerUserIdByLevel(initiator, level);
        if (managerId != null) {
            return Collections.singletonList(managerId);
        }
        // 如果指定层级没有找到，尝试获取默认部门负责人
        return orgIntegrationService.getDeptManagerUserIds(initiator);
    }

    /**
     * 获取发起人用户ID列表
     */
    private List<String> getInitiatorUserIds(Map<String, Object> variables) {
        String initiator = (String) variables.get("initiator");
        if (initiator == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(initiator);
    }

    /**
     * 执行表达式获取用户ID列表
     */
    @SuppressWarnings("unchecked")
    private List<String> evaluateExpression(String expression, Map<String, Object> variables) {
        if (expression == null || expression.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            variables.forEach(context::setVariable);
            
            Object result = expressionParser.parseExpression(expression)
                    .getValue(context);
            
            if (result instanceof List) {
                return ((List<?>) result).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
            } else if (result instanceof String) {
                return Collections.singletonList((String) result);
            }
            
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("执行审批人表达式失败: {}", expression, e);
            return Collections.emptyList();
        }
    }
}
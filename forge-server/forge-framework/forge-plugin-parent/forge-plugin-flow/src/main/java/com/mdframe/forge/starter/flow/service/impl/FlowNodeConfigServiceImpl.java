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
import com.mdframe.forge.starter.flow.service.support.FlowSafeExpressionEvaluator;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final FlowOrgIntegrationService orgIntegrationService;

    @Override
    public List<FlowNodeConfig> getByModelId(String modelId) {
        Long tenantId = currentTenantId();
        if (tenantId == null || tenantId <= 0 || modelId == null || modelId.isBlank()) {
            return Collections.emptyList();
        }
        return baseMapper.selectByModelRef(modelId.trim(), tenantId);
    }

    @Override
    public FlowNodeConfig getByModelAndNode(String modelId, String nodeId) {
        Long tenantId = currentTenantId();
        if (tenantId == null || tenantId <= 0 || modelId == null || modelId.isBlank()
                || nodeId == null || nodeId.isBlank()) {
            return null;
        }
        return baseMapper.selectByModelKeyAndNode(modelId.trim(), nodeId.trim(), tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveNodeConfig(FlowNodeConfig nodeConfig, List<FlowApprovalLevel> levels) {
        Long tenantId = currentTenantId();
        if (tenantId == null || tenantId <= 0 || nodeConfig == null) {
            return false;
        }
        if (nodeConfig.getTenantId() != null && !tenantId.equals(nodeConfig.getTenantId())) {
            return false;
        }
        nodeConfig.setTenantId(tenantId);
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
        Long tenantId = currentTenantId();
        if (tenantId == null || tenantId <= 0 || nodeConfig == null || nodeConfig.getId() == null
                || (nodeConfig.getTenantId() != null && !tenantId.equals(nodeConfig.getTenantId()))) {
            return false;
        }
        FlowNodeConfig existing = baseMapper.selectByIdAndTenant(nodeConfig.getId(), tenantId);
        if (existing == null) {
            return false;
        }
        nodeConfig.setTenantId(tenantId);
        return updateById(nodeConfig);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveNodeConfig(List<FlowNodeConfig> nodeConfigs) {
        if (nodeConfigs == null || nodeConfigs.isEmpty()) {
            return true;
        }
        Long tenantId = currentTenantId();
        if (tenantId == null || tenantId <= 0) {
            return false;
        }
        for (FlowNodeConfig nodeConfig : nodeConfigs) {
            if (nodeConfig == null || (nodeConfig.getTenantId() != null
                    && !tenantId.equals(nodeConfig.getTenantId()))) {
                return false;
            }
            nodeConfig.setTenantId(tenantId);
        }
        return saveBatch(nodeConfigs);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNodeConfig(String id) {
        Long tenantId = currentTenantId();
        if (tenantId == null || tenantId <= 0 || id == null || id.isBlank()) {
            return false;
        }
        if (baseMapper.selectByIdAndTenant(id.trim(), tenantId) == null) {
            return false;
        }
        // 删除层级配置
        approvalLevelMapper.delete(new LambdaQueryWrapper<FlowApprovalLevel>()
                .eq(FlowApprovalLevel::getNodeConfigId, id));
        
        // 删除操作权限配置
        nodeOperationMapper.delete(new LambdaQueryWrapper<FlowNodeOperation>()
                .eq(FlowNodeOperation::getNodeConfigId, id));
        
        // 字符串主键表必须把当前主键写入 del_flag，不能使用 MP 默认固定删除值。
        return baseMapper.logicDeleteById(id, tenantId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByModelId(String modelId) {
        Long tenantId = currentTenantId();
        if (tenantId == null || tenantId <= 0 || modelId == null || modelId.isBlank()) {
            return false;
        }
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
        
        // 每一行写入自身主键作为删除墓碑。
        return baseMapper.logicDeleteByModelId(modelId.trim(), tenantId) > 0;
    }

    @Override
    public List<FlowApprovalLevel> getApprovalLevels(String nodeConfigId) {
        if (!isOwnedByCurrentTenant(nodeConfigId)) {
            return Collections.emptyList();
        }
        return approvalLevelMapper.selectList(new LambdaQueryWrapper<FlowApprovalLevel>()
                .eq(FlowApprovalLevel::getNodeConfigId, nodeConfigId)
                .orderByAsc(FlowApprovalLevel::getLevelIndex));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveApprovalLevels(String nodeConfigId, List<FlowApprovalLevel> levels) {
        if (!isOwnedByCurrentTenant(nodeConfigId)) {
            return false;
        }
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
        FlowNodeConfig nodeConfig = getOwnedNodeConfig(nodeConfigId);
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
                Boolean result = FlowSafeExpressionEvaluator.evaluateBoolean(
                        nextLevel.getConditionExpr(), variables);
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                log.warn("评估层级条件失败，表达式已拒绝或求值异常", e);
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
        if (!isOwnedByCurrentTenant(nodeConfigId)) {
            return Collections.emptyList();
        }
        return nodeOperationMapper.selectList(new LambdaQueryWrapper<FlowNodeOperation>()
                .eq(FlowNodeOperation::getNodeConfigId, nodeConfigId)
                .orderByAsc(FlowNodeOperation::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveNodeOperations(String nodeConfigId, List<FlowNodeOperation> operations) {
        if (!isOwnedByCurrentTenant(nodeConfigId)) {
            return false;
        }
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
        FlowNodeConfig nodeConfig = getOwnedNodeConfig(nodeConfigId);
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

    private boolean isOwnedByCurrentTenant(String nodeConfigId) {
        return getOwnedNodeConfig(nodeConfigId) != null;
    }

    private FlowNodeConfig getOwnedNodeConfig(String nodeConfigId) {
        Long tenantId = currentTenantId();
        if (tenantId == null || tenantId <= 0 || nodeConfigId == null || nodeConfigId.isBlank()) {
            return null;
        }
        return baseMapper.selectByIdAndTenant(nodeConfigId.trim(), tenantId);
    }

    private Long currentTenantId() {
        Long contextTenantId = TenantContextHolder.getTenantId();
        if (contextTenantId != null && contextTenantId > 0) {
            return contextTenantId;
        }
        return SessionHelper.getTenantId();
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
     * 根据节点配置计算审批人
     */
    public List<String> calculateApproversByConfig(FlowNodeConfig nodeConfig, Map<String, Object> variables) {
        if (nodeConfig == null) {
            log.warn("[审批人计算] 节点配置为空，无法计算审批人");
            return Collections.emptyList();
        }
        
        String assigneeType = nodeConfig.getAssigneeType();
        String assigneeValue = nodeConfig.getAssigneeValue();
        
        log.debug("[审批人计算] 开始计算: nodeConfigId={}, nodeDefKey={}, assigneeType={}",
                nodeConfig.getId(), nodeConfig.getFormKey(), assigneeType);
        
        if (assigneeType == null) {
            log.warn("[审批人计算] assigneeType为空，使用默认空列表");
            return Collections.emptyList();
        }
        
        List<String> result;
        switch (assigneeType) {
            case "user":
                result = parseUserIds(assigneeValue);
                log.debug("[审批人计算] 指定用户完成: count={}", result.size());
                return result;
            case "role":
                result = getUserIdsByRole(assigneeValue);
                log.debug("[审批人计算] 按角色查询完成: count={}", result.size());
                return result;
            case "dept":
                result = getUserIdsByDept(assigneeValue);
                log.debug("[审批人计算] 按部门查询完成: count={}", result.size());
                return result;
            case "post":
                result = getUserIdsByPost(assigneeValue);
                log.debug("[审批人计算] 按岗位查询完成: count={}", result.size());
                return result;
            case "leader":
                result = getLeaderUserIds(variables);
                log.debug("[审批人计算] 按上级查询完成: count={}", result.size());
                return result;
            case "deptManager":
                result = getDeptManagerUserIds(variables);
                log.debug("[审批人计算] 按部门负责人查询完成: count={}", result.size());
                return result;
            case "initiator":
                result = getInitiatorUserIds(variables);
                log.debug("[审批人计算] 按发起人查询完成: count={}", result.size());
                return result;
            case "expr":
                log.debug("[审批人计算] 按表达式计算: expressionLength={}",
                        nodeConfig.getAssigneeExpr() == null ? 0 : nodeConfig.getAssigneeExpr().length());
                return evaluateExpression(nodeConfig.getAssigneeExpr(), variables);
            case "deptUser":
                result = getUserIdsByDept(assigneeValue);
                log.debug("[审批人计算] 按部门用户查询完成: count={}", result.size());
                return result;
            default:
                log.warn("[审批人计算] 未知的assigneeType: {}, 无法计算审批人", assigneeType);
                return Collections.emptyList();
        }
    }
    
    /**
     * 解析用户ID列表（支持逗号分隔或JSON数组）
     */
    private List<String> parseUserIds(String jsonValue) {
        if (jsonValue == null || jsonValue.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 简单逗号分隔格式
        if (!jsonValue.startsWith("[") && !jsonValue.startsWith("{")) {
            return Arrays.asList(jsonValue.split(","));
        }
        
        // JSON数组格式
        try {
            return objectMapper.readValue(jsonValue, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("解析用户ID失败: error={}", e.getMessage(), e);
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
            log.warn("[审批人表达式] 表达式为空，无法计算审批人");
            return Collections.emptyList();
        }
        
        log.debug("[审批人表达式] 开始执行: expressionLength={}, variableCount={}",
                expression.length(), variables == null ? 0 : variables.size());
        
        try {
            Object result = FlowSafeExpressionEvaluator.evaluate(expression, variables);
            
            log.debug("[审批人表达式] 执行结果: resultType={}",
                    result != null ? result.getClass().getSimpleName() : "null");
            
            if (result == null) {
                log.warn("[审批人表达式] 表达式返回null，未匹配到审批人，请检查变量或表达式配置");
                return Collections.emptyList();
            }
            
            if (result instanceof List) {
                List<String> userIds = ((List<?>) result).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
                log.debug("[审批人表达式] 返回用户列表: count={}", userIds.size());
                return userIds;
            } else if (result instanceof String) {
                String userId = (String) result;
                if (userId.isEmpty()) {
                    log.warn("[审批人表达式] 表达式返回空字符串");
                    return Collections.emptyList();
                }
                log.debug("[审批人表达式] 返回单个用户");
                return Collections.singletonList(userId);
            }
            
            log.warn("[审批人表达式] 表达式返回非预期类型: resultType={}", result.getClass().getName());
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("[审批人表达式] 执行失败，表达式已拒绝或求值异常", e);
            return Collections.emptyList();
        }
    }
}

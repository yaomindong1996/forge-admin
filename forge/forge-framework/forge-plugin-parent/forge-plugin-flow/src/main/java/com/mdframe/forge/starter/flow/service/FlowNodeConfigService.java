package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.entity.FlowApprovalLevel;
import com.mdframe.forge.starter.flow.entity.FlowNodeOperation;

import java.util.List;
import java.util.Map;

/**
 * 流程审批节点配置服务接口
 */
public interface FlowNodeConfigService extends IService<FlowNodeConfig> {

    /**
     * 根据模型ID获取所有节点配置
     * @param modelId 模型ID
     * @return 节点配置列表
     */
    List<FlowNodeConfig> getByModelId(String modelId);

    /**
     * 根据模型ID和节点ID获取配置
     * @param modelId 模型ID
     * @param nodeId 节点ID
     * @return 节点配置
     */
    FlowNodeConfig getByModelAndNode(String modelId, String nodeId);

    /**
     * 保存节点配置（包含层级配置）
     * @param nodeConfig 节点配置
     * @param levels 层级配置列表
     * @return 是否成功
     */
    boolean saveNodeConfig(FlowNodeConfig nodeConfig, List<FlowApprovalLevel> levels);

    /**
     * 批量保存节点配置
     * @param nodeConfigs 节点配置列表
     * @return 是否成功
     */
    boolean batchSaveNodeConfig(List<FlowNodeConfig> nodeConfigs);

    /**
     * 更新节点配置
     * @param nodeConfig 节点配置
     * @return 是否成功
     */
    boolean updateNodeConfig(FlowNodeConfig nodeConfig);

    /**
     * 删除节点配置
     * @param id 配置ID
     * @return 是否成功
     */
    boolean deleteNodeConfig(String id);

    /**
     * 根据模型ID删除所有节点配置
     * @param modelId 模型ID
     * @return 是否成功
     */
    boolean deleteByModelId(String modelId);

    /**
     * 根据节点配置ID获取层级配置列表
     * @param nodeConfigId 节点配置ID
     * @return 层级配置列表
     */
    List<FlowApprovalLevel> getApprovalLevels(String nodeConfigId);

    /**
     * 保存层级配置
     * @param nodeConfigId 节点配置ID
     * @param levels 层级配置列表
     * @return 是否成功
     */
    boolean saveApprovalLevels(String nodeConfigId, List<FlowApprovalLevel> levels);

    /**
     * 获取节点操作权限配置
     * @param nodeConfigId 节点配置ID
     * @return 操作权限配置列表
     */
    List<FlowNodeOperation> getNodeOperations(String nodeConfigId);

    /**
     * 保存节点操作权限配置
     * @param nodeConfigId 节点配置ID
     * @param operations 操作权限配置列表
     * @return 是否成功
     */
    boolean saveNodeOperations(String nodeConfigId, List<FlowNodeOperation> operations);

    /**
     * 计算审批人
     * @param nodeConfigId 节点配置ID
     * @param variables 流程变量
     * @return 审批人用户ID列表
     */
    List<String> calculateApprovers(String nodeConfigId, Map<String, Object> variables);

    /**
     * 计算当前层级审批人
     * @param nodeConfigId 节点配置ID
     * @param levelIndex 层级索引
     * @param variables 流程变量
     * @return 审批人用户ID列表
     */
    List<String> calculateLevelApprovers(String nodeConfigId, int levelIndex, Map<String, Object> variables);

    /**
     * 检查是否需要进入下一层级
     * @param nodeConfigId 节点配置ID
     * @param currentLevel 当前层级
     * @param variables 流程变量
     * @return 是否需要进入下一层级
     */
    boolean hasNextLevel(String nodeConfigId, int currentLevel, Map<String, Object> variables);

    /**
     * 检查是否有下一层级（简化版）
     * @param nodeConfigId 节点配置ID
     * @param currentLevelIndex 当前层级索引
     * @return 是否有下一层级
     */
    boolean hasNextLevel(String nodeConfigId, Integer currentLevelIndex);

    /**
     * 获取节点超时配置
     * @param nodeConfigId 节点配置ID
     * @return 超时时间（毫秒）
     */
    Long getTimeoutMillis(String nodeConfigId);
}
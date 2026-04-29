package com.mdframe.forge.flow.controller;

import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.entity.FlowApprovalLevel;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.entity.FlowNodeOperation;
import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 流程节点配置控制器
 *
 * @author forge
 */
@RestController
@RequestMapping("/api/flow/nodeConfig")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowNodeConfigController {

    private final FlowNodeConfigService flowNodeConfigService;

    /**
     * 根据模型ID获取所有节点配置
     */
    @GetMapping("/model/{modelId}")
    public RespInfo<List<FlowNodeConfig>> getByModelId(@PathVariable String modelId) {
        return RespInfo.success(flowNodeConfigService.getByModelId(modelId));
    }

    /**
     * 根据模型ID和节点ID获取配置
     */
    @GetMapping("/model/{modelId}/node/{nodeId}")
    public RespInfo<FlowNodeConfig> getByModelAndNode(
            @PathVariable String modelId,
            @PathVariable String nodeId) {
        return RespInfo.success(flowNodeConfigService.getByModelAndNode(modelId, nodeId));
    }

    /**
     * 保存节点配置
     */
    @PostMapping
    public RespInfo<Boolean> saveNodeConfig(@RequestBody FlowNodeConfig nodeConfig) {
        return RespInfo.success(flowNodeConfigService.saveNodeConfig(nodeConfig, null));
    }

    /**
     * 批量保存节点配置
     */
    @PostMapping("/batch")
    public RespInfo<Boolean> batchSaveNodeConfig(@RequestBody List<FlowNodeConfig> nodeConfigs) {
        return RespInfo.success(flowNodeConfigService.batchSaveNodeConfig(nodeConfigs));
    }

    /**
     * 删除节点配置
     */
    @DeleteMapping("/{id}")
    public RespInfo<Boolean> deleteNodeConfig(@PathVariable String id) {
        return RespInfo.success(flowNodeConfigService.deleteNodeConfig(id));
    }

    /**
     * 根据模型ID删除所有节点配置
     */
    @DeleteMapping("/model/{modelId}")
    public RespInfo<Boolean> deleteByModelId(@PathVariable String modelId) {
        return RespInfo.success(flowNodeConfigService.deleteByModelId(modelId));
    }

    /**
     * 获取节点审批层级配置
     */
    @GetMapping("/{nodeConfigId}/levels")
    public RespInfo<List<FlowApprovalLevel>> getApprovalLevels(@PathVariable String nodeConfigId) {
        return RespInfo.success(flowNodeConfigService.getApprovalLevels(nodeConfigId));
    }

    /**
     * 保存审批层级配置
     */
    @PostMapping("/{nodeConfigId}/levels")
    public RespInfo<Boolean> saveApprovalLevels(
            @PathVariable String nodeConfigId,
            @RequestBody List<FlowApprovalLevel> levels) {
        return RespInfo.success(flowNodeConfigService.saveApprovalLevels(nodeConfigId, levels));
    }

    /**
     * 获取节点操作权限配置
     */
    @GetMapping("/{nodeConfigId}/operations")
    public RespInfo<List<FlowNodeOperation>> getNodeOperations(@PathVariable String nodeConfigId) {
        return RespInfo.success(flowNodeConfigService.getNodeOperations(nodeConfigId));
    }

    /**
     * 保存节点操作权限配置
     */
    @PostMapping("/{nodeConfigId}/operations")
    public RespInfo<Boolean> saveNodeOperations(
            @PathVariable String nodeConfigId,
            @RequestBody List<FlowNodeOperation> operations) {
        return RespInfo.success(flowNodeConfigService.saveNodeOperations(nodeConfigId, operations));
    }

    /**
     * 计算审批人
     * 根据流程实例变量动态计算当前节点的审批人
     */
    @PostMapping("/calculateApprovers")
    public RespInfo<List<String>> calculateApprovers(
            @RequestParam String nodeConfigId,
            @RequestBody Map<String, Object> variables) {
        return RespInfo.success(flowNodeConfigService.calculateApprovers(nodeConfigId, variables));
    }

    /**
     * 计算指定层级的审批人
     */
    @PostMapping("/calculateLevelApprovers")
    public RespInfo<List<String>> calculateLevelApprovers(
            @RequestParam String nodeConfigId,
            @RequestParam Integer levelIndex,
            @RequestBody Map<String, Object> variables) {
        return RespInfo.success(flowNodeConfigService.calculateLevelApprovers(nodeConfigId, levelIndex, variables));
    }

    /**
     * 检查是否有下一级审批
     */
    @GetMapping("/hasNextLevel")
    public RespInfo<Boolean> hasNextLevel(
            @RequestParam String nodeConfigId,
            @RequestParam Integer currentLevelIndex) {
        return RespInfo.success(flowNodeConfigService.hasNextLevel(nodeConfigId, currentLevelIndex));
    }

    /**
     * 获取超时时间（毫秒）
     */
    @GetMapping("/timeout")
    public RespInfo<Long> getTimeoutMillis(@RequestParam String nodeConfigId) {
        return RespInfo.success(flowNodeConfigService.getTimeoutMillis(nodeConfigId));
    }
}
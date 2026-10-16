package com.mdframe.forge.plugin.ai.agenttool.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.agenttool.domain.AiAgentToolConfig;
import com.mdframe.forge.plugin.ai.agenttool.domain.AiAgentToolPermission;
import com.mdframe.forge.plugin.ai.agenttool.mapper.AiAgentToolConfigMapper;
import com.mdframe.forge.plugin.ai.agenttool.mapper.AiAgentToolPermissionMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Agent 工具管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentToolService extends ServiceImpl<AiAgentToolConfigMapper, AiAgentToolConfig> {

    private final AiAgentToolPermissionMapper toolPermissionMapper;

    /**
     * 分页查询工具绑定
     */
    public Page<AiAgentToolConfig> selectToolPage(Integer pageNum, Integer pageSize,
                                                   Long agentId, String toolSource,
                                                   String keyword) {
        return baseMapper.selectToolConfigPage(
                new Page<>(pageNum, pageSize), agentId, toolSource, keyword);
    }

    /**
     * 新增工具绑定
     */
    @Transactional(rollbackFor = Exception.class)
    public void createTool(AiAgentToolConfig tool) {
        validateTool(tool.getAgentId(), tool.getToolSource(),
                tool.getToolKey(), null);
        if (!save(tool)) {
            throw new BusinessException("工具新增失败");
        }
    }

    /**
     * 更新工具绑定
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTool(AiAgentToolConfig tool) {
        if (tool == null || tool.getId() == null) {
            throw new BusinessException("工具ID不能为空");
        }
        validateTool(tool.getAgentId(), tool.getToolSource(),
                tool.getToolKey(), tool.getId());
        if (!updateById(tool)) {
            throw new BusinessException("工具更新失败");
        }
    }

    /**
     * 删除工具绑定（级联删除权限记录）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTool(Long id) {
        AiAgentToolConfig tool = getById(id);
        if (tool == null) {
            throw new BusinessException("工具不存在");
        }
        // 级联删除工具权限
        toolPermissionMapper.deleteByAgentIdAndToolKey(
                tool.getAgentId(), tool.getToolKey());
        removeById(id);
    }

    /**
     * 查询 Agent 工具权限列表
     */
    public List<AiAgentToolPermission> getPermissions(Long agentId, String toolKey) {
        if (toolKey != null && !toolKey.isBlank()) {
            return toolPermissionMapper.selectByAgentIdAndToolKey(agentId, toolKey);
        }
        return toolPermissionMapper.selectByAgentId(agentId);
    }

    /**
     * 按 Agent 查询已启用(enabled='1')的工具绑定配置，供引擎运行时声明工具给模型。
     * 已过滤逻辑删除与未启用项。
     */
    public List<AiAgentToolConfig> listEnabledByAgentId(Long agentId) {
        if (agentId == null) {
            return List.of();
        }
        return baseMapper.selectEnabledByAgentId(agentId);
    }

    /**
     * 批量保存工具权限
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchSavePermissions(Long agentId, String toolKey,
                                      List<AiAgentToolPermission> permissions) {
        // 先清除旧权限
        toolPermissionMapper.deleteByAgentIdAndToolKey(agentId, toolKey);
        // 再批量写入
        for (AiAgentToolPermission perm : permissions) {
            perm.setAgentId(agentId);
            perm.setToolKey(toolKey);
            toolPermissionMapper.insert(perm);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void savePermissions(Long agentId, String toolKey,
                                 List<AiAgentToolPermission> permissions) {
        batchSavePermissions(agentId, toolKey, permissions);
    }

    /**
     * 删除工具权限
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePermissions(Long agentId, String toolKey) {
        toolPermissionMapper.deleteByAgentIdAndToolKey(agentId, toolKey);
    }

    private void validateTool(Long agentId, String toolSource,
                               String toolKey, Long excludeId) {
        if (agentId == null) {
            throw new BusinessException("Agent不能为空");
        }
        if (toolSource == null || toolSource.isBlank()) {
            throw new BusinessException("工具来源不能为空");
        }
        if (toolKey == null || toolKey.isBlank()) {
            throw new BusinessException("工具标识不能为空");
        }
        int count = baseMapper.countByAgentAndKey(
                agentId, toolSource, toolKey, excludeId);
        if (count > 0) {
            throw new BusinessException("同一个Agent下已存在相同来源和标识的工具");
        }
    }
}

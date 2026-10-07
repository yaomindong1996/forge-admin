package com.mdframe.forge.plugin.system.service.plugin;

import com.mdframe.forge.plugin.system.entity.SysPluginTask;
import com.mdframe.forge.plugin.system.mapper.SysPluginTaskMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PluginTaskRepository {
    private final SysPluginTaskMapper mapper;

    public SysPluginTask required(PluginTaskActor actor, String id) {
        SysPluginTask task = mapper.selectTask(actor.tenantId(), id);
        if (task == null) {
            throw new BusinessException(404, "插件任务不存在或不属于当前租户");
        }
        return task;
    }

    public SysPluginTask byRequest(PluginTaskActor actor, String requestId) {
        return mapper.selectByRequest(actor.tenantId(), actor.userId(), requestId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(SysPluginTask task) {
        mapper.insert(task);
    }
}

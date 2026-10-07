package com.mdframe.forge.plugin.system.service.plugin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysPluginTaskQuery;
import com.mdframe.forge.plugin.system.mapper.SysPluginTaskMapper;
import com.mdframe.forge.plugin.system.vo.SysPluginTaskPageVO;
import com.mdframe.forge.plugin.system.vo.SysPluginTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PluginTaskQueryService {
    private final SysPluginTaskMapper mapper;
    private final PluginTaskRepository repository;
    private final PluginTaskViews views;
    private final PluginBuildViews builds;

    public SysPluginTaskPageVO page(SysPluginTaskQuery query, PluginTaskActor actor) {
        var page = mapper.selectTaskPage(new Page<>(query.getPageNum(), query.getPageSize()), actor.tenantId(), query);
        return new SysPluginTaskPageVO(page.getRecords().stream().map(task -> views.view(task, false)).toList(),
                page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    public SysPluginTaskVO detail(String id, PluginTaskActor actor) {
        return views.view(repository.required(actor, id), true).withExecution(builds.summary(actor.tenantId(), id));
    }
}

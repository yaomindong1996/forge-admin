package com.mdframe.forge.plugin.system.service.plugin;

import com.mdframe.forge.plugin.system.dto.SysPluginTaskCommandDTO;
import com.mdframe.forge.plugin.system.entity.SysPluginTask;
import com.mdframe.forge.plugin.system.enums.PluginTaskStatus;
import com.mdframe.forge.plugin.system.mapper.SysPluginTaskMapper;
import com.mdframe.forge.plugin.system.vo.SysPluginTaskVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PluginTaskCommandService {
    private final PluginTaskRepository repository;
    private final SysPluginTaskMapper mapper;
    private final PluginTaskViews views;
    private final PluginPackagePreflight preflight;
    private final PluginPreviewPlanner planner;

    @Transactional(rollbackFor = Exception.class)
    public SysPluginTaskVO confirm(String id, SysPluginTaskCommandDTO command, PluginTaskActor actor) {
        SysPluginTask task = repository.required(actor, id);
        requireHash(task, command);
        if (PluginTaskStatus.QUEUED.matches(task.getTaskStatus())) {
            requireRepeatRevision(task, command);
            return views.view(task, true);
        }
        requireRevision(task, command);
        require(PluginTaskStatus.AWAIT_CONFIRMATION.matches(task.getTaskStatus()), "当前任务不能确认");
        require(task.getRuntimeSnapshot().equals(planner.snapshot()), "运行清单已变化，请重新上传预检");
        SysPluginTask archive = mapper.selectArchive(actor.tenantId(), id);
        byte[] bytes = archive == null ? null : archive.getArchiveData();
        require(bytes != null && task.getArchiveSha256().equals(PackageDigests.sha256(bytes)), "任务包摘要异常");
        require(planner.preview(preflight.inspect(bytes)).blockers().isEmpty(), "当前包预检已被阻断，请重新上传");
        task.setConfirmedBy(actor.userId());
        task.setConfirmedTime(LocalDateTime.now());
        transition(task, PluginTaskStatus.QUEUED, command, actor);
        return views.view(task, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public SysPluginTaskVO cancel(String id, SysPluginTaskCommandDTO command, PluginTaskActor actor) {
        SysPluginTask task = repository.required(actor, id);
        requireHash(task, command);
        if (PluginTaskStatus.CANCELLED.matches(task.getTaskStatus())) {
            requireRepeatRevision(task, command);
            return views.view(task, true);
        }
        requireRevision(task, command);
        require(PluginTaskStatus.AWAIT_CONFIRMATION.matches(task.getTaskStatus())
                || PluginTaskStatus.QUEUED.matches(task.getTaskStatus()), "当前任务不能取消");
        task.setCancelledBy(actor.userId());
        task.setCancelledTime(LocalDateTime.now());
        task.setActivePluginId(null);
        transition(task, PluginTaskStatus.CANCELLED, command, actor);
        return views.view(task, true);
    }

    private void transition(SysPluginTask task, PluginTaskStatus target,
                            SysPluginTaskCommandDTO command, PluginTaskActor actor) {
        String previous = task.getTaskStatus();
        task.setTaskStatus(target.getCode());
        task.setUpdateBy(actor.userId());
        task.setUpdateTime(LocalDateTime.now());
        require(mapper.transition(task, previous, command.getRevision()) == 1, "任务已被其它请求修改，请刷新");
        task.setRevision(command.getRevision() + 1);
    }

    private void requireHash(SysPluginTask task, SysPluginTaskCommandDTO command) {
        require(task.getArchiveSha256().equals(command.getSha256()), "包摘要与预览不一致，请刷新");
    }

    private void requireRevision(SysPluginTask task, SysPluginTaskCommandDTO command) {
        require(task.getRevision().equals(command.getRevision()), "预览版本已过期，请刷新");
    }

    private void requireRepeatRevision(SysPluginTask task, SysPluginTaskCommandDTO command) {
        int revision = command.getRevision();
        require(revision == task.getRevision() || revision == task.getRevision() - 1, "预览版本已过期，请刷新");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(409, message);
        }
    }
}

package com.mdframe.forge.plugin.system.service.plugin;

import com.mdframe.forge.plugin.system.dto.SysPluginUploadDTO;
import com.mdframe.forge.plugin.system.entity.SysPluginTask;
import com.mdframe.forge.plugin.system.enums.PluginTaskStatus;
import com.mdframe.forge.plugin.system.vo.SysPluginPreviewVO;
import com.mdframe.forge.plugin.system.vo.SysPluginTaskVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PluginTaskUploadService {
    private final PluginTaskRepository repository;
    private final PluginPackagePreflight preflight;
    private final PluginPreviewPlanner planner;
    private final PluginTaskViews views;

    public SysPluginTaskVO upload(SysPluginUploadDTO dto, PluginTaskActor actor) {
        String name = preflight.fileName(dto.getFile());
        byte[] bytes = preflight.read(dto.getFile());
        SysPluginTask existing = repository.byRequest(actor, dto.getRequestId());
        if (existing != null) {
            return idempotent(existing, bytes);
        }
        SysPluginPreviewVO preview = planner.preview(preflight.inspect(bytes));
        SysPluginTask task = createTask(dto, actor, preview, name);
        task.setArchiveData(bytes);
        try {
            repository.create(task);
        } catch (DuplicateKeyException exception) {
            // 多实例并发相同 requestId 由数据库唯一键兜底，不会多创建任务/包。
            SysPluginTask duplicate = repository.byRequest(actor, dto.getRequestId());
            if (duplicate == null) {
                throw new BusinessException(409, "该插件已有未结束任务，请先处理或取消后重新上传");
            }
            return idempotent(duplicate, bytes);
        }
        return views.view(task, true);
    }

    private SysPluginTask createTask(SysPluginUploadDTO dto, PluginTaskActor actor,
                                     SysPluginPreviewVO preview, String name) {
        var descriptor = preview.source().descriptor();
        SysPluginTask task = new SysPluginTask();
        task.setId(UUID.randomUUID().toString());
        task.setTenantId(actor.tenantId());
        task.setRequestId(dto.getRequestId());
        task.setPluginId(descriptor.id());
        task.setPluginName(descriptor.name());
        task.setPluginVersion(descriptor.version());
        task.setOperationType(preview.operation());
        task.setTaskStatus((preview.blockers().isEmpty()
                ? PluginTaskStatus.AWAIT_CONFIRMATION : PluginTaskStatus.BLOCKED).getCode());
        task.setActivePluginId(preview.blockers().isEmpty() ? descriptor.id() : null);
        task.setRevision(0);
        task.setArchiveSha256(preview.source().sha256());
        task.setArchiveBytes(preview.source().archiveBytes());
        task.setFileName(name);
        task.setRuntimeSnapshot(preview.runtimeSnapshot());
        task.setPreviewJson(views.encode(preview));
        task.setCreateBy(actor.userId());
        task.setCreateDept(actor.deptId());
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateBy(actor.userId());
        task.setUpdateTime(task.getCreateTime());
        task.setDelFlag(0);
        return task;
    }

    private SysPluginTaskVO idempotent(SysPluginTask task, byte[] bytes) {
        if (!task.getArchiveSha256().equals(PackageDigests.sha256(bytes))) {
            throw new BusinessException(409, "同一上传请求不能替换包内容，请新建上传请求");
        }
        return views.view(task, true);
    }
}

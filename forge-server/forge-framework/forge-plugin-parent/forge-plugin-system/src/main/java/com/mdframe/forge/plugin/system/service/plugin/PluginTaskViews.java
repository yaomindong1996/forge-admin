package com.mdframe.forge.plugin.system.service.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.entity.SysPluginTask;
import com.mdframe.forge.plugin.system.vo.SysPluginPreviewVO;
import com.mdframe.forge.plugin.system.vo.SysPluginTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PluginTaskViews {
    private final ObjectMapper mapper;

    public String encode(SysPluginPreviewVO preview) {
        try {
            return mapper.writeValueAsString(preview);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法保存插件预览", exception);
        }
    }

    public SysPluginTaskVO view(SysPluginTask task, boolean detail) {
        return new SysPluginTaskVO(task.getId(), task.getPluginId(), task.getPluginName(), task.getPluginVersion(),
                task.getOperationType(), task.getTaskStatus(), task.getRevision(), task.getArchiveSha256(),
                task.getFileName(), task.getArchiveBytes(), task.getCreateTime(), task.getConfirmedTime(),
                task.getCancelledTime(), detail ? decode(task.getPreviewJson()) : null, null, List.of(), List.of());
    }

    private SysPluginPreviewVO decode(String value) {
        try {
            return mapper.readValue(value, SysPluginPreviewVO.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("插件任务预览损坏，请联系管理员", exception);
        }
    }
}

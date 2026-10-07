package com.mdframe.forge.plugin.system.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SysPluginTaskVO(String id, String pluginId, String name, String version, String operation,
                              String status, Integer revision, String sha256, String fileName,
                              int archiveBytes, LocalDateTime createdTime, LocalDateTime confirmedTime,
                              LocalDateTime cancelledTime, SysPluginPreviewVO preview,
                              PluginBuildExecutionVO execution, List<PluginTaskReviewVO> reviews) {
    public SysPluginTaskVO withExecution(PluginBuildExecutionVO build) {
        return new SysPluginTaskVO(id, pluginId, name, version, operation, status, revision, sha256, fileName,
                archiveBytes, createdTime, confirmedTime, cancelledTime, preview, build, reviews);
    }
    public SysPluginTaskVO withReviews(List<PluginTaskReviewVO> history) {
        return new SysPluginTaskVO(id, pluginId, name, version, operation, status, revision, sha256, fileName,
                archiveBytes, createdTime, confirmedTime, cancelledTime, preview, execution, history);
    }
}

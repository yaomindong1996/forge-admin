package com.mdframe.forge.plugin.system.vo;

import java.time.LocalDateTime;

public record SysPluginTaskVO(String id, String pluginId, String name, String version, String operation,
                              String status, Integer revision, String sha256, String fileName,
                              int archiveBytes, LocalDateTime createdTime, LocalDateTime confirmedTime,
                              LocalDateTime cancelledTime, SysPluginPreviewVO preview) {
}

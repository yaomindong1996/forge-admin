package com.mdframe.forge.plugin.system.vo;

import java.time.LocalDateTime;

public record PluginBuildClaimVO(String taskId, String pluginId, String version, String operation,
                                  String sha256, int archiveBytes, String status, int revision,
                                  LocalDateTime leaseExpiresTime, int leaseSeconds) {
}

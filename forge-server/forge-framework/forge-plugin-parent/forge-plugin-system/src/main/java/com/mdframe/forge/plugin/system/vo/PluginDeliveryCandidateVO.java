package com.mdframe.forge.plugin.system.vo;
public record PluginDeliveryCandidateVO(String taskId, String releaseId, String repositoryId,
                                        String pluginId, String pluginVersion) {}

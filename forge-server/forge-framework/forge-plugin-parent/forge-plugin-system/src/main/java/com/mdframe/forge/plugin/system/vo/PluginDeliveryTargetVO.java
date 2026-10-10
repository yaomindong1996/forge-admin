package com.mdframe.forge.plugin.system.vo;
public record PluginDeliveryTargetVO(String id, String name, String repositoryId, String currentReleaseId,
                                     String previousReleaseId, String activeTaskId, String unverifiedReleaseId) {}

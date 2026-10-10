package com.mdframe.forge.plugin.system.vo;
import com.mdframe.forge.plugin.system.dto.PluginArtifactMetadataDTO;
public record PluginDeliveryClaimVO(String id, String targetId, String action, String releaseId,
                                    String previousReleaseId, String status, String nonce,
                                    String lease, PluginArtifactMetadataDTO metadata, String workerId,
                                    String unverifiedReleaseId) {}

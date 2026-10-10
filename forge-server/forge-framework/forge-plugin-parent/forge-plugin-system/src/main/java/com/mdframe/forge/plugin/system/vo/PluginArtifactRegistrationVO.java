package com.mdframe.forge.plugin.system.vo;

import com.mdframe.forge.plugin.system.dto.PluginArtifactMetadataDTO;
import java.time.LocalDateTime;

public record PluginArtifactRegistrationVO(String id, String requestId, PluginArtifactMetadataDTO metadata,
                                           Long registeredBy, LocalDateTime registeredTime, String note,
                                           boolean currentApprovalMatches, String checkedAt,
                                           boolean serverArtifactBytesVerified, boolean deployed) {}

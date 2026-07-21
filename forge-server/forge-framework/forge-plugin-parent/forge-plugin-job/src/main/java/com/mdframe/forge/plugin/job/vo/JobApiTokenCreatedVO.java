package com.mdframe.forge.plugin.job.vo;

import java.time.LocalDateTime;

public record JobApiTokenCreatedVO(
        Long id,
        String token,
        String tokenPrefix,
        LocalDateTime expiresAt) {
}

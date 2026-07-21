package com.mdframe.forge.plugin.job.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class JobApiTokenVO {

    private Long id;
    private String callerName;
    private String callerDescription;
    private String tokenPrefix;
    private Set<String> scopes;
    private Set<Long> jobIds;
    private Set<String> jobGroups;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createTime;
}

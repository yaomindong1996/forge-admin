package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扩展编辑锁结果。
 */
@Data
public class BusinessExtensionLockVO {

    private Long extensionId;

    private Long holderUserId;

    private String holderUsername;

    private String lockToken;

    private LocalDateTime expireTime;
}

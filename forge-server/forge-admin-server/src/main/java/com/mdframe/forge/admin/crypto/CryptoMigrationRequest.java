package com.mdframe.forge.admin.crypto;

import lombok.Data;

import java.util.List;

/**
 * 持久化密文迁移请求。默认 dry-run，不提供明文、密文或密钥字段。
 */
@Data
public class CryptoMigrationRequest {

    private List<String> configKeys;

    private String expectedActiveKeyId;

    private Integer batchSize;

    private Boolean dryRun;

    private Boolean includeDataConnections;

    private Boolean includeLowcode;
}

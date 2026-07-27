package com.mdframe.forge.starter.crypto.migration;

import lombok.Data;

/**
 * 单个迁移来源、字段或阻塞项的无敏感值报告。
 */
@Data
public class CryptoMigrationItem {

    private String source;

    private String identifier;

    private String configKey;

    private String tableName;

    private String fieldName;

    private String keyId;

    private String format;

    private Long count;

    private String status;

    private String reason;

    public static CryptoMigrationItem count(String source,
                                            String identifier,
                                            String configKey,
                                            String tableName,
                                            String fieldName,
                                            String format,
                                            long count) {
        CryptoMigrationItem item = new CryptoMigrationItem();
        item.setSource(source);
        item.setIdentifier(identifier);
        item.setConfigKey(configKey);
        item.setTableName(tableName);
        item.setFieldName(fieldName);
        item.setFormat(format);
        item.setCount(count);
        item.setStatus("COUNT");
        return item;
    }

    public static CryptoMigrationItem blocked(String source,
                                              String identifier,
                                              String configKey,
                                              String tableName,
                                              String fieldName,
                                              String reason) {
        CryptoMigrationItem item = new CryptoMigrationItem();
        item.setSource(source);
        item.setIdentifier(identifier);
        item.setConfigKey(configKey);
        item.setTableName(tableName);
        item.setFieldName(fieldName);
        item.setFormat("BLOCKED");
        item.setCount(1L);
        item.setStatus("BLOCKED");
        item.setReason(reason);
        return item;
    }
}

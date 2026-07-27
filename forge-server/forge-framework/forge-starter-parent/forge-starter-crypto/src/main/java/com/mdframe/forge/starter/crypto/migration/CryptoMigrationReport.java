package com.mdframe.forge.starter.crypto.migration;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 持久化密文盘点与迁移报告。不得包含明文、密文、密钥或可逆摘要。
 */
@Data
public class CryptoMigrationReport {

    private Long tenantId;

    private String scope;

    private String expectedActiveKeyId;

    private String activeKeyId;

    private boolean dryRun = true;

    private Integer batchSize;

    private Map<String, Long> totals = new LinkedHashMap<>();

    private List<CryptoMigrationItem> items = new ArrayList<>();

    public static CryptoMigrationReport of(Long tenantId, String scope) {
        CryptoMigrationReport report = new CryptoMigrationReport();
        report.setTenantId(tenantId);
        report.setScope(scope);
        return report;
    }

    public void increment(String key) {
        increment(key, 1L);
    }

    public void increment(String key, long count) {
        if (count <= 0) {
            return;
        }
        totals.merge(key, count, Long::sum);
    }

    public void addItem(CryptoMigrationItem item) {
        if (item == null) {
            return;
        }
        items.add(item);
        if (item.getFormat() != null && item.getCount() != null) {
            increment(item.getFormat(), item.getCount());
        }
    }

    public void merge(CryptoMigrationReport other) {
        if (other == null) {
            return;
        }
        if (activeKeyId == null) {
            activeKeyId = other.getActiveKeyId();
        }
        other.getTotals().forEach(this::increment);
        items.addAll(other.getItems());
    }

    public boolean canRetireLegacy() {
        return count("LEGACY") == 0
                && count("HISTORICAL") == 0
                && count("UNKNOWN") == 0
                && count("UNKNOWN_KEY") == 0
                && count("BLOCKED") == 0
                && count("FAILED") == 0
                && count("CONFLICT") == 0;
    }

    private long count(String key) {
        return totals.getOrDefault(key, 0L);
    }
}

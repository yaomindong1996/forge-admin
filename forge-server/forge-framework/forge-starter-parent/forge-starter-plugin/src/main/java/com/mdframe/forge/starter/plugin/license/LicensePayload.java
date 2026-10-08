package com.mdframe.forge.starter.plugin.license;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** v1 跨工程协议；维护期限与运行使用期限刻意独立。 */
public record LicensePayload(int version, String licenseId, Binding binding, Scope scope, Terms terms) {
    public LicensePayload {
        if (version != 1 || !uuid(licenseId) || binding == null || scope == null || terms == null) {
            throw new IllegalArgumentException("非法许可证载荷");
        }
    }

    public record Binding(String customerId, String installationId) {
        public Binding {
            if (customerId == null || !customerId.matches("[1-9][0-9]{0,18}") || !uuid(installationId)) {
                throw new IllegalArgumentException("非法客户或项目安装标识");
            }
        }
    }

    public record Scope(List<String> pluginIds, List<String> featureCodes) {
        public Scope {
            pluginIds = codes(pluginIds, "[a-z][a-z0-9-]{1,63}");
            featureCodes = codes(featureCodes, "ee\\.[a-zA-Z0-9][a-zA-Z0-9_.-]{0,119}");
        }

        private static List<String> codes(List<String> values, String pattern) {
            if (values == null || values.isEmpty() || values.size() > 100
                    || new HashSet<>(values).size() != values.size()
                    || values.stream().anyMatch(value -> value == null || !value.matches(pattern))) {
                throw new IllegalArgumentException("非法授权范围");
            }
            return List.copyOf(values);
        }
    }

    /** 时间为 UTC epoch seconds；null 使用截止表示永久，不代表下载权益永久。 */
    public record Terms(long issuedAt, long notBefore, Long validUntil, Long maintenanceUntil) {
        public Terms {
            if (!time(issuedAt) || !time(notBefore) || !optionalTime(validUntil)
                    || !optionalTime(maintenanceUntil) || validUntil != null && validUntil <= notBefore) {
                throw new IllegalArgumentException("非法许可证期限");
            }
        }

        public boolean allowsUse(long now) {
            return issuedAt <= now && notBefore <= now && (validUntil == null || now < validUntil);
        }

        private static boolean time(long value) {
            return value >= 0 && value <= 253402300799L;
        }

        private static boolean optionalTime(Long value) {
            return value == null || time(value);
        }
    }

    static boolean uuid(String value) {
        try {
            return value != null && UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}

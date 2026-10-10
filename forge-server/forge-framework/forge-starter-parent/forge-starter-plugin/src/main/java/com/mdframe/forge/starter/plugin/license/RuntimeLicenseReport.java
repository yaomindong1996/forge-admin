package com.mdframe.forge.starter.plugin.license;

import java.util.List;

/** 只包含安全读视图：禁止放入路径、原始载荷、签名、密钥或未验证文件声明。 */
public record RuntimeLicenseReport(Configuration configuration, String checkedAt, String edition, List<Entry> entries) {
    public RuntimeLicenseReport {
        entries = List.copyOf(entries);
    }

    public record Configuration(LicensePayload.Binding binding, String loadedAt,
                                int trustedPublicKeys, int rejectedPublicKeys, boolean filesConfigurationValid) {
    }

    public record Entry(int position, String state, String licenseId,
                        LicensePayload.Scope scope, LicensePayload.Terms terms) {
    }
}

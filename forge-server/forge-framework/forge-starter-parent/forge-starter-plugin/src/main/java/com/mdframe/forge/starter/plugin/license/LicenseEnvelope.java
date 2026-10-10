package com.mdframe.forge.starter.plugin.license;

/** 不接收公钥 URL；keyId 只能选择宿主固定信任的公钥。 */
public record LicenseEnvelope(String algorithm, String keyId, String payload, String signature) {
    public static final String ALGORITHM = "Ed25519";

    public LicenseEnvelope {
        if (!ALGORITHM.equals(algorithm) || keyId == null || !keyId.matches("[a-zA-Z0-9_.-]{1,64}")
                || payload == null || signature == null) {
            throw new IllegalArgumentException("非法许可证封装");
        }
    }

    @Override
    public String toString() {
        return "LicenseEnvelope[redacted]";
    }
}

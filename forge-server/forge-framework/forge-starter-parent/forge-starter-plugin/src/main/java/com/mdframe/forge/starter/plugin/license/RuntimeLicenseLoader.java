package com.mdframe.forge.starter.plugin.license;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 失败只关闭对应商业授权；不打印载荷、客户标识、文件路径或异常秘钥信息。 */
public final class RuntimeLicenseLoader {
    private static final Logger LOG = LoggerFactory.getLogger(RuntimeLicenseLoader.class);

    private RuntimeLicenseLoader() {
    }

    public static RuntimeLicenseFeatureGate load(RuntimeLicenseProperties properties) {
        LicensePayload.Binding binding;
        try {
            binding = new LicensePayload.Binding(properties.getCustomerId(), properties.getInstallationId());
        } catch (IllegalArgumentException ex) {
            LOG.warn("运行时许可证绑定配置无效，商业功能关闭");
            return new RuntimeLicenseFeatureGate(null, List.of(), Clock.systemUTC());
        }
        Map<String, PublicKey> keys = keys(properties);
        List<LicensePayload> licenses = new ArrayList<>();
        var files = properties.getFiles();
        if (files == null || files.size() > 100) {
            LOG.warn("运行时许可证文件配置无效，商业功能关闭");
            return new RuntimeLicenseFeatureGate(binding, List.of(), Clock.systemUTC());
        }
        for (var file : files) {
            try {
                licenses.add(LicenseCodec.verify(LicenseCodec.readFile(file, LicenseCodec.MAX_DOCUMENT_BYTES), keys));
            } catch (Exception ex) {
                LOG.warn("一份运行时许可证无效，对应商业功能关闭");
            }
        }
        return new RuntimeLicenseFeatureGate(binding, licenses, Clock.systemUTC());
    }

    private static Map<String, PublicKey> keys(RuntimeLicenseProperties properties) {
        Map<String, PublicKey> keys = new HashMap<>();
        var paths = properties.getPublicKeys();
        if (paths == null || paths.size() > 10) {
            return keys;
        }
        paths.forEach((keyId, path) -> {
            try {
                if (keyId != null && keyId.matches("[a-zA-Z0-9_.-]{1,64}")) {
                    keys.put(keyId, LicenseCodec.publicKey(LicenseCodec.readFile(path, 4096)));
                }
            } catch (Exception ex) {
                LOG.warn("一份运行时许可证公钥无效");
            }
        });
        return keys;
    }
}

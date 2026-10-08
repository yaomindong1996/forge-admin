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
        return load(properties, Clock.systemUTC());
    }

    static RuntimeLicenseFeatureGate load(RuntimeLicenseProperties properties, Clock clock) {
        LicensePayload.Binding binding;
        try {
            binding = new LicensePayload.Binding(properties.getCustomerId(), properties.getInstallationId());
        } catch (IllegalArgumentException ex) {
            LOG.warn("运行时许可证绑定配置无效，商业功能关闭");
            return empty(null, clock, false);
        }
        Map<String, PublicKey> keys = keys(properties);
        List<LoadedRuntimeLicense> licenses = new ArrayList<>();
        var files = properties.getFiles();
        if (files == null || files.size() > 100) {
            LOG.warn("运行时许可证文件配置无效，商业功能关闭");
            return empty(binding, clock, false);
        }
        for (int index = 0; index < files.size(); index++) {
            try {
                LicensePayload payload = LicenseCodec.verify(
                        LicenseCodec.readFile(files.get(index), LicenseCodec.MAX_DOCUMENT_BYTES), keys);
                licenses.add(new LoadedRuntimeLicense(index + 1, payload));
            } catch (Exception ex) {
                licenses.add(new LoadedRuntimeLicense(index + 1, null));
                LOG.warn("一份运行时许可证无效，对应商业功能关闭");
            }
        }
        int keyCount = properties.getPublicKeys() == null ? 0 : properties.getPublicKeys().size();
        var configuration = new RuntimeLicenseReport.Configuration(binding, clock.instant().toString(),
                keys.size(), keyCount - keys.size(), true);
        return new RuntimeLicenseFeatureGate(binding, licenses, clock, configuration);
    }

    private static RuntimeLicenseFeatureGate empty(LicensePayload.Binding binding, Clock clock, boolean filesValid) {
        var configuration = new RuntimeLicenseReport.Configuration(
                binding, clock.instant().toString(), 0, 0, filesValid);
        return new RuntimeLicenseFeatureGate(binding, List.of(), clock, configuration);
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

package com.mdframe.forge.starter.plugin;

import com.mdframe.forge.starter.plugin.version.SemanticVersion;
import com.mdframe.forge.starter.plugin.version.VersionRange;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** 从本模块的构建资源读取核心版本，禁止缺资源时静默退回旧硬编码版本。 */
public final class ForgeVersion {

    private static final String RESOURCE = "/META-INF/forge/forge-version.properties";

    public static final String CURRENT = readVersion();

    private ForgeVersion() {
    }

    public static boolean satisfies(String range) {
        return VersionRange.parse(range).contains(CURRENT);
    }

    public static boolean satisfies(String version, String range) {
        return VersionRange.parse(range).contains(version);
    }

    private static String readVersion() {
        try (InputStream input = ForgeVersion.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("核心版本资源不存在：" + RESOURCE);
            }
            Properties properties = new Properties();
            properties.load(input);
            String version = properties.getProperty("version");
            SemanticVersion.parse(version);
            return version;
        } catch (IOException exception) {
            throw new IllegalStateException("读取核心版本资源失败", exception);
        }
    }
}

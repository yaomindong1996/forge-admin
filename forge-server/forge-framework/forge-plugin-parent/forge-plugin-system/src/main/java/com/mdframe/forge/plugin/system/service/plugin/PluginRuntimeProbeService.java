package com.mdframe.forge.plugin.system.service.plugin;
import com.mdframe.forge.plugin.system.config.PluginRuntimeProbeProperties;
import com.mdframe.forge.plugin.system.vo.PluginRuntimeProbeVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.plugin.ForgeVersion;
import com.mdframe.forge.starter.plugin.catalog.RuntimePluginCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
@Service @RequiredArgsConstructor
public class PluginRuntimeProbeService {
    private final PluginRuntimeProbeProperties properties;
    private final RuntimePluginCatalog catalog;
    public PluginRuntimeProbeVO probe(String nonce) {
        var plugins = catalog.getPlugins().stream().map(value -> new PluginRuntimeProbeVO.Plugin(
                value.id(), value.version(), value.serverModule() != null)).toList();
        return new PluginRuntimeProbeVO(nonce, ForgeVersion.CURRENT, runningJarDigest(), plugins);
    }
    private String runningJarDigest() {
        String file = properties.getJarPath();
        String command = System.getProperty("sun.java.command", "");
        if (file == null || !(command.equals(file) || command.startsWith(file + " "))) {
            throw new BusinessException(409, "实例不是配置的独立JAR运行方式，不能证明构建身份");
        }
        Path jar = Path.of(file);
        try {
            if (!jar.isAbsolute() || !Files.isRegularFile(jar, LinkOption.NOFOLLOW_LINKS)
                    || Files.size(jar) > 536870912L) {
                throw new BusinessException(409, "运行构建文件不可核验");
            }
            var digest = MessageDigest.getInstance("SHA-256");
            try (var stream = Files.newInputStream(jar, LinkOption.NOFOLLOW_LINKS)) {
                byte[] buffer = new byte[65536];
                int size;
                while ((size = stream.read(buffer)) != -1) { digest.update(buffer, 0, size); }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException failure) {
            throw new BusinessException(409, "运行构建文件核验失败");
        }
    }
}

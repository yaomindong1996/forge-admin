package com.mdframe.forge.plugin.system.service.plugin;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.plugin.ForgeVersion;
import com.mdframe.forge.starter.plugin.delivery.PluginZipReader;
import com.mdframe.forge.starter.plugin.delivery.SourcePluginPackage;
import com.mdframe.forge.starter.plugin.delivery.SourcePluginPackageReader;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.Semaphore;

@Component
public class PluginPackagePreflight {
    private final Semaphore slots = new Semaphore(2);

    public String fileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.length() > 128 || !name.toLowerCase(Locale.ROOT).endsWith(".zip")
                || name.matches("(?s).*[\\\\/\\x00-\\x1f\\x7f].*")) {
            throw new BusinessException(400, "请上传安全文件名的 ZIP 包");
        }
        return name;
    }

    public byte[] read(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > PluginZipReader.MAX_ARCHIVE) {
            throw new BusinessException(400, "ZIP 不能为空或超过 8 MiB");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] bytes = input.readNBytes(PluginZipReader.MAX_ARCHIVE + 1);
            if (bytes.length > PluginZipReader.MAX_ARCHIVE) {
                throw new BusinessException(400, "ZIP 超过 8 MiB");
            }
            return bytes;
        } catch (IOException exception) {
            throw new BusinessException(400, "无法读取 ZIP 包，请重新上传");
        }
    }

    public SourcePluginPackage inspect(byte[] bytes) {
        if (!slots.tryAcquire()) {
            throw new BusinessException(429, "预检繁忙，请稍后重试");
        }
        try {
            return SourcePluginPackageReader.read(bytes, ForgeVersion.CURRENT);
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw new BusinessException(400, exception.getMessage());
        } finally {
            slots.release();
        }
    }
}

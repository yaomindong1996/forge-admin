package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.service.ISysTenantService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.storage.FileStorage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 登录页租户品牌图公开访问接口。
 *
 * <p>只允许访问启用租户配置中直接引用的 logo/favicon 文件，避免放开通用文件下载接口。</p>
 */
@RestController
@RequestMapping("/auth/tenant/assets")
@RequiredArgsConstructor
@IgnoreTenant
@SaIgnore
@ApiPermissionIgnore
public class LoginTenantAssetController {

    private static final Pattern SAFE_FILE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,128}$");
    private static final Set<String> SAFE_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "ico");

    private final ISysTenantService tenantService;
    private final FileManager fileManager;

    /**
     * 获取登录页租户品牌图。
     *
     * @param tenantId  租户ID
     * @param assetType 资源类型：logo、icon、favicon
     */
    @GetMapping("/{tenantId}/{assetType}")
    public void getTenantAsset(@PathVariable Long tenantId,
                               @PathVariable String assetType,
                               HttpServletResponse response) {
        SysTenant tenant = selectEnabledTenant(tenantId);
        String assetReference = resolveAssetReference(tenant, assetType);
        String fileId = extractManagedFileId(assetReference);
        if (fileId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "租户品牌图不存在");
        }

        FileMetadata metadata = fileManager.getMetadata(fileId);
        if (metadata == null || !isSafeImage(metadata)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "租户品牌图不存在");
        }
        FileStorage storage = fileManager.getStorage(metadata.getStorageType());
        if (storage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "租户品牌图不存在");
        }

        try (InputStream inputStream = storage.download(fileId)) {
            response.setContentType(resolveContentType(metadata));
            response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=300");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename="
                    + URLEncoder.encode(resolveFileName(metadata, fileId), StandardCharsets.UTF_8));
            inputStream.transferTo(response.getOutputStream());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "租户品牌图不存在", ex);
        }
    }

    private SysTenant selectEnabledTenant(Long tenantId) {
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "租户不存在");
        }
        SysTenant tenant = tenantService.selectEnabledTenantForLogin(tenantId);
        if (tenant == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "租户不存在");
        }
        return tenant;
    }

    private String resolveAssetReference(SysTenant tenant, String assetType) {
        if ("logo".equalsIgnoreCase(assetType)) {
            return tenant.getSystemLogo();
        }
        if ("icon".equalsIgnoreCase(assetType) || "favicon".equalsIgnoreCase(assetType)) {
            return tenant.getBrowserIcon();
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "租户品牌图不存在");
    }

    private String extractManagedFileId(String assetReference) {
        if (assetReference == null || assetReference.isBlank()) {
            return null;
        }
        String value = assetReference.trim();
        String lowerValue = value.toLowerCase(Locale.ROOT);
        if (lowerValue.startsWith("http://")
                || lowerValue.startsWith("https://")
                || lowerValue.startsWith("data:")
                || lowerValue.startsWith("blob:")) {
            return null;
        }

        String fileId = extractFileIdAfterMarker(value, "/api/file/download/");
        if (fileId == null) {
            fileId = extractFileIdAfterMarker(value, "/api/file/url/");
        }
        if (fileId == null && !value.startsWith("/") && !value.contains("/") && !value.contains("\\")) {
            fileId = value;
        }
        if (fileId == null) {
            return null;
        }
        int queryIndex = fileId.indexOf('?');
        if (queryIndex >= 0) {
            fileId = fileId.substring(0, queryIndex);
        }
        int hashIndex = fileId.indexOf('#');
        if (hashIndex >= 0) {
            fileId = fileId.substring(0, hashIndex);
        }
        return SAFE_FILE_ID_PATTERN.matcher(fileId).matches() ? fileId : null;
    }

    private String extractFileIdAfterMarker(String value, String marker) {
        int index = value.indexOf(marker);
        if (index < 0) {
            return null;
        }
        String rest = value.substring(index + marker.length());
        int slashIndex = rest.indexOf('/');
        return slashIndex >= 0 ? rest.substring(0, slashIndex) : rest;
    }

    private boolean isSafeImage(FileMetadata metadata) {
        String extension = metadata.getExtension();
        if (extension != null && "svg".equalsIgnoreCase(extension)) {
            return false;
        }
        String mimeType = metadata.getMimeType();
        if (mimeType != null) {
            String normalizedMimeType = mimeType.toLowerCase(Locale.ROOT);
            if (normalizedMimeType.startsWith("image/svg+xml")) {
                return false;
            }
            if (normalizedMimeType.startsWith("image/")) {
                return true;
            }
        }
        return extension != null && SAFE_IMAGE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    private String resolveFileName(FileMetadata metadata, String fileId) {
        if (metadata.getOriginalName() != null && !metadata.getOriginalName().isBlank()) {
            return metadata.getOriginalName();
        }
        if (metadata.getStorageName() != null && !metadata.getStorageName().isBlank()) {
            return metadata.getStorageName();
        }
        return fileId;
    }

    private String resolveContentType(FileMetadata metadata) {
        String mimeType = metadata.getMimeType();
        if (mimeType != null && !mimeType.isBlank() && !"application/octet-stream".equalsIgnoreCase(mimeType)) {
            return mimeType;
        }
        String extension = metadata.getExtension();
        if (extension == null) {
            return "image/png";
        }
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "ico" -> "image/x-icon";
            default -> "image/png";
        };
    }
}

package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysFileDownloadLogCreateDTO;
import com.mdframe.forge.plugin.system.dto.SysFileDownloadLogQuery;
import com.mdframe.forge.plugin.system.entity.SysFileDownloadLog;
import com.mdframe.forge.plugin.system.mapper.SysFileDownloadLogMapper;
import com.mdframe.forge.plugin.system.support.FileDownloadLogSupport;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 下载记录写入与按身份分页。写失败不能影响文件本身的下载。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFileDownloadLogServiceImpl {

    private final SysFileDownloadLogMapper downloadLogMapper;

    public Page<SysFileDownloadLog> page(Page<SysFileDownloadLog> page, SysFileDownloadLogQuery query) {
        SysFileDownloadLogQuery safeQuery = query == null ? new SysFileDownloadLogQuery() : query;
        boolean seeAll = SessionHelper.isAdmin() || SessionHelper.isTenantAdmin();
        if (!seeAll) {
            safeQuery.setUsername(null);
        }
        Long userId = FileDownloadLogSupport.restrictedUserId(seeAll, SessionHelper.getUserId());
        return downloadLogMapper.selectDownloadLogPage(page, safeQuery, userId);
    }

    /** 直链下载补记。调用时仍在 MVC 内，租户上下文还在。 */
    public void recordExternal(SysFileDownloadLogCreateDTO dto) {
        Long userId = SessionHelper.getUserId();
        if (userId == null || dto == null) {
            return;
        }
        SysFileDownloadLog row = new SysFileDownloadLog();
        row.setUserId(userId);
        row.setUsername(limit(SessionHelper.getUsername(), 64));
        row.setFileId(limit(dto.getFileId(), 64));
        row.setFileName(limit(dto.getFileName(), 255));
        row.setFileSize(resolveSize(dto.getFileSize(), dto.getFileId()));
        row.setSource(FileDownloadLogSupport.SOURCE_FILE);
        row.setDelFlag(0);
        downloadLogMapper.insert(row);
    }

    public void record(HttpServletRequest request, String disposition, String contentLength, LoginUser actor) {
        try {
            LoginUser current = actor != null ? actor : SessionHelper.getLoginUser();
            Long resolvedUserId = current != null && current.getUserId() != null
                    ? current.getUserId() : SessionHelper.getUserId();
            Long resolvedTenantId = current != null && current.getTenantId() != null
                    ? current.getTenantId() : SessionHelper.getTenantId();
            String resolvedUsername = current != null && current.getUsername() != null
                    ? current.getUsername() : SessionHelper.getUsername();
            if (resolvedUserId == null || resolvedTenantId == null || request == null) {
                return;
            }
            // 过滤器在 MVC 结束后才写库，此时租户拦截器已清掉上下文，必须按登录租户补回去。
            TenantContextHolder.executeWithTenant(resolvedTenantId,
                    () -> insert(request, disposition, contentLength, resolvedUserId, resolvedUsername));
        } catch (Exception ex) {
            log.warn("记录文件下载失败: {}", ex.getMessage());
        }
    }

    private void insert(HttpServletRequest request, String disposition, String contentLength,
                        Long userId, String username) {
        String uri = request.getRequestURI();
        SysFileDownloadLog row = new SysFileDownloadLog();
        row.setUserId(userId);
        row.setUsername(limit(username, 64));
        row.setFileId(limit(FileDownloadLogSupport.fileIdOf(uri), 64));
        row.setFileName(FileDownloadLogSupport.fileNameOf(disposition));
        row.setFileSize(resolveSize(parseSize(contentLength), row.getFileId()));
        row.setSource(FileDownloadLogSupport.sourceOf(uri));
        row.setRequestPath(limit(uri, 255));
        row.setClientIp(limit(clientIp(request), 64));
        row.setDelFlag(0);
        downloadLogMapper.insert(row);
    }

    private Long resolveSize(Long reportedSize, String fileId) {
        if (reportedSize != null && reportedSize > 0) {
            return reportedSize;
        }
        if (fileId == null || fileId.isBlank()) {
            return reportedSize;
        }
        try {
            return downloadLogMapper.selectMetadataFileSize(fileId);
        } catch (Exception ex) {
            log.warn("读取文件大小失败: {}", ex.getMessage());
            return reportedSize;
        }
    }

    private static Long parseSize(String contentLength) {
        if (contentLength == null || contentLength.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(contentLength.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String limit(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}

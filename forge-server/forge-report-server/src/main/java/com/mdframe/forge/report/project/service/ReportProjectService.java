package com.mdframe.forge.report.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.report.project.directory.service.ReportDirectoryService;
import com.mdframe.forge.report.project.domain.ReportProject;
import com.mdframe.forge.report.project.mapper.ReportProjectMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/**
 * Go-View 项目 Service
 */
@Service
@RequiredArgsConstructor
public class ReportProjectService extends ServiceImpl<ReportProjectMapper, ReportProject> {

    private final ReportDirectoryService directoryService;
    private final ReportProjectVersionService projectVersionService;

    /**
     * 分页查询项目
     */
    public Page<ReportProject> pageProjects(Integer pageNum, Integer pageSize, String projectName, Long directoryId) {
        Page<ReportProject> page = new Page<>(pageNum, pageSize);
        List<Long> directoryIds = null;
        if (directoryId != null && directoryId > 0) {
            directoryIds = directoryService.selectDirectoryAndChildrenIds(directoryId);
            if (directoryIds.isEmpty()) {
                page.setRecords(Collections.emptyList());
                page.setTotal(0);
                return page;
            }
        }
        String keyword = StringUtils.hasText(projectName) ? projectName.trim() : null;
        return baseMapper.selectProjectPage(page, keyword, directoryIds);
    }

    /**
     * 创建项目
     */
    @Transactional(rollbackFor = Exception.class)
    public ReportProject createProject(ReportProject project) {
        if (project == null) {
            throw new BusinessException("项目参数不能为空");
        }
        directoryService.validateDirectoryExists(project.getDirectoryId());
        if (!StringUtils.hasText(project.getProjectName())) {
            project.setProjectName("新项目");
        }
        if (!StringUtils.hasText(project.getStatus())) {
            project.setStatus(EnableStatus.DISABLED.codeAsString());
        }
        if (!StringUtils.hasText(project.getPublishStatus())) {
            project.setPublishStatus(EnableStatus.DISABLED.codeAsString());
        }
        save(project);
        return project;
    }

    /**
     * 更新项目配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(ReportProject project) {
        if (project == null || project.getId() == null) {
            throw new BusinessException("项目ID不能为空");
        }
        ReportProject exists = getById(project.getId());
        if (exists == null) {
            throw new BusinessException("项目不存在");
        }
        if (project.getProjectName() != null) {
            exists.setProjectName(project.getProjectName());
        }
        if (project.getRemark() != null) {
            exists.setRemark(project.getRemark());
        }
        if (project.getIndexImg() != null) {
            exists.setIndexImg(normalizeIndexImg(project.getIndexImg()));
        }
        if (project.getStatus() != null) {
            exists.setStatus(project.getStatus());
        }
        if (project.getDirectoryId() != null && project.getDirectoryId() > 0) {
            directoryService.validateDirectoryExists(project.getDirectoryId());
            exists.setDirectoryId(project.getDirectoryId());
        }
        if (project.getCanvasWidth() != null) {
            exists.setCanvasWidth(project.getCanvasWidth());
        }
        if (project.getCanvasHeight() != null) {
            exists.setCanvasHeight(project.getCanvasHeight());
        }
        if (project.getBackgroundColor() != null) {
            exists.setBackgroundColor(project.getBackgroundColor());
        }
        if (project.getComponentData() != null) {
            exists.setComponentData(project.getComponentData());
        }

        boolean updated = updateById(exists);
        if (!updated) {
            throw new BusinessException("项目配置保存失败，请检查租户或项目状态");
        }
    }

    /**
     * 发布项目
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishProject(Long id, String publishUrl) {
        ReportProject project = getById(id);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        project.setPublishStatus(EnableStatus.ENABLED.codeAsString());
        project.setPublishUrl(publishUrl);
        project.setPublishTime(new Date());
        boolean updated = updateById(project);
        if (!updated) {
            throw new BusinessException("项目发布状态保存失败，请检查租户或项目状态");
        }
        projectVersionService.createPublishVersion(project);
    }

    private String normalizeIndexImg(String indexImg) {
        String value = indexImg == null ? "" : indexImg.trim();
        if (!StringUtils.hasText(value)) {
            return value;
        }

        if (value.startsWith("forge-file://")) {
            value = value.substring("forge-file://".length()).trim();
        }

        String directFileId = extractDownloadFileId(value);
        if (StringUtils.hasText(directFileId)) {
            return directFileId;
        }

        if (!isUrlLike(value)) {
            return value;
        }

        String objectKey = extractObjectKey(value);
        String fileId = baseMapper.selectFileIdByImageReference(value, objectKey);
        if (StringUtils.hasText(fileId)) {
            return fileId;
        }

        throw new BusinessException("项目截图保存失败，无法根据图片路径匹配文件ID");
    }

    private boolean isUrlLike(String value) {
        return value.startsWith("http://")
                || value.startsWith("https://")
                || value.startsWith("/api/file/")
                || value.startsWith("/forge-report-api/api/file/");
    }

    private String extractDownloadFileId(String value) {
        int downloadIndex = value.indexOf("/api/file/download/");
        if (downloadIndex < 0) {
            return null;
        }
        String fileId = value.substring(downloadIndex + "/api/file/download/".length());
        int queryIndex = fileId.indexOf('?');
        if (queryIndex >= 0) {
            fileId = fileId.substring(0, queryIndex);
        }
        int slashIndex = fileId.indexOf('/');
        if (slashIndex >= 0) {
            fileId = fileId.substring(0, slashIndex);
        }
        return URLDecoder.decode(fileId, StandardCharsets.UTF_8).trim();
    }

    private String extractObjectKey(String value) {
        try {
            URI uri = URI.create(value);
            String path = uri.getPath();
            if (!StringUtils.hasText(path)) {
                return null;
            }
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            return URLDecoder.decode(path, StandardCharsets.UTF_8).trim();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

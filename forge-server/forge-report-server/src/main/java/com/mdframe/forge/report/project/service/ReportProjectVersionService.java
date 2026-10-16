package com.mdframe.forge.report.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.report.project.domain.ReportProject;
import com.mdframe.forge.report.project.domain.ReportProjectVersion;
import com.mdframe.forge.report.project.mapper.ReportProjectMapper;
import com.mdframe.forge.report.project.mapper.ReportProjectVersionMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/**
 * 大屏项目版本服务。
 */
@Service
@RequiredArgsConstructor
public class ReportProjectVersionService extends ServiceImpl<ReportProjectVersionMapper, ReportProjectVersion> {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String OPERATION_PUBLISH = "publish";
    private static final String OPERATION_ROLLBACK = "rollback";

    private final ReportProjectVersionMapper versionMapper;
    private final ReportProjectMapper projectMapper;

    public Page<ReportProjectVersion> pageVersions(Long projectId, Integer pageNum, Integer pageSize) {
        if (projectId == null || projectId <= 0) {
            throw new BusinessException("项目ID不能为空");
        }
        Page<ReportProjectVersion> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return versionMapper.selectVersionPage(page, SessionHelper.getTenantId(), projectId);
    }

    public ReportProjectVersion getVersionDetail(Long versionId) {
        if (versionId == null || versionId <= 0) {
            throw new BusinessException("版本ID不能为空");
        }
        ReportProjectVersion version = getById(versionId);
        if (version == null) {
            throw new BusinessException("版本不存在");
        }
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId != null && version.getTenantId() != null && !tenantId.equals(version.getTenantId())) {
            throw new BusinessException("版本不存在或无权访问");
        }
        return version;
    }

    @Transactional(rollbackFor = Exception.class)
    public ReportProjectVersion createPublishVersion(ReportProject project) {
        return createVersion(project, OPERATION_PUBLISH, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReportProjectVersion rollbackVersion(Long versionId) {
        ReportProjectVersion sourceVersion = getVersionDetail(versionId);
        ReportProject project = projectMapper.selectById(sourceVersion.getProjectId());
        if (project == null) {
            throw new BusinessException("项目不存在");
        }

        project.setProjectName(sourceVersion.getProjectName());
        project.setRemark(sourceVersion.getRemark());
        project.setIndexImg(sourceVersion.getIndexImg());
        project.setCanvasWidth(sourceVersion.getCanvasWidth());
        project.setCanvasHeight(sourceVersion.getCanvasHeight());
        project.setBackgroundColor(sourceVersion.getBackgroundColor());
        project.setComponentData(sourceVersion.getComponentData());
        project.setPublishStatus(EnableStatus.ENABLED.codeAsString());
        project.setPublishUrl(sourceVersion.getPublishUrl());
        project.setPublishTime(new Date());

        int updated = projectMapper.updateById(project);
        if (updated <= 0) {
            throw new BusinessException("版本回退失败，请检查租户或项目状态");
        }

        return createVersion(project, OPERATION_ROLLBACK, sourceVersion.getId());
    }

    private ReportProjectVersion createVersion(ReportProject project, String operationType, Long sourceVersionId) {
        if (project == null || project.getId() == null) {
            throw new BusinessException("项目ID不能为空");
        }
        if (!StringUtils.hasText(project.getComponentData())) {
            throw new BusinessException("当前项目暂无可发布配置");
        }

        Long tenantId = project.getTenantId() != null ? project.getTenantId() : SessionHelper.getTenantId();
        Integer currentMaxVersionNo = versionMapper.selectMaxVersionNo(tenantId, project.getId());
        int nextVersionNo = currentMaxVersionNo == null ? 1 : currentMaxVersionNo + 1;
        LoginUser loginUser = SessionHelper.getLoginUser();

        ReportProjectVersion version = new ReportProjectVersion();
        version.setTenantId(tenantId);
        version.setProjectId(project.getId());
        version.setProjectName(project.getProjectName());
        version.setVersionNo(nextVersionNo);
        version.setVersionName("V" + nextVersionNo);
        version.setOperationType(StringUtils.hasText(operationType) ? operationType : OPERATION_PUBLISH);
        version.setSourceVersionId(sourceVersionId);
        version.setPublisherId(loginUser != null ? loginUser.getUserId() : SessionHelper.getUserId());
        version.setPublisherName(resolvePublisherName(loginUser));
        version.setPublishTime(project.getPublishTime() != null ? project.getPublishTime() : new Date());
        version.setPublishUrl(project.getPublishUrl());
        version.setIndexImg(project.getIndexImg());
        version.setCanvasWidth(project.getCanvasWidth());
        version.setCanvasHeight(project.getCanvasHeight());
        version.setBackgroundColor(project.getBackgroundColor());
        version.setComponentData(project.getComponentData());
        version.setRemark(project.getRemark());
        save(version);
        return version;
    }

    private String resolvePublisherName(LoginUser loginUser) {
        if (loginUser == null) {
            return null;
        }
        if (StringUtils.hasText(loginUser.getRealName())) {
            return loginUser.getRealName();
        }
        return loginUser.getUsername();
    }

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? 1L : pageNum;
    }

    private long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}

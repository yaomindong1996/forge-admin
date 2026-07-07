package com.mdframe.forge.report.project.template.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.report.project.directory.domain.ReportDirectory;
import com.mdframe.forge.report.project.directory.service.ReportDirectoryService;
import com.mdframe.forge.report.project.domain.ReportProject;
import com.mdframe.forge.report.project.service.ReportProjectService;
import com.mdframe.forge.report.project.template.domain.ReportTemplate;
import com.mdframe.forge.report.project.template.dto.TemplateCopyDTO;
import com.mdframe.forge.report.project.template.mapper.ReportTemplateMapper;
import com.mdframe.forge.report.project.template.vo.TemplateCopyResultVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * Go-View 模板 Service
 */
@Service
@RequiredArgsConstructor
public class ReportTemplateService extends ServiceImpl<ReportTemplateMapper, ReportTemplate> {

    private final ReportProjectService projectService;
    private final ReportDirectoryService directoryService;

    /**
     * 分页查询我的模板
     */
    public Page<ReportTemplate> pageMyTemplates(Integer pageNum, Integer pageSize, String templateName, String publishStatus) {
        Long ownerId = SessionHelper.getUserId();
        if (ownerId == null) {
            throw new BusinessException("用户未登录");
        }
        return baseMapper.selectMyTemplatePage(new Page<>(pageNum, pageSize),
                normalizeText(templateName),
                normalizeText(publishStatus),
                ownerId);
    }

    /**
     * 分页查询模板市场
     */
    public Page<ReportTemplate> pageTemplateMarket(Integer pageNum, Integer pageSize, String templateName) {
        return baseMapper.selectTemplateMarketPage(new Page<>(pageNum, pageSize), normalizeText(templateName));
    }

    /**
     * 查询模板详情
     */
    public ReportTemplate getTemplateDetail(Long id) {
        ReportTemplate template = baseMapper.selectTemplateDetail(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        return template;
    }

    /**
     * 从项目创建模板
     */
    @Transactional(rollbackFor = Exception.class)
    public ReportTemplate createTemplateFromProject(ReportTemplate request) {
        if (request == null || request.getSourceProjectId() == null) {
            throw new BusinessException("来源项目ID不能为空");
        }

        ReportProject sourceProject = projectService.getById(request.getSourceProjectId());
        if (sourceProject == null) {
            throw new BusinessException("来源项目不存在");
        }

        ReportTemplate stored = baseMapper.selectTemplateByIdIncludingDeleted(sourceProject.getId());
        boolean exists = stored != null;
        if (!exists) {
            stored = new ReportTemplate();
            stored.setId(sourceProject.getId());
            stored.setSourceProjectId(sourceProject.getId());
            stored.setPublishStatus("0");
            stored.setTemplateScope("0");
            stored.setCopiedCount(0);
        }

        mergeTemplateFromProject(stored, request, sourceProject);
        stored.setOwnerName(null);

        if (!exists) {
            save(stored);
        } else {
            if ("1".equals(stored.getDelFlag())) {
                int restored = baseMapper.restoreTemplateById(stored.getId());
                if (restored <= 0) {
                    throw new BusinessException("模板恢复失败");
                }
                stored.setDelFlag("0");
            }
            updateById(stored);
        }
        return stored;
    }

    /**
     * 更新模板
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(ReportTemplate request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException("模板ID不能为空");
        }
        ReportTemplate stored = getById(request.getId());
        if (stored == null) {
            throw new BusinessException("模板不存在");
        }
        if (request.getTemplateName() != null) {
            if (!StringUtils.hasText(request.getTemplateName())) {
                throw new BusinessException("模板名称不能为空");
            }
            stored.setTemplateName(request.getTemplateName().trim());
        }
        if (request.getRemark() != null) {
            stored.setRemark(request.getRemark().trim());
        }
        if (request.getIndexImg() != null) {
            stored.setIndexImg(request.getIndexImg().trim());
        }
        if (request.getStatus() != null) {
            stored.setStatus(request.getStatus().trim());
        }
        if (request.getDirectoryId() != null && request.getDirectoryId() > 0) {
            directoryService.validateDirectoryExists(request.getDirectoryId());
            stored.setDirectoryId(request.getDirectoryId());
        }
        if (request.getCanvasWidth() != null) {
            stored.setCanvasWidth(request.getCanvasWidth());
        }
        if (request.getCanvasHeight() != null) {
            stored.setCanvasHeight(request.getCanvasHeight());
        }
        if (request.getBackgroundColor() != null) {
            stored.setBackgroundColor(request.getBackgroundColor().trim());
        }
        if (request.getComponentData() != null) {
            stored.setComponentData(request.getComponentData());
        }
        if (request.getTemplateScope() != null) {
            applyTemplateScope(stored, request.getTemplateScope(), stored.getPublishUrl());
        }
        if (!updateById(stored)) {
            throw new BusinessException("模板保存失败");
        }
    }

    /**
     * 删除模板
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        ReportTemplate stored = getById(id);
        if (stored == null) {
            throw new BusinessException("模板不存在");
        }
        if (!removeById(id)) {
            throw new BusinessException("模板删除失败");
        }
    }

    /**
     * 发布模板到市场
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishTemplate(Long id, String publishUrl) {
        ReportTemplate template = getById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (!StringUtils.hasText(template.getComponentData())) {
            throw new BusinessException("模板内容不能为空");
        }
        applyTemplateScope(template, "1", publishUrl);
        if (!updateById(template)) {
            throw new BusinessException("模板发布失败");
        }
    }

    /**
     * 基于模板复制新项目
     */
    @Transactional(rollbackFor = Exception.class)
    public TemplateCopyResultVO copyToProject(TemplateCopyDTO copyDTO) {
        if (copyDTO == null || copyDTO.getTemplateId() == null) {
            throw new BusinessException("模板ID不能为空");
        }
        ReportTemplate template = getById(copyDTO.getTemplateId());
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        ReportProject sourceProject = null;
        if (template.getSourceProjectId() != null) {
            sourceProject = projectService.getById(template.getSourceProjectId());
        }

        String componentData = firstText(template.getComponentData(),
                sourceProject != null ? sourceProject.getComponentData() : null);
        if (!StringUtils.hasText(componentData)) {
            throw new BusinessException("模板内容不能为空");
        }

        ReportProject project = new ReportProject();
        project.setDirectoryId(resolveTargetDirectoryId(template, sourceProject));
        project.setProjectName(firstText(copyDTO.getProjectName(), template.getTemplateName(), "新项目"));
        project.setRemark(firstText(template.getRemark(), sourceProject != null ? sourceProject.getRemark() : null));
        project.setIndexImg(firstText(template.getIndexImg(), sourceProject != null ? sourceProject.getIndexImg() : null));
        project.setStatus("0");
        project.setCanvasWidth(firstPositive(template.getCanvasWidth(),
                sourceProject != null ? sourceProject.getCanvasWidth() : null, 1920));
        project.setCanvasHeight(firstPositive(template.getCanvasHeight(),
                sourceProject != null ? sourceProject.getCanvasHeight() : null, 1080));
        project.setBackgroundColor(firstText(template.getBackgroundColor(),
                sourceProject != null ? sourceProject.getBackgroundColor() : null, ""));
        project.setComponentData(componentData);
        project.setPublishStatus("0");

        ReportProject created = projectService.createProject(project);
        template.setCopiedCount((template.getCopiedCount() == null ? 0 : template.getCopiedCount()) + 1);
        updateById(template);
        return new TemplateCopyResultVO(created.getId());
    }

    private void mergeTemplateFromProject(ReportTemplate stored, ReportTemplate request, ReportProject sourceProject) {
        stored.setId(sourceProject.getId());
        stored.setSourceProjectId(sourceProject.getId());
        stored.setDirectoryId(firstPositive(request.getDirectoryId(), sourceProject.getDirectoryId(), stored.getDirectoryId()));
        stored.setTemplateName(firstText(request.getTemplateName(), sourceProject.getProjectName(), stored.getTemplateName(), "未命名模板"));
        stored.setRemark(firstText(request.getRemark(), sourceProject.getRemark(), stored.getRemark()));
        stored.setIndexImg(firstText(request.getIndexImg(), sourceProject.getIndexImg(), stored.getIndexImg()));
        stored.setStatus(firstText(request.getStatus(), stored.getStatus(), "0"));
        stored.setCanvasWidth(firstPositive(request.getCanvasWidth(), sourceProject.getCanvasWidth(), stored.getCanvasWidth(), 1920));
        stored.setCanvasHeight(firstPositive(request.getCanvasHeight(), sourceProject.getCanvasHeight(), stored.getCanvasHeight(), 1080));
        stored.setBackgroundColor(firstText(request.getBackgroundColor(), sourceProject.getBackgroundColor(), stored.getBackgroundColor(), ""));
        String componentData = firstText(request.getComponentData(), sourceProject.getComponentData(), stored.getComponentData());
        if (!StringUtils.hasText(componentData)) {
            throw new BusinessException("模板内容不能为空");
        }
        stored.setComponentData(componentData);
        if (!StringUtils.hasText(stored.getPublishStatus())) {
            stored.setPublishStatus("0");
        }
        if (!StringUtils.hasText(stored.getTemplateScope())) {
            stored.setTemplateScope("0");
        }
        if (stored.getCopiedCount() == null) {
            stored.setCopiedCount(0);
        }
    }

    private void applyTemplateScope(ReportTemplate template, String templateScope, String publishUrl) {
        String normalizedScope = "1".equals(normalizeText(templateScope)) ? "1" : "0";
        template.setTemplateScope(normalizedScope);
        template.setPublishStatus(normalizedScope);
        if ("1".equals(normalizedScope)) {
            if (publishUrl != null) {
                template.setPublishUrl(publishUrl.trim());
            }
            if (template.getPublishTime() == null) {
                template.setPublishTime(new Date());
            }
        } else {
            template.setPublishUrl(null);
            template.setPublishTime(null);
        }
    }

    private Long resolveTargetDirectoryId(ReportTemplate template, ReportProject sourceProject) {
        if (directoryExists(template.getDirectoryId())) {
            return template.getDirectoryId();
        }
        if (sourceProject != null && directoryExists(sourceProject.getDirectoryId())) {
            return sourceProject.getDirectoryId();
        }
        Long fallbackDirectoryId = findFirstDirectoryId(directoryService.listTree());
        if (fallbackDirectoryId != null) {
            return fallbackDirectoryId;
        }
        throw new BusinessException("当前租户下暂无可用目录，请先创建目录");
    }

    private boolean directoryExists(Long directoryId) {
        return directoryId != null && directoryId > 0 && directoryService.getById(directoryId) != null;
    }

    private Long findFirstDirectoryId(List<ReportDirectory> directories) {
        if (CollectionUtils.isEmpty(directories)) {
            return null;
        }
        for (ReportDirectory directory : directories) {
            if (directory.getId() != null) {
                return directory.getId();
            }
            Long childId = findFirstDirectoryId(directory.getChildren());
            if (childId != null) {
                return childId;
            }
        }
        return null;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private Integer firstPositive(Integer... values) {
        if (values == null) {
            return null;
        }
        for (Integer value : values) {
            if (value != null && value > 0) {
                return value;
            }
        }
        return null;
    }

    private Long firstPositive(Long... values) {
        if (values == null) {
            return null;
        }
        for (Long value : values) {
            if (value != null && value > 0) {
                return value;
            }
        }
        return null;
    }
}

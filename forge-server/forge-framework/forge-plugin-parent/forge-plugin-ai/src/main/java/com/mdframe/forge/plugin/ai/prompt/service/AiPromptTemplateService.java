package com.mdframe.forge.plugin.ai.prompt.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.prompt.domain.AiPromptTemplate;
import com.mdframe.forge.plugin.ai.prompt.dto.AiPromptTemplateQuery;
import com.mdframe.forge.plugin.ai.prompt.mapper.AiPromptTemplateMapper;
import com.mdframe.forge.plugin.ai.prompt.vo.AiPromptTemplateVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiPromptTemplateService extends ServiceImpl<AiPromptTemplateMapper, AiPromptTemplate> {

    public static final String SCENE_DASHBOARD_GENERATE = "dashboard_generate";
    public static final String STATUS_ENABLED = "0";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_LIST_LIMIT = 50;
    private static final int MAX_LIST_LIMIT = 100;

    private final AiPromptTemplateMapper promptTemplateMapper;

    public Page<AiPromptTemplateVO> pageForAdmin(Integer pageNum, Integer pageSize, AiPromptTemplateQuery query) {
        Page<AiPromptTemplateVO> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return promptTemplateMapper.selectAdminPage(page, currentTenantId(), normalizeQuery(query));
    }

    public List<AiPromptTemplateVO> listEnabled(AiPromptTemplateQuery query) {
        AiPromptTemplateQuery safeQuery = normalizeQuery(query);
        Integer limit = normalizeLimit(safeQuery.getLimit());
        return promptTemplateMapper.selectEnabledList(currentTenantId(), safeQuery, limit);
    }

    public AiPromptTemplate getDetail(Long id) {
        AiPromptTemplate template = promptTemplateMapper.selectDetail(currentTenantId(), normalizeId(id));
        if (template == null) {
            throw new BusinessException("提示词模板不存在");
        }
        return template;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createTemplate(AiPromptTemplate template) {
        if (template == null) {
            throw new BusinessException("提示词模板不能为空");
        }
        normalizeTemplateForSave(template, false);
        ensureTemplateCodeUnique(template.getTemplateCode(), null);
        save(template);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(AiPromptTemplate template) {
        if (template == null || template.getId() == null) {
            throw new BusinessException("提示词模板ID不能为空");
        }
        AiPromptTemplate existing = getDetail(template.getId());
        normalizeTemplateForSave(template, true);
        template.setTenantId(existing.getTenantId());
        clearSystemManagedFields(template);
        ensureTemplateCodeUnique(template.getTemplateCode(), template.getId());
        updateById(template);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        int deleted = promptTemplateMapper.deleteByTenantId(currentTenantId(), normalizeId(id));
        if (deleted <= 0) {
            throw new BusinessException("提示词模板不存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public AiPromptTemplate useTemplate(Long id) {
        incrementCounter(id, "use");
        return getDetail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public AiPromptTemplate testTemplate(Long id) {
        incrementCounter(id, "test");
        return getDetail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public AiPromptTemplate downloadTemplate(Long id) {
        incrementCounter(id, "download");
        return getDetail(id);
    }

    private void incrementCounter(Long id, String counterType) {
        Long tenantId = currentTenantId();
        Long templateId = normalizeId(id);
        int updated;
        if ("use".equals(counterType)) {
            updated = promptTemplateMapper.incrementUseCount(tenantId, templateId);
        } else if ("test".equals(counterType)) {
            updated = promptTemplateMapper.incrementTestCount(tenantId, templateId);
        } else if ("download".equals(counterType)) {
            updated = promptTemplateMapper.incrementDownloadCount(tenantId, templateId);
        } else {
            throw new BusinessException("不支持的计数类型");
        }
        if (updated <= 0) {
            throw new BusinessException("提示词模板不存在或已停用");
        }
    }

    private void normalizeTemplateForSave(AiPromptTemplate template, boolean update) {
        if (!StringUtils.hasText(template.getTemplateName())) {
            throw new BusinessException("模板名称不能为空");
        }
        if (!StringUtils.hasText(template.getPromptContent())) {
            throw new BusinessException("提示词内容不能为空");
        }
        template.setTenantId(currentTenantId());
        template.setTemplateName(trim(template.getTemplateName(), 128));
        template.setTemplateCode(trimToNull(template.getTemplateCode(), 80));
        template.setUsageScene(StringUtils.hasText(template.getUsageScene())
                ? trim(template.getUsageScene(), 64)
                : SCENE_DASHBOARD_GENERATE);
        template.setBusinessCategory(trimToNull(template.getBusinessCategory(), 64));
        template.setDomainCategory(trimToNull(template.getDomainCategory(), 64));
        template.setTemplateTags(trimToNull(template.getTemplateTags(), 255));
        template.setDescription(trimToNull(template.getDescription(), 500));
        template.setExampleInput(trimToNull(template.getExampleInput(), 2000));
        template.setPromptContent(template.getPromptContent().trim());
        template.setStatus(StringUtils.hasText(template.getStatus()) ? trim(template.getStatus(), 1) : STATUS_ENABLED);
        template.setIsRecommended(StringUtils.hasText(template.getIsRecommended()) ? trim(template.getIsRecommended(), 1) : "0");
        template.setSortOrder(template.getSortOrder() == null ? 0 : template.getSortOrder());
        template.setRemark(trimToNull(template.getRemark(), 500));
        if (!update) {
            template.setId(null);
            template.setUseCount(nonNegative(template.getUseCount()));
            template.setTestCount(nonNegative(template.getTestCount()));
            template.setDownloadCount(nonNegative(template.getDownloadCount()));
        }
    }

    private void clearSystemManagedFields(AiPromptTemplate template) {
        template.setUseCount(null);
        template.setTestCount(null);
        template.setDownloadCount(null);
        template.setCreateBy(null);
        template.setCreateTime(null);
        template.setCreateDept(null);
        template.setUpdateBy(null);
        template.setUpdateTime(null);
    }

    private void ensureTemplateCodeUnique(String templateCode, Long excludeId) {
        if (!StringUtils.hasText(templateCode)) {
            return;
        }
        int count = promptTemplateMapper.countByCode(currentTenantId(), templateCode, excludeId);
        if (count > 0) {
            throw new BusinessException("模板编码已存在: " + templateCode);
        }
    }

    private AiPromptTemplateQuery normalizeQuery(AiPromptTemplateQuery query) {
        return query == null ? new AiPromptTemplateQuery() : query;
    }

    private Long currentTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private Long normalizeId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("提示词模板ID不能为空");
        }
        return id;
    }

    private Long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? 1L : pageNum.longValue();
    }

    private Long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return (long) DEFAULT_PAGE_SIZE;
        }
        return (long) Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private Integer normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIST_LIMIT;
        }
        return Math.min(limit, MAX_LIST_LIMIT);
    }

    private Integer nonNegative(Integer value) {
        return value != null && value >= 0 ? value : 0;
    }

    private String trim(String value, int maxLength) {
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private String trimToNull(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return trim(value, maxLength);
    }
}

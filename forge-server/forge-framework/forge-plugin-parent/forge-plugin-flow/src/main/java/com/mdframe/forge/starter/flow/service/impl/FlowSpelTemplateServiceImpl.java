package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.entity.FlowSpelTemplate;
import com.mdframe.forge.starter.flow.enums.FlowEnableStatus;
import com.mdframe.forge.starter.flow.mapper.FlowSpelTemplateMapper;
import com.mdframe.forge.starter.flow.service.FlowSpelTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

import java.util.List;

/**
 * SPEL表达式模板服务实现
 *
 * @author forge
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowSpelTemplateServiceImpl extends ServiceImpl<FlowSpelTemplateMapper, FlowSpelTemplate> implements FlowSpelTemplateService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_EXPRESSION_LENGTH = 2000;

    @Override
    public Page<FlowSpelTemplate> getPage(String templateName, String category, Integer status, Integer page, Integer pageSize) {
        Long tenantId = requireTenantId();
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        Page<FlowSpelTemplate> result = new Page<>(safePage, safePageSize);
        baseMapper.selectTenantPage(result, tenantId, trimToNull(templateName), trimToNull(category), status);
        return result;
    }

    @Override
    public List<FlowSpelTemplate> getEnabledList() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            log.warn("加载流程表达式模板时缺少可信租户上下文");
            return List.of();
        }
        return baseMapper.selectEnabledList(tenantId);
    }

    @Override
    public FlowSpelTemplate getByIdForCurrentTenant(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, "模板ID不合法");
        }
        return baseMapper.selectByIdAndTenant(id, requireTenantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTemplate(FlowSpelTemplate template) {
        Long tenantId = requireTenantId();
        validate(template, true);
        if (baseMapper.countByCodeAndTenant(template.getTemplateCode(), tenantId, null) > 0) {
            throw new BusinessException(409, "模板编码已存在");
        }
        template.setTenantId(tenantId);
        template.setCreateBy(SessionHelper.getUserId());
        if (template.getStatus() == null) {
            template.setStatus(FlowEnableStatus.ENABLED.getCode());
        }
        if (template.getSort() == null) {
            template.setSort(100);
        }
        
        return save(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTemplate(FlowSpelTemplate template) {
        Long tenantId = requireTenantId();
        validate(template, false);
        FlowSpelTemplate existing = baseMapper.selectByIdAndTenant(template.getId(), tenantId);
        if (existing == null) {
            throw new BusinessException(404, "模板不存在");
        }
        
        // 不允许修改templateCode
        template.setTemplateCode(existing.getTemplateCode());
        if (template.getStatus() == null) {
            template.setStatus(existing.getStatus());
        }
        if (template.getSort() == null) {
            template.setSort(existing.getSort());
        }
        
        template.setTenantId(tenantId);
        template.setUpdateBy(SessionHelper.getUserId());
        template.setUpdateTime(LocalDateTime.now());
        if (baseMapper.countByCodeAndTenant(existing.getTemplateCode(), tenantId, template.getId()) > 0) {
            throw new BusinessException(409, "模板编码已存在");
        }
        return baseMapper.updateByIdAndTenant(template, tenantId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTemplate(Long id) {
        return baseMapper.logicallyDeleteByIdAndTenant(id, requireTenantId()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableTemplate(Long id) {
        Long tenantId = requireTenantId();
        FlowSpelTemplate template = baseMapper.selectByIdAndTenant(id, tenantId);
        if (template == null) {
            throw new BusinessException(404, "模板不存在");
        }
        return baseMapper.updateStatusByIdAndTenant(id, tenantId, FlowEnableStatus.ENABLED.getCode()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableTemplate(Long id) {
        Long tenantId = requireTenantId();
        FlowSpelTemplate template = baseMapper.selectByIdAndTenant(id, tenantId);
        if (template == null) {
            throw new BusinessException(404, "模板不存在");
        }
        return baseMapper.updateStatusByIdAndTenant(id, tenantId, FlowEnableStatus.DISABLED.getCode()) > 0;
    }

    private Long requireTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException(403, "无法确定当前租户，禁止管理流程表达式模板");
        }
        return tenantId;
    }

    private void validate(FlowSpelTemplate template, boolean requireCode) {
        if (template == null || template.getId() != null && template.getId() <= 0) {
            throw new BusinessException(400, "表达式模板参数不合法");
        }
        if (!StringUtils.hasText(template.getTemplateName()) || template.getTemplateName().length() > 100) {
            throw new BusinessException(400, "模板名称不能为空且长度不能超过100");
        }
        if (requireCode && (!StringUtils.hasText(template.getTemplateCode()) || template.getTemplateCode().length() > 100)) {
            throw new BusinessException(400, "模板编码不能为空且长度不能超过100");
        }
        if (!StringUtils.hasText(template.getExpression()) || template.getExpression().length() > MAX_EXPRESSION_LENGTH) {
            throw new BusinessException(400, "表达式不能为空且长度不能超过" + MAX_EXPRESSION_LENGTH);
        }
        if (template.getCategory() != null && template.getCategory().length() > 50) {
            throw new BusinessException(400, "模板分类长度不能超过50");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}

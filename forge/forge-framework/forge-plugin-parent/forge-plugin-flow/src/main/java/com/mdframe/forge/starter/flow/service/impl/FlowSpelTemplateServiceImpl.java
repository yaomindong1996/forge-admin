package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.flow.entity.FlowSpelTemplate;
import com.mdframe.forge.starter.flow.mapper.FlowSpelTemplateMapper;
import com.mdframe.forge.starter.flow.service.FlowSpelTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Override
    public Page<FlowSpelTemplate> getPage(String templateName, String category, Integer status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<FlowSpelTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(templateName), FlowSpelTemplate::getTemplateName, templateName)
                .eq(StringUtils.hasText(category), FlowSpelTemplate::getCategory, category)
                .eq(status != null, FlowSpelTemplate::getStatus, status)
                .orderByAsc(FlowSpelTemplate::getSort)
                .orderByDesc(FlowSpelTemplate::getCreateTime);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public List<FlowSpelTemplate> getEnabledList() {
        // 获取当前租户ID（从上下文或默认值）
        Long tenantId = 1L;
        return baseMapper.selectEnabledList(tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTemplate(FlowSpelTemplate template) {
        // 检查编码是否存在
        LambdaQueryWrapper<FlowSpelTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowSpelTemplate::getTemplateCode, template.getTemplateCode());
        if (exists(wrapper)) {
            throw new RuntimeException("模板编码已存在: " + template.getTemplateCode());
        }
        
        // 设置初始值
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        if (template.getSort() == null) {
            template.setSort(100);
        }
        
        return save(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTemplate(FlowSpelTemplate template) {
        FlowSpelTemplate existing = getById(template.getId());
        if (existing == null) {
            throw new RuntimeException("模板不存在");
        }
        
        // 不允许修改templateCode
        template.setTemplateCode(existing.getTemplateCode());
        
        return updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTemplate(Long id) {
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableTemplate(Long id) {
        FlowSpelTemplate template = getById(id);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        template.setStatus(1);
        return updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableTemplate(Long id) {
        FlowSpelTemplate template = getById(id);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        template.setStatus(0);
        return updateById(template);
    }
}
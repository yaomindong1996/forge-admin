package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowTemplate;
import com.mdframe.forge.starter.flow.mapper.FlowTemplateMapper;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import com.mdframe.forge.starter.flow.service.FlowTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 流程模板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowTemplateServiceImpl extends ServiceImpl<FlowTemplateMapper, FlowTemplate> implements FlowTemplateService {

    @Autowired(required = false)
    private FlowModelService flowModelService;

    @Override
    public IPage<FlowTemplate> pageTemplate(Page<FlowTemplate> page, String templateName, String category, Integer status) {
        LambdaQueryWrapper<FlowTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(templateName != null && !templateName.isEmpty(), FlowTemplate::getTemplateName, templateName)
                .eq(category != null && !category.isEmpty(), FlowTemplate::getCategory, category)
                .eq(status != null, FlowTemplate::getStatus, status)
                .orderByAsc(FlowTemplate::getSortOrder)
                .orderByDesc(FlowTemplate::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<FlowTemplate> getEnabledTemplates(String category) {
        LambdaQueryWrapper<FlowTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowTemplate::getStatus, 1)
                .eq(category != null && !category.isEmpty(), FlowTemplate::getCategory, category)
                .orderByAsc(FlowTemplate::getSortOrder);
        return list(wrapper);
    }

    @Override
    public FlowTemplate getTemplateDetail(String id) {
        return getById(id);
    }

    @Override
    public FlowTemplate getTemplateByKey(String templateKey) {
        LambdaQueryWrapper<FlowTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowTemplate::getTemplateKey, templateKey);
        return getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowTemplate createTemplate(FlowTemplate template) {
        // 生成模板Key
        if (template.getTemplateKey() == null || template.getTemplateKey().isEmpty()) {
            template.setTemplateKey("template_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        }
        
        // 设置默认值
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        if (template.getVersion() == null) {
            template.setVersion(1);
        }
        if (template.getUsageCount() == null) {
            template.setUsageCount(0);
        }
        if (template.getIsSystem() == null) {
            template.setIsSystem(0);
        }
        if (template.getSortOrder() == null) {
            template.setSortOrder(0);
        }
        if (template.getDelFlag() == null) {
            template.setDelFlag(0);
        }
        
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        
        save(template);
        log.info("创建流程模板成功：{}", template.getTemplateKey());
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowTemplate updateTemplate(FlowTemplate template) {
        template.setUpdateTime(LocalDateTime.now());
        updateById(template);
        log.info("更新流程模板成功：{}", template.getTemplateKey());
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(String id) {
        FlowTemplate template = getById(id);
        if (template != null) {
            // 系统内置模板不允许删除
            if (template.getIsSystem() != null && template.getIsSystem() == 1) {
                throw new RuntimeException("系统内置模板不允许删除");
            }
            removeById(id);
            log.info("删除流程模板成功：{}", template.getTemplateKey());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableTemplate(String id) {
        FlowTemplate template = getById(id);
        if (template != null) {
            template.setStatus(1);
            template.setUpdateTime(LocalDateTime.now());
            updateById(template);
            log.info("启用流程模板：{}", template.getTemplateKey());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableTemplate(String id) {
        FlowTemplate template = getById(id);
        if (template != null) {
            template.setStatus(0);
            template.setUpdateTime(LocalDateTime.now());
            updateById(template);
            log.info("禁用流程模板：{}", template.getTemplateKey());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createModelFromTemplate(String templateKey, String modelName, String modelKey) {
        FlowTemplate template = getTemplateByKey(templateKey);
        if (template == null) {
            throw new RuntimeException("模板不存在：" + templateKey);
        }
        
        if (template.getStatus() != 1) {
            throw new RuntimeException("模板已禁用，无法使用");
        }
        
        // 创建流程模型
        FlowModel model = new FlowModel();
        model.setModelName(modelName);
        model.setModelKey(modelKey != null && !modelKey.isEmpty() ? modelKey : 
                "model_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        model.setCategory(template.getCategory());
        model.setFormType(template.getFormType());
        model.setFormJson(template.getFormJson());
        model.setBpmnXml(template.getBpmnXml());
        model.setStatus(0); // 设计态
        model.setVersion(1);
        model.setDelFlag(0);
        
        FlowModel savedModel = flowModelService.createModel(model);
        
        // 增加模板使用次数
        incrementUsageCount(templateKey);
        
        log.info("从模板创建流程模型成功，模板：{}，模型：{}", templateKey, savedModel.getModelKey());
        return savedModel.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUsageCount(String templateKey) {
        LambdaUpdateWrapper<FlowTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(FlowTemplate::getTemplateKey, templateKey)
                .setSql("usage_count = usage_count + 1");
        update(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowTemplate copyTemplate(String id, String newName) {
        FlowTemplate source = getById(id);
        if (source == null) {
            throw new RuntimeException("源模板不存在");
        }
        
        FlowTemplate newTemplate = new FlowTemplate();
        newTemplate.setTemplateKey("template_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        newTemplate.setTemplateName(newName);
        newTemplate.setCategory(source.getCategory());
        newTemplate.setDescription(source.getDescription());
        newTemplate.setIcon(source.getIcon());
        newTemplate.setFormType(source.getFormType());
        newTemplate.setFormJson(source.getFormJson());
        newTemplate.setBpmnXml(source.getBpmnXml());
        newTemplate.setVariables(source.getVariables());
        newTemplate.setStatus(1);
        newTemplate.setVersion(1);
        newTemplate.setUsageCount(0);
        newTemplate.setIsSystem(0);
        newTemplate.setSortOrder(source.getSortOrder());
        newTemplate.setDelFlag(0);
        
        save(newTemplate);
        log.info("复制模板成功，源模板：{}，新模板：{}", source.getTemplateKey(), newTemplate.getTemplateKey());
        return newTemplate;
    }
}
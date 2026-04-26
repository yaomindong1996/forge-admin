package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiPageTemplate;
import com.mdframe.forge.plugin.generator.dto.AiPageTemplateDTO;
import com.mdframe.forge.plugin.generator.mapper.AiPageTemplateMapper;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPageTemplateService extends ServiceImpl<AiPageTemplateMapper, AiPageTemplate> {

    private final ObjectMapper objectMapper;

    /**
     * 分页查询
     */
    public Page<AiPageTemplate> listPage(PageQuery pageQuery, String templateKey, String templateName) {
        Page<AiPageTemplate> page = pageQuery.toPage();
        LambdaQueryWrapper<AiPageTemplate> wrapper = new LambdaQueryWrapper<AiPageTemplate>()
                .like(StringUtils.isNotBlank(templateKey), AiPageTemplate::getTemplateKey, templateKey)
                .like(StringUtils.isNotBlank(templateName), AiPageTemplate::getTemplateName, templateName)
                .orderByAsc(AiPageTemplate::getSort);
        return page(page, wrapper);
    }

    /**
     * 查询所有启用的模板（前端选择用）
     */
    public List<AiPageTemplate> listEnabled() {
        return list(new LambdaQueryWrapper<AiPageTemplate>()
                .eq(AiPageTemplate::getEnabled, 1)
                .orderByAsc(AiPageTemplate::getSort));
    }

    /**
     * 根据 templateKey 查询
     */
    public AiPageTemplate getByTemplateKey(String templateKey) {
        return getOne(new LambdaQueryWrapper<AiPageTemplate>()
                .eq(AiPageTemplate::getTemplateKey, templateKey));
    }

    /**
     * 新增模板
     */
    @Transactional(rollbackFor = Exception.class)
    public void createTemplate(AiPageTemplateDTO dto) {
        if (StringUtils.isBlank(dto.getTemplateKey())) {
            throw new BusinessException("模板标识不能为空");
        }
        AiPageTemplate existing = getByTemplateKey(dto.getTemplateKey());
        if (existing != null) {
            throw new BusinessException("模板标识已存在: " + dto.getTemplateKey());
        }
        validateJsonFields(dto);
        AiPageTemplate template = new AiPageTemplate();
        copyDtoToEntity(dto, template);
        template.setIsBuiltin(0);
        if (template.getEnabled() == null) template.setEnabled(1);
        if (template.getSort() == null) template.setSort(99);
        save(template);
    }

    /**
     * 更新模板
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(AiPageTemplateDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        AiPageTemplate template = getById(dto.getId());
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        validateJsonFields(dto);
        copyDtoToEntity(dto, template);
        updateById(template);
    }

    /**
     * 删除模板
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        AiPageTemplate template = getById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (Integer.valueOf(1).equals(template.getIsBuiltin())) {
            throw new BusinessException("内置模板不允许删除");
        }
        removeById(id);
    }

    private void copyDtoToEntity(AiPageTemplateDTO dto, AiPageTemplate template) {
        if (dto.getTemplateKey() != null) template.setTemplateKey(dto.getTemplateKey());
        if (dto.getTemplateName() != null) template.setTemplateName(dto.getTemplateName());
        if (dto.getDescription() != null) template.setDescription(dto.getDescription());
        if (dto.getIcon() != null) template.setIcon(dto.getIcon());
        if (dto.getSystemPrompt() != null) template.setSystemPrompt(dto.getSystemPrompt());
        if (dto.getSchemaHint() != null) template.setSchemaHint(StringUtils.isBlank(dto.getSchemaHint()) ? null : dto.getSchemaHint());
        if (dto.getDefaultConfig() != null) template.setDefaultConfig(StringUtils.isBlank(dto.getDefaultConfig()) ? null : dto.getDefaultConfig());
        if (dto.getEnabled() != null) template.setEnabled(dto.getEnabled());
        if (dto.getSort() != null) template.setSort(dto.getSort());
        if (dto.getCodegenType() != null) template.setCodegenType(dto.getCodegenType());
    }

    private void validateJsonFields(AiPageTemplateDTO dto) {
        validateJson(dto.getSchemaHint(), "schemaHint");
        validateJson(dto.getDefaultConfig(), "defaultConfig");
    }

    private void validateJson(String json, String fieldName) {
        if (StringUtils.isBlank(json)) return;
        try {
            objectMapper.readTree(json);
        } catch (Exception e) {
            throw new BusinessException(fieldName + " JSON格式不正确");
        }
    }
}

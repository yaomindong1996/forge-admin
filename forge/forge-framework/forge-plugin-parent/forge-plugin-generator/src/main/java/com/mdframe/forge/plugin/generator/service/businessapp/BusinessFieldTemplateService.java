package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFieldTemplate;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.mapper.BusinessFieldTemplateMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFieldVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务字段模板服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessFieldTemplateService extends ServiceImpl<BusinessFieldTemplateMapper, AiBusinessFieldTemplate> {

    private final ObjectMapper objectMapper;
    private final BusinessFieldSchemaService fieldSchemaService;

    public List<AiBusinessFieldTemplate> listTemplates(String suiteCode) {
        return baseMapper.selectEnabledTemplates(resolveTenantId(), StringUtils.trimToNull(suiteCode));
    }

    public List<BusinessFieldVO> listTemplateFields(String suiteCode) {
        return listTemplates(suiteCode).stream()
                .map(this::toFieldVO)
                .toList();
    }

    public AiBusinessFieldTemplate requireByCode(String templateCode) {
        String code = StringUtils.trimToNull(templateCode);
        if (StringUtils.isBlank(code)) {
            throw new BusinessException("字段模板编码不能为空");
        }
        AiBusinessFieldTemplate template = baseMapper.selectByTemplateCode(resolveTenantId(), code);
        if (template == null) {
            throw new BusinessException("字段模板不存在: " + code);
        }
        return template;
    }

    public LowcodeFieldSchema readFieldSchema(AiBusinessFieldTemplate template) {
        if (template == null || StringUtils.isBlank(template.getFieldSchema())) {
            throw new BusinessException("字段模板协议不能为空");
        }
        try {
            return objectMapper.readValue(template.getFieldSchema(), LowcodeFieldSchema.class);
        } catch (Exception e) {
            throw new BusinessException("字段模板协议格式不正确: " + template.getTemplateCode());
        }
    }

    private BusinessFieldVO toFieldVO(AiBusinessFieldTemplate template) {
        BusinessFieldVO vo = fieldSchemaService.toFieldVO(readFieldSchema(template));
        vo.setTemplateCode(template.getTemplateCode());
        vo.setTemplateName(template.getTemplateName());
        return vo;
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}

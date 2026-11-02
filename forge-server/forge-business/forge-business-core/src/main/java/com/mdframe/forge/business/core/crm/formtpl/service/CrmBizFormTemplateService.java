package com.mdframe.forge.business.core.crm.formtpl.service;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.mdframe.forge.business.core.crm.formtpl.BizFormObjectTypes;
import com.mdframe.forge.business.core.crm.formtpl.domain.CrmBizFormField;
import com.mdframe.forge.business.core.crm.formtpl.domain.CrmBizFormTemplate;
import com.mdframe.forge.business.core.crm.formtpl.domain.CrmBizFormTemplateField;
import com.mdframe.forge.business.core.crm.formtpl.dto.CrmBizFormFieldSettingDTO;
import com.mdframe.forge.business.core.crm.formtpl.dto.CrmBizFormTemplateSaveDTO;
import com.mdframe.forge.business.core.crm.formtpl.mapper.CrmBizFormFieldMapper;
import com.mdframe.forge.business.core.crm.formtpl.mapper.CrmBizFormTemplateFieldMapper;
import com.mdframe.forge.business.core.crm.formtpl.mapper.CrmBizFormTemplateMapper;
import com.mdframe.forge.business.core.crm.formtpl.vo.CrmBizFormSchemeVO;
import com.mdframe.forge.business.core.crm.formtpl.vo.CrmBizFormTemplateDetailVO;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CrmBizFormTemplateService {
    /** 自定义字段：小写蛇形；系统预定义字段允许驼峰（与目录 seed 一致） */
    private static final Pattern CUSTOM_FIELD_CODE = Pattern.compile("^[a-z][a-z0-9_]{1,62}$");
    private static final Pattern SYSTEM_FIELD_CODE = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{1,62}$");
    /** 对齐 AiForm fieldComponentSpecs（排除纯展示 text） */
    private static final Set<String> COMPONENT_TYPES = Set.of(
            "input", "barcodeScanner", "textarea", "number", "money", "slider", "rate", "color",
            "select", "dictSelect", "radio", "radioButton", "checkbox", "transfer", "cascader",
            "treeSelect", "customSelect", "date", "datetime", "daterange", "datetimerange",
            "month", "year", "timerange", "switch", "userSelect", "orgTreeSelect", "regionTreeSelect",
            "objectReference", "recordSelector", "fileUpload", "imageUpload",
            "customerSelect", "section"
    );
    private static final Set<String> TYPES_NEED_OPTIONS = Set.of(
            "select", "radio", "radioButton", "checkbox", "transfer", "cascader", "treeSelect", "customSelect"
    );
    private static final Set<String> TYPES_NEED_DICT = Set.of("dictSelect");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final CrmBizFormTemplateMapper templateMapper;
    private final CrmBizFormFieldMapper fieldMapper;
    private final CrmBizFormTemplateFieldMapper templateFieldMapper;
    private final IdentifierGenerator identifierGenerator;

    public CrmBizFormSchemeVO scheme(String objectType) {
        CrmBizFormTemplate template = requireDefault(objectType);
        CrmBizFormSchemeVO vo = new CrmBizFormSchemeVO();
        vo.setObjectType(template.getObjectType());
        vo.setTemplateId(template.getId());
        vo.setTemplateCode(template.getTemplateCode());
        vo.setTemplateName(template.getTemplateName());
        vo.setFormEnabled(template.getFormEnabled());
        vo.setVersion(template.getVersion());
        vo.setFields(fieldMapper.selectSchemeFields(tenant(), template.getId()));
        return vo;
    }

    public CrmBizFormTemplateDetailVO detailDefault(String objectType) {
        CrmBizFormTemplate template = requireDefault(objectType);
        CrmBizFormTemplateDetailVO vo = new CrmBizFormTemplateDetailVO();
        vo.setId(template.getId());
        vo.setObjectType(template.getObjectType());
        vo.setTemplateCode(template.getTemplateCode());
        vo.setTemplateName(template.getTemplateName());
        vo.setIsDefault(template.getIsDefault());
        vo.setFormEnabled(template.getFormEnabled());
        vo.setStatus(template.getStatus());
        vo.setVersion(template.getVersion());
        vo.setFields(fieldMapper.selectTemplateFieldSettings(tenant(), template.getId()));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public CrmBizFormTemplateDetailVO saveDefault(CrmBizFormTemplateSaveDTO dto) {
        CrmBizFormTemplate template = requireDefault(dto.getObjectType());
        if (!Objects.equals(template.getVersion(), dto.getVersion())) {
            throw new BusinessException("表单配置已变化，请刷新后重试");
        }
        if (StringUtils.hasText(dto.getTemplateName())) {
            template.setTemplateName(dto.getTemplateName().trim());
        }
        if (dto.getFormEnabled() != null) {
            template.setFormEnabled(EnableStatus.ENABLED.matches(dto.getFormEnabled())
                    ? EnableStatus.ENABLED.getCode() : EnableStatus.DISABLED.getCode());
        }
        applyFieldSettings(template, dto.getFields());
        if (templateMapper.updateById(template) != 1) {
            throw new BusinessException("表单配置已变化，请刷新后重试");
        }
        return detailDefault(dto.getObjectType());
    }

    private void applyFieldSettings(CrmBizFormTemplate template, List<CrmBizFormFieldSettingDTO> fields) {
        if (fields == null || fields.isEmpty()) {
            throw new BusinessException("字段配置不能为空");
        }
        Set<String> codes = new HashSet<>();
        templateFieldMapper.logicDeleteByTemplateId(tenant(), template.getId());
        int order = 10;
        for (CrmBizFormFieldSettingDTO item : fields) {
            if (Boolean.TRUE.equals(item.getRemove())) {
                removeCustomField(template.getObjectType(), item);
                continue;
            }
            boolean custom = "CUSTOM".equalsIgnoreCase(item.getFieldKind());
            String code = normalizeCode(item.getFieldCode(), custom);
            if (!codes.add(code.toLowerCase(Locale.ROOT))) {
                throw new BusinessException("字段编码重复：" + code);
            }
            CrmBizFormField field = upsertField(template.getObjectType(), item, code);
            boolean locked = EnableStatus.ENABLED.matches(field.getLocked());
            if (locked) {
                item.setEnabled(EnableStatus.ENABLED.getCode());
                item.setRequired(EnableStatus.ENABLED.getCode());
            }
            insertTemplateField(template.getId(), field.getId(), item, order);
            order += 10;
        }
    }

    private CrmBizFormField upsertField(String objectType, CrmBizFormFieldSettingDTO item, String code) {
        boolean custom = "CUSTOM".equalsIgnoreCase(item.getFieldKind());
        CrmBizFormField existing = item.getFieldId() != null ? fieldMapper.selectById(item.getFieldId()) : null;
        if (existing == null || existing.getDelFlag() == null || existing.getDelFlag() != 0L) {
            existing = fieldMapper.selectByObjectAndCode(tenant(), objectType, code);
        }
        if (existing != null) {
            if ("SYSTEM".equalsIgnoreCase(existing.getFieldKind())) {
                // 系统字段原始名保留在目录；显示名只写模版字段 display_label
                return existing;
            }
            if (!custom) {
                throw new BusinessException("不能将系统字段改为自定义字段");
            }
            fillCustom(existing, item, code);
            fieldMapper.updateById(existing);
            return existing;
        }
        if (!custom) {
            throw new BusinessException("未知系统字段：" + code);
        }
        CrmBizFormField created = new CrmBizFormField();
        created.setId(identifierGenerator.nextId(created).longValue());
        created.setTenantId(tenant());
        created.setObjectType(objectType);
        created.setFieldKind("CUSTOM");
        created.setLocked(EnableStatus.DISABLED.getCode());
        created.setDefaultEnabled(EnableStatus.ENABLED.getCode());
        created.setDefaultRequired(EnableStatus.DISABLED.getCode());
        created.setDelFlag(0L);
        fillCustom(created, item, code);
        fieldMapper.insert(created);
        return created;
    }

    private void fillCustom(CrmBizFormField field, CrmBizFormFieldSettingDTO item, String code) {
        field.setFieldCode(code);
        field.setFieldLabel(item.getFieldLabel().trim());
        String componentType = StringUtils.hasText(item.getComponentType()) ? item.getComponentType().trim() : "input";
        validateCustomComponent(componentType, item);
        field.setComponentType(componentType);
        field.setDictType(trimToNull(item.getDictType()));
        field.setOptionsJson(trimToNull(item.getOptionsJson()));
        field.setSectionCode(StringUtils.hasText(item.getSectionCode()) ? item.getSectionCode().trim() : "extra");
        field.setSortOrder(item.getSortOrder() == null ? 900 : item.getSortOrder());
    }

    private void validateCustomComponent(String componentType, CrmBizFormFieldSettingDTO item) {
        if (!COMPONENT_TYPES.contains(componentType)) {
            throw new BusinessException("不支持的字段类型：" + componentType);
        }
        if (TYPES_NEED_DICT.contains(componentType) && !StringUtils.hasText(item.getDictType())) {
            throw new BusinessException("类型「" + componentType + "」需要填写字典类型");
        }
        if (TYPES_NEED_OPTIONS.contains(componentType)) {
            if (!StringUtils.hasText(item.getOptionsJson())) {
                throw new BusinessException("类型「" + componentType + "」至少需要一个选项");
            }
            try {
                JsonNode node = JSON.readTree(item.getOptionsJson().trim());
                if (!node.isArray() || node.isEmpty()) {
                    throw new BusinessException("类型「" + componentType + "」选项必须为非空 JSON 数组");
                }
            } catch (BusinessException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BusinessException("类型「" + componentType + "」选项 JSON 无效");
            }
        }
    }

    private void insertTemplateField(Long templateId, Long fieldId, CrmBizFormFieldSettingDTO item, int order) {
        CrmBizFormTemplateField link = new CrmBizFormTemplateField();
        link.setId(identifierGenerator.nextId(link).longValue());
        link.setTenantId(tenant());
        link.setTemplateId(templateId);
        link.setFieldId(fieldId);
        link.setEnabled(flag(item.getEnabled()));
        link.setRequired(flag(item.getRequired()));
        link.setListVisible(flag(item.getListVisible()));
        link.setListSortable(flag(item.getListSortable()));
        link.setCommonlyUsed(flag(item.getCommonlyUsed()));
        link.setDisplayLabel(trimToNull(item.getDisplayLabel()));
        link.setSortOrder(item.getSortOrder() == null ? order : item.getSortOrder());
        link.setDelFlag(0L);
        templateFieldMapper.insert(link);
    }

    private void removeCustomField(String objectType, CrmBizFormFieldSettingDTO item) {
        CrmBizFormField field = item.getFieldId() != null
                ? fieldMapper.selectById(item.getFieldId())
                : fieldMapper.selectByObjectAndCode(tenant(), objectType, normalizeCode(item.getFieldCode(), true));
        if (field == null || field.getDelFlag() == null || field.getDelFlag() != 0L) {
            return;
        }
        if (!"CUSTOM".equalsIgnoreCase(field.getFieldKind())) {
            throw new BusinessException("系统预定义字段不能删除");
        }
        fieldMapper.deleteById(field.getId());
    }

    private CrmBizFormTemplate requireDefault(String objectType) {
        String type = BizFormObjectTypes.require(objectType).objectType();
        CrmBizFormTemplate template = templateMapper.selectDefaultTemplate(tenant(), type);
        if (template == null) {
            throw new BusinessException("未配置默认表单模版：" + type);
        }
        return template;
    }

    private static String normalizeCode(String raw, boolean custom) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException("字段编码不能为空");
        }
        String code = raw.trim();
        if (custom) {
            code = code.toLowerCase(Locale.ROOT);
            if (!CUSTOM_FIELD_CODE.matcher(code).matches()) {
                throw new BusinessException("自定义字段编码需为小写字母开头的英文下划线格式");
            }
            return code;
        }
        if (!SYSTEM_FIELD_CODE.matcher(code).matches()) {
            throw new BusinessException("系统字段编码格式无效");
        }
        return code;
    }

    private static int flag(Integer value) {
        return EnableStatus.ENABLED.matches(value) ? EnableStatus.ENABLED.getCode() : EnableStatus.DISABLED.getCode();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long tenant() {
        Long id = SessionHelper.getTenantId();
        if (id == null) {
            throw new BusinessException("租户未核实");
        }
        return id;
    }
}

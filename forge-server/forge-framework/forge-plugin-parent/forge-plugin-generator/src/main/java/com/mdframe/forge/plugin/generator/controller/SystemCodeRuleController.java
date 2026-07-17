package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleGenerateDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRulePreviewDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSaveDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSegmentDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleStatusDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectQueryDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFieldDesignService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectService;
import com.mdframe.forge.plugin.generator.service.businessapp.CodeRuleService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFieldVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRuleCapabilityVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRuleDetailVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRuleGenerateVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRulePreviewVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 系统编码规则结构化管理接口。
 */
@RestController
@RequestMapping("/system/code-rule")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class SystemCodeRuleController {

    private static final Set<String> VARIABLE_SOURCES = Set.of("CUSTOM", "LOWCODE");
    private static final Pattern CUSTOM_VARIABLE_PATTERN =
            Pattern.compile("^[A-Za-z_][A-Za-z0-9_]{0,63}$");

    private final CodeRuleService codeRuleService;
    private final BusinessObjectService businessObjectService;
    private final BusinessFieldDesignService businessFieldDesignService;

    @GetMapping("/page")
    @SaCheckPermission("system:codeRule:list")
    @OperationLog(module = "编码规则", type = OperationType.QUERY, desc = "分页查询编码规则")
    public RespInfo<Page<CodeRuleDetailVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String ruleCode,
            @RequestParam(required = false) String ruleName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        return RespInfo.success(codeRuleService.pageDetails(
                pageNum, pageSize, ruleCode, ruleName, category, status));
    }

    @GetMapping("/list")
    @SaCheckPermission("system:codeRule:list")
    @OperationLog(module = "编码规则", type = OperationType.QUERY, desc = "查询可选择编码规则")
    public RespInfo<List<CodeRuleDetailVO>> list(
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String objectCode) {
        return RespInfo.success(codeRuleService.listSelectable(scene, objectCode));
    }

    @PostMapping("/getById")
    @SaCheckPermission("system:codeRule:list")
    @OperationLog(module = "编码规则", type = OperationType.QUERY, desc = "查询编码规则详情")
    public RespInfo<CodeRuleDetailVO> getById(@RequestParam Long id) {
        return RespInfo.success(codeRuleService.detailVO(id));
    }

    @PostMapping("/add")
    @SaCheckPermission("system:codeRule:add")
    @OperationLog(module = "编码规则", type = OperationType.ADD, desc = "新增编码规则")
    public RespInfo<Long> add(@Valid @RequestBody CodeRuleSaveDTO dto) {
        validateAndEnrichBusinessFieldMapping(dto);
        return RespInfo.success(codeRuleService.create(dto));
    }

    @PostMapping("/edit")
    @SaCheckPermission("system:codeRule:edit")
    @OperationLog(module = "编码规则", type = OperationType.UPDATE, desc = "修改编码规则")
    public RespInfo<Void> edit(@Valid @RequestBody CodeRuleSaveDTO dto) {
        validateAndEnrichBusinessFieldMapping(dto);
        codeRuleService.update(dto);
        return RespInfo.success();
    }

    @PostMapping("/remove/{id}")
    @SaCheckPermission("system:codeRule:remove")
    @OperationLog(module = "编码规则", type = OperationType.DELETE, desc = "删除编码规则")
    public RespInfo<Void> remove(@PathVariable Long id) {
        codeRuleService.delete(id);
        return RespInfo.success();
    }

    @PostMapping("/status")
    @SaCheckPermission("system:codeRule:edit")
    @OperationLog(module = "编码规则", type = OperationType.UPDATE, desc = "启停编码规则")
    public RespInfo<Void> status(@Valid @RequestBody CodeRuleStatusDTO dto) {
        codeRuleService.updateStatus(dto);
        return RespInfo.success();
    }

    @PostMapping("/preview")
    @SaCheckPermission("system:codeRule:use")
    public RespInfo<CodeRulePreviewVO> preview(
            @Valid @RequestBody(required = false) CodeRulePreviewDTO dto) {
        return RespInfo.success(codeRuleService.preview(dto));
    }

    @PostMapping("/generate")
    @SaCheckPermission("system:codeRule:use")
    @OperationLog(module = "编码规则", type = OperationType.OTHER, desc = "生成业务编码")
    public RespInfo<CodeRuleGenerateVO> generate(@Valid @RequestBody CodeRuleGenerateDTO dto) {
        CodeRuleGenerateDTO request = dto == null ? new CodeRuleGenerateDTO() : dto;
        Map<String, Object> fields = request.getFields() == null ? request.getContext() : request.getFields();
        return RespInfo.success(codeRuleService.generateResult(request.getRuleCode(), fields));
    }

    @GetMapping("/capabilities")
    @SaCheckPermission("system:codeRule:list")
    @OperationLog(module = "编码规则", type = OperationType.QUERY, desc = "查询编码规则能力")
    public RespInfo<CodeRuleCapabilityVO> capabilities(
            @RequestParam(required = false) Long sourceObjectId) {
        CodeRuleCapabilityVO result = codeRuleService.capabilities();
        BusinessObjectQueryDTO query = new BusinessObjectQueryDTO();
        query.setStatus(1);
        result.setBusinessObjects(businessObjectService.list(query).stream()
                .map(object -> option(
                        StringUtils.defaultIfBlank(object.getObjectName(), object.getObjectCode())
                                + "（" + object.getObjectCode() + "）",
                        String.valueOf(object.getId()),
                        object.getSuiteName()))
                .toList());
        if (sourceObjectId != null) {
            BusinessObjectVO object = businessObjectService.detail(sourceObjectId);
            if (Integer.valueOf(1).equals(object.getStatus())) {
                result.setBusinessFields(availableBusinessFields(sourceObjectId).stream()
                        .map(field -> option(
                                StringUtils.defaultIfBlank(field.getFieldName(), field.getFieldCode())
                                        + "（" + field.getFieldCode() + "）",
                                field.getFieldCode(),
                                businessFieldDescription(field)))
                        .toList());
            } else {
                result.setBusinessFields(List.of());
            }
        }
        return RespInfo.success(result);
    }

    private void validateAndEnrichBusinessFieldMapping(CodeRuleSaveDTO dto) {
        if (dto == null) {
            return;
        }
        normalizeVariableSources(dto);
        Set<String> customNames = customVariableNames(dto);
        Set<String> invalidCustomNames = customNames.stream()
                .filter(name -> !isSafeCustomVariableName(name))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!invalidCustomNames.isEmpty()) {
            throw new BusinessException("自定义变量名格式不正确: " + String.join(", ", invalidCustomNames));
        }
        Set<String> variableFieldCodes = lowCodeVariableFieldCodes(dto);
        if (!hasLowCodeVariable(dto)) {
            dto.setSourceObjectId(null);
            dto.setSourceObjectCode(null);
            return;
        }
        if (dto.getSourceObjectId() == null) {
            throw new BusinessException("低代码业务变量必须选择字段来源业务对象");
        }
        BusinessObjectVO object = businessObjectService.detail(dto.getSourceObjectId());
        if (!Integer.valueOf(1).equals(object.getStatus())) {
            throw new BusinessException("字段来源业务对象已停用");
        }
        Set<String> availableFieldCodes = availableBusinessFields(dto.getSourceObjectId()).stream()
                .map(BusinessFieldVO::getFieldCode)
                .collect(Collectors.toSet());
        Set<String> missingFieldCodes = missingVariableFieldCodes(variableFieldCodes, availableFieldCodes);
        if (!missingFieldCodes.isEmpty()) {
            throw new BusinessException("业务变量字段不存在或已停用: " + String.join(", ", missingFieldCodes));
        }
        dto.setSourceObjectCode(object.getObjectCode());
    }

    static Set<String> lowCodeVariableFieldCodes(CodeRuleSaveDTO dto) {
        List<CodeRuleSegmentDTO> segments = dto == null || dto.getSegments() == null
                ? List.of() : dto.getSegments();
        return segments.stream()
                .filter(segment -> segment != null && "VARIABLE".equalsIgnoreCase(segment.getSegmentType()))
                .filter(segment -> "LOWCODE".equalsIgnoreCase(segment.getVariableSource()))
                .map(segment -> segment.getSegmentValue() == null ? "" : segment.getSegmentValue().trim())
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static boolean hasLowCodeVariable(CodeRuleSaveDTO dto) {
        List<CodeRuleSegmentDTO> segments = dto == null || dto.getSegments() == null
                ? List.of() : dto.getSegments();
        return segments.stream()
                .filter(Objects::nonNull)
                .anyMatch(segment -> "VARIABLE".equalsIgnoreCase(segment.getSegmentType())
                        && "LOWCODE".equalsIgnoreCase(segment.getVariableSource()));
    }

    static Set<String> customVariableNames(CodeRuleSaveDTO dto) {
        List<CodeRuleSegmentDTO> segments = dto == null || dto.getSegments() == null
                ? List.of() : dto.getSegments();
        return segments.stream()
                .filter(segment -> segment != null && "VARIABLE".equalsIgnoreCase(segment.getSegmentType()))
                .filter(segment -> !"LOWCODE".equalsIgnoreCase(segment.getVariableSource()))
                .map(segment -> segment.getSegmentValue() == null ? "" : segment.getSegmentValue().trim())
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static boolean isSafeCustomVariableName(String value) {
        return value != null && CUSTOM_VARIABLE_PATTERN.matcher(value).matches();
    }

    private void normalizeVariableSources(CodeRuleSaveDTO dto) {
        List<CodeRuleSegmentDTO> segments = dto.getSegments() == null ? List.of() : dto.getSegments();
        for (CodeRuleSegmentDTO segment : segments) {
            if (segment == null || !"VARIABLE".equalsIgnoreCase(segment.getSegmentType())) {
                continue;
            }
            String variableSource = StringUtils.upperCase(
                    StringUtils.defaultIfBlank(segment.getVariableSource(), "CUSTOM"));
            if (!VARIABLE_SOURCES.contains(variableSource)) {
                throw new BusinessException("不支持的业务变量来源: " + variableSource);
            }
            segment.setVariableSource(variableSource);
        }
    }

    static Set<String> missingVariableFieldCodes(Set<String> requestedFieldCodes,
                                                 Set<String> availableFieldCodes) {
        Set<String> requested = requestedFieldCodes == null ? Set.of() : requestedFieldCodes;
        Set<String> available = availableFieldCodes == null ? Set.of() : availableFieldCodes;
        return requested.stream()
                .filter(fieldCode -> !available.contains(fieldCode))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<BusinessFieldVO> availableBusinessFields(Long sourceObjectId) {
        return businessFieldDesignService.listFields(sourceObjectId).stream()
                .filter(field -> !Boolean.TRUE.equals(field.getSystemField()))
                .filter(field -> StringUtils.isNotBlank(field.getFieldCode()))
                .filter(field -> !StringUtils.equalsAnyIgnoreCase(
                        field.getFieldStatus(), "DISABLED", "HIDDEN"))
                .toList();
    }

    private String businessFieldDescription(BusinessFieldVO field) {
        String fieldType = StringUtils.defaultIfBlank(field.getFieldType(), "业务字段");
        String columnName = StringUtils.defaultIfBlank(field.getColumnName(), "未映射数据列");
        return fieldType + " · " + columnName;
    }

    private CodeRuleCapabilityVO.OptionVO option(String label, String value, String description) {
        return new CodeRuleCapabilityVO.OptionVO(label, value, description);
    }
}

package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessDocumentConfigDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessDocumentNoRulePreviewDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessDocumentConfigMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentConfigVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentNoRulePreviewVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentNoRuleTokenVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 业务对象单据配置服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessDocumentConfigService {

    private static final Set<String> SYSTEM_FIELDS = Set.of(
            "id", "tenantId", "tenant_id", "createBy", "create_by", "createTime", "create_time",
            "createDept", "create_dept", "updateBy", "update_by", "updateTime", "update_time"
    );

    private static final Pattern NO_RULE_TOKEN_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern LEGACY_NO_RULE_TOKEN_PATTERN = Pattern.compile("(?<!\\$)\\{([^{}]+)}");
    private static final Pattern LEGACY_SEQ_TOKEN_PATTERN = Pattern.compile("seq(\\d+)");

    private final BusinessDocumentConfigMapper documentConfigMapper;
    private final BusinessBindingMapper bindingMapper;
    private final BusinessObjectService objectService;
    private final AiCrudConfigMapper crudConfigMapper;
    private final ObjectMapper objectMapper;

    public BusinessDocumentConfigVO getConfig(Long objectId) {
        AiBusinessObject object = objectService.requireEntity(objectId);
        AiBusinessDocumentConfig config = documentConfigMapper.selectByObjectId(resolveTenantId(), objectId);
        if (config == null) {
            config = documentConfigMapper.selectByObjectCode(resolveTenantId(), object.getObjectCode());
        }
        if (config == null) {
            BusinessDocumentConfigVO vo = new BusinessDocumentConfigVO();
            vo.setObjectId(object.getId());
            vo.setSuiteCode(object.getSuiteCode());
            vo.setObjectCode(object.getObjectCode());
            vo.setConfigKey(object.getConfigKey());
            vo.setDocumentEnabled(false);
            vo.setDocumentName(object.getObjectName() + "单据");
            vo.setStatusMapping(defaultStatusMapping());
            vo.setStatusMappingRows(defaultStatusRows());
            vo.setMainFlowSummary(buildMainFlowSummary(resolveTenantId(), object.getObjectCode(), null));
            return vo;
        }
        return toVO(config);
    }

    public List<BusinessDocumentNoRuleTokenVO> listNoRuleTokens() {
        List<BusinessDocumentNoRuleTokenVO> tokens = new ArrayList<>();
        tokens.add(token("${yyyy}", "年份", "日期时间", "当前年份，四位数字", "2026", "2026"));
        tokens.add(token("${yyyyMM}", "年月", "日期时间", "当前年月，六位数字", "202606", "202606"));
        tokens.add(token("${yyyyMMdd}", "年月日", "日期时间", "当前日期，八位数字", "20260602", "20260602"));
        tokens.add(token("${HHmmss}", "时分秒", "日期时间", "当前时间，六位数字", "203405", "203405"));
        tokens.add(token("${seq}", "流水号", "序列", "预览使用样例序号，不占用真实序列", "1", "1"));
        tokens.add(token("${seq:4}", "四位流水号", "序列", "流水号左侧补零到指定长度", "0001", "0001"));
        tokens.add(token("${suiteCode}", "套件编码", "上下文", "当前业务套件编码", "CRM", "CRM"));
        tokens.add(token("${objectCode}", "对象编码", "上下文", "当前业务对象编码", "OPPORTUNITY", "OPPORTUNITY"));
        tokens.add(token("${starter}", "发起人", "上下文", "当前发起人用户名或用户编码", "zhangsan", "zhangsan"));
        tokens.add(token("${deptCode}", "部门编码", "上下文", "当前发起人部门编码", "SALES", "SALES"));
        tokens.add(token("${field:<fieldCode>}", "单据字段", "业务字段", "从样例数据读取业务字段值，例如 ${field:customerName}", "ACME", "ACME"));
        return tokens;
    }

    public BusinessDocumentNoRulePreviewVO previewNoRule(BusinessDocumentNoRulePreviewDTO dto) {
        BusinessDocumentNoRulePreviewDTO source = dto == null ? new BusinessDocumentNoRulePreviewDTO() : dto;
        String template = StringUtils.defaultIfBlank(normalizeNoRuleTemplate(source.getTemplate()), "DOC-${yyyyMMdd}-${seq:4}");
        return renderNoRulePreview(
                template,
                StringUtils.defaultIfBlank(source.getSuiteCode(), "SUITE"),
                StringUtils.defaultIfBlank(source.getObjectCode(), "OBJECT"),
                StringUtils.defaultIfBlank(source.getStarter(), "starter"),
                StringUtils.defaultIfBlank(source.getDeptCode(), "DEPT"),
                source.getSampleData(),
                source.getSequence() == null ? 1 : source.getSequence()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(Long objectId, BusinessDocumentConfigDTO dto) {
        if (dto == null) {
            throw new BusinessException("单据配置不能为空");
        }
        AiBusinessObject object = objectService.requireEntity(objectId);
        boolean enabled = Boolean.TRUE.equals(dto.getDocumentEnabled());
        String documentNoRule = normalizeNoRuleTemplate(StringUtils.firstNonBlank(dto.getNoRuleTemplate(), dto.getDocumentNoRule()));
        List<BusinessDocumentConfigVO.StatusMappingRowVO> statusRows = normalizeStatusRows(
                dto.getStatusMappingRows(), dto.getStatusMapping());
        Map<String, String> statusMapping = statusMappingFromRows(statusRows);
        if (StringUtils.isNotBlank(documentNoRule)) {
            validateNoRuleTemplate(documentNoRule);
        }
        if (enabled) {
            validateRequiredField("单据状态字段", dto.getStatusField());
            validateObjectField(object, dto.getStatusField(), "单据状态字段");
            validateOptionalObjectField(object, dto.getStarterField(), "发起人字段");
            validateOptionalObjectField(object, dto.getOwnerField(), "负责人字段");
        }

        AiBusinessDocumentConfig config = documentConfigMapper.selectByObjectId(resolveTenantId(), objectId);
        if (config == null) {
            config = documentConfigMapper.selectByObjectCode(resolveTenantId(), object.getObjectCode());
        }
        if (config == null) {
            config = new AiBusinessDocumentConfig();
            config.setTenantId(resolveTenantId());
            config.setObjectId(object.getId());
            config.setSuiteCode(object.getSuiteCode());
            config.setObjectCode(object.getObjectCode());
        }
        config.setObjectId(object.getId());
        config.setSuiteCode(object.getSuiteCode());
        config.setObjectCode(object.getObjectCode());
        config.setConfigKey(object.getConfigKey());
        config.setDocumentEnabled(enabled ? 1 : 0);
        config.setDocumentName(StringUtils.defaultIfBlank(dto.getDocumentName(), object.getObjectName() + "单据"));
        config.setDocumentNoRule(StringUtils.trimToNull(documentNoRule));
        config.setStatusField(StringUtils.trimToNull(dto.getStatusField()));
        config.setStarterField(StringUtils.trimToNull(dto.getStarterField()));
        config.setOwnerField(StringUtils.trimToNull(dto.getOwnerField()));
        String requestedFlowKey = StringUtils.trimToNull(dto.getDefaultFlowKey());
        AiBusinessBinding mainFlowBinding = selectMainFlowBinding(resolveTenantId(), object.getObjectCode());
        config.setDefaultFlowKey(mainFlowBinding == null
                ? requestedFlowKey
                : StringUtils.defaultIfBlank(resolveFlowModelKey(readBindingConfig(mainFlowBinding.getBindingConfig())),
                        mainFlowBinding.getBindingKey()));
        config.setStatusMapping(writeJson(statusMapping, "单据状态映射"));
        config.setOptions(writeJson(buildOptions(dto, documentNoRule, statusRows), "单据扩展配置"));

        if (config.getId() == null) {
            documentConfigMapper.insert(config);
        } else {
            documentConfigMapper.updateById(config);
        }
        syncLegacyFlowBindingIfNeeded(object, requestedFlowKey, config.getDocumentName());
    }

    public AiBusinessDocumentConfig selectEnabledByObjectCode(String objectCode) {
        return selectEnabledByObjectCode(resolveTenantId(), objectCode);
    }

    public AiBusinessDocumentConfig selectEnabledByObjectCode(Long tenantId, String objectCode) {
        if (StringUtils.isBlank(objectCode)) {
            return null;
        }
        AiBusinessDocumentConfig config = documentConfigMapper.selectByObjectCode(
                tenantId != null ? tenantId : resolveTenantId(), objectCode);
        if (config == null || !Integer.valueOf(1).equals(config.getDocumentEnabled())) {
            return null;
        }
        return config;
    }

    public BusinessDocumentConfigVO toVO(AiBusinessDocumentConfig config) {
        Map<String, Object> options = readObjectMap(config.getOptions());
        Long tenantId = config.getTenantId() != null ? config.getTenantId() : resolveTenantId();
        Map<String, Object> mainFlowSummary = buildMainFlowSummary(tenantId, config.getObjectCode(), config.getDefaultFlowKey());
        BusinessDocumentConfigVO vo = new BusinessDocumentConfigVO();
        vo.setId(config.getId());
        vo.setObjectId(config.getObjectId());
        vo.setSuiteCode(config.getSuiteCode());
        vo.setObjectCode(config.getObjectCode());
        vo.setConfigKey(config.getConfigKey());
        vo.setDocumentEnabled(Integer.valueOf(1).equals(config.getDocumentEnabled()));
        vo.setDocumentName(config.getDocumentName());
        String normalizedDocumentNoRule = normalizeNoRuleTemplate(config.getDocumentNoRule());
        vo.setDocumentNoRule(normalizedDocumentNoRule);
        vo.setNoRuleTemplate(StringUtils.defaultIfBlank(normalizeNoRuleTemplate(text(options.get("noRuleTemplate"))), normalizedDocumentNoRule));
        if (StringUtils.isNotBlank(vo.getNoRuleTemplate())) {
            BusinessDocumentNoRulePreviewDTO previewDTO = new BusinessDocumentNoRulePreviewDTO();
            previewDTO.setTemplate(vo.getNoRuleTemplate());
            previewDTO.setSuiteCode(config.getSuiteCode());
            previewDTO.setObjectCode(config.getObjectCode());
            vo.setNoRulePreview(previewNoRule(previewDTO));
        }
        vo.setStatusField(config.getStatusField());
        vo.setStarterField(config.getStarterField());
        vo.setOwnerField(config.getOwnerField());
        vo.setDefaultFlowKey(StringUtils.defaultIfBlank(text(mainFlowSummary.get("flowModelKey")), config.getDefaultFlowKey()));
        vo.setStatusMapping(readStringMap(config.getStatusMapping()));
        vo.setStatusMappingRows(readStatusRows(options, vo.getStatusMapping()));
        vo.setStatusActionPolicy(readObjectMap(options.get("statusActionPolicy")));
        vo.setMainFlowSummary(mainFlowSummary);
        vo.setOptions(options);
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }

    public void syncDefaultFlowKeyByObjectCode(Long tenantId, String objectCode, String flowModelKey) {
        if (StringUtils.isBlank(objectCode)) {
            return;
        }
        AiBusinessDocumentConfig config = documentConfigMapper.selectByObjectCode(
                tenantId != null ? tenantId : resolveTenantId(), objectCode);
        if (config == null) {
            return;
        }
        config.setDefaultFlowKey(StringUtils.trimToNull(flowModelKey));
        documentConfigMapper.updateById(config);
    }

    private BusinessDocumentNoRuleTokenVO token(String insertText, String label, String groupName,
                                                String description, String example, String sampleValue) {
        BusinessDocumentNoRuleTokenVO vo = new BusinessDocumentNoRuleTokenVO();
        vo.setToken(insertText);
        vo.setInsertText(insertText);
        vo.setLabel(label);
        vo.setGroupName(groupName);
        vo.setDescription(description);
        vo.setExample(example);
        vo.setSampleValue(sampleValue);
        return vo;
    }

    private BusinessDocumentNoRulePreviewVO renderNoRulePreview(String template,
                                                                String suiteCode,
                                                                String objectCode,
                                                                String starter,
                                                                String deptCode,
                                                                Map<String, Object> sampleData,
                                                                Integer sequence) {
        BusinessDocumentNoRulePreviewVO vo = new BusinessDocumentNoRulePreviewVO();
        vo.setTemplate(template);
        StringBuilder result = new StringBuilder();
        Matcher matcher = NO_RULE_TOKEN_PATTERN.matcher(template);
        int lastIndex = 0;
        LocalDateTime now = LocalDateTime.now();
        while (matcher.find()) {
            result.append(template, lastIndex, matcher.start());
            String token = matcher.group(1);
            vo.getUsedTokens().add("${" + token + "}");
            String replacement = resolveNoRuleToken(token, suiteCode, objectCode, starter, deptCode, sampleData, sequence, now, vo);
            result.append(replacement);
            lastIndex = matcher.end();
        }
        result.append(template.substring(lastIndex));
        String previewNo = result.toString();
        vo.setPreviewNo(previewNo);
        if (!previewNo.matches("[A-Za-z0-9_\\-./]+")) {
            vo.getWarnings().add(issue(null, "编号包含空格或特殊字符，可能不适合作为对外单据号", "建议只使用字母、数字、短横线、下划线、点和斜线"));
        }
        if (previewNo.length() > 64) {
            vo.getWarnings().add(issue(null, "编号长度超过 64 个字符", "建议缩短固定前缀或字段变量内容"));
        }
        vo.setValid(vo.getErrors().isEmpty());
        return vo;
    }

    private String resolveNoRuleToken(String token,
                                      String suiteCode,
                                      String objectCode,
                                      String starter,
                                      String deptCode,
                                      Map<String, Object> sampleData,
                                      Integer sequence,
                                      LocalDateTime now,
                                      BusinessDocumentNoRulePreviewVO vo) {
        if ("yyyy".equals(token)) {
            return now.format(DateTimeFormatter.ofPattern("yyyy"));
        }
        if ("yyyyMM".equals(token)) {
            return now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        }
        if ("yyyyMMdd".equals(token)) {
            return now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        if ("HHmmss".equals(token)) {
            return now.format(DateTimeFormatter.ofPattern("HHmmss"));
        }
        if ("seq".equals(token)) {
            return String.valueOf(Math.max(sequence == null ? 1 : sequence, 1));
        }
        if (token.startsWith("seq:")) {
            return renderSequenceToken(token, sequence, vo);
        }
        if ("suiteCode".equals(token)) {
            return suiteCode;
        }
        if ("objectCode".equals(token)) {
            return objectCode;
        }
        if ("starter".equals(token)) {
            return starter;
        }
        if ("deptCode".equals(token)) {
            return deptCode;
        }
        if (token.startsWith("field:")) {
            String fieldCode = token.substring("field:".length()).trim();
            if (StringUtils.isBlank(fieldCode)) {
                vo.getErrors().add(issue("${" + token + "}", "字段变量缺少字段编码", "改为 ${field:字段编码}"));
                return "";
            }
            Object value = readSampleValue(sampleData, fieldCode);
            if (value == null) {
                vo.getWarnings().add(issue("${" + token + "}", "样例数据中没有字段 " + fieldCode, "预览时可填入 sampleData，保存后运行态会读取真实字段"));
                return fieldCode;
            }
            return String.valueOf(value);
        }
        vo.getErrors().add(issue("${" + token + "}", "未知编号变量: ${" + token + "}", "从内置变量列表选择，或使用 ${field:<fieldCode>} 引用业务字段"));
        return "";
    }

    private String renderSequenceToken(String token, Integer sequence, BusinessDocumentNoRulePreviewVO vo) {
        String lengthText = token.substring("seq:".length()).trim();
        int length;
        try {
            length = Integer.parseInt(lengthText);
        } catch (Exception e) {
            vo.getErrors().add(issue("${" + token + "}", "流水号长度必须是数字", "例如 ${seq:4}"));
            return "";
        }
        if (length <= 0 || length > 12) {
            vo.getErrors().add(issue("${" + token + "}", "流水号长度建议在 1-12 之间", "例如 ${seq:4}"));
            return "";
        }
        String seqText = String.valueOf(Math.max(sequence == null ? 1 : sequence, 1));
        return StringUtils.leftPad(seqText, length, '0');
    }

    private Object readSampleValue(Map<String, Object> sampleData, String fieldCode) {
        if (sampleData == null || StringUtils.isBlank(fieldCode)) {
            return null;
        }
        if (sampleData.containsKey(fieldCode)) {
            return sampleData.get(fieldCode);
        }
        String camel = snakeToCamel(fieldCode);
        if (sampleData.containsKey(camel)) {
            return sampleData.get(camel);
        }
        String snake = camelToSnake(fieldCode);
        return sampleData.get(snake);
    }

    private BusinessDocumentNoRulePreviewVO.PreviewIssueVO issue(String token, String message, String suggestion) {
        BusinessDocumentNoRulePreviewVO.PreviewIssueVO vo = new BusinessDocumentNoRulePreviewVO.PreviewIssueVO();
        vo.setToken(token);
        vo.setMessage(message);
        vo.setSuggestion(suggestion);
        return vo;
    }

    private void validateNoRuleTemplate(String template) {
        BusinessDocumentNoRulePreviewVO preview = renderNoRulePreview(normalizeNoRuleTemplate(template), "SUITE", "OBJECT", "starter", "DEPT",
                Map.of("fieldCode", "SAMPLE"), 1);
        if (!preview.getErrors().isEmpty()) {
            String message = preview.getErrors().stream()
                    .map(BusinessDocumentNoRulePreviewVO.PreviewIssueVO::getMessage)
                    .findFirst()
                    .orElse("编号规则不正确");
            throw new BusinessException(message);
        }
    }

    private Map<String, Object> buildOptions(BusinessDocumentConfigDTO dto,
                                             String documentNoRule,
                                             List<BusinessDocumentConfigVO.StatusMappingRowVO> statusRows) {
        Map<String, Object> options = new LinkedHashMap<>();
        if (dto.getOptions() != null) {
            options.putAll(dto.getOptions());
        }
        options.put("noRuleTemplate", StringUtils.trimToNull(documentNoRule));
        options.put("statusMappingRows", statusRows);
        options.put("statusActionPolicy", dto.getStatusActionPolicy() == null
                ? new LinkedHashMap<>()
                : dto.getStatusActionPolicy());
        return options;
    }

    private String normalizeNoRuleTemplate(String template) {
        String value = StringUtils.trimToNull(template);
        if (value == null) {
            return null;
        }
        Matcher matcher = LEGACY_NO_RULE_TOKEN_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("${" + normalizeNoRuleToken(matcher.group(1)) + "}"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String normalizeNoRuleToken(String token) {
        String value = StringUtils.defaultString(token).trim();
        Matcher seqMatcher = LEGACY_SEQ_TOKEN_PATTERN.matcher(value);
        if (seqMatcher.matches()) {
            return "seq:" + seqMatcher.group(1);
        }
        return value;
    }

    private List<BusinessDocumentConfigVO.StatusMappingRowVO> normalizeStatusRows(
            List<BusinessDocumentConfigDTO.StatusMappingRowDTO> rows,
            Map<String, String> legacyMapping) {
        Map<String, BusinessDocumentConfigVO.StatusMappingRowVO> defaults = new LinkedHashMap<>();
        for (BusinessDocumentConfigVO.StatusMappingRowVO row : defaultStatusRows()) {
            defaults.put(row.getStandardStatus(), row);
        }
        if (legacyMapping != null) {
            legacyMapping.forEach((key, value) -> {
                BusinessDocumentConfigVO.StatusMappingRowVO row = defaults.get(key);
                if (row != null && StringUtils.isNotBlank(value)) {
                    row.setStatusValue(value.trim());
                }
            });
        }
        if (rows != null) {
            for (BusinessDocumentConfigDTO.StatusMappingRowDTO input : rows) {
                if (input == null || StringUtils.isBlank(input.getStandardStatus())) {
                    continue;
                }
                String standardStatus = input.getStandardStatus().trim().toUpperCase();
                BusinessDocumentConfigVO.StatusMappingRowVO row = defaults.computeIfAbsent(
                        standardStatus, key -> statusRow(key, key, key, key, "default", true, true, false));
                if (StringUtils.isNotBlank(input.getStatusValue())) {
                    row.setStatusValue(input.getStatusValue().trim());
                }
                if (StringUtils.isNotBlank(input.getDisplayName())) {
                    row.setDisplayName(input.getDisplayName().trim());
                }
                if (StringUtils.isNotBlank(input.getTagType())) {
                    row.setTagType(input.getTagType().trim());
                }
                if (input.getAllowEdit() != null) {
                    row.setAllowEdit(input.getAllowEdit());
                }
                if (input.getAllowDelete() != null) {
                    row.setAllowDelete(input.getAllowDelete());
                }
                if (input.getAllowStartFlow() != null) {
                    row.setAllowStartFlow(input.getAllowStartFlow());
                }
            }
        }
        for (BusinessDocumentConfigVO.StatusMappingRowVO row : defaults.values()) {
            if (StringUtils.isBlank(row.getStatusValue())) {
                throw new BusinessException("状态映射缺少存储值: " + row.getStandardStatus());
            }
        }
        return new ArrayList<>(defaults.values());
    }

    private List<BusinessDocumentConfigVO.StatusMappingRowVO> readStatusRows(Map<String, Object> options,
                                                                            Map<String, String> legacyMapping) {
        Object rawRows = options == null ? null : options.get("statusMappingRows");
        if (rawRows != null) {
            try {
                List<BusinessDocumentConfigVO.StatusMappingRowVO> rows = objectMapper.convertValue(
                        rawRows, new TypeReference<List<BusinessDocumentConfigVO.StatusMappingRowVO>>() {});
                if (rows != null && !rows.isEmpty()) {
                    return normalizeStatusRowsFromVO(rows, legacyMapping);
                }
            } catch (Exception ignored) {
                // Fall back to legacy map.
            }
        }
        return normalizeStatusRows(null, legacyMapping);
    }

    private List<BusinessDocumentConfigVO.StatusMappingRowVO> normalizeStatusRowsFromVO(
            List<BusinessDocumentConfigVO.StatusMappingRowVO> rows,
            Map<String, String> legacyMapping) {
        List<BusinessDocumentConfigDTO.StatusMappingRowDTO> dtoRows = new ArrayList<>();
        for (BusinessDocumentConfigVO.StatusMappingRowVO row : rows) {
            BusinessDocumentConfigDTO.StatusMappingRowDTO dto = new BusinessDocumentConfigDTO.StatusMappingRowDTO();
            dto.setStandardStatus(row.getStandardStatus());
            dto.setStatusValue(row.getStatusValue());
            dto.setDisplayName(row.getDisplayName());
            dto.setTagType(row.getTagType());
            dto.setAllowEdit(row.getAllowEdit());
            dto.setAllowDelete(row.getAllowDelete());
            dto.setAllowStartFlow(row.getAllowStartFlow());
            dtoRows.add(dto);
        }
        return normalizeStatusRows(dtoRows, legacyMapping);
    }

    private Map<String, String> statusMappingFromRows(List<BusinessDocumentConfigVO.StatusMappingRowVO> rows) {
        Map<String, String> result = new LinkedHashMap<>();
        for (BusinessDocumentConfigVO.StatusMappingRowVO row : rows) {
            if (StringUtils.isNotBlank(row.getStandardStatus()) && StringUtils.isNotBlank(row.getStatusValue())) {
                result.put(row.getStandardStatus(), row.getStatusValue());
            }
        }
        return normalizeStatusMapping(result);
    }

    private List<BusinessDocumentConfigVO.StatusMappingRowVO> defaultStatusRows() {
        List<BusinessDocumentConfigVO.StatusMappingRowVO> rows = new ArrayList<>();
        rows.add(statusRow("DRAFT", "草稿", "DRAFT", "草稿", "default", true, true, true));
        rows.add(statusRow("SUBMITTED", "已提交", "SUBMITTED", "已提交", "info", false, false, false));
        rows.add(statusRow("IN_PROCESS", "流程中", "IN_PROCESS", "流程中", "warning", false, false, false));
        rows.add(statusRow("APPROVED", "已通过", "APPROVED", "已通过", "success", false, false, false));
        rows.add(statusRow("REJECTED", "已驳回", "REJECTED", "已驳回", "error", true, false, true));
        rows.add(statusRow("CANCELED", "已撤回", "CANCELED", "已撤回", "default", true, false, true));
        rows.add(statusRow("CLOSED", "已关闭", "CLOSED", "已关闭", "default", false, false, false));
        return rows;
    }

    private BusinessDocumentConfigVO.StatusMappingRowVO statusRow(String standardStatus,
                                                                  String standardLabel,
                                                                  String statusValue,
                                                                  String displayName,
                                                                  String tagType,
                                                                  Boolean allowEdit,
                                                                  Boolean allowDelete,
                                                                  Boolean allowStartFlow) {
        BusinessDocumentConfigVO.StatusMappingRowVO row = new BusinessDocumentConfigVO.StatusMappingRowVO();
        row.setStandardStatus(standardStatus);
        row.setStandardLabel(standardLabel);
        row.setStatusValue(statusValue);
        row.setDisplayName(displayName);
        row.setTagType(tagType);
        row.setAllowEdit(allowEdit);
        row.setAllowDelete(allowDelete);
        row.setAllowStartFlow(allowStartFlow);
        return row;
    }

    private Map<String, Object> buildMainFlowSummary(Long tenantId, String objectCode, String legacyDefaultFlowKey) {
        Map<String, Object> summary = new LinkedHashMap<>();
        AiBusinessBinding binding = selectMainFlowBinding(tenantId, objectCode);
        if (binding != null) {
            Map<String, Object> config = readBindingConfig(binding.getBindingConfig());
            String flowModelKey = StringUtils.defaultIfBlank(resolveFlowModelKey(config), binding.getBindingKey());
            String startMode = normalizeStartMode(text(config.get("startMode")));
            List<?> variableMapping = config.get("variableMapping") instanceof List<?> list ? list : List.of();
            List<String> gaps = new ArrayList<>();
            if (StringUtils.isBlank(flowModelKey)) {
                gaps.add("未配置主流程");
            }
            if (StringUtils.isBlank(startMode)) {
                gaps.add("发起方式未配置");
            }
            if (variableMapping.isEmpty()) {
                gaps.add("变量映射缺失");
            }
            if (Integer.valueOf(0).equals(binding.getStatus())) {
                gaps.add("主流程绑定已停用");
            }
            summary.put("configured", StringUtils.isNotBlank(flowModelKey));
            summary.put("bindingId", binding.getId());
            summary.put("bindingType", binding.getBindingType());
            summary.put("bindingStatus", binding.getStatus());
            summary.put("flowModelKey", flowModelKey);
            summary.put("flowModelName", StringUtils.defaultIfBlank(text(config.get("flowModelName")), binding.getBindingName()));
            summary.put("startMode", startMode);
            summary.put("variableMappingCount", variableMapping.size());
            summary.put("complete", gaps.isEmpty());
            summary.put("gaps", gaps);
            summary.put("compatibilitySource", "APPROVAL".equalsIgnoreCase(binding.getBindingType())
                    ? "LEGACY_APPROVAL_BINDING"
                    : "AI_BUSINESS_BINDING");
            return summary;
        }
        if (StringUtils.isNotBlank(legacyDefaultFlowKey)) {
            summary.put("configured", true);
            summary.put("flowModelKey", legacyDefaultFlowKey);
            summary.put("flowModelName", legacyDefaultFlowKey);
            summary.put("startMode", "MANUAL");
            summary.put("variableMappingCount", 0);
            summary.put("complete", false);
            summary.put("gaps", List.of("历史默认流程缺少变量映射，请在流程与自动化中保存一次主流程"));
            summary.put("compatibilitySource", "DOCUMENT_DEFAULT_FLOW");
            return summary;
        }
        summary.put("configured", false);
        summary.put("complete", false);
        summary.put("gaps", List.of("未配置主流程"));
        summary.put("compatibilitySource", "NONE");
        return summary;
    }

    private AiBusinessBinding selectMainFlowBinding(Long tenantId, String objectCode) {
        if (StringUtils.isBlank(objectCode)) {
            return null;
        }
        Long effectiveTenantId = tenantId != null ? tenantId : resolveTenantId();
        AiBusinessBinding flowBinding = bindingMapper.selectBindingByTypeAndCode(
                effectiveTenantId, "OBJECT", objectCode, "FLOW");
        if (isBindingEnabled(flowBinding)) {
            return flowBinding;
        }
        AiBusinessBinding legacyApprovalBinding = bindingMapper.selectBindingByTypeAndCode(
                effectiveTenantId, "OBJECT", objectCode, "APPROVAL");
        if (isBindingEnabled(legacyApprovalBinding)) {
            return legacyApprovalBinding;
        }
        return flowBinding != null ? flowBinding : legacyApprovalBinding;
    }

    private boolean isBindingEnabled(AiBusinessBinding binding) {
        return binding != null && !Integer.valueOf(0).equals(binding.getStatus());
    }

    private String normalizeStartMode(String startMode) {
        String normalized = StringUtils.defaultIfBlank(startMode, "MANUAL").trim().toUpperCase();
        if ("MANUAL_AND_TRIGGER".equals(normalized) || "MANUAL_TRIGGER".equals(normalized) || "BOTH".equals(normalized)) {
            return "BOTH";
        }
        if ("AUTO".equals(normalized) || "AUTOMATIC".equals(normalized)) {
            return "TRIGGER";
        }
        if ("TRIGGER".equals(normalized)) {
            return "TRIGGER";
        }
        return "MANUAL";
    }

    private void syncLegacyFlowBindingIfNeeded(AiBusinessObject object, String requestedFlowKey, String documentName) {
        if (object == null || StringUtils.isBlank(object.getObjectCode()) || StringUtils.isBlank(requestedFlowKey)) {
            return;
        }
        Long tenantId = resolveTenantId();
        AiBusinessBinding existing = selectMainFlowBinding(tenantId, object.getObjectCode());
        if (existing != null) {
            return;
        }
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("flowModelKey", requestedFlowKey);
        config.put("flowModelName", requestedFlowKey);
        config.put("titleTemplate", StringUtils.defaultIfBlank(documentName, object.getObjectName()) + "-${id}");
        config.put("startMode", "MANUAL");
        config.put("variableMapping", new ArrayList<>());
        config.put("conditionFlows", new ArrayList<>());
        config.put("options", Map.of("compatibilitySource", "DOCUMENT_DEFAULT_FLOW"));

        AiBusinessBinding binding = new AiBusinessBinding();
        binding.setTenantId(tenantId);
        binding.setTargetType("OBJECT");
        binding.setTargetId(object.getId());
        binding.setTargetCode(object.getObjectCode());
        binding.setBindingType("FLOW");
        binding.setBindingKey(requestedFlowKey);
        binding.setBindingName(requestedFlowKey + " 流程");
        binding.setBindingConfig(writeJson(config, "流程绑定兼容配置"));
        binding.setStatus(1);
        binding.setSortOrder(0);
        bindingMapper.insert(binding);
    }

    private Map<String, Object> readBindingConfig(String bindingConfig) {
        if (StringUtils.isBlank(bindingConfig)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(bindingConfig, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String resolveFlowModelKey(Map<String, Object> config) {
        if (config == null) {
            return null;
        }
        return StringUtils.firstNonBlank(
                text(config.get("flowModelKey")),
                text(config.get("flowKey")),
                text(config.get("processDefinitionKey")),
                text(config.get("modelKey"))
        );
    }

    private void validateRequiredField(String label, String field) {
        if (StringUtils.isBlank(field)) {
            throw new BusinessException(label + "不能为空");
        }
    }

    private void validateOptionalObjectField(AiBusinessObject object, String field, String label) {
        if (StringUtils.isBlank(field)) {
            return;
        }
        validateObjectField(object, field, label);
    }

    private void validateObjectField(AiBusinessObject object, String field, String label) {
        Set<String> fields = collectObjectFields(object);
        if (!fields.contains(field)) {
            throw new BusinessException(label + "不存在: " + field);
        }
    }

    private Set<String> collectObjectFields(AiBusinessObject object) {
        Set<String> fields = new LinkedHashSet<>(SYSTEM_FIELDS);
        if (object == null || StringUtils.isBlank(object.getConfigKey())) {
            return fields;
        }
        AiCrudConfig config = crudConfigMapper.selectByConfigKey(resolveTenantId(), object.getConfigKey());
        if (config == null || StringUtils.isBlank(config.getModelSchema())) {
            return fields;
        }
        try {
            LowcodeModelSchema modelSchema = objectMapper.readValue(config.getModelSchema(), LowcodeModelSchema.class);
            if (modelSchema.getFields() == null) {
                return fields;
            }
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field == null) {
                    continue;
                }
                addFieldAlias(fields, field.getField());
                addFieldAlias(fields, field.getColumnName());
            }
        } catch (Exception e) {
            throw new BusinessException("读取业务对象字段失败: " + e.getMessage());
        }
        return fields;
    }

    private void addFieldAlias(Set<String> fields, String field) {
        if (StringUtils.isBlank(field)) {
            return;
        }
        fields.add(field);
        fields.add(snakeToCamel(field));
    }

    private Map<String, String> normalizeStatusMapping(Map<String, String> input) {
        Map<String, String> result = defaultStatusMapping();
        if (input == null) {
            return result;
        }
        input.forEach((key, value) -> {
            if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                result.put(key.trim(), value.trim());
            }
        });
        return result;
    }

    private Map<String, String> defaultStatusMapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("DRAFT", "DRAFT");
        mapping.put("SUBMITTED", "SUBMITTED");
        mapping.put("IN_PROCESS", "IN_PROCESS");
        mapping.put("APPROVED", "APPROVED");
        mapping.put("REJECTED", "REJECTED");
        mapping.put("CANCELED", "CANCELED");
        mapping.put("CLOSED", "CLOSED");
        return mapping;
    }

    private Map<String, String> readStringMap(String json) {
        if (StringUtils.isBlank(json)) {
            return defaultStatusMapping();
        }
        try {
            Map<String, String> value = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
            return normalizeStatusMapping(value);
        } catch (Exception e) {
            return defaultStatusMapping();
        }
    }

    private Map<String, Object> readObjectMap(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private Map<String, Object> readObjectMap(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, itemValue) -> {
                if (key != null) {
                    result.put(String.valueOf(key), itemValue);
                }
            });
            return result;
        }
        try {
            return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String writeJson(Object value, String label) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(label + "格式不正确");
        }
    }

    private String snakeToCamel(String value) {
        if (StringUtils.isBlank(value) || !value.contains("_")) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }

    private String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                result.append('_').append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
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

package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.domain.entity.AiCodeRule;
import com.mdframe.forge.plugin.generator.domain.entity.AiCodeRuleSegment;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRulePreviewDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSaveDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleSegmentDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.CodeRuleStatusDTO;
import com.mdframe.forge.plugin.generator.manager.coderule.CodeRuleDefinition;
import com.mdframe.forge.plugin.generator.manager.coderule.CodeRuleEngine;
import com.mdframe.forge.plugin.generator.manager.coderule.LegacyCodeRuleParser;
import com.mdframe.forge.plugin.generator.mapper.CodeRuleMapper;
import com.mdframe.forge.plugin.generator.mapper.CodeRuleSegmentMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRuleCapabilityVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRuleDetailVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRuleGenerateVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRulePreviewVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.CodeRuleTokenVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 编码规则事务编排与兼容门面。
 */
@Service
@RequiredArgsConstructor
public class CodeRuleService extends ServiceImpl<CodeRuleMapper, AiCodeRule> {

    private static final Pattern RULE_CODE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,63}$");

    private final CodeRuleSegmentMapper segmentMapper;
    private final CodeRuleEngine codeRuleEngine;
    private final LegacyCodeRuleParser legacyParser;

    /**
     * 兼容旧管理入口的实体分页。
     */
    public Page<AiCodeRule> page(Integer pageNum,
                                 Integer pageSize,
                                 String ruleCode,
                                 String ruleName,
                                 String scene,
                                 Integer status) {
        Page<AiCodeRule> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return baseMapper.selectRulePage(
                page,
                resolveTenantId(),
                trimToNull(ruleCode),
                trimToNull(ruleName),
                trimToNull(scene),
                null,
                status
        );
    }

    public Page<CodeRuleDetailVO> pageDetails(Integer pageNum,
                                              Integer pageSize,
                                              String ruleCode,
                                              String ruleName,
                                              String category,
                                              Integer status) {
        Page<CodeRuleDetailVO> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return baseMapper.selectDetailPage(
                page,
                resolveTenantId(),
                trimToNull(ruleCode),
                trimToNull(ruleName),
                trimToNull(category),
                status
        );
    }

    public List<AiCodeRule> listEnabled(String scene) {
        return listEnabled(scene, null);
    }

    public List<AiCodeRule> listEnabled(String scene, String sourceObjectCode) {
        return baseMapper.selectEnabledList(
                resolveTenantId(), trimToNull(scene), trimToNull(sourceObjectCode), true);
    }

    public List<CodeRuleDetailVO> listSelectable(String scene, String sourceObjectCode) {
        return baseMapper.selectEnabledList(
                        resolveTenantId(), trimToNull(scene), trimToNull(sourceObjectCode), true).stream()
                .map(this::toSummaryVO)
                .toList();
    }

    public AiCodeRule detail(Long id) {
        return requireRule(id, resolveTenantId());
    }

    public CodeRuleDetailVO detailVO(Long id) {
        Long tenantId = resolveTenantId();
        AiCodeRule rule = requireRule(id, tenantId);
        List<AiCodeRuleSegment> entities = segmentMapper.selectByRuleId(tenantId, id);
        List<CodeRuleSegmentDTO> segments;
        CodeRuleDetailVO result = toSummaryVO(rule);
        if (entities.isEmpty()) {
            segments = legacyParser.parse(rule.getTemplate(), rule.getResetPolicy(), rule.getSeqLength());
            result.setCompatibilityMode("LEGACY");
            result.getWarnings().add("该规则仍使用历史模板，保存后会物化为结构化分段");
        } else {
            segments = entities.stream().map(this::toSegmentDTO).toList();
            result.setCompatibilityMode("STRUCTURED");
        }
        result.setSegments(new ArrayList<>(segments));
        result.setSegmentCount(segments.size());
        return result;
    }

    /**
     * 兼容旧实体新增入口，内部仍转为结构化 DTO。
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(AiCodeRule rule) {
        if (rule == null) {
            throw new BusinessException("编码规则不能为空");
        }
        create(toSaveDTO(rule, null));
    }

    /**
     * 兼容旧实体修改入口。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(AiCodeRule rule) {
        if (rule == null || rule.getId() == null) {
            throw new BusinessException("编码规则ID不能为空");
        }
        CodeRuleDetailVO current = detailVO(rule.getId());
        update(toSaveDTO(rule, current));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(CodeRuleSaveDTO dto) {
        CodeRuleSaveDTO source = normalizeAndValidateSave(dto, true);
        Long tenantId = resolveTenantId();
        validateUniqueRuleCode(tenantId, source.getRuleCode(), null);
        CodeRuleDefinition definition = toDefinition(null, tenantId, source.getRuleCode(), source.getRuleName(), source.getSegments());
        codeRuleEngine.validate(definition);

        AiCodeRule rule = toEntity(source);
        rule.setTenantId(tenantId);
        rule.setBuiltin(0);
        rule.setVersionNo(1);
        rule.setDelFlag("0");
        syncLegacySummary(rule, source.getSegments());
        if (!save(rule)) {
            throw new BusinessException("新增编码规则失败");
        }
        insertSegments(rule.getId(), tenantId, source.getSegments());
        return rule.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(CodeRuleSaveDTO dto) {
        CodeRuleSaveDTO source = normalizeAndValidateSave(dto, false);
        Long tenantId = resolveTenantId();
        AiCodeRule existing = requireRule(source.getId(), tenantId);
        if (!Objects.equals(existing.getRuleCode(), source.getRuleCode())) {
            throw new BusinessException("规则编码创建后不能修改");
        }
        if (!Objects.equals(existing.getVersionNo(), source.getVersionNo())) {
            throw new BusinessException("编码规则已被其他用户修改，请刷新后重试");
        }

        if (Integer.valueOf(1).equals(existing.getBuiltin())) {
            assertBuiltinFieldsUnchanged(existing, source);
            updateBuiltin(existing, source);
            return;
        }

        validateUniqueRuleCode(tenantId, source.getRuleCode(), source.getId());
        CodeRuleDefinition definition = toDefinition(
                source.getId(), tenantId, source.getRuleCode(), source.getRuleName(), source.getSegments());
        codeRuleEngine.validate(definition);

        AiCodeRule update = toEntity(source);
        update.setTenantId(tenantId);
        update.setBuiltin(existing.getBuiltin());
        update.setVersionNo(source.getVersionNo());
        syncLegacySummary(update, source.getSegments());
        if (!updateById(update)) {
            throw new BusinessException("编码规则已被其他用户修改，请刷新后重试");
        }
        replaceSegments(source.getId(), tenantId, source.getSegments());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        AiCodeRule existing = requireRule(id, resolveTenantId());
        CodeRuleStatusDTO dto = new CodeRuleStatusDTO();
        dto.setId(id);
        dto.setStatus(status);
        dto.setVersionNo(existing.getVersionNo());
        updateStatus(dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(CodeRuleStatusDTO dto) {
        if (dto == null || dto.getId() == null || dto.getVersionNo() == null) {
            throw new BusinessException("规则ID和版本号不能为空");
        }
        Long tenantId = resolveTenantId();
        AiCodeRule existing = requireRule(dto.getId(), tenantId);
        if (!Objects.equals(existing.getVersionNo(), dto.getVersionNo())) {
            throw new BusinessException("编码规则已被其他用户修改，请刷新后重试");
        }
        AiCodeRule update = new AiCodeRule();
        update.setId(existing.getId());
        update.setTenantId(tenantId);
        update.setStatus(Integer.valueOf(1).equals(dto.getStatus()) ? 1 : 0);
        update.setVersionNo(dto.getVersionNo());
        if (!updateById(update)) {
            throw new BusinessException("编码规则已被其他用户修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long tenantId = resolveTenantId();
        AiCodeRule existing = requireRule(id, tenantId);
        if (Integer.valueOf(1).equals(existing.getBuiltin())) {
            throw new BusinessException("内置编码规则不能删除，可停用后新建自定义规则");
        }
        segmentMapper.logicalDeleteByRuleId(tenantId, id, currentUserId());
        if (!removeById(id)) {
            throw new BusinessException("删除编码规则失败");
        }
    }

    public CodeRulePreviewVO preview(CodeRulePreviewDTO dto) {
        CodeRulePreviewDTO source = dto == null ? new CodeRulePreviewDTO() : dto;
        Long tenantId = resolveTenantId();
        CodeRuleDefinition definition;
        if (source.getSegments() != null && !source.getSegments().isEmpty()) {
            definition = toDefinition(source.getId(), tenantId,
                    StringUtils.defaultIfBlank(source.getRuleCode(), "preview_rule"),
                    StringUtils.defaultIfBlank(source.getRuleName(), "编码规则预览"),
                    source.getSegments());
        } else if (StringUtils.isNotBlank(source.getRuleCode())) {
            AiCodeRule rule = requireRuleByCode(source.getRuleCode(), tenantId, false);
            definition = loadDefinition(rule, tenantId);
            if (StringUtils.isNotBlank(source.getTemplate())) {
                definition.setSegments(legacyParser.parse(source.getTemplate(), rule.getResetPolicy(), rule.getSeqLength()));
            }
        } else {
            definition = toDefinition(null, tenantId, "preview_rule", "编码规则预览",
                    legacyParser.parse(StringUtils.defaultIfBlank(source.getTemplate(), "CODE${yyyyMMdd}${seq:4}"), "DAY", 4));
        }
        return codeRuleEngine.preview(
                definition,
                mergePreviewFields(source),
                trustedSystemVariables(tenantId, true),
                source.getSequence() == null ? null : source.getSequence().longValue()
        );
    }

    public String generate(String ruleCode, Map<String, Object> context) {
        return generateResult(ruleCode, extractBusinessFields(context)).getCode();
    }

    public CodeRuleGenerateVO generateResult(String ruleCode, Map<String, Object> fields) {
        Long tenantId = resolveTenantId();
        AiCodeRule rule = requireRuleByCode(ruleCode, tenantId, true);
        Map<String, Object> businessFields = extractBusinessFields(fields);
        validateSourceObjectContext(rule, businessFields);
        return codeRuleEngine.generate(
                loadDefinition(rule, tenantId),
                businessFields,
                trustedSystemVariables(tenantId, false)
        );
    }

    public CodeRuleCapabilityVO capabilities() {
        CodeRuleCapabilityVO result = new CodeRuleCapabilityVO();
        result.setSegmentTypes(List.of(
                option("日期", "DATE", "使用服务端白名单日期格式"),
                option("固定值", "FIXED", "输出固定前缀、后缀或分隔符"),
                option("流水号（顺序递增）", "SEQ", "每条规则最多一个，按取号顺序递增并支持进制和周期"),
                option("业务变量", "VARIABLE", "从当前业务记录 fields 中读取"),
                option("系统变量", "SYS_VAR", "只从可信登录与租户上下文读取")
        ));
        result.setDateFormats(List.of(
                option("年", "yyyy", "2026"),
                option("年月", "yyyyMM", "202607"),
                option("年月日", "yyyyMMdd", "20260716"),
                option("年月日时", "yyyyMMddHH", "2026071617"),
                option("年月日时分", "yyyyMMddHHmm", "202607161730"),
                option("年月日时分秒", "yyyyMMddHHmmss", "20260716173021"),
                option("时分秒", "HHmmss", "173021")
        ));
        result.setRadixTypes(List.of(
                option("十进制", "DECIMAL", "0-9"),
                option("十六进制", "HEX", "0-9、A-F"),
                option("大写字母", "ALPHA_UPPER", "A-Z"),
                option("小写字母", "ALPHA_LOWER", "a-z"),
                option("数字与大写字母", "ALPHANUMERIC", "0-9、A-Z")
        ));
        result.setResetPolicies(List.of(
                option("不重置", "NONE", "全局持续递增"),
                option("按年", "YEAR", "每年使用独立计数键"),
                option("按月", "MONTH", "每月使用独立计数键"),
                option("按日", "DAY", "每日使用独立计数键"),
                option("按时", "HOUR", "每小时使用独立计数键")
        ));
        result.setSystemVariables(List.of(
                option("租户ID", "tenantId", "当前租户"),
                option("用户ID", "userId", "当前登录用户"),
                option("用户名", "username", "当前登录用户名"),
                option("部门ID", "deptId", "当前主组织"),
                option("组织ID", "orgId", "当前活动组织"),
                option("部门编码", "deptCode", "当前主组织ID兼容值"),
                option("组织编码", "orgCode", "当前活动组织ID兼容值"),
                option("发起人", "starter", "当前登录用户名")
        ));
        result.setVariableSources(List.of(
                option("自定义变量", "CUSTOM", "由业务代码通过 fields 传入"),
                option("低代码字段", "LOWCODE", "映射当前低代码业务对象的字段")
        ));
        return result;
    }

    public List<CodeRuleTokenVO> listTokens() {
        List<CodeRuleTokenVO> tokens = new ArrayList<>();
        tokens.add(token("${yyyy}", "年份", "日期时间", "当前年份，四位数字", "2026"));
        tokens.add(token("${yyyyMM}", "年月", "日期时间", "当前年月，六位数字", "202607"));
        tokens.add(token("${yyyyMMdd}", "年月日", "日期时间", "当前日期，八位数字", "20260716"));
        tokens.add(token("${yyyyMMddHHmmss}", "年月日时分秒", "日期时间", "当前时间到秒", "20260716173021"));
        tokens.add(token("${seq:4}", "四位流水号", "序列", "流水号左侧补零到指定长度", "0001"));
        tokens.add(token("${tenantId}", "租户ID", "系统变量", "当前可信租户ID", "1"));
        tokens.add(token("${userId}", "用户ID", "系统变量", "当前可信登录用户ID", "10001"));
        tokens.add(token("${username}", "用户名", "系统变量", "当前可信登录用户名", "zhangsan"));
        tokens.add(token("${field:<fieldCode>}", "业务字段", "业务字段", "从 fields 读取业务字段", "RAW"));
        return tokens;
    }

    private CodeRuleSaveDTO normalizeAndValidateSave(CodeRuleSaveDTO dto, boolean creating) {
        if (dto == null) {
            throw new BusinessException("编码规则不能为空");
        }
        if (!creating && (dto.getId() == null || dto.getVersionNo() == null)) {
            throw new BusinessException("规则ID和版本号不能为空");
        }
        dto.setRuleCode(StringUtils.trimToNull(dto.getRuleCode()));
        dto.setRuleName(StringUtils.trimToNull(dto.getRuleName()));
        dto.setScene(StringUtils.defaultIfBlank(StringUtils.trimToNull(dto.getScene()), "COMMON"));
        dto.setCategory(StringUtils.defaultIfBlank(StringUtils.trimToNull(dto.getCategory()), "COMMON"));
        dto.setStatus(Integer.valueOf(0).equals(dto.getStatus()) ? 0 : 1);
        dto.setInCodeList(Integer.valueOf(0).equals(dto.getInCodeList()) ? 0 : 1);
        dto.setRemark(StringUtils.trimToNull(dto.getRemark()));
        if (StringUtils.isBlank(dto.getRuleCode()) || !RULE_CODE_PATTERN.matcher(dto.getRuleCode()).matches()) {
            throw new BusinessException("规则编码必须以字母开头，且只能包含字母、数字和下划线");
        }
        if (StringUtils.isBlank(dto.getRuleName())) {
            throw new BusinessException("规则名称不能为空");
        }
        if (dto.getSegments() == null || dto.getSegments().isEmpty()) {
            throw new BusinessException("编码规则至少需要一个分段");
        }
        for (int index = 0; index < dto.getSegments().size(); index++) {
            CodeRuleSegmentDTO segment = dto.getSegments().get(index);
            if (segment != null && StringUtils.isBlank(segment.getSegmentKey())) {
                segment.setSegmentKey("segment_" + (index + 1) + "_" + Long.toUnsignedString(IdWorker.getId(), 36));
            }
            if (segment != null) {
                segment.setSegmentOrder(index + 1);
                segment.setVariableSource(StringUtils.upperCase(
                        StringUtils.defaultIfBlank(segment.getVariableSource(), "CUSTOM")));
            }
        }
        boolean hasLowCodeVariable = dto.getSegments().stream()
                .filter(Objects::nonNull)
                .anyMatch(segment -> "VARIABLE".equalsIgnoreCase(segment.getSegmentType())
                        && "LOWCODE".equalsIgnoreCase(segment.getVariableSource()));
        if (!hasLowCodeVariable) {
            dto.setSourceObjectId(null);
            dto.setSourceObjectCode(null);
        } else if (dto.getSourceObjectId() == null || StringUtils.isBlank(dto.getSourceObjectCode())) {
            throw new BusinessException("低代码业务变量必须绑定来源业务对象");
        }
        return dto;
    }

    private void validateUniqueRuleCode(Long tenantId, String ruleCode, Long excludeId) {
        if (baseMapper.countByRuleCode(tenantId, ruleCode, excludeId) > 0) {
            throw new BusinessException("规则编码已存在");
        }
    }

    private void updateBuiltin(AiCodeRule existing, CodeRuleSaveDTO source) {
        AiCodeRule update = new AiCodeRule();
        update.setId(existing.getId());
        update.setTenantId(existing.getTenantId());
        update.setRuleName(source.getRuleName());
        update.setRemark(source.getRemark());
        update.setStatus(source.getStatus());
        update.setVersionNo(source.getVersionNo());
        if (!updateById(update)) {
            throw new BusinessException("编码规则已被其他用户修改，请刷新后重试");
        }
    }

    private void assertBuiltinFieldsUnchanged(AiCodeRule existing, CodeRuleSaveDTO source) {
        if (!Objects.equals(existing.getScene(), source.getScene())
                || !Objects.equals(existing.getCategory(), source.getCategory())
                || !Objects.equals(existing.getSourceObjectId(), source.getSourceObjectId())
                || !Objects.equals(existing.getSourceObjectCode(), source.getSourceObjectCode())
                || !Objects.equals(existing.getInCodeList(), source.getInCodeList())) {
            throw new BusinessException("内置规则只允许修改名称、说明和状态");
        }
        List<CodeRuleSegmentDTO> persisted = detailVO(existing.getId()).getSegments();
        if (!segmentsEquivalent(persisted, source.getSegments())) {
            throw new BusinessException("内置规则的编码分段不能修改");
        }
    }

    private boolean segmentsEquivalent(List<CodeRuleSegmentDTO> left, List<CodeRuleSegmentDTO> right) {
        if (left == null || right == null || left.size() != right.size()) {
            return false;
        }
        List<CodeRuleSegmentDTO> orderedLeft = left.stream()
                .sorted(Comparator.comparing(CodeRuleSegmentDTO::getSegmentOrder)).toList();
        List<CodeRuleSegmentDTO> orderedRight = right.stream()
                .sorted(Comparator.comparing(CodeRuleSegmentDTO::getSegmentOrder)).toList();
        for (int index = 0; index < orderedLeft.size(); index++) {
            if (!segmentFingerprint(orderedLeft.get(index)).equals(segmentFingerprint(orderedRight.get(index)))) {
                return false;
            }
        }
        return true;
    }

    private String segmentFingerprint(CodeRuleSegmentDTO value) {
        return String.join("|",
                text(value.getSegmentKey()), text(value.getSegmentType()), text(value.getSegmentValue()),
                text(value.getVariableSource()),
                text(value.getSegmentLength()), text(value.getPadEnabled()), text(value.getPadChar()),
                text(value.getPadDirection()), text(value.getGroupEnabled()), text(value.getIncludeInCode()),
                text(value.getRadixType()), text(value.getResetEnabled()), text(value.getResetPolicy()),
                text(value.getStartValue()), text(value.getExcludeAmbiguous()));
    }

    private void replaceSegments(Long ruleId, Long tenantId, List<CodeRuleSegmentDTO> segments) {
        segmentMapper.logicalDeleteByRuleId(tenantId, ruleId, currentUserId());
        insertSegments(ruleId, tenantId, segments);
    }

    private void insertSegments(Long ruleId, Long tenantId, List<CodeRuleSegmentDTO> segments) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = currentUserId();
        Long deptId = currentDeptId();
        List<AiCodeRuleSegment> entities = new ArrayList<>();
        for (CodeRuleSegmentDTO dto : segments) {
            AiCodeRuleSegment entity = toSegmentEntity(dto);
            entity.setId(IdWorker.getId());
            entity.setTenantId(tenantId);
            entity.setRuleId(ruleId);
            entity.setDelFlag("0");
            entity.setCreateBy(userId);
            entity.setCreateDept(deptId);
            entity.setCreateTime(now);
            entity.setUpdateBy(userId);
            entity.setUpdateTime(now);
            entities.add(entity);
        }
        if (!entities.isEmpty() && segmentMapper.insertBatch(entities) != entities.size()) {
            throw new BusinessException("保存编码规则分段失败");
        }
    }

    private CodeRuleDefinition loadDefinition(AiCodeRule rule, Long tenantId) {
        List<AiCodeRuleSegment> stored = segmentMapper.selectByRuleId(tenantId, rule.getId());
        List<CodeRuleSegmentDTO> segments = stored.isEmpty()
                ? legacyParser.parse(rule.getTemplate(), rule.getResetPolicy(), rule.getSeqLength())
                : stored.stream().map(this::toSegmentDTO).toList();
        return toDefinition(rule.getId(), tenantId, rule.getRuleCode(), rule.getRuleName(), segments);
    }

    private CodeRuleDefinition toDefinition(Long ruleId,
                                            Long tenantId,
                                            String ruleCode,
                                            String ruleName,
                                            List<CodeRuleSegmentDTO> segments) {
        CodeRuleDefinition definition = new CodeRuleDefinition();
        definition.setRuleId(ruleId);
        definition.setTenantId(tenantId);
        definition.setRuleCode(ruleCode);
        definition.setRuleName(ruleName);
        definition.setSegments(new ArrayList<>(segments));
        return definition;
    }

    private AiCodeRule requireRule(Long id, Long tenantId) {
        if (id == null) {
            throw new BusinessException("编码规则ID不能为空");
        }
        AiCodeRule rule = baseMapper.selectByRuleId(tenantId, id);
        if (rule == null) {
            throw new BusinessException("编码规则不存在");
        }
        return rule;
    }

    private AiCodeRule requireRuleByCode(String ruleCode, Long tenantId, boolean enabledOnly) {
        String normalized = StringUtils.trimToNull(ruleCode);
        if (normalized == null) {
            throw new BusinessException("自动编号未选择编码规则");
        }
        AiCodeRule rule = baseMapper.selectByRuleCode(tenantId, normalized);
        if (rule == null) {
            throw new BusinessException("编码规则不存在: " + normalized);
        }
        if (enabledOnly && !Integer.valueOf(1).equals(rule.getStatus())) {
            throw new BusinessException("编码规则已停用: " + normalized);
        }
        return rule;
    }

    private void syncLegacySummary(AiCodeRule rule, List<CodeRuleSegmentDTO> segments) {
        StringBuilder template = new StringBuilder();
        CodeRuleSegmentDTO sequence = null;
        for (CodeRuleSegmentDTO segment : segments) {
            if (!Integer.valueOf(1).equals(segment.getIncludeInCode())) {
                continue;
            }
            template.append(switch (segment.getSegmentType()) {
                case "FIXED" -> StringUtils.defaultString(segment.getSegmentValue());
                case "DATE" -> "${" + segment.getSegmentValue() + "}";
                case "SEQ" -> "${seq:" + segment.getSegmentLength() + "}";
                case "VARIABLE" -> "${field:" + segment.getSegmentValue() + "}";
                case "SYS_VAR" -> "${" + segment.getSegmentValue() + "}";
                default -> "";
            });
            if ("SEQ".equals(segment.getSegmentType())) {
                sequence = segment;
            }
        }
        rule.setTemplate(template.toString());
        rule.setSeqLength(sequence == null ? 4 : sequence.getSegmentLength());
        rule.setResetPolicy(sequence == null ? "NONE" : sequence.getResetPolicy());
        rule.setOptions(null);
    }

    private AiCodeRule toEntity(CodeRuleSaveDTO dto) {
        AiCodeRule rule = new AiCodeRule();
        rule.setId(dto.getId());
        rule.setRuleCode(dto.getRuleCode());
        rule.setRuleName(dto.getRuleName());
        rule.setScene(dto.getScene());
        rule.setCategory(dto.getCategory());
        rule.setSourceObjectId(dto.getSourceObjectId());
        rule.setSourceObjectCode(dto.getSourceObjectCode());
        rule.setStatus(dto.getStatus());
        rule.setInCodeList(dto.getInCodeList());
        rule.setVersionNo(dto.getVersionNo());
        rule.setRemark(dto.getRemark());
        return rule;
    }

    private CodeRuleSaveDTO toSaveDTO(AiCodeRule rule, CodeRuleDetailVO current) {
        CodeRuleSaveDTO dto = new CodeRuleSaveDTO();
        dto.setId(rule.getId());
        dto.setVersionNo(rule.getVersionNo() == null && current != null ? current.getVersionNo() : rule.getVersionNo());
        dto.setRuleCode(StringUtils.defaultIfBlank(rule.getRuleCode(), current == null ? null : current.getRuleCode()));
        dto.setRuleName(StringUtils.defaultIfBlank(rule.getRuleName(), current == null ? null : current.getRuleName()));
        dto.setScene(StringUtils.defaultIfBlank(rule.getScene(), current == null ? "COMMON" : current.getScene()));
        dto.setCategory(StringUtils.defaultIfBlank(rule.getCategory(), current == null ? "COMMON" : current.getCategory()));
        dto.setSourceObjectId(rule.getSourceObjectId() == null && current != null
                ? current.getSourceObjectId() : rule.getSourceObjectId());
        dto.setSourceObjectCode(StringUtils.defaultIfBlank(rule.getSourceObjectCode(),
                current == null ? null : current.getSourceObjectCode()));
        dto.setStatus(rule.getStatus() == null && current != null ? current.getStatus() : rule.getStatus());
        dto.setInCodeList(rule.getInCodeList() == null && current != null ? current.getInCodeList() : rule.getInCodeList());
        dto.setRemark(rule.getRemark() == null && current != null ? current.getRemark() : rule.getRemark());
        if (StringUtils.isNotBlank(rule.getTemplate())) {
            dto.setSegments(legacyParser.parse(rule.getTemplate(), rule.getResetPolicy(), rule.getSeqLength()));
        } else if (current != null) {
            dto.setSegments(new ArrayList<>(current.getSegments()));
        }
        return dto;
    }

    private CodeRuleDetailVO toSummaryVO(AiCodeRule rule) {
        CodeRuleDetailVO vo = new CodeRuleDetailVO();
        vo.setId(rule.getId());
        vo.setTenantId(rule.getTenantId());
        vo.setRuleCode(rule.getRuleCode());
        vo.setRuleName(rule.getRuleName());
        vo.setScene(rule.getScene());
        vo.setCategory(rule.getCategory());
        vo.setSourceObjectId(rule.getSourceObjectId());
        vo.setSourceObjectCode(rule.getSourceObjectCode());
        vo.setTemplate(rule.getTemplate());
        vo.setStatus(rule.getStatus());
        vo.setBuiltin(rule.getBuiltin());
        vo.setInCodeList(rule.getInCodeList());
        vo.setVersionNo(rule.getVersionNo());
        vo.setRemark(rule.getRemark());
        vo.setCreateTime(rule.getCreateTime());
        vo.setUpdateTime(rule.getUpdateTime());
        return vo;
    }

    private CodeRuleSegmentDTO toSegmentDTO(AiCodeRuleSegment entity) {
        CodeRuleSegmentDTO dto = new CodeRuleSegmentDTO();
        dto.setSegmentKey(entity.getSegmentKey());
        dto.setSegmentOrder(entity.getSegmentOrder());
        dto.setSegmentType(entity.getSegmentType());
        dto.setSegmentValue(entity.getSegmentValue());
        dto.setVariableSource(entity.getVariableSource());
        dto.setSegmentLength(entity.getSegmentLength());
        dto.setPadEnabled(entity.getPadEnabled());
        dto.setPadChar(entity.getPadChar());
        dto.setPadDirection(entity.getPadDirection());
        dto.setGroupEnabled(entity.getGroupEnabled());
        dto.setIncludeInCode(entity.getIncludeInCode());
        dto.setRadixType(entity.getRadixType());
        dto.setResetEnabled(entity.getResetEnabled());
        dto.setResetPolicy(entity.getResetPolicy());
        dto.setStartValue(entity.getStartValue());
        dto.setExcludeAmbiguous(entity.getExcludeAmbiguous());
        return dto;
    }

    private AiCodeRuleSegment toSegmentEntity(CodeRuleSegmentDTO dto) {
        AiCodeRuleSegment entity = new AiCodeRuleSegment();
        entity.setSegmentKey(dto.getSegmentKey());
        entity.setSegmentOrder(dto.getSegmentOrder());
        entity.setSegmentType(dto.getSegmentType());
        entity.setSegmentValue(dto.getSegmentValue());
        entity.setVariableSource(StringUtils.defaultIfBlank(dto.getVariableSource(), "CUSTOM"));
        entity.setSegmentLength(dto.getSegmentLength());
        entity.setPadEnabled(dto.getPadEnabled());
        entity.setPadChar(dto.getPadChar());
        entity.setPadDirection(dto.getPadDirection());
        entity.setGroupEnabled(dto.getGroupEnabled());
        entity.setIncludeInCode(dto.getIncludeInCode());
        entity.setRadixType(dto.getRadixType());
        entity.setResetEnabled(dto.getResetEnabled());
        entity.setResetPolicy(dto.getResetPolicy());
        entity.setStartValue(dto.getStartValue());
        entity.setExcludeAmbiguous(dto.getExcludeAmbiguous());
        return entity;
    }

    private Map<String, Object> mergePreviewFields(CodeRulePreviewDTO dto) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.putAll(extractBusinessFields(dto.getContext()));
        result.putAll(extractBusinessFields(dto.getSampleData()));
        result.putAll(extractBusinessFields(dto.getFields()));
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractBusinessFields(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (String nestedKey : List.of("recordData", "record", "data", "sampleData", "fields")) {
            Object nested = source.get(nestedKey);
            if (nested instanceof Map<?, ?> nestedMap) {
                result.putAll((Map<String, Object>) nestedMap);
            }
        }
        source.forEach((key, value) -> {
            if (!(value instanceof Map<?, ?>)) {
                result.put(key, value);
            }
        });
        return result;
    }

    private void validateSourceObjectContext(AiCodeRule rule, Map<String, Object> fields) {
        if (rule == null || StringUtils.isBlank(rule.getSourceObjectCode())) {
            return;
        }
        Object objectCodeValue = fields == null ? null : fields.get("objectCode");
        String actualObjectCode = objectCodeValue == null ? null : StringUtils.trimToNull(String.valueOf(objectCodeValue));
        if (StringUtils.isBlank(actualObjectCode)) {
            throw new BusinessException("编码规则已绑定业务对象，生成上下文缺少objectCode");
        }
        if (!StringUtils.equals(rule.getSourceObjectCode(), actualObjectCode)) {
            throw new BusinessException("编码规则绑定的业务对象与当前低代码对象不一致");
        }
    }

    private Map<String, Object> trustedSystemVariables(Long tenantId, boolean previewMode) {
        LoginUser user = safeLoginUser();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("tenantId", tenantId);
        if (user != null) {
            values.put("userId", user.getUserId());
            values.put("username", StringUtils.firstNonBlank(user.getUsername(), user.getRealName()));
            values.put("deptId", user.getMainOrgId());
            values.put("orgId", user.getActiveOrgId() == null ? user.getMainOrgId() : user.getActiveOrgId());
            values.put("deptCode", user.getMainOrgId());
            values.put("orgCode", user.getActiveOrgId() == null ? user.getMainOrgId() : user.getActiveOrgId());
            values.put("starter", StringUtils.firstNonBlank(user.getUsername(), user.getRealName()));
        } else if (previewMode) {
            values.put("userId", 10001L);
            values.put("username", "demo");
            values.put("deptId", 100L);
            values.put("orgId", 100L);
            values.put("deptCode", "100");
            values.put("orgCode", "100");
            values.put("starter", "demo");
        }
        values.values().removeIf(Objects::isNull);
        return values;
    }

    private LoginUser safeLoginUser() {
        try {
            return SessionHelper.getLoginUser();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long resolveTenantId() {
        Long tenantId = null;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception ignored) {
            // 后台任务可由 TenantContextHolder 提供租户。
        }
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("缺少有效租户上下文");
        }
        return tenantId;
    }

    private Long currentUserId() {
        try {
            return SessionHelper.getUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long currentDeptId() {
        try {
            return SessionHelper.getActiveOrgId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private CodeRuleCapabilityVO.OptionVO option(String label, String value, String description) {
        return new CodeRuleCapabilityVO.OptionVO(label, value, description);
    }

    private CodeRuleTokenVO token(String insertText,
                                  String label,
                                  String groupName,
                                  String description,
                                  String example) {
        CodeRuleTokenVO vo = new CodeRuleTokenVO();
        vo.setToken(insertText);
        vo.setInsertText(insertText);
        vo.setLabel(label);
        vo.setGroupName(groupName);
        vo.setDescription(description);
        vo.setExample(example);
        vo.setSampleValue(example);
        return vo;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
    }

    private String trimToNull(String value) {
        return StringUtils.trimToNull(value);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}

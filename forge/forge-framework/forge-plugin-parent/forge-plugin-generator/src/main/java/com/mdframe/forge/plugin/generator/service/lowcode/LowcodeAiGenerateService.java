package com.mdframe.forge.plugin.generator.service.lowcode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeDomain;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAiAgentStepDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAiAppGenerateRequest;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAiAppGenerateResult;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAiDecisionDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAiDomainDraftDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAppDraftDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDataModelDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeObjectSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTreeConfig;
import com.mdframe.forge.plugin.generator.service.AiClientAdapter;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 低代码 AI 应用生成编排。先尝试复用既有 AI 调用链，失败时回落到 Agent 形态规则规划。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LowcodeAiGenerateService {

    private static final String AGENT_CODE = "lowcode_system_generator";
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String SIMPLE_LAYOUT = "simple-crud";
    private static final String TREE_LAYOUT = "tree-crud";
    private static final int MAX_OBJECTS = 6;
    private static final Pattern TABLE_PREFIX_PATTERN = Pattern.compile("(?:表名|数据表|表).*?(?:以|前缀为|前缀是|前缀)\\s*([A-Za-z][A-Za-z0-9_]*_)");
    private static final Pattern TABLE_PREFIX_FALLBACK_PATTERN = Pattern.compile("([A-Za-z][A-Za-z0-9_]*_)\\s*开头");

    private static final List<DomainCandidate> DOMAIN_CANDIDATES = List.of(
            new DomainCandidate("crm", "客户关系", "客户、合同、商机、回款等客户经营业务", Set.of("客户", "合同", "商机", "线索", "回款", "拜访", "销售")),
            new DomainCandidate("scm", "供应链", "商品、采购、库存、供应商、仓储等供应链业务", Set.of("商品", "采购", "库存", "供应商", "仓库", "入库", "出库")),
            new DomainCandidate("finance", "财务", "发票、账单、费用、付款和预算等财务业务", Set.of("发票", "账单", "付款", "收款", "费用", "预算", "结算")),
            new DomainCandidate("hr", "人力资源", "员工、部门、考勤、请假和薪资等组织人事业务", Set.of("员工", "部门", "岗位", "考勤", "请假", "薪资", "招聘")),
            new DomainCandidate("project", "项目管理", "项目、任务、里程碑、工时和缺陷等项目协作业务", Set.of("项目", "任务", "里程碑", "工时", "缺陷", "迭代")),
            new DomainCandidate("ops", "运营管理", "活动、工单、审批、计划和台账等运营业务", Set.of("活动", "工单", "审批", "计划", "台账", "巡检"))
    );

    private static final List<ObjectCandidate> OBJECT_CANDIDATES = List.of(
            new ObjectCandidate("customer", "客户", "crm", Set.of("客户", "客户档案", "客户管理"), false),
            new ObjectCandidate("contact", "联系人", "crm", Set.of("联系人", "客户联系人"), false),
            new ObjectCandidate("opportunity", "商机", "crm", Set.of("商机", "销售机会"), false),
            new ObjectCandidate("contract", "合同", "crm", Set.of("合同", "协议"), false),
            new ObjectCandidate("payment", "回款", "crm", Set.of("回款", "收款"), false),
            new ObjectCandidate("product", "商品", "scm", Set.of("商品", "产品", "物料"), false),
            new ObjectCandidate("supplier", "供应商", "scm", Set.of("供应商", "供货商"), false),
            new ObjectCandidate("purchase_order", "采购订单", "scm", Set.of("采购订单", "采购单", "采购"), false),
            new ObjectCandidate("sales_order", "销售订单", "scm", Set.of("销售订单", "销售单", "订单"), false),
            new ObjectCandidate("inventory", "库存", "scm", Set.of("库存", "仓储", "入库", "出库"), false),
            new ObjectCandidate("invoice", "发票", "finance", Set.of("发票", "开票"), false),
            new ObjectCandidate("expense", "费用", "finance", Set.of("费用", "报销"), false),
            new ObjectCandidate("employee", "员工", "hr", Set.of("员工", "人员", "职员"), false),
            new ObjectCandidate("department", "部门", "hr", Set.of("部门", "组织架构", "组织树"), true),
            new ObjectCandidate("leave_request", "请假申请", "hr", Set.of("请假", "休假"), false),
            new ObjectCandidate("project", "项目", "project", Set.of("项目"), false),
            new ObjectCandidate("task", "任务", "project", Set.of("任务", "待办"), false),
            new ObjectCandidate("work_order", "工单", "ops", Set.of("工单", "服务单"), false),
            new ObjectCandidate("category", "分类", "ops", Set.of("分类", "类目", "目录", "树形"), true)
    );

    private final ObjectMapper objectMapper;
    private final LowcodeDomainService domainService;
    private final LowcodeRuntimeConfigBuilder runtimeConfigBuilder;
    private final AiClientAdapter aiClientAdapter;
    private final LowcodeModelSchemaNormalizer schemaNormalizer;
    private final LowcodePolicyService policyService;

    public LowcodeAiAppGenerateResult generateAppDraft(LowcodeAiAppGenerateRequest request) {
        validateRequest(request);
        LowcodeAiAppGenerateResult aiResult = tryGenerateWithAi(request);
        if (aiResult != null) {
            return aiResult;
        }
        return buildRuleAgentResult(request);
    }

    public Flux<ServerSentEvent<String>> streamGenerateApp(LowcodeAiAppGenerateRequest request) {
        validateRequest(request);
        Map<String, String> contextVars = buildContextVars(request);
        String message = buildAgentMessage(request);
        StringBuilder aiContent = new StringBuilder();
        AtomicReference<String> stageRef = new AtomicReference<>("analyzing");

        Flux<ServerSentEvent<String>> aiStream = aiClientAdapter.stream(
                        request.getDescription(),
                        request.getSessionId(),
                        AGENT_CODE,
                        message,
                        contextVars,
                        request.getProviderId(),
                        request.getModelId(),
                        request.getTemperature() == null ? null : request.getTemperature().doubleValue(),
                        request.getMaxTokens())
                .concatMap(chunk -> {
                    if (StringUtils.isBlank(chunk)) {
                        return Flux.empty();
                    }
                    aiContent.append(chunk);
                    List<ServerSentEvent<String>> events = new ArrayList<>();
                    Map<String, Object> stageProgress = detectStreamStage(aiContent.toString(), stageRef);
                    if (stageProgress != null) {
                        events.add(event("progress", stageProgress));
                    }
                    events.add(event("chunk", Map.of("content", chunk)));
                    return Flux.fromIterable(events);
                })
                .onErrorResume(e -> {
                    log.warn("[LowcodeAiGenerateService] AI stream failed, fallback to rule agent: {}", e.getMessage());
                    return Flux.empty();
                });

        return Flux.concat(
                Flux.just(event("progress", progress("analyzing", "理解业务需求", "正在提取业务目标、对象、字段和约束", "running", null))),
                aiStream,
                Flux.defer(() -> finishStreamGenerate(request, aiContent.toString()))
        );
    }

    public LowcodeAiAppGenerateResult refineApp(Long appId, LowcodeAiAppGenerateRequest request) {
        LowcodeAiAppGenerateResult result = generateAppDraft(request);
        List<String> notes = new ArrayList<>(safeList(result.getGenerationNotes()));
        notes.add("基于应用 " + appId + " 生成了优化建议草稿，尚未保存。");
        result.setGenerationNotes(notes);
        return result;
    }

    private LowcodeAiAppGenerateResult tryGenerateWithAi(LowcodeAiAppGenerateRequest request) {
        try {
            AiClientAdapter.AiClientResult aiResult = aiClientAdapter.call(
                    AGENT_CODE,
                    buildAgentMessage(request),
                    buildContextVars(request),
                    90);
            if (aiResult == null || aiResult.isFallback() || StringUtils.isBlank(aiResult.getContent())) {
                log.info("[LowcodeAiGenerateService] AI unavailable, fallback={}",
                        aiResult == null ? null : aiResult.getFallbackReason());
                return null;
            }
            LowcodeAiAppGenerateResult normalized = parseAiGenerateResult(aiResult.getContent(), request);
            if (normalized == null) {
                return null;
            }
            normalized.setFallback(false);
            return normalized;
        } catch (Exception e) {
            log.info("[LowcodeAiGenerateService] AI result ignored and fallback to rule agent: {}", e.getMessage());
            return null;
        }
    }

    private LowcodeAiAppGenerateResult normalizeAiResult(LowcodeAiAppGenerateResult result,
                                                         LowcodeAiAppGenerateRequest request) {
        if (result == null) {
            return null;
        }
        normalizeResultCollections(result);
        if (result.getModels().isEmpty() && result.getModelDraft() != null) {
            result.getModels().add(result.getModelDraft());
        }
        if (result.getApps().isEmpty() && result.getAppDraft() != null) {
            result.getApps().add(result.getAppDraft());
        }
        if (result.getModels().isEmpty() || result.getApps().isEmpty()) {
            return null;
        }
        AiLowcodeDomain selectedDomain = request.getDomainId() == null ? null : domainService.requireEnabledDomain(request.getDomainId());
        if (result.getDomains().isEmpty()) {
            LowcodeAiDomainDraftDTO domainDraft = selectedDomain != null
                    ? domainDraftFromEntity(selectedDomain)
                    : buildNewDomainDraft(inferDomainCandidates(request.getDescription()).get(0), request.getDescription());
            result.getDomains().add(domainDraft);
        }
        if (selectedDomain != null) {
            LowcodeAiDomainDraftDTO domainDraft = domainDraftFromEntity(selectedDomain);
            domainDraft.setObjectCodes(resolveGeneratedObjectCodes(result));
            result.getDomains().clear();
            result.getDomains().add(domainDraft);
        }
        applySingleDomainPreference(result, selectedDomain, request);
        applyRequestedTablePrefix(result, request.getDescription());
        for (LowcodeAiDomainDraftDTO domainDraft : result.getDomains()) {
            normalizeDomainDraft(domainDraft, request.getDescription());
        }
        LowcodeAiDomainDraftDTO primaryDomain = result.getDomains().get(0);
        for (LowcodeDataModelDTO model : result.getModels()) {
            normalizeModelDraft(model, primaryDomain, request.getDescription());
        }
        for (LowcodeAppDraftDTO app : result.getApps()) {
            LowcodeDataModelDTO model = resolveModelForApp(result.getModels(), app);
            LowcodeModelSchema modelSchema = model.getModelSchema();
            LowcodePageSchema pageSchema = app.getPageSchema() == null
                    ? buildPageSchema(modelSchema, SIMPLE_LAYOUT, false)
                    : app.getPageSchema();
            String layoutType = chooseSupportedLayout(pageSchema.getLayoutType(), modelSchema);
            pageSchema.setLayoutType(layoutType);
            fillAppDraft(app, primaryDomain, model, modelSchema, pageSchema);
        }
        result.setModelDraft(result.getModels().get(0));
        result.setAppDraft(result.getApps().get(0));
        result.setModelSchema(result.getModelDraft().getModelSchema());
        result.setPageSchema(result.getAppDraft().getPageSchema());
        if (StringUtils.isBlank(result.getRequirementSummary())) {
            result.setRequirementSummary(summarizeRequirement(request.getDescription()));
        }
        if (result.getSteps().isEmpty()) {
            result.setSteps(completedSteps("AI 已返回低代码协议，系统已完成兼容性校验"));
        }
        if (result.getGenerationNotes().isEmpty()) {
            result.setGenerationNotes(List.of("已根据需求生成业务领域、数据模型和应用草稿", "AI 结果不会自动保存，请确认后保存"));
        }
        validateRuntime(result);
        return result;
    }

    private LowcodeAiAppGenerateResult buildRuleAgentResult(LowcodeAiAppGenerateRequest request) {
        AiLowcodeDomain selectedDomain = request.getDomainId() == null ? null : domainService.requireEnabledDomain(request.getDomainId());
        List<ObjectPlan> objectPlans = inferObjectPlans(request.getDescription(), selectedDomain);
        List<LowcodeAiDomainDraftDTO> domainDrafts = buildDomainDrafts(objectPlans, selectedDomain, request.getDescription());
        applyRequestedTablePrefixToDomains(domainDrafts, request.getDescription());
        Map<String, LowcodeAiDomainDraftDTO> domainMap = new LinkedHashMap<>();
        for (LowcodeAiDomainDraftDTO domainDraft : domainDrafts) {
            domainMap.put(domainDraft.getDomainCode(), domainDraft);
        }

        List<LowcodeDataModelDTO> modelDrafts = new ArrayList<>();
        List<LowcodeAppDraftDTO> appDrafts = new ArrayList<>();
        List<LowcodeAiDecisionDTO> decisions = new ArrayList<>();
        int sort = 0;
        for (ObjectPlan objectPlan : objectPlans) {
            LowcodeAiDomainDraftDTO domainDraft = domainMap.get(objectPlan.domainCode());
            if (domainDraft == null) {
                domainDraft = domainDrafts.get(0);
            }
            String layoutType = chooseLayoutForObject(objectPlan, request.getDescription());
            LowcodeModelSchema modelSchema = buildModelSchema(domainDraft, objectPlan, request.getDescription(), TREE_LAYOUT.equals(layoutType));
            LowcodePageSchema pageSchema = buildPageSchema(modelSchema, layoutType, TREE_LAYOUT.equals(layoutType));
            LowcodeRuntimeConfig runtimeConfig = runtimeConfigBuilder.buildRuntimeConfig(
                    normalizeConfigKey(StringUtils.defaultIfBlank(domainDraft.getConfigKeyPrefix(), domainDraft.getDomainCode() + "_") + objectPlan.code()),
                    modelSchema,
                    pageSchema);

            LowcodeDataModelDTO modelDraft = new LowcodeDataModelDTO();
            modelDraft.setDomainId(domainDraft.getExistingDomainId());
            modelDraft.setModelCode(objectPlan.code());
            modelDraft.setModelName(objectPlan.name());
            modelDraft.setModelDesc(request.getDescription());
            modelDraft.setStatus(STATUS_ENABLED);
            modelDraft.setTenantEnabled(true);
            modelDraft.setMasterData(false);
            modelDraft.setModelSchema(modelSchema);
            modelDraft.setSyncDdl(false);
            modelDraft.setConfirmSyncDdl(false);
            modelDrafts.add(modelDraft);

            LowcodeAppDraftDTO appDraft = new LowcodeAppDraftDTO();
            appDraft.setDomainId(domainDraft.getExistingDomainId());
            appDraft.setDomainCode(domainDraft.getDomainCode());
            appDraft.setDomainName(domainDraft.getDomainName());
            appDraft.setObjectCode(objectPlan.code());
            appDraft.setObjectName(objectPlan.name());
            appDraft.setConfigKey(runtimeConfig.getConfigKey());
            appDraft.setAppName(objectPlan.name() + "管理");
            appDraft.setMenuName(appDraft.getAppName());
            appDraft.setMenuParentId(domainDraft.getMenuParentId());
            appDraft.setMenuSort(sort++);
            appDraft.setModelSchema(modelSchema);
            appDraft.setPageSchema(pageSchema);
            appDrafts.add(appDraft);

            decisions.add(decision(
                    "template",
                    "页面模板选择",
                    objectPlan.name(),
                    layoutLabel(layoutType),
                    buildTemplateReason(objectPlan, layoutType, request.getDescription()),
                    Map.of("layoutType", layoutType, "objectCode", objectPlan.code())));
        }

        LowcodeAiAppGenerateResult result = new LowcodeAiAppGenerateResult();
        result.setRequirementSummary(summarizeRequirement(request.getDescription()));
        result.setDomainSuggestion(domainDrafts.stream().map(LowcodeAiDomainDraftDTO::getDomainName).findFirst().orElse("业务领域"));
        result.setDomains(domainDrafts);
        result.setModels(modelDrafts);
        result.setModelDraft(modelDrafts.get(0));
        result.setApps(appDrafts);
        result.setAppDraft(appDrafts.get(0));
        result.setModelSchema(modelDrafts.get(0).getModelSchema());
        result.setPageSchema(appDrafts.get(0).getPageSchema());
        result.setSteps(completedSteps("已通过规则 Agent 完成端到端草稿生成"));
        result.setDecisions(buildDecisions(domainDrafts, objectPlans, decisions));
        result.setFallback(true);
        result.setGenerationNotes(List.of(
                "已自动划分业务领域并生成模型与应用草稿，确认前不会保存任何数据",
                "页面模板由系统根据需求关键词和模型结构自动选择，后续可在应用设计器继续调整",
                "当前未执行 DDL，不会自动发布应用"
        ));
        return result;
    }

    private void validateRequest(LowcodeAiAppGenerateRequest request) {
        if (request == null || StringUtils.isBlank(request.getDescription())) {
            throw new BusinessException("需求描述不能为空");
        }
    }

    private String buildAgentMessage(LowcodeAiAppGenerateRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下业务需求生成低代码业务系统草稿，只返回符合 Agent 上下文规范的 JSON。\n\n");
        sb.append("## 业务需求\n").append(request.getDescription()).append("\n\n");
        if (request.getDomainId() != null) {
            AiLowcodeDomain domain = domainService.requireEnabledDomain(request.getDomainId());
            sb.append("## 用户指定的目标业务领域\n");
            sb.append("请优先在该已有业务领域内生成模型和应用，domains[0].existingDomainId 必须使用该领域 ID；除非需求明确跨领域，否则不要新建领域。\n");
            sb.append("- domainId: ").append(domain.getId()).append("\n");
            sb.append("- domainCode: ").append(domain.getDomainCode()).append("\n");
            sb.append("- domainName: ").append(domain.getDomainName()).append("\n\n");
        }
        if (!safeList(request.getExistingModels()).isEmpty()) {
            sb.append("## 当前领域已有数据模型\n");
            sb.append("请优先复用这些模型生成页面；只有需求明确缺少对象时才补充新模型。\n");
            sb.append(summarizeExistingModels(request.getExistingModels())).append("\n\n");
        }
        if (StringUtils.isNotBlank(request.getDraftContext())) {
            sb.append("## 当前已生成草稿\n");
            sb.append("用户正在基于这份草稿追加要求，请在保留合理结构的基础上调整，不要忽略已有草稿。\n");
            sb.append(trimForPrompt(request.getDraftContext(), 6000)).append("\n\n");
        }
        sb.append("## 运行时变量\n");
        sb.append("- allowAutoSave: false\n");
        sb.append("- supportedLayouts: simple-crud, tree-crud, master-detail-crud\n");
        sb.append("- firstPhaseCodegen: single-main-model\n");
        return sb.toString();
    }

    private String summarizeExistingModels(List<LowcodeDataModelDTO> models) {
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (LowcodeDataModelDTO model : safeList(models)) {
            LowcodeModelSchema schema = model.getModelSchema();
            sb.append(index++).append(". ")
                    .append(StringUtils.defaultIfBlank(model.getModelName(), "-"))
                    .append(" / ")
                    .append(StringUtils.defaultIfBlank(model.getModelCode(), "-"));
            if (schema != null) {
                sb.append(" / table=").append(StringUtils.defaultIfBlank(schema.getTableName(), "-"));
                List<LowcodeFieldSchema> fields = safeList(schema.getFields());
                if (!fields.isEmpty()) {
                    sb.append(" / fields=");
                    sb.append(fields.stream()
                            .limit(12)
                            .map(field -> StringUtils.defaultIfBlank(field.getField(), "-") + ":" + StringUtils.defaultIfBlank(field.getLabel(), "-"))
                            .toList());
                }
            }
            sb.append("\n");
        }
        return trimForPrompt(sb.toString(), 5000);
    }

    private String trimForPrompt(String text, int maxLength) {
        String value = StringUtils.defaultString(text);
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "\n...已截断";
    }

    private Map<String, String> buildContextVars(LowcodeAiAppGenerateRequest request) {
        Map<String, String> contextVars = new HashMap<>();
        contextVars.put("description", request.getDescription());
        if (request.getDomainId() != null) {
            AiLowcodeDomain domain = domainService.requireEnabledDomain(request.getDomainId());
            contextVars.put("domainId", String.valueOf(domain.getId()));
            contextVars.put("domainCode", domain.getDomainCode());
            contextVars.put("domainName", domain.getDomainName());
        }
        return contextVars;
    }

    private LowcodeAiAppGenerateResult parseAiGenerateResult(String content, LowcodeAiAppGenerateRequest request) {
        try {
            String json = extractJson(extractAnswerContent(content));
            if (StringUtils.isBlank(json)) {
                return null;
            }
            LowcodeAiAppGenerateResult result = objectMapper.readValue(json, LowcodeAiAppGenerateResult.class);
            return normalizeAiResult(result, request);
        } catch (Exception e) {
            log.info("[LowcodeAiGenerateService] AI stream result parse failed: {}", e.getMessage());
            return null;
        }
    }

    private String extractAnswerContent(String content) {
        String text = StringUtils.defaultString(content);
        String answerDelimiter = "==================== 完整回复 ====================";
        int answerIndex = text.indexOf(answerDelimiter);
        if (answerIndex >= 0) {
            return text.substring(answerIndex + answerDelimiter.length());
        }
        return text;
    }

    private Flux<ServerSentEvent<String>> finishStreamGenerate(LowcodeAiAppGenerateRequest request, String content) {
        try {
            LowcodeAiAppGenerateResult result = parseAiGenerateResult(content, request);
            if (result == null) {
                result = buildRuleAgentResult(request);
            } else {
                result.setFallback(false);
            }
            return Flux.just(
                    event("progress", progress("validating", "校验低代码协议", "正在校验模型字段、页面分区和运行时协议", "running", null)),
                    event("progress", progress("validating", "校验低代码协议", "已完成模型和页面协议校验，等待用户确认保存", "completed", summarizeApps(result))),
                    event("result", result),
                    event("complete", Map.of("message", "生成完成，请确认后保存模型和应用草稿"))
            );
        } catch (Exception e) {
            log.error("[LowcodeAiGenerateService] stream generate failed", e);
            return Flux.just(event("error", Map.of("message", StringUtils.defaultIfBlank(e.getMessage(), "生成失败"))));
        }
    }

    private Map<String, Object> detectStreamStage(String content, AtomicReference<String> stageRef) {
        String nextStage = resolveStreamStage(content);
        String current = stageRef.get();
        if (StringUtils.isBlank(nextStage) || nextStage.equals(current)) {
            return null;
        }
        stageRef.set(nextStage);
        return switch (nextStage) {
            case "domain-planning" -> progress("domain-planning", "划分业务领域", "模型正在输出领域归属和领域复用策略", "running", null);
            case "model-generating" -> progress("model-generating", "生成数据模型", "模型正在输出业务对象、字段、字典和安全策略", "running", null);
            case "page-generating" -> progress("page-generating", "生成应用页面", "模型正在输出页面模板、分区和应用草稿", "running", null);
            default -> null;
        };
    }

    private String resolveStreamStage(String content) {
        String text = StringUtils.defaultString(content);
        if (text.contains("\"apps\"") || text.contains("\"appDraft\"") || text.contains("\"pageSchema\"")) {
            return "page-generating";
        }
        if (text.contains("\"models\"") || text.contains("\"modelDraft\"") || text.contains("\"modelSchema\"")) {
            return "model-generating";
        }
        if (text.contains("\"domains\"") || text.contains("\"domainSuggestion\"")) {
            return "domain-planning";
        }
        return "analyzing";
    }

    private List<String> resolveGeneratedObjectCodes(LowcodeAiAppGenerateResult result) {
        Set<String> codes = new LinkedHashSet<>();
        for (LowcodeDataModelDTO model : safeList(result.getModels())) {
            if (StringUtils.isNotBlank(model.getModelCode())) {
                codes.add(model.getModelCode());
            }
        }
        for (LowcodeAppDraftDTO app : safeList(result.getApps())) {
            if (StringUtils.isNotBlank(app.getObjectCode())) {
                codes.add(app.getObjectCode());
            }
        }
        return new ArrayList<>(codes);
    }

    private void normalizeResultCollections(LowcodeAiAppGenerateResult result) {
        if (result.getSteps() == null) {
            result.setSteps(new ArrayList<>());
        }
        if (result.getDecisions() == null) {
            result.setDecisions(new ArrayList<>());
        }
        if (result.getDomains() == null) {
            result.setDomains(new ArrayList<>());
        }
        if (result.getModels() == null) {
            result.setModels(new ArrayList<>());
        }
        if (result.getApps() == null) {
            result.setApps(new ArrayList<>());
        }
        if (result.getGenerationNotes() == null) {
            result.setGenerationNotes(new ArrayList<>());
        }
        if (result.getDdlPreview() == null) {
            result.setDdlPreview(new ArrayList<>());
        }
    }

    private void normalizeDomainDraft(LowcodeAiDomainDraftDTO domainDraft, String description) {
        if (StringUtils.isBlank(domainDraft.getDomainCode())) {
            domainDraft.setDomainCode("biz_ops");
        }
        domainDraft.setDomainCode(normalizeCode(domainDraft.getDomainCode(), "biz_ops", 48));
        if (StringUtils.isBlank(domainDraft.getDomainName())) {
            domainDraft.setDomainName("业务运营");
        }
        if (StringUtils.isBlank(domainDraft.getDomainDesc())) {
            domainDraft.setDomainDesc(description);
        }
        domainDraft.setStatus(StringUtils.defaultIfBlank(domainDraft.getStatus(), STATUS_ENABLED));
        domainDraft.setSort(domainDraft.getSort() == null ? 0 : domainDraft.getSort());
        domainDraft.setTablePrefix(normalizePrefix(StringUtils.defaultIfBlank(domainDraft.getTablePrefix(), "biz_" + domainDraft.getDomainCode() + "_")));
        domainDraft.setConfigKeyPrefix(normalizePrefix(StringUtils.defaultIfBlank(domainDraft.getConfigKeyPrefix(), domainDraft.getDomainCode() + "_")));
        domainDraft.setDefaultAppType(StringUtils.defaultIfBlank(domainDraft.getDefaultAppType(), "SINGLE"));
        domainDraft.setDefaultLayoutType(StringUtils.defaultIfBlank(domainDraft.getDefaultLayoutType(), SIMPLE_LAYOUT));
        domainDraft.setDefaultTableMode(StringUtils.defaultIfBlank(domainDraft.getDefaultTableMode(), "CREATE"));
        if (domainDraft.getDomainSchema() == null) {
            domainDraft.setDomainSchema(buildDomainSchema(domainDraft, description));
        }
    }

    private void normalizeModelDraft(LowcodeDataModelDTO model, LowcodeAiDomainDraftDTO domainDraft, String description) {
        if (model.getModelSchema() == null) {
            model.setModelSchema(new LowcodeModelSchema());
        }
        String modelCode = normalizeCode(StringUtils.defaultIfBlank(model.getModelCode(),
                model.getModelSchema().getObject() == null ? null : model.getModelSchema().getObject().getCode()), "ai_model", 48);
        String modelName = StringUtils.defaultIfBlank(model.getModelName(),
                StringUtils.defaultIfBlank(model.getModelSchema().getBusinessName(), "业务对象"));
        model.setDomainId(domainDraft.getExistingDomainId());
        model.setModelCode(modelCode);
        model.setModelName(modelName);
        model.setModelDesc(StringUtils.defaultIfBlank(model.getModelDesc(), description));
        model.setStatus(StringUtils.defaultIfBlank(model.getStatus(), STATUS_ENABLED));
        model.setTenantEnabled(model.getTenantEnabled() == null || model.getTenantEnabled());
        model.setMasterData(Boolean.TRUE.equals(model.getMasterData()));
        model.setSyncDdl(false);
        model.setConfirmSyncDdl(false);

        LowcodeModelSchema schema = model.getModelSchema();
        schema.setSchemaVersion(2);
        schema.setAppType(StringUtils.defaultIfBlank(schema.getAppType(), "SINGLE"));
        schema.setTableMode(StringUtils.defaultIfBlank(schema.getTableMode(), "CREATE"));
        String requestedTablePrefix = parseRequestedTablePrefix(description);
        String tableName = normalizeTableName(StringUtils.defaultIfBlank(schema.getTableName(), domainDraft.getTablePrefix() + modelCode));
        if (StringUtils.isNotBlank(requestedTablePrefix) && !tableName.startsWith(requestedTablePrefix)) {
            tableName = normalizeTableName(requestedTablePrefix + modelCode);
        }
        schema.setTableName(tableName);
        schema.setBusinessName(modelName);
        LowcodeDomainRef domainRef = schema.getDomain() == null ? new LowcodeDomainRef() : schema.getDomain();
        domainRef.setId(domainDraft.getExistingDomainId());
        domainRef.setCode(domainDraft.getDomainCode());
        domainRef.setName(domainDraft.getDomainName());
        schema.setDomain(domainRef);
        LowcodeObjectSchema object = schema.getObject() == null ? new LowcodeObjectSchema() : schema.getObject();
        object.setCode(modelCode);
        object.setName(modelName);
        object.setDescription(StringUtils.defaultIfBlank(object.getDescription(), description));
        schema.setObject(object);
        if (schema.getFields() == null || schema.getFields().isEmpty()) {
            schema.setFields(defaultFields(modelName));
        }
        schemaNormalizer.normalizeModelFields(schema, true);
        policyService.normalizeModelSchema(schema);
    }

    private LowcodeDataModelDTO resolveModelForApp(List<LowcodeDataModelDTO> models, LowcodeAppDraftDTO app) {
        if (app != null && StringUtils.isNotBlank(app.getObjectCode())) {
            return models.stream()
                    .filter(model -> app.getObjectCode().equals(model.getModelCode()))
                    .findFirst()
                    .orElse(models.get(0));
        }
        return models.get(0);
    }

    private void fillAppDraft(LowcodeAppDraftDTO app, LowcodeAiDomainDraftDTO domainDraft, LowcodeDataModelDTO model,
                              LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        LowcodeRuntimeConfig runtimeConfig = runtimeConfigBuilder.buildRuntimeConfig(
                normalizeConfigKey(StringUtils.defaultIfBlank(app.getConfigKey(), domainDraft.getConfigKeyPrefix() + model.getModelCode())),
                modelSchema,
                pageSchema);
        app.setDomainId(domainDraft.getExistingDomainId());
        app.setDomainCode(domainDraft.getDomainCode());
        app.setDomainName(domainDraft.getDomainName());
        app.setObjectCode(model.getModelCode());
        app.setObjectName(model.getModelName());
        app.setConfigKey(runtimeConfig.getConfigKey());
        app.setAppName(StringUtils.defaultIfBlank(app.getAppName(), model.getModelName() + "管理"));
        app.setMenuName(StringUtils.defaultIfBlank(app.getMenuName(), app.getAppName()));
        app.setMenuParentId(app.getMenuParentId() == null ? domainDraft.getMenuParentId() : app.getMenuParentId());
        app.setMenuSort(app.getMenuSort() == null ? 0 : app.getMenuSort());
        app.setModelSchema(modelSchema);
        app.setPageSchema(pageSchema);
    }

    private void validateRuntime(LowcodeAiAppGenerateResult result) {
        for (LowcodeAppDraftDTO app : result.getApps()) {
            runtimeConfigBuilder.buildRuntimeConfig(app.getConfigKey(), app.getModelSchema(), app.getPageSchema());
        }
    }

    private List<ObjectPlan> inferObjectPlans(String description, AiLowcodeDomain selectedDomain) {
        String text = StringUtils.defaultString(description);
        Map<String, ObjectPlan> plans = new LinkedHashMap<>();
        for (ObjectCandidate candidate : OBJECT_CANDIDATES) {
            if (containsAny(text, candidate.keywords())) {
                String domainCode = selectedDomain == null ? candidate.domainCode() : selectedDomain.getDomainCode();
                plans.put(candidate.code(), new ObjectPlan(candidate.code(), candidate.name(), domainCode, candidate.tree()));
            }
        }
        if (plans.isEmpty()) {
            DomainCandidate domainCandidate = inferDomainCandidates(description).get(0);
            String objectName = inferObjectName(description);
            String objectCode = normalizeCode(objectName, "ai_model", 48);
            String domainCode = selectedDomain == null ? domainCandidate.code() : selectedDomain.getDomainCode();
            plans.put(objectCode, new ObjectPlan(objectCode, objectName, domainCode, shouldUseTree(description, objectName)));
        }
        if (selectedDomain == null && isSingleDomainRequested(description)) {
            String primaryDomainCode = plans.values().stream()
                    .findFirst()
                    .map(ObjectPlan::domainCode)
                    .orElseGet(() -> inferDomainCandidates(description).get(0).code());
            Map<String, ObjectPlan> collapsedPlans = new LinkedHashMap<>();
            for (ObjectPlan plan : plans.values()) {
                collapsedPlans.put(plan.code(), new ObjectPlan(plan.code(), plan.name(), primaryDomainCode, plan.tree()));
            }
            plans = collapsedPlans;
        }
        return plans.values().stream()
                .limit(MAX_OBJECTS)
                .toList();
    }

    private List<DomainCandidate> inferDomainCandidates(String description) {
        String text = StringUtils.defaultString(description);
        List<DomainCandidate> candidates = DOMAIN_CANDIDATES.stream()
                .filter(candidate -> containsAny(text, candidate.keywords()))
                .toList();
        if (candidates.isEmpty()) {
            return List.of(new DomainCandidate("biz_ops", "业务运营", "通用业务运营领域", Set.of()));
        }
        return candidates;
    }

    private List<LowcodeAiDomainDraftDTO> buildDomainDrafts(List<ObjectPlan> objectPlans, AiLowcodeDomain selectedDomain,
                                                            String description) {
        if (selectedDomain != null) {
            LowcodeAiDomainDraftDTO domainDraft = domainDraftFromEntity(selectedDomain);
            domainDraft.setObjectCodes(objectPlans.stream().map(ObjectPlan::code).distinct().toList());
            return List.of(domainDraft);
        }
        if (isSingleDomainRequested(description)) {
            String primaryDomainCode = objectPlans.stream()
                    .findFirst()
                    .map(ObjectPlan::domainCode)
                    .orElseGet(() -> inferDomainCandidates(description).get(0).code());
            LowcodeAiDomainDraftDTO domainDraft = buildResolvedDomainDraft(findDomainCandidate(primaryDomainCode), description);
            domainDraft.setObjectCodes(objectPlans.stream()
                    .map(ObjectPlan::code)
                    .distinct()
                    .toList());
            return List.of(domainDraft);
        }
        Map<String, DomainCandidate> candidates = new LinkedHashMap<>();
        for (DomainCandidate candidate : inferDomainCandidates(description)) {
            candidates.put(candidate.code(), candidate);
        }
        for (ObjectPlan objectPlan : objectPlans) {
            candidates.putIfAbsent(objectPlan.domainCode(), findDomainCandidate(objectPlan.domainCode()));
        }
        List<LowcodeAiDomainDraftDTO> result = new ArrayList<>();
        for (DomainCandidate candidate : candidates.values()) {
            LowcodeAiDomainDraftDTO draft = buildResolvedDomainDraft(candidate, description);
            draft.setObjectCodes(objectPlans.stream()
                    .filter(plan -> candidate.code().equals(plan.domainCode()))
                    .map(ObjectPlan::code)
                    .distinct()
                    .toList());
            result.add(draft);
        }
        return result.stream()
                .sorted(Comparator.comparing(LowcodeAiDomainDraftDTO::getDomainCode))
                .toList();
    }

    private DomainCandidate findDomainCandidate(String domainCode) {
        return DOMAIN_CANDIDATES.stream()
                .filter(candidate -> candidate.code().equals(domainCode))
                .findFirst()
                .orElse(new DomainCandidate(domainCode, "业务运营", "通用业务运营领域", Set.of()));
    }

    private LowcodeAiDomainDraftDTO buildResolvedDomainDraft(DomainCandidate candidate, String description) {
        AiLowcodeDomain existing = domainService.getByCode(candidate.code());
        if (existing != null && STATUS_ENABLED.equals(existing.getStatus())) {
            return domainDraftFromEntity(existing);
        }
        String domainCode = candidate.code();
        if (existing != null) {
            domainCode = nextAvailableDomainCode(candidate.code() + "_ai");
        }
        DomainCandidate available = new DomainCandidate(domainCode, candidate.name(), candidate.description(), candidate.keywords());
        return buildNewDomainDraft(available, description);
    }

    private void applySingleDomainPreference(LowcodeAiAppGenerateResult result, AiLowcodeDomain selectedDomain,
                                             LowcodeAiAppGenerateRequest request) {
        if (selectedDomain != null || !isSingleDomainRequested(request.getDescription()) || result.getDomains().size() <= 1) {
            return;
        }
        LowcodeAiDomainDraftDTO primaryDomain = choosePrimaryDomainDraft(result, request.getDescription());
        primaryDomain.setObjectCodes(resolveGeneratedObjectCodes(result));
        result.getDomains().clear();
        result.getDomains().add(primaryDomain);
        result.getDecisions().add(decision(
                "domain",
                "业务领域合并",
                primaryDomain.getDomainName(),
                "单一业务领域",
                "用户明确要求不要拆分多个业务领域，已将模型和应用统一归属到同一领域。",
                Map.of("singleDomain", true, "domainCode", primaryDomain.getDomainCode())));
    }

    private LowcodeAiDomainDraftDTO choosePrimaryDomainDraft(LowcodeAiAppGenerateResult result, String description) {
        String preferredDomainCode = null;
        if (!result.getModels().isEmpty()) {
            LowcodeModelSchema schema = result.getModels().get(0).getModelSchema();
            if (schema != null && schema.getDomain() != null) {
                preferredDomainCode = schema.getDomain().getCode();
            }
        }
        if (StringUtils.isBlank(preferredDomainCode) && !result.getApps().isEmpty()) {
            preferredDomainCode = result.getApps().get(0).getDomainCode();
        }
        if (StringUtils.isNotBlank(preferredDomainCode)) {
            for (LowcodeAiDomainDraftDTO domain : result.getDomains()) {
                if (preferredDomainCode.equals(domain.getDomainCode())) {
                    return domain;
                }
            }
        }
        if (!result.getDomains().isEmpty()) {
            return result.getDomains().get(0);
        }
        return buildNewDomainDraft(inferDomainCandidates(description).get(0), description);
    }

    private void applyRequestedTablePrefix(LowcodeAiAppGenerateResult result, String description) {
        String tablePrefix = parseRequestedTablePrefix(description);
        if (StringUtils.isBlank(tablePrefix)) {
            return;
        }
        applyTablePrefixToDomains(result.getDomains(), tablePrefix);
        result.getDecisions().add(decision(
                "model",
                "表名前缀",
                "数据模型",
                tablePrefix,
                "用户明确要求数据表使用该前缀，已在模型协议归一化时强制应用。",
                Map.of("tablePrefix", tablePrefix)));
    }

    private void applyRequestedTablePrefixToDomains(List<LowcodeAiDomainDraftDTO> domains, String description) {
        String tablePrefix = parseRequestedTablePrefix(description);
        if (StringUtils.isBlank(tablePrefix)) {
            return;
        }
        applyTablePrefixToDomains(domains, tablePrefix);
    }

    private void applyTablePrefixToDomains(List<LowcodeAiDomainDraftDTO> domains, String tablePrefix) {
        for (LowcodeAiDomainDraftDTO domain : safeList(domains)) {
            domain.setTablePrefix(tablePrefix);
            syncDomainSchemaNaming(domain);
        }
    }

    private void syncDomainSchemaNaming(LowcodeAiDomainDraftDTO domain) {
        if (domain == null || domain.getDomainSchema() == null) {
            return;
        }
        domain.getDomainSchema().getNaming().setTablePrefix(domain.getTablePrefix());
        domain.getDomainSchema().getNaming().setConfigKeyPrefix(domain.getConfigKeyPrefix());
        domain.getDomainSchema().getCodegen().setModuleName(domain.getDomainCode());
    }

    private boolean isSingleDomainRequested(String description) {
        String text = StringUtils.defaultString(description);
        return text.contains("一个业务领域")
                || text.contains("同一个业务领域")
                || text.contains("不要分开")
                || text.contains("不要拆分")
                || text.contains("不分开")
                || text.contains("不拆分");
    }

    private String nextAvailableDomainCode(String baseCode) {
        String normalized = normalizeCode(baseCode, "biz_ops", 44);
        String candidate = normalized;
        int index = 1;
        while (domainService.getByCode(candidate) != null) {
            candidate = normalizeCode(normalized + "_" + index, "biz_ops", 48);
            index++;
        }
        return candidate;
    }

    private LowcodeAiDomainDraftDTO domainDraftFromEntity(AiLowcodeDomain domain) {
        LowcodeAiDomainDraftDTO draft = new LowcodeAiDomainDraftDTO();
        draft.setExistingDomainId(domain.getId());
        draft.setDomainCode(domain.getDomainCode());
        draft.setDomainName(domain.getDomainName());
        draft.setDomainDesc(domain.getDomainDesc());
        draft.setIcon(domain.getIcon());
        draft.setSort(domain.getSort());
        draft.setStatus(domain.getStatus());
        draft.setMenuParentId(domain.getMenuParentId());
        draft.setTablePrefix(domain.getTablePrefix());
        draft.setConfigKeyPrefix(domain.getConfigKeyPrefix());
        draft.setDefaultAppType(domain.getDefaultAppType());
        draft.setDefaultLayoutType(domain.getDefaultLayoutType());
        draft.setDefaultTableMode(domain.getDefaultTableMode());
        return draft;
    }

    private LowcodeAiDomainDraftDTO buildNewDomainDraft(DomainCandidate candidate, String description) {
        LowcodeAiDomainDraftDTO draft = new LowcodeAiDomainDraftDTO();
        draft.setDomainCode(candidate.code());
        draft.setDomainName(candidate.name());
        draft.setDomainDesc(candidate.description());
        draft.setIcon("apps");
        draft.setSort(0);
        draft.setStatus(STATUS_ENABLED);
        draft.setTablePrefix(normalizePrefix("biz_" + candidate.code()));
        draft.setConfigKeyPrefix(normalizePrefix(candidate.code()));
        draft.setDefaultAppType("SINGLE");
        draft.setDefaultLayoutType(SIMPLE_LAYOUT);
        draft.setDefaultTableMode("CREATE");
        draft.setDomainSchema(buildDomainSchema(draft, description));
        return draft;
    }

    private LowcodeDomainSchema buildDomainSchema(LowcodeAiDomainDraftDTO draft, String description) {
        LowcodeDomainSchema schema = new LowcodeDomainSchema();
        schema.getAiContext().setDescription(description);
        schema.getAiContext().setTerms(new ArrayList<>(new LinkedHashSet<>(safeList(draft.getObjectCodes()))));
        schema.getAiContext().setCommonObjects(new ArrayList<>(safeList(draft.getObjectCodes())));
        schema.getAiContext().setGenerationNotes(List.of("由 AI 业务系统生成 Agent 自动规划，用户确认后保存"));
        schema.getNaming().setTablePrefix(draft.getTablePrefix());
        schema.getNaming().setConfigKeyPrefix(draft.getConfigKeyPrefix());
        schema.getNaming().setObjectCodeStyle("lower_snake");
        schema.getDefaults().setAppType("SINGLE");
        schema.getDefaults().setLayoutType(SIMPLE_LAYOUT);
        schema.getDefaults().setTableMode("CREATE");
        schema.getDefaults().setMenuParentId(draft.getMenuParentId());
        schema.getCodegen().setModuleName(draft.getDomainCode());
        return schema;
    }

    private LowcodeModelSchema buildModelSchema(LowcodeAiDomainDraftDTO domainDraft, ObjectPlan objectPlan,
                                                String description, boolean treeLayout) {
        LowcodeModelSchema schema = new LowcodeModelSchema();
        schema.setSchemaVersion(2);
        schema.setAppType(treeLayout ? "TREE" : "SINGLE");
        schema.setTableMode("CREATE");
        schema.setTableName(normalizeTableName(StringUtils.defaultIfBlank(domainDraft.getTablePrefix(), "biz_") + objectPlan.code()));
        schema.setBusinessName(objectPlan.name());

        LowcodeDomainRef domainRef = new LowcodeDomainRef();
        domainRef.setId(domainDraft.getExistingDomainId());
        domainRef.setCode(domainDraft.getDomainCode());
        domainRef.setName(domainDraft.getDomainName());
        schema.setDomain(domainRef);

        LowcodeObjectSchema object = new LowcodeObjectSchema();
        object.setCode(objectPlan.code());
        object.setName(objectPlan.name());
        object.setDescription(description);
        schema.setObject(object);

        List<LowcodeFieldSchema> fields = new ArrayList<>(fieldsForObject(objectPlan));
        if (treeLayout) {
            fields.add(0, field("parentId", "parent_id", "上级节点", "bigint", null, false, false, false, true, "treeSelect"));
            LowcodeTreeConfig treeConfig = new LowcodeTreeConfig();
            treeConfig.setEnabled(true);
            treeConfig.setKeyField("id");
            treeConfig.setParentField("parentId");
            treeConfig.setLabelField(resolveLabelField(fields));
            treeConfig.setChildrenField("children");
            treeConfig.setTreeTitle(objectPlan.name() + "树");
            treeConfig.setLoadMode("lazy");
            schema.setTreeConfig(treeConfig);
        }
        schema.setFields(fields);
        schemaNormalizer.normalizeModelFields(schema, true);
        policyService.normalizeModelSchema(schema);
        return schema;
    }

    private LowcodePageSchema buildPageSchema(LowcodeModelSchema modelSchema, String layoutType, boolean treeLayout) {
        LowcodePageSchema pageSchema = new LowcodePageSchema();
        pageSchema.setLayoutType(StringUtils.defaultIfBlank(layoutType, SIMPLE_LAYOUT));
        pageSchema.setPrimaryModelCode(modelSchema.getObject() == null ? null : modelSchema.getObject().getCode());
        pageSchema.getZones().add(zone("search", "search-form", modelSchema.getFields().stream()
                .filter(field -> Boolean.TRUE.equals(field.getSearchable()))
                .map(LowcodeFieldSchema::getField)
                .limit(4)
                .toList()));
        LowcodePageZone tableZone = zone("table", "data-table", modelSchema.getFields().stream()
                .filter(field -> Boolean.TRUE.equals(field.getListVisible()))
                .map(LowcodeFieldSchema::getField)
                .limit(7)
                .toList());
        if (treeLayout) {
            Map<String, Object> treeConfig = new LinkedHashMap<>();
            treeConfig.put("keyField", "id");
            treeConfig.put("parentField", "parentId");
            treeConfig.put("labelField", resolveLabelField(modelSchema.getFields()));
            treeConfig.put("childrenField", "children");
            treeConfig.put("treeTitle", modelSchema.getBusinessName() + "树");
            treeConfig.put("loadMode", "lazy");
            tableZone.getProps().put("treeConfig", treeConfig);
        }
        pageSchema.getZones().add(tableZone);
        pageSchema.getZones().add(zone("edit", "edit-form", modelSchema.getFields().stream()
                .filter(field -> field.getFormVisible() == null || field.getFormVisible())
                .map(LowcodeFieldSchema::getField)
                .toList()));
        pageSchema.getZones().add(zone("detail", "detail-panel", modelSchema.getFields().stream()
                .filter(field -> field.getListVisible() == null || field.getListVisible())
                .map(LowcodeFieldSchema::getField)
                .limit(8)
                .toList()));
        return pageSchema;
    }

    private List<LowcodeFieldSchema> fieldsForObject(ObjectPlan objectPlan) {
        return switch (objectPlan.code()) {
            case "customer" -> List.of(
                    field("customerName", "customer_name", "客户名称", "varchar", 128, true, true, true, true, "input"),
                    field("contactPerson", "contact_person", "联系人", "varchar", 64, false, true, true, true, "input"),
                    field("phone", "phone", "联系电话", "varchar", 32, false, true, true, true, "input", "PHONE"),
                    field("customerLevel", "customer_level", "客户等级", "varchar", 32, false, true, true, true, "select"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"),
                    field("remark", "remark", "备注", "text", null, false, false, true, true, "textarea"));
            case "contact" -> List.of(
                    field("contactName", "contact_name", "联系人姓名", "varchar", 64, true, true, true, true, "input", "NAME"),
                    field("customerName", "customer_name", "所属客户", "varchar", 128, false, true, true, true, "input"),
                    field("phone", "phone", "手机号码", "varchar", 32, false, true, true, true, "input", "PHONE"),
                    field("email", "email", "邮箱", "varchar", 128, false, true, true, true, "input", "EMAIL"),
                    field("positionName", "position_name", "职务", "varchar", 64, false, false, true, true, "input"),
                    field("remark", "remark", "备注", "text", null, false, false, true, true, "textarea"));
            case "opportunity" -> List.of(
                    field("opportunityName", "opportunity_name", "商机名称", "varchar", 128, true, true, true, true, "input"),
                    field("customerName", "customer_name", "客户名称", "varchar", 128, false, true, true, true, "input"),
                    field("estimatedAmount", "estimated_amount", "预计金额", "decimal", 18, false, false, true, true, "number"),
                    field("stage", "stage", "阶段", "varchar", 32, false, true, true, true, "select"),
                    field("ownerName", "owner_name", "负责人", "varchar", 64, false, true, true, true, "userSelect"),
                    field("remark", "remark", "备注", "text", null, false, false, true, true, "textarea"));
            case "contract" -> List.of(
                    field("contractNo", "contract_no", "合同编号", "varchar", 64, true, true, true, true, "input"),
                    field("contractName", "contract_name", "合同名称", "varchar", 128, true, true, true, true, "input"),
                    field("customerName", "customer_name", "客户名称", "varchar", 128, false, true, true, true, "input"),
                    field("contractAmount", "contract_amount", "合同金额", "decimal", 18, false, false, true, true, "number"),
                    field("signDate", "sign_date", "签订日期", "date", null, false, true, true, true, "date"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"),
                    field("remark", "remark", "备注", "text", null, false, false, true, true, "textarea"));
            case "payment" -> List.of(
                    field("paymentNo", "payment_no", "回款编号", "varchar", 64, true, true, true, true, "input"),
                    field("customerName", "customer_name", "客户名称", "varchar", 128, false, true, true, true, "input"),
                    field("contractName", "contract_name", "关联合同", "varchar", 128, false, true, true, true, "input"),
                    field("paymentAmount", "payment_amount", "回款金额", "decimal", 18, true, false, true, true, "number"),
                    field("paymentDate", "payment_date", "回款日期", "date", null, true, true, true, true, "date"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"));
            case "product" -> List.of(
                    field("productCode", "product_code", "商品编码", "varchar", 64, true, true, true, true, "input"),
                    field("productName", "product_name", "商品名称", "varchar", 128, true, true, true, true, "input"),
                    field("categoryName", "category_name", "商品分类", "varchar", 64, false, true, true, true, "input"),
                    field("unitName", "unit_name", "单位", "varchar", 32, false, false, true, true, "input"),
                    field("salePrice", "sale_price", "销售价", "decimal", 18, false, false, true, true, "number"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"));
            case "supplier" -> List.of(
                    field("supplierName", "supplier_name", "供应商名称", "varchar", 128, true, true, true, true, "input"),
                    field("contactPerson", "contact_person", "联系人", "varchar", 64, false, true, true, true, "input"),
                    field("phone", "phone", "联系电话", "varchar", 32, false, true, true, true, "input", "PHONE"),
                    field("address", "address", "地址", "varchar", 255, false, false, true, true, "input", "ADDRESS"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"),
                    field("remark", "remark", "备注", "text", null, false, false, true, true, "textarea"));
            case "purchase_order", "sales_order" -> List.of(
                    field("orderNo", "order_no", "订单编号", "varchar", 64, true, true, true, true, "input"),
                    field("partnerName", "partner_name", "往来单位", "varchar", 128, false, true, true, true, "input"),
                    field("totalAmount", "total_amount", "订单金额", "decimal", 18, false, false, true, true, "number"),
                    field("orderDate", "order_date", "订单日期", "date", null, false, true, true, true, "date"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"),
                    field("remark", "remark", "备注", "text", null, false, false, true, true, "textarea"));
            case "inventory" -> List.of(
                    field("productName", "product_name", "商品名称", "varchar", 128, true, true, true, true, "input"),
                    field("warehouseName", "warehouse_name", "仓库", "varchar", 64, true, true, true, true, "input"),
                    field("quantity", "quantity", "库存数量", "decimal", 18, false, false, true, true, "number"),
                    field("warningQuantity", "warning_quantity", "预警数量", "decimal", 18, false, false, true, true, "number"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"));
            case "invoice" -> List.of(
                    field("invoiceNo", "invoice_no", "发票号码", "varchar", 64, true, true, true, true, "input"),
                    field("customerName", "customer_name", "客户名称", "varchar", 128, false, true, true, true, "input"),
                    field("invoiceAmount", "invoice_amount", "发票金额", "decimal", 18, false, false, true, true, "number"),
                    field("invoiceDate", "invoice_date", "开票日期", "date", null, false, true, true, true, "date"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"));
            case "expense" -> List.of(
                    field("expenseNo", "expense_no", "费用编号", "varchar", 64, true, true, true, true, "input"),
                    field("expenseType", "expense_type", "费用类型", "varchar", 32, false, true, true, true, "select"),
                    field("amount", "amount", "金额", "decimal", 18, true, false, true, true, "number"),
                    field("expenseDate", "expense_date", "发生日期", "date", null, false, true, true, true, "date"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"));
            case "employee" -> List.of(
                    field("employeeNo", "employee_no", "员工编号", "varchar", 64, true, true, true, true, "input"),
                    field("employeeName", "employee_name", "员工姓名", "varchar", 64, true, true, true, true, "input", "NAME"),
                    field("departmentName", "department_name", "部门", "varchar", 64, false, true, true, true, "orgTreeSelect"),
                    field("phone", "phone", "手机号", "varchar", 32, false, true, true, true, "input", "PHONE"),
                    field("hireDate", "hire_date", "入职日期", "date", null, false, true, true, true, "date"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"));
            case "department", "category" -> List.of(
                    field("name", "name", objectPlan.name() + "名称", "varchar", 128, true, true, true, true, "input"),
                    field("code", "code", objectPlan.name() + "编码", "varchar", 64, true, true, true, true, "input"),
                    field("sortNo", "sort_no", "排序", "int", null, false, false, true, true, "number"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"),
                    field("remark", "remark", "备注", "text", null, false, false, true, true, "textarea"));
            case "leave_request" -> List.of(
                    field("employeeName", "employee_name", "申请人", "varchar", 64, true, true, true, true, "userSelect", "NAME"),
                    field("leaveType", "leave_type", "请假类型", "varchar", 32, true, true, true, true, "select"),
                    field("startTime", "start_time", "开始时间", "datetime", null, true, true, true, true, "datetime"),
                    field("endTime", "end_time", "结束时间", "datetime", null, true, true, true, true, "datetime"),
                    field("reason", "reason", "请假原因", "text", null, false, false, true, true, "textarea"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"));
            case "project" -> List.of(
                    field("projectCode", "project_code", "项目编码", "varchar", 64, true, true, true, true, "input"),
                    field("projectName", "project_name", "项目名称", "varchar", 128, true, true, true, true, "input"),
                    field("ownerName", "owner_name", "负责人", "varchar", 64, false, true, true, true, "userSelect"),
                    field("startDate", "start_date", "开始日期", "date", null, false, true, true, true, "date"),
                    field("endDate", "end_date", "结束日期", "date", null, false, true, true, true, "date"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"));
            case "task" -> List.of(
                    field("taskName", "task_name", "任务名称", "varchar", 128, true, true, true, true, "input"),
                    field("projectName", "project_name", "所属项目", "varchar", 128, false, true, true, true, "input"),
                    field("assigneeName", "assignee_name", "负责人", "varchar", 64, false, true, true, true, "userSelect"),
                    field("dueDate", "due_date", "截止日期", "date", null, false, true, true, true, "date"),
                    field("priority", "priority", "优先级", "varchar", 32, false, true, true, true, "select"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"));
            case "work_order" -> List.of(
                    field("workOrderNo", "work_order_no", "工单编号", "varchar", 64, true, true, true, true, "input"),
                    field("title", "title", "工单标题", "varchar", 128, true, true, true, true, "input"),
                    field("handlerName", "handler_name", "处理人", "varchar", 64, false, true, true, true, "userSelect"),
                    field("priority", "priority", "优先级", "varchar", 32, false, true, true, true, "select"),
                    field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"),
                    field("description", "description", "问题描述", "text", null, false, false, true, true, "textarea"));
            default -> defaultFields(objectPlan.name());
        };
    }

    private List<LowcodeFieldSchema> defaultFields(String objectName) {
        return List.of(
                field("name", "name", objectName + "名称", "varchar", 128, true, true, true, true, "input"),
                field("code", "code", objectName + "编码", "varchar", 64, false, true, true, true, "input"),
                field("status", "status", "状态", "varchar", 32, false, true, true, true, "select"),
                field("remark", "remark", "备注", "text", null, false, false, true, true, "textarea")
        );
    }

    private LowcodePageZone zone(String key, String component, List<String> fields) {
        LowcodePageZone zone = new LowcodePageZone();
        zone.setZoneKey(key);
        zone.setComponentKey(component);
        zone.setEnabled(true);
        zone.setFieldRefs(fields == null ? new ArrayList<>() : new ArrayList<>(fields));
        return zone;
    }

    private LowcodeFieldSchema field(String field, String column, String label, String dataType,
                                     Integer length, boolean required, boolean searchable,
                                     boolean listVisible, boolean formVisible, String component) {
        return field(field, column, label, dataType, length, required, searchable, listVisible, formVisible, component, "NONE");
    }

    private LowcodeFieldSchema field(String field, String column, String label, String dataType,
                                     Integer length, boolean required, boolean searchable,
                                     boolean listVisible, boolean formVisible, String component, String sensitiveType) {
        LowcodeFieldSchema schema = new LowcodeFieldSchema();
        schema.setField(field);
        schema.setColumnName(column);
        schema.setLabel(label);
        schema.setDataType(dataType);
        schema.setLength(length);
        schema.setPrecision("decimal".equals(dataType) ? 2 : null);
        schema.setRequired(required);
        schema.setSearchable(searchable);
        schema.setListVisible(listVisible);
        schema.setFormVisible(formVisible);
        schema.setComponentType(component);
        schema.setQueryType("varchar".equals(dataType) && searchable ? "like" : "eq");
        schema.setSensitiveType(StringUtils.defaultIfBlank(sensitiveType, "NONE"));
        schema.setSystemField(false);
        schema.setReadonly(false);
        schema.setSortable("date".equals(dataType) || "datetime".equals(dataType) || "decimal".equals(dataType) || "int".equals(dataType));
        schema.setWidth("text".equals(dataType) ? 220 : 160);
        return schema;
    }

    private String chooseLayoutForObject(ObjectPlan objectPlan, String description) {
        if (objectPlan.tree() || shouldUseTree(description, objectPlan.name())) {
            return TREE_LAYOUT;
        }
        return SIMPLE_LAYOUT;
    }

    private String chooseSupportedLayout(String layoutType, LowcodeModelSchema modelSchema) {
        if (TREE_LAYOUT.equals(layoutType)) {
            return TREE_LAYOUT;
        }
        if ("master-detail-crud".equals(layoutType) && modelSchema != null && modelSchema.getChildren() != null
                && !modelSchema.getChildren().isEmpty()) {
            return "master-detail-crud";
        }
        return SIMPLE_LAYOUT;
    }

    private boolean shouldUseTree(String description, String objectName) {
        String text = StringUtils.defaultString(description) + StringUtils.defaultString(objectName);
        return containsAny(text, Set.of("树", "树形", "组织架构", "部门", "分类", "类目", "目录", "父级", "上级"));
    }

    private String buildTemplateReason(ObjectPlan objectPlan, String layoutType, String description) {
        if (TREE_LAYOUT.equals(layoutType)) {
            return "需求或对象包含组织、分类、树形、父级等层级关键词，已补充 parentId 字段和树形配置。";
        }
        if (containsAny(description, Set.of("明细", "子表", "子项", "订单行"))) {
            return "识别到明细/子项诉求；首期先生成可运行的单表应用草稿，后续可在应用设计器扩展主子表协议。";
        }
        return "需求以列表、查询、编辑和详情为主，适合标准单表 CRUD 模板。";
    }

    private String layoutLabel(String layoutType) {
        if (TREE_LAYOUT.equals(layoutType)) {
            return "左树右表/树形单表";
        }
        if ("master-detail-crud".equals(layoutType)) {
            return "主子表";
        }
        return "标准单表";
    }

    private List<LowcodeAiDecisionDTO> buildDecisions(List<LowcodeAiDomainDraftDTO> domains,
                                                       List<ObjectPlan> objects,
                                                       List<LowcodeAiDecisionDTO> templateDecisions) {
        List<LowcodeAiDecisionDTO> decisions = new ArrayList<>();
        decisions.add(decision(
                "domain",
                "业务领域划分",
                "业务系统",
                domains.stream().map(LowcodeAiDomainDraftDTO::getDomainName).toList().toString(),
                "根据需求关键词和业务对象边界自动划分领域；已有启用领域会直接复用。",
                Map.of("domainCount", domains.size())));
        decisions.add(decision(
                "model",
                "数据模型规划",
                "业务对象",
                objects.stream().map(ObjectPlan::name).toList().toString(),
                "每个核心业务对象生成一个可独立维护的数据模型，便于后续二次开发和代码下载。",
                Map.of("modelCount", objects.size())));
        decisions.addAll(templateDecisions);
        decisions.add(decision(
                "save",
                "保存策略",
                "模型与应用草稿",
                "用户确认后保存",
                "AI 只生成草稿协议，不自动建表、不自动发布，确认后才写入模型和应用草稿。",
                Map.of("autoSave", false)));
        return decisions;
    }

    private List<LowcodeAiAgentStepDTO> completedSteps(String finalSummary) {
        return List.of(
                step(1, "analyzing", "理解业务需求", "completed", "已提取业务目标、对象、字段和约束", "形成需求摘要"),
                step(2, "domain-planning", "划分业务领域", "completed", "已确定业务领域和对象归属", "领域可复用或新建"),
                step(3, "model-generating", "生成数据模型", "completed", "已生成模型字段、表名和基础策略", "模型确认后保存"),
                step(4, "page-generating", "生成应用页面", "completed", "已自动选择模板并生成页面协议", "应用确认后保存"),
                step(5, "validating", "校验低代码协议", "completed", "已完成运行时协议校验", finalSummary)
        );
    }

    private LowcodeAiAgentStepDTO step(Integer orderNo, String key, String title, String status, String message, String summary) {
        LowcodeAiAgentStepDTO step = new LowcodeAiAgentStepDTO();
        step.setOrderNo(orderNo);
        step.setStepKey(key);
        step.setTitle(title);
        step.setStatus(status);
        step.setMessage(message);
        step.setSummary(summary);
        return step;
    }

    private LowcodeAiDecisionDTO decision(String type, String title, String target, String value, String reason,
                                          Map<String, Object> meta) {
        LowcodeAiDecisionDTO decision = new LowcodeAiDecisionDTO();
        decision.setDecisionType(type);
        decision.setTitle(title);
        decision.setTarget(target);
        decision.setValue(value);
        decision.setReason(reason);
        decision.setMeta(meta == null ? new LinkedHashMap<>() : new LinkedHashMap<>(meta));
        return decision;
    }

    private Map<String, Object> progress(String stage, String title, String message, String status, String summary) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stage", stage);
        data.put("stepKey", stage);
        data.put("title", title);
        data.put("message", message);
        data.put("status", status);
        if (StringUtils.isNotBlank(summary)) {
            data.put("summary", summary);
        }
        return data;
    }

    private String summarizeApps(LowcodeAiAppGenerateResult result) {
        if (result == null || result.getApps() == null || result.getApps().isEmpty()) {
            return null;
        }
        return "已生成 " + result.getApps().size() + " 个应用草稿：" + result.getApps().stream()
                .map(LowcodeAppDraftDTO::getAppName)
                .toList();
    }

    private String summarizeRequirement(String description) {
        String text = StringUtils.normalizeSpace(StringUtils.defaultString(description));
        if (text.length() > 120) {
            return text.substring(0, 120) + "...";
        }
        return text;
    }

    private String inferObjectName(String description) {
        String text = StringUtils.defaultString(description).trim();
        if (text.length() > 18) {
            text = text.substring(0, 18);
        }
        text = text.replaceAll("[，。,.\\s].*$", "");
        return StringUtils.defaultIfBlank(text, "业务对象");
    }

    private String resolveLabelField(List<LowcodeFieldSchema> fields) {
        return fields.stream()
                .map(LowcodeFieldSchema::getField)
                .filter(field -> Set.of("name", "title", "label", "customerName", "productName", "departmentName").contains(field))
                .findFirst()
                .orElse(fields.isEmpty() ? "name" : fields.get(0).getField());
    }

    private boolean containsAny(String text, Set<String> keywords) {
        if (StringUtils.isBlank(text) || keywords == null || keywords.isEmpty()) {
            return false;
        }
        return keywords.stream().anyMatch(keyword -> StringUtils.isNotBlank(keyword) && text.contains(keyword));
    }

    private String normalizeCode(String value, String fallback, int maxLength) {
        String normalized = StringUtils.defaultString(value)
                .replaceAll("[^A-Za-z0-9_\\u4e00-\\u9fa5]+", "_")
                .replaceAll("[\\u4e00-\\u9fa5]+", fallback)
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("_+", "_")
                .toLowerCase(Locale.ROOT)
                .replaceAll("^[^a-z]+", "")
                .replaceAll("_+$", "");
        if (StringUtils.isBlank(normalized)) {
            normalized = fallback;
        }
        if (normalized.length() > maxLength) {
            normalized = normalized.substring(0, maxLength).replaceAll("_+$", "");
        }
        return normalized;
    }

    private String normalizePrefix(String prefix) {
        String value = normalizeCode(prefix, "biz", 48);
        return value.endsWith("_") ? value : value + "_";
    }

    private String parseRequestedTablePrefix(String description) {
        String text = StringUtils.defaultString(description);
        Matcher matcher = TABLE_PREFIX_PATTERN.matcher(text);
        if (matcher.find()) {
            return normalizePrefix(matcher.group(1));
        }
        matcher = TABLE_PREFIX_FALLBACK_PATTERN.matcher(text);
        if (matcher.find()) {
            return normalizePrefix(matcher.group(1));
        }
        return null;
    }

    private String normalizeTableName(String value) {
        String normalized = StringUtils.defaultString(value)
                .replaceAll("[^A-Za-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .toLowerCase(Locale.ROOT)
                .replaceAll("^[^a-z]+", "")
                .replaceAll("_+$", "");
        return StringUtils.defaultIfBlank(normalized, "biz_ai_model");
    }

    private String normalizeConfigKey(String value) {
        return normalizeCode(value, "ai_app", 64);
    }

    private String extractJson(String content) {
        String text = StringUtils.defaultString(content).trim();
        if (text.contains("```json")) {
            text = text.substring(text.indexOf("```json") + 7);
            text = text.substring(0, text.indexOf("```"));
            return text.trim();
        }
        if (text.contains("```")) {
            text = text.substring(text.indexOf("```") + 3);
            text = text.substring(0, text.indexOf("```"));
            return text.trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    private ServerSentEvent<String> event(String event, Object data) {
        try {
            return ServerSentEvent.builder(objectMapper.writeValueAsString(data))
                    .event(event)
                    .build();
        } catch (Exception e) {
            return ServerSentEvent.builder("{\"message\":\"事件序列化失败\"}")
                    .event("error")
                    .build();
        }
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? new ArrayList<>() : source;
    }

    private record DomainCandidate(String code, String name, String description, Set<String> keywords) {
    }

    private record ObjectCandidate(String code, String name, String domainCode, Set<String> keywords, boolean tree) {
    }

    private record ObjectPlan(String code, String name, String domainCode, boolean tree) {
    }
}

package com.mdframe.forge.flow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.flow.dto.FlowGenerateRequest;
import com.mdframe.forge.plugin.ai.client.AiClient;
import com.mdframe.forge.plugin.ai.client.dto.AiClientRequest;
import com.mdframe.forge.starter.flow.entity.FlowForm;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.entity.FlowSpelTemplate;
import com.mdframe.forge.starter.flow.service.FlowFormService;
import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import com.mdframe.forge.starter.flow.service.FlowSpelTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowBpmnGenerateService {

    private static final String FLOW_AGENT_CODE = "flow_bpmn_builder";
    private static final int MAX_CONTEXT_ITEMS = 50;

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final FlowSpelTemplateService flowSpelTemplateService;
    private final FlowFormService flowFormService;
    private final FlowNodeConfigService flowNodeConfigService;

    public Flux<ServerSentEvent<String>> streamGenerate(FlowGenerateRequest request) {
        if (!StringUtils.hasText(request.getDescription())) {
            return Flux.just(buildErrorEvent("流程需求不能为空"));
        }
        if (!StringUtils.hasText(request.getModelKey())) {
            return Flux.just(buildErrorEvent("modelKey不能为空"));
        }

        String sessionId = StringUtils.hasText(request.getSessionId())
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        AiClientRequest clientRequest = new AiClientRequest();
        clientRequest.setAgentCode(FLOW_AGENT_CODE);
        clientRequest.setSessionId(sessionId);
        clientRequest.setMessage(buildPrompt(request));
        clientRequest.setUserInput(request.getDescription());
        clientRequest.setProviderId(request.getProviderId());
        clientRequest.setTemperature(request.getTemperature() != null ? request.getTemperature() : 0.2D);
        clientRequest.setMaxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 12000);
        clientRequest.setContextVars(buildContextVars(request));
        applyAiModelSelection(clientRequest, request);

        return Flux.concat(
                Flux.just(buildProgressEvent("analyzing", "正在分析流程需求...")),
                aiClient.stream(clientRequest)
                        .map(this::buildChunkEvent)
                        .onErrorResume(e -> {
                            log.error("[FlowBpmnGenerateService] 流式生成失败", e);
                            return Flux.just(buildErrorEvent("生成失败: " + e.getMessage()));
                        }),
                Flux.just(buildCompleteEvent(sessionId))
        );
    }

    private void applyAiModelSelection(AiClientRequest clientRequest, FlowGenerateRequest request) {
        if (StringUtils.hasText(request.getModelId())) {
            clientRequest.setModelName(request.getModelId());
            return;
        }
        if (StringUtils.hasText(request.getAiModelName())) {
            clientRequest.setModelName(request.getAiModelName());
        }
    }

    private String buildPrompt(FlowGenerateRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 输出约束\n");
        sb.append("如返回 BPMN XML，必须是完整且语法合法的 XML。BPMNDI 图形信息只能使用 ");
        sb.append("bpmndi:BPMNDiagram、bpmndi:BPMNPlane、bpmndi:BPMNShape、bpmndi:BPMNEdge、dc:Bounds、di:waypoint。");
        sb.append("禁止输出 bpmdi:waypoint、bpmnwaypoint 等无效标签或属性，禁止把属性和节点文本拼接错位。\n\n");
        sb.append("以下是本次流程生成或修改请求的动态上下文。\n\n");
        sb.append("## 用户需求\n").append(request.getDescription()).append("\n\n");
        sb.append("## 当前流程模型\n");
        sb.append(toJson(buildModelContext(request))).append("\n\n");
        sb.append("## 平台流程配置上下文\n");
        sb.append(toJson(buildFlowConfigContext(request))).append("\n\n");
        if (StringUtils.hasText(request.getCurrentFormJson())) {
            sb.append("## 当前表单JSON\n").append(request.getCurrentFormJson()).append("\n\n");
        }
        sb.append("## 当前 BPMN XML\n");
        sb.append(StringUtils.hasText(request.getCurrentBpmnXml()) ? request.getCurrentBpmnXml() : "暂无");
        return sb.toString();
    }

    private Map<String, String> buildModelContext(FlowGenerateRequest request) {
        Map<String, String> model = new HashMap<>();
        model.put("flowModelId", safeText(request.getFlowModelId()));
        model.put("modelKey", safeText(request.getModelKey()));
        model.put("modelName", safeText(request.getModelName()));
        model.put("category", safeText(request.getCategory()));
        model.put("flowType", safeText(request.getFlowType()));
        model.put("formType", safeText(request.getFormType()));
        return model;
    }

    private Map<String, Object> buildFlowConfigContext(FlowGenerateRequest request) {
        Map<String, Object> context = new HashMap<>();
        context.put("assigneeTypes", List.of(
                option("user", "指定用户，生成 flowable:assignee 或 flowable:candidateUsers"),
                option("role", "按角色，优先生成 flowable:candidateGroups 或角色查询表达式"),
                option("dept", "按部门，使用部门变量或部门查询表达式"),
                option("post", "按岗位，使用岗位编码或岗位查询表达式"),
                option("leader", "发起人上级，可使用 ${flowSpelService.getInitiatorLeader(execution)}"),
                option("deptManager", "部门负责人，可使用 ${flowSpelService.findDeptManager(execution.getVariable(\"deptId\"))}"),
                option("initiator", "发起人，可使用 ${initiator}"),
                option("expr", "表达式，优先从 spelTemplates 中选择")
        ));
        context.put("formTypes", List.of(
                option("dynamic", "动态表单，使用 formJson/formSchema"),
                option("external", "外部表单，使用 flowable:formUrl"),
                option("builtin", "内置表单组件，使用 componentPath"),
                option("none", "无表单")
        ));
        context.put("multiInstanceTypes", List.of(
                option("none", "单人审批"),
                option("sequential", "依次审批"),
                option("parallel", "并行会签")
        ));
        context.put("completionConditions", List.of(
                option("all", "全部通过"),
                option("any", "任一通过"),
                option("rate", "按通过比例")
        ));
        context.put("timeoutActions", List.of(
                option("auto_pass", "超时自动通过"),
                option("auto_reject", "超时自动拒绝"),
                option("notify", "超时发送通知"),
                option("none", "无超时动作")
        ));
        context.put("spelTemplates", buildSpelTemplateContext());
        context.put("enabledForms", buildFormContext());
        context.put("currentNodeConfigs", buildCurrentNodeConfigContext(request));
        return context;
    }

    private Map<String, String> option(String value, String label) {
        Map<String, String> option = new HashMap<>();
        option.put("value", value);
        option.put("label", label);
        return option;
    }

    private List<Map<String, String>> buildSpelTemplateContext() {
        try {
            return flowSpelTemplateService.getEnabledList().stream()
                    .limit(MAX_CONTEXT_ITEMS)
                    .map(this::toSpelTemplateContext)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[FlowBpmnGenerateService] 获取SPEL模板上下文失败", e);
            return Collections.emptyList();
        }
    }

    private Map<String, String> toSpelTemplateContext(FlowSpelTemplate template) {
        Map<String, String> data = new HashMap<>();
        data.put("templateCode", safeText(template.getTemplateCode()));
        data.put("templateName", safeText(template.getTemplateName()));
        data.put("expression", safeText(template.getExpression()));
        data.put("description", safeText(template.getDescription()));
        data.put("category", safeText(template.getCategory()));
        data.put("exampleParams", safeText(template.getExampleParams()));
        return data;
    }

    private List<Map<String, String>> buildFormContext() {
        try {
            return flowFormService.getEnabledForms().stream()
                    .limit(MAX_CONTEXT_ITEMS)
                    .map(this::toFormContext)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[FlowBpmnGenerateService] 获取表单上下文失败", e);
            return Collections.emptyList();
        }
    }

    private Map<String, String> toFormContext(FlowForm form) {
        Map<String, String> data = new HashMap<>();
        data.put("formKey", safeText(form.getFormKey()));
        data.put("formName", safeText(form.getFormName()));
        data.put("formType", safeText(form.getFormType()));
        data.put("formUrl", safeText(form.getFormUrl()));
        data.put("componentPath", safeText(form.getComponentPath()));
        data.put("description", safeText(form.getDescription()));
        data.put("version", form.getVersion() == null ? "" : String.valueOf(form.getVersion()));
        return data;
    }

    private List<Map<String, String>> buildCurrentNodeConfigContext(FlowGenerateRequest request) {
        String modelId = StringUtils.hasText(request.getFlowModelId()) ? request.getFlowModelId() : request.getModelKey();
        if (!StringUtils.hasText(modelId)) {
            return Collections.emptyList();
        }
        try {
            return flowNodeConfigService.getByModelId(modelId).stream()
                    .limit(MAX_CONTEXT_ITEMS)
                    .map(this::toNodeConfigContext)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[FlowBpmnGenerateService] 获取当前节点配置上下文失败, modelId={}", modelId, e);
            return Collections.emptyList();
        }
    }

    private Map<String, String> toNodeConfigContext(FlowNodeConfig config) {
        Map<String, String> data = new HashMap<>();
        data.put("nodeId", safeText(config.getNodeId()));
        data.put("nodeName", safeText(config.getNodeName()));
        data.put("nodeType", safeText(config.getNodeType()));
        data.put("assigneeType", safeText(config.getAssigneeType()));
        data.put("assigneeValue", safeText(config.getAssigneeValue()));
        data.put("assigneeExpr", safeText(config.getAssigneeExpr()));
        data.put("multiInstanceType", safeText(config.getMultiInstanceType()));
        data.put("completionCondition", safeText(config.getCompletionCondition()));
        data.put("passRate", config.getPassRate() == null ? "" : config.getPassRate().toPlainString());
        data.put("formKey", safeText(config.getFormKey()));
        data.put("timeoutAction", safeText(config.getTimeoutAction()));
        data.put("allowDelegate", boolText(config.getAllowDelegate()));
        data.put("allowTransfer", boolText(config.getAllowTransfer()));
        data.put("allowAddSign", boolText(config.getAllowAddSign()));
        data.put("allowCounterSign", boolText(config.getAllowCounterSign()));
        data.put("allowReject", boolText(config.getAllowReject()));
        data.put("allowRejectToStart", boolText(config.getAllowRejectToStart()));
        data.put("allowWithdraw", boolText(config.getAllowWithdraw()));
        return data;
    }

    private Map<String, String> buildContextVars(FlowGenerateRequest request) {
        Map<String, String> vars = new HashMap<>();
        vars.put("flowModelId", safeText(request.getFlowModelId()));
        vars.put("modelKey", safeText(request.getModelKey()));
        vars.put("modelName", safeText(request.getModelName()));
        vars.put("category", safeText(request.getCategory()));
        vars.put("flowType", safeText(request.getFlowType()));
        vars.put("formType", safeText(request.getFormType()));
        vars.put("currentBpmnXml", safeText(request.getCurrentBpmnXml()));
        vars.put("currentFormJson", safeText(request.getCurrentFormJson()));
        vars.put("description", safeText(request.getDescription()));
        return vars;
    }

    private ServerSentEvent<String> buildProgressEvent(String stage, String message) {
        Map<String, String> data = new HashMap<>();
        data.put("stage", stage);
        data.put("message", message);
        return ServerSentEvent.builder(toJson(data))
                .event("progress")
                .build();
    }

    private ServerSentEvent<String> buildChunkEvent(String chunk) {
        Map<String, String> data = new HashMap<>();
        data.put("content", chunk);
        return ServerSentEvent.builder(toJson(data))
                .event("chunk")
                .build();
    }

    private ServerSentEvent<String> buildCompleteEvent(String sessionId) {
        Map<String, String> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("message", "生成完成");
        return ServerSentEvent.builder(toJson(data))
                .event("complete")
                .build();
    }

    private ServerSentEvent<String> buildErrorEvent(String message) {
        Map<String, String> data = new HashMap<>();
        data.put("message", message);
        return ServerSentEvent.builder(toJson(data))
                .event("error")
                .build();
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private String boolText(Boolean value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{\"message\":\"JSON序列化失败\"}";
        }
    }
}

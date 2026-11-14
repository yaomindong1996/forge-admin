package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.helper.BpmnXmlUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 模型部署前的 BPMN 文本兼容处理与可执行性预检。 */
@Slf4j
final class FlowModelBpmnPreflight {

    private FlowModelBpmnPreflight() {
    }

    static String extractProcessKey(String bpmnXml) {
        try {
            int start = bpmnXml.indexOf("<bpmn:process id=\"");
            if (start == -1) {
                start = bpmnXml.indexOf("<process id=\"");
            }
            if (start == -1) {
                return null;
            }
            start = bpmnXml.indexOf("id=\"", start) + 4;
            int end = bpmnXml.indexOf("\"", start);
            return bpmnXml.substring(start, end);
        } catch (Exception e) {
            log.warn("提取流程Key失败", e);
            return null;
        }
    }

    static String replaceProcessId(String bpmnXml, String modelKey) {
        try {
            String currentProcessId = extractProcessKey(bpmnXml);
            if (currentProcessId == null) {
                log.warn("无法提取当前流程ID，跳过替换");
                return bpmnXml;
            }
            if (currentProcessId.equals(modelKey)) {
                log.debug("流程ID已经是 {}，无需替换", modelKey);
                return bpmnXml;
            }
            log.info("将流程ID从 {} 替换为 {}", currentProcessId, modelKey);
            bpmnXml = bpmnXml.replace(
                    "<bpmn:process id=\"" + currentProcessId + "\"",
                    "<bpmn:process id=\"" + modelKey + "\"");
            bpmnXml = bpmnXml.replace(
                    "<process id=\"" + currentProcessId + "\"",
                    "<process id=\"" + modelKey + "\"");
            bpmnXml = bpmnXml.replace(
                    "bpmnElement=\"" + currentProcessId + "\"",
                    "bpmnElement=\"" + modelKey + "\"");
            return bpmnXml;
        } catch (Exception e) {
            log.warn("替换流程ID失败", e);
            return bpmnXml;
        }
    }

    static void validateSequenceFlowRefs(String bpmnXml) {
        java.util.regex.Pattern flowPattern = java.util.regex.Pattern.compile(
                "<(?:bpmn:)?sequenceFlow\\b([^>]*?)/?>",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Pattern idPattern = java.util.regex.Pattern.compile("\\bid=\"([^\"]*)\"");
        java.util.regex.Pattern targetRefPattern = java.util.regex.Pattern.compile("\\btargetRef=\"([^\"]*)\"");
        java.util.regex.Pattern sourceRefPattern = java.util.regex.Pattern.compile("\\bsourceRef=\"([^\"]*)\"");
        java.util.regex.Matcher matcher = flowPattern.matcher(bpmnXml);
        while (matcher.find()) {
            String flowElement = matcher.group(0);
            String attrs = matcher.group(1);
            java.util.regex.Matcher idMatcher = idPattern.matcher(flowElement);
            String flowId = idMatcher.find() ? idMatcher.group(1) : "unknown";
            boolean hasTargetRef = targetRefPattern.matcher(attrs).find();
            boolean hasSourceRef = sourceRefPattern.matcher(attrs).find();
            if (!hasTargetRef || !hasSourceRef) {
                String missing = !hasTargetRef ? "targetRef" : "sourceRef";
                log.error("BPMN sequenceFlow [{}] 缺少 {} 属性，XML 片段: {}",
                        flowId, missing, flowElement);
                throw new RuntimeException(String.format(
                        "流程图数据不完整：连线 [%s] 缺少 %s 属性。"
                                + "请在流程设计器中检查所有连线是否完整连接到目标节点，重新保存后再部署。",
                        flowId, missing));
            }
        }
    }

    static void validateBpmnStructure(String bpmnXml) {
        if (countMatches(bpmnXml, "<(?:bpmn:)?startEvent\\b") == 0) {
            throw new RuntimeException("流程模型缺少开始节点，请至少配置一个开始节点。");
        }
        if (countMatches(bpmnXml, "<(?:bpmn:)?endEvent\\b") == 0) {
            throw new RuntimeException("流程模型缺少结束节点，请至少配置一个结束节点。");
        }
        Set<String> nodeIds = new HashSet<>();
        java.util.regex.Matcher nodeMatcher = java.util.regex.Pattern.compile(
                "<(?:bpmn:)?(?:startEvent|endEvent|userTask|serviceTask|scriptTask|exclusiveGateway|parallelGateway|inclusiveGateway|callActivity|subProcess)\\b([^>]*)>",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(bpmnXml);
        java.util.regex.Pattern idPattern = java.util.regex.Pattern.compile("\\bid=\"([^\"]+)\"");
        while (nodeMatcher.find()) {
            java.util.regex.Matcher idMatcher = idPattern.matcher(nodeMatcher.group(1));
            if (idMatcher.find()) {
                nodeIds.add(idMatcher.group(1));
            }
        }
        java.util.regex.Matcher flowMatcher = java.util.regex.Pattern.compile(
                "<(?:bpmn:)?sequenceFlow\\b([^>]*)/?>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(bpmnXml);
        while (flowMatcher.find()) {
            String attrs = flowMatcher.group(1);
            String source = attributeValue(attrs, "sourceRef");
            String target = attributeValue(attrs, "targetRef");
            if (source == null || target == null || !nodeIds.contains(source) || !nodeIds.contains(target)) {
                throw new RuntimeException("流程模型存在悬空连线，请检查 sourceRef 和 targetRef 是否指向有效节点。");
            }
        }
    }

    static void validateExecutableNodesAndGatewayConditions(String bpmnXml) {
        java.util.regex.Pattern nodePattern = java.util.regex.Pattern.compile(
                "<(?:bpmn:)?(scriptTask|callActivity|subProcess|serviceTask|userTask)\\b([^>]*)>",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher nodeMatcher = nodePattern.matcher(bpmnXml);
        while (nodeMatcher.find()) {
            String type = nodeMatcher.group(1).toLowerCase(Locale.ROOT);
            String attrs = nodeMatcher.group(2);
            String id = attributeValue(attrs, "id");
            String name = attributeValue(attrs, "name");
            String label = (name == null || name.isBlank()) ? id : name;
            if ("scriptTask".equalsIgnoreCase(type)
                    || "callActivity".equalsIgnoreCase(type)
                    || "subProcess".equalsIgnoreCase(type)) {
                throw new RuntimeException(String.format(
                        "节点 [%s] 使用了暂不支持的执行类型 [%s]，请改用用户任务或受支持的抄送节点。",
                        label == null ? "未命名" : label, type));
            }
            if ("serviceTask".equalsIgnoreCase(type)
                    && !"cc".equalsIgnoreCase(attributeValue(attrs, "type"))) {
                throw new RuntimeException(String.format(
                        "节点 [%s] 的 serviceTask 未声明受支持的 flowable:type=cc，无法保证运行时执行委托。",
                        label == null ? "未命名" : label));
            }
            if ("serviceTask".equalsIgnoreCase(type)
                    && containsUnsupportedExecutionAttribute(attrs)) {
                throw new RuntimeException(String.format(
                        "节点 [%s] 包含未注册的执行委托属性，禁止通过 raw XML 绕过执行白名单。",
                        label == null ? "未命名" : label));
            }
            if ("userTask".equalsIgnoreCase(type)) {
                boolean hasAssignee = hasText(attributeValue(attrs, "assignee"));
                boolean hasCandidateUsers = hasText(attributeValue(attrs, "candidateUsers"));
                boolean hasCandidateGroups = hasText(attributeValue(attrs, "candidateGroups"));
                if (!hasAssignee && !hasCandidateUsers && !hasCandidateGroups) {
                    throw new RuntimeException(String.format(
                            "审批节点 [%s] 未配置处理人、候选用户或候选组，请先完成审批人配置。",
                            label == null ? "未命名" : label));
                }
            }
        }
        java.util.regex.Pattern gatewayPattern = java.util.regex.Pattern.compile(
                "<(?:bpmn:)?(exclusiveGateway|inclusiveGateway)\\b([^>]*)>([\\s\\S]*?)</(?:bpmn:)?\\1>",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Pattern flowPattern = java.util.regex.Pattern.compile(
                "<(?:bpmn:)?sequenceFlow\\b([^>]*)>([\\s\\S]*?)</(?:bpmn:)?sequenceFlow>|"
                        + "<(?:bpmn:)?sequenceFlow\\b([^>]*)/>",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher gatewayMatcher = gatewayPattern.matcher(bpmnXml);
        while (gatewayMatcher.find()) {
            String gatewayAttrs = gatewayMatcher.group(2);
            String gatewayId = attributeValue(gatewayAttrs, "id");
            if (gatewayId == null) {
                continue;
            }
            String defaultFlow = attributeValue(gatewayAttrs, "default");
            int outgoingCount = 0;
            java.util.regex.Matcher flowMatcher = flowPattern.matcher(bpmnXml);
            while (flowMatcher.find()) {
                String flowAttrs = flowMatcher.group(1) != null ? flowMatcher.group(1) : flowMatcher.group(3);
                if (!gatewayId.equals(attributeValue(flowAttrs, "sourceRef"))) {
                    continue;
                }
                outgoingCount++;
                String flowId = attributeValue(flowAttrs, "id");
                boolean hasCondition = flowMatcher.group(2) != null
                        && flowMatcher.group(2).toLowerCase(Locale.ROOT).contains("conditionexpression");
                if (!hasCondition && !Objects.equals(defaultFlow, flowId)) {
                    throw new RuntimeException(String.format(
                            "网关 [%s] 的分支连线 [%s] 缺少条件表达式或默认分支。",
                            gatewayId, flowId == null ? "未命名" : flowId));
                }
            }
            if (outgoingCount > 1 && defaultFlow == null) {
                log.debug("网关 [{}] 未声明 default，但所有出口均已配置条件表达式", gatewayId);
            }
        }
    }

    static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    static String normalizeBpmnXml(String bpmnXml, String operation) {
        BpmnXmlUtils.NormalizationResult result = BpmnXmlUtils.normalizeDuplicateSequenceFlows(bpmnXml);
        if (result.hasRepairs()) {
            String repairSummary = result.getRepairs().stream()
                    .map(repair -> String.format("%s->%s 保留 [%s] 删除 %s",
                            repair.getSourceRef(), repair.getTargetRef(), repair.getKeptFlowId(),
                            repair.getRemovedFlowIds()))
                    .collect(Collectors.joining("; "));
            log.warn("{}：已自动清理 BPMN 重复连线，{}", operation, repairSummary);
        }
        BpmnXmlUtils.LegacyMultiInstanceNormalizationResult multiInstanceResult =
                BpmnXmlUtils.normalizeLegacyMultiInstanceExpressions(result.getBpmnXml());
        if (multiInstanceResult.hasRepairs()) {
            log.warn("{}：已自动清理 BPMN 旧版会签表达式，节点={}",
                    operation, multiInstanceResult.getNormalizedNodeIds());
        }
        return multiInstanceResult.getBpmnXml();
    }

    private static final java.util.regex.Pattern CC_SERVICE_TASK_TYPE = java.util.regex.Pattern.compile(
            "(<(?:bpmn:)?serviceTask\\b[^>]*?\\s)flowable:type=\"cc\"", java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * 部署前转换为引擎可执行 XML：flowable:type 是 Flowable 服务任务保留属性（mail/http 等），
     * 设计器用它标记抄送节点会触发 flowable-servicetask-invalid-type；部署时改写为不冲突的
     * flowable:forgeNodeType，执行仍由 ${flowCcNodeDelegate} 委托完成。模型保存的 XML 保持原样供设计器识别。
     */
    static String toEngineExecutableXml(String bpmnXml) {
        if (bpmnXml == null || bpmnXml.isEmpty()) {
            return bpmnXml;
        }
        return CC_SERVICE_TASK_TYPE.matcher(bpmnXml).replaceAll("$1flowable:forgeNodeType=\"cc\"");
    }

    /** 设计器抄送节点固定绑定的已注册委托 Bean（FlowCcNodeDelegate）；其它委托表达式一律拒绝。 */
    private static final String REGISTERED_CC_DELEGATE_EXPRESSION = "${flowCcNodeDelegate}";

    private static boolean containsUnsupportedExecutionAttribute(String attributes) {
        String normalized = attributes == null ? "" : attributes.toLowerCase(Locale.ROOT);
        boolean registeredCcDelegate = "cc".equalsIgnoreCase(attributeValue(attributes, "flowable:type"))
                && REGISTERED_CC_DELEGATE_EXPRESSION.equals(attributeValue(attributes, "flowable:delegateExpression"))
                && countMatches(normalized, "delegateexpression=") == 1;
        return normalized.contains("flowable:class=")
                || (normalized.contains("flowable:delegateexpression=") && !registeredCcDelegate)
                || normalized.contains("flowable:expression=")
                || normalized.contains("activiti:class=")
                || normalized.contains("activiti:delegateexpression=")
                || normalized.contains("activiti:expression=");
    }

    private static int countMatches(String value, String regex) {
        int count = 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex,
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(value);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static String attributeValue(String attributes, String name) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "\\b" + name + "=\"([^\"]*)\"").matcher(attributes);
        return matcher.find() ? matcher.group(1) : null;
    }
}

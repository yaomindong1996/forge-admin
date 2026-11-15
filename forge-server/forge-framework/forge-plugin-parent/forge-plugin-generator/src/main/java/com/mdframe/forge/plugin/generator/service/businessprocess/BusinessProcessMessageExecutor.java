package com.mdframe.forge.plugin.generator.service.businessprocess;

import com.mdframe.forge.plugin.generator.businessprocess.schema.BusinessProcessNode;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcess;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcessRun;
import com.mdframe.forge.plugin.generator.mapper.BusinessProcessMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessMessageChannelService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessMessageTenantContext;
import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Explicit message node only; binding a connection never adds notification nodes automatically. */
@Component
@RequiredArgsConstructor
public class BusinessProcessMessageExecutor {
    private final BusinessMessageChannelService channels;
    private final MessageService messages;
    private final BusinessProcessMapper processMapper;

    public String execute(AiBusinessProcessRun run, BusinessProcessNode node) {
        if (run.getTenantId() == null || !Objects.equals(run.getTenantId(), BusinessMessageTenantContext.requireTenantId())
                || run.getActorUserId() == null || run.getApplicationId() == null || run.getId() == null)
            throw new BusinessException("消息节点缺少可信租户、应用或发起人上下文");
        if (node == null || node.getId() == null || node.getId().isBlank())
            throw new BusinessException("消息节点缺少稳定节点标识，请重新发布流程");
        Map<String, Object> config = node.getConfig() == null ? Map.of() : node.getConfig();
        Object collaboration = config.get("useApplicationCollaboration");
        if (collaboration != null && !(collaboration instanceof Boolean))
            throw new BusinessException("企业协同发送开关必须为布尔值，请重新配置消息节点");
        String template = Objects.toString(config.get("messageTemplateCode"), "").trim();
        if (template.isBlank()) throw new BusinessException("请配置消息模板");
        // Published flows can outlive a template: do not silently enqueue empty content.
        if (messages.resolveEnabledTemplateCode(template) == null)
            throw new BusinessException("消息模板不存在或已停用，请检查消息模板配置");
        MessageSendRequestDTO request = new MessageSendRequestDTO();
        request.setTemplateCode(template);
        request.setChannel("WEB");
        if (Boolean.TRUE.equals(collaboration)) {
            var channel = channels.resolveChannel("app_" + run.getApplicationId() + "_collaboration");
            if (channel.getConnectionId() == null || !Boolean.TRUE.equals(channel.getEnabled()))
                throw new BusinessException("应用企业协同消息通道不可用");
            request.setChannel(channel.getSendChannel());
            request.setConnectionId(channel.getConnectionId());
        }
        request.setSendScope("USERS");
        request.setUserIds(Set.of(run.getActorUserId()));
        request.setType("SYSTEM");
        request.setBizType("BUSINESS_PROCESS");
        request.setBizKey(run.getBusinessKey());
        request.setIdempotencyKey("business-process:" + run.getId() + ":" + node.getId());
        request.setParams(buildTemplateParams(run, node));
        messages.send(request);
        return "消息已入队，接收人为流程发起人；实际投递结果请查看消息中心";
    }

    /**
     * 补齐 FLOW_TODO_CARD 等通用模板占位符，避免站内信正文残留 ${taskTitle}。
     * 业务流程没有 Flowable 任务标题时，用流程名称 / 编码兜底。
     */
    Map<String, Object> buildTemplateParams(AiBusinessProcessRun run, BusinessProcessNode node) {
        Map<String, Object> params = new LinkedHashMap<>();
        String processCode = Objects.toString(run.getProcessCode(), "").trim();
        String processName = resolveProcessName(run.getTenantId(), run.getProcessId(), processCode);
        String nodeName = node == null ? "" : Objects.toString(node.getName(), "").trim();
        String taskTitle = firstNonBlank(nodeName, processName, processCode, "业务流程通知");
        params.put("objectCode", Objects.toString(run.getSubjectObjectCode(), ""));
        params.put("recordId", Objects.toString(run.getSubjectRecordId(), ""));
        params.put("processCode", processCode);
        params.put("processName", processName);
        params.put("taskTitle", taskTitle);
        // 接收人即发起人；用户名由消息中心渲染侧可再补，这里先给稳定空串避免字面量残留
        params.put("startUserName", "");
        return params;
    }

    private String resolveProcessName(Long tenantId, Long processId, String processCode) {
        if (tenantId != null && processId != null) {
            AiBusinessProcess process = processMapper.selectActiveById(tenantId, processId);
            if (process != null) {
                String name = Objects.toString(process.getProcessName(), "").trim();
                if (!name.isBlank())
                    return name;
            }
        }
        return processCode;
    }

    private static String firstNonBlank(String... values) {
        if (values == null)
            return "";
        for (String value : values) {
            if (value != null && !value.isBlank())
                return value.trim();
        }
        return "";
    }
}

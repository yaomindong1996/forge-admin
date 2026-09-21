package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcessRun;
import com.mdframe.forge.plugin.generator.mapper.BusinessProcessRunMapper;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.plugin.print.spi.PrintRecordRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从 Flow 服务的受保护接口解析打印身份。客户端提交的 object/record/task/instance/run
 * 只作为待核验声明，不能直接进入打印上下文。
 */
@Component
@RequiredArgsConstructor
public class FlowPrintContextResolver {

    private final ObjectProvider<FlowClient> flowClients;
    private final BusinessProcessRunMapper processRuns;

    public record Context(String businessKey,
                          String objectCode,
                          String recordId,
                          String processInstanceId,
                          Long processRunId,
                          String taskId,
                          String taskDefKey,
                          Integer taskStatus,
                          String assignee,
                          String owner,
                          String startUserId,
                          String formKey,
                          String formFieldPermissions,
                          String printTemplatePolicy,
                          String printTemplateIds) {
    }

    public Context resolve(PrintActor actor, PrintRecordRequest request) {
        if (actor == null || request == null || !request.isSceneValid() || !isFlowScene(request.scene())) {
            throw PrintFailure.denied();
        }
        FlowClient client = flowClients.getIfAvailable();
        if (client == null) {
            throw PrintFailure.of(503, "PRINT_FLOW_UNAVAILABLE", "流程服务未配置，无法校验打印身份");
        }

        Map<String, Object> task = request.taskId() == null
                ? Map.of()
                : data(client.getTaskDetail(request.taskId()));
        String processInstanceId = first(text(task.get("processInstanceId")), request.processInstanceId());
        String businessKey = text(task.get("businessKey"));
        String processDefKey = text(task.get("processDefKey"));
        String taskDefKey = text(task.get("taskDefKey"));
        Map<String, Object> form = data(client.getProcessFormInfo(
                processInstanceId,
                businessKey,
                processDefKey,
                request.taskId(),
                taskDefKey));

        processInstanceId = first(text(form.get("processInstanceId")), processInstanceId);
        businessKey = first(text(form.get("businessKey")), businessKey);
        taskDefKey = first(text(form.get("taskDefKey")), taskDefKey);
        String objectCode = first(
                text(form.get("objectCode")),
                text(nested(form.get("formRef")).get("objectCode")),
                businessObjectCode(businessKey));
        String recordId = first(
                text(form.get("recordId")),
                text(nested(form.get("variables")).get("recordId")),
                businessRecordId(businessKey));
        String serverFormKey = text(form.get("formKey"));
        String formKey = first(serverFormKey, request.source().formKey());

        requireEqual(request.processInstanceId(), processInstanceId);
        requireEqual(request.source().objectCode(), objectCode);
        requireEqual(request.recordId(), recordId);
        if (request.source().sourceType() == com.mdframe.forge.plugin.print.enums.PrintSourceType.CODE) {
            requireEqual(request.source().formKey(), serverFormKey);
        }
        if (request.taskId() != null) {
            requireEqual(request.taskId(), first(text(task.get("taskId")), text(task.get("id"))));
        }
        requireBusinessKey(businessKey, objectCode, recordId);

        AiBusinessProcessRun run = resolveRun(actor, request, processInstanceId, businessKey, objectCode, recordId);
        Long runId = run == null ? null : run.getId();
        return new Context(
                businessKey,
                objectCode,
                recordId,
                processInstanceId,
                runId,
                request.taskId(),
                taskDefKey,
                integer(task.get("status")),
                text(task.get("assignee")),
                text(task.get("owner")),
                first(text(form.get("startUserId")), text(task.get("startUserId"))),
                formKey,
                text(form.get("formFieldPermissions")),
                text(form.get("printTemplatePolicy")),
                text(form.get("printTemplateIds")));
    }

    /**
     * 列表/详情打印可选解析：按业务单据找最近流程实例。找不到返回 null，不拦截打印。
     */
    public Context resolveForRecord(PrintActor actor, PrintRecordRequest request) {
        if (actor == null || request == null || request.source() == null) {
            return null;
        }
        String objectCode = text(request.source().objectCode());
        String recordId = text(request.recordId());
        if (objectCode == null || recordId == null || request.source().applicationId() == null) {
            return null;
        }
        AiBusinessProcessRun run = processRuns.selectLatestBySubject(
                actor.tenantId(), request.source().applicationId(), objectCode, recordId);
        if (run == null || text(run.getFlowProcessInstanceId()) == null) {
            return null;
        }
        return new Context(
                first(text(run.getBusinessKey()), objectCode + ":" + recordId),
                objectCode,
                recordId,
                run.getFlowProcessInstanceId(),
                run.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private AiBusinessProcessRun resolveRun(PrintActor actor,
                                            PrintRecordRequest request,
                                            String processInstanceId,
                                            String businessKey,
                                            String objectCode,
                                            String recordId) {
        AiBusinessProcessRun run = request.processRunId() == null
                ? processRuns.selectByProcessInstanceId(actor.tenantId(), processInstanceId)
                : processRuns.selectRunById(actor.tenantId(), request.processRunId());
        if (request.processRunId() != null && run == null) {
            throw PrintFailure.denied();
        }
        if (run == null) {
            return null;
        }
        if (!request.source().applicationId().equals(run.getApplicationId())
                || !same(run.getFlowProcessInstanceId(), processInstanceId)
                || !same(run.getBusinessKey(), businessKey)
                || !same(run.getSubjectObjectCode(), objectCode)
                || !same(run.getSubjectRecordId(), recordId)) {
            throw PrintFailure.denied();
        }
        return run;
    }

    private Map<String, Object> data(FlowResult<Map<String, Object>> result) {
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw PrintFailure.denied();
        }
        return new LinkedHashMap<>(result.getData());
    }

    private Map<String, Object> nested(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private void requireBusinessKey(String businessKey, String objectCode, String recordId) {
        String expected = objectCode + ":" + recordId;
        if (!same(expected, businessKey) && !StringUtils.startsWith(businessKey, expected + ":R")) {
            throw PrintFailure.denied();
        }
    }

    private String businessObjectCode(String businessKey) {
        int split = StringUtils.indexOf(businessKey, ':');
        return split <= 0 ? null : businessKey.substring(0, split);
    }

    private String businessRecordId(String businessKey) {
        int split = StringUtils.indexOf(businessKey, ':');
        if (split < 0 || split == businessKey.length() - 1) {
            return null;
        }
        String value = businessKey.substring(split + 1);
        int retry = value.indexOf(":R");
        return retry < 0 ? value : value.substring(0, retry);
    }

    private void requireEqual(String requested, String actual) {
        if (!same(requested, actual)) {
            throw PrintFailure.denied();
        }
    }

    private boolean same(String left, String right) {
        return StringUtils.equals(StringUtils.trimToNull(left), StringUtils.trimToNull(right));
    }

    private String first(String... values) {
        return StringUtils.firstNonBlank(values);
    }

    private String text(Object value) {
        return value == null ? null : StringUtils.trimToNull(String.valueOf(value));
    }

    private Integer integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isFlowScene(PrintScene scene) {
        return scene == PrintScene.FLOW_TODO || scene == PrintScene.FLOW_DONE || scene == PrintScene.FLOW_STARTED;
    }
}

package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 按流程实例读取稳定顺序的真实任务记录，不按节点名称合并会签或重提轮次。 */
@Component
@RequiredArgsConstructor
public class FlowPrintHistoryAdapter {

    private static final int HISTORY_LIMIT = 1000;

    private final ObjectProvider<FlowClient> flowClients;

    public List<PrintFieldCatalogVO.Field> catalog() {
        return List.of(
                new PrintFieldCatalogVO.Field("flow.processInstanceId", "流程实例ID", "TEXT"),
                new PrintFieldCatalogVO.Field("flow.businessKey", "业务Key", "TEXT"),
                new PrintFieldCatalogVO.Field("flow.taskDefKey", "当前节点", "TEXT"),
                new PrintFieldCatalogVO.Field("flow.history", "审批记录", "COLLECTION"),
                new PrintFieldCatalogVO.Field("flow.history.taskId", "任务ID", "TEXT"),
                new PrintFieldCatalogVO.Field("flow.history.taskName", "节点名称", "TEXT"),
                new PrintFieldCatalogVO.Field("flow.history.assigneeName", "办理人", "TEXT"),
                new PrintFieldCatalogVO.Field("flow.history.action", "办理动作", "TEXT"),
                new PrintFieldCatalogVO.Field("flow.history.comment", "办理意见", "TEXT"),
                new PrintFieldCatalogVO.Field("flow.history.signature", "办理签名", "IMAGE"),
                new PrintFieldCatalogVO.Field("flow.history.createTime", "到达时间", "DATE"),
                new PrintFieldCatalogVO.Field("flow.history.completeTime", "完成时间", "DATE"));
    }

    public Map<String, Object> load(FlowPrintContextResolver.Context context) {
        FlowClient client = flowClients.getIfAvailable();
        if (client == null) {
            throw PrintFailure.of(503, "PRINT_FLOW_UNAVAILABLE", "流程服务未配置，无法读取审批记录");
        }
        FlowResult<Map<String, Object>> response = client.getProcessHistoryPage(
                context.processInstanceId(), 1, HISTORY_LIMIT);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw PrintFailure.denied();
        }
        Object raw = response.getData().get("records");
        if (!(raw instanceof List<?> records) || records.size() > HISTORY_LIMIT) {
            throw PrintFailure.of(400, "PRINT_DATA_LIMIT", "审批记录超过打印上限");
        }
        List<Map<String, Object>> history = new ArrayList<>();
        for (Object value : records) {
            if (!(value instanceof Map<?, ?> item)) {
                throw PrintFailure.denied();
            }
            Map<String, Object> row = new LinkedHashMap<>();
            copy(row, item, "taskId");
            copy(row, item, "taskName");
            copy(row, item, "assigneeName");
            copy(row, item, "action");
            copy(row, item, "comment");
            copy(row, item, "signature");
            copy(row, item, "createTime");
            copy(row, item, "completeTime");
            history.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processInstanceId", context.processInstanceId());
        result.put("businessKey", context.businessKey());
        result.put("taskDefKey", context.taskDefKey());
        result.put("history", history);
        return result;
    }

    /** 无流程实例时仍提供空审批记录，避免列表/详情模板因缺集合而失败。 */
    public Map<String, Object> empty() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processInstanceId", null);
        result.put("businessKey", null);
        result.put("taskDefKey", null);
        result.put("history", List.of());
        return result;
    }

    private void copy(Map<String, Object> target, Map<?, ?> source, String key) {
        Object value = source.get(key);
        target.put(key, value == null ? null : String.valueOf(value));
    }
}

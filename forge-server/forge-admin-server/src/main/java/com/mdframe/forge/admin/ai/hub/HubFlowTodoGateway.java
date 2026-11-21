package com.mdframe.forge.admin.ai.hub;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 全局 AI 助手待办只读查询网关，直接复用 FlowClient 的当前登录用户待办接口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
class HubFlowTodoGateway {

    private final ObjectProvider<FlowClient> flowClientProvider;

    TodoPage list(HubAccess.Actor actor, int pageNum, int pageSize) {
        int safePage = Math.max(1, pageNum);
        int safeSize = Math.min(Math.max(1, pageSize), 20);
        FlowClient client = flowClientProvider.getIfAvailable();
        if (client == null) {
            return TodoPage.unavailable("当前环境未接入流程服务");
        }
        try {
            FlowResult<Map<String, Object>> result = client.getTodoTasks(
                    String.valueOf(actor.userId()), safePage, safeSize);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                return TodoPage.unavailable("流程服务暂不可用，请稍后前往待办中心查看");
            }
            Map<String, Object> data = result.getData();
            long total = toLong(data.get("total"));
            Object recordsObj = data.get("records") != null ? data.get("records") : data.get("list");
            List<?> records = recordsObj instanceof List<?> list ? list : List.of();
            return new TodoPage(true, total, records, null);
        } catch (Exception ex) {
            log.warn("[HubAssistant] 待办查询降级: userId={}, exceptionType={}",
                    actor.userId(), ex.getClass().getSimpleName());
            return TodoPage.unavailable("流程服务暂未启动或连接超时，请稍后重试");
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number num) {
            return num.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    public record TodoPage(boolean available, long total, List<?> records, String message) {
        static TodoPage unavailable(String message) {
            return new TodoPage(false, 0L, List.of(), message);
        }
    }
}

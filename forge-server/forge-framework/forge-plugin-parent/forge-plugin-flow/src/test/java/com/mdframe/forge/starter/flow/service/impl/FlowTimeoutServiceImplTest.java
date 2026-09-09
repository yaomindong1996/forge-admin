package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlowTimeoutServiceImplTest {

    @Test
    void shouldNotReportSuccessBeforeNotificationChannelsAreImplemented() {
        TaskService taskService = mock(TaskService.class);
        TaskQuery taskQuery = mock(TaskQuery.class);
        Task task = mock(Task.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId("task-1")).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);
        when(task.getAssignee()).thenReturn("1001");

        FlowTimeoutServiceImpl service = new FlowTimeoutServiceImpl(
                taskService,
                mock(RuntimeService.class),
                mock(HistoryService.class),
                mock(FlowNodeConfigService.class),
                mock(FlowTaskMapper.class));

        assertThat(service.sendTimeoutNotification("task-1", "system")).isFalse();
        assertThat(service.handleTimeoutTask("task-1", "notify")).isFalse();
    }
}

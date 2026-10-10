package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.flow.security.FlowAccessGuard;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlowTaskRemindCoordinatorTest {

    private static final String TASK_ID = "task-1";
    private static final String EXPECTED_KEY = "forge:flow:remind:1:task-1:2001";

    private TaskService taskService;
    private Task task;
    private FlowAccessGuard accessGuard;
    private MessageService messageService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        taskService = mock(TaskService.class);
        TaskQuery taskQuery = mock(TaskQuery.class);
        task = mock(Task.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId(TASK_ID)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);
        when(task.getId()).thenReturn(TASK_ID);
        when(task.getName()).thenReturn("部门负责人审批");
        when(task.getAssignee()).thenReturn("1001");
        accessGuard = mock(FlowAccessGuard.class);
        messageService = mock(MessageService.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private FlowTaskRemindCoordinator coordinator(StringRedisTemplate template) {
        return new FlowTaskRemindCoordinator(taskService, accessGuard, messageService, template, () -> 1L, () -> 2001L);
    }

    @Test
    void remindKeyIsScopedByTenantTaskAndReminder() {
        assertThat(FlowTaskRemindCoordinator.remindKey(1L, TASK_ID, 2001L)).isEqualTo(EXPECTED_KEY);
        assertThat(FlowTaskRemindCoordinator.REMIND_INTERVAL).hasMinutes(10);
    }

    @Test
    void firstRemindSendsMessageToAssignee() {
        when(valueOperations.setIfAbsent(EXPECTED_KEY, "1", FlowTaskRemindCoordinator.REMIND_INTERVAL)).thenReturn(true);

        coordinator(redisTemplate).remind(TASK_ID);

        verify(accessGuard).requireTaskVisible(TASK_ID);
        ArgumentCaptor<MessageSendRequestDTO> captor = ArgumentCaptor.forClass(MessageSendRequestDTO.class);
        verify(messageService).send(captor.capture());
        assertThat(captor.getValue().getUserIds()).containsExactly(1001L);
        assertThat(captor.getValue().getTitle()).isEqualTo("流程催办提醒");
    }

    @Test
    void repeatedRemindWithinIntervalIsRejected() {
        when(valueOperations.setIfAbsent(EXPECTED_KEY, "1", FlowTaskRemindCoordinator.REMIND_INTERVAL)).thenReturn(false);

        assertThatThrownBy(() -> coordinator(redisTemplate).remind(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("已催办过，请 10 分钟后再试");
        verify(messageService, never()).send(any());
    }

    @Test
    void unclaimedTaskIsRejectedBeforeThrottle() {
        when(task.getAssignee()).thenReturn(null);

        assertThatThrownBy(() -> coordinator(redisTemplate).remind(TASK_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("任务尚未被签收，暂时无法催办");
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), any());
        verify(messageService, never()).send(any());
    }

    @Test
    void redisFailureDoesNotBlockRemind() {
        when(valueOperations.setIfAbsent(eq(EXPECTED_KEY), eq("1"), any()))
                .thenThrow(new RedisConnectionFailureException("down"));

        coordinator(redisTemplate).remind(TASK_ID);

        verify(messageService).send(any());
    }

    @Test
    void missingRedisDoesNotBlockRemind() {
        coordinator(null).remind(TASK_ID);

        verify(messageService).send(any());
    }

    @Test
    void sendFailureKeepsThrottleKey() {
        when(valueOperations.setIfAbsent(EXPECTED_KEY, "1", FlowTaskRemindCoordinator.REMIND_INTERVAL)).thenReturn(true);
        doThrow(new IllegalStateException("mail down")).when(messageService).send(any());

        coordinator(redisTemplate).remind(TASK_ID);

        verify(redisTemplate, never()).delete(anyString());
    }
}

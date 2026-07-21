package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import com.mdframe.forge.plugin.job.support.JobLogSanitizer;
import com.mdframe.forge.plugin.job.vo.JobFailureAlarmContextVO;
import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.message.channel.MessageChannel;
import com.mdframe.forge.starter.message.sdk.MessageClient;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 在单次执行进入最终失败状态后发送安全裁剪的通知。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobFailureAlarmService {

    private static final String BIZ_TYPE = "JOB_FAILURE";
    private static final String CHANNEL_WEB = "WEB";
    private static final String CHANNEL_EMAIL = "EMAIL";
    private static final int MAX_ALARM_SUMMARY_LENGTH = 500;
    private static final DateTimeFormatter FAILURE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysJobLogMapper jobLogMapper;
    private final JobLogSanitizer logSanitizer;
    private final ObjectProvider<MessageService> messageServiceProvider;
    private final ObjectProvider<MessageClient> messageClientProvider;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public void notifyFinalFailure(Long executionId) {
        try {
            doNotifyFinalFailure(executionId);
        } catch (RuntimeException exception) {
            recordFailure(executionId, "UNKNOWN", exception.getClass().getSimpleName());
        }
    }

    private void doNotifyFinalFailure(Long executionId) {
        JobFailureAlarmContextVO context = jobLogMapper.selectFailureAlarmContext(executionId);
        if (context == null || !Integer.valueOf(1).equals(context.getAlarmEnabled())) {
            return;
        }
        Set<String> channels = parseCsv(context.getAlarmChannels());
        String title = "定时任务执行失败：" + safeText(context.getJobName(), "未知任务");
        String content = buildContent(context);
        if (channels.contains(CHANNEL_WEB)) {
            sendWeb(context, title, content);
        }
        if (channels.contains(CHANNEL_EMAIL)) {
            sendEmail(context, title, content);
        }
    }

    private void sendWeb(JobFailureAlarmContextVO context, String title, String content) {
        Set<Long> userIds = parseUserIds(context.getAlarmRecipientUserIds());
        if (userIds.isEmpty()) {
            recordFailure(context.getExecutionId(), CHANNEL_WEB, "NO_RECIPIENT");
            return;
        }
        MessageService messageService = messageServiceProvider.getIfAvailable();
        if (messageService == null) {
            recordFailure(context.getExecutionId(), CHANNEL_WEB, "SERVICE_UNAVAILABLE");
            return;
        }
        MessageSendRequestDTO request = new MessageSendRequestDTO();
        request.setTitle(title);
        request.setContent(content);
        request.setUserIds(userIds);
        request.setSendScope("USERS");
        request.setChannel(CHANNEL_WEB);
        request.setType("SYSTEM");
        try {
            messageService.sendIfAbsent(request, BIZ_TYPE,
                    context.getExecutionId() + ":" + CHANNEL_WEB);
        } catch (RuntimeException exception) {
            recordFailure(context.getExecutionId(), CHANNEL_WEB,
                    exception.getClass().getSimpleName());
        }
    }

    private void sendEmail(JobFailureAlarmContextVO context, String title, String content) {
        List<String> emails = List.copyOf(parseCsv(context.getAlarmEmail()));
        if (emails.isEmpty()) {
            recordFailure(context.getExecutionId(), CHANNEL_EMAIL, "NO_RECIPIENT");
            return;
        }
        MessageClient messageClient = messageClientProvider.getIfAvailable();
        if (messageClient == null) {
            recordFailure(context.getExecutionId(), CHANNEL_EMAIL, "SERVICE_UNAVAILABLE");
            return;
        }
        MessageChannel.SendRequest request = new MessageChannel.SendRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setChannel(CHANNEL_EMAIL);
        request.setType("SYSTEM");
        request.setEmailList(emails);
        try {
            MessageChannel.SendResult result = messageClient.send(request);
            if (result == null || !result.success) {
                recordFailure(context.getExecutionId(), CHANNEL_EMAIL, "CHANNEL_REJECTED");
            }
        } catch (RuntimeException exception) {
            recordFailure(context.getExecutionId(), CHANNEL_EMAIL,
                    exception.getClass().getSimpleName());
        }
    }

    private String buildContent(JobFailureAlarmContextVO context) {
        String summary = logSanitizer.sanitizeException(context.getExceptionSummary());
        if (StringUtils.isBlank(summary)) {
            summary = "任务执行失败，未返回异常摘要";
        }
        int lineBreak = summary.indexOf('\n');
        if (lineBreak >= 0) {
            summary = summary.substring(0, lineBreak);
        }
        summary = StringUtils.abbreviate(summary, MAX_ALARM_SUMMARY_LENGTH);
        String failureTime = context.getFailureTime() == null
                ? "未知"
                : FAILURE_TIME_FORMAT.format(context.getFailureTime());
        return "任务：" + safeText(context.getJobGroup(), "DEFAULT") + "."
                + safeText(context.getJobName(), "未知任务") + "\n"
                + "执行ID：" + context.getExecutionId() + "\n"
                + "失败时间：" + failureTime + "\n"
                + "异常摘要：" + summary + "\n"
                + "详情入口：/system/job-config?jobConfigId=" + context.getJobConfigId()
                + "&executionId=" + context.getExecutionId();
    }

    private Set<Long> parseUserIds(String value) {
        Set<Long> result = new LinkedHashSet<>();
        for (String item : parseCsv(value)) {
            try {
                long userId = Long.parseLong(item);
                if (userId > 0) {
                    result.add(userId);
                }
            } catch (NumberFormatException ignored) {
                log.warn("定时任务告警收件人ID被忽略: reason=INVALID_USER_ID");
            }
        }
        return result;
    }

    private Set<String> parseCsv(String value) {
        Set<String> result = new LinkedHashSet<>();
        if (StringUtils.isBlank(value)) {
            return result;
        }
        for (String item : value.split(",")) {
            String normalized = StringUtils.trimToNull(item);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.defaultIfBlank(StringUtils.trim(value), fallback);
    }

    private void recordFailure(Long executionId, String channel, String reasonCode) {
        log.error("定时任务最终失败告警发送失败: executionId={}, channel={}, reasonCode={}",
                executionId, channel, reasonCode);
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry != null) {
            meterRegistry.counter("forge.job.alarm.send.failures", "channel", channel).increment();
        }
    }
}

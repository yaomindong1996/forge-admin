package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.dto.JobCronPreviewRequest;
import com.mdframe.forge.plugin.job.constant.JobScheduleType;
import com.mdframe.forge.plugin.job.vo.JobCronPreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.text.ParseException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Cron表达式校验、说明与执行时间预览。
 */
@Service
@RequiredArgsConstructor
public class JobCronService {

    private static final int PREVIEW_COUNT = 5;

    private static final Map<String, String> WEEKDAY_NAMES = Map.of(
            "MON", "周一",
            "TUE", "周二",
            "WED", "周三",
            "THU", "周四",
            "FRI", "周五",
            "SAT", "周六",
            "SUN", "周日"
    );

    private final JobScheduleDomainService jobScheduleDomainService;

    public JobCronPreviewVO preview(JobCronPreviewRequest request, Clock clock) {
        String expression = normalize(request == null ? null : request.getCronExpression());
        if (expression == null) {
            throw new BusinessException("Cron表达式不能为空");
        }

        CronExpression cronExpression = parse(expression);
        String timezone = StringUtils.defaultIfBlank(
                StringUtils.trim(request == null ? null : request.getTimezone()),
                JobScheduleType.DEFAULT_TIMEZONE);
        ZoneId zoneId = jobScheduleDomainService.requireZoneId(timezone);
        cronExpression.setTimeZone(TimeZone.getTimeZone(zoneId));
        Date cursor = Date.from((clock == null ? Clock.systemUTC() : clock).instant());
        List<LocalDateTime> nextFireTimes = new ArrayList<>(PREVIEW_COUNT);
        for (int index = 0; index < PREVIEW_COUNT; index++) {
            Date next = cronExpression.getNextValidTimeAfter(cursor);
            if (next == null) {
                break;
            }
            nextFireTimes.add(LocalDateTime.ofInstant(next.toInstant(), zoneId));
            cursor = next;
        }

        if (nextFireTimes.isEmpty()) {
            throw new BusinessException("该执行计划没有未来触发时间，请调整Cron表达式");
        }
        return JobCronPreviewVO.builder()
                .cronExpression(expression)
                .timezone(zoneId.getId())
                .description(describe(expression))
                .nextFireTimes(nextFireTimes)
                .build();
    }

    public String describe(String cronExpression) {
        String expression = normalize(cronExpression);
        if (expression == null) {
            return "尚未配置执行计划";
        }
        String[] fields = expression.toUpperCase(Locale.ROOT).split(" ");
        if (fields.length != 6 || !"0".equals(fields[0])) {
            return "自定义执行计划";
        }

        String minute = fields[1];
        String hour = fields[2];
        String dayOfMonth = fields[3];
        String month = fields[4];
        String dayOfWeek = fields[5];
        if ("*".equals(hour) && "*".equals(dayOfMonth) && "*".equals(month) && "?".equals(dayOfWeek)
                && minute.matches("(?:0|\\*)/[1-9]\\d*")) {
            return "每 " + minute.substring(minute.indexOf('/') + 1) + " 分钟执行";
        }
        if (minute.matches("\\d{1,2}") && "*".equals(hour) && "*".equals(dayOfMonth)
                && "*".equals(month) && "?".equals(dayOfWeek)) {
            return "每小时第 " + Integer.parseInt(minute) + " 分钟执行";
        }
        if (isTime(minute, hour) && "*".equals(dayOfMonth) && "*".equals(month) && "?".equals(dayOfWeek)) {
            return "每天 " + formatTime(hour, minute) + " 执行";
        }
        if (isTime(minute, hour) && "?".equals(dayOfMonth) && "*".equals(month)
                && WEEKDAY_NAMES.containsKey(dayOfWeek)) {
            return "每" + WEEKDAY_NAMES.get(dayOfWeek) + " " + formatTime(hour, minute) + " 执行";
        }
        if (isTime(minute, hour) && dayOfMonth.matches("(?:[1-9]|[12]\\d|3[01])")
                && "*".equals(month) && "?".equals(dayOfWeek)) {
            return "每月 " + Integer.parseInt(dayOfMonth) + " 日 " + formatTime(hour, minute) + " 执行";
        }
        return "自定义执行计划";
    }

    private CronExpression parse(String expression) {
        try {
            return new CronExpression(expression);
        } catch (ParseException | IllegalArgumentException exception) {
            throw new BusinessException("Cron表达式格式不正确，请检查后重试");
        }
    }

    private String normalize(String expression) {
        String trimmed = StringUtils.trimToNull(expression);
        return trimmed == null ? null : trimmed.replaceAll("\\s+", " ");
    }

    private boolean isTime(String minute, String hour) {
        if (!minute.matches("\\d{1,2}") || !hour.matches("\\d{1,2}")) {
            return false;
        }
        return Integer.parseInt(minute) <= 59 && Integer.parseInt(hour) <= 23;
    }

    private String formatTime(String hour, String minute) {
        return String.format("%02d:%02d", Integer.parseInt(hour), Integer.parseInt(minute));
    }
}

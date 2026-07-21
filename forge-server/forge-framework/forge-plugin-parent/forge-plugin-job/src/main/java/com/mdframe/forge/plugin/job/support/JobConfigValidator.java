package com.mdframe.forge.plugin.job.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.job.constant.JobInvokeMode;
import com.mdframe.forge.plugin.job.dto.JobConfigSaveRequest;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.plugin.job.executor.IJobExecutor;
import com.mdframe.forge.plugin.job.constant.JobConcurrentPolicy;
import com.mdframe.forge.plugin.job.constant.JobMisfirePolicy;
import com.mdframe.forge.plugin.job.constant.JobScheduleType;
import com.mdframe.forge.plugin.job.service.JobExecutorCatalogService;
import com.mdframe.forge.plugin.job.service.JobScheduleDomainService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 定时任务配置权威校验器。
 */
@Component
@RequiredArgsConstructor
public class JobConfigValidator {

    private static final Set<String> EXECUTE_MODES = Set.of("BEAN", "HANDLER", "RPC");
    private static final int MAX_RETRY_COUNT = 5;
    private static final Set<String> ALARM_CHANNELS = Set.of("WEB", "EMAIL");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");

    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    private final JobExecutorCatalogService executorCatalogService;
    private final JobScheduleDomainService jobScheduleDomainService;

    public void validateCreate(JobConfigSaveRequest request) {
        validateCommon(request, Clock.systemUTC());
    }

    public void validateUpdate(JobConfigSaveRequest request, SysJobConfig current) {
        if (request == null || request.getId() == null) {
            throw new BusinessException("任务ID不能为空");
        }
        if (current == null) {
            throw new BusinessException("定时任务不存在");
        }
        validateCommon(request, Clock.systemUTC());
        if (!Objects.equals(current.getJobName(), request.getJobName())
                || !Objects.equals(current.getJobGroup(), request.getJobGroup())) {
            throw new BusinessException("任务标识创建后不可修改，请新建任务");
        }
        if (request.getVersion() == null) {
            throw new BusinessException("任务版本不能为空，请刷新后重试");
        }
    }

    private void validateCommon(JobConfigSaveRequest request, Clock clock) {
        if (request == null) {
            throw new BusinessException("任务配置不能为空");
        }
        normalize(request);
        requireText(request.getJobName(), "任务名称不能为空");
        requireLength(request.getJobName(), 200, "任务名称长度不能超过200个字符");
        requireLength(request.getJobGroup(), 200, "任务分组长度不能超过200个字符");
        requireLength(request.getDescription(), 500, "任务描述长度不能超过500个字符");
        validateSchedule(request, clock);
        if (!JobInvokeMode.isSupported(request.getInvokeMode())) {
            throw new BusinessException("调用方式仅支持 SINGLE 或 FLOW");
        }
        if (request.getStatus() == null || (request.getStatus() != 0 && request.getStatus() != 1)) {
            throw new BusinessException("任务状态仅支持0或1");
        }
        validateExecutionPolicy(request);
        validateAlarm(request);
        validateJsonParameter(request.getJobParam(), JobInvokeMode.FLOW.equals(request.getInvokeMode()));
        if (JobInvokeMode.FLOW.equals(request.getInvokeMode())) {
            validateFlowBinding(request);
        } else {
            if (!EXECUTE_MODES.contains(request.getExecuteMode())) {
                throw new BusinessException("执行模式仅支持 BEAN、HANDLER 或 RPC");
            }
            validateExecutionTarget(request);
        }
    }

    private void normalize(JobConfigSaveRequest request) {
        request.setJobName(StringUtils.trimToNull(request.getJobName()));
        request.setJobGroup(StringUtils.defaultIfBlank(StringUtils.trim(request.getJobGroup()), "DEFAULT"));
        request.setDescription(StringUtils.trimToNull(request.getDescription()));
        request.setExecutorBean(StringUtils.trimToNull(request.getExecutorBean()));
        request.setExecutorMethod(StringUtils.trimToNull(request.getExecutorMethod()));
        request.setExecutorHandler(StringUtils.trimToNull(request.getExecutorHandler()));
        request.setExecutorService(StringUtils.trimToNull(request.getExecutorService()));
        request.setScheduleType(StringUtils.upperCase(StringUtils.defaultIfBlank(
                StringUtils.trim(request.getScheduleType()), JobScheduleType.CRON)));
        request.setCronExpression(StringUtils.trimToNull(request.getCronExpression()));
        request.setTimezone(StringUtils.defaultIfBlank(
                StringUtils.trim(request.getTimezone()), JobScheduleType.DEFAULT_TIMEZONE));
        request.setJobParam(StringUtils.trimToNull(request.getJobParam()));
        request.setExecuteMode(StringUtils.upperCase(StringUtils.trimToNull(request.getExecuteMode())));
        request.setInvokeMode(StringUtils.upperCase(StringUtils.defaultIfBlank(
                StringUtils.trim(request.getInvokeMode()), JobInvokeMode.SINGLE)));
        request.setFlowModelKey(StringUtils.trimToNull(request.getFlowModelKey()));
        request.setConcurrentPolicy(StringUtils.upperCase(StringUtils.defaultIfBlank(
                StringUtils.trim(request.getConcurrentPolicy()), JobConcurrentPolicy.DEFAULT)));
        request.setMisfirePolicy(StringUtils.upperCase(StringUtils.defaultIfBlank(
                StringUtils.trim(request.getMisfirePolicy()), JobMisfirePolicy.DEFAULT)));
        request.setAlarmChannels(normalizeCsv(request.getAlarmChannels(), true));
        request.setAlarmRecipientUserIds(normalizeUserIds(request.getAlarmRecipientUserIds()));
        request.setAlarmEmail(normalizeCsv(request.getAlarmEmail(), false));
        if (request.getStatus() == null) {
            request.setStatus(1);
        }
        if (request.getRetryCount() == null) {
            request.setRetryCount(0);
        }
        if (request.getIdempotentFlag() == null) {
            request.setIdempotentFlag(0);
        }
        if (request.getAlarmEnabled() == null) {
            request.setAlarmEnabled(0);
        }
        if (JobInvokeMode.FLOW.equals(request.getInvokeMode())) {
            request.setExecuteMode(null);
            request.setExecutorBean(null);
            request.setExecutorMethod(null);
            request.setExecutorHandler(null);
            request.setExecutorService(null);
        } else if (JobInvokeMode.SINGLE.equals(request.getInvokeMode())) {
            request.setFlowModelKey(null);
            request.setFlowModelVersion(null);
        }
    }

    private void validateAlarm(JobConfigSaveRequest request) {
        if (request.getAlarmEnabled() != 0 && request.getAlarmEnabled() != 1) {
            throw new BusinessException("告警开关仅支持0或1");
        }
        Set<String> channels = splitCsv(request.getAlarmChannels());
        if (!ALARM_CHANNELS.containsAll(channels)) {
            throw new BusinessException("告警渠道仅支持WEB或EMAIL");
        }
        if (request.getAlarmChannels() != null && request.getAlarmChannels().length() > 64) {
            throw new BusinessException("告警渠道配置长度不能超过64个字符");
        }
        if (request.getAlarmRecipientUserIds() != null
                && request.getAlarmRecipientUserIds().length() > 2000) {
            throw new BusinessException("站内信接收人配置长度不能超过2000个字符");
        }
        if (request.getAlarmEmail() != null && request.getAlarmEmail().length() > 500) {
            throw new BusinessException("告警邮箱配置长度不能超过500个字符");
        }
        for (String email : splitCsv(request.getAlarmEmail())) {
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new BusinessException("告警邮箱格式不正确");
            }
        }
        if (request.getAlarmEnabled() == 0) {
            return;
        }
        if (channels.isEmpty()) {
            throw new BusinessException("启用告警后至少选择一个通知渠道");
        }
        if (channels.contains("WEB") && request.getAlarmRecipientUserIds() == null) {
            throw new BusinessException("站内信告警必须选择平台用户");
        }
        if (channels.contains("EMAIL") && request.getAlarmEmail() == null) {
            throw new BusinessException("邮件告警必须填写邮箱地址");
        }
    }

    private String normalizeUserIds(String value) {
        Set<String> ids = splitCsv(value);
        for (String id : ids) {
            try {
                if (Long.parseLong(id) <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                throw new BusinessException("站内信接收用户ID格式不正确");
            }
        }
        return ids.isEmpty() ? null : String.join(",", ids);
    }

    private String normalizeCsv(String value, boolean uppercase) {
        Set<String> values = splitCsv(value).stream()
                .map(item -> uppercase ? StringUtils.upperCase(item) : item)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return values.isEmpty() ? null : String.join(",", values);
    }

    private Set<String> splitCsv(String value) {
        Set<String> values = new LinkedHashSet<>();
        if (StringUtils.isBlank(value)) {
            return values;
        }
        for (String item : value.split(",")) {
            String normalized = StringUtils.trimToNull(item);
            if (normalized != null) {
                values.add(normalized);
            }
        }
        return values;
    }

    private void validateExecutionPolicy(JobConfigSaveRequest request) {
        if (!JobConcurrentPolicy.isSupported(request.getConcurrentPolicy())) {
            throw new BusinessException("并发策略仅支持 ALLOW 或 SKIP_IF_RUNNING");
        }
        if (!JobMisfirePolicy.isSupported(request.getMisfirePolicy())) {
            throw new BusinessException("Misfire策略仅支持 FIRE_ONCE_NOW 或 DO_NOTHING");
        }
        if (request.getIdempotentFlag() != 0 && request.getIdempotentFlag() != 1) {
            throw new BusinessException("幂等标识仅支持0或1");
        }
        if (request.getRetryCount() < 0 || request.getRetryCount() > MAX_RETRY_COUNT) {
            throw new BusinessException("失败重试次数必须在0到5之间");
        }
        if (request.getRetryCount() > 0 && request.getIdempotentFlag() != 1) {
            throw new BusinessException("只有明确声明幂等安全的任务才能开启自动重试");
        }
    }

    private void validateSchedule(JobConfigSaveRequest request, Clock clock) {
        requireLength(request.getTimezone(), 64, "时区长度不能超过64个字符");
        ZoneId zoneId = jobScheduleDomainService.requireZoneId(request.getTimezone());
        if (JobScheduleType.CRON.equals(request.getScheduleType())) {
            requireText(request.getCronExpression(), "Cron表达式不能为空");
            requireLength(request.getCronExpression(), 100, "Cron表达式长度不能超过100个字符");
            if (!CronExpression.isValidExpression(request.getCronExpression())) {
                throw new BusinessException("Cron表达式格式不正确");
            }
            if (request.getFireOnceTime() != null) {
                throw new BusinessException("周期任务不能填写一次性执行时间");
            }
            return;
        }
        if (!JobScheduleType.ONCE.equals(request.getScheduleType())) {
            throw new BusinessException("调度类型仅支持CRON或ONCE");
        }
        if (request.getCronExpression() != null) {
            throw new BusinessException("一次性任务不能填写Cron表达式");
        }
        Instant fireInstant = jobScheduleDomainService.resolveOnceInstant(request.getFireOnceTime(), zoneId);
        if (!fireInstant.isAfter(clock.instant())) {
            throw new BusinessException("一次性执行时间必须晚于当前时间");
        }
    }

    private void validateJsonParameter(String jobParam, boolean requireObject) {
        if (jobParam == null) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(jobParam);
            if (requireObject && (root == null || !root.isObject())) {
                throw new BusinessException("FLOW任务参数必须是JSON对象");
            }
        } catch (JsonProcessingException exception) {
            throw new BusinessException("任务参数必须是合法JSON");
        }
    }

    private void validateFlowBinding(JobConfigSaveRequest request) {
        requireText(request.getFlowModelKey(), "流程模型Key不能为空");
        requireLength(request.getFlowModelKey(), 100, "流程模型Key长度不能超过100个字符");
        if (request.getFlowModelVersion() == null || request.getFlowModelVersion() <= 0) {
            throw new BusinessException("流程模型版本必须大于0");
        }
    }

    private void validateExecutionTarget(JobConfigSaveRequest request) {
        switch (request.getExecuteMode()) {
            case "BEAN" -> validateBeanTarget(request);
            case "HANDLER" -> validateHandlerTarget(request);
            case "RPC" -> validateRpcTarget(request);
            default -> throw new BusinessException("不支持的执行模式");
        }
    }

    private void validateBeanTarget(JobConfigSaveRequest request) {
        requireText(request.getExecutorBean(), "Bean模式必须选择执行Bean");
        requireText(request.getExecutorMethod(), "Bean模式必须填写执行方法");
        requireLength(request.getExecutorBean(), 200, "执行Bean名称长度不能超过200个字符");
        requireLength(request.getExecutorMethod(), 200, "执行方法名称长度不能超过200个字符");
        if (!applicationContext.containsBean(request.getExecutorBean())) {
            throw new BusinessException("执行Bean不存在: " + request.getExecutorBean());
        }
        Object bean = applicationContext.getBean(request.getExecutorBean());
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        boolean methodExists = false;
        for (Method method : targetClass.getMethods()) {
            if (isSupportedMethod(method, request.getExecutorMethod())) {
                methodExists = true;
                break;
            }
        }
        if (!methodExists) {
            for (Method method : targetClass.getDeclaredMethods()) {
                if (isSupportedMethod(method, request.getExecutorMethod())) {
                    methodExists = true;
                    break;
                }
            }
        }
        if (!methodExists) {
            throw new BusinessException("执行方法不存在或参数签名不支持: " + request.getExecutorMethod());
        }
    }

    private boolean isSupportedMethod(Method method, String methodName) {
        if (!method.getName().equals(methodName)) {
            return false;
        }
        return method.getParameterCount() == 0
                || (method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class);
    }

    private void validateHandlerTarget(JobConfigSaveRequest request) {
        requireText(request.getExecutorHandler(), "Handler模式必须选择执行Handler");
        requireLength(request.getExecutorHandler(), 200, "执行Handler名称长度不能超过200个字符");
        String beanName = executorCatalogService.resolveHandlerBeanName(request.getExecutorHandler());
        if (!applicationContext.containsBean(beanName)) {
            throw new BusinessException("执行Handler不存在: " + request.getExecutorHandler());
        }
        try {
            applicationContext.getBean(beanName, IJobExecutor.class);
        } catch (RuntimeException exception) {
            throw new BusinessException("执行Handler未实现IJobExecutor: " + request.getExecutorHandler());
        }
    }

    private void validateRpcTarget(JobConfigSaveRequest request) {
        requireText(request.getExecutorService(), "RPC模式必须填写执行服务");
        requireText(request.getExecutorHandler(), "RPC模式必须填写执行Handler");
        requireLength(request.getExecutorService(), 200, "执行服务名称长度不能超过200个字符");
        requireLength(request.getExecutorHandler(), 200, "执行Handler名称长度不能超过200个字符");
    }

    private void requireText(String value, String message) {
        if (StringUtils.isBlank(value)) {
            throw new BusinessException(message);
        }
    }

    private void requireLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(message);
        }
    }
}

package com.mdframe.forge.plugin.job.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.job.dto.JobConfigSaveRequest;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.plugin.job.executor.IJobExecutor;
import com.mdframe.forge.plugin.job.service.JobExecutorCatalogService;
import com.mdframe.forge.plugin.job.service.JobScheduleDomainService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class JobConfigValidatorTest {

    private final SampleJobExecutor sampleJobExecutor = new SampleJobExecutor();
    private GenericApplicationContext applicationContext;
    private JobConfigValidator validator;

    @BeforeEach
    void setUp() {
        applicationContext = new GenericApplicationContext();
        applicationContext.getBeanFactory().registerSingleton("sampleJobExecutor", sampleJobExecutor);
        applicationContext.getBeanFactory().registerSingleton("cleanupHandler", new CleanupHandler());
        applicationContext.refresh();
        validator = new JobConfigValidator(new ObjectMapper(), applicationContext,
                new JobExecutorCatalogService(), new JobScheduleDomainService());
    }

    @AfterEach
    void tearDown() {
        applicationContext.close();
    }

    @Test
    void shouldAcceptValidBeanJob() {
        JobConfigSaveRequest request = validBeanRequest();

        assertDoesNotThrow(() -> validator.validateCreate(request));

        assertEquals("SINGLE", request.getInvokeMode());
    }

    @Test
    void shouldAcceptFlowBindingAndClearSingleExecutorTarget() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setInvokeMode("flow");
        request.setFlowModelKey(" daily-settlement ");
        request.setFlowModelVersion(7);

        assertDoesNotThrow(() -> validator.validateCreate(request));

        assertEquals("FLOW", request.getInvokeMode());
        assertEquals("daily-settlement", request.getFlowModelKey());
        assertNull(request.getExecuteMode());
        assertNull(request.getExecutorBean());
        assertNull(request.getExecutorMethod());
    }

    @Test
    void shouldRejectInvalidFlowBindingOrNonObjectInput() {
        JobConfigSaveRequest missingBinding = validBeanRequest();
        missingBinding.setInvokeMode("FLOW");
        missingBinding.setFlowModelKey(null);
        missingBinding.setFlowModelVersion(null);
        assertTrue(assertThrows(BusinessException.class,
                () -> validator.validateCreate(missingBinding)).getMessage().contains("流程模型"));

        JobConfigSaveRequest arrayInput = validBeanRequest();
        arrayInput.setInvokeMode("FLOW");
        arrayInput.setFlowModelKey("daily-settlement");
        arrayInput.setFlowModelVersion(7);
        arrayInput.setJobParam("[1,2]");
        assertTrue(assertThrows(BusinessException.class,
                () -> validator.validateCreate(arrayInput)).getMessage().contains("JSON对象"));
    }

    @Test
    void shouldRejectUnsupportedInvokeMode() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setInvokeMode("SCRIPT");

        assertTrue(assertThrows(BusinessException.class,
                () -> validator.validateCreate(request)).getMessage().contains("调用方式"));
    }

    @Test
    void shouldRejectInvalidCronExpression() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setCronExpression("not-a-cron");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validateCreate(request));

        assertTrue(exception.getMessage().contains("Cron"));
    }

    @Test
    void shouldRejectCronWithOnceTime() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setFireOnceTime(LocalDateTime.of(2099, 7, 19, 10, 0));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validateCreate(request));

        assertTrue(exception.getMessage().contains("周期任务不能填写一次性执行时间"));
    }

    @Test
    void shouldAcceptFutureOnceAndClearCronRequirement() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setScheduleType("ONCE");
        request.setCronExpression(null);
        request.setFireOnceTime(LocalDateTime.of(2099, 7, 19, 10, 0));

        assertDoesNotThrow(() -> validator.validateCreate(request));
    }

    @Test
    void shouldRejectOnceWithCronOrPastTime() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setScheduleType("ONCE");
        request.setFireOnceTime(LocalDateTime.of(2099, 7, 19, 10, 0));

        BusinessException mutualException = assertThrows(BusinessException.class,
                () -> validator.validateCreate(request));
        assertTrue(mutualException.getMessage().contains("一次性任务不能填写Cron表达式"));

        request.setCronExpression(null);
        request.setFireOnceTime(LocalDateTime.of(2000, 1, 1, 0, 0));
        BusinessException pastException = assertThrows(BusinessException.class,
                () -> validator.validateCreate(request));
        assertTrue(pastException.getMessage().contains("必须晚于当前时间"));
    }

    @Test
    void shouldRejectInvalidTimezone() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setTimezone("Mars/Olympus_Mons");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validateCreate(request));

        assertTrue(exception.getMessage().contains("时区"));
    }

    @Test
    void shouldRejectInvalidJsonParameter() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setJobParam("plain-text");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validateCreate(request));

        assertTrue(exception.getMessage().contains("JSON"));
    }

    @Test
    void shouldRejectMissingExecutionTarget() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setExecutorBean("missingBean");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validateCreate(request));

        assertTrue(exception.getMessage().contains("Bean"));
    }

    @Test
    void shouldValidateHandlerAndRpcTargets() {
        JobConfigSaveRequest handlerRequest = validBeanRequest();
        handlerRequest.setExecuteMode("HANDLER");
        handlerRequest.setExecutorBean(null);
        handlerRequest.setExecutorMethod(null);
        handlerRequest.setExecutorHandler("cleanupHandler");
        assertDoesNotThrow(() -> validator.validateCreate(handlerRequest));

        JobConfigSaveRequest rpcRequest = validBeanRequest();
        rpcRequest.setExecuteMode("RPC");
        rpcRequest.setExecutorBean(null);
        rpcRequest.setExecutorMethod(null);
        rpcRequest.setExecutorHandler("remoteCleanupHandler");
        rpcRequest.setExecutorService("business-service");
        assertDoesNotThrow(() -> validator.validateCreate(rpcRequest));
    }

    @Test
    void shouldRejectJobKeyChangesOnUpdate() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setId(10L);
        request.setVersion(2);
        request.setJobName("renamedJob");

        SysJobConfig current = new SysJobConfig();
        current.setId(10L);
        current.setJobName("sampleJob");
        current.setJobGroup("DEFAULT");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validateUpdate(request, current));

        assertTrue(exception.getMessage().contains("任务标识"));
    }

    @Test
    void shouldNormalizeExecutionPolicyDefaults() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setConcurrentPolicy(null);
        request.setMisfirePolicy(null);
        request.setIdempotentFlag(null);

        assertDoesNotThrow(() -> validator.validateCreate(request));

        assertTrue("ALLOW".equals(request.getConcurrentPolicy()));
        assertTrue("DO_NOTHING".equals(request.getMisfirePolicy()));
        assertTrue(Integer.valueOf(0).equals(request.getIdempotentFlag()));
    }

    @Test
    void shouldRejectRetryForNonIdempotentTask() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setRetryCount(2);
        request.setIdempotentFlag(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validateCreate(request));

        assertTrue(exception.getMessage().contains("幂等"));
    }

    @Test
    void shouldAcceptBoundedRetryForExplicitlyIdempotentTask() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setRetryCount(2);
        request.setIdempotentFlag(1);

        assertDoesNotThrow(() -> validator.validateCreate(request));
    }

    @Test
    void shouldRejectUnsupportedPoliciesAndExcessiveRetries() {
        JobConfigSaveRequest concurrentRequest = validBeanRequest();
        concurrentRequest.setConcurrentPolicy("QUEUE");
        assertTrue(assertThrows(BusinessException.class,
                () -> validator.validateCreate(concurrentRequest)).getMessage().contains("并发策略"));

        JobConfigSaveRequest misfireRequest = validBeanRequest();
        misfireRequest.setMisfirePolicy("IGNORE");
        assertTrue(assertThrows(BusinessException.class,
                () -> validator.validateCreate(misfireRequest)).getMessage().contains("Misfire"));

        JobConfigSaveRequest retryRequest = validBeanRequest();
        retryRequest.setRetryCount(6);
        retryRequest.setIdempotentFlag(1);
        assertTrue(assertThrows(BusinessException.class,
                () -> validator.validateCreate(retryRequest)).getMessage().contains("5"));
    }

    @Test
    void shouldNormalizeAndValidateAlarmConfiguration() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setAlarmEnabled(1);
        request.setAlarmChannels(" web,EMAIL,WEB ");
        request.setAlarmRecipientUserIds(" 12,13,12 ");
        request.setAlarmEmail("ops@example.com, owner@example.com");

        assertDoesNotThrow(() -> validator.validateCreate(request));

        assertTrue("WEB,EMAIL".equals(request.getAlarmChannels()));
        assertTrue("12,13".equals(request.getAlarmRecipientUserIds()));
    }

    @Test
    void shouldRejectEnabledAlarmWithoutRecipientsOrWithUnsupportedChannel() {
        JobConfigSaveRequest request = validBeanRequest();
        request.setAlarmEnabled(1);
        request.setAlarmChannels("WEB");
        assertTrue(assertThrows(BusinessException.class,
                () -> validator.validateCreate(request)).getMessage().contains("平台用户"));

        JobConfigSaveRequest webhookRequest = validBeanRequest();
        webhookRequest.setAlarmEnabled(1);
        webhookRequest.setAlarmChannels("WEBHOOK");
        assertTrue(assertThrows(BusinessException.class,
                () -> validator.validateCreate(webhookRequest)).getMessage().contains("WEB或EMAIL"));

        JobConfigSaveRequest emailRequest = validBeanRequest();
        emailRequest.setAlarmEnabled(1);
        emailRequest.setAlarmChannels("EMAIL");
        emailRequest.setAlarmEmail("invalid-email");
        assertTrue(assertThrows(BusinessException.class,
                () -> validator.validateCreate(emailRequest)).getMessage().contains("邮箱格式"));
    }

    private JobConfigSaveRequest validBeanRequest() {
        JobConfigSaveRequest request = new JobConfigSaveRequest();
        request.setJobName("sampleJob");
        request.setJobGroup("DEFAULT");
        request.setDescription("示例任务");
        request.setExecutorBean("sampleJobExecutor");
        request.setExecutorMethod("execute");
        request.setScheduleType("CRON");
        request.setCronExpression("0 0/5 * * * ?");
        request.setTimezone("Asia/Shanghai");
        request.setJobParam("{\"limit\":10}");
        request.setStatus(1);
        request.setExecuteMode("BEAN");
        request.setRetryCount(0);
        return request;
    }

    static class SampleJobExecutor {
        public String execute(String param) {
            return param;
        }
    }

    static class CleanupHandler implements IJobExecutor {
        @Override
        public String execute(String param) {
            return "SUCCESS";
        }
    }
}

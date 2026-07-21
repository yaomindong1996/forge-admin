package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.vo.JobExecutorCatalogVO;
import com.mdframe.forge.starter.job.annotation.JobHandler;
import com.mdframe.forge.starter.job.annotation.ScheduledJob;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JobExecutorCatalogServiceTest {

    @Test
    void shouldBuildCatalogFromExplicitAnnotations() throws Exception {
        JobExecutorCatalogService service = new JobExecutorCatalogService();
        JobHandler handler = SampleHandler.class.getAnnotation(JobHandler.class);
        Method method = ScheduledSamples.class.getDeclaredMethod("closeInventory");
        ScheduledJob scheduledJob = method.getAnnotation(ScheduledJob.class);

        service.registerHandler("sampleHandlerBean", null, handler);
        service.registerScheduledJob("scheduledSamples", method.getName(), scheduledJob);

        List<JobExecutorCatalogVO> result = service.listExecutors();
        assertEquals(2, result.size());
        assertEquals("sampleHandlerBean", service.resolveHandlerBeanName("inventoryCloseHandler"));
        JobExecutorCatalogVO scheduled = service.findByTarget("BEAN", null,
                "scheduledSamples", "closeInventory");
        assertEquals("库存日结", scheduled.getDisplayName());
    }

    @Test
    void shouldKeepHistoricalHandlerCodeAsFallback() {
        JobExecutorCatalogService service = new JobExecutorCatalogService();

        assertEquals("legacyHandler", service.resolveHandlerBeanName("legacyHandler"));
        assertNull(service.find("HANDLER", "legacyHandler"));
    }

    @JobHandler(value = "inventoryCloseHandler", description = "库存日结，汇总每日库存流水", group = "INVENTORY")
    private static class SampleHandler {
    }

    private static class ScheduledSamples {

        @ScheduledJob(name = "inventoryClose", cron = "0 0 2 * * ?", description = "库存日结")
        private void closeInventory() {
        }
    }
}

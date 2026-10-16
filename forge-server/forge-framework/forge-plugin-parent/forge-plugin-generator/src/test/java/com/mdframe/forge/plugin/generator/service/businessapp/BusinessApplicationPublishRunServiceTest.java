package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationPublishStatus;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationPublishStep;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationPublishRun;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationPublishRunMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationVersionMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationAssetSelectionVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("BusinessApplicationPublishRunService")
class BusinessApplicationPublishRunServiceTest {

    @Test
    @DisplayName("page menu synchronization is a named side-effect step")
    void pageMenusIsNamedSideEffectStep() throws Exception {
        AtomicReference<AiBusinessApplicationPublishRun> inserted = new AtomicReference<>();
        AtomicReference<String> progressStatus = new AtomicReference<>();
        BusinessApplicationPublishRunMapper runMapper = proxy(
                BusinessApplicationPublishRunMapper.class, (method, args) -> switch (method) {
                    case "lockApplication" -> 10L;
                    case "selectMaxTargetVersionNo" -> 0;
                    case "insert" -> {
                        inserted.set((AiBusinessApplicationPublishRun) args[0]);
                        yield 1;
                    }
                    case "updateProgress" -> {
                        progressStatus.set(String.valueOf(args[3]));
                        yield 1;
                    }
                    default -> null;
                });
        BusinessApplicationVersionMapper versionMapper = proxy(
                BusinessApplicationVersionMapper.class,
                (method, args) -> "selectMaxVersionNo".equals(method) ? 0 : null);
        BusinessApplicationPublishRunService service = new BusinessApplicationPublishRunService(
                new ObjectMapper().findAndRegisterModules(), versionMapper);
        injectMapper(service, runMapper);

        AiBusinessApplicationPublishRun run = service.reserve(
                10L,
                "publish-run-test-0001",
                "PUBLISH",
                null,
                new BusinessApplicationSnapshotService.SnapshotBundle("{}", "hash", Map.of()),
                new BusinessApplicationAssetSelectionVO());

        assertNotNull(inserted.get());
        assertEquals("同步应用页面菜单", service.toVO(run).getSteps().stream()
                .filter(step -> BusinessApplicationPublishStep.PAGE_MENUS.equals(step.getStepCode()))
                .findFirst().orElseThrow().getStepName());

        run.setId(20L);
        service.markFailed(run, BusinessApplicationPublishStep.PAGE_MENUS,
                "PAGE_MENU_FAILED", "页面菜单同步失败");

        assertEquals(BusinessApplicationPublishStatus.PARTIAL.getCode(), progressStatus.get());
        assertEquals(BusinessApplicationPublishStatus.PARTIAL.getCode(), run.getRunStatus());
    }

    private static void injectMapper(BusinessApplicationPublishRunService service,
                                     BusinessApplicationPublishRunMapper mapper) throws Exception {
        Field field = org.springframework.util.ReflectionUtils.findField(ServiceImpl.class, "baseMapper");
        field.setAccessible(true);
        field.set(service, mapper);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type},
                (proxy, method, args) -> invocation.invoke(method.getName(), args == null ? new Object[0] : args));
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] args) throws Throwable;
    }
}

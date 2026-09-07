package com.mdframe.forge.plugin.system.controller;

import com.mdframe.forge.plugin.system.service.ISysDataScopeConfigService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.controller.DataScopeController;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysDataScopeConfigControllerTest {
    private final ISysDataScopeConfigService configService = mock(ISysDataScopeConfigService.class);
    private final IDataScopeService cacheService = mock(IDataScopeService.class);
    private final SysDataScopeConfigController controller = new SysDataScopeConfigController(configService, cacheService);

    @Test
    void shouldRequireAdminForBothCacheRefreshEntrypointsAndStatusChanges() {
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(() -> SessionHelper.assertAdmin(anyString()))
                    .thenThrow(new BusinessException("只有超级管理员可以维护数据权限配置"));
            assertThrows(BusinessException.class, controller::refreshCache);
            assertThrows(BusinessException.class, () -> controller.updateStatus(null));
            assertThrows(BusinessException.class, () -> new DataScopeController(cacheService).refreshCache());
            verifyNoInteractions(configService, cacheService);
        }
    }

    @Test
    void shouldValidateStatusRequestAndAcceptTypedPayload() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        try (var session = mockStatic(SessionHelper.class)) {
            mvc.perform(post("/system/dataScopeConfig/status").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"id\":7,\"enabled\":2,\"expectedEnabled\":1}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(post("/system/dataScopeConfig/status").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"id\":7,\"enabled\":0}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(configService);
            mvc.perform(post("/system/dataScopeConfig/status").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"id\":7,\"enabled\":0,\"expectedEnabled\":1}"))
                    .andExpect(status().isOk());
            verify(configService).updateConfigStatus(any());
        }
    }
}

package com.mdframe.forge.plugin.generator.controller;

import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.service.IGenDatasourceService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GenDatasourceController enabled list")
class GenDatasourceControllerTest {

    @Test
    @DisplayName("enabled list hides connection secrets but keeps selector fields")
    void enabledListHidesConnectionSecrets() {
        GenDatasource source = new GenDatasource();
        source.setDatasourceId(183L);
        source.setDatasourceName("应用数据源");
        source.setDbType("MySQL");
        source.setUrl("jdbc:mysql://10.0.0.1:3306/demo");
        source.setUsername("root");
        source.setPassword("cipher");
        source.setIsDefault(1);
        source.setAllowRuntimeWrite(1);
        source.setAllowRuntimeDdl(0);

        IGenDatasourceService service = mock(IGenDatasourceService.class);
        when(service.selectEnabledDatasources("LOWCODE_RUNTIME")).thenReturn(List.of(source));

        RespInfo<List<GenDatasource>> response = new GenDatasourceController(service).enabledList("LOWCODE_RUNTIME");
        GenDatasource safe = response.getData().get(0);

        assertNull(safe.getUrl());
        assertNull(safe.getUsername());
        assertNull(safe.getPassword());
        assertEquals(183L, safe.getDatasourceId());
        assertEquals("应用数据源", safe.getDatasourceName());
        assertEquals("MySQL", safe.getDbType());
        assertEquals(1, safe.getIsDefault());
        assertEquals(1, safe.getAllowRuntimeWrite());
        assertEquals(0, safe.getAllowRuntimeDdl());
        assertEquals("cipher", source.getPassword(), "原实例供服务端建表使用，不能被改写");
    }

    @Test
    @DisplayName("enabled list tolerates empty service result")
    void enabledListToleratesNull() {
        IGenDatasourceService service = mock(IGenDatasourceService.class);
        when(service.selectEnabledDatasources(null)).thenReturn(null);

        RespInfo<List<GenDatasource>> response = new GenDatasourceController(service).enabledList(null);

        assertEquals(0, response.getData().size());
    }
}

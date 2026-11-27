package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.plugin.generator.service.lowcode.runtime.MySqlRuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialectFactory;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeJdbcTemplateProvider;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicCrudTableMetadataGatewayTest {

    @Test
    void cachesColumnsAndMappingUntilTableMetadataIsCleared() {
        RuntimeJdbcTemplateProvider provider = mock(RuntimeJdbcTemplateProvider.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(provider.jdbcTemplate(any())).thenReturn(jdbc);
        when(jdbc.queryForList(anyString(), eq(String.class), any()))
            .thenReturn(List.of("ID", "order_code"));
        DynamicCrudTableMetadataGateway gateway = gateway(provider);

        assertEquals(Set.of("id", "order_code"), gateway.columns("biz_order"));
        assertEquals("order_code", gateway.columnMapping("biz_order",
            () -> gateway.columns("biz_order")).get("orderCode"));
        gateway.columns("biz_order");
        verify(jdbc).queryForList(anyString(), eq(String.class), any());

        gateway.clear("biz_order");
        gateway.columns("biz_order");
        verify(jdbc, org.mockito.Mockito.times(2))
            .queryForList(anyString(), eq(String.class), any());
    }

    @Test
    void invalidatesLogicDeleteDecisionTogetherWithTableMetadata() {
        DynamicCrudTableMetadataGateway gateway = gateway(mock(RuntimeJdbcTemplateProvider.class));
        AtomicInteger lookups = new AtomicInteger();

        assertTrue(gateway.hasLogicDeleteColumn("biz_order", "del_flag", true, () -> {
            lookups.incrementAndGet();
            return Set.of("id", "del_flag");
        }));
        assertTrue(gateway.hasLogicDeleteColumn("biz_order", "del_flag", true, () -> {
            lookups.incrementAndGet();
            return Set.of();
        }));
        assertEquals(1, lookups.get());

        gateway.clear("biz_order");
        assertTrue(gateway.hasLogicDeleteColumn("biz_order", "del_flag", true, () -> {
            lookups.incrementAndGet();
            return Set.of("del_flag");
        }));
        assertEquals(2, lookups.get());
    }

    @Test
    void elevatesDigitPrimaryKeyAndKeepsLogicFlagWhenColumnTypesUnavailable() {
        RuntimeJdbcTemplateProvider provider = mock(RuntimeJdbcTemplateProvider.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(provider.jdbcTemplate(any())).thenReturn(jdbc);
        when(jdbc.execute(any(ConnectionCallback.class)))
            .thenThrow(new RuntimeException("metadata unavailable"));
        DynamicCrudTableMetadataGateway gateway = gateway(provider);

        assertEquals(1017L, gateway.coercePrimaryKeyValue("biz_order", "id", "1017"));
        assertEquals("ABC", gateway.coercePrimaryKeyValue("biz_order", "id", "ABC"));
        assertEquals("0", gateway.coerceLogicFlagLiteral("biz_order", "del_flag", "0"));
        assertEquals("0", gateway.normalizeQueryValue("biz_order", "del_flag", "0"));
    }

    private DynamicCrudTableMetadataGateway gateway(RuntimeJdbcTemplateProvider provider) {
        return new DynamicCrudTableMetadataGateway(provider,
            new RuntimeDatabaseDialectFactory(List.of(new MySqlRuntimeDatabaseDialect())));
    }
}

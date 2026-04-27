package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicCrudRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @Test
    void insertShouldAutoFillAuditFieldsWithoutOverridingExplicitValues() {
        DynamicCrudRepository repository = spy(new DynamicCrudRepository(namedJdbcTemplate));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "demo");
        data.put("create_by", 999L);

        doReturn(true).when(repository).tableExists("demo_table");
        doReturn(Set.of("name", "tenant_id", "create_by", "create_dept", "create_time", "update_by", "update_time"))
                .when(repository).getTableColumns("demo_table");
        when(namedJdbcTemplate.update(eq("INSERT INTO demo_table (name, create_by, tenant_id, create_dept, create_time, update_by, update_time) VALUES (:name, :create_by, :tenant_id, :create_dept, :create_time, :update_by, :update_time)"), any(MapSqlParameterSource.class)))
                .thenReturn(1);

        try (MockedStatic<TenantContextHolder> tenantContext = org.mockito.Mockito.mockStatic(TenantContextHolder.class);
             MockedStatic<SessionHelper> sessionHelper = org.mockito.Mockito.mockStatic(SessionHelper.class)) {
            tenantContext.when(TenantContextHolder::getTenantId).thenReturn(7L);
            sessionHelper.when(SessionHelper::getUserId).thenReturn(123L);
            sessionHelper.when(SessionHelper::getMainOrgId).thenReturn(88L);

            repository.insert("demo_table", data);
        }

        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(namedJdbcTemplate).update(eq("INSERT INTO demo_table (name, create_by, tenant_id, create_dept, create_time, update_by, update_time) VALUES (:name, :create_by, :tenant_id, :create_dept, :create_time, :update_by, :update_time)"), paramsCaptor.capture());

        MapSqlParameterSource params = paramsCaptor.getValue();
        assertEquals("demo", params.getValue("name"));
        assertEquals(999L, params.getValue("create_by"));
        assertEquals(7L, params.getValue("tenant_id"));
        assertEquals(88L, params.getValue("create_dept"));
        assertEquals(123L, params.getValue("update_by"));
        assertNotNull(params.getValue("create_time"));
        assertNotNull(params.getValue("update_time"));
    }

    @Test
    void updateByIdShouldStripImmutableFieldsAndPreserveExplicitAuditValues() {
        DynamicCrudRepository repository = spy(new DynamicCrudRepository(namedJdbcTemplate));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", 101L);
        data.put("tenant_id", 1L);
        data.put("name", "updated");
        data.put("update_by", 999L);

        doReturn(true).when(repository).tableExists("demo_table");
        doReturn(Set.of("name", "update_by", "update_time"))
                .when(repository).getTableColumns("demo_table");
        when(namedJdbcTemplate.update(eq("UPDATE demo_table SET name = :name, update_by = :update_by, update_time = :update_time WHERE id = :id AND tenant_id = :tenantId"), any(MapSqlParameterSource.class)))
                .thenReturn(1);

        try (MockedStatic<TenantContextHolder> tenantContext = org.mockito.Mockito.mockStatic(TenantContextHolder.class);
             MockedStatic<SessionHelper> sessionHelper = org.mockito.Mockito.mockStatic(SessionHelper.class)) {
            tenantContext.when(TenantContextHolder::getTenantId).thenReturn(7L);
            sessionHelper.when(SessionHelper::getUserId).thenReturn(123L);

            repository.updateById("demo_table", 101L, data);
        }

        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(namedJdbcTemplate).update(eq("UPDATE demo_table SET name = :name, update_by = :update_by, update_time = :update_time WHERE id = :id AND tenant_id = :tenantId"), paramsCaptor.capture());

        MapSqlParameterSource params = paramsCaptor.getValue();
        assertEquals(101L, params.getValue("id"));
        assertEquals("updated", params.getValue("name"));
        assertEquals(999L, params.getValue("update_by"));
        assertEquals(7L, params.getValue("tenantId"));
        assertNotNull(params.getValue("update_time"));
        assertEquals(false, params.hasValue("tenant_id"));
    }
}

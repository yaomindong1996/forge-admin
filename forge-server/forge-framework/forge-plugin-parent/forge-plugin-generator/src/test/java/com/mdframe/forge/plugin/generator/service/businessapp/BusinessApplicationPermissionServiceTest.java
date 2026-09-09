package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessObjectDesignStatus;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDataScopeAdapterDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationRolePermissionDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.service.ApplicationPermissionAdapter;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodePolicyService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPermissionWorkspaceVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BusinessApplicationPermissionService")
class BusinessApplicationPermissionServiceTest {

    private static final String APPLICATION_CODE = "ORDER_APP";
    private static final String MODULE_CODE = "ai:business:ORDER";

    private final BusinessApplicationService applicationService = mock(BusinessApplicationService.class);
    private final BusinessApplicationObjectService objectService = mock(BusinessApplicationObjectService.class);
    private final BusinessAppService businessAppService = mock(BusinessAppService.class);
    private final ApplicationPermissionAdapter permissionAdapter = mock(ApplicationPermissionAdapter.class);
    private final BusinessObjectDesignerService designerService = mock(BusinessObjectDesignerService.class);
    private final LowcodePolicyService lowcodePolicyService = new LowcodePolicyService();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BusinessApplicationPermissionService service = new BusinessApplicationPermissionService(
            applicationService, objectService, businessAppService, permissionAdapter,
            designerService, lowcodePolicyService, objectMapper);

    @BeforeEach
    void setUp() {
        BusinessApplicationVO application = new BusinessApplicationVO();
        application.setId(1L);
        application.setApplicationCode(APPLICATION_CODE);
        application.setApplicationName("订单应用");
        application.setLastPublishVersion(3);
        application.setOptions("""
                {"inAppBuilder":{"nodes":[
                  {"id":"sales","type":"group","title":"销售","systemMenuVisible":true,"sort":1},
                  {"id":"orders","type":"page","title":"订单","parentId":"sales","systemMenuVisible":true,"sort":2}
                ]}}
                """);
        when(applicationService.detailByCode(APPLICATION_CODE)).thenReturn(application);
        when(objectService.list(1L)).thenReturn(List.of(object("FOLLOW_SYSTEM")));
        when(businessAppService.list(org.mockito.ArgumentMatchers.any(BusinessAppQueryDTO.class)))
                .thenReturn(List.of(entry()));
        when(permissionAdapter.listAssignableRoles()).thenReturn(List.of(
                new ApplicationPermissionAdapter.RoleInfo(7L, "销售", "sales", 1, 5)));
        when(permissionAdapter.findResourcesByPermissions(anySet())).thenReturn(resources());
        when(permissionAdapter.loadRoleGrant(eq(7L), anySet(), anySet())).thenReturn(
                new ApplicationPermissionAdapter.RoleGrant(
                        new ApplicationPermissionAdapter.RoleInfo(7L, "销售", "sales", 1, 5),
                        Set.of(100L, 101L, 102L), Map.of()));
    }

    @Test
    @DisplayName("page grant adds the application root and keeps structural groups implicit")
    void addsRootForSelectedPage() {
        BusinessApplicationRolePermissionDTO request = request(Set.of(102L), MODULE_CODE, 3);

        service.saveRolePermission(APPLICATION_CODE, 7L, request);

        verify(permissionAdapter).saveRoleGrant(
                eq(7L),
                org.mockito.ArgumentMatchers.argThat(ids -> ids.containsAll(Set.of(100L, 101L, 102L, 201L))),
                eq(Set.of(102L, 100L)),
                eq(Set.of(MODULE_CODE)),
                eq(Map.of(MODULE_CODE, 3)));
        assertTrue(service.rolePermission(APPLICATION_CODE, 7L).getResourceIds().contains(102L));
        assertFalse(service.rolePermission(APPLICATION_CODE, 7L).getResourceIds().contains(100L));
        assertFalse(service.rolePermission(APPLICATION_CODE, 7L).getResourceIds().contains(101L));
    }

    @Test
    @DisplayName("application access entries are included in the page permission catalog")
    void includesApplicationEntryPermissions() {
        BusinessApplicationPermissionWorkspaceVO.PagePermission entry = service.workspace(APPLICATION_CODE)
                .getPages().stream()
                .filter(page -> "ENTRY".equals(page.getPageType()))
                .findFirst()
                .orElseThrow();

        assertTrue(entry.isRegistered());
        assertEquals("ai:businessApp:open:order_entry", entry.getPermissionCode());
    }

    @Test
    @DisplayName("permission workspace exposes the current adapter and selectable fields")
    void exposesDataScopeAdapter() {
        BusinessApplicationPermissionWorkspaceVO.ObjectPermission object = service.workspace(APPLICATION_CODE)
                .getObjects().get(0);

        assertEquals("FOLLOW_SYSTEM", object.getDataScopeAdapter().getDataScope());
        assertEquals("createBy", object.getDataScopeAdapter().getUserField());
        assertEquals("createDept", object.getDataScopeAdapter().getOrgField());
        assertEquals(2L, object.getSharedApplicationCount());
        assertTrue(object.getDataScopeAdapter().getFields().stream()
                .anyMatch(field -> "regionCode".equals(field.getField())
                        && "region_code".equals(field.getColumnName())));
    }

    @Test
    @DisplayName("resource outside the application catalog is rejected")
    void rejectsResourceOutsideApplication() {
        BusinessApplicationRolePermissionDTO request = request(Set.of(999L), null, null);

        assertThrows(BusinessException.class,
                () -> service.saveRolePermission(APPLICATION_CODE, 7L, request));

        verify(permissionAdapter, never()).saveRoleGrant(eq(7L), anySet(), anySet(), anySet(), anyMap());
    }

    @Test
    @DisplayName("object without FOLLOW_SYSTEM cannot receive a role data scope override")
    void rejectsScopeForObjectWithoutAdapter() {
        when(objectService.list(1L)).thenReturn(List.of(object("TENANT")));
        BusinessApplicationRolePermissionDTO request = request(Set.of(), MODULE_CODE, 5);

        assertThrows(BusinessException.class,
                () -> service.saveRolePermission(APPLICATION_CODE, 7L, request));

        verify(permissionAdapter, never()).saveRoleGrant(eq(7L), anySet(), anySet(), anySet(), anyMap());
    }

    @Test
    @DisplayName("adapter save rejects an object outside the application")
    void rejectsAdapterForObjectOutsideApplication() {
        BusinessApplicationDataScopeAdapterDTO request = adapterRequest();

        assertThrows(BusinessException.class,
                () -> service.saveDataScopeAdapter(APPLICATION_CODE, 99L, request));

        verify(designerService, never()).loadContext(99L);
    }

    @Test
    @DisplayName("adapter save maps fields to columns without replacing the object model")
    void savesAdapterWithCanonicalModel() {
        BusinessObjectDesignerService.DesignerContext context = designerContext("TENANT");
        when(designerService.loadContext(11L)).thenReturn(context);

        BusinessApplicationPermissionWorkspaceVO.ObjectPermission result = service.saveDataScopeAdapter(
                APPLICATION_CODE, 11L, adapterRequest());

        LowcodeModelSchema modelSchema = context.getModelSchema();
        assertEquals("biz_order", modelSchema.getTableName());
        assertEquals(3, modelSchema.getFields().size());
        assertEquals("FOLLOW_SYSTEM", modelSchema.getPolicies().getDataScope());
        assertEquals("createBy", modelSchema.getPolicies().getUserField());
        assertEquals("create_by", modelSchema.getPolicies().getUserColumn());
        assertEquals("createDept", modelSchema.getPolicies().getOrgField());
        assertEquals("create_dept", modelSchema.getPolicies().getOrgColumn());
        assertEquals("region_code", modelSchema.getPolicies().getRegionColumn());
        assertEquals(Boolean.TRUE, modelSchema.getPolicies().getFlowRelatedVisible());
        assertTrue(result.isDataScopeReady());
        assertTrue(result.getDataScopeAdapter().getFlowRelatedVisible());
        verify(designerService).saveDraft(context, BusinessObjectDesignStatus.CHANGED.getCode());
    }

    private BusinessApplicationObjectVO object(String dataScope) {
        BusinessApplicationObjectVO object = new BusinessApplicationObjectVO();
        object.setObjectId(11L);
        object.setObjectCode("ORDER");
        object.setObjectName("订单");
        object.setSharedApplicationCount(2L);
        object.setModelSchema("""
                {
                  "tableName":"biz_order",
                  "fields":[
                    {"field":"createBy","columnName":"create_by","label":"创建人"},
                    {"field":"createDept","columnName":"create_dept","label":"创建部门"},
                    {"field":"regionCode","columnName":"region_code","label":"行政区划"}
                  ],
                  "policies":{"dataScope":"%s"}
                }
                """.formatted(dataScope));
        return object;
    }

    private BusinessObjectDesignerService.DesignerContext designerContext(String dataScope) {
        try {
            BusinessObjectDesignerService.DesignerContext context =
                    new BusinessObjectDesignerService.DesignerContext();
            context.setModelSchema(objectMapper.readValue(object(dataScope).getModelSchema(), LowcodeModelSchema.class));
            return context;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private BusinessApplicationDataScopeAdapterDTO adapterRequest() {
        BusinessApplicationDataScopeAdapterDTO request = new BusinessApplicationDataScopeAdapterDTO();
        request.setDataScope("FOLLOW_SYSTEM");
        request.setUserField("createBy");
        request.setOrgField("createDept");
        request.setRegionField("regionCode");
        return request;
    }

    private BusinessAppVO entry() {
        BusinessAppVO entry = new BusinessAppVO();
        entry.setId(31L);
        entry.setAppCode("ORDER_ENTRY");
        entry.setAppName("订单入口");
        entry.setAdminMenuSyncEnabled(true);
        entry.setSortOrder(1);
        return entry;
    }

    private BusinessApplicationRolePermissionDTO request(Set<Long> resourceIds,
                                                         String moduleCode,
                                                         Integer dataScope) {
        BusinessApplicationRolePermissionDTO request = new BusinessApplicationRolePermissionDTO();
        request.setResourceIds(resourceIds);
        if (moduleCode != null) {
            BusinessApplicationRolePermissionDTO.ModuleScope scope =
                    new BusinessApplicationRolePermissionDTO.ModuleScope();
            scope.setModuleCode(moduleCode);
            scope.setDataScope(dataScope);
            request.setModuleScopes(List.of(scope));
        }
        return request;
    }

    private Map<String, ApplicationPermissionAdapter.ResourceInfo> resources() {
        Map<String, ApplicationPermissionAdapter.ResourceInfo> resources = new LinkedHashMap<>();
        resources.put("ai:business:application:ORDER_APP:page:root", resource(100L, 9L, "root"));
        resources.put("ai:business:application:ORDER_APP:page:sales", resource(101L, 100L, "sales"));
        resources.put("ai:business:application:ORDER_APP:page:orders", resource(102L, 101L, "orders"));
        resources.put("ai:businessApp:open:order_entry", resource(301L, 30L, "order-entry"));
        resources.put("ai:business:ORDER:list", resource(201L, 20L, "list"));
        return resources;
    }

    private ApplicationPermissionAdapter.ResourceInfo resource(Long id, Long parentId, String name) {
        return new ApplicationPermissionAdapter.ResourceInfo(id, parentId, 2, name, name);
    }
}

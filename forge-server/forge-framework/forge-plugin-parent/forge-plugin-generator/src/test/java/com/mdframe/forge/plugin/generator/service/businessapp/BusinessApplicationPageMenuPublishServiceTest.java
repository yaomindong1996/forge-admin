package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPageMenuDTO;
import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BusinessApplicationPageMenuPublishService")
class BusinessApplicationPageMenuPublishServiceTest {

    @Test
    @DisplayName("pages without system-menu mount still register hidden portal access resources")
    void pagesRegisterHiddenPortalAccessByDefault() {
        MenuRegisterAdapter adapter = mock(MenuRegisterAdapter.class);
        when(adapter.syncApplicationPageMenus(eq("hr_apply"), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Map.of());
        BusinessApplicationPageMenuPublishService service = new BusinessApplicationPageMenuPublishService(adapter);

        service.sync(snapshot(List.of(Map.of(
                "id", "page_apply",
                "type", "page",
                "title", "申请列表"
        ))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessApplicationPageMenuDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(adapter).syncApplicationPageMenus(eq("hr_apply"), captor.capture());
        List<BusinessApplicationPageMenuDTO> menus = captor.getValue();
        assertEquals(2, menus.size());
        assertEquals("root", menus.get(0).getNodeId());
        assertTrue(!menus.get(0).isVisible());
        assertEquals("page_apply", menus.get(1).getNodeId());
        assertTrue(!menus.get(1).isVisible());
        assertTrue(!menus.get(1).isExternalParent());
        assertEquals("ai:business:application:hr_apply:page:page_apply", menus.get(1).getPerms());
    }

    @Test
    @DisplayName("enabled pages without explicit parent degrade to hidden portal access instead of failing")
    void enabledPageWithoutParentRegistersHiddenPortalAccess() {
        MenuRegisterAdapter adapter = mock(MenuRegisterAdapter.class);
        when(adapter.syncApplicationPageMenus(eq("hr_apply"), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Map.of());
        BusinessApplicationPageMenuPublishService service = new BusinessApplicationPageMenuPublishService(adapter);

        service.sync(snapshot(List.of(Map.of(
                "id", "page_apply",
                "type", "page",
                "title", "申请列表",
                "systemMenuVisible", true
        ))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessApplicationPageMenuDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(adapter).syncApplicationPageMenus(eq("hr_apply"), captor.capture());
        BusinessApplicationPageMenuDTO page = captor.getValue().stream()
                .filter(item -> "page_apply".equals(item.getNodeId()))
                .findFirst()
                .orElseThrow();
        assertTrue(!page.isVisible());
        assertTrue(!page.isExternalParent());
    }

    @Test
    @DisplayName("enabled page with explicit parent mounts only under that parent")
    void explicitlyMountedPageUsesExternalParent() {
        MenuRegisterAdapter adapter = mock(MenuRegisterAdapter.class);
        when(adapter.syncApplicationPageMenus(eq("hr_apply"), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Map.of());
        BusinessApplicationPageMenuPublishService service = new BusinessApplicationPageMenuPublishService(adapter);

        service.sync(snapshot(List.of(Map.of(
                "id", "page_apply",
                "type", "page",
                "title", "申请列表",
                "systemMenuVisible", true,
                "menuParentId", "42"
        ))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessApplicationPageMenuDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(adapter).syncApplicationPageMenus(eq("hr_apply"), captor.capture());
        List<BusinessApplicationPageMenuDTO> menus = captor.getValue();
        BusinessApplicationPageMenuDTO page = menus.stream()
                .filter(item -> "page_apply".equals(item.getNodeId()))
                .findFirst()
                .orElseThrow();
        assertTrue(menus.stream().anyMatch(item -> "root".equals(item.getNodeId())));
        assertEquals("42", page.getParentNodeId());
        assertTrue(page.isExternalParent());
        assertTrue(page.isVisible());
        assertEquals("/app/hr_apply?pageId=page_apply", page.getPath());
        assertEquals("app-center/application-portal", page.getComponent());
    }

    @Test
    @DisplayName("published menus prefer the application portal slug")
    void publishedMenusPreferPortalSlug() {
        MenuRegisterAdapter adapter = mock(MenuRegisterAdapter.class);
        when(adapter.syncApplicationPageMenus(eq("hr_apply"), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Map.of());
        BusinessApplicationPageMenuPublishService service = new BusinessApplicationPageMenuPublishService(adapter);

        Map<String, Object> application = new java.util.LinkedHashMap<>(Map.of(
                "applicationCode", "hr_apply",
                "portalSlug", "hr-portal",
                "applicationName", "人事申请",
                "options", Map.of("inAppBuilder", Map.of(
                        "homePageId", "page_apply",
                        "nodes", List.of(Map.of(
                                "id", "page_apply",
                                "type", "page",
                                "title", "申请列表",
                                "systemMenuVisible", true,
                                "menuParentId", "42"
                        ))
                ))
        ));
        service.sync(new java.util.LinkedHashMap<>(Map.of("application", application)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessApplicationPageMenuDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(adapter).syncApplicationPageMenus(eq("hr_apply"), captor.capture());
        assertEquals("/app/hr-portal?pageId=page_apply", captor.getValue().stream()
                .filter(item -> "page_apply".equals(item.getNodeId()))
                .findFirst()
                .orElseThrow()
                .getPath());
    }

    @Test
    @DisplayName("object pages publish their standalone CRUD route")
    void objectPageUsesStandaloneCrudRoute() {
        MenuRegisterAdapter adapter = mock(MenuRegisterAdapter.class);
        when(adapter.syncApplicationPageMenus(eq("hr_apply"), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Map.of());
        BusinessApplicationPageMenuPublishService service = new BusinessApplicationPageMenuPublishService(adapter);

        Map<String, Object> application = new java.util.LinkedHashMap<>(Map.of(
                "id", "1001",
                "applicationCode", "hr_apply",
                "applicationName", "人事申请",
                "options", Map.of("inAppBuilder", Map.of(
                        "homePageId", "page_apply",
                        "nodes", List.of(Map.of(
                                "id", "page_apply",
                                "type", "page",
                                "title", "申请表单",
                                "pageType", "object",
                                "systemMenuVisible", true,
                                "menuParentId", "42",
                                "objectRef", Map.of(
                                        "configKey", "hr_apply",
                                        "pageKey", "form",
                                        "pageMode", "form",
                                        "formKey", "hr_apply_form"
                                )
                        ))
                ))
        ));
        service.sync(new java.util.LinkedHashMap<>(Map.of("application", application)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessApplicationPageMenuDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(adapter).syncApplicationPageMenus(eq("hr_apply"), captor.capture());
        BusinessApplicationPageMenuDTO page = captor.getValue().stream()
                .filter(item -> "page_apply".equals(item.getNodeId()))
                .findFirst()
                .orElseThrow();
        assertEquals("/ai/crud-page/hr_apply?pageKey=form&appId=1001&formKey=hr_apply_form&runtimeOpenMode=CREATE_FORM&mode=create",
                page.getPath());
        assertEquals("ai/crud-page", page.getComponent());
    }

    @Test
    @DisplayName("BOTH mount projects separate pc and h5 menu resources")
    void bothMountCreatesTwoClientMenus() {
        MenuRegisterAdapter adapter = mock(MenuRegisterAdapter.class);
        when(adapter.syncApplicationPageMenus(eq("hr_apply"), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Map.of());
        BusinessApplicationPageMenuPublishService service = new BusinessApplicationPageMenuPublishService(adapter);

        service.sync(snapshot(List.of(Map.of(
                "id", "page_apply",
                "type", "page",
                "title", "申请列表",
                "systemMenuVisible", true,
                "mountTarget", "BOTH",
                "menuParentId", "42",
                "mobileMenuParentId", "84"
        ))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessApplicationPageMenuDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(adapter).syncApplicationPageMenus(eq("hr_apply"), captor.capture());
        assertEquals(List.of("pc", "h5"), captor.getValue().stream()
                .filter(item -> "page_apply".equals(item.getNodeId()))
                .map(BusinessApplicationPageMenuDTO::getClientCode).toList());
    }

    @Test
    @DisplayName("mobile page menu points to the H5 lowcode runtime")
    void mobilePageUsesH5RuntimeTarget() {
        MenuRegisterAdapter adapter = mock(MenuRegisterAdapter.class);
        when(adapter.syncApplicationPageMenus(eq("hr_apply"), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Map.of());
        BusinessApplicationPageMenuPublishService service = new BusinessApplicationPageMenuPublishService(adapter);

        Map<String, Object> application = new java.util.LinkedHashMap<>(Map.of(
                "id", "1001",
                "applicationCode", "hr_apply",
                "applicationName", "人事申请",
                "options", Map.of("inAppBuilder", Map.of(
                        "homePageId", "page_apply",
                        "nodes", List.of(Map.of(
                                "id", "page_apply",
                                "type", "page",
                                "title", "移动申请",
                                "systemMenuVisible", true,
                                "mountTarget", "MOBILE",
                                "mobileMenuParentId", "84",
                                "objectRef", Map.of("configKey", "hr_apply")
                        ))
                ))
        ));
        service.sync(new java.util.LinkedHashMap<>(Map.of("application", application)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessApplicationPageMenuDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(adapter).syncApplicationPageMenus(eq("hr_apply"), captor.capture());
        BusinessApplicationPageMenuDTO page = captor.getValue().stream()
                .filter(item -> "page_apply".equals(item.getNodeId()))
                .findFirst()
                .orElseThrow();
        assertEquals("/pages/lowcode-runtime?configKey=hr_apply&appId=1001", page.getPath());
        assertEquals(page.getPath(), page.getComponent());
    }

    @Test
    @DisplayName("mobile pages use configured mobile parent without application root")
    void mobilePagesUseConfiguredParentWithoutAppRoot() {
        MenuRegisterAdapter adapter = mock(MenuRegisterAdapter.class);
        when(adapter.syncApplicationPageMenus(eq("hr_apply"), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Map.of());
        BusinessApplicationPageMenuPublishService service = new BusinessApplicationPageMenuPublishService(adapter);

        service.sync(snapshot(List.of(
                Map.of("id", "legacy_group", "type", "group", "title", "旧分组"),
                Map.of("id", "page_apply", "type", "page", "title", "移动申请",
                        "parentId", "legacy_group",
                        "systemMenuVisible", true, "mountTarget", "MOBILE",
                        "mobileMenuParentId", "84")
        )));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BusinessApplicationPageMenuDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(adapter).syncApplicationPageMenus(eq("hr_apply"), captor.capture());
        List<BusinessApplicationPageMenuDTO> pages = captor.getValue().stream()
                .filter(item -> "page_apply".equals(item.getNodeId()))
                .toList();
        assertEquals(1, pages.size());
        assertEquals("h5", pages.get(0).getClientCode());
        assertEquals("84", pages.get(0).getParentNodeId());
        assertTrue(pages.get(0).isExternalParent());
    }

    @Test
    @DisplayName("validate ignores incomplete mount flags and only checks homepage")
    void validateIgnoresIncompleteMountFlags() {
        BusinessApplicationPageMenuPublishService service =
                new BusinessApplicationPageMenuPublishService(mock(MenuRegisterAdapter.class));
        List<String> errors = service.validate(snapshot(List.of(Map.of(
                "id", "page_apply",
                "type", "page",
                "title", "申请列表",
                "systemMenuVisible", true,
                "mountTarget", "BOTH"
        ))));
        assertTrue(errors.stream().noneMatch(error -> error.contains("父级菜单")));
    }

    private Map<String, Object> snapshot(List<Map<String, Object>> nodes) {
        return new java.util.LinkedHashMap<>(Map.of(
                "application", new java.util.LinkedHashMap<>(Map.of(
                        "applicationCode", "hr_apply",
                        "applicationName", "人事申请",
                        "options", Map.of("inAppBuilder", Map.of(
                                "homePageId", "page_apply",
                                "nodes", nodes
                        ))
                ))
        ));
    }
}

package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationVersion;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationRuntimeVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessApplicationRuntimeService")
class BusinessApplicationRuntimeServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("normal runtime reads the immutable published version")
    void readsPublishedVersionSnapshot() throws Exception {
        BusinessApplicationVO application = application(3);
        Map<String, Object> builder = Map.of(
                "homePageId", "page_home",
                "nodes", List.of(node("page_home", null, null)),
                "pages", Map.of("page_home", Map.of("title", "已发布首页")));
        Map<String, Object> snapshot = Map.of(
                "application", Map.of(
                        "id", "10",
                        "applicationCode", "crm_test",
                        "portalSlug", "crm-portal",
                        "applicationName", "发布时名称",
                        "suiteCode", "crm",
                        "status", 1,
                        "portalConfig", Map.of("themeColor", "#3370ff"),
                        "aiAssistantConfig", Map.of("enabled", true, "pageIds", List.of("page_home")),
                        "options", Map.of("inAppBuilder", builder)),
                "objects", List.of(Map.of(
                        "objectId", "11", "objectCode", "customer", "objectName", "客户",
                        "objectRole", "PRIMARY")),
                "entries", List.of(),
                "extensions", List.of(
                        Map.of("id", "31", "extensionType", "VISUAL_RULE", "hookCode", "BEFORE_SUBMIT",
                                "status", "ENABLED", "enabledVersion", 2, "content", "{\"conditions\":[],\"actions\":[]}"),
                        Map.of("id", "32", "extensionType", "SERVER_BINDING", "hookCode", "AFTER_SUBMIT",
                                "status", "ENABLED", "enabledVersion", 1, "content", "must-not-leak",
                                "configJson", "{\"handlerCode\":\"internal\"}"),
                        Map.of("id", "33", "extensionType", "CLIENT_JS", "hookCode", "PAGE_INIT",
                                "status", "DISABLED", "enabledVersion", 1, "content", "must-not-run")));
        AiBusinessApplicationVersion version = version(3, objectMapper.writeValueAsString(snapshot));
        BusinessApplicationRuntimeService service = service(application, version);

        BusinessApplicationRuntimeVO runtime = service.runtimeByCode("crm_test");

        assertEquals(3, runtime.getVersionNo());
        assertEquals("发布时名称", runtime.getApplication().getApplicationName());
        assertEquals(3, runtime.getApplication().getLastPublishVersion());
        assertEquals(11L, runtime.getObjects().get(0).getObjectId());
        assertTrue(runtime.getApplication().getOptions().contains("已发布首页"));
        assertEquals("crm-portal", runtime.getApplication().getPortalSlug());
        assertTrue(runtime.getApplication().getPortalConfig().contains("#3370ff"));
        assertTrue(runtime.getApplication().getAiAssistantConfig().contains("page_home"));
        assertEquals(2, runtime.getExtensions().size());
        assertTrue(String.valueOf(runtime.getExtensions().get(0).get("content")).contains("conditions"));
        assertFalse(runtime.getExtensions().get(1).containsKey("content"));
        assertFalse(runtime.getExtensions().get(1).containsKey("configJson"));
    }

    @Test
    @DisplayName("portal runtime resolves either the application code or portal slug")
    void portalRuntimeResolvesCodeOrSlug() throws Exception {
        BusinessApplicationVO application = application(2);
        application.setPortalSlug("crm-portal");
        Map<String, Object> snapshot = Map.of(
                "application", Map.of(
                        "id", "10",
                        "applicationCode", "crm_test",
                        "portalSlug", "crm-portal",
                        "applicationName", "客户门户",
                        "suiteCode", "crm",
                        "status", 1,
                        "options", Map.of()),
                "objects", List.of(),
                "entries", List.of());
        BusinessApplicationRuntimeService service = service(
                application,
                version(2, objectMapper.writeValueAsString(snapshot)));

        BusinessApplicationRuntimeVO runtime = service.runtimeByCodeOrSlug("crm-portal");

        assertEquals("crm-portal", runtime.getApplication().getPortalSlug());
        assertEquals("客户门户", runtime.getApplication().getApplicationName());
    }

    @Test
    @DisplayName("legacy primary-object application restores its object page")
    void restoresLegacyPrimaryObjectPage() throws Exception {
        BusinessApplicationVO application = application(2);
        Map<String, Object> snapshot = Map.of(
                "application", Map.of(
                        "id", "10",
                        "applicationCode", "presale_app",
                        "applicationName", "门店预售登记",
                        "suiteCode", "presale",
                        "icon", "ionicons5:CartOutline",
                        "status", 1,
                        "options", Map.of(
                                "primaryObjectCode", "PS_PRESALE_ORDER",
                                "inAppBuilder", Map.of(
                                        "nodes", List.of(),
                                        "pages", Map.of(),
                                        "formAssets", List.of()))),
                "objects", List.of(Map.of(
                        "objectId", "1910000000000001111",
                        "objectCode", "PS_PRESALE_ORDER",
                        "objectName", "预售单",
                        "objectRole", "PRIMARY",
                        "configKey", "ps_presale_order",
                        "layoutType", "master-detail-crud",
                        "options", "{\"pageKey\":\"list\"}")),
                "entries", List.of());
        BusinessApplicationRuntimeService service = service(
                application, version(2, objectMapper.writeValueAsString(snapshot)));

        BusinessApplicationRuntimeVO runtime = service.runtimeByCode("presale_app");
        Map<String, Object> runtimeOptions = objectMapper.readValue(
                runtime.getApplication().getOptions(), Map.class);
        Map<String, Object> builder = (Map<String, Object>) runtimeOptions.get("inAppBuilder");
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) builder.get("nodes");

        assertEquals("page_ps_presale_order", builder.get("homePageId"));
        assertEquals(Set.of("page_ps_presale_order"), ((Map<?, ?>) builder.get("pages")).keySet());
        assertEquals(1, nodes.size());
        assertEquals("预售单", nodes.get(0).get("title"));
        assertEquals("master-detail", nodes.get(0).get("pageTemplate"));
        assertEquals(true, nodes.get(0).get("legacyObjectPage"));
        assertEquals("ps_presale_order",
                ((Map<?, ?>) nodes.get(0).get("objectRef")).get("configKey"));
    }

    @Test
    @DisplayName("workbench uses the published slug and only keeps a reachable published page")
    void workbenchUsesPublishedRuntimeProjection() throws Exception {
        BusinessApplicationVO application = application(2);
        application.setPortalSlug("draft-slug");
        Map<String, Object> builder = Map.of(
                "homePageId", "page_home",
                "nodes", List.of(node("page_home", null, null)),
                "pages", Map.of("page_home", Map.of("title", "正式首页")));
        Map<String, Object> snapshot = Map.of(
                "application", Map.of(
                        "id", "10",
                        "applicationCode", "crm_test",
                        "portalSlug", "released-slug",
                        "applicationName", "已发布应用",
                        "suiteCode", "crm",
                        "status", 1,
                        "portalConfig", Map.of(
                                "permission", Map.of("visibility", "all"),
                                "distribution", Map.of("workbench", Map.of(
                                        "enabled", true,
                                        "targetType", "CURRENT_USER",
                                        "targetUserId", 7))),
                        "options", Map.of("inAppBuilder", builder)),
                "objects", List.of(),
                "entries", List.of());
        BusinessApplicationRuntimeService service = service(
                application,
                version(2, objectMapper.writeValueAsString(snapshot)));
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(7L);
        loginUser.setTenantId(1L);
        try (ExecutionIdentityContextHolder.Scope ignored = ExecutionIdentityContextHolder.open(
                new ExecutionIdentity(loginUser, "USER", 7L, null, 1L, "pc", "workbench-runtime", Set.of()))) {
            List<BusinessApplicationVO> result = service.workbenchApplications();

            assertEquals(1, result.size());
            assertEquals("released-slug", result.get(0).getPortalSlug());
            assertEquals("已发布应用", result.get(0).getApplicationName());
        }
    }

    @Test
    @DisplayName("workbench ignores draft-only distribution switches")
    void workbenchIgnoresDraftDistribution() throws Exception {
        BusinessApplicationVO application = application(2);
        application.setPortalConfig("{\"distribution\":{\"workbench\":{\"enabled\":true,\"targetType\":\"CURRENT_USER\",\"targetUserId\":7}}}");
        Map<String, Object> builder = Map.of(
                "homePageId", "page_home",
                "nodes", List.of(node("page_home", null, null)),
                "pages", Map.of("page_home", Map.of("title", "正式首页")));
        Map<String, Object> snapshot = Map.of(
                "application", Map.of(
                        "id", "10",
                        "applicationCode", "crm_test",
                        "portalSlug", "released-slug",
                        "applicationName", "已发布应用",
                        "suiteCode", "crm",
                        "status", 1,
                        "portalConfig", Map.of("permission", Map.of("visibility", "all")),
                        "options", Map.of("inAppBuilder", builder)),
                "objects", List.of(),
                "entries", List.of());
        BusinessApplicationRuntimeService service = service(
                application,
                version(2, objectMapper.writeValueAsString(snapshot)));
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(7L);
        loginUser.setTenantId(1L);
        try (ExecutionIdentityContextHolder.Scope ignored = ExecutionIdentityContextHolder.open(
                new ExecutionIdentity(loginUser, "USER", 7L, null, 1L, "pc", "workbench-draft", Set.of()))) {
            assertTrue(service.workbenchApplications().isEmpty());
        }
    }

    @Test
    @DisplayName("unpublished application has no formal runtime configuration")
    void unpublishedApplicationIsRejected() {
        BusinessApplicationRuntimeService service = service(application(null), null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.runtimeByCode("crm_test"));

        assertTrue(error.getMessage().contains("尚未发布"));
    }

    @Test
    @DisplayName("RBAC page filtering keeps internal pages and falls back to the first accessible page")
    void filtersPagesAndFallsBackHome() {
        BusinessApplicationRuntimeService service = service(application(1), version(1, "{}"));
        Map<String, Object> builder = new LinkedHashMap<>();
        builder.put("homePageId", "page_admin");
        builder.put("nodes", List.of(
                group("group_sales"),
                menuNode("page_admin", null),
                menuNode("page_sales", "group_sales"),
                hiddenNode("page_hidden")));
        builder.put("pages", Map.of(
                "page_admin", Map.of("title", "管理页"),
                "page_sales", Map.of("title", "销售页"),
                "page_hidden", Map.of("title", "隐藏页")));

        Map<String, Object> filtered = service.filterBuilder(builder, "crm_test",
                Set.of("ai:business:application:crm_test:page:page_sales",
                        "ai:business:application:crm_test:page:group_sales"));
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) filtered.get("nodes");
        Map<String, Object> pages = (Map<String, Object>) filtered.get("pages");

        assertEquals("page_sales", filtered.get("homePageId"));
        assertEquals(List.of("group_sales", "page_sales"),
                nodes.stream().map(item -> String.valueOf(item.get("id"))).toList());
        assertEquals(Set.of("page_sales"), pages.keySet());
        assertFalse(pages.containsKey("page_admin"));
    }

    @Test
    @DisplayName("administrator bypasses page role restrictions")
    void administratorBypassesPageRoleRestrictions() {
        BusinessApplicationRuntimeService service = service(application(1), version(1, "{}"));
        Map<String, Object> builder = new LinkedHashMap<>();
        builder.put("homePageId", "page_admin");
        builder.put("nodes", List.of(
                menuNode("page_admin", null),
                menuNode("page_sales", null)));
        builder.put("pages", Map.of(
                "page_admin", Map.of("title", "管理页"),
                "page_sales", Map.of("title", "销售页")));

        Map<String, Object> filtered = service.filterBuilder(builder, "crm_test", Set.of(), true);

        assertEquals("page_admin", filtered.get("homePageId"));
        assertEquals(Set.of("page_admin", "page_sales"),
                ((Map<?, ?>) filtered.get("pages")).keySet());
    }

    @Test
    @DisplayName("RBAC filtering supports legacy system-menu markers stored in settings")
    void filtersLegacySettingsSystemMenuPage() {
        BusinessApplicationRuntimeService service = service(application(1), version(1, "{}"));
        Map<String, Object> builder = new LinkedHashMap<>();
        builder.put("homePageId", "page_legacy");
        builder.put("nodes", List.of(settingsMenuNode("page_legacy")));
        builder.put("pages", Map.of("page_legacy", Map.of("title", "历史菜单页")));

        Map<String, Object> filtered = service.filterBuilder(builder, "crm_test", Set.of());

        assertEquals(List.of(), filtered.get("nodes"));
        assertEquals(Map.of(), filtered.get("pages"));
        assertEquals(null, filtered.get("homePageId"));
    }

    private BusinessApplicationRuntimeService service(BusinessApplicationVO application,
                                                      AiBusinessApplicationVersion version) {
        BusinessApplicationSnapshotService snapshotService = new BusinessApplicationSnapshotService(
                objectMapper, null, null, null, null, null, null, null, null, null);
        return new BusinessApplicationRuntimeService(
                new StubApplicationService(application),
                new StubVersionService(version),
                snapshotService,
                objectMapper);
    }

    private BusinessApplicationVO application(Integer lastPublishVersion) {
        BusinessApplicationVO application = new BusinessApplicationVO();
        application.setId(10L);
        application.setApplicationCode("crm_test");
        application.setApplicationName("草稿名称");
        application.setSuiteName("CRM");
        application.setStatus(1);
        application.setLastPublishVersion(lastPublishVersion);
        return application;
    }

    private AiBusinessApplicationVersion version(Integer versionNo, String snapshotJson) {
        if (versionNo == null) {
            return null;
        }
        AiBusinessApplicationVersion version = new AiBusinessApplicationVersion();
        version.setApplicationId(10L);
        version.setVersionNo(versionNo);
        version.setSnapshotJson(snapshotJson);
        return version;
    }

    private Map<String, Object> group(String id) {
        return Map.of("id", id, "type", "group", "title", id, "sort", 0,
                "systemMenuVisible", true);
    }

    private Map<String, Object> menuNode(String id, String parentId) {
        Map<String, Object> node = node(id, parentId, null);
        node.put("systemMenuVisible", true);
        return node;
    }

    private Map<String, Object> settingsMenuNode(String id) {
        Map<String, Object> node = node(id, null, null);
        node.put("settings", Map.of("systemMenuVisible", true));
        return node;
    }

    private Map<String, Object> node(String id, String parentId, Map<String, Object> access) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        node.put("type", "page");
        node.put("title", id);
        node.put("sort", "page_admin".equals(id) ? 0 : 10);
        if (parentId != null) {
            node.put("parentId", parentId);
        }
        if (access != null) {
            node.put("access", access);
        }
        return node;
    }

    private Map<String, Object> hiddenNode(String id) {
        Map<String, Object> node = new LinkedHashMap<>(node(id, null, null));
        node.put("navigationVisible", false);
        return node;
    }

    private static class StubApplicationService extends BusinessApplicationService {

        private final BusinessApplicationVO application;

        StubApplicationService(BusinessApplicationVO application) {
            super(null, null, null, null);
            this.application = application;
        }

        @Override
        public BusinessApplicationVO detailByCode(String applicationCode) {
            return application;
        }

        @Override
        public BusinessApplicationVO detailByCodeOrSlug(String identifier) {
            return application;
        }

        @Override
        public BusinessApplicationVO detailByPublishedCodeOrSlug(String identifier) {
            return application;
        }

        @Override
        public List<BusinessApplicationVO> workbenchDistributionCandidates() {
            return List.of(application);
        }

        @Override
        public boolean canCurrentUserAccessPortal(String portalConfig) {
            return true;
        }

        @Override
        public boolean currentUserIsApplicationAdministrator(String portalConfig) {
            return true;
        }
    }

    private static class StubVersionService extends BusinessApplicationVersionService {

        private final AiBusinessApplicationVersion version;

        StubVersionService(AiBusinessApplicationVersion version) {
            super(null, null, null, null);
            this.version = version;
        }

        @Override
        public AiBusinessApplicationVersion requireVersion(Long applicationId, Integer versionNo) {
            return version;
        }
    }
}

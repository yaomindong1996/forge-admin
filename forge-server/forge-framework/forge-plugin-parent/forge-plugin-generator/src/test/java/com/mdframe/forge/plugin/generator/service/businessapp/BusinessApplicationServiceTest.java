package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationDesignStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessSuite;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDistributionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPortalConfigDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationQueryDTO;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationCreateVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessApplicationService")
class BusinessApplicationServiceTest {

    @Test
    @DisplayName("create initializes a tenant-scoped draft application")
    void createInitializesDraftApplication() throws Exception {
        AtomicReference<AiBusinessApplication> inserted = new AtomicReference<>();
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("countByApplicationCode".equals(method)) {
                return 0L;
            }
            if ("insert".equals(method)) {
                AiBusinessApplication application = (AiBusinessApplication) args[0];
                application.setId(101L);
                inserted.set(application);
                return 1;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationDTO dto = applicationDto();

        BusinessApplicationCreateVO result = service.create(dto);

        assertEquals(101L, result.getId());
        assertEquals("crm_center", result.getApplicationCode());
        assertNotNull(inserted.get());
        assertEquals(1L, inserted.get().getTenantId());
        assertEquals(BusinessApplicationDesignStatus.DRAFT.getCode(), inserted.get().getDesignStatus());
        assertEquals("crm_center", inserted.get().getApplicationCode());
        assertEquals("crm_center", inserted.get().getPortalSlug());
    }

    @Test
    @DisplayName("create generates the application code when the caller omits it")
    void createGeneratesApplicationCodeWhenOmitted() throws Exception {
        AtomicReference<AiBusinessApplication> inserted = new AtomicReference<>();
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("countByApplicationCode".equals(method)) {
                return 0L;
            }
            if ("insert".equals(method)) {
                AiBusinessApplication application = (AiBusinessApplication) args[0];
                application.setId(102L);
                inserted.set(application);
                return 1;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationDTO dto = applicationDto();
        dto.setApplicationCode(null);
        dto.setApplicationName("采购仓库");
        dto.setSuiteCode("procurement");

        BusinessApplicationCreateVO result = service.create(dto);

        assertEquals(102L, result.getId());
        assertEquals("procurement_warehouse", result.getApplicationCode());
        assertEquals(result.getApplicationCode(), inserted.get().getApplicationCode());
    }

    @Test
    @DisplayName("generated application codes avoid tenant duplicates with a stable suffix")
    void generatedApplicationCodeAvoidsTenantDuplicates() throws Exception {
        AtomicReference<AiBusinessApplication> inserted = new AtomicReference<>();
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("countByApplicationCode".equals(method)) {
                return "crm_sales_center".equals(args[1]) ? 1L : 0L;
            }
            if ("insert".equals(method)) {
                AiBusinessApplication application = (AiBusinessApplication) args[0];
                application.setId(103L);
                inserted.set(application);
                return 1;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationDTO dto = applicationDto();
        dto.setApplicationCode(" ");
        dto.setApplicationName("sales center");

        BusinessApplicationCreateVO result = service.create(dto);

        assertEquals("crm_sales_center_2", result.getApplicationCode());
        assertEquals(result.getApplicationCode(), inserted.get().getApplicationCode());
    }

    @Test
    @DisplayName("unknown Chinese application names still produce a stable valid code")
    void unknownChineseApplicationNameProducesStableCode() throws Exception {
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("countByApplicationCode".equals(method)) {
                return 0L;
            }
            if ("insert".equals(method)) {
                AiBusinessApplication application = (AiBusinessApplication) args[0];
                application.setId(104L);
                return 1;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationDTO dto = applicationDto();
        dto.setApplicationCode(null);
        dto.setApplicationName("财资中台");

        String code = service.create(dto).getApplicationCode();

        assertTrue(code.matches("^crm_app_[a-z0-9]+$"));
        assertTrue(code.length() <= 64);
    }

    @Test
    @DisplayName("application options reject embedded secrets")
    void applicationOptionsRejectSecrets() throws Exception {
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class,
                BusinessApplicationServiceTest::defaultValue);
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationDTO dto = applicationDto();
        dto.setOptions("{\"integration\":{\"client_secret\":\"plain-text\"}}");

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(dto));

        assertTrue(error.getMessage().contains("不能保存"));
    }

    @Test
    @DisplayName("duplicate active application code is rejected")
    void duplicateApplicationCodeIsRejected() throws Exception {
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("countByApplicationCode".equals(method)) {
                return 1L;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(applicationDto()));

        assertTrue(error.getMessage().contains("应用编码已存在"));
    }

    @Test
    @DisplayName("portal slug rejects reserved words and invalid characters")
    void portalSlugRejectsReservedAndInvalidValues() throws Exception {
        BusinessApplicationService service = service(
                proxy(BusinessApplicationMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));

        BusinessException reserved = assertThrows(BusinessException.class,
                () -> service.slugAvailable("admin", null));
        BusinessException invalid = assertThrows(BusinessException.class,
                () -> service.slugAvailable("中文地址", null));

        assertTrue(reserved.getMessage().contains("保留路径"));
        assertTrue(invalid.getMessage().contains("格式不正确"));
    }

    @Test
    @DisplayName("portal slug availability checks both slugs and application codes")
    void portalSlugAvailabilityChecksSlugAndApplicationCodeCollisions() throws Exception {
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("countByApplicationCode".equals(method)) {
                return "existing_code".equals(args[1]) ? 1L : 0L;
            }
            if ("countByPortalSlug".equals(method)) {
                return "existing_slug".equals(args[1]) ? 1L : 0L;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));

        assertFalse(service.slugAvailable("existing_code", null));
        assertFalse(service.slugAvailable("existing_slug", null));
        assertTrue(service.slugAvailable("available_slug", null));
    }

    @Test
    @DisplayName("application visibility uses trusted role, department and user identity")
    void applicationVisibilityUsesTrustedIdentity() throws Exception {
        BusinessApplicationService service = service(
                proxy(BusinessApplicationMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(7L);
        loginUser.setTenantId(1L);
        loginUser.setRoleIds(List.of(11L));
        loginUser.setOrgIds(List.of(21L));
        try (ExecutionIdentityContextHolder.Scope ignored = ExecutionIdentityContextHolder.open(
                new ExecutionIdentity(loginUser, "USER", 7L, null, 1L, "pc", "visibility-test", java.util.Set.of()))) {
            assertTrue(service.canCurrentUserAccessPortal("{\"permission\":{\"visibility\":\"roles\",\"roleIds\":[\"11\"]}}"));
            assertTrue(service.canCurrentUserAccessPortal("{\"permission\":{\"visibility\":\"departments\",\"departmentIds\":[21]}}"));
            assertTrue(service.canCurrentUserAccessPortal("{\"permission\":{\"visibility\":\"users\",\"userIds\":[7]}}"));
            assertFalse(service.canCurrentUserAccessPortal("{\"permission\":{\"visibility\":\"roles\",\"roleIds\":[99]}}"));
        }
    }

    @Test
    @DisplayName("application administrator bypasses visibility without trusting system menu flags")
    void applicationAdministratorBypassesVisibility() throws Exception {
        BusinessApplicationService service = service(
                proxy(BusinessApplicationMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(7L);
        loginUser.setTenantId(1L);
        try (ExecutionIdentityContextHolder.Scope ignored = ExecutionIdentityContextHolder.open(
                new ExecutionIdentity(loginUser, "USER", 7L, null, 1L, "pc", "administrator-test", java.util.Set.of()))) {
            String config = "{\"permission\":{\"visibility\":\"users\",\"userIds\":[],\"administrators\":[\"7\"]}}";
            assertTrue(service.canCurrentUserAccessPortal(config));
            assertTrue(service.currentUserIsApplicationAdministrator(config));
        }
    }

    @Test
    @DisplayName("published portal slug lookup does not use the mutable design slug")
    void publishedPortalSlugLookupUsesPublishedAlias() throws Exception {
        AtomicBoolean publishedLookup = new AtomicBoolean();
        BusinessApplicationVO published = new BusinessApplicationVO();
        published.setId(101L);
        published.setApplicationCode("crm_center");
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("selectApplicationDetailByCode".equals(method)) {
                return null;
            }
            if ("selectApplicationDetailByPublishedSlug".equals(method)) {
                publishedLookup.set(true);
                return "released_slug".equals(args[1]) ? published : null;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));

        BusinessApplicationVO result = service.detailByPublishedCodeOrSlug("released_slug");

        assertEquals(101L, result.getId());
        assertTrue(publishedLookup.get());
    }

    @Test
    @DisplayName("workbench candidates are published applications only")
    void workbenchCandidatesReturnPublishedApplications() throws Exception {
        List<BusinessApplicationVO> applications = List.of(
                workbenchApplication(1L, "{\"distribution\":{\"workbench\":{\"enabled\":true}}}"),
                workbenchApplication(2L, "{\"distribution\":{\"workbench\":{\"enabled\":false}}}"));
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("selectPublishedWorkbenchApplications".equals(method)) {
                return applications;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        assertEquals(List.of(1L, 2L), service.workbenchDistributionCandidates().stream()
                .map(BusinessApplicationVO::getId).toList());
    }

    @Test
    @DisplayName("workbench distribution reads the supplied portal config")
    void workbenchDistributionReadsSuppliedConfig() throws Exception {
        BusinessApplicationService service = service(
                proxy(BusinessApplicationMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(7L);
        loginUser.setTenantId(1L);
        loginUser.setRoleIds(List.of(11L));
        try (ExecutionIdentityContextHolder.Scope ignored = ExecutionIdentityContextHolder.open(
                new ExecutionIdentity(loginUser, "USER", 7L, null, 1L, "pc", "workbench-test", java.util.Set.of()))) {
            assertTrue(service.isCurrentUserDistributedToWorkbench(
                    "{\"distribution\":{\"workbench\":{\"enabled\":true,\"targetType\":\"CURRENT_USER\",\"targetUserId\":7}}}"));
            assertFalse(service.isCurrentUserDistributedToWorkbench(
                    "{\"distribution\":{\"workbench\":{\"enabled\":true,\"targetType\":\"CURRENT_USER\",\"targetUserId\":8}}}"));
        }
    }

    @Test
    @DisplayName("role distribution rejects roles without active portal permission")
    void roleDistributionRejectsRoleWithoutPortalPermission() throws Exception {
        AiBusinessApplication existing = applicationEntity();
        existing.setLastPublishVersion(1);
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("selectEntityById".equals(method)) {
                return existing;
            }
            if ("countActiveDistributionRoles".equals(method)) {
                return 0L;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationDistributionDTO dto = new BusinessApplicationDistributionDTO();
        dto.setChannel("WORKBENCH");
        dto.setTargetType("ROLES");
        dto.setRoleIds(List.of(9007199254740991L));
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(7L);
        loginUser.setTenantId(1L);
        loginUser.setRoleIds(List.of(9007199254740991L));
        try (ExecutionIdentityContextHolder.Scope ignored = ExecutionIdentityContextHolder.open(
                new ExecutionIdentity(loginUser, "USER", 7L, null, 1L, "pc", "distribution-test", java.util.Set.of()))) {
            BusinessException error = assertThrows(BusinessException.class,
                    () -> service.distribute(existing.getId(), dto));
            assertTrue(error.getMessage().contains("门户访问权限"));
        }
    }

    @Test
    @DisplayName("role distribution respects the current user's role management scope")
    void roleDistributionRespectsRoleManagementScope() throws Exception {
        AiBusinessApplication existing = applicationEntity();
        existing.setLastPublishVersion(1);
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("selectEntityById".equals(method)) {
                return existing;
            }
            if ("countActiveDistributionRoles".equals(method)) {
                return 1L;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationDistributionDTO dto = new BusinessApplicationDistributionDTO();
        dto.setChannel("WORKBENCH");
        dto.setTargetType("ROLES");
        dto.setRoleIds(List.of(12L));
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(7L);
        loginUser.setTenantId(1L);
        loginUser.setRoleIds(List.of(11L));
        try (ExecutionIdentityContextHolder.Scope ignored = ExecutionIdentityContextHolder.open(
                new ExecutionIdentity(loginUser, "USER", 7L, null, 1L, "pc", "distribution-scope-test", java.util.Set.of()))) {
            BusinessException error = assertThrows(BusinessException.class,
                    () -> service.distribute(existing.getId(), dto));
            assertTrue(error.getMessage().contains("角色管理范围"));
        }
    }

    @Test
    @DisplayName("saving portal config persists normalized JSON and marks the draft changed")
    void savePortalConfigPersistsNormalizedJson() throws Exception {
        AiBusinessApplication existing = applicationEntity();
        existing.setPortalSlug("crm_center");
        AtomicReference<AiBusinessApplication> updated = new AtomicReference<>();
        AtomicBoolean markedChanged = new AtomicBoolean();
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("selectEntityById".equals(method)) {
                return existing;
            }
            if ("updateById".equals(method)) {
                updated.set((AiBusinessApplication) args[0]);
                return 1;
            }
            if ("markChanged".equals(method)) {
                markedChanged.set(true);
                return 1;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationPortalConfigDTO dto = new BusinessApplicationPortalConfigDTO();
        dto.setPortalSlug("crm_portal");
        dto.setPortalConfig(Map.of("themeColor", "#3370ff",
                "watermark", Map.of("enabled", true)));

        service.savePortalConfig(existing.getId(), dto);

        assertNotNull(updated.get());
        assertEquals("crm_portal", updated.get().getPortalSlug());
        assertTrue(updated.get().getPortalConfig().contains("#3370ff"));
        assertTrue(markedChanged.get());
    }

    @Test
    @DisplayName("invalid query status is rejected before mapper execution")
    void invalidQueryStatusIsRejected() throws Exception {
        BusinessApplicationService service = service(
                proxy(BusinessApplicationMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationQueryDTO query = new BusinessApplicationQueryDTO();
        query.setStatus(2);

        BusinessException error = assertThrows(BusinessException.class, () -> service.list(query));

        assertEquals("状态值不正确", error.getMessage());
    }

    @Test
    @DisplayName("parent suite filter expands to its complete subtree")
    void parentSuiteFilterExpandsToSubtree() throws Exception {
        AtomicReference<BusinessApplicationQueryDTO> captured = new AtomicReference<>();
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("selectApplicationPage".equals(method)) {
                captured.set((BusinessApplicationQueryDTO) args[2]);
                return args[0];
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationQueryDTO query = new BusinessApplicationQueryDTO();
        query.setSuiteCode("crm");

        service.page(1, 20, query);

        assertEquals(List.of("crm", "crm_sales"), captured.get().getSuiteCodes());
    }

    @Test
    @DisplayName("application code is immutable after creation")
    void applicationCodeIsImmutable() throws Exception {
        AiBusinessApplication existing = applicationEntity();
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("selectEntityById".equals(method)) {
                return existing;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationDTO dto = applicationDto();
        dto.setId(existing.getId());
        dto.setApplicationCode("changed_code");

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(dto));

        assertEquals("应用编码创建后不能修改", error.getMessage());
    }

    @Test
    @DisplayName("deleting application disables menus and detaches active entries")
    void deleteDisablesAndDetachesActiveEntries() throws Exception {
        AiBusinessApplication existing = applicationEntity();
        AiBusinessApp activeEntry = new AiBusinessApp();
        activeEntry.setId(11L);
        activeEntry.setStatus(1);
        activeEntry.setOptions("{\"adminMenu\":{\"menuResourceId\":\"88\"}}");
        AtomicBoolean disabledDetached = new AtomicBoolean();
        AtomicBoolean compositionDeleted = new AtomicBoolean();
        AtomicBoolean applicationDeleted = new AtomicBoolean();
        AtomicReference<Long> disabledMenuId = new AtomicReference<>();
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("selectEntityById".equals(method)) {
                return existing;
            }
            if ("deleteById".equals(method)) {
                applicationDeleted.set(true);
                return 1;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationObjectMapper objectMapper = proxy(BusinessApplicationObjectMapper.class, (method, args) -> {
            if ("logicDeleteByApplicationId".equals(method)) {
                compositionDeleted.set(true);
                return 2;
            }
            return defaultValue(method, args);
        });
        BusinessAppMapper appMapper = proxy(BusinessAppMapper.class, (method, args) -> {
            if ("selectByApplicationId".equals(method)) {
                return List.of(activeEntry);
            }
            if ("disableAndDetachByApplicationId".equals(method)) {
                disabledDetached.set(true);
                return 1;
            }
            return defaultValue(method, args);
        });
        MenuRegisterAdapter menus = new MenuRegisterAdapter() {
            @Override
            public Long registerMenu(String menuName, Long parentId, String configKey, Integer sort) {
                return null;
            }

            @Override
            public void updateMenu(Long menuResourceId, String menuName, Long parentId, Integer sort) {
            }

            @Override
            public void deleteMenu(Long menuResourceId) {
            }

            @Override
            public void disableMenu(Long menuResourceId) {
                disabledMenuId.set(menuResourceId);
            }
        };
        BusinessApplicationService service = service(applicationMapper, objectMapper, appMapper, menus);

        service.delete(existing.getId());

        assertEquals(88L, disabledMenuId.get());
        assertTrue(disabledDetached.get());
        assertTrue(compositionDeleted.get());
        assertTrue(applicationDeleted.get());
    }

    @Test
    @DisplayName("deleting application detaches entries and only deletes composition")
    void deleteDetachesEntriesAndComposition() throws Exception {
        AiBusinessApplication existing = applicationEntity();
        AtomicBoolean disabledDetached = new AtomicBoolean();
        AtomicBoolean compositionDeleted = new AtomicBoolean();
        AtomicBoolean applicationDeleted = new AtomicBoolean();
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("selectEntityById".equals(method)) {
                return existing;
            }
            if ("deleteById".equals(method)) {
                applicationDeleted.set(true);
                return 1;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationObjectMapper objectMapper = proxy(BusinessApplicationObjectMapper.class, (method, args) -> {
            if ("logicDeleteByApplicationId".equals(method)) {
                compositionDeleted.set(true);
                return 2;
            }
            return defaultValue(method, args);
        });
        BusinessAppMapper appMapper = proxy(BusinessAppMapper.class, (method, args) -> {
            if ("selectByApplicationId".equals(method)) {
                return List.of();
            }
            if ("disableAndDetachByApplicationId".equals(method)) {
                disabledDetached.set(true);
                return 3;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper, objectMapper, appMapper);

        service.delete(existing.getId());

        assertTrue(disabledDetached.get());
        assertTrue(compositionDeleted.get());
        assertTrue(applicationDeleted.get());
    }

    private BusinessApplicationService service(BusinessApplicationMapper applicationMapper,
                                               BusinessApplicationObjectMapper objectMapper,
                                               BusinessAppMapper appMapper) throws Exception {
        return service(applicationMapper, objectMapper, appMapper, new MenuRegisterAdapter() {
            @Override
            public Long registerMenu(String menuName, Long parentId, String configKey, Integer sort) {
                return null;
            }

            @Override
            public void updateMenu(Long menuResourceId, String menuName, Long parentId, Integer sort) {
            }

            @Override
            public void deleteMenu(Long menuResourceId) {
            }
        });
    }

    private BusinessApplicationService service(BusinessApplicationMapper applicationMapper,
                                               BusinessApplicationObjectMapper objectMapper,
                                               BusinessAppMapper appMapper,
                                               MenuRegisterAdapter menuRegisterAdapter) throws Exception {
        BusinessApplicationService service = new BusinessApplicationService(
                new ExistingSuiteService(), objectMapper, appMapper, new BusinessNamingService(), menuRegisterAdapter);
        setBaseMapper(service, applicationMapper);
        return service;
    }

    private BusinessApplicationDTO applicationDto() {
        BusinessApplicationDTO dto = new BusinessApplicationDTO();
        dto.setApplicationCode("crm_center");
        dto.setApplicationName("客户经营");
        dto.setSuiteCode("crm");
        dto.setStatus(1);
        return dto;
    }

    private AiBusinessApplication applicationEntity() {
        AiBusinessApplication application = new AiBusinessApplication();
        application.setId(101L);
        application.setTenantId(1L);
        application.setApplicationCode("crm_center");
        application.setApplicationName("客户经营");
        application.setSuiteCode("crm");
        application.setStatus(1);
        application.setDesignStatus(BusinessApplicationDesignStatus.DRAFT.getCode());
        return application;
    }

    private BusinessApplicationVO workbenchApplication(Long id, String portalConfig) {
        BusinessApplicationVO application = new BusinessApplicationVO();
        application.setId(id);
        application.setApplicationCode("app_" + id);
        application.setPortalSlug("app-" + id);
        application.setStatus(1);
        application.setLastPublishVersion(1);
        application.setPortalConfig(portalConfig);
        return application;
    }

    private static void setBaseMapper(Object service, Object mapper) throws Exception {
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, mapper);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, ProxyHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + "Proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return handler.invoke(method.getName(), args == null ? new Object[0] : args);
                });
    }

    private static Object defaultValue(String method, Object[] args) {
        return switch (method) {
            case "countByApplicationCode", "countByPortalSlug", "countByApplicationId",
                    "countActiveByApplicationId", "countActiveDistributionRoles" -> 0L;
            case "insert", "updateById", "deleteById", "detachDisabledByApplicationId",
                    "disableAndDetachByApplicationId", "logicDeleteByApplicationId", "insertBatch" -> 1;
            case "selectByApplicationId" -> List.of();
            default -> null;
        };
    }

    @FunctionalInterface
    private interface ProxyHandler {
        Object invoke(String method, Object[] args) throws Throwable;
    }

    private static class ExistingSuiteService extends BusinessSuiteService {

        ExistingSuiteService() {
            super(null, null, null);
        }

        @Override
        public AiBusinessSuite requireByCode(String suiteCode) {
            AiBusinessSuite suite = new AiBusinessSuite();
            suite.setId(1L);
            suite.setTenantId(1L);
            suite.setSuiteCode(suiteCode);
            return suite;
        }

        @Override
        public List<String> listSelfAndDescendantCodes(String suiteCode) {
            return List.of(suiteCode, suiteCode + "_sales");
        }
    }
}

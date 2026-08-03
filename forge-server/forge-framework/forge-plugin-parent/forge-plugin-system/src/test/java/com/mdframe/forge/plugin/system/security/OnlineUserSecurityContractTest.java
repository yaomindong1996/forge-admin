package com.mdframe.forge.plugin.system.security;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.system.constant.OnlineUserPermissions;
import com.mdframe.forge.plugin.system.controller.SysOnlineUserController;
import com.mdframe.forge.plugin.system.vo.SysOnlineUserVO;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class OnlineUserSecurityContractTest {

    @Test
    void apiPermissionExclusionsShouldNotBypassLoginChecks() throws IOException {
        String config = Files.readString(Path.of(
                "../../forge-starter-parent/forge-starter-auth/src/main/java/"
                        + "com/mdframe/forge/starter/auth/config/SaTokenConfig.java"));

        assertThat(config)
                .doesNotContain(".notMatch(authProperties.getApiPermissionExcludePaths())")
                .contains(".excludePathPatterns(authProperties.getApiPermissionExcludePaths())");
    }

    @Test
    void onlineManagementShouldRequireExplicitPermissions() throws NoSuchMethodException {
        assertPermission("getOnlineUsersPage", OnlineUserPermissions.QUERY,
                Long.class, Long.class, String.class);
        assertPermission("getOnlineUsers", OnlineUserPermissions.QUERY, String.class);
        assertPermission("kickoutUser", OnlineUserPermissions.KICKOUT, Long.class);
        assertPermission("batchKickoutUser", OnlineUserPermissions.BATCH_KICKOUT, java.util.List.class);
        assertPermission("banUser", OnlineUserPermissions.BAN, Long.class, long.class, String.class);
        assertPermission("unbanUser", OnlineUserPermissions.UNBAN, Long.class);
        assertPermission("getUserSessionIds", OnlineUserPermissions.QUERY, Long.class);
    }

    @Test
    void onlineResponsesShouldNotExposeBearerTokens() {
        assertThat(Arrays.stream(SysOnlineUserVO.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName))
                .doesNotContain("tokenValue", "token")
                .contains("sessionId");
        assertThat(Arrays.stream(SysOnlineUserController.class.getDeclaredMethods())
                .map(Method::getName))
                .doesNotContain("test");
    }

    @Test
    void onlineListQueriesShouldNotReadStoredBearerTokens() throws IOException {
        String mapper = Files.readString(Path.of("src/main/resources/mapper/SysOnlineUserMapper.xml"));
        String controller = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/system/controller/SysOnlineUserController.java"));
        String clientController = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/system/controller/SysClientController.java"));

        assertThat(xmlElement(mapper, "OnlineUserSummaryColumns", "sql"))
                .doesNotContain("token_value");
        assertThat(xmlStatement(mapper, "selectOnlineUsersPage"))
                .contains("OnlineUserSummaryColumns")
                .doesNotContain("token_value");
        assertThat(xmlStatement(mapper, "selectOnlineUsers"))
                .contains("OnlineUserSummaryColumns")
                .doesNotContain("token_value");
        assertThat(controller)
                .doesNotContain("@SaIgnore", "RespInfo<List<SysOnlineUser>>", "RespInfo<IPage<SysOnlineUser>>")
                .contains("RespInfo<List<Long>> getUserSessionIds");
        assertThat(clientController).contains("RespInfo<List<SysOnlineUserVO>> getOnlineUsers");
    }

    @Test
    void onlinePermissionMigrationShouldPreserveReadAccessWithoutGrantingActions() throws IOException {
        String migration = Files.readString(Path.of(
                "../../../db/migration/V1.0.56__secure_online_user_management.sql"));

        assertThat(migration)
                .contains("tenant_id = 1", "perms = 'system:online:list'", "system:online:query")
                .contains("system:online:kickout", "system:online:batchKickout",
                        "system:online:ban", "system:online:unban")
                .doesNotContain("INSERT INTO sys_role_resource");
    }

    @Test
    void dashboardStatsShouldTolerateDeniedOnlineQuery() throws IOException {
        String dashboard = Files.readString(Path.of(
                "../../../../forge-admin-ui/src/views/home/index.vue"));

        assertThat(dashboard)
                .contains("Promise.allSettled([", "request.get('/auth/online/page'", "needTip: false")
                .doesNotContain("const [onlineRes, userRes] = await Promise.all([");
    }

    @Test
    void loginShouldEstablishTenantContextBeforeReadingPreviousOnlineSessions() throws IOException {
        String authService = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/system/service/impl/SystemAuthServiceImpl.java"));

        assertThat(authService)
                .contains("executeWithRequiredTenant(loginUser.getTenantId()",
                        "handleSameAccountLogin(loginUser.getUserId(), client, resolvedClient)")
                .contains("TenantContextHolder.setIgnore(false)",
                        "TenantContextHolder.executeWithTenant(tenantId, action)");
    }

    private String xmlStatement(String mapper, String statementId) {
        return xmlElement(mapper, statementId, "select");
    }

    private String xmlElement(String mapper, String elementId, String elementName) {
        int start = mapper.indexOf("id=\"" + elementId + "\"");
        int end = mapper.indexOf("</" + elementName + ">", start);
        assertThat(start).as(elementId).isGreaterThanOrEqualTo(0);
        assertThat(end).as(elementId).isGreaterThan(start);
        return mapper.substring(start, end);
    }

    private void assertPermission(String methodName, String permission, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = SysOnlineUserController.class.getDeclaredMethod(methodName, parameterTypes);
        SaCheckPermission annotation = method.getAnnotation(SaCheckPermission.class);
        assertThat(annotation).as(methodName).isNotNull();
        assertThat(annotation.value()).containsExactly(permission);
    }
}

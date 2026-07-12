package com.mdframe.forge.plugin.capability.identity.context;

import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceTaskDecorator;
import com.mdframe.forge.plugin.capability.identity.domain.AiCapabilityAccessToken;
import com.mdframe.forge.starter.orm.handler.InjectionMetaObjectHandler;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionIdentityContextHolderTest {

    @AfterEach
    void cleanup() {
        ExecutionIdentityContextHolder.clear();
    }

    @Test
    void shouldExposeExplicitUserAndRestoreNestedIdentity() {
        ExecutionIdentity userA = identity(101L, 201L, "client-a", "token-a");
        ExecutionIdentity userB = identity(102L, 202L, "client-b", "token-b");

        try (ExecutionIdentityContextHolder.Scope ignored = ExecutionIdentityContextHolder.open(userA)) {
            assertThat(SessionHelper.getUserId()).isEqualTo(101L);
            assertThat(SessionHelper.getActiveOrgId()).isEqualTo(201L);

            try (ExecutionIdentityContextHolder.Scope nested = ExecutionIdentityContextHolder.open(userB)) {
                assertThat(SessionHelper.getUserId()).isEqualTo(102L);
                assertThat(SessionHelper.getActiveOrgId()).isEqualTo(202L);
            }

            assertThat(SessionHelper.getUserId()).isEqualTo(101L);
        }

        assertThat(ExecutionIdentityContextHolder.current()).isEmpty();
    }

    @Test
    void shouldPropagateCapturedIdentityAndRestoreWorkerThread() {
        ExecutionIdentity userA = identity(101L, 201L, "client-a", "token-a");
        ExecutionIdentity userB = identity(102L, 202L, "client-b", "token-b");
        AtomicLong observedUser = new AtomicLong();
        Runnable decorated;
        try (ExecutionIdentityContextHolder.Scope ignored = ExecutionIdentityContextHolder.open(userA)) {
            decorated = new TenantBusinessDataSourceTaskDecorator()
                    .decorate(() -> observedUser.set(SessionHelper.getUserId()));
        }

        try (ExecutionIdentityContextHolder.Scope ignored = ExecutionIdentityContextHolder.open(userB)) {
            decorated.run();
            assertThat(observedUser.get()).isEqualTo(101L);
            assertThat(SessionHelper.getUserId()).isEqualTo(102L);
        }
    }

    @Test
    void shouldFillOrmAuditFieldsFromExplicitActor() {
        ExecutionIdentity userA = identity(101L, 201L, "client-a", "token-a");
        AiCapabilityAccessToken entity = new AiCapabilityAccessToken();

        try (ExecutionIdentityContextHolder.Scope ignored = ExecutionIdentityContextHolder.open(userA)) {
            new InjectionMetaObjectHandler().insertFill(SystemMetaObject.forObject(entity));
        }

        assertThat(entity.getCreateBy()).isEqualTo(101L);
        assertThat(entity.getUpdateBy()).isEqualTo(101L);
        assertThat(entity.getCreateDept()).isEqualTo(201L);
    }

    @Test
    void shouldKeepImmutableLoginUserSnapshot() {
        LoginUser mutable = new LoginUser();
        mutable.setUserId(101L);
        mutable.setTenantId(1L);
        mutable.setActiveOrgId(201L);
        mutable.setPermissions(new java.util.HashSet<>(Set.of("permission:a")));
        ExecutionIdentity identity = new ExecutionIdentity(
                mutable, "USER", 101L, 999L, 301L,
                "client-a", "token-a", Set.of("mcp:invoke"));

        mutable.setActiveOrgId(999L);
        mutable.getPermissions().clear();
        LoginUser exposed = identity.loginUser();
        exposed.setActiveOrgId(888L);

        assertThat(identity.loginUser().getActiveOrgId()).isEqualTo(201L);
        assertThat(identity.loginUser().getPermissions()).containsExactly("permission:a");
    }

    private ExecutionIdentity identity(Long userId, Long orgId, String clientCode, String tokenId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setTenantId(1L);
        loginUser.setActiveOrgId(orgId);
        loginUser.setUsername("user-" + userId);
        return new ExecutionIdentity(
                loginUser, "USER", userId, 999L, 301L, clientCode, tokenId, Set.of("mcp:invoke"));
    }
}

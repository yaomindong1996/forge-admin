package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.plugin.system.auth.PasswordPolicySupport;
import com.mdframe.forge.plugin.system.auth.UserPasswordCredential;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.config.config.SecurityConfig;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 用真实 Mapper XML 在 H2 的 MySQL / PostgreSQL 兼容模式下跑通密码凭证读写。
 * H2 只是方言模拟，不能替代目标数据库验收。
 */
class SysUserCredentialMapperH2Test {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 11L;
    private static final Long DELETED_USER_ID = 12L;
    private static final LocalDateTime SEEDED_CHANGE_TIME = LocalDateTime.of(2026, 1, 1, 0, 0);

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void readsCredentialAndSkipsDeletedUsers(String mode) throws Exception {
        Fixture fixture = new Fixture(mode);

        UserPasswordCredential credential = fixture.mapper.selectCredential(USER_ID);

        assertThat(credential.getUserId()).isEqualTo(USER_ID);
        assertThat(credential.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(credential.getPassword()).isEqualTo("seed-hash");
        assertThat(credential.getPasswordChangedTime()).isEqualTo(SEEDED_CHANGE_TIME);
        assertThat(fixture.mapper.selectCredential(DELETED_USER_ID)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void changeCredentialUpdatesUserRowAndKeepsHistoryInsideWindow(String mode) throws Exception {
        Fixture fixture = new Fixture(mode);
        fixture.policy.setHistoryCount(2);

        assertThat(fixture.support.changeCredential(USER_ID, "hash-1", false)).isTrue();

        try (var connection = fixture.source.getConnection(); var statement = connection.createStatement();
             ResultSet row = statement.executeQuery("SELECT password, password_version, force_password_change, "
                     + "password_changed_time, update_time FROM sys_user WHERE id = " + USER_ID)) {
            assertThat(row.next()).isTrue();
            assertThat(row.getString("password")).isEqualTo("hash-1");
            assertThat(row.getLong("password_version")).isEqualTo(1L);
            assertThat(row.getInt("force_password_change")).isZero();
            LocalDateTime changedTime = row.getTimestamp("password_changed_time").toLocalDateTime();
            assertThat(changedTime).isAfter(SEEDED_CHANGE_TIME);
            assertThat(row.getTimestamp("update_time").toLocalDateTime()).isEqualTo(changedTime);
        }
        assertThat(fixture.mapper.selectRecentPasswordHashes(TENANT_ID, USER_ID, 5)).containsExactly("seed-hash");

        assertThat(fixture.support.changeCredential(USER_ID, "hash-2", true)).isTrue();
        assertThat(fixture.support.changeCredential(USER_ID, "hash-3", false)).isTrue();

        // 窗口为 2：最早被替换的 seed-hash 已物理删除，按替换时间倒序返回
        assertThat(fixture.mapper.selectRecentPasswordHashes(TENANT_ID, USER_ID, 5))
                .containsExactly("hash-2", "hash-1");
        assertThat(fixture.mapper.selectRecentPasswordHashes(TENANT_ID, USER_ID, 1)).containsExactly("hash-2");
        assertThat(fixture.mapper.selectHistoryIds(TENANT_ID, USER_ID)).hasSize(2);
        assertThat(fixture.queryLong("SELECT password_version FROM sys_user WHERE id = " + USER_ID)).isEqualTo(3L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void forcePasswordChangeFlagIsWrittenAsNumericLiteral(String mode) throws Exception {
        Fixture fixture = new Fixture(mode);

        assertThat(fixture.mapper.updateCredential(USER_ID, "hash-1", true, LocalDateTime.now())).isEqualTo(1);
        assertThat(fixture.queryLong("SELECT force_password_change FROM sys_user WHERE id = " + USER_ID)).isEqualTo(1L);

        assertThat(fixture.mapper.updateCredential(USER_ID, "hash-2", false, LocalDateTime.now())).isEqualTo(1);
        assertThat(fixture.queryLong("SELECT force_password_change FROM sys_user WHERE id = " + USER_ID)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void deletedUserCannotBeUpdated(String mode) throws Exception {
        Fixture fixture = new Fixture(mode);

        assertThat(fixture.support.changeCredential(DELETED_USER_ID, "hash-1", false)).isFalse();
        assertThat(fixture.mapper.updateCredential(DELETED_USER_ID, "hash-1", false, LocalDateTime.now())).isZero();
        assertThat(fixture.queryLong("SELECT COUNT(*) FROM sys_user_password_history")).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void historyIsIsolatedByTenantAndUser(String mode) throws Exception {
        Fixture fixture = new Fixture(mode);
        LocalDateTime now = LocalDateTime.now();
        fixture.mapper.insertPasswordHistory(TENANT_ID, USER_ID, "mine", now);
        fixture.mapper.insertPasswordHistory(TENANT_ID, 99L, "other-user", now);
        fixture.mapper.insertPasswordHistory(2L, USER_ID, "other-tenant", now);

        assertThat(fixture.mapper.selectRecentPasswordHashes(TENANT_ID, USER_ID, 5)).containsExactly("mine");

        List<Long> foreignIds = fixture.mapper.selectHistoryIds(2L, USER_ID);
        assertThat(fixture.mapper.deleteHistoryByIds(TENANT_ID, USER_ID, foreignIds)).isZero();
        assertThat(fixture.queryLong("SELECT COUNT(*) FROM sys_user_password_history")).isEqualTo(3L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MySQL", "PostgreSQL"})
    void reusePolicyWorksAgainstStoredHistory(String mode) throws Exception {
        Fixture fixture = new Fixture(mode);
        fixture.policy.setHistoryCount(1);
        fixture.support.changeCredential(USER_ID, PasswordUtil.encrypt("First1234"), false);
        fixture.support.changeCredential(USER_ID, PasswordUtil.encrypt("Second1234"), false);
        fixture.support.changeCredential(USER_ID, PasswordUtil.encrypt("Third1234"), false);

        assertThatThrownBy(() -> fixture.support.validateNotReused(USER_ID, "Third1234"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("新密码不能与当前密码相同");
        assertThatThrownBy(() -> fixture.support.validateNotReused(USER_ID, "Second1234"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("新密码不能与最近1次使用过的密码相同");
        // 已经滑出窗口的旧密码可以再次使用
        assertThatCode(() -> fixture.support.validateNotReused(USER_ID, "First1234")).doesNotThrowAnyException();
    }

    private static final class Fixture {
        final JdbcDataSource source = new JdbcDataSource();
        final SysUserCredentialMapper mapper;
        final SecurityConfig.PasswordPolicyConfig policy;
        final PasswordPolicySupport support;

        Fixture(String mode) throws Exception {
            source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=" + mode
                    + ";DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
            execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY, tenant_id BIGINT, username VARCHAR(50), "
                    + "password VARCHAR(100), password_version BIGINT DEFAULT 0 NOT NULL, "
                    + "password_changed_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, "
                    + "force_password_change SMALLINT DEFAULT 0 NOT NULL, update_time TIMESTAMP, "
                    + "del_flag BIGINT DEFAULT 0 NOT NULL)");
            execute("CREATE TABLE sys_user_password_history (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, "
                    + "tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, password_hash VARCHAR(100) NOT NULL, "
                    + "changed_time TIMESTAMP NOT NULL, create_dept BIGINT, create_by BIGINT, "
                    + "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, update_by BIGINT, "
                    + "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL)");
            execute("INSERT INTO sys_user (id, tenant_id, username, password, password_changed_time, "
                    + "force_password_change, del_flag) VALUES (" + USER_ID + ", " + TENANT_ID
                    + ", 'alice', 'seed-hash', TIMESTAMP '2026-01-01 00:00:00', 1, 0)");
            execute("INSERT INTO sys_user (id, tenant_id, username, password, del_flag) VALUES ("
                    + DELETED_USER_ID + ", " + TENANT_ID + ", 'gone', 'seed-hash', " + DELETED_USER_ID + ")");

            var configuration = new MybatisConfiguration();
            configuration.addMapper(SysUserCredentialMapper.class);
            var factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(source);
            factory.setConfiguration(configuration);
            factory.setMapperLocations(new ClassPathResource("mapper/SysUserCredentialMapper.xml"));
            factory.afterPropertiesSet();
            mapper = new SqlSessionTemplate(factory.getObject()).getMapper(SysUserCredentialMapper.class);

            SecurityConfig securityConfig = new SecurityConfig();
            policy = securityConfig.getPasswordPolicy();
            ConfigManagerService configManagerService = mock(ConfigManagerService.class);
            when(configManagerService.getSecurityConfig()).thenReturn(securityConfig);
            support = new PasswordPolicySupport(configManagerService, mapper);
        }

        void execute(String sql) throws Exception {
            try (var connection = source.getConnection(); var statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }

        long queryLong(String sql) throws Exception {
            try (var connection = source.getConnection(); var statement = connection.createStatement();
                 ResultSet row = statement.executeQuery(sql)) {
                row.next();
                return row.getLong(1);
            }
        }
    }
}

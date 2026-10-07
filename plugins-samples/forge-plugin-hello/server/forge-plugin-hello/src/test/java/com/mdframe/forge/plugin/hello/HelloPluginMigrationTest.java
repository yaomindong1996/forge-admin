package com.mdframe.forge.plugin.hello;

import com.mdframe.forge.starter.plugin.descriptor.PluginRegistry;
import com.mdframe.forge.starter.plugin.migration.PluginFlywayFactory;
import com.mdframe.forge.starter.plugin.migration.PluginFlywayMigrationStrategy;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HelloPluginMigrationTest {

    private static final String MAIN_HISTORY = "forge_schema_history";
    private final JdbcDataSource dataSource = new JdbcDataSource();

    @BeforeEach
    void initialize() throws Exception {
        dataSource.setURL("jdbc:h2:mem:hello_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        script("/hello-resource-fixture.sql");
    }

    @AfterEach
    void shutdown() throws SQLException {
        execute("SHUTDOWN");
    }

    @Test
    void should_migrate_real_sample_after_host_and_isolate_history() throws Exception {
        migrate();
        assertThat(count("SELECT COUNT(*) FROM sys_resource")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM hello_host_marker")).isZero();
        assertThat(count("SELECT COUNT(*) FROM `" + pluginHistory() + "` WHERE version='1.0.0'"))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM `" + MAIN_HISTORY + "` WHERE version='1.0.1'"))
                .isEqualTo(1);
        assertResources();
    }

    @Test
    void should_not_repeat_resources_or_history_on_restart() throws Exception {
        migrate();
        migrate();
        assertThat(count("SELECT COUNT(*) FROM sys_resource")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM `" + pluginHistory() + "` WHERE version='1.0.0'"))
                .isEqualTo(1);
    }

    @Test
    void should_be_idempotent_without_flyway_history() throws Exception {
        sampleScript();
        sampleScript();
        assertResources();
        assertThat(count("SELECT COUNT(*) FROM sys_resource")).isEqualTo(2);
    }

    @Test
    void should_recreate_after_logical_delete_without_changing_old_rows() throws Exception {
        sampleScript();
        execute("UPDATE sys_resource SET del_flag=id");
        sampleScript();
        assertThat(count("SELECT COUNT(*) FROM sys_resource WHERE del_flag<>0")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM sys_resource WHERE del_flag=0")).isEqualTo(2);
        assertResources();
    }

    @Test
    void should_not_reenable_disabled_resources_or_grant_roles() throws Exception {
        sampleScript();
        execute("UPDATE sys_resource SET visible=0, menu_status=0");
        sampleScript();
        assertThat(count("SELECT COUNT(*) FROM sys_resource WHERE visible=0 AND menu_status=0")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM sys_role_resource WHERE resource_id=999")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM sys_role_resource")).isEqualTo(1);
    }

    @Test
    void should_leave_customer_route_collision_untouched() throws Exception {
        execute("INSERT INTO sys_resource (tenant_id, resource_name, resource_type, path, perms) "
                + "VALUES (1, '客户菜单', 2, '/plugins/hello', 'customer:view')");
        sampleScript();
        assertThat(count("SELECT COUNT(*) FROM sys_resource")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM sys_resource WHERE perms='customer:view'")).isEqualTo(1);
    }

    private void assertResources() throws SQLException {
        assertThat(count("SELECT COUNT(*) FROM sys_resource WHERE del_flag=0 AND tenant_id=1 "
                + "AND feature_code IS NULL AND is_public=0 AND client_code='pc' AND min_user_type=2"))
                .isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM sys_resource WHERE del_flag=0 AND resource_type=2 "
                + "AND path='/plugins/hello' AND component='plugins/hello/index' AND perms='plugin:hello:view'"))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM sys_resource api JOIN sys_resource menu ON api.parent_id=menu.id "
                + "WHERE api.del_flag=0 AND menu.del_flag=0 AND api.resource_type=4 AND api.api_method='GET' "
                + "AND api.api_url='/plugin/hello/info' AND api.perms='plugin:hello:info'"))
                .isEqualTo(1);
    }

    private void migrate() {
        var resolver = new PathMatchingResourcePatternResolver();
        var main = Flyway.configure(getClass().getClassLoader()).dataSource(dataSource)
                .locations("classpath:db/hello-host").table(MAIN_HISTORY).baselineOnMigrate(true)
                .baselineVersion("1.0.0").placeholderReplacement(false).failOnMissingLocations(true).load();
        new PluginFlywayMigrationStrategy(new PluginRegistry(resolver), new PluginFlywayFactory()).migrate(main);
    }

    private String pluginHistory() {
        return MAIN_HISTORY.replace("_schema_history", "_plugin_hello_history");
    }

    private void sampleScript() throws Exception {
        script("/db/plugin/hello/V1.0.0__add_hello_resources.sql");
    }

    private void script(String name) throws Exception {
        try (Connection connection = dataSource.getConnection();
             var reader = new InputStreamReader(getClass().getResourceAsStream(name), StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private int count(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }
}

package com.mdframe.forge.starter.plugin.migration;

import com.mdframe.forge.starter.plugin.descriptor.PluginRegistry;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/** 每个用例独占随机内存库和临时 classpath，永不读取本地应用配置。 */
public final class MigrationFixture implements AutoCloseable {

    public static final String MAIN_SQL = "CREATE TABLE IF NOT EXISTS plugin_test_value (id INT PRIMARY KEY)";
    private static final String MAIN_HISTORY = "forge_schema_history";
    private final Path root;
    private final List<URL> roots = new ArrayList<>();
    private final JdbcDataSource dataSource = new JdbcDataSource();
    private URLClassLoader loader;

    public MigrationFixture(Path root) throws IOException {
        this.root = root;
        dataSource.setURL("jdbc:h2:mem:plugin_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        Path main = root.resolve("main");
        write(main.resolve("db/t2-main/V1.0.1__create_value.sql"), MAIN_SQL);
        roots.add(main.toUri().toURL());
    }

    public JdbcDataSource dataSource() {
        return dataSource;
    }

    public void addPlugin(String id) throws IOException {
        Path classes = root.resolve(id);
        write(classes.resolve("META-INF/forge-plugin.json"), """
                {"id":"%s","name":"迁移测试插件","version":"1.0.0","edition":"community",
                 "requiresCore":">=1.2.0 <2.0.0","features":[]}
                """.formatted(id));
        roots.add(classes.toUri().toURL());
    }

    public void sql(String id, String file, String sql) throws IOException {
        write(root.resolve(id).resolve("db/plugin").resolve(id).resolve(file), sql);
    }

    public void packagePluginAsJar(String id) throws IOException {
        Path classes = root.resolve(id);
        Path jar = root.resolve(id + ".jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar));
             var files = Files.walk(classes)) {
            for (Path file : files.filter(path -> !path.equals(classes)).sorted().toList()) {
                String name = classes.relativize(file).toString().replace('\\', '/');
                output.putNextEntry(new JarEntry(Files.isDirectory(file) ? name + "/" : name));
                if (Files.isRegularFile(file)) {
                    Files.copy(file, output);
                }
                output.closeEntry();
            }
        }
        roots.remove(classes.toUri().toURL());
        roots.add(jar.toUri().toURL());
    }

    public URLClassLoader loader() {
        if (loader == null) {
            loader = new URLClassLoader(roots.toArray(URL[]::new), getClass().getClassLoader());
        }
        return loader;
    }

    public PluginRegistry registry() {
        return new PluginRegistry(new PathMatchingResourcePatternResolver(loader()));
    }

    public FluentConfiguration configuration() {
        return Flyway.configure(loader()).dataSource(dataSource).locations("classpath:db/t2-main")
                .table(MAIN_HISTORY).baselineOnMigrate(true).baselineVersion("1.0.0")
                .placeholderReplacement(false).failOnMissingLocations(true);
    }

    public static String historyTable(String id) {
        // 主表字面量会随脚手架改名，验收同一随机库中真实的工程历史表，而不是固定模板前缀。
        String prefix = MAIN_HISTORY.substring(0, MAIN_HISTORY.length() - "_schema_history".length());
        return prefix + "_plugin_" + id.replace('-', '_') + "_history";
    }

    public static String quotedHistory(String id) {
        return "\"" + historyTable(id) + "\"";
    }

    public void migrate() {
        new PluginFlywayMigrationStrategy(registry(), new PluginFlywayFactory()).migrate(configuration().load());
    }

    public int count(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    public boolean tableExists(String table) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             ResultSet result = connection.getMetaData().getTables(null, null, table, null)) {
            return result.next();
        }
    }

    private static void write(Path file, String source) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
    }

    @Override
    public void close() throws SQLException, IOException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        } finally {
            if (loader != null) {
                loader.close();
            }
        }
    }
}

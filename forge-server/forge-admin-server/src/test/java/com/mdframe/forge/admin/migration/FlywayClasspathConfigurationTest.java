package com.mdframe.forge.admin.migration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayClasspathConfigurationTest {

    @Test
    void defaultsToBundledSqlAndRequiresTheLocationToExist() throws IOException {
        FlywayProperties properties = bind(Map.of());

        assertEquals(List.of("classpath:db/migration"), properties.getLocations());
        assertTrue(properties.isFailOnMissingLocations());
        assertTrue(properties.isEnabled());
        assertTrue(properties.isValidateOnMigrate());
        assertFalse(properties.isPlaceholderReplacement());
        assertEquals("forge_schema_history", properties.getTable());
    }

    @Test
    void explicitExternalLocationStillOverridesTheDefaultWithoutFallback() throws IOException {
        FlywayProperties properties = bind(Map.of(
                "FORGE_FLYWAY_LOCATIONS", "filesystem:/explicit-release/db/migration"));

        assertEquals(List.of("filesystem:/explicit-release/db/migration"), properties.getLocations());
        assertTrue(properties.isFailOnMissingLocations());
        assertTrue(properties.isValidateOnMigrate());
    }

    @Test
    void explicitEnabledFlagRemainsSupported() throws IOException {
        assertFalse(bind(Map.of("FORGE_FLYWAY_ENABLED", "false")).isEnabled());
    }

    @Test
    void everySourceSqlIsAvailableUnchangedWithoutExtraClasspathMigrations() throws IOException {
        Path source = Path.of(System.getProperty("basedir", ".")).resolve("../db/migration").normalize();
        assertTrue(Files.isDirectory(source), "Run this test from the Admin Maven module");
        Set<String> expected;
        try (var files = Files.list(source)) {
            expected = files.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".sql")).collect(Collectors.toSet());
        }
        assertFalse(expected.isEmpty(), "The source migration directory must not be empty");

        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:db/migration/*.sql");
        Set<String> actual = new HashSet<>();
        for (Resource resource : resources) {
            String name = resource.getFilename();
            assertTrue(actual.add(name), "Duplicate migration resource: " + name);
            assertTrue(expected.contains(name), "Unexpected migration resource: " + name);
            try (var stream = resource.getInputStream()) {
                assertArrayEquals(Files.readAllBytes(source.resolve(name)), stream.readAllBytes(), name);
            }
        }
        assertEquals(expected, actual, "Every source SQL must be present on the application classpath");
    }

    private FlywayProperties bind(Map<String, Object> overrides) throws IOException {
        // 只绑定已处理的 application.yml；隔离宿主变量，不启动应用，也不触碰真实数据库。
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(new MapPropertySource("test-overrides", overrides));
        var sources = new YamlPropertySourceLoader().load("admin", new ClassPathResource("application.yml"));
        sources.forEach(environment.getPropertySources()::addLast);
        return Binder.get(environment).bind("spring.flyway", FlywayProperties.class).get();
    }
}

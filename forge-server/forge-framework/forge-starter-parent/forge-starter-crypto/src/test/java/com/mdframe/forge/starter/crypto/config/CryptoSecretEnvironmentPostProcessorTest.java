package com.mdframe.forge.starter.crypto.config;

import com.mdframe.forge.starter.core.context.CryptoProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoSecretEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateAndReuseStableSecrets() {
        Path secretFile = tempDir.resolve("secrets/crypto.properties");
        StandardEnvironment first = environment(secretFile, Map.of());

        new CryptoSecretEnvironmentPostProcessor().postProcessEnvironment(
                first, new SpringApplication(Object.class));

        String firstTransportKey = first.getProperty(CryptoSecretEnvironmentPostProcessor.SECRET_KEY_PROPERTY);
        String firstPersistenceKey = first.getProperty(CryptoSecretEnvironmentPostProcessor.ACTIVE_KEY_PROPERTY);
        assertThat(Base64.getDecoder().decode(firstTransportKey)).hasSize(16);
        assertThat(Base64.getDecoder().decode(firstPersistenceKey)).hasSize(16);
        assertThat(firstPersistenceKey).isNotEqualTo(firstTransportKey);
        assertThat(first.getProperty(CryptoSecretEnvironmentPostProcessor.WRITE_VERSIONED_PROPERTY)).isEqualTo("true");
        assertThat(first.getProperty(CryptoSecretEnvironmentPostProcessor.LEGACY_READ_ENABLED_PROPERTY)).isEqualTo("false");
        assertThat(secretFile).isRegularFile();
        CryptoProperties boundProperties = Binder.get(first)
                .bind("forge.crypto", Bindable.of(CryptoProperties.class))
                .orElseThrow(() -> new AssertionError("forge.crypto 配置未绑定"));
        new CryptoConfigurationValidator().validate(boundProperties);

        StandardEnvironment second = environment(secretFile, Map.of());
        new CryptoSecretEnvironmentPostProcessor().postProcessEnvironment(
                second, new SpringApplication(Object.class));

        assertThat(second.getProperty(CryptoSecretEnvironmentPostProcessor.SECRET_KEY_PROPERTY))
                .isEqualTo(firstTransportKey);
        assertThat(second.getProperty(CryptoSecretEnvironmentPostProcessor.ACTIVE_KEY_PROPERTY))
                .isEqualTo(firstPersistenceKey);
    }

    @Test
    void shouldNotCreateFileWhenTransportKeyIsExplicitlyConfigured() {
        Path secretFile = tempDir.resolve("explicit/crypto.properties");
        String explicitKey = Base64.getEncoder().encodeToString(new byte[16]);
        StandardEnvironment environment = environment(secretFile, Map.of(
                CryptoSecretEnvironmentPostProcessor.SECRET_KEY_PROPERTY, explicitKey));

        new CryptoSecretEnvironmentPostProcessor().postProcessEnvironment(
                environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty(CryptoSecretEnvironmentPostProcessor.SECRET_KEY_PROPERTY))
                .isEqualTo(explicitKey);
        assertThat(secretFile).doesNotExist();
    }

    @Test
    void shouldReplaceBlankExternalValuesWithPersistedValues() {
        Path secretFile = tempDir.resolve("blank/crypto.properties");
        StandardEnvironment environment = environment(secretFile, Map.of(
                CryptoSecretEnvironmentPostProcessor.SECRET_KEY_PROPERTY, " ",
                CryptoSecretEnvironmentPostProcessor.ACTIVE_KEY_PROPERTY, ""));

        new CryptoSecretEnvironmentPostProcessor().postProcessEnvironment(
                environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty(CryptoSecretEnvironmentPostProcessor.SECRET_KEY_PROPERTY)).isNotBlank();
        assertThat(environment.getProperty(CryptoSecretEnvironmentPostProcessor.ACTIVE_KEY_PROPERTY)).isNotBlank();
    }

    @Test
    void shouldFailClosedForCorruptedSecretFile() throws IOException {
        Path secretFile = tempDir.resolve("corrupt/crypto.properties");
        Files.createDirectories(secretFile.getParent());
        Files.writeString(secretFile, "forge.crypto.secret-key=not-base64\n");
        StandardEnvironment environment = environment(secretFile, Map.of());

        assertThatThrownBy(() -> new CryptoSecretEnvironmentPostProcessor().postProcessEnvironment(
                environment, new SpringApplication(Object.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("必须是合法 Base64 编码");
    }

    @Test
    void shouldSerializeConcurrentFirstStart() throws Exception {
        Path secretFile = tempDir.resolve("concurrent/crypto.properties");
        Callable<String> bootstrap = () -> {
            StandardEnvironment environment = environment(secretFile, Map.of());
            new CryptoSecretEnvironmentPostProcessor(new SecureRandom()).postProcessEnvironment(
                    environment, new SpringApplication(Object.class));
            return environment.getProperty(CryptoSecretEnvironmentPostProcessor.SECRET_KEY_PROPERTY);
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var results = executor.invokeAll(java.util.List.of(bootstrap, bootstrap));
            assertThat(results.get(0).get()).isEqualTo(results.get(1).get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRestrictSecretFilePermissionsOnPosixFileSystems() throws IOException {
        Path secretFile = tempDir.resolve("permissions/crypto.properties");
        StandardEnvironment environment = environment(secretFile, Map.of());
        new CryptoSecretEnvironmentPostProcessor().postProcessEnvironment(
                environment, new SpringApplication(Object.class));

        if (Files.getFileAttributeView(secretFile, PosixFileAttributeView.class) != null) {
            assertThat(Files.getPosixFilePermissions(secretFile)).isEqualTo(Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
            assertThat(Files.getPosixFilePermissions(secretFile.getParent())).isEqualTo(Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE));
        }
    }

    @Test
    void shouldRegisterBootstrapBeforeApplicationConfigurationBinding() throws IOException {
        boolean registered = false;
        var resources = CryptoSecretEnvironmentPostProcessor.class.getClassLoader()
                .getResources("META-INF/spring.factories");
        while (resources.hasMoreElements()) {
            Properties factories = new Properties();
            try (InputStream input = resources.nextElement().openStream()) {
                factories.load(input);
            }
            String processors = factories.getProperty("org.springframework.boot.env.EnvironmentPostProcessor");
            if (processors != null && processors.contains(CryptoSecretEnvironmentPostProcessor.class.getName())) {
                registered = true;
                break;
            }
        }
        assertThat(registered).isTrue();
    }

    @Test
    void springApplicationShouldInjectGeneratedSecretsWithoutManualExport() {
        Path secretFile = tempDir.resolve("spring-application/crypto.properties");
        SpringApplication application = new SpringApplication(BootstrapTestConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setLogStartupInfo(false);

        try (ConfigurableApplicationContext context = application.run(
                "--spring.main.banner-mode=off",
                "--logging.level.root=OFF",
                "--forge.crypto.bootstrap.file=" + secretFile)) {
            assertThat(context.getEnvironment().getProperty(
                    CryptoSecretEnvironmentPostProcessor.SECRET_KEY_PROPERTY)).isNotBlank();
            assertThat(context.getEnvironment().getProperty(
                    CryptoSecretEnvironmentPostProcessor.ACTIVE_KEY_PROPERTY)).isNotBlank();
            assertThat(secretFile).isRegularFile();
        }
    }

    private StandardEnvironment environment(Path secretFile, Map<String, Object> overrides) {
        StandardEnvironment environment = new StandardEnvironment();
        Map<String, Object> properties = new java.util.LinkedHashMap<>(overrides);
        properties.put(CryptoSecretEnvironmentPostProcessor.BOOTSTRAP_FILE_PROPERTY, secretFile.toString());
        environment.getPropertySources().addFirst(new MapPropertySource("testOverrides", properties));
        return environment;
    }

    @Configuration(proxyBeanMethods = false)
    static class BootstrapTestConfiguration {
    }
}

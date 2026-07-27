package com.mdframe.forge.starter.crypto.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Injects stable generated crypto secrets before configuration properties are bound.
 */
public final class CryptoSecretEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "forgeCryptoBootstrap";
    static final String BOOTSTRAP_ENABLED_PROPERTY = "forge.crypto.bootstrap.enabled";
    static final String BOOTSTRAP_FILE_PROPERTY = "forge.crypto.bootstrap.file";
    static final String CRYPTO_ENABLED_PROPERTY = "forge.crypto.enabled";
    static final String SECRET_KEY_PROPERTY = "forge.crypto.secret-key";
    static final String PERSISTENCE_ENABLED_PROPERTY = "forge.crypto.persistence.enabled";
    static final String WRITE_VERSIONED_PROPERTY = "forge.crypto.persistence.write-versioned";
    static final String LEGACY_READ_ENABLED_PROPERTY = "forge.crypto.persistence.legacy-read-enabled";
    static final String LEGACY_KEY_PROPERTY = "forge.crypto.persistence.legacy-key";
    static final String ACTIVE_KEY_ID_PROPERTY = "forge.crypto.persistence.active-key-id";
    static final String ACTIVE_KEY_PROPERTY = "forge.crypto.persistence.active-key";
    static final String HISTORICAL_KEY_PREFIX = "forge.crypto.persistence.keys.";

    private static final Pattern KEY_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final Set<String> FIXED_PROPERTIES = Set.of(
            SECRET_KEY_PROPERTY,
            PERSISTENCE_ENABLED_PROPERTY,
            WRITE_VERSIONED_PROPERTY,
            LEGACY_READ_ENABLED_PROPERTY,
            LEGACY_KEY_PROPERTY,
            ACTIVE_KEY_ID_PROPERTY,
            ACTIVE_KEY_PROPERTY
    );
    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE
    );
    private static final Set<PosixFilePermission> FILE_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
    );
    private static final Map<Path, Object> JVM_FILE_LOCKS = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom;

    public CryptoSecretEnvironmentPostProcessor() {
        this(new SecureRandom());
    }

    CryptoSecretEnvironmentPostProcessor(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!getBoolean(environment, BOOTSTRAP_ENABLED_PROPERTY, true)
                || !getBoolean(environment, CRYPTO_ENABLED_PROPERTY, true)
                || StringUtils.hasText(environment.getProperty(SECRET_KEY_PROPERTY))) {
            return;
        }

        Path secretFile = resolveSecretFile(environment);
        Map<String, Object> fileProperties = loadOrCreate(secretFile);
        Map<String, Object> effectiveProperties = applyExternalOverrides(environment, fileProperties);

        environment.getPropertySources().remove(PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(
                new MapPropertySource(PROPERTY_SOURCE_NAME, effectiveProperties));
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    private boolean getBoolean(ConfigurableEnvironment environment, String key, boolean defaultValue) {
        String value = environment.getProperty(key);
        return StringUtils.hasText(value) ? Boolean.parseBoolean(value.trim()) : defaultValue;
    }

    private Path resolveSecretFile(ConfigurableEnvironment environment) {
        String configured = environment.getProperty(BOOTSTRAP_FILE_PROPERTY);
        Path path = StringUtils.hasText(configured)
                ? Path.of(configured.trim())
                : Path.of(System.getProperty("user.home"), ".forge", "secrets", "crypto.properties");
        return path.toAbsolutePath().normalize();
    }

    private Map<String, Object> loadOrCreate(Path secretFile) {
        Object jvmLock = JVM_FILE_LOCKS.computeIfAbsent(secretFile, ignored -> new Object());
        synchronized (jvmLock) {
            try {
                Path directory = secretFile.getParent();
                if (directory == null) {
                    throw new IllegalStateException("自动密钥文件必须具有父目录: " + secretFile);
                }
                Files.createDirectories(directory);
                tightenPermissions(directory, DIRECTORY_PERMISSIONS);
                rejectSymbolicLink(secretFile);

                Path lockFile = directory.resolve(secretFile.getFileName() + ".lock");
                rejectSymbolicLink(lockFile);
                try (FileChannel lockChannel = FileChannel.open(lockFile,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                    tightenPermissions(lockFile, FILE_PERMISSIONS);
                    try (var ignored = lockChannel.lock()) {
                        if (Files.exists(secretFile, LinkOption.NOFOLLOW_LINKS)) {
                            return readAndValidate(secretFile);
                        }
                        Map<String, Object> generated = generateProperties();
                        writeAtomically(secretFile, generated);
                        return generated;
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("无法初始化自动密钥文件: " + secretFile, e);
            }
        }
    }

    private Map<String, Object> generateProperties() {
        Map<String, Object> generated = new LinkedHashMap<>();
        generated.put(SECRET_KEY_PROPERTY, generateBase64Key());
        generated.put(PERSISTENCE_ENABLED_PROPERTY, "true");
        generated.put(WRITE_VERSIONED_PROPERTY, "true");
        generated.put(LEGACY_READ_ENABLED_PROPERTY, "false");
        generated.put(ACTIVE_KEY_ID_PROPERTY, generateKeyId());
        generated.put(ACTIVE_KEY_PROPERTY, generateBase64Key());
        return generated;
    }

    private String generateKeyId() {
        byte[] suffix = new byte[8];
        secureRandom.nextBytes(suffix);
        return "bootstrap-" + Base64.getUrlEncoder().withoutPadding().encodeToString(suffix);
    }

    private String generateBase64Key() {
        byte[] key = new byte[16];
        secureRandom.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    private Map<String, Object> readAndValidate(Path secretFile) throws IOException {
        if (!Files.isRegularFile(secretFile, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("自动密钥路径不是普通文件: " + secretFile);
        }
        tightenPermissions(secretFile, FILE_PERMISSIONS);

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(secretFile)) {
            properties.load(input);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (!FIXED_PROPERTIES.contains(key) && !key.startsWith(HISTORICAL_KEY_PREFIX)) {
                throw new IllegalStateException("自动密钥文件包含不允许的配置键: " + key);
            }
            result.put(key, properties.getProperty(key));
        }
        validateProperties(result);
        return result;
    }

    private void validateProperties(Map<String, Object> properties) {
        String transportKey = required(properties, SECRET_KEY_PROPERTY);
        validateBase64Key(SECRET_KEY_PROPERTY, transportKey);
        boolean persistenceEnabled = requiredBoolean(properties, PERSISTENCE_ENABLED_PROPERTY);
        boolean writeVersioned = requiredBoolean(properties, WRITE_VERSIONED_PROPERTY);
        boolean legacyReadEnabled = requiredBoolean(properties, LEGACY_READ_ENABLED_PROPERTY);

        if (persistenceEnabled && writeVersioned) {
            String activeKeyId = required(properties, ACTIVE_KEY_ID_PROPERTY);
            if (!KEY_ID_PATTERN.matcher(activeKeyId).matches()) {
                throw new IllegalStateException(ACTIVE_KEY_ID_PROPERTY + " 必须匹配 [A-Za-z0-9_-]{1,32}");
            }
            validateBase64Key(ACTIVE_KEY_PROPERTY, required(properties, ACTIVE_KEY_PROPERTY));
        }
        if (properties.containsKey(LEGACY_KEY_PROPERTY)) {
            validateBase64Key(LEGACY_KEY_PROPERTY, required(properties, LEGACY_KEY_PROPERTY));
        } else if (persistenceEnabled && (legacyReadEnabled || !writeVersioned)) {
            validateBase64Key(SECRET_KEY_PROPERTY, transportKey);
        }
        properties.forEach((key, value) -> {
            if (key.startsWith(HISTORICAL_KEY_PREFIX)) {
                String keyId = key.substring(HISTORICAL_KEY_PREFIX.length());
                if (!KEY_ID_PATTERN.matcher(keyId).matches()) {
                    throw new IllegalStateException("历史密钥 keyId 非法: " + keyId);
                }
                validateBase64Key(key, String.valueOf(value));
            }
        });
    }

    private boolean requiredBoolean(Map<String, Object> properties, String key) {
        String value = required(properties, key);
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalStateException(key + " 必须是 true 或 false");
        }
        return Boolean.parseBoolean(value);
    }

    private String required(Map<String, Object> properties, String key) {
        Object value = properties.get(key);
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            throw new IllegalStateException("自动密钥文件缺少配置: " + key);
        }
        return String.valueOf(value).trim();
    }

    private void validateBase64Key(String keyName, String value) {
        try {
            if (Base64.getDecoder().decode(value.trim()).length != 16) {
                throw new IllegalStateException(keyName + " 必须是 Base64 编码的 16 字节密钥");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(keyName + " 必须是合法 Base64 编码", e);
        }
    }

    private Map<String, Object> applyExternalOverrides(ConfigurableEnvironment environment,
                                                       Map<String, Object> fileProperties) {
        Map<String, Object> effective = new LinkedHashMap<>(fileProperties);
        for (String key : fileProperties.keySet()) {
            String externalValue = findExternalOverride(environment, key);
            if (StringUtils.hasText(externalValue)) {
                effective.put(key, externalValue.trim());
            }
        }
        return effective;
    }

    private String findExternalOverride(ConfigurableEnvironment environment, String key) {
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            String name = propertySource.getName();
            if (PROPERTY_SOURCE_NAME.equals(name) || isConfigDataSource(name)) {
                continue;
            }
            Object value = propertySource.getProperty(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private boolean isConfigDataSource(String name) {
        return name.startsWith("Config resource '") || name.startsWith("applicationConfig:");
    }

    private void writeAtomically(Path secretFile, Map<String, Object> values) throws IOException {
        Path directory = secretFile.getParent();
        Path temporary = Files.createTempFile(directory, ".crypto-", ".tmp");
        try {
            tightenPermissions(temporary, FILE_PERMISSIONS);
            Properties properties = new Properties();
            values.forEach((key, value) -> properties.setProperty(key, String.valueOf(value)));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            properties.store(output, "Generated by Forge crypto bootstrap. Do not commit this file.");
            try (FileChannel channel = FileChannel.open(temporary,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.write(ByteBuffer.wrap(output.toByteArray()));
                channel.force(true);
            }
            try {
                Files.move(temporary, secretFile, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, secretFile);
            }
            tightenPermissions(secretFile, FILE_PERMISSIONS);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void rejectSymbolicLink(Path path) {
        if (Files.isSymbolicLink(path)) {
            throw new IllegalStateException("自动密钥路径禁止使用符号链接: " + path);
        }
    }

    private void tightenPermissions(Path path, Set<PosixFilePermission> permissions) throws IOException {
        if (Files.getFileAttributeView(path, PosixFileAttributeView.class) != null) {
            Files.setPosixFilePermissions(path, permissions);
        }
    }
}

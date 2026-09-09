package com.mdframe.forge.starter.file.core;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import com.mdframe.forge.starter.file.storage.FileStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("FileManager upload policy")
@Tag("dev")
class FileManagerTest {

    private ExecutionIdentityContextHolder.Scope identityScope;

    @BeforeEach
    void openIdentity() {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setTenantId(1L);
        identityScope = ExecutionIdentityContextHolder.open(
                new ExecutionIdentity(user, "USER", 1L, null, 1L, "test", "test-token", java.util.Set.of()));
    }

    @AfterEach
    void closeIdentity() {
        if (identityScope != null) {
            identityScope.close();
        }
    }

    @Test
    @DisplayName("rejects upload when storage allowed types are blank")
    void rejectsUploadWhenAllowedTypesAreBlank() throws Exception {
        FileManager fileManager = fileManagerWithAllowedTypes(" ");
        MultipartFile file = multipartFile("report.pdf", "application/pdf");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileManager.upload(file, "test", "1", "local"));

        assertTrue(exception.getMessage().contains("未设置允许的文件类型"));
    }

    @Test
    @DisplayName("uses configured storage whitelist instead of default fallback")
    void usesConfiguredStorageWhitelist() throws Exception {
        FileManager fileManager = fileManagerWithAllowedTypes("pdf");
        MultipartFile file = multipartFile("avatar.jpg", "image/jpeg");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileManager.upload(file, "test", "1", "local"));

        assertTrue(exception.getMessage().contains("不支持的文件类型: jpg"));
    }

    @Test
    @DisplayName("rejects delete when caller cannot modify the file")
    void rejectsDeleteWhenCannotModify() throws Exception {
        FileManager fileManager = new FileManager();
        AtomicBoolean deleted = new AtomicBoolean(false);
        setPersistence(fileManager, persistence(false, deleted));

        BusinessException exception = assertThrows(BusinessException.class, () -> fileManager.delete("file-1"));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("无权删除该文件"));
        assertTrue(!deleted.get());
    }

    @Test
    @DisplayName("deletes metadata when caller can modify the file")
    void deletesWhenCanModify() throws Exception {
        FileManager fileManager = new FileManager();
        AtomicBoolean deleted = new AtomicBoolean(false);
        setPersistence(fileManager, persistence(true, deleted));

        assertTrue(fileManager.delete("file-1"));
        assertTrue(deleted.get());
    }

    @Test
    @DisplayName("rejects private file download when caller lacks permission")
    void rejectsPrivateDownloadWhenCallerLacksPermission() throws Exception {
        FileManager fileManager = new FileManager();
        setPersistence(fileManager, new FileMetadataPersistence() {
            @Override
            public void save(FileMetadata metadata) {
            }

            @Override
            public FileMetadata getById(String fileId) {
                return FileMetadata.builder().fileId(fileId).storageType("local").isPrivate(true).build();
            }

            @Override
            public FileMetadata getByMd5(String md5) {
                return null;
            }

            @Override
            public void incrementDownloadCount(String fileId) {
            }

            @Override
            public void delete(String fileId) {
            }

            @Override
            public boolean checkPermission(String fileId, Long userId) {
                return false;
            }

            @Override
            public boolean canModify(String fileId, Long userId) {
                return false;
            }
        });

        BusinessException exception = assertThrows(BusinessException.class,
                () -> fileManager.download("file-1", new org.springframework.mock.web.MockHttpServletResponse()));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("无权读取"));
    }

    @Test
    @DisplayName("sets no-store headers for private downloads")
    void setsNoStoreHeadersForPrivateDownloads() throws Exception {
        FileManager fileManager = new FileManager();
        FileMetadata metadata = FileMetadata.builder().fileId("file-1").storageType("local")
                .isPrivate(true).originalName("secret.txt").mimeType("text/plain").build();
        setPersistence(fileManager, new FileMetadataPersistence() {
            @Override public void save(FileMetadata value) { }
            @Override public FileMetadata getById(String fileId) { return metadata; }
            @Override public FileMetadata getByMd5(String md5) { return null; }
            @Override public void incrementDownloadCount(String fileId) { }
            @Override public void delete(String fileId) { }
            @Override public boolean checkPermission(String fileId, Long userId) { return true; }
            @Override public boolean canModify(String fileId, Long userId) { return true; }
        });
        FileStorage storage = mock(FileStorage.class);
        when(storage.download("file-1")).thenReturn(new ByteArrayInputStream("secret".getBytes()));
        Field storageField = FileManager.class.getDeclaredField("storageMap");
        storageField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, FileStorage> storageMap = (java.util.Map<String, FileStorage>) storageField.get(fileManager);
        storageMap.put("local", storage);

        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
        fileManager.download("file-1", response);

        assertEquals("private, no-store", response.getHeader("Cache-Control"));
        assertEquals("no-cache", response.getHeader("Pragma"));
    }

    private void setPersistence(FileManager fileManager, FileMetadataPersistence persistence) throws Exception {
        Field field = FileManager.class.getDeclaredField("metadataPersistence");
        field.setAccessible(true);
        field.set(fileManager, persistence);
    }

    private FileMetadataPersistence persistence(boolean allowedToModify, AtomicBoolean deleted) {
        return new FileMetadataPersistence() {
            @Override
            public void save(FileMetadata metadata) {
            }

            @Override
            public FileMetadata getById(String fileId) {
                return FileMetadata.builder().fileId(fileId).storageType("local").build();
            }

            @Override
            public FileMetadata getByMd5(String md5) {
                return null;
            }

            @Override
            public void incrementDownloadCount(String fileId) {
            }

            @Override
            public void delete(String fileId) {
                deleted.set(true);
            }

            @Override
            public boolean checkPermission(String fileId, Long userId) {
                return true;
            }

            @Override
            public boolean canModify(String fileId, Long userId) {
                return allowedToModify;
            }
        };
    }

    private FileManager fileManagerWithAllowedTypes(String allowedTypes) throws Exception {
        FileManager fileManager = new FileManager();
        StorageConfig config = new StorageConfig();
        config.setStorageType("local");
        config.setAllowedTypes(allowedTypes);

        Field field = FileManager.class.getDeclaredField("configProvider");
        field.setAccessible(true);
        field.set(fileManager, new StorageConfigProvider() {
            @Override
            public StorageConfig getDefaultConfig() {
                return config;
            }

            @Override
            public StorageConfig getConfigByType(String storageType) {
                return config;
            }

            @Override
            public List<StorageConfig> getAllEnabledConfigs() {
                return List.of(config);
            }

            @Override
            public void refreshConfig() {
                // Test provider has no cache.
            }
        });
        return fileManager;
    }

    private MultipartFile multipartFile(String originalFilename, String contentType) {
        byte[] content = "test".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return originalFilename;
            }

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public boolean isEmpty() {
                return content.length == 0;
            }

            @Override
            public long getSize() {
                return content.length;
            }

            @Override
            public byte[] getBytes() {
                return content;
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(content);
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                throw new UnsupportedOperationException("Not needed for policy tests");
            }
        };
    }
}

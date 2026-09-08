package com.mdframe.forge.starter.file.storage.impl;

import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalFileStorageTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldStoreValidUploadInsideBaseDirectory() {
        LocalFileStorage storage = createStorage();

        FileMetadata metadata = storage.upload(new ByteArrayInputStream("ok".getBytes()),
                "report.txt", "text/plain", "reports", "1");

        assertTrue(Files.isRegularFile(tempDirectory.resolve(metadata.getFilePath()).normalize()));
    }

    @Test
    void shouldRejectTraversalBusinessType() {
        LocalFileStorage storage = createStorage();

        assertThrows(IllegalArgumentException.class, () -> storage.upload(
                new ByteArrayInputStream("bad".getBytes()), "report.txt", "text/plain", "../escape", "1"));
    }

    @Test
    void shouldRejectTraversalFromPersistedMetadata() {
        LocalFileStorage storage = createStorage();
        FileMetadataPersistence persistence = mock(FileMetadataPersistence.class);
        FileMetadata metadata = FileMetadata.builder().fileId("file-1").filePath("../../outside.txt").build();
        when(persistence.getById("file-1")).thenReturn(metadata);
        ReflectionTestUtils.setField(storage, "metadataPersistence", persistence);

        assertThrows(IllegalArgumentException.class, () -> storage.download("file-1"));
        assertThrows(IllegalArgumentException.class, () -> storage.delete("file-1"));
    }

    @Test
    void shouldRejectTraversalBucketName() {
        LocalFileStorage storage = createStorage();

        assertThrows(IllegalArgumentException.class, () -> storage.createBucket("../escape"));
        assertThrows(IllegalArgumentException.class, () -> storage.deleteBucket("../escape"));
    }

    @Test
    void shouldRejectSymbolicLinkEscape() throws Exception {
        Path base = Files.createDirectory(tempDirectory.resolve("base"));
        Path outside = Files.createDirectory(tempDirectory.resolve("outside"));
        Files.createSymbolicLink(base.resolve("linked"), outside);
        LocalFileStorage storage = createStorage(base);

        assertThrows(IllegalArgumentException.class, () -> storage.upload(
                new ByteArrayInputStream("bad".getBytes()), "report.txt", "text/plain", "linked", "1"));
    }

    @Test
    void shouldRejectIncompleteMultipartUpload() {
        LocalFileStorage storage = createStorage();
        String uploadId = storage.initMultipartUpload("report.txt", "reports", "1");
        storage.uploadPart(uploadId, 1, new ByteArrayInputStream("part".getBytes()));

        assertThrows(IllegalArgumentException.class,
                () -> storage.completeMultipartUpload(uploadId, java.util.List.of("etag-1", "etag-2")));
    }

    @Test
    void shouldCleanExpiredMultipartUpload() throws Exception {
        LocalFileStorage storage = createStorage();
        ReflectionTestUtils.setField(storage, "multipartContextTtlMillis", 1_000L);
        String uploadId = storage.initMultipartUpload("report.txt", "reports", "1");
        Path tempDir = tempDirectory.resolve("multipart").resolve(uploadId);
        assertTrue(Files.isDirectory(tempDir));
        Thread.sleep(1_100L);

        storage.cleanupExpiredMultipartUploads();

        assertTrue(Files.notExists(tempDir));
        assertThrows(RuntimeException.class,
                () -> storage.uploadPart(uploadId, 1, new ByteArrayInputStream("part".getBytes())));
    }

    private LocalFileStorage createStorage() {
        return createStorage(tempDirectory);
    }

    private LocalFileStorage createStorage(Path baseDirectory) {
        StorageConfig config = new StorageConfig();
        config.setBasePath(baseDirectory.toString());
        LocalFileStorage storage = new LocalFileStorage();
        storage.init(config);
        return storage;
    }
}

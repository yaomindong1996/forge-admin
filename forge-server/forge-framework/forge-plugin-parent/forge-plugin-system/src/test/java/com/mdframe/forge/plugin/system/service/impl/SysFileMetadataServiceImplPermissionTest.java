package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysFileMetadata;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysFileMetadataServiceImplPermissionTest {

    @Test
    void getByFileIdShouldRejectWhenReadPermissionDenied() {
        SysFileMetadata privateFile = metadata("f-private", 9L, true);
        FileMetadataPersistence persistence = mock(FileMetadataPersistence.class);
        when(persistence.checkPermission(eq("f-private"), isNull())).thenReturn(false);
        SysFileMetadataServiceImpl service = service(persistence, privateFile, null);

        assertThatThrownBy(() -> service.getByFileId("f-private"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权访问");
    }

    @Test
    void getByFileIdShouldReturnWhenReadable() {
        SysFileMetadata publicFile = metadata("f-public", 9L, false);
        FileMetadataPersistence persistence = mock(FileMetadataPersistence.class);
        when(persistence.checkPermission(eq("f-public"), isNull())).thenReturn(true);
        SysFileMetadataServiceImpl service = service(persistence, publicFile, null);

        assertThat(service.getByFileId("f-public")).isSameAs(publicFile);
    }

    @Test
    void getReadableByIdShouldRejectPrivateForeignFile() {
        SysFileMetadata privateFile = metadata("f-1", 2L, true);
        privateFile.setId(11L);
        FileMetadataPersistence persistence = mock(FileMetadataPersistence.class);
        when(persistence.checkPermission(eq("f-1"), isNull())).thenReturn(false);
        SysFileMetadataServiceImpl service = service(persistence, privateFile, privateFile);

        assertThatThrownBy(() -> service.getReadableById(11L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权访问");
    }

    @Test
    void listByBusinessShouldFilterUnreadables() {
        SysFileMetadata readable = metadata("f-ok", 1L, false);
        SysFileMetadata hidden = metadata("f-no", 2L, true);
        FileMetadataPersistence persistence = mock(FileMetadataPersistence.class);
        when(persistence.checkPermission(eq("f-ok"), isNull())).thenReturn(true);
        when(persistence.checkPermission(eq("f-no"), isNull())).thenReturn(false);
        SysFileMetadataServiceImpl service = new SysFileMetadataServiceImpl(mock(FileManager.class), persistence) {
            @Override
            List<SysFileMetadata> findEnabledByBusiness(String businessType, String businessId) {
                return List.of(readable, hidden);
            }
        };

        assertThat(service.listByBusiness("order", "100")).containsExactly(readable);
    }

    @Test
    void renameShouldRejectWhenUploaderIdMissingForNonAdmin() {
        SysFileMetadata orphan = metadata("f-orphan", null, true);
        FileMetadataPersistence persistence = mock(FileMetadataPersistence.class);
        when(persistence.canModify(eq("f-orphan"), isNull())).thenReturn(false);
        SysFileMetadataServiceImpl service = service(persistence, orphan, null);

        assertThatThrownBy(() -> service.rename("f-orphan", "new-name.png"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
    }

    @Test
    void removeByFileIdShouldCallFileManagerOnlyWhenOwned() {
        SysFileMetadata owned = metadata("f-owned", 7L, true);
        FileManager fileManager = mock(FileManager.class);
        FileMetadataPersistence persistence = mock(FileMetadataPersistence.class);
        when(persistence.canModify(eq("f-owned"), isNull())).thenReturn(true);
        SysFileMetadataServiceImpl service = new SysFileMetadataServiceImpl(fileManager, persistence) {
            @Override
            SysFileMetadata findEnabledByFileId(String fileId) {
                return owned;
            }
        };

        service.removeByFileId("f-owned");
        verify(fileManager).delete("f-owned");
    }

    @Test
    void removeByFileIdShouldNotDeleteWhenOwnershipFails() {
        SysFileMetadata foreign = metadata("f-foreign", 8L, true);
        FileManager fileManager = mock(FileManager.class);
        FileMetadataPersistence persistence = mock(FileMetadataPersistence.class);
        when(persistence.canModify(eq("f-foreign"), isNull())).thenReturn(false);
        SysFileMetadataServiceImpl service = new SysFileMetadataServiceImpl(fileManager, persistence) {
            @Override
            SysFileMetadata findEnabledByFileId(String fileId) {
                return foreign;
            }
        };

        assertThatThrownBy(() -> service.removeByFileId("f-foreign"))
                .isInstanceOf(BusinessException.class);
        verify(fileManager, never()).delete(anyString());
        verify(fileManager, never()).delete(any());
    }

    private static SysFileMetadataServiceImpl service(
            FileMetadataPersistence persistence,
            SysFileMetadata byFileId,
            SysFileMetadata byPk) {
        AtomicReference<SysFileMetadata> byFile = new AtomicReference<>(byFileId);
        AtomicReference<SysFileMetadata> byId = new AtomicReference<>(byPk);
        return new SysFileMetadataServiceImpl(mock(FileManager.class), persistence) {
            @Override
            SysFileMetadata findEnabledByFileId(String fileId) {
                return byFile.get();
            }

            @Override
            public SysFileMetadata getById(java.io.Serializable id) {
                return byId.get();
            }
        };
    }

    private static SysFileMetadata metadata(String fileId, Long uploaderId, boolean isPrivate) {
        SysFileMetadata metadata = new SysFileMetadata();
        metadata.setFileId(fileId);
        metadata.setUploaderId(uploaderId);
        metadata.setIsPrivate(isPrivate);
        metadata.setStatus(1);
        metadata.setOriginalName(fileId + ".png");
        return metadata;
    }
}

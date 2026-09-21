package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.print.service.*;
import com.mdframe.forge.plugin.system.entity.SysFileMetadata;
import com.mdframe.forge.plugin.system.service.ISysFileMetadataService;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.model.FileMetadata;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintLowcodeTestData.ACTOR;

class LowcodePrintResourceAccessTest {
    @Test void validatesTenantThenUsesDownloadAuthorizationAndSafeImageTypes() {
        var identity = mock(PrintIdentity.class); when(identity.current()).thenReturn(ACTOR);
        var metadata = mock(ISysFileMetadataService.class); var files = mock(FileManager.class);
        var service = new LowcodePrintResourceAccess(identity, metadata, files);
        var row = new SysFileMetadata(); row.setTenantId(2L);
        when(metadata.getByFileId("image")).thenReturn(row);
        assertThatThrownBy(() -> service.validate(ACTOR, Set.of("image"))).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(files);
        row.setTenantId(1L); when(files.getFileMetadata("image")).thenThrow(PrintFailure.denied());
        assertThatThrownBy(() -> service.validate(ACTOR, Set.of("image"))).isInstanceOf(RuntimeException.class);
        var file = FileMetadata.builder().build(); file.setMimeType("image/png"); file.setFileSize(100L);
        doReturn(file).when(files).getFileMetadata("image");
        service.validate(ACTOR, Set.of("image"));
        file.setMimeType("image/svg+xml");
        assertThatThrownBy(() -> service.validate(ACTOR, Set.of("image"))).hasMessageContaining("图片");
        file.setMimeType("image/png"); file.setFileSize(11L * 1024 * 1024);
        assertThatThrownBy(() -> service.validate(ACTOR, Set.of("image"))).hasMessageContaining("图片");
    }
}

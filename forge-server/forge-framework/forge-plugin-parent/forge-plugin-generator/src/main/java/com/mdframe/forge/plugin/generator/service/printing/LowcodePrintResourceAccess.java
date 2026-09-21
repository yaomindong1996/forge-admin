package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.plugin.system.service.ISysFileMetadataService;
import com.mdframe.forge.starter.file.core.FileManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class LowcodePrintResourceAccess {
    private final PrintIdentity identity;
    private final ISysFileMetadataService metadata;
    private final FileManager files;

    public void validate(PrintActor actor, Set<String> ids) {
        if (!actor.equals(identity.current())) {
            throw PrintFailure.denied();
        }
        for (String id : ids) {
            var row = metadata.getByFileId(id);
            if (row == null || !actor.tenantId().equals(row.getTenantId())) {
                throw PrintFailure.denied();
            }
            // 使用文件下载同一鉴权链路（私有资源权限、过期检查），不返回直链。
            var file = files.getFileMetadata(id);
            if (file == null || !Set.of("image/png", "image/jpeg", "image/webp").contains(file.getMimeType())
                    || file.getFileSize() == null || file.getFileSize() > 10 * 1024 * 1024) {
                throw PrintFailure.of(400, "PRINT_RESOURCE_INVALID", "打印图片不存在、类型不支持或超过 10MiB");
            }
        }
    }
}

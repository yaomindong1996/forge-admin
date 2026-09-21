package com.mdframe.forge.plugin.print.vo;

import com.mdframe.forge.plugin.print.spi.PrintData;
import java.time.LocalDateTime;

public record PrintContextVO(Long executionId, Long applicationVersionId, Long templateId, Long templateVersionId, Integer versionNo, String schemaJson, PrintData context, PrintFieldCatalogVO catalog, String dataMode, LocalDateTime generatedAt) {
}

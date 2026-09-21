package com.mdframe.forge.plugin.print.vo;

import com.mdframe.forge.plugin.print.entity.PrintTemplateVersion;
import java.time.LocalDateTime;

public record PrintVersionVO(Long id, Long templateId, Integer versionNo, String schemaHash, LocalDateTime publishTime, String schemaJson) {

    public static PrintVersionVO from(PrintTemplateVersion row, boolean schema) {
        return new PrintVersionVO(row.getId(), row.getTemplateId(), row.getVersionNo(), row.getSchemaHash(), row.getPublishTime(), schema ? row.getSchemaJson() : null);
    }
}

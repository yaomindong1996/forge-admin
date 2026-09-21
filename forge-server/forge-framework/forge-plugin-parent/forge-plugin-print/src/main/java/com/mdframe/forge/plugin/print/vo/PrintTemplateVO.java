package com.mdframe.forge.plugin.print.vo;

import com.mdframe.forge.plugin.print.entity.PrintTemplate;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import java.time.LocalDateTime;
import java.util.List;

public record PrintTemplateVO(Long id, PrintSourceRequest source, String templateCode, String templateName, Long draftRevision, String designStatus, Long publishedVersionId, Integer status, String schemaJson, LocalDateTime updateTime) {

    public static PrintTemplateVO from(PrintTemplate row, boolean draft) {
        return new PrintTemplateVO(row.getId(), PrintSourceRequest.from(row), row.getTemplateCode(), row.getTemplateName(), row.getDraftRevision(), row.getDesignStatus(), row.getPublishedVersionId(), row.getStatus(), draft ? row.getDraftSchema() : null, row.getUpdateTime());
    }

    public record Page(List<PrintTemplateVO> records, long total, int pageNum, int pageSize) {
    }
}

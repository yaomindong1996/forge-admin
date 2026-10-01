package com.mdframe.forge.plugin.print.vo;

import com.mdframe.forge.plugin.print.entity.PrintBusinessSource;

import java.time.LocalDateTime;
import java.util.List;

public record PrintBusinessSourceVO(
        Long id,
        String sourceCode,
        String sourceName,
        String sourceType,
        String providerCode,
        Long datasetId,
        String objectCode,
        String parameterSchemaJson,
        String mappingJson,
        Long sourceRevision,
        Integer status,
        LocalDateTime updateTime) {

    public static PrintBusinessSourceVO from(PrintBusinessSource row) {
        return new PrintBusinessSourceVO(
                row.getId(), row.getSourceCode(), row.getSourceName(), row.getSourceType(), row.getProviderCode(),
                row.getDatasetId(), row.getObjectCode(), row.getParameterSchemaJson(), row.getMappingJson(),
                row.getSourceRevision(), row.getStatus(), row.getUpdateTime());
    }

    public record Page(List<PrintBusinessSourceVO> records, long total, int pageNum, int pageSize) {
    }

    public record Option(Long id, String sourceCode, String sourceName, String sourceType, String objectCode) {

        public static Option from(PrintBusinessSource row) {
            return new Option(row.getId(), row.getSourceCode(), row.getSourceName(), row.getSourceType(),
                    row.getObjectCode());
        }
    }
}

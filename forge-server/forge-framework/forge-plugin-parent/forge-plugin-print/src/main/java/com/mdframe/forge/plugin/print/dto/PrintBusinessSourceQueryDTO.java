package com.mdframe.forge.plugin.print.dto;

import com.mdframe.forge.plugin.print.enums.PrintBusinessSourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record PrintBusinessSourceQueryDTO(
        @Size(max = 100) String sourceName,
        PrintBusinessSourceType sourceType,
        @Min(0) @Max(1) Integer status,
        @Min(1) Integer pageNum,
        @Min(1) @Max(100) Integer pageSize) {

    public int resolvedPageNum() {
        return pageNum == null ? 1 : pageNum;
    }

    public int resolvedPageSize() {
        return pageSize == null ? 20 : pageSize;
    }
}

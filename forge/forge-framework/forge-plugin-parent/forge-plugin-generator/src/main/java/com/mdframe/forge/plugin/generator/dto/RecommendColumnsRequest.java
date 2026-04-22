package com.mdframe.forge.plugin.generator.dto;

import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import lombok.Data;

import java.util.List;

@Data
public class RecommendColumnsRequest {

    private Long tableId;

    private List<GenTableColumn> columns;
}

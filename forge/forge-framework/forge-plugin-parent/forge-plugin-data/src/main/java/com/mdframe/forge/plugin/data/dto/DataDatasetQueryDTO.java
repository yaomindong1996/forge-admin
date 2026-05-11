package com.mdframe.forge.plugin.data.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DataDatasetQueryDTO {

    private Long datasetId;

    private Map<String, Object> params;

    private List<String> fields;

    private Integer pageNum;

    private Integer pageSize;

    private Integer maxRows;

    private String outputMode;
}
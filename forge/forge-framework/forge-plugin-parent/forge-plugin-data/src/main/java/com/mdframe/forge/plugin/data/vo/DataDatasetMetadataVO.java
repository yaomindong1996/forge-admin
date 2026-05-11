package com.mdframe.forge.plugin.data.vo;

import lombok.Data;

import java.util.List;

@Data
public class DataDatasetMetadataVO {

    private Long datasetId;

    private String datasetCode;

    private String datasetName;

    private String datasetType;

    private List<DataDatasetFieldVO> fields;

    private String paramSchemaJson;
}
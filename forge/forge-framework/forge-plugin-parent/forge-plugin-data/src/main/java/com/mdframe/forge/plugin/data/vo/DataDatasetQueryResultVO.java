package com.mdframe.forge.plugin.data.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DataDatasetQueryResultVO {

    private List<String> dimensions;

    private List<Map<String, Object>> source;

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    private List<DataDatasetFieldVO> fields;
}
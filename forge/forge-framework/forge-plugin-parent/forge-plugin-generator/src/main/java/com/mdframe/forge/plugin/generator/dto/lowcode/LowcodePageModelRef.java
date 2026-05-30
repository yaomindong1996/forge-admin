package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用页面引用的数据模型快照。
 */
@Data
public class LowcodePageModelRef {

    private Long modelId;

    private String modelCode;

    private String modelName;

    private String tableName;

    private Boolean primary;

    private List<LowcodeRelationSchema> relations = new ArrayList<>();

    private Map<String, Object> props = new LinkedHashMap<>();

    private List<Map<String, Object>> fields = new ArrayList<>();
}

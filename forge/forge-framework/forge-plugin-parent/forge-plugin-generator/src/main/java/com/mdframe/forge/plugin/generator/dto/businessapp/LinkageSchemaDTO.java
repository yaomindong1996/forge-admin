package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段级联和联动 Schema。
 */
@Data
public class LinkageSchemaDTO {

    private String schemaVersion = "linkage-schema-v1";

    private List<Map<String, Object>> rules = new ArrayList<>();

    private Map<String, Object> settings = new LinkedHashMap<>();
}

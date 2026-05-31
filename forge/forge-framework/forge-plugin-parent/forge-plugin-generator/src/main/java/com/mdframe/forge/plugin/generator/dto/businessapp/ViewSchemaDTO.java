package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 查询、列表、详情视图投影 Schema。
 */
@Data
public class ViewSchemaDTO {

    private String schemaVersion = "view-schema-v1";

    private Map<String, Object> search = new LinkedHashMap<>();

    private Map<String, Object> list = new LinkedHashMap<>();

    private Map<String, Object> detail = new LinkedHashMap<>();

    private Map<String, Object> overrides = new LinkedHashMap<>();
}

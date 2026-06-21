package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 表单优先设计器 Schema。
 */
@Data
public class FormDesignerSchemaDTO {

    private String schemaVersion = "form-first-v1";

    private String formKey;

    private String formName;

    private String defaultFormKey;

    private List<Map<String, Object>> forms = new ArrayList<>();

    private Map<String, Object> layout = new LinkedHashMap<>();

    private List<Map<String, Object>> components = new ArrayList<>();

    private Map<String, Object> settings = new LinkedHashMap<>();
}

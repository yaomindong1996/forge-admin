package com.mdframe.forge.plugin.generator.dto.businessapp;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务对象布局设计参数。
 */
@Data
public class BusinessLayoutDTO {

    private String layoutKey;

    private String layoutName;

    private String layoutType;

    private LowcodePageSchema pageSchema;

    private ViewSchemaDTO viewSchema;

    private List<LowcodePageZone> zones = new ArrayList<>();

    private Map<String, Object> settings = new LinkedHashMap<>();
}

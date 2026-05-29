package com.mdframe.forge.plugin.generator.vo.businessapp;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务对象布局视图。
 */
@Data
public class BusinessLayoutVO {

    private String layoutKey;

    private String layoutName;

    private String layoutType;

    private LowcodePageSchema pageSchema;

    private List<LowcodePageZone> zones = new ArrayList<>();

    private Map<String, Object> settings = new LinkedHashMap<>();
}

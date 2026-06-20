package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单表低代码页面搭建协议。
 */
@Data
public class LowcodePageSchema {

    private String layoutType;

    private String listLayoutMode;

    private Map<String, Object> listGridLayout = new LinkedHashMap<>();

    private Long primaryModelId;

    private String primaryModelCode;

    private List<LowcodePageModelRef> modelRefs = new ArrayList<>();

    private List<LowcodePageZone> zones = new ArrayList<>();

    /**
     * 列表设计器多页面画布。运行态首期主要使用 listGridLayout，
     * 但设计器必须完整保存 list/detail/custom 页，避免保存后详情页被默认布局补回。
     */
    private List<Map<String, Object>> pages = new ArrayList<>();

    /** 用户显式删除的内置页面 key，例如 detail。 */
    private List<String> removedPageKeys = new ArrayList<>();
}

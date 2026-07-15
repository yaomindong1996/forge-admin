package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 协调发布选择及自动补齐依赖。
 */
@Data
public class BusinessApplicationAssetSelectionVO {

    private List<Long> objectIds = new ArrayList<>();

    private List<Long> entryIds = new ArrayList<>();

    private List<Long> extensionIds = new ArrayList<>();

    private List<Long> autoIncludedObjectIds = new ArrayList<>();

    private List<String> dependencyMessages = new ArrayList<>();

    private Boolean includeAutomation = true;
}

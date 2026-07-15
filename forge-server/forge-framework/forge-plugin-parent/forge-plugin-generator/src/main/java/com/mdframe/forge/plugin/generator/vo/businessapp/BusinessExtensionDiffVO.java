package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 两个扩展版本的可审查差异。
 */
@Data
public class BusinessExtensionDiffVO {

    private Integer baseVersion;

    private Integer targetVersion;

    private boolean changed;

    private String baseContent;

    private String targetContent;

    private String baseConfigJson;

    private String targetConfigJson;
}

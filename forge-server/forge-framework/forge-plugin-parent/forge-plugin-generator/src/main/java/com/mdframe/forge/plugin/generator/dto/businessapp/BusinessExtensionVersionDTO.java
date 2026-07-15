package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 扩展草稿版本保存参数。
 */
@Data
public class BusinessExtensionVersionDTO {

    private String content;

    private String processedContent;

    private String configJson;

    private String changeSummary;

    private String lockToken;
}

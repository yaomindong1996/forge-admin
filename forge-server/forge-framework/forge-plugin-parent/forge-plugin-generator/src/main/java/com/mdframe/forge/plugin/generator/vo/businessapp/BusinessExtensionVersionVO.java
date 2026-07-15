package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扩展历史版本视图。
 */
@Data
public class BusinessExtensionVersionVO {

    private Long id;

    private Long extensionId;

    private Integer versionNo;

    private String content;

    private String processedContent;

    private String configJson;

    private String contentHash;

    private Integer validationPassed;

    private String validationSummary;

    private Integer testPassed;

    private String testSummary;

    private String changeSummary;

    private Long createBy;

    private LocalDateTime createTime;
}

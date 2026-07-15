package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 不可变应用发布版本视图。
 */
@Data
public class BusinessApplicationVersionVO {

    private Long id;

    private Long applicationId;

    private Integer versionNo;

    private String snapshotHash;

    private String publishStatus;

    private String publishSummary;

    private Integer sourceVersionNo;

    private Long publishedBy;

    private LocalDateTime publishedTime;

    private LocalDateTime createTime;

    private Map<String, Object> snapshot = new LinkedHashMap<>();
}

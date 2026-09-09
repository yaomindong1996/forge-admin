package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

/** 历史版本清理结果，便于前端展示跳过原因。 */
@Data
public class VersionCleanupVO {

    private String modelId;
    private Integer retainLatest;
    private Integer scanned;
    private Integer deleted;
    private Integer skippedProtected;
    private Integer skippedRunning;
    private Integer skippedCurrent;
}

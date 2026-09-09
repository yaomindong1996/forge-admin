package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

/** 流程模型历史版本清理请求。 */
@Data
public class VersionCleanupDTO {

    private String modelId;

    /** 至少保留最近一个版本，最大值由服务端限制为 100。 */
    private Integer retainLatest;
}

package com.mdframe.forge.plugin.job.dto;

import lombok.Data;

/**
 * Cron预览请求。
 */
@Data
public class JobCronPreviewRequest {

    private String cronExpression;

    private String timezone;
}

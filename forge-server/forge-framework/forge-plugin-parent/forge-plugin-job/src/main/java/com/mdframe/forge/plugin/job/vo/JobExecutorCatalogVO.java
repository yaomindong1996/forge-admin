package com.mdframe.forge.plugin.job.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 可配置任务执行目标。
 */
@Data
@Builder
public class JobExecutorCatalogVO {

    private String code;

    private String displayName;

    private String description;

    private String group;

    private String source;

    private String executeMode;

    private String executorBean;

    private String executorMethod;

    private String executorHandler;
}

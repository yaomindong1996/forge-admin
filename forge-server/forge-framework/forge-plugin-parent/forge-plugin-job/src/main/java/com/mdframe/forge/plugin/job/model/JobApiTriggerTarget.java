package com.mdframe.forge.plugin.job.model;

import lombok.Data;

@Data
public class JobApiTriggerTarget {

    private Long id;
    private String jobName;
    private String jobGroup;
    private Integer status;
    private String syncStatus;
}

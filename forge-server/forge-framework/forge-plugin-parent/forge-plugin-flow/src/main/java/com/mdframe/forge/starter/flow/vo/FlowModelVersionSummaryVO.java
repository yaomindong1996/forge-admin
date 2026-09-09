package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.util.Date;

/** Flowable 流程定义版本摘要。 */
@Data
public class FlowModelVersionSummaryVO {

    private String id;
    private String key;
    private String name;
    private Integer version;
    private String deploymentId;
    private Boolean suspended;
    private Date deploymentTime;
}

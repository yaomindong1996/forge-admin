package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.util.Date;

/** 流程实例历史用户任务活动。 */
@Data
public class FlowMonitorActivityVO {

    private String activityId;
    private String activityName;
    private String activityType;
    private String assignee;
    private Date startTime;
    private Date endTime;
}

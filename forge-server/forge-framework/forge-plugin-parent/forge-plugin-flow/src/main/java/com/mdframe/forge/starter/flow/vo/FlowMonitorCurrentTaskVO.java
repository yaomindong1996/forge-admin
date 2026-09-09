package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.util.Date;

/** 流程实例当前活动任务。 */
@Data
public class FlowMonitorCurrentTaskVO {

    private String id;
    private String name;
    private String assignee;
    private Date createTime;
}

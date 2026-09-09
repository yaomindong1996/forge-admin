package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程单据经手人索引。
 */
@Data
@TableName("sys_flow_record_participant")
public class FlowRecordParticipant {

    public static final String INITIATOR = "INITIATOR";
    public static final String ASSIGNEE = "ASSIGNEE";
    public static final String CC = "CC";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String businessType;

    private String businessId;

    private String userId;

    private String relationType;

    private String processInstanceId;

    private LocalDateTime createTime;
}

package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 流程节点信息DTO
 * 用于交互式流程图展示节点详情
 */
@Data
public class ProcessNodeInfo {
    
    /**
     * 节点ID
     */
    private String nodeId;
    
    /**
     * 节点名称
     */
    private String nodeName;
    
    /**
     * 节点类型（startEvent, userTask, serviceTask, endEvent, gateway等）
     */
    private String nodeType;
    
    /**
     * 节点状态（pending-待处理, running-处理中, completed-已完成, skipped-已跳过）
     */
    private String status;
    
    /**
     * 处理人ID列表
     */
    private List<String> assigneeIds;
    
    /**
     * 处理人姓名列表
     */
    private List<String> assigneeNames;
    
    /**
     * 处理人组织列表
     */
    private List<String> assigneeOrgs;
    
    /**
     * 处理人详情列表
     */
    private List<Map<String, Object>> assigneeDetails;
    
    /**
     * 候选用户ID列表
     */
    private List<String> candidateUserIds;
    
    /**
     * 候选用户姓名列表
     */
    private List<String> candidateUserNames;
    
    /**
     * 候选组ID列表
     */
    private List<String> candidateGroupIds;
    
    /**
     * 候选组名称列表
     */
    private List<String> candidateGroupNames;
    
    /**
     * 开始时间
     */
    private Date startTime;
    
    /**
     * 结束时间
     */
    private Date endTime;
    
    /**
     * 处理时长（毫秒）
     */
    private Long duration;
    
    /**
     * 审批意见
     */
    private String comment;
    
    /**
     * 任务ID（当前任务）
     */
    private String taskId;
    
    /**
     * 节点X坐标
     */
    private Double x;
    
    /**
     * 节点Y坐标
     */
    private Double y;
    
    /**
     * 节点宽度
     */
    private Double width;
    
    /**
     * 节点高度
     */
    private Double height;
}
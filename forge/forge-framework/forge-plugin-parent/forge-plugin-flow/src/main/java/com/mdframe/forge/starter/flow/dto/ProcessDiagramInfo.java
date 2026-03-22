package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

import java.util.List;

/**
 * 流程图详情DTO
 * 包含流程图图片和节点信息，用于交互式展示
 */
@Data
public class ProcessDiagramInfo {
    
    /**
     * 流程实例ID
     */
    private String processInstanceId;
    
    /**
     * 流程定义ID
     */
    private String processDefinitionId;
    
    /**
     * 流程名称
     */
    private String processName;
    
    /**
     * 发起人ID
     */
    private String startUserId;
    
    /**
     * 发起人姓名
     */
    private String startUserName;
    
    /**
     * 流程状态（running-运行中, completed-已完成, terminated-已终止）
     */
    private String status;
    
    /**
     * 开始时间
     */
    private java.util.Date startTime;
    
    /**
     * 结束时间
     */
    private java.util.Date endTime;
    
    /**
     * 流程图图片Base64编码
     */
    private String diagramBase64;
    
    /**
     * BPMN XML（用于前端 bpmn-js 渲染）
     */
    private String bpmnXml;
    
    /**
     * 图片宽度
     */
    private Integer imageWidth;
    
    /**
     * 图片高度
     */
    private Integer imageHeight;
    
    /**
     * 节点信息列表
     */
    private List<ProcessNodeInfo> nodes;
}
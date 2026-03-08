package com.mdframe.forge.plugin.message.domain.dto;

import lombok.Data;

/**
 * 消息查询请求DTO
 */
@Data
public class MessageQueryDTO {
    
    /**
     * 消息类型：SYSTEM/SMS/EMAIL/CUSTOM
     */
    private String type;
    
    /**
     * 已读状态：0-未读/1-已读
     */
    private Integer readFlag;
    
    /**
     * 关键词搜索（标题或内容）
     */
    private String keyword;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 结束时间
     */
    private String endTime;
}

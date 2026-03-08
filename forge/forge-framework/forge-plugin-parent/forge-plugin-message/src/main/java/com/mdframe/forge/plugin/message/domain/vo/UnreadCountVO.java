package com.mdframe.forge.plugin.message.domain.vo;

import lombok.Data;

/**
 * 未读消息统计VO
 */
@Data
public class UnreadCountVO {
    
    /**
     * 未读总数
     */
    private Long totalCount;
    
    /**
     * 系统消息未读数
     */
    private Long systemCount;
    
    /**
     * 短信未读数
     */
    private Long smsCount;
    
    /**
     * 邮件未读数
     */
    private Long emailCount;
}

package com.mdframe.forge.plugin.message.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息发送记录表
 */
@Data
@TableName("sys_message_send_record")
public class SysMessageSendRecord implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户编号
     */
    private Long tenantId;
    
    /**
     * 消息ID
     */
    private Long messageId;
    
    /**
     * 发送渠道：WEB/SMS/EMAIL/PUSH
     */
    private String channel;
    
    /**
     * 接收人数量
     */
    private Integer receiverCount;
    
    /**
     * 发送成功数量
     */
    private Integer successCount;
    
    /**
     * 发送失败数量
     */
    private Integer failCount;
    
    /**
     * 第三方渠道返回的消息ID
     */
    private String externalId;
    
    /**
     * 发送状态：0-发送中/1-成功/2-失败
     */
    private Integer status;
    
    /**
     * 错误信息
     */
    private String errorMsg;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
    
    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;
}

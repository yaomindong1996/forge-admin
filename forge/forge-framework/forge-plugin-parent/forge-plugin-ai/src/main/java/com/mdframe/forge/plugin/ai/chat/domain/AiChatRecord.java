package com.mdframe.forge.plugin.ai.chat.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 对话记录实体（对应 ai_chat_record 表）
 */
@Data
@Builder
@TableName("ai_chat_record")
public class AiChatRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 用户ID */
    private Long userId;

    /** Agent 编码 */
    private String agentCode;

    /** 会话ID */
    private String sessionId;

    /** 角色（user / assistant / system） */
    private String role;

    /** 消息内容 */
    private String content;

    /** Token 消耗 */
    private Integer tokenUsage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time")
    private LocalDateTime createTime;
}

package com.mdframe.forge.plugin.ai.session.domain;

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
 * AI 会话实体（对应 ai_chat_session 表）
 */
@Data
@Builder
@TableName("ai_chat_session")
public class AiChatSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话ID（UUID，由前端或服务端生成） */
    @TableId(value = "id")
    private String id;

    /** 租户ID */
    private Long tenantId;

    /** 用户ID */
    private Long userId;

    /** 关联的 Agent 编码 */
    private String agentCode;

    /** 会话标题（首条消息截取） */
    private String sessionName;

    /** 状态（0正常 1已删除） */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}

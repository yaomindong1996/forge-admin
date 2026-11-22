package com.mdframe.forge.plugin.ai.chat.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 对话工具调用明细实体（对应 ai_chat_message_tool_call 表）。
 * 一次工具调用一行，按 record_id 关联 assistant 消息，用于回放、筛选与审计。
 * 必须保留无参构造：自定义 resultMap 只映射部分列，避免 MyBatis 走全参构造器自动映射失败。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_chat_message_tool_call")
public class AiChatMessageToolCall implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 关联的 assistant 消息 id（ai_chat_record.id） */
    private Long recordId;

    /** 会话ID */
    private String sessionId;

    /** 同一消息内的工具调用顺序 */
    private Integer seq;

    /** 工具名称 */
    private String toolName;

    /** 工具入参 JSON */
    private String toolArgsJson;

    /** 工具结果 JSON 或结果摘要 */
    private String toolResultJson;

    /** 状态：pending/running/waiting_confirm/success/error/aborted */
    private String status;

    /** 错误信息 */
    private String errorMsg;

    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    private Long createDept;

    private Long updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除标志（0正常，删除后写主键） */
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}

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
 * AI 对话记录实体（对应 ai_chat_record 表）
 * 保留无参构造，避免自定义 resultMap 触发全参构造器自动映射失败。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    /** 消息内容（assistant 仅存最终回复正文，思考过程见 reasoning） */
    private String content;

    /** 思考过程（reasoning），与正文分离存储 */
    private String reasoning;

    /** Token 消耗（总量，兼容旧字段） */
    private Integer tokenUsage;

    /** Token 用量明细 JSON（prompt/completion/total 等） */
    private String usageJson;

    /** 首 token 耗时（毫秒）：从发起到首个正文/思考增量，仅 assistant 行 */
    private Long firstTokenMs;

    /** 生成总耗时（毫秒）：从发起到 assistant 收口，仅 assistant 行 */
    private Long totalMs;

    /** 附件明细 JSON（挂在用户消息行，存 fileId/类型/缩略图等） */
    private String attachmentJson;

    /** 消息状态：streaming/waiting_confirm/done/error/aborted */
    private String status;

    /** HITL 中断标识，waiting_confirm 时写入，resume 后接回同一行 */
    private String interruptId;

    /** 失败原因，status=error 时写入 */
    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除标志（0正常，删除后写主键），支撑重试/重生成软删旧行 */
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}

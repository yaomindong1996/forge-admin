package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 业务应用平台-消息推送通道实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_message_channel")
public class AiBusinessMessageChannel extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String channelCode;

    private String channelName;

    /** WECHAT_WORK/FEISHU/DINGTALK/WEBHOOK/INTERNAL */
    private String channelType;

    /** 通道配置安全引用（不存明文密钥） */
    private String channelConfigRef;

    /** Webhook地址（仅WEBHOOK类型） */
    private String webhookUrl;

    private String description;

    /** 1-启用，0-禁用 */
    private Integer status;

    private Integer sortOrder;

    /** 扩展配置JSON */
    private String options;
}

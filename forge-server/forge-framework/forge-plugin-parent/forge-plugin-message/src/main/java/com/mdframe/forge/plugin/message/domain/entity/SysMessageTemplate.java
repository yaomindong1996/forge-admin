package com.mdframe.forge.plugin.message.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息模板表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message_template")
public class SysMessageTemplate extends BaseEntity {
    
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
     * 模板编码（唯一）
     */
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;
    
    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    
    /**
     * 消息类型：SYSTEM/SMS/EMAIL/CUSTOM
     */
    private String type;
    
    /**
     * 标题模板（支持${}变量占位符）
     */
    private String titleTemplate;
    
    /**
     * 内容模板（支持${}变量占位符）
     */
    @NotBlank(message = "内容模板不能为空")
    private String contentTemplate;
    
    /**
     * 默认发送渠道
     */
    private String defaultChannel;
    
    /**
     * 是否启用：0-禁用/1-启用
     */
    private Integer enabled;
    
    /**
     * 备注说明
     */
    private String remark;

    /**
     * 删除标志：0-正常，删除后写主键
     */
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}

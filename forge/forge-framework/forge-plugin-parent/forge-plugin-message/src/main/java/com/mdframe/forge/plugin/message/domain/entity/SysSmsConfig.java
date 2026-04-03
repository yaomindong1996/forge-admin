package com.mdframe.forge.plugin.message.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_sms_config")
public class SysSmsConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configId;

    private String supplier;

    private String accessKeyId;

    private String accessKeySecret;

    private String signature;

    private String templateId;

    private Integer weight;

    private Integer retryInterval;

    private Integer maxRetries;

    private Integer maximum;

    private String extraConfig;

    private Integer dailyLimit;

    private Integer minuteLimit;

    private Integer status;

    private Long tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String createBy;

    private String updateBy;

    private String remark;
}

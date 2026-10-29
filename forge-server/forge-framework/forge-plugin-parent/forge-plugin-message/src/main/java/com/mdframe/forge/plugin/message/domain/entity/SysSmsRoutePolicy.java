package com.mdframe.forge.plugin.message.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_sms_route_policy")
public class SysSmsRoutePolicy {

    @TableId
    private Long tenantId;

    private String routeMode;

    private Integer captchaLockPrimary;

    private Integer failoverEnabled;

    private Integer failoverMaxAttempts;

    private Integer pingEnabled;

    private String pingPhones;

    private String pingCron;

    private String pingMode;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;
}

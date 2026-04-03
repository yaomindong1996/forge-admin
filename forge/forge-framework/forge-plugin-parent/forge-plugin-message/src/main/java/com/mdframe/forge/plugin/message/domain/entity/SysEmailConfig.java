package com.mdframe.forge.plugin.message.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_email_config")
public class SysEmailConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configId;

    private String smtpServer;

    private Integer port;

    private String username;

    private String password;

    private String fromAddress;

    private String fromName;

    private Boolean isSsl;

    private Boolean isAuth;

    private Integer retryInterval;

    private Integer maxRetries;

    private Integer status;

    private Long tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String createBy;

    private String updateBy;

    private String remark;
}

package com.mdframe.forge.plugin.message.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_sms_ping_log")
public class SysSmsPingLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String configId;

    private String supplier;

    private String phone;

    private Integer status;

    private String errorMsg;

    private LocalDateTime sendTime;

    private LocalDateTime createTime;
}

package com.mdframe.forge.starter.outbound.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_outbound_whitelist")
public class SysOutboundWhitelist extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String scene;

    private String protocol;

    private String host;

    private Integer portStart;

    private Integer portEnd;

    private Integer allowPrivate;

    private Integer status;

    private String remark;

    @TableLogic
    private Integer delFlag;
}

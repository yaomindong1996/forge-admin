package com.mdframe.forge.plugin.job.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job_api_idempotency")
public class SysJobApiIdempotency extends TenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tokenId;
    private Long jobConfigId;
    private String idempotencyKeyHash;
    private Long executionId;
    private LocalDateTime expiresAt;
    private String remark;

    @TableLogic
    private Integer delFlag;
}

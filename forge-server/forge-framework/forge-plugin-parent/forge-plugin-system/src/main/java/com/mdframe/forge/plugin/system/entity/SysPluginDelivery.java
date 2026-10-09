package com.mdframe.forge.plugin.system.entity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper = true) @TableName("sys_plugin_delivery")
public class SysPluginDelivery extends TenantEntity {
    @TableId(type = IdType.INPUT) private String id;
    private String targetId;
    private String taskId;
    private String releaseId;
    private String previousReleaseId;
    private String unverifiedReleaseId;
    private String action;
    private String status;
    private String requestId;
    private String commandSha256;
    private String workerId;
    @ToString.Exclude private String leaseHash;
    private LocalDateTime leaseExpiresTime;
    private LocalDateTime deadlineTime;
    private String artifactManifestSha256;
    private Boolean cosVerified;
    private Boolean runtimeVerified;
    private String failureCode;
    private String backupReference;
    private Boolean migrationsReviewed;
    private Boolean backwardCompatible;
    @ToString.Exclude private String note;
    @ToString.Exclude private String reconcileNote;
    private Long reconciledBy;
    private LocalDateTime reconciledTime;
    @TableLogic private Integer delFlag;
}

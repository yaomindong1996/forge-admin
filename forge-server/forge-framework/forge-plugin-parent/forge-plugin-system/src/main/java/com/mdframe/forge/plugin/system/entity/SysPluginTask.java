package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 用户可見的审计任务不物理删除；ZIP 绝不随普通实体查询返回。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_plugin_task")
public class SysPluginTask extends TenantEntity {
    @TableId(type = IdType.INPUT)
    private String id;
    private String requestId;
    private String pluginId;
    private String pluginName;
    private String pluginVersion;
    private String operationType;
    private String taskStatus;
    private String activePluginId;
    private Integer revision;
    private String archiveSha256;
    private String fileName;
    private Integer archiveBytes;
    private String runtimeSnapshot;
    private String previewJson;
    private Long confirmedBy;
    private LocalDateTime confirmedTime;
    private Long cancelledBy;
    private LocalDateTime cancelledTime;
    @TableField(select = false)
    private byte[] archiveData;
    @TableLogic
    private Integer delFlag;
}

package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件下载记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file_download_log")
public class SysFileDownloadLog extends TenantEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String username;

    private String fileId;

    private String fileName;

    private Long fileSize;

    /** file 或 export，与字典 sys_file_download_source 一致。 */
    private String source;

    private String requestPath;

    private String clientIp;

    @TableLogic
    private Integer delFlag;
}

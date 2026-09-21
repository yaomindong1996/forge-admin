package com.mdframe.forge.plugin.print.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * sys_print_template_version，租户及审计字段由服务端填充。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_print_template_version")
public class PrintTemplateVersion extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模板 ID。
     */
    private Long templateId;

    /**
     * 跨历史永久唯一版本号。
     */
    private Integer versionNo;

    /**
     * 协议版本。
     */
    private Integer schemaVersion;

    /**
     * 不可变规范化协议。
     */
    private String schemaJson;

    /**
     * 规范化协议 SHA-256。
     */
    private String schemaHash;

    /**
     * 受控文件标识清单，不含签名 URL。
     */
    private String resourceManifest;

    /**
     * 发布时间。
     */
    private LocalDateTime publishTime;

    /**
     * 逻辑删除，不释放版本号。
     */
    @TableLogic
    private Integer delFlag;
}

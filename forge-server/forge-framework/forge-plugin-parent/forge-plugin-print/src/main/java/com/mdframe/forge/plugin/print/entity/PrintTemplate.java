package com.mdframe.forge.plugin.print.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_print_template，租户及审计字段由服务端填充。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_print_template")
public class PrintTemplate extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 应用 ID。
     */
    private Long applicationId;

    /**
     * 应用内模板编码。
     */
    private String templateCode;

    /**
     * 模板名称。
     */
    private String templateName;

    /**
     * LOWCODE/CODE。
     */
    private String sourceType;

    /**
     * 服务端规范化来源。
     */
    private String sourceKey;

    /**
     * 低代码页面。
     */
    private String pageId;

    /**
     * 代码表单标识。
     */
    private String formKey;

    /**
     * 业务对象编码。
     */
    private String objectCode;

    /**
     * 已校验的协议草稿。
     */
    private String draftSchema;

    /**
     * 并发修订号。
     */
    private Long draftRevision;

    /**
     * 设计状态。
     */
    private String designStatus;

    /**
     * 当前发布版本。
     */
    private Long publishedVersionId;

    /**
     * 启停。
     */
    private Integer status;

    /**
     * 删除墓碑：有效为 0，删除写主键。
     */
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}

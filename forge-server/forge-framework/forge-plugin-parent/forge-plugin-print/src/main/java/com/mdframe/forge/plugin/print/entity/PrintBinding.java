package com.mdframe.forge.plugin.print.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_print_binding，租户及审计字段由服务端填充。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_print_binding")
public class PrintBinding extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 应用 ID。
     */
    private Long applicationId;

    /**
     * 独立业务来源 ID；低代码应用来源为空。
     */
    private Long businessSourceId;

    /**
     * 独立业务来源编码。
     */
    private String sourceCode;

    /**
     * 来源类型。
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
     * 对象编码。
     */
    private String objectCode;

    /**
     * 模板 ID。
     */
    private Long templateId;

    /**
     * 独立来源固定的已发布模板版本；应用来源继续从发布快照解析。
     */
    private Long templateVersionId;

    /**
     * 使用场景。
     */
    private String scene;

    /**
     * 是否默认，同应用锁内维护。
     */
    private Boolean isDefault;

    /**
     * 排序。
     */
    private Integer sortOrder;

    /**
     * 启停。
     */
    private Integer status;

    /**
     * 绑定并发修订号。
     */
    private Long bindingRevision;

    /**
     * 删除墓碑。
     */
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}

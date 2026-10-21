package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户已安装内容资产（打印/表单等模板库）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_content_installed_asset")
public class SysContentInstalledAsset extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** print / form / print-component */
    private String kind;

    private String assetKey;

    private String assetName;

    private String assetVersion;

    private String packageJson;

    private String sourceFileName;

    private String remark;

    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}

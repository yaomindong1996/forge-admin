package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.List;

/**
 * 应用页面发布到系统菜单时使用的受控描述。
 * 草稿只保存页面配置；系统资源和角色资源关系始终由发布服务生成。
 */
@Data
public class BusinessApplicationPageMenuDTO {

    private String nodeId;

    private String parentNodeId;

    private String menuName;

    private String path;

    private String component;

    private String perms;

    private String icon;

    private Integer sort;

    private boolean directory;

    private boolean visible;

    /** 继承应用运行入口已授权角色；否则只授予 roleIds。 */
    private boolean inheritRuntimeRoles;

    private List<Long> roleIds;
}

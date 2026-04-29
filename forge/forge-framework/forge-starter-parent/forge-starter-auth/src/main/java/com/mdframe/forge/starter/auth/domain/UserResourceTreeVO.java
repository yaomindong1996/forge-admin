package com.mdframe.forge.starter.auth.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResourceTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String resourceName;
    /** 资源类型（1-目录，2-菜单，3-按钮，4-API接口） */
    private Integer resourceType;
    private Integer sort;
    private String path;
    private String component;
    private Integer isExternal;
    /**
     * 是否公开资源（0-否，1-是，公开资源无需权限验证）
     */
    private Integer isPublic;
    private Integer menuStatus;
    private Integer visible;
    private String perms;
    private String icon;
    private Integer keepAlive;
    private Integer alwaysShow;
    private String redirect;
    /**
     * 备注
     */
    private String remark;
    private String apiMethod;
    private String apiUrl;
    private List<UserResourceTreeVO> children;
}

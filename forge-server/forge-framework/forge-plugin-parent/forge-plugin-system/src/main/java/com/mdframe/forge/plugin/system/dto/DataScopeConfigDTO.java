package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 数据权限规则编辑参数。状态通过专用接口维护，不能随表单覆盖。 */
@Data
public class DataScopeConfigDTO {
    private Long id;
    @NotBlank(message = "请选择适用页面")
    private String resourceCode;
    @NotBlank(message = "请输入显示名称")
    private String resourceName;
    @NotBlank(message = "请完善技术配置中的查询方法")
    private String mapperMethod;
    private String tableAlias;
    private String userIdColumn;
    private String orgIdColumn;
    private String tenantIdColumn;
    private String regionCodeColumn;
    private String userRegionColumn;
    private String userTableAlias;
    private Integer flowRelatedVisible;
    private String flowBusinessType;
    private String recordIdColumn;
    @Size(max = 500, message = "备注不能超过 500 字")
    private String remark;
}

package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

/** 创建流程用户组请求。 */
@Data
public class FlowUserGroupCreateDTO {

    private String groupCode;

    private String groupName;

    private Integer status;

    private String remark;
}

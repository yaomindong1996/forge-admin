package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

/** 更新流程用户组请求。用户组编码创建后不可变，避免已发布 BPMN 引用失效。 */
@Data
public class FlowUserGroupUpdateDTO {

    private Long id;

    private String groupName;

    private Integer status;

    private String remark;
}

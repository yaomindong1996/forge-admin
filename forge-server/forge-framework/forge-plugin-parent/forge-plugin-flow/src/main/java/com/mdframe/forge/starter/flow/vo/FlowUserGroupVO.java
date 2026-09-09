package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 流程用户组列表/详情响应。 */
@Data
public class FlowUserGroupVO {

    private Long id;

    private String groupCode;

    private String groupName;

    private Integer status;

    private String remark;

    private Long memberCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

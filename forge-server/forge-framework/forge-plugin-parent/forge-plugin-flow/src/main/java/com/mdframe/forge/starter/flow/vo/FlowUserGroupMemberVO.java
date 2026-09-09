package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 流程用户组成员响应。 */
@Data
public class FlowUserGroupMemberVO {

    private Long userId;

    private String username;

    private String realName;

    private Integer status;

    private LocalDateTime createTime;
}

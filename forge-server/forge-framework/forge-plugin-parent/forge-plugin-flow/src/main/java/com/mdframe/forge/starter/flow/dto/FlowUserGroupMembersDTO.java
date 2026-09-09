package com.mdframe.forge.starter.flow.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** 批量维护流程用户组成员请求。 */
@Data
public class FlowUserGroupMembersDTO {

    @NotEmpty(message = "用户ID不能为空")
    private List<Long> userIds;
}

package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 流程用户组成员关系。 */
@Data
@TableName("sys_flow_user_group_member")
public class FlowUserGroupMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long groupId;

    private Long userId;

    private Integer status;

    private Long createBy;

    private LocalDateTime createTime;

    private Long createDept;

    private Long updateBy;

    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}

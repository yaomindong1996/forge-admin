package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程用户组。用户组编码可以直接作为 BPMN candidateGroups 的稳定引用。
 */
@Data
@TableName("sys_flow_user_group")
public class FlowUserGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String groupCode;

    private String groupName;

    private Integer status;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long createDept;

    private Long updateBy;

    private LocalDateTime updateTime;

    /** 删除后写入当前行主键，支持同编码在历史墓碑存在时重新创建。 */
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}

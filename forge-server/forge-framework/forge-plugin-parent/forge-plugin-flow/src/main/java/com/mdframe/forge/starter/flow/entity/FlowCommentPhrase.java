package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程常用审批意见。企业意见对当前租户可见，个人意见仅所属用户可见。
 */
@Data
@TableName("sys_flow_comment_phrase")
public class FlowCommentPhrase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Integer ownerType;

    private Long userId;

    private String scene;

    private String content;

    private Integer sortOrder;

    private Integer status;

    private Long createBy;

    private LocalDateTime createTime;

    private Long createDept;

    private Long updateBy;

    private LocalDateTime updateTime;

    /** 删除后写入当前行主键，支持相同意见内容重建。 */
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}

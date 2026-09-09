package com.mdframe.forge.starter.flow.dto;

import com.mdframe.forge.starter.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 流程用户组分页查询条件。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FlowUserGroupQuery extends PageQuery {

    /** 用户组编码或名称的模糊检索词。 */
    private String keyword;

    private String groupCode;

    private String groupName;

    private Integer status;
}

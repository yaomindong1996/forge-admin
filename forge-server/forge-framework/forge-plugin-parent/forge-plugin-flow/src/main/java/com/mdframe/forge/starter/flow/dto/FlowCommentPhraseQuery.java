package com.mdframe.forge.starter.flow.dto;

import com.mdframe.forge.starter.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 常用审批意见分页查询。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FlowCommentPhraseQuery extends PageQuery {

    private String keyword;

    private String scene;

    private Integer ownerType;

    private Integer status;
}

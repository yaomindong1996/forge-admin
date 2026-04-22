package com.mdframe.forge.plugin.ai.session.dto;

import com.mdframe.forge.starter.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiSessionPageQuery extends PageQuery {

    private String keyword;
    private String startTime;
    private String endTime;
    private String status;
    private String agentCode;
}

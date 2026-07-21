package com.mdframe.forge.starter.outbound.domain.dto;

import com.mdframe.forge.starter.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OutboundWhitelistQuery extends PageQuery {

    private String scene;

    private String protocol;

    private String host;

    private Integer status;
}

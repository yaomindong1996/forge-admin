package com.mdframe.forge.plugin.external.dto;

import lombok.Data;

@Data
public class ExternalApiLogQuery {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long tenantId;

    private Long systemId;

    private Long apiId;

    private Integer callStatus;

    private Boolean debugFlag;
}

package com.mdframe.forge.plugin.data.support;

import lombok.Data;

import java.util.List;

@Data
public class DataDatasetAccessQuery {

    private Long tenantId;

    private Long userId;

    private List<Long> roleIds;

    private List<Long> orgIds;

    private List<String> accessLevels;

    private boolean skipAccessFilter;
}

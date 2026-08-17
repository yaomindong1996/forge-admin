package com.mdframe.forge.plugin.system.dto;

import com.mdframe.forge.starter.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 受管缓存定义查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysCachePolicyQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    private String applicationCode;

    private String cacheName;
}

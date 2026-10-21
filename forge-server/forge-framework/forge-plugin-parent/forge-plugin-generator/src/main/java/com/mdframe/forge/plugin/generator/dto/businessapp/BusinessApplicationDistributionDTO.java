package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge 系统首页「我的应用」分发配置。
 * 当前仅支持 WORKBENCH；保存后立即按应用表 portalConfig 生效，不依赖重新发布。
 */
@Data
public class BusinessApplicationDistributionDTO {

    /** 仅 WORKBENCH；缺省按 WORKBENCH 处理。 */
    private String channel;

    /** CURRENT_USER / ROLES。 */
    private String targetType;

    private List<Long> roleIds = new ArrayList<>();

    private Boolean enabled;
}

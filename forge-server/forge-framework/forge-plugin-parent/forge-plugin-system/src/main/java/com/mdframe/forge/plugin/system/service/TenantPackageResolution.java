package com.mdframe.forge.plugin.system.service;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 套餐解析结果：供创建租户开通引擎使用。
 */
@Value
@Builder
public class TenantPackageResolution {

    Long packageId;
    String packageCode;
    String packageName;
    @Builder.Default
    List<String> bootstrapPackages = new ArrayList<>();
    /** 空表示沿用「从 admin 拷贝并排除租户管理」默认策略 */
    @Builder.Default
    Set<Long> resourceIds = new LinkedHashSet<>();
    Integer userLimit;
}

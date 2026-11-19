package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.system.dto.SysTenantPackageDTO;
import com.mdframe.forge.plugin.system.dto.SysTenantPackageQuery;
import com.mdframe.forge.plugin.system.entity.SysTenantPackage;
import com.mdframe.forge.plugin.system.vo.SysTenantPackageVO;

import java.util.List;
import java.util.Set;

public interface ISysTenantPackageService {

    IPage<SysTenantPackage> selectPage(SysTenantPackageQuery query);

    SysTenantPackageVO detail(Long id);

    List<SysTenantPackage> options();

    boolean insertPackage(SysTenantPackageDTO dto);

    boolean updatePackage(SysTenantPackageDTO dto);

    boolean deleteById(Long id);

    boolean deleteByIds(Long[] ids);

    TenantPackageResolution resolve(Long packageId);

    TenantPackageResolution resolveDefault();

    void recordGrant(Long tenantId, TenantPackageResolution resolution, List<String> appliedBootstrap);

    /**
     * 运行时菜单可见白名单：优先开通快照中的 resourceIds，其次当前绑定套餐 RESOURCE 项。
     * 空集表示不额外裁剪（沿用角色授权结果）。不用于 API/按钮鉴权。
     */
    Set<Long> resolveMenuResourceWhitelist(Long tenantId);
}

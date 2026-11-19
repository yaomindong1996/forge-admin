package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.system.dto.SysTenantPackageDTO;
import com.mdframe.forge.plugin.system.dto.SysTenantPackageQuery;
import com.mdframe.forge.plugin.system.entity.SysTenantPackage;
import com.mdframe.forge.plugin.system.vo.SysTenantPackageVO;

import java.util.List;

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
}

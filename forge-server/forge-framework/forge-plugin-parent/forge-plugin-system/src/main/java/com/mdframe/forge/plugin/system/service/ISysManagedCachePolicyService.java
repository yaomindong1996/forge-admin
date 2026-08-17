package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysCachePolicyEditDTO;
import com.mdframe.forge.plugin.system.dto.SysCachePolicyQuery;
import com.mdframe.forge.plugin.system.vo.SysManagedCachePolicyVO;

public interface ISysManagedCachePolicyService {

    Page<SysManagedCachePolicyVO> page(SysCachePolicyQuery query);

    void edit(SysCachePolicyEditDTO dto);

    void reset(String applicationCode, String cacheName);

    void clear(String applicationCode, String cacheName);

    void synchronizePolicies();
}

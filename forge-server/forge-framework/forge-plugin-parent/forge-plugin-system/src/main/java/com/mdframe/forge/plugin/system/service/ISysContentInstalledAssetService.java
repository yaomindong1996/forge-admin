package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.ContentInstalledAssetInstallDTO;
import com.mdframe.forge.plugin.system.vo.ContentInstalledAssetVO;

public interface ISysContentInstalledAssetService {

    Page<ContentInstalledAssetVO> page(String kind, String keyword, int pageNum, int pageSize);

    ContentInstalledAssetVO detail(Long id);

    ContentInstalledAssetVO install(ContentInstalledAssetInstallDTO dto);

    void remove(Long id);
}

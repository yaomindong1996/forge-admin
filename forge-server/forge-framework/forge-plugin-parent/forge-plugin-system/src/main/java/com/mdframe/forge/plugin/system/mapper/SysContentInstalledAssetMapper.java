package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysContentInstalledAsset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysContentInstalledAssetMapper extends BaseMapper<SysContentInstalledAsset> {

    long countByKind(@Param("tenantId") Long tenantId,
                     @Param("kind") String kind,
                     @Param("keyword") String keyword);

    List<SysContentInstalledAsset> selectPageByKind(@Param("tenantId") Long tenantId,
                                                    @Param("kind") String kind,
                                                    @Param("keyword") String keyword,
                                                    @Param("offset") long offset,
                                                    @Param("limit") int limit);

    SysContentInstalledAsset selectByKindAndKey(@Param("tenantId") Long tenantId,
                                                @Param("kind") String kind,
                                                @Param("assetKey") String assetKey);

    SysContentInstalledAsset selectActiveById(@Param("tenantId") Long tenantId,
                                              @Param("id") Long id);
}

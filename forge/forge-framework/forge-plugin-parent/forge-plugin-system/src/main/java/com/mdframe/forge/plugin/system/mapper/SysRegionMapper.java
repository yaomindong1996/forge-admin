package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysRegion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 行政区划Mapper接口
 */
@Mapper
public interface SysRegionMapper extends BaseMapper<SysRegion> {

    /**
     * 查询行政区划列表（带数据权限过滤）
     * 数据权限拦截器会根据 mapperMethod 配置自动追加 region_code 过滤条件
     *
     * @param name     名称（模糊查询，可为空）
     * @param level    行政级别（可为空）
     * @param rootCode 根区域编码（可为空）
     * @return 区划列表
     */
    List<SysRegion> listSysRegion(@Param("name") String name,
                                  @Param("level") Integer level,
                                  @Param("rootCode") String rootCode);

    /**
     * 查询行政区划列表（无数据权限过滤）
     * 不受数据权限拦截器控制，用于管理页面等场景
     *
     * @param name     名称（模糊查询，可为空）
     * @param level    行政级别（可为空）
     * @param rootCode 根区域编码（可为空）
     * @return 区划列表
     */
    List<SysRegion> listSysRegionNoRight(@Param("name") String name,
                                         @Param("level") Integer level,
                                         @Param("rootCode") String rootCode);

    /**
     * 根据区划编码查询区划信息
     *
     * @param code 区划编码
     * @return 区划信息
     */
    SysRegion selectSysRegion(@Param("code") String code);
}
package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.system.dto.SysRegionDTO;
import com.mdframe.forge.plugin.system.entity.SysRegion;
import com.mdframe.forge.plugin.system.vo.SysRegionTreeVO;

import java.util.List;

/**
 * 行政区划Service接口
 */
public interface ISysRegionService extends IService<SysRegion> {

    /**
     * 获取行政区划树形结构
     */
    List<SysRegionTreeVO> selectRegionTree();

    /**
     * 根据code获取详情
     */
    SysRegion selectRegionByCode(String code);

    /**
     * 新增行政区划
     */
    boolean insertRegion(SysRegionDTO dto);

    /**
     * 更新行政区划
     */
    boolean updateRegion(SysRegionDTO dto);

    /**
     * 删除行政区划
     */
    boolean deleteRegionByCode(String code);

    /**
     * 获取子级列表
     */
    List<SysRegion> selectChildrenByParentCode(String parentCode);

    /**
     * 获取子级VO列表（用于懒加载）
     */
    List<SysRegionTreeVO> selectChildrenVOByParentCode(String parentCode);

    /**
     * 搜索行政区划
     */
    List<SysRegion> searchRegionByName(String name);

    /**
     * 构建祖级编码
     */
    String buildAncestors(String code);

    /**
     * 根据rootCode加载完整行政区划树（含虚拟组织）
     *
     * @param rootCode 根区域编码，如150000表示内蒙古
     * @param dataRight 是否启用数据权限过滤
     * @return 完整的区划树（含虚拟组织节点）
     */
    List<SysRegionTreeVO> selectRegionTreeAll(String rootCode, Boolean dataRight);

    /**
     * 刷新行政区划树缓存
     */
    void refreshRegionTreeCache();
}
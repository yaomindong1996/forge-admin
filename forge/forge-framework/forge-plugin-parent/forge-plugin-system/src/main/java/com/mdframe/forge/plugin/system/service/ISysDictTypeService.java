package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.system.dto.SysDictTypeDTO;
import com.mdframe.forge.plugin.system.dto.SysDictTypeQuery;
import com.mdframe.forge.plugin.system.entity.SysDictType;
import com.mdframe.forge.starter.core.domain.PageQuery;

import java.util.List;

/**
 * 字典类型Service接口
 */
public interface ISysDictTypeService extends IService<SysDictType> {

    /**
     * 分页查询字典类型列表
     */
    Page<SysDictType> selectDictTypePage(PageQuery pageQuery, SysDictTypeQuery query);

    /**
     * 查询字典类型列表
     */
    List<SysDictType> selectDictTypeList(SysDictTypeQuery query);

    /**
     * 根据ID查询字典类型详情
     */
    SysDictType selectDictTypeById(Long dictId);

    /**
     * 新增字典类型
     */
    boolean insertDictType(SysDictTypeDTO dto);

    /**
     * 修改字典类型
     */
    boolean updateDictType(SysDictTypeDTO dto);

    /**
     * 删除字典类型
     */
    boolean deleteDictTypeById(Long dictId);

    /**
     * 批量删除字典类型
     */
    boolean deleteDictTypeByIds(Long[] dictIds);
}

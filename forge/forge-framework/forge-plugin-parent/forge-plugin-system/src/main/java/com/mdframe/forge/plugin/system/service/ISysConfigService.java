package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.system.dto.SysConfigDTO;
import com.mdframe.forge.plugin.system.dto.SysConfigQuery;
import com.mdframe.forge.plugin.system.entity.SysConfig;
import com.mdframe.forge.starter.core.domain.PageQuery;

import java.util.List;

/**
 * 系统配置Service接口
 */
public interface ISysConfigService extends IService<SysConfig> {

    /**
     * 分页查询配置列表
     */
    Page<SysConfig> selectConfigPage(PageQuery pageQuery, SysConfigQuery query);

    /**
     * 查询配置列表
     */
    List<SysConfig> selectConfigList(SysConfigQuery query);

    /**
     * 根据配置键名查询配置值
     */
    String selectConfigByKey(String configKey);

    /**
     * 根据ID查询配置详情
     */
    SysConfig selectConfigById(Long configId);

    /**
     * 新增配置
     */
    boolean insertConfig(SysConfigDTO dto);

    /**
     * 修改配置
     */
    boolean updateConfig(SysConfigDTO dto);

    /**
     * 删除配置
     */
    boolean deleteConfigById(Long configId);

    /**
     * 批量删除配置
     */
    boolean deleteConfigByIds(Long[] configIds);
}

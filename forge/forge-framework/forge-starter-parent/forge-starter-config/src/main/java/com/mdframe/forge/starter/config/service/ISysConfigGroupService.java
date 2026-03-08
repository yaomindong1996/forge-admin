package com.mdframe.forge.starter.config.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.starter.config.entity.SysConfigGroup;
import java.util.List;

/**
 * 系统配置分组服务接口
 * 对应数据库表: sys_config_group
 */
public interface ISysConfigGroupService extends IService<SysConfigGroup> {

    /**
     * 根据分组编码查询配置分组
     *
     * @param groupCode 分组编码
     * @return 配置分组信息
     */
    SysConfigGroup selectByGroupCode(String groupCode);

    /**
     * 查询所有启用的配置分组
     *
     * @return 配置分组列表
     */
    List<SysConfigGroup> selectEnabledGroups();

    /**
     * 根据分组编码更新配置值
     *
     * @param groupCode 分组编码
     * @param configValue 配置值
     * @return 更新结果
     */
    boolean updateConfigValueByGroupCode(String groupCode, String configValue);

    /**
     * 根据分组编码启用或禁用配置分组
     *
     * @param groupCode 分组编码
     * @param status 状态(0-禁用 1-启用)
     * @return 更新结果
     */
    boolean updateStatusByGroupCode(String groupCode, Integer status);
}
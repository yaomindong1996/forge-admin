package com.mdframe.forge.starter.social.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.social.domain.dto.SocialPlatformInfo;
import com.mdframe.forge.starter.social.domain.entity.SysSocialConfig;

import java.util.List;

/**
 * 三方登录配置服务接口
 */
public interface ISocialConfigService {

    /**
     * 分页查询配置
     */
    Page<SysSocialConfig> selectConfigPage(Page<SysSocialConfig> page, SysSocialConfig query);

    /**
     * 查询配置列表
     */
    List<SysSocialConfig> selectConfigList(SysSocialConfig query);

    /**
     * 根据ID查询配置
     */
    SysSocialConfig selectConfigById(Long id);

    /**
     * 根据平台和租户查询配置
     */
    SysSocialConfig selectByPlatformAndTenant(String platform, Long tenantId);

    /**
     * 查询租户下所有启用的平台信息
     */
    List<SocialPlatformInfo> selectEnabledPlatforms(Long tenantId);

    /**
     * 新增配置
     */
    boolean insertConfig(SysSocialConfig config);

    /**
     * 修改配置
     */
    boolean updateConfig(SysSocialConfig config);

    /**
     * 删除配置
     */
    boolean deleteConfigById(Long id);

    /**
     * 批量删除配置
     */
    boolean deleteConfigByIds(Long[] ids);

    /**
     * 刷新配置缓存
     */
    void refreshCache();
}

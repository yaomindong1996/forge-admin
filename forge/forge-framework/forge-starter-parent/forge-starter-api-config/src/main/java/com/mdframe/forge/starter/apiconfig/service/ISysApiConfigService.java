package com.mdframe.forge.starter.apiconfig.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigQuery;
import com.mdframe.forge.starter.apiconfig.domain.entity.SysApiConfig;

import java.util.List;

/**
 * API配置服务接口
 */
public interface ISysApiConfigService {

    /**
     * 分页查询API配置列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    Page<SysApiConfig> selectConfigPage(Page<SysApiConfig> page, ApiConfigQuery query);

    /**
     * 查询API配置列表
     *
     * @param query 查询条件
     * @return 配置列表
     */
    List<SysApiConfig> selectConfigList(ApiConfigQuery query);

    /**
     * 根据ID查询配置详情
     *
     * @param id 配置ID
     * @return 配置信息
     */
    SysApiConfig selectConfigById(Long id);

    /**
     * 根据请求路径和方法查询配置
     *
     * @param urlPath  请求路径
     * @param reqMethod 请求方式
     * @return 配置信息
     */
    SysApiConfig selectByUrlAndMethod(String urlPath, String reqMethod);

    /**
     * 新增API配置
     *
     * @param config 配置信息
     * @return 是否成功
     */
    boolean insertConfig(SysApiConfig config);

    /**
     * 修改API配置
     *
     * @param config 配置信息
     * @return 是否成功
     */
    boolean updateConfig(SysApiConfig config);

    /**
     * 删除API配置
     *
     * @param id 配置ID
     * @return 是否成功
     */
    boolean deleteConfigById(Long id);

    /**
     * 批量删除API配置
     *
     * @param ids 配置ID数组
     * @return 是否成功
     */
    boolean deleteConfigByIds(Long[] ids);

    /**
     * 查询所有启用的配置
     *
     * @return 配置列表
     */
    List<SysApiConfig> selectAllEnabled();
}

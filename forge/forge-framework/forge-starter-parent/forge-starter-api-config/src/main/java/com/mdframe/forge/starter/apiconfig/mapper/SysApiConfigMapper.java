package com.mdframe.forge.starter.apiconfig.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.apiconfig.domain.entity.SysApiConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * API配置Mapper接口
 */
@Mapper
public interface SysApiConfigMapper extends BaseMapper<SysApiConfig> {

    /**
     * 根据请求路径和方法查询配置
     *
     * @param urlPath 请求路径
     * @param reqMethod 请求方式
     * @return 配置信息
     */
    SysApiConfig selectByUrlAndMethod(@Param("urlPath") String urlPath, @Param("reqMethod") String reqMethod);

    /**
     * 根据接口编码查询配置
     *
     * @param apiCode 接口编码
     * @return 配置信息
     */
    SysApiConfig selectByApiCode(@Param("apiCode") String apiCode);

    /**
     * 根据模块编码查询配置列表
     *
     * @param moduleCode 模块编码
     * @return 配置列表
     */
    java.util.List<SysApiConfig> selectByModuleCode(@Param("moduleCode") String moduleCode);

    /**
     * 查询所有启用的配置
     *
     * @return 配置列表
     */
    java.util.List<SysApiConfig> selectAllEnabled();
}

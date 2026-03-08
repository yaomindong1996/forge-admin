package com.mdframe.forge.starter.apiconfig.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigQuery;
import com.mdframe.forge.starter.apiconfig.domain.entity.SysApiConfig;
import com.mdframe.forge.starter.apiconfig.mapper.SysApiConfigMapper;
import com.mdframe.forge.starter.apiconfig.service.ISysApiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * API配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysApiConfigServiceImpl implements ISysApiConfigService {

    private final SysApiConfigMapper apiConfigMapper;

    @Override
    public Page<SysApiConfig> selectConfigPage(Page<SysApiConfig> page, ApiConfigQuery query) {
        LambdaQueryWrapper<SysApiConfig> wrapper = buildQueryWrapper(query);
        return apiConfigMapper.selectPage(page, wrapper);
    }

    @Override
    public List<SysApiConfig> selectConfigList(ApiConfigQuery query) {
        LambdaQueryWrapper<SysApiConfig> wrapper = buildQueryWrapper(query);
        return apiConfigMapper.selectList(wrapper);
    }

    @Override
    public SysApiConfig selectConfigById(Long id) {
        return apiConfigMapper.selectById(id);
    }

    @Override
    public SysApiConfig selectByUrlAndMethod(String urlPath, String reqMethod) {
        return apiConfigMapper.selectByUrlAndMethod(urlPath, reqMethod);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertConfig(SysApiConfig config) {
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        int result = apiConfigMapper.insert(config);
        log.info("新增API配置: urlPath={}, reqMethod={}, result={}", config.getUrlPath(), config.getReqMethod(), result > 0);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConfig(SysApiConfig config) {
        config.setUpdateTime(LocalDateTime.now());
        int result = apiConfigMapper.updateById(config);
        log.info("修改API配置: id={}, urlPath={}, reqMethod={}, result={}", config.getId(), config.getUrlPath(), config.getReqMethod(), result > 0);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfigById(Long id) {
        int result = apiConfigMapper.deleteById(id);
        log.info("删除API配置: id={}, result={}", id, result > 0);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfigByIds(Long[] ids) {
        int result = apiConfigMapper.deleteBatchIds(Arrays.asList(ids));
        log.info("批量删除API配置: ids={}, result={}", Arrays.toString(ids), result > 0);
        return result > 0;
    }

    @Override
    public List<SysApiConfig> selectAllEnabled() {
        return apiConfigMapper.selectAllEnabled();
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<SysApiConfig> buildQueryWrapper(ApiConfigQuery query) {
        LambdaQueryWrapper<SysApiConfig> wrapper = new LambdaQueryWrapper<>();

        if (query != null) {
            // 接口名称模糊查询
            if (StringUtils.isNotBlank(query.getApiName())) {
                wrapper.like(SysApiConfig::getApiName, query.getApiName());
            }

            // 接口编码
            if (StringUtils.isNotBlank(query.getApiCode())) {
                wrapper.eq(SysApiConfig::getApiCode, query.getApiCode());
            }

            // 请求方式
            if (StringUtils.isNotBlank(query.getReqMethod())) {
                wrapper.eq(SysApiConfig::getReqMethod, query.getReqMethod());
            }

            // 请求路径模糊查询
            if (StringUtils.isNotBlank(query.getUrlPath())) {
                wrapper.like(SysApiConfig::getUrlPath, query.getUrlPath());
            }

            // 接口版本号
            if (StringUtils.isNotBlank(query.getApiVersion())) {
                wrapper.eq(SysApiConfig::getApiVersion, query.getApiVersion());
            }

            // 所属业务模块
            if (StringUtils.isNotBlank(query.getModuleCode())) {
                wrapper.eq(SysApiConfig::getModuleCode, query.getModuleCode());
            }

            // 所属微服务ID
            if (StringUtils.isNotBlank(query.getServiceId())) {
                wrapper.eq(SysApiConfig::getServiceId, query.getServiceId());
            }

            // 是否需认证/鉴权
            if (query.getAuthFlag() != null) {
                wrapper.eq(SysApiConfig::getAuthFlag, query.getAuthFlag());
            }

            // 是否需报文加解密
            if (query.getEncryptFlag() != null) {
                wrapper.eq(SysApiConfig::getEncryptFlag, query.getEncryptFlag());
            }

            // 是否启用租户隔离
            if (query.getTenantFlag() != null) {
                wrapper.eq(SysApiConfig::getTenantFlag, query.getTenantFlag());
            }

            // 是否开启限流
            if (query.getLimitFlag() != null) {
                wrapper.eq(SysApiConfig::getLimitFlag, query.getLimitFlag());
            }

            // 状态
            if (query.getStatus() != null) {
                wrapper.eq(SysApiConfig::getStatus, query.getStatus());
            }

            // 备注模糊查询
            if (StringUtils.isNotBlank(query.getRemark())) {
                wrapper.like(SysApiConfig::getRemark, query.getRemark());
            }

            // 时间范围查询
            if (StringUtils.isNotBlank(query.getStartTime())) {
                wrapper.ge(SysApiConfig::getCreateTime, query.getStartTime());
            }
            if (StringUtils.isNotBlank(query.getEndTime())) {
                wrapper.le(SysApiConfig::getCreateTime, query.getEndTime());
            }
        }

        // 按ID降序排序
        wrapper.orderByDesc(SysApiConfig::getId);

        return wrapper;
    }
}

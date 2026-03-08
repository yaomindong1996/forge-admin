package com.mdframe.forge.starter.apiconfig.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * API配置信息DTO（用于缓存和返回）
 */
@Data
public class ApiConfigInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 接口名称
     */
    private String apiName;

    /**
     * 接口编码
     */
    private String apiCode;

    /**
     * 请求方式
     */
    private String reqMethod;

    /**
     * 接口请求路径
     */
    private String urlPath;

    /**
     * 接口版本号
     */
    private String apiVersion;

    /**
     * 所属业务模块
     */
    private String moduleCode;

    /**
     * 所属微服务ID
     */
    private String serviceId;

    /**
     * 是否需认证/鉴权
     */
    private Boolean needAuth;

    /**
     * 是否需报文加解密
     */
    private Boolean needEncrypt;

    /**
     * 是否启用租户隔离
     */
    private Boolean needTenant;

    /**
     * 是否开启限流
     */
    private Boolean needLimit;

    /**
     * 需脱敏字段列表
     */
    private List<String> sensitiveFields;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 缓存时间戳
     */
    private Long cacheTime;

    /**
     * 构建缓存Key
     */
    public String buildCacheKey() {
        return urlPath + ":" + reqMethod;
    }

    /**
     * 从实体转换为DTO
     */
    public static ApiConfigInfo fromEntity(com.mdframe.forge.starter.apiconfig.domain.entity.SysApiConfig entity) {
        if (entity == null) {
            return null;
        }
        ApiConfigInfo info = new ApiConfigInfo();
        info.setId(entity.getId());
        info.setApiName(entity.getApiName());
        info.setApiCode(entity.getApiCode());
        info.setReqMethod(entity.getReqMethod());
        info.setUrlPath(entity.getUrlPath());
        info.setApiVersion(entity.getApiVersion());
        info.setModuleCode(entity.getModuleCode());
        info.setServiceId(entity.getServiceId());
        info.setNeedAuth(entity.getAuthFlag() != null && entity.getAuthFlag() == 1);
        info.setNeedEncrypt(entity.getEncryptFlag() != null && entity.getEncryptFlag() == 1);
        info.setNeedTenant(entity.getTenantFlag() != null && entity.getTenantFlag() == 1);
        info.setNeedLimit(entity.getLimitFlag() != null && entity.getLimitFlag() == 1);
        info.setEnabled(entity.getStatus() != null && entity.getStatus() == 1);
        info.setRemark(entity.getRemark());
        info.setCacheTime(System.currentTimeMillis());
        return info;
    }
}

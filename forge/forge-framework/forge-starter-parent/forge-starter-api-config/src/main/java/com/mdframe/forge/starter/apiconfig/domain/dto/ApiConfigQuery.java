package com.mdframe.forge.starter.apiconfig.domain.dto;

import lombok.Data;

/**
 * API配置查询条件类
 */
@Data
public class ApiConfigQuery {

    /**
     * 接口名称（模糊查询）
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
     * 接口请求路径（模糊查询）
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
    private Integer authFlag;

    /**
     * 是否需报文加解密
     */
    private Integer encryptFlag;

    /**
     * 是否启用租户隔离
     */
    private Integer tenantFlag;

    /**
     * 是否开启限流
     */
    private Integer limitFlag;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 备注（模糊查询）
     */
    private String remark;

    /**
     * 开始时间（创建时间）
     */
    private String startTime;

    /**
     * 结束时间（创建时间）
     */
    private String endTime;
}

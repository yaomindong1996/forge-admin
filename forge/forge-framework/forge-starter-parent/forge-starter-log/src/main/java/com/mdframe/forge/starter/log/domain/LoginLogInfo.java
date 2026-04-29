package com.mdframe.forge.starter.log.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志实体
 */
@Data
public class LoginLogInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 租户编号
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录类型（LOGIN/LOGOUT/REGISTER）
     */
    private String loginType;

    /**
     * 登录状态（0-失败，1-成功）
     */
    private Integer loginStatus;

    /**
     * 登录IP
     */
    private String loginIp;

    /**
     * 登录地点
     */
    private String loginLocation;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 登录信息
     */
    private String loginMessage;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;
    
    /**
     * 客户端代码
     */
    private String clientCode;
}

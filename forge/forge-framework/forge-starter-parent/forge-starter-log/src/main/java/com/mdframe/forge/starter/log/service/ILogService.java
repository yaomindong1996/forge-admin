package com.mdframe.forge.starter.log.service;

import com.mdframe.forge.starter.log.domain.LoginLogInfo;
import com.mdframe.forge.starter.log.domain.OperationLogInfo;

/**
 * 日志服务接口
 * 业务模块需要实现此接口来持久化日志
 */
public interface ILogService {

    /**
     * 保存操作日志
     *
     * @param logInfo 操作日志信息
     */
    void saveOperationLog(OperationLogInfo logInfo);

    /**
     * 保存登录日志
     *
     * @param logInfo 登录日志信息
     */
    void saveLoginLog(LoginLogInfo logInfo);
}

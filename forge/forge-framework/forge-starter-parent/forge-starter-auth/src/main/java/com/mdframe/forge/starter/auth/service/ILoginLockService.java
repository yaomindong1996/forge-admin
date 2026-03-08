package com.mdframe.forge.starter.auth.service;

import com.mdframe.forge.starter.core.session.LoginUser;

/**
 * 登录锁定服务接口
 * 用于防止暴力破解密码
 */
public interface ILoginLockService {

    /**
     * 记录登录失败
     *
     * @param loginUser loginUser
     * @return 剩余尝试次数
     */
    int recordLoginFailure(LoginUser loginUser);

    /**
     * 检查账号是否被锁定
     *
     * @param loginUser loginUser
     * @return 是否被锁定
     */
    boolean isLocked(LoginUser loginUser);

    /**
     * 清除登录失败记录（登录成功后调用）
     *
     * @param userId 用户id
     */
    void clearLoginFailure(Long userId);

    /**
     * 获取账号锁定剩余时间（秒）
     *
     * @param loginUser 用户名
     * @return 剩余锁定时间（秒），0表示未锁定
     */
    long getLockRemainingTime(LoginUser loginUser);

    /**
     * 手动解锁账号
     *
     * @param userId 用户id
     */
    void unlock(Long userId);
}

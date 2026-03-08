package com.mdframe.forge.starter.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.auth.service.ILoginLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录锁定服务实现
 * 使用 Redis 记录登录失败次数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLockServiceImpl implements ILoginLockService {

    private final StringRedisTemplate stringRedisTemplate;
    private final AuthProperties authProperties;

    /**
     * 登录失败记录的 Redis key 前缀
     */
    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";

    /**
     * 账号锁定的 Redis key 前缀
     */
    private static final String LOGIN_LOCK_KEY_PREFIX = "login:lock:";

    @Override
    public int recordLoginFailure(LoginUser loginUser) {
        String failKey = LOGIN_FAIL_KEY_PREFIX + loginUser.getUserId();

        // 获取配置
        int maxAttempts = authProperties.getMaxLoginAttempts();
        long lockDuration = authProperties.getLockDuration();

        // 增加失败次数
        Long failCount = stringRedisTemplate.opsForValue().increment(failKey);
        if (failCount == null) {
            failCount = 0L;
        }

        // 第一次失败，设置过期时间（失败记录有效期）
        if (failCount == 1) {
            stringRedisTemplate.expire(failKey, authProperties.getFailRecordExpire(), TimeUnit.MINUTES);
        }

        log.warn("用户 {} 登录失败 {} 次", loginUser.getUsername(), failCount);

        // 达到最大尝试次数，锁定账号
        if (failCount >= maxAttempts) {
            StpUtil.disable(loginUser.getUserId(),lockDuration * 60);
            log.warn("用户 {} 因登录失败次数过多被锁定 {} 分钟", loginUser.getUsername(), lockDuration);
            return 0;
        }

        // 返回剩余尝试次数
        return (int) (maxAttempts - failCount);
    }

    @Override
    public boolean isLocked(LoginUser loginUser) {
        return StpUtil.isDisable(loginUser.getUserId());
    }

    @Override
    public void clearLoginFailure(Long userId) {
        String failKey = LOGIN_FAIL_KEY_PREFIX + userId;
        String lockKey = LOGIN_LOCK_KEY_PREFIX + userId;
        
        stringRedisTemplate.delete(failKey);
        stringRedisTemplate.delete(lockKey);
        
        log.debug("清除用户 {} 的登录失败记录", userId);
    }

    @Override
    public long getLockRemainingTime(LoginUser loginUser) {
        return StpUtil.getDisableTime(loginUser.getUserId());
    }

    @Override
    public void unlock(Long userId) {
        clearLoginFailure(userId);
        log.info("手动解锁用户: {}", userId);
    }
}

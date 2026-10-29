package com.mdframe.forge.plugin.message.sms;

import com.mdframe.forge.starter.message.sms.SmsRouteSequence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 跨实例短信加权轮询序号。
 */
@Slf4j
@Primary
@Component
@ConditionalOnBean(RedissonClient.class)
@RequiredArgsConstructor
public class RedisSmsRouteSequence implements SmsRouteSequence {

    private static final String KEY_PREFIX = "forge:sms:wrr:";

    private final RedissonClient redissonClient;

    @Override
    public long next(long tenantId) {
        try {
            RAtomicLong counter = redissonClient.getAtomicLong(KEY_PREFIX + tenantId);
            return Math.max(0L, counter.incrementAndGet() - 1);
        } catch (Exception ex) {
            log.warn("短信 WRR Redis 计数失败: tenantId={}, err={}", tenantId, ex.getMessage());
            return System.nanoTime();
        }
    }
}

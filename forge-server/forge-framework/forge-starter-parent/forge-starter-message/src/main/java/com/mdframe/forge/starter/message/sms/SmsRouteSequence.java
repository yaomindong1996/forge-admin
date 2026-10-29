package com.mdframe.forge.starter.message.sms;

/**
 * 加权轮询序号源；默认本地，有 Redis 时可替换为跨实例实现。
 */
public interface SmsRouteSequence {

    long next(long tenantId);
}

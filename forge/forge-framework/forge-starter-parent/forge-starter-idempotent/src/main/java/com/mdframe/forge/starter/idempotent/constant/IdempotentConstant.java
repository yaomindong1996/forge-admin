package com.mdframe.forge.starter.idempotent.constant;

/**
 * 幂等常量定义
 * 
 * @description 定义幂等组件使用的常量值
 * @author Forge Framework
 */
public interface IdempotentConstant {
    
    /**
     * 默认幂等键前缀
     */
    String DEFAULT_PREFIX = "idempotent:";
    
    /**
     * 默认幂等键过期时间（秒）
     */
    int DEFAULT_EXPIRE = 600;
    
    /**
     * 默认重复提交提示消息
     */
    String DEFAULT_MESSAGE = "请勿重复提交";
    
    /**
     * 默认结果缓存过期时间（秒）
     */
    int CACHE_EXPIRE_DEFAULT = 3600;
}

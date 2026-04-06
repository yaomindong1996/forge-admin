package com.mdframe.forge.starter.idempotent.annotation;

import com.mdframe.forge.starter.idempotent.constant.IdempotentConstant;
import com.mdframe.forge.starter.idempotent.constant.IdempotentStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等注解
 * 
 * <p>用于标记需要幂等控制的方法或类。支持多种幂等策略，可根据业务场景选择合适的策略。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * // 基础用法：返回缓存结果
 * {@code @Idempotent(key = "'order:' + #orderId")}
 * public Order createOrder(Long orderId) { ... }
 * 
 * // 严格模式：直接拒绝重复请求
 * {@code @Idempotent(key = "'payment:' + #paymentId", strategy = IdempotentStrategy.STRICT)}
 * public PaymentResult pay(PaymentRequest request) { ... }
 * 
 * // Token模式：需要先获取Token
 * {@code @Idempotent(key = "'transfer:' + #requestId", strategy = IdempotentStrategy.TOKEN_REQUIRED)}
 * public TransferResult transfer(TransferRequest request) { ... }
 * </pre>
 * 
 * @author Forge Framework
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等键前缀
     * <p>用于区分不同业务场景的幂等键，避免键冲突</p>
     * 
     * @return 幂等键前缀，默认为 "idempotent:"
     */
    String prefix() default IdempotentConstant.DEFAULT_PREFIX;

    /**
     * 幂等键过期时间（秒）
     * <p>控制幂等键的生命周期，过期后可重复提交</p>
     * 
     * @return 过期时间，默认为 600秒（10分钟）
     */
    int expire() default IdempotentConstant.DEFAULT_EXPIRE;

    /**
     * 幂等键表达式
     * <p>支持SpEL表达式，可引用方法参数。为空时使用默认键生成策略</p>
     * 
     * @return SpEL表达式，默认为空
     */
    String key() default "";

    /**
     * 重复提交提示消息
     * <p>当触发幂等拦截时返回给客户端的提示信息</p>
     * 
     * @return 提示消息，默认为 "请勿重复提交"
     */
    String message() default IdempotentConstant.DEFAULT_MESSAGE;

    /**
     * 成功后是否删除幂等键
     * <p>适用于一次性操作的场景，如支付、转账等</p>
     * 
     * @return 是否删除，默认为 false
     */
    boolean deleteKeyAfterSuccess() default false;

    /**
     * 幂等策略
     * <p>定义如何处理重复请求：</p>
     * <ul>
     *   <li>STRICT: 严格拒绝重复请求，直接抛出异常</li>
     *   <li>RETURN_CACHE: 返回上次缓存的结果（默认策略）</li>
     *   <li>TOKEN_REQUIRED: 必须携带有效的幂等Token</li>
     * </ul>
     * 
     * @return 幂等策略，默认为 RETURN_CACHE
     */
    IdempotentStrategy strategy() default IdempotentStrategy.RETURN_CACHE;

    /**
     * 结果缓存过期时间（秒）
     * <p>仅当 strategy=RETURN_CACHE 且 cacheResult=true 时生效</p>
     * 
     * @return 缓存过期时间，默认为 3600秒（1小时）
     */
    int cacheExpire() default IdempotentConstant.CACHE_EXPIRE_DEFAULT;

    /**
     * 是否缓存执行结果
     * <p>仅当 strategy=RETURN_CACHE 时生效</p>
     * <ul>
     *   <li>true: 缓存执行结果，重复请求返回缓存结果</li>
     *   <li>false: 不缓存结果，仅做防重拦截</li>
     * </ul>
     * 
     * @return 是否缓存结果，默认为 true
     */
    boolean cacheResult() default true;

    /**
     * 是否开启监控指标
     * <p>开启后会记录幂等拦截的次数、耗时等监控指标</p>
     * 
     * @return 是否开启监控，默认为 true
     */
    boolean enableMetrics() default true;
}

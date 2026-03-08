package com.mdframe.forge.starter.cache.service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务接口
 */
public interface ICacheService {

    /**
     * 设置缓存
     *
     * @param key   键
     * @param value 值
     */
    void set(String key, Object value);

    /**
     * 设置缓存，并指定过期时间
     *
     * @param key      键
     * @param value    值
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    void set(String key, Object value, long timeout, TimeUnit timeUnit);

    /**
     * 设置缓存，并指定过期时间
     *
     * @param key      键
     * @param value    值
     * @param duration 过期时间
     */
    void set(String key, Object value, Duration duration);

    /**
     * 获取缓存
     *
     * @param key 键
     * @return 值
     */
    <T> T get(String key);

    /**
     * 获取缓存，指定类型
     *
     * @param key   键
     * @param clazz 值类型
     * @return 值
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 删除缓存
     *
     * @param key 键
     * @return 是否删除成功
     */
    boolean delete(String key);

    /**
     * 批量删除缓存
     *
     * @param keys 键集合
     * @return 删除数量
     */
    long delete(Collection<String> keys);

    /**
     * 判断缓存是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    boolean hasKey(String key);

    /**
     * 设置过期时间
     *
     * @param key      键
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return 是否设置成功
     */
    boolean expire(String key, long timeout, TimeUnit timeUnit);

    /**
     * 获取过期时间
     *
     * @param key 键
     * @return 过期时间（秒），-1表示永不过期，-2表示key不存在
     */
    long getExpire(String key);

    /**
     * 模糊删除缓存
     *
     * @param pattern 匹配模式
     * @return 删除数量
     */
    long deletePattern(String pattern);

    /**
     * 获取Hash中的值
     *
     * @param key     键
     * @param hashKey Hash键
     * @return 值
     */
    <T> T hGet(String key, String hashKey);

    /**
     * 设置Hash中的值
     *
     * @param key     键
     * @param hashKey Hash键
     * @param value   值
     */
    void hSet(String key, String hashKey, Object value);

    /**
     * 删除Hash中的值
     *
     * @param key      键
     * @param hashKeys Hash键
     * @return 删除数量
     */
    long hDelete(String key, Object... hashKeys);

    /**
     * 判断Hash中是否存在该键
     *
     * @param key     键
     * @param hashKey Hash键
     * @return 是否存在
     */
    boolean hHasKey(String key, String hashKey);

    /**
     * 获取Hash的所有键值对
     *
     * @param key 键
     * @return 键值对Map
     */
    <T> Map<String, T> hGetAll(String key);

    /**
     * 设置Set集合
     *
     * @param key    键
     * @param values 值
     * @return 添加数量
     */
    long sAdd(String key, Object... values);

    /**
     * 获取Set集合
     *
     * @param key 键
     * @return Set集合
     */
    <T> Set<T> sMembers(String key);

    /**
     * 判断Set中是否存在该值
     *
     * @param key   键
     * @param value 值
     * @return 是否存在
     */
    boolean sIsMember(String key, Object value);

    /**
     * 移除Set中的值
     *
     * @param key    键
     * @param values 值
     * @return 移除数量
     */
    long sRemove(String key, Object... values);

    /**
     * 递增
     *
     * @param key   键
     * @param delta 增量
     * @return 递增后的值
     */
    long increment(String key, long delta);

    /**
     * 递减
     *
     * @param key   键
     * @param delta 减量
     * @return 递减后的值
     */
    long decrement(String key, long delta);

    /**
     * 根据模式获取缓存键列表(支持分页)
     *
     * @param pattern  匹配模式(如"user:*")
     * @param page     页码(从1开始)
     * @param pageSize 每页大小
     * @return 缓存键列表
     */
    java.util.List<String> getKeysByPattern(String pattern, int page, int pageSize);

    /**
     * 获取缓存键总数
     *
     * @param pattern 匹配模式
     * @return 总数
     */
    long countKeysByPattern(String pattern);

    /**
     * 获取缓存值的类型
     *
     * @param key 键
     * @return 类型(STRING/HASH/SET/LIST等)
     */
    String getType(String key);

    /**
     * 获取缓存详细信息
     *
     * @param key 键
     * @return 缓存信息(包含键、值、类型、过期时间等)
     */
    Map<String, Object> getCacheInfo(String key);

    /**
     * 根据模式获取所有匹配的缓存键
     *
     * @param pattern 匹配模式(如"user:*")
     * @return 匹配的所有键列表
     */
    List<String> keys(String pattern);

    /**
     * 获取Redis服务器信息
     *
     * @return 服务器信息Map
     */
    Map<String, Object> getServerInfo();

    /**
     * 获取Redis内存使用情况
     *
     * @return 内存使用信息
     */
    Map<String, Object> getMemoryInfo();

    /**
     * 获取Redis统计信息（QPS、连接数等）
     *
     * @return 统计信息
     */
    Map<String, Object> getStatsInfo();
}

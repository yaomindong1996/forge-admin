# 缓存模块

基于 Spring Cache 和 Redis 的缓存模块。

## 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-cache</artifactId>
</dependency>
```

## 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0

forge:
  cache:
    type: redis
    default-ttl: 3600
```

## 使用示例

### 注解方式

```java
// 缓存查询结果
@Cacheable(value = "user", key = "#userId")
public User getById(Long userId) {
    return userMapper.selectById(userId);
}

// 更新缓存
@CachePut(value = "user", key = "#user.userId")
public User update(User user) {
    userMapper.updateById(user);
    return user;
}

// 删除缓存
@CacheEvict(value = "user", key = "#userId")
public void delete(Long userId) {
    userMapper.deleteById(userId);
}

// 删除所有缓存
@CacheEvict(value = "user", allEntries = true)
public void clearAll() {
    // ...
}
```

### 编程式缓存

```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

// 设置缓存
redisTemplate.opsForValue().set("user:1", user, 1, TimeUnit.HOURS);

// 获取缓存
User user = (User) redisTemplate.opsForValue().get("user:1");

// 删除缓存
redisTemplate.delete("user:1");

// 批量删除
redisTemplate.delete(Arrays.asList("user:1", "user:2"));
```

## 缓存注解

| 注解 | 说明 |
|------|------|
| @Cacheable | 缓存查询结果 |
| @CachePut | 更新缓存 |
| @CacheEvict | 删除缓存 |
| @Caching | 组合多个缓存操作 |

## 缓存配置

```java
@Configuration
public class CacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

## 注意事项

1. 缓存键建议使用统一的命名规范
2. 注意缓存穿透、击穿、雪崩问题
3. 更新数据时及时更新或删除缓存
4. 合理设置过期时间
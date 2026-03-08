# 数据库配置中心使用示例

## 1. 配置数据库连接（application.yml）

```yaml
# 配置数据源（用于读取配置的数据库）
config:
  datasource:
    url: jdbc:mysql://localhost:3306/forge_config?useUnicode=true&characterEncoding=utf8
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver

# 配置刷新设置
forge:
  config:
    # 是否启用自动刷新
    auto-refresh: true
    # 刷新间隔（毫秒），默认30秒
    refresh-interval: 30000
    # 是否启用刷新接口
    enable-refresh-endpoint: true
```

## 2. 创建配置表

```sql
CREATE TABLE `config_properties` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `key` VARCHAR(200) NOT NULL COMMENT '配置键',
  `value` TEXT COMMENT '配置值',
  `description` VARCHAR(500) COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置属性表';

-- 插入示例数据
INSERT INTO config_properties (`key`, `value`, `description`) VALUES
('app.name', 'MyApp', '应用名称'),
('app.version', '1.0.0', '应用版本'),
('feature.enabled', 'true', '功能开关');
```

## 3. 使用@RefreshScope注解

```java
@Component
@RefreshScope  // 标记此Bean支持配置动态刷新
public class MyConfig {
    
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.version}")
    private String appVersion;
    
    @Value("${feature.enabled:false}")
    private boolean featureEnabled;
    
    public String getAppName() {
        return appName;
    }
    
    public boolean isFeatureEnabled() {
        return featureEnabled;
    }
}
```

## 4. 使用@ConfigurationProperties

```java
@Component
@RefreshScope  // 支持动态刷新
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private String name;
    private String version;
    private boolean featureEnabled;
}
```

## 5. 监听配置变更事件

```java
@Component
@Slf4j
public class ConfigChangeEventListener {
    
    @EventListener
    public void onConfigRefresh(ConfigRefreshEvent event) {
        Map<String, String> changedProperties = event.getChangedProperties();
        log.info("配置已刷新，变更项: {}", changedProperties);
        
        // 执行自定义逻辑
        changedProperties.forEach((key, newValue) -> {
            log.info("配置 {} 变更为: {}", key, newValue);
        });
    }
}
```

## 6. 手动刷新配置

### 6.1 通过REST接口刷新

```bash
curl -X POST http://localhost:8080/api/config/refresh
```

### 6.2 编程方式刷新

```java
@Service
public class MyService {
    
    @Autowired
    private ConfigRefresher configRefresher;
    
    public void refreshConfig() {
        boolean refreshed = configRefresher.refresh();
        if (refreshed) {
            System.out.println("配置刷新成功");
        }
    }
}
```

## 7. 刷新机制说明

### 自动刷新
- 默认每30秒自动检查数据库配置变更
- 可通过 `forge.config.refresh-interval` 配置刷新间隔
- 通过 `forge.config.auto-refresh=false` 关闭自动刷新

### 手动刷新
- 调用REST接口: `POST /api/config/refresh`
- 注入 `ConfigRefresher` 编程式刷新

### 刷新流程
1. 从数据库重新加载配置
2. 比较配置是否变化
3. 更新 PropertySource
4. 刷新 @RefreshScope Bean（销毁旧实例，下次获取时重新创建）
5. 发布 ConfigRefreshEvent 事件
6. 发布 EnvironmentChangeEvent 事件（兼容Spring Cloud）

## 8. 注意事项

1. **@RefreshScope 必须标记在Bean上**
   - 直接在Controller/Service中使用@Value不会自动刷新
   - 需要将配置封装到带@RefreshScope的Bean中

2. **Singleton Bean不会刷新**
   - 默认的单例Bean在初始化后不会重新创建
   - 必须使用@RefreshScope或手动处理

3. **性能考虑**
   - 刷新间隔不宜过短，避免频繁查询数据库
   - 建议30秒以上

4. **事务安全**
   - 配置刷新是同步操作
   - 在配置刷新期间，可能会有短暂的不一致

## 9. 完整使用示例

```java
// 1. 配置类
@Component
@RefreshScope
@ConfigurationProperties(prefix = "business")
@Data
public class BusinessConfig {
    private String apiUrl;
    private Integer timeout;
    private Boolean enableCache;
}

// 2. 使用配置
@Service
@RequiredArgsConstructor
public class BusinessService {
    
    private final BusinessConfig businessConfig;
    
    public void doSomething() {
        String apiUrl = businessConfig.getApiUrl();
        // 数据库中修改配置后，这里会自动获取新值
        System.out.println("当前API地址: " + apiUrl);
    }
}

// 3. 监听配置变更
@Component
@Slf4j
public class BusinessConfigListener {
    
    @EventListener
    public void onConfigChange(ConfigRefreshEvent event) {
        Map<String, String> changed = event.getChangedProperties();
        if (changed.containsKey("business.apiUrl")) {
            log.info("API地址已变更: {}", changed.get("business.apiUrl"));
            // 执行相关清理或初始化操作
        }
    }
}
```

## 10. 与Nacos的对比

| 功能 | 数据库配置中心 | Nacos |
|------|--------------|-------|
| 配置存储 | 数据库 | Nacos服务器 |
| 刷新方式 | 定时轮询 | 长轮询/推送 |
| 实时性 | 30秒延迟（可配置） | 秒级 |
| 部署复杂度 | 低（仅需数据库） | 中（需独立部署Nacos） |
| 适用场景 | 中小型项目 | 大型微服务 |
| 配置管理UI | 需自行开发 | 自带控制台 |

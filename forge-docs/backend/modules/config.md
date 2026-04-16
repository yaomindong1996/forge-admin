# 动态配置中心（forge-starter-config）

基于数据库的动态配置中心，支持运行时配置刷新，无需重启服务。

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-config</artifactId>
</dependency>
```

### 2. 使用 `@RefreshScope`

```java
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app")
@Data
public class AppConfigExample {
    private String name;
    private String version;
    private Boolean featureEnabled;
}
```

## 核心功能

### 内置配置管理

配置中心内置以下配置类别，支持通过 REST API 管理：

| 配置类 | 说明 | 主要字段 |
|--------|------|---------|
| `LoginConfig` | 登录配置 | `enableCaptcha`, `captchaType`, `enableRememberMe`, `rememberMeDays`, `enableIpLimit`, `ipWhitelist` |
| `SecurityConfig` | 安全策略 | 包含 `SaTokenConfig`（token 超时、并发策略）和 `PasswordPolicyConfig`（密码强度、过期策略） |
| `SystemConfig` | 系统配置 | `systemName`, `systemVersion`, `copyright`, `enableWatermark` |
| `WatermarkConfig` | 水印配置 | `enable`, `content`, `opacity`, `fontSize`, `fontColor`, `rotate` |

### REST API

配置管理接口位于 `/api/config/manage`：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/login` | 获取登录配置 |
| PUT | `/login` | 更新登录配置 |
| GET | `/watermark` | 获取水印配置 |
| PUT | `/watermark` | 更新水印配置 |
| GET | `/security` | 获取安全配置 |
| PUT | `/security` | 更新安全配置 |
| GET | `/system` | 获取系统配置 |
| PUT | `/system` | 更新系统配置 |
| POST | `/refresh` | 手动触发配置刷新 |

### 数据库表

| 表名 | 说明 |
|------|------|
| `sys_config_group` | 配置分组表，存储 JSON 格式的配置 |
| `sys_config` | 配置项表（key-value 格式） |

启动时 `ConfigSyncService` 会自动将 `sys_config_group` 中的配置同步到 `sys_config` 表。

## 配置刷新机制

### 架构

```
启动时: DbPropertySourcePostProcessor → 加载 sys_config → DbPropertySource
运行时: ConfigRefresher.refresh() → 更新 DbPropertySource → refreshScope.refreshAll() → 发布 ConfigRefreshEvent
```

### 自定义配置属性

```java
// 1. 定义配置类
@Component
@RefreshScope
@ConfigurationProperties(prefix = "my.module")
@Data
public class MyModuleConfig {
    private Boolean enabled;
    private Integer timeout;
}

// 2. 注册到 ConfigManagerService
// 在 ConfigManagerService 中添加 get/save 方法

// 3. 注册到 ConfigConverter
// 在 ConfigConverter 中添加 convert 方法，将配置转为 key-value 格式存入 sys_config

// 4. 在 SysConfigGroup 中维护配置组
```

### 配置刷新事件

配置刷新后会发布以下事件：

- `ConfigRefreshEvent` — 携带旧配置和新配置
- `EnvironmentChangeEvent` — Spring Environment 变更事件

## 配置数据源

数据库连接信息可通过配置指定：

```yaml
config:
  datasource:
    url: jdbc:mysql://localhost:3306/forge_admin
    username: root
    password: your_password
    driverClassName: com.mysql.cj.jdbc.Driver
```

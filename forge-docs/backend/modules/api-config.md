# API 配置管理（forge-starter-api-config）

API 端点自动注册与动态配置模块，支持启动时扫描 Controller 并自动注册到数据库，提供两级缓存和实时刷新能力。

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-api-config</artifactId>
</dependency>
```

### 2. 自动注册

模块启动时会自动扫描所有 Controller，将端点注册到 `sys_api_config` 表。

## 核心功能

### 自动注册

`ApiConfigAutoRegistrar` 在启动时通过 `RequestMappingHandlerMapping` 扫描所有 REST 端点，并解析以下标志位：

| 标志位 | 说明 | 解析方式 |
|--------|------|---------|
| `authFlag` | 是否需要认证 | 根据 `@ApiPermissionIgnore` / `@IgnoreTenant` 等注解推断 |
| `encryptFlag` | 是否需要加解密 | 根据 `@ApiEncrypt` / `@ApiDecrypt` 注解推断 |
| `tenantFlag` | 是否启用租户隔离 | 根据 `@IgnoreTenant` 注解推断 |
| `limitFlag` | 是否启用限流 | 根据 `@RateLimiter` 等注解推断 |

注册时会自动对比数据库中已存在的配置，增量更新。

### 两级缓存

API 配置查询采用 L1 + L2 两级缓存：

| 级别 | 存储 | 最大容量 | 过期时间 |
|------|------|---------|---------|
| L1 | Caffeine（本地） | 1000 | 10 分钟 |
| L2 | Redis | - | 30 分钟 |

**查询优先级**：L1 → L2 → 数据库

### 缓存预热

启动时自动预热所有已启用的 API 配置到缓存：

```yaml
forge:
  api-config:
    cache-warm-up: true
```

### 实时刷新

支持通过 Spring Event 实时刷新 API 配置：

```
修改数据库 → 发布 ApiConfigRefreshEvent → 清除缓存 → 重新加载
```

## 数据库表

### `sys_api_config`

| 字段 | 说明 |
|------|------|
| `api_name` | API 名称（如 "查询用户列表"） |
| `api_code` | API 编码（唯一标识） |
| `req_method` | 请求方法（GET/POST/PUT/DELETE） |
| `url_path` | 请求路径（如 "/api/user/page"） |
| `auth_flag` | 是否需要认证（0/1） |
| `encrypt_flag` | 是否加解密（0/1） |
| `tenant_flag` | 是否租户隔离（0/1） |
| `limit_flag` | 是否限流（0/1） |
| `sensitive_fields` | 敏感字段列表（JSON） |
| `status` | 状态（0=禁用，1=启用） |
| `module_code` | 所属模块编码 |
| `service_id` | 所属服务 ID |

## REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/api-config/page` | 分页查询 API 配置 |
| GET | `/api/api-config/{id}` | 获取 API 配置详情 |
| PUT | `/api/api-config` | 更新 API 配置 |
| POST | `/api/api-config/refresh` | 手动触发配置刷新 |
| POST | `/api/api-config/cache/clear` | 清除所有缓存 |
| GET | `/api/api-config/cache/stats` | 获取缓存统计信息 |

## 配置项

```yaml
forge:
  api-config:
    enabled: true
    auto-register: true
    cache-warm-up: true
    scan-packages: []  # 自定义扫描包路径（为空则扫描全部）
    cache:
      local:
        max-size: 1000
        expire-minutes: 10
      redis:
        enabled: true
        expire-seconds: 1800
        key-prefix: "api:config:"
```

## 核心组件

| 组件 | 说明 |
|------|------|
| `ApiConfigScanner` | 扫描 `RequestMappingHandlerMapping` 发现 API 端点 |
| `ApiConfigAutoRegistrar` | 自动注册 API 到数据库 |
| `IApiConfigManager` | API 配置管理接口 |
| `ApiConfigManagerImpl` | 实现类（支持数据库 > 注解 > 默认的优先级查询） |
| `ApiConfigContextHolder` | ThreadLocal 上下文持有者 |
| `ApiConfigRefreshListener` | 监听配置刷新事件 |

## 使用场景

### 运行时权限判断

```java
@Autowired
private IApiConfigManager apiConfigManager;

public boolean needAuth(String urlPath, String method) {
    return apiConfigManager.needAuth(urlPath, method);
}
```

### 获取所有已启用的配置

```java
List<SysApiConfig> configs = apiConfigManager.getAllEnabledConfigs();
```

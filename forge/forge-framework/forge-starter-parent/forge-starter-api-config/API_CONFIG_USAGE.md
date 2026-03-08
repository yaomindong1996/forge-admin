# API配置管理组件使用指南

## 1. 组件简介

API配置管理组件是一个通用的接口配置管理组件，用于统一管理系统的接口鉴权、加密、租户隔离等配置。该组件支持：

- **启动时自动注册**：扫描所有Controller中的接口，自动注册到数据库
- **配置优先级**：数据库配置 > 代码注解配置 > 系统默认值
- **高性能缓存**：L1（Caffeine）+ L2（Redis）两级缓存
- **实时刷新**：通过Spring Event机制实现配置变更的实时通知

## 2. 快速开始

### 2.1 添加依赖

在项目的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-api-config</artifactId>
    <version>${forge.version}</version>
</dependency>
```

### 2.2 配置文件

在 `application.yml` 中添加配置：

```yaml
forge:
  api-config:
    # 是否启用API配置管理
    enabled: true
    # 是否自动注册接口配置
    auto-register: true
    # 是否预热缓存
    cache-warm-up: true
    # 缓存配置
    cache:
      local:
        max-size: 1000
        expire-minutes: 10
      redis:
        enabled: true
        expire-seconds: 1800
        key-prefix: "api:config:"
```

### 2.3 执行SQL脚本

执行以下SQL脚本创建表和菜单：

```sql
-- 创建表和字典
source: sql/api_config_tables.sql

-- 创建菜单
source: sql/api_config_menu.sql
```

## 3. 核心功能

### 3.1 启动时自动注册

组件会在Spring容器启动完成后，自动扫描所有Controller中的接口：

1. 解析 `@RequestMapping` 注解，获取接口路径和方法
2. 解析方法上的注解（`@ApiDecrypt`, `@ApiEncrypt`, `@ApiPermissionIgnore`, `@IgnoreTenant`）
3. 与数据库 `sys_api_config` 表比对
4. 如果数据库中不存在该接口，则根据注解默认值插入新记录
5. 如果数据库中已存在，则跳过（以数据库为准）

### 3.2 配置优先级

组件按照以下优先级决定接口行为：

```
数据库配置 > 代码注解配置 > 系统默认值
```

**示例场景**：
- 代码中加了 `@ApiPermissionIgnore`，但数据库里 `auth_flag` 被改为 1，则最终结果是"需要鉴权"
- 数据库配置可以覆盖代码中的注解配置

### 3.3 两级缓存架构

组件使用两级缓存提高性能：

```
请求 → L1缓存(Caffeine) → L2缓存(Redis) → 数据库
```

**缓存配置**：
- L1缓存：Caffeine本地缓存，默认最大1000条，过期时间10分钟
- L2缓存：Redis缓存，默认过期时间30分钟

### 3.4 缓存刷新机制

当通过管理后台更新数据库配置时，会自动触发缓存刷新：

1. Service层发布 `ApiConfigRefreshEvent` 事件
2. `ApiConfigRefreshListener` 监听事件
3. 刷新L1和L2缓存
4. 所有节点同步刷新

## 4. 使用示例

### 4.1 在代码中使用

#### 4.1.1 获取接口配置

```java
@Autowired
private IApiConfigManager apiConfigManager;

public void someMethod() {
    // 根据请求路径和方法获取配置
    ApiConfigInfo config = apiConfigManager.getApiConfig("/api/user/info", "GET");

    if (config != null) {
        boolean needAuth = config.getNeedAuth();
        boolean needEncrypt = config.getNeedEncrypt();
        boolean needTenant = config.getNeedTenant();
        // ... 根据配置执行相应逻辑
    }
}
```

#### 4.1.2 判断接口行为

```java
// 判断是否需要鉴权
if (apiConfigManager.needAuth("/api/user/info", "GET")) {
    // 执行鉴权逻辑
}

// 判断是否需要加密
if (apiConfigManager.needEncrypt("/api/user/info", "GET")) {
    // 执行加密逻辑
}

// 判断是否需要租户隔离
if (apiConfigManager.needTenant("/api/user/info", "GET")) {
    // 执行租户隔离逻辑
}
```

#### 4.1.3 使用上下文

在拦截器中，组件会自动将当前接口配置存入 `ApiConfigContextHolder`：

```java
// 在业务代码中直接使用
if (ApiConfigContextHolder.needAuth()) {
    // 需要鉴权
}

if (ApiConfigContextHolder.needEncrypt()) {
    // 需要加密
}
```

### 4.2 管理后台接口

组件提供以下管理接口：

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| /system/apiConfig/page | GET | 分页查询 |
| /system/apiConfig/list | GET | 列表查询 |
| /system/apiConfig/getById | GET | 根据ID查询 |
| /system/apiConfig/add | POST | 新增 |
| /system/apiConfig/edit | POST | 修改 |
| /system/apiConfig/remove | POST | 删除 |
| /system/apiConfig/removeBatch | POST | 批量删除 |

### 4.3 缓存管理接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| /apiConfig/refresh | POST | 刷新所有缓存 |
| /apiConfig/refreshSingle | POST | 刷新单个接口缓存 |
| /apiConfig/clearCache | POST | 清空所有缓存 |
| /apiConfig/cacheStats | GET | 获取缓存统计信息 |
| /apiConfig/getAllEnabled | GET | 获取所有启用的配置 |

## 5. 注解兼容

组件兼容以下现有注解：

| 注解 | 对应字段 | 说明 |
|-----|---------|------|
| @ApiDecrypt | encryptFlag | 自动注册时设置 encryptFlag=1 |
| @ApiEncrypt | encryptFlag | 自动注册时设置 encryptFlag=1 |
| @ApiPermissionIgnore | authFlag | 自动注册时设置 authFlag=0 |
| @OperationLog | - | 记录日志信息，用于生成接口名称 |
| @IgnoreTenant | tenantFlag | 自动注册时设置 tenantFlag=0 |

## 6. 配置说明

### 6.1 数据库配置字段

| 字段 | 类型 | 说明 | 默认值 |
|-----|------|------|--------|
| auth_flag | tinyint | 是否需认证/鉴权 | 1 |
| encrypt_flag | tinyint | 是否需报文加解密 | 0 |
| tenant_flag | tinyint | 是否启用租户隔离 | 1 |
| limit_flag | tinyint | 是否开启限流 | 0 |
| status | tinyint | 状态 | 1 |

### 6.2 配置属性

| 配置项 | 类型 | 说明 | 默认值 |
|-------|------|------|--------|
| forge.api-config.enabled | boolean | 是否启用 | true |
| forge.api-config.auto-register | boolean | 是否自动注册 | true |
| forge.api-config.cache-warm-up | boolean | 是否预热缓存 | true |
| forge.api-config.cache.local.max-size | long | L1缓存最大容量 | 1000 |
| forge.api-config.cache.local.expire-minutes | long | L1缓存过期时间 | 10 |
| forge.api-config.cache.redis.enabled | boolean | 是否启用Redis | true |
| forge.api-config.cache.redis.expire-seconds | long | L2缓存过期时间 | 1800 |

## 7. 性能优化建议

1. **缓存命中率**：建议L1缓存命中率保持在95%以上
2. **缓存预热**：生产环境建议开启缓存预热
3. **批量刷新**：避免频繁单条刷新，尽量使用批量操作
4. **监控告警**：建议对缓存命中率进行监控和告警

## 8. 故障排查

### 8.1 接口配置未生效

1. 检查数据库中是否存在该接口配置
2. 检查配置的 `status` 字段是否为 1
3. 查看缓存统计信息：`GET /apiConfig/cacheStats`
4. 手动刷新缓存：`POST /apiConfig/refresh`

### 8.2 自动注册未执行

1. 检查 `forge.api-config.auto-register` 是否为 true
2. 查看启动日志，搜索 "API配置自动注册"
3. 检查Controller是否被Spring扫描到

### 8.3 缓存刷新不及时

1. 检查事件监听器是否正常加载
2. 检查Service层是否正确发布事件
3. 查看日志中的事件发布和接收情况

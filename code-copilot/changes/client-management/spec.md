# Sa-Token客户端管理功能 Spec

## 变更概述

**变更名称**: client-management  
**变更类型**: 新功能开发  
**优先级**: 高  
**影响范围**: 认证模块（forge-starter-auth）、系统插件（forge-plugin-system）  
**预计工作量**: 2-3人日  

---

## 一、背景与动机

### 1.1 业务背景

Forge Admin作为企业级后台管理系统，需要支持多终端访问（PC端、APP端、H5端、微信小程序等）。不同客户端具有不同的业务场景和安全要求：

- **PC端**：办公场景，session模式，较短token有效期（如8小时）
- **APP端**：移动场景，长期登录，较长token有效期（如30天）
- **H5端**：临时访问，中等token有效期（如7天）
- **微信小程序**：特殊认证方式，长期token有效期

当前系统缺少客户端级别的配置管理能力，无法针对不同客户端设置差异化的Token策略、安全策略和业务限制。

### 1.2 当前问题

| 问题类别 | 具体问题 | 业务影响 |
|---------|---------|---------|
| **配置缺失** | 无客户端配置表，无法按客户端设置token过期时间 | 所有客户端使用统一配置，无法满足差异化需求 |
| **安全不足** | 无AppId/AppSecret管理，无法验证客户端身份 | 无法防范伪造客户端请求 |
| **管理缺失** | 无法统计各客户端在线用户数 | 缺少运营数据和监控能力 |
| **灵活性不足** | 无法为不同客户端设置不同的认证方式限制 | 安全策略一刀切，不够灵活 |
| **并发控制缺失** | 无法控制各客户端的并发登录策略 | 移动端和PC端应该有不同的并发策略 |

**代码现状分析**（基于代码调研）：

| 组件 | 现状 | 文件路径 |
|------|------|---------|
| Sa-Token配置 | 从数据库动态加载，但仅全局配置 | `SaTokenConfigLoader.java:38-69` |
| 登录流程 | 已支持userClient字段，但未应用客户端配置 | `SystemAuthServiceImpl.java:48-91` |
| 认证策略 | 支持按客户端类型选择策略 | `AuthStrategyFactory.java:34-60` |
| 配置模型 | 仅支持全局SaTokenConfig | `SecurityConfig.java:24-70` |

### 1.3 优化目标

1. **客户端配置管理**：支持客户端CRUD、AppId/AppSecret管理、Token策略配置
2. **登录流程增强**：登录时动态应用客户端级别的Token配置
3. **在线用户管理**：按客户端统计在线用户、踢出客户端用户
4. **安全增强**：客户端身份验证、IP白名单、加密传输配置
5. **运营监控**：客户端级别的在线统计、登录日志分析

---

## 二、需求规格

### 2.1 核心功能需求

#### 功能1：客户端配置管理

**需求描述**：提供客户端配置的增删改查功能，支持配置AppId、AppSecret、Token策略、安全策略等。

**业务价值**：
- 支持按客户端设置差异化Token策略
- 提供客户端身份验证能力（AppId/AppSecret）
- 增强安全控制（IP白名单、加密传输）

**功能规格**：
- 客户端信息管理：编码、名称、描述、状态
- Token策略配置：有效期、活跃超时、并发登录、共享token
- 安全策略配置：IP白名单、加密传输、加密算法
- 业务限制配置：最大用户数、最大在线数、支持认证方式

#### 功能2：登录流程增强

**需求描述**：登录时根据客户端类型动态应用对应的Token配置，支持客户端级别的并发登录控制。

**业务价值**：
- APP端可长期登录（30天），PC端session模式（8小时）
- 移动端允许并发登录，PC端单点登录
- 精细化token管理，提升用户体验

**功能规格**：
- LoginRequest增加appId字段（可选）
- 登录时根据userClient查询客户端配置
- 动态设置Sa-Token的timeout、activeTimeout、isConcurrent等参数
- 使用SaLoginModel.setDevice()标记设备类型

#### 功能3：在线用户管理增强

**需求描述**：支持按客户端类型查询在线用户、踢出指定客户端的用户。

**业务价值**：
- 运营监控：统计各客户端在线用户数
- 安全管控：踢出可疑客户端的所有用户
- 用户体验：支持按设备类型管理会话

**功能规格**：
- 查询指定客户端的在线用户列表
- 踢出指定用户在指定客户端的会话
- 按客户端统计在线用户数量

#### 功能4：客户端验证（可选）

**需求描述**：提供客户端AppId/AppSecret验证机制，防止伪造客户端请求。

**业务价值**：
- 防范恶意伪造客户端
- 增强系统安全性
- 支持客户端级别的API鉴权

**功能规格**：
- 登录时可选验证AppId（通过配置开关控制）
- AppSecret加密存储，前端脱敏展示
- 提供AppId/AppSecret查看接口（管理员权限）

---

## 三、技术设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│          Client Management Architecture                   │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ SysClient    │  │ ClientConfig │  │ ClientService │  │
│  │ Entity       │  │ Loader       │  │ Impl          │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ AuthService  │  │ OnlineUser   │  │ ClientController│ │
│  │ Enhance      │  │ Enhance      │  │ Management     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

### 3.2 数据模型设计

#### sys_client 表结构

```sql
CREATE TABLE `sys_client` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `client_code` varchar(50) NOT NULL COMMENT '客户端编码',
  `client_name` varchar(100) NOT NULL COMMENT '客户端名称',
  `app_id` varchar(64) NOT NULL COMMENT '应用ID',
  `app_secret` varchar(128) NOT NULL COMMENT '应用密钥',
  
  -- Token配置
  `token_timeout` bigint DEFAULT 7200 COMMENT 'Token有效期（秒）',
  `token_activity_timeout` bigint DEFAULT -1 COMMENT 'Token活跃超时（秒）',
  `token_prefix` varchar(20) DEFAULT 'Bearer' COMMENT 'Token前缀',
  `token_name` varchar(50) DEFAULT 'Authorization' COMMENT 'Token名称',
  `concurrent_login` tinyint(1) DEFAULT 0 COMMENT '是否允许并发登录（0-否 1-是）',
  `share_token` tinyint(1) DEFAULT 0 COMMENT '是否共享Token（0-否 1-是）',
  
  -- 安全配置
  `enable_ip_limit` tinyint(1) DEFAULT 0 COMMENT '是否启用IP限制',
  `ip_whitelist` text COMMENT 'IP白名单（JSON数组）',
  `enable_encrypt` tinyint(1) DEFAULT 0 COMMENT '是否启用加密传输',
  `encrypt_algorithm` varchar(50) DEFAULT 'AES' COMMENT '加密算法',
  
  -- 业务配置
  `max_user_count` bigint DEFAULT -1 COMMENT '最大用户数（-1不限）',
  `max_online_count` bigint DEFAULT -1 COMMENT '最大在线数（-1不限）',
  `auth_types` varchar(255) DEFAULT 'password' COMMENT '支持的认证方式（逗号分隔）',
  
  -- 状态与描述
  `status` tinyint DEFAULT 1 COMMENT '状态（0-禁用 1-启用）',
  `description` varchar(500) COMMENT '客户端描述',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  
  -- 基础字段
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `create_by` bigint,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_by` bigint,
  `create_dept` bigint,
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_client_code` (`client_code`),
  UNIQUE KEY `uk_app_id` (`app_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端管理表';
```

**初始化数据**：
```sql
INSERT INTO `sys_client` VALUES 
(1, 'pc', 'PC端', 'forge_pc_001', 'pc_secret_key_encrypted_xxx', 86400, 7200, 'Bearer', 'Authorization', 0, 0, 0, NULL, 0, 'AES', -1, -1, 'password,password_captcha', 1, 'PC客户端', NULL, NOW(), 1, NOW(), 1, NULL),
(2, 'app', 'APP端', 'forge_app_001', 'app_secret_key_encrypted_xxx', 2592000, -1, 'Bearer', 'Authorization', 1, 0, 0, NULL, 1, 'AES', -1, -1, 'password,phone_captcha', 1, '移动APP客户端', NULL, NOW(), 1, NOW(), 1, NULL),
(3, 'h5', 'H5端', 'forge_h5_001', 'h5_secret_key_encrypted_xxx', 604800, 3600, 'Bearer', 'Authorization', 1, 0, 0, NULL, 0, 'AES', -1, -1, 'password,wechat', 1, 'H5网页客户端', NULL, NOW(), 1, NOW(), 1, NULL),
(4, 'wechat', '微信小程序', 'forge_wx_001', 'wx_secret_key_encrypted_xxx', 2592000, -1, 'Bearer', 'Authorization', 1, 0, 0, NULL, 1, 'AES', -1, -1, 'wechat', 1, '微信小程序客户端', NULL, NOW(), 1, NOW(), 1, NULL);
```

### 3.3 Entity类设计

```java
@Data
@TableName("sys_client")
public class SysClient extends BaseEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private String clientCode;
    private String clientName;
    private String appId;
    
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String appSecret;
    
    private Long tokenTimeout;
    private Long tokenActivityTimeout;
    private String tokenPrefix;
    private String tokenName;
    private Boolean concurrentLogin;
    private Boolean shareToken;
    
    private Boolean enableIpLimit;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> ipWhitelist;
    
    private Boolean enableEncrypt;
    private String encryptAlgorithm;
    
    private Long maxUserCount;
    private Long maxOnlineCount;
    private String authTypes;
    
    private Integer status;
    private String description;
    private Long tenantId;
}
```

### 3.4 核心服务接口

```java
public interface IClientService extends IService<SysClient> {
    
    SysClient getByCode(String clientCode);
    
    SysClient getByAppId(String appId);
    
    boolean validateAppSecret(String appId, String appSecret);
    
    List<SysClient> listByTenant(Long tenantId);
    
    void reloadClientConfig(String clientCode);
}
```

### 3.5 登录流程改造

**核心改动点**：`SystemAuthServiceImpl.login()` 方法

```java
@Override
public LoginResult login(LoginRequest request) {
    // 1. 参数校验和默认值
    if (StrUtil.isBlank(request.getAuthType())) {
        request.setAuthType("password");
    }
    
    // 2. 验证客户端（新增）
    SysClient client = validateAndLoadClient(request.getUserClient(), request.getAppId());
    
    // 3. 应用客户端Token配置（新增）
    applyClientTokenConfig(client);
    
    // 4. 获取认证策略
    IAuthStrategy strategy = authStrategyFactory.getStrategy(
        request.getAuthType(), request.getUserClient()
    );
    
    // 5. 执行认证策略
    LoginUser loginUser = strategy.authenticate(request);
    
    // 6. 处理同一账号登录策略
    handleSameAccountLogin(loginUser.getUserId(), client);
    
    // 7. 设置登录时间和IP
    loginUser.setLoginTime(System.currentTimeMillis());
    
    // 8. 执行Sa-Token登录（新增设备类型标记）
    StpUtil.login(loginUser.getUserId(), new SaLoginModel()
        .setDevice(request.getUserClient())
        .setTimeout(client.getTokenTimeout())
    );
    
    // 9. 设置Session
    SessionHelper.setLoginUser(loginUser);
    loginUser.setUserClient(request.getUserClient());
    
    // 10. 构建返回结果
    return buildLoginResult(loginUser, client);
}

private SysClient validateAndLoadClient(String userClient, String appId) {
    if (StrUtil.isBlank(userClient)) {
        userClient = "pc";
    }
    
    SysClient client = clientService.getByCode(userClient);
    if (client == null) {
        throw new RuntimeException("客户端不存在: " + userClient);
    }
    
    if (client.getStatus() == 0) {
        throw new RuntimeException("客户端已禁用: " + userClient);
    }
    
    // 可选：验证AppId（根据配置开关）
    if (authProperties.getEnableClientValidation() && StrUtil.isNotBlank(appId)) {
        if (!client.getAppId().equals(appId)) {
            throw new RuntimeException("客户端AppId不匹配");
        }
    }
    
    return client;
}

private void applyClientTokenConfig(SysClient client) {
    cn.dev33.satoken.config.SaTokenConfig config = SaManager.getConfig();
    config.setTimeout(client.getTokenTimeout());
    config.setActiveTimeout(client.getTokenActivityTimeout());
    config.setIsConcurrent(client.getConcurrentLogin());
    config.setIsShare(client.getShareToken());
}
```

### 3.6 在线用户管理增强

**改动点**：`SysOnlineUserServiceImpl` 新增方法

```java
public List<OnlineUser> getOnlineUsersByClient(String clientType) {
    List<OnlineUser> result = new ArrayList<>();
    
    List<String> tokens = StpUtil.searchTokenValue("", 0, -1, false);
    for (String token : tokens) {
        SaSession session = StpUtil.getTokenSessionByToken(token);
        LoginUser loginUser = (LoginUser) session.get(SessionHelper.LOGIN_USER_KEY);
        
        if (loginUser != null && loginUser.getUserClient().equals(clientType)) {
            OnlineUser user = buildOnlineUser(loginUser, token);
            result.add(user);
        }
    }
    
    return result;
}

public void kickoutByClient(Long userId, String clientType) {
    StpUtil.kickout(userId, clientType);
}

public Map<String, Long> getOnlineCountByClient() {
    Map<String, Long> result = new HashMap<>();
    
    List<SysClient> clients = clientService.list();
    for (SysClient client : clients) {
        long count = getOnlineUsersByClient(client.getClientCode()).size();
        result.put(client.getClientCode(), count);
    }
    
    return result;
}
```

---

## 四、数据模型变更

### 4.1 新增表

| 操作 | 表名 | 说明 |
|------|------|------|
| 新增 | `sys_client` | 客户端管理表，存储客户端配置、Token策略、安全策略 |

### 4.2 表字段变更

| 操作 | 表名 | 字段 | 说明 |
|------|------|------|------|
| 无变更 | `sys_user` | userClient字段已存在 | 用户触点字段，无需修改 |

### 4.3 Redis Key变更

| Key类型 | 变更类型 | 说明 |
|--------|---------|------|
| 客户端配置缓存 | **新增** | `client:config:{clientCode}` 缓存客户端配置 |

---

## 五、接口变更

### 5.1 新增接口

#### API1：客户端管理接口

```java
@RestController
@RequestMapping("/api/system/client")
public class SysClientController {
    
    @GetMapping("/page")
    public RespInfo<PageResult<SysClientVO>> page(@RequestParam Map<String, Object> params);
    
    @GetMapping("/{id}")
    public RespInfo<SysClientVO> getById(@PathVariable Long id);
    
    @PostMapping
    public RespInfo<Boolean> create(@RequestBody SysClientDTO dto);
    
    @PutMapping
    public RespInfo<Boolean> update(@RequestBody SysClientDTO dto);
    
    @DeleteMapping("/{id}")
    public RespInfo<Boolean> delete(@PathVariable Long id);
    
    @GetMapping("/online/{clientCode}")
    public RespInfo<List<OnlineUserVO>> getOnlineUsers(@PathVariable String clientCode);
    
    @GetMapping("/online/stats")
    public RespInfo<Map<String, Long>> getOnlineStats();
    
    @PostMapping("/kickout/{userId}/{clientCode}")
    public RespInfo<Boolean> kickout(@PathVariable Long userId, @PathVariable String clientCode);
    
    @GetMapping("/secret/{id}")
    @PreAuth("hasPermission('system:client:view-secret')")
    public RespInfo<String> getAppSecret(@PathVariable Long id);
}
```

### 5.2 登录接口变更

#### 变更：LoginRequest增加字段

**原LoginRequest**：
```java
@Data
public class LoginRequest {
    private String username;
    private String password;
    private String authType = "password";
    private String userClient;  // 已存在
    // ...
}
```

**新LoginRequest**：
```java
@Data
public class LoginRequest {
    private String username;
    private String password;
    private String authType = "password";
    private String userClient;
    private String appId;  // 新增：客户端AppId（可选）
    // ...
}
```

**兼容性影响**：
- 前端无需立即修改（appId可选）
- 建议前端传递userClient字段（pc/app/h5/wechat）

---

## 六、风险评估

### 6.1 技术风险

| 风险项 | 风险等级 | 影响分析 | 缓解措施 |
|-------|---------|---------|---------|
| Sa-Token配置动态修改 | 中 | 多线程并发修改config可能冲突 | 使用ThreadLocal或每次登录前设置 |
| AppSecret加密存储 | 低 | 加密解密性能影响 | 使用高效加密算法（AES） |
| 客户端配置缓存一致性 | 中 | 配置修改后缓存未更新 | 提供配置刷新接口 |
| 客户端不存在异常 | 低 | 登录失败，用户体验影响 | 提供默认客户端配置 |

### 6.2 业务风险

| 风险项 | 风险等级 | 影响分析 | 缓解措施 |
|-------|---------|---------|---------|
| 客户端配置错误 | 中 | Token过期时间设置错误影响登录 | 提供配置验证和默认值 |
| AppId泄露 | 高 | 恶意伪造客户端请求 | AppSecret加密存储，定期更换 |
| 并发登录策略冲突 | 低 | 用户在不同设备登录体验差异 | 提供明确的策略配置说明 |
| IP白名单误配置 | 中 | 正常用户无法登录 | 提供IP白名单测试接口 |

### 6.3 兼容性风险

| 风险项 | 风险等级 | 影响分析 | 缓解措施 |
|-------|---------|---------|---------|
| 登录接口变更 | 低 | appId字段可选，现有调用无影响 | 提向前端传递userClient |
| Token配置动态修改 | 中 | 可能影响现有登录逻辑 | 充分测试，提供回退机制 |
| 数据库表新增 | 低 | 无破坏性变更 | 提供初始化SQL脚本 |

---

## 七、测试策略

### 7.1 单元测试

**测试范围**：
- ClientService测试（查询、验证、缓存）
- ClientConfigLoader测试（配置加载、应用）
- AuthService增强测试（客户端验证、配置应用）
- OnlineUserService增强测试（按客户端查询）

**测试用例清单**：

| 测试类 | 核心测试方法 | 验证点 |
|-------|-------------|--------|
| `ClientServiceImplTest` | `testGetByCode` | 根据编码查询客户端 |
| `ClientServiceImplTest` | `testValidateAppSecret` | AppSecret验证 |
| `ClientServiceImplTest` | `testConfigCache` | 配置缓存一致性 |
| `SystemAuthServiceImplTest` | `testLoginWithClientConfig` | 客户端配置应用 |
| `SystemAuthServiceImplTest` | `testClientValidation` | 客户端验证逻辑 |
| `SysOnlineUserServiceImplTest` | `testGetOnlineByClient` | 按客户端查询在线用户 |
| `SysOnlineUserServiceImplTest` | `testKickoutByClient` | 按客户端踢出用户 |

### 7.2 集成测试

**测试场景**：

1. **端到端登录流程测试**
   - PC端登录 → 验证8小时token
   - APP端登录 → 验证30天token
   - H5端登录 → 验证7天token

2. **客户端验证测试**
   - 无AppId登录 → 正常
   - 错误AppId登录 → 拒绝
   - 禁用客户端登录 → 拒绝

3. **在线用户管理测试**
   - 查询指定客户端在线用户
   - 踢出指定客户端用户
   - 统计各客户端在线数

4. **并发登录策略测试**
   - APP端允许并发登录
   - PC端拒绝并发登录

**测试工具**：
- MockMvc（Controller测试）
- Testcontainers（可选Redis容器）
- 多线程测试（并发场景）

### 7.3 性能测试

**测试指标**：

| 指标 | 目标值 | 测试方法 |
|-----|-------|---------|
| 客户端配置查询耗时 | < 10ms（缓存） | 单元测试 |
| AppSecret验证耗时 | < 5ms | 单元测试 |
| 登录流程额外耗时 | < 20ms | 集成测试对比 |
| 在线用户查询耗时 | < 50ms（1000用户） | 压力测试 |

---

## 八、实施计划

### 8.1 开发任务拆解

| 任务ID | 任务描述 | 预估工时 | 优先级 | 涉及文件 |
|-------|---------|---------|-------|---------|
| T1 | 创建sys_client表和初始化数据 | 0.5人日 | P0 | SQL脚本 |
| T2 | 生成SysClient实体类 | 0.5人日 | P0 | SysClient.java |
| T3 | 实现ClientService和Mapper | 1人日 | P0 | ClientServiceImpl.java, SysClientMapper.java |
| T4 | 改造登录流程（应用客户端配置） | 1人日 | P0 | SystemAuthServiceImpl.java |
| T5 | 增强在线用户管理（按客户端查询） | 0.5人日 | P0 | SysOnlineUserServiceImpl.java |
| T6 | 实现客户端管理Controller | 0.5人日 | P0 | SysClientController.java |
| T7 | 生成前端管理页面（使用代码生成器） | 0.5人日 | P1 | Vue页面 |
| T8 | 编写单元测试 | 1人日 | P1 | 测试类 |
| T9 | 编写集成测试 | 0.5人日 | P2 | 测试类 |
| T10 | 文档编写（使用指南） | 0.5人日 | P2 | 使用文档 |

**总工时**: 5人日  
**建议团队**: 1人  

### 8.2 实施里程碑

**Phase 1：核心功能实现（2人日）**
- T1-T6完成，核心客户端管理功能可用
- 支持客户端配置、登录流程改造、在线用户增强

**Phase 2：前端和测试（2人日）**
- T7-T9完成，前端页面生成、测试覆盖
- 管理页面可用，测试通过率>85%

**Phase 3：文档和优化（1人日）**
- T10完成，文档完善
- 使用文档完整、性能达标

---

## 九、待澄清问题

### 9.1 技术决策待确认

| 问题ID | 问题描述 | 影响范围 | 建议方案 |
|-------|---------|---------|---------|
| Q1 | AppSecret加密算法：AES还是RSA？ | 安全性 | 使用AES（对称加密，性能好） |
| Q2 | 客户端配置是否需要缓存？ | 性能 | 建议缓存，使用Redis，配置变更时刷新 |
| Q3 | 是否强制验证AppId？ | 安全性 | 默认不强制，通过配置开关控制 |
| Q4 | Token配置动态修改是否影响其他用户？ | 并发安全 | 每次登录前单独设置，不影响全局 |
| Q5 | 客户端不存在时是否提供默认配置？ | 兼容性 | 提供默认pc客户端配置 |

### 9.2 业务规则待确认

| 问题ID | 问题描述 | 影响范围 | 建议方案 |
|-------|---------|---------|---------|
| Q6 | 是否支持多租户级别的客户端配置？ | 多租户 | 支持，client表有tenant_id字段 |
| Q7 | AppId/AppSecret是否需要定期更换？ | 安全性 | 建议提供更换接口，但不强制定期更换 |
| Q8 | 是否限制客户端的认证方式？ | 安全性 | 支持，通过auth_types字段限制 |
| Q9 | IP白名单是否支持动态更新？ | 灵活性 | 支持，配置变更立即生效（缓存刷新） |
| Q10 | 是否需要客户端级别的API鉴权？ | 安全性 | 本次不实现，作为后续扩展点 |

---

## 十、验收标准

### 10.1 功能验收标准

| 验收项 | 验收标准 | 验证方法 |
|-------|---------|---------|
| 客户端配置管理 | CRUD功能正常，配置可保存和查询 | 功能测试 |
| 登录流程改造 | 不同客户端登录后token有效期符合配置 | 登录测试 |
| 客户端验证 | AppId验证正常，错误AppId拒绝登录 | 安全测试 |
| 在线用户管理 | 按客户端查询、踢出功能正常 | 功能测试 |
| 前端页面 | 管理页面可用，在线统计展示正常 | UI测试 |

### 10.2 性能验收标准

| 验收项 | 验收标准 | 验证方法 |
|-------|---------|---------|
| 登录流程性能 | 客户端配置查询+应用增加耗时<20ms | 性能测试 |
| 在线用户查询 | 查询1000用户耗时<100ms | 压力测试 |
| 配置缓存 | 缓存命中率>90% | 监控统计 |

### 10.3 兼容性验收标准

| 验收项 | 验收标准 | 验证方法 |
|-------|---------|---------|
| 登录接口兼容 | 现有登录调用无需修改即可运行 | 兼容性测试 |
| Token配置兼容 | 未指定userClient时使用默认配置 | 兼容性测试 |
| 数据库兼容 | 新增表不影响现有表 | 数据库测试 |

---

## 十一、知识沉淀

### 11.1 参考资源

| 资源名称 | 参考内容 | 链接 |
|---------|---------|------|
| Sa-Token官方文档 | 多端登录、设备类型标记 | https://sa-token.cc |
| Sa-Token源码 | SaLoginModel、StpLogic实现 | GitHub |
| Spring Boot加密 | EncryptTypeHandler实现 | MyBatis-Plus文档 |

### 11.2 关键技术点

1. **Sa-Token设备类型标记**：`SaLoginModel.setDevice()` 区分不同客户端
2. **动态Token配置**：登录前动态修改SaTokenConfig参数
3. **按设备踢出**：`StpUtil.kickout(userId, device)` 踢出指定设备
4. **敏感字段加密**：MyBatis-Plus TypeHandler实现字段加密
5. **配置缓存刷新**：配置变更时主动刷新Redis缓存

---

## 十二、附录

### 12.1 配置示例

**application.yml增加配置**：
```yaml
forge:
  auth:
    enable-client-validation: false  # 是否强制验证客户端AppId
    client-config-cache-expire: 3600 # 客户端配置缓存时间（秒）
```

**客户端初始化配置**：
```sql
-- PC端：session模式，8小时token，不允许并发
INSERT INTO sys_client VALUES (1, 'pc', 'PC端', 'forge_pc_001', 'encrypted_secret', 86400, 7200, 'Bearer', 'Authorization', 0, 0, ...);

-- APP端：长期登录，30天token，允许并发
INSERT INTO sys_client VALUES (2, 'app', 'APP端', 'forge_app_001', 'encrypted_secret', 2592000, -1, 'Bearer', 'Authorization', 1, 0, ...);
```

### 12.2 使用示例

**示例1：前端登录调用**

```javascript
// PC端登录
const loginPC = async (loginForm) => {
  const data = {
    username: loginForm.username,
    password: loginForm.password,
    userClient: 'pc',  // 指定客户端类型
    appId: 'forge_pc_001'  // 可选：客户端AppId
  }
  return request.post('/auth/login', data)
}

// APP端登录
const loginApp = async (loginForm) => {
  const data = {
    username: loginForm.username,
    password: loginForm.password,
    userClient: 'app',  // APP端
    authType: 'phone_captcha'  // 手机验证码登录
  }
  return request.post('/auth/login', data)
}
```

**示例2：查询客户端在线用户**

```java
@GetMapping("/api/system/client/online/app")
public RespInfo<List<OnlineUserVO>> getAppOnlineUsers() {
    List<OnlineUser> users = onlineUserService.getOnlineUsersByClient("app");
    return RespInfo.success(users);
}
```

**示例3：踢出PC端用户**

```java
@PostMapping("/api/system/client/kickout/{userId}/pc")
public RespInfo<Boolean> kickoutPCUser(@PathVariable Long userId) {
    onlineUserService.kickoutByClient(userId, "pc");
    return RespInfo.success(true);
}
```

---

**文档版本**: v1.0  
**编写日期**: 2026-04-07  
**编写人**: AI Assistant  
**审核状态**: 待用户审核
# 任务拆分 — Sa-Token客户端管理功能

> 拆分顺序：数据模型 → 接口协议 → 底层实现 → 上层编排 → 入口层
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> 每个任务必须精确到文件路径和函数签名

## 前置条件

- [ ] 已确认Spec文档（`spec.md`）
- [ ] 已理解Sa-Token设备类型标记机制
- [ ] 已理解项目代码生成器使用方式

---

## Task 1: 创建数据库表和初始化数据

- **目标**: 创建sys_client表并插入初始客户端配置数据
- **涉及文件**:
    - `forge/forge-admin/sql/sys_client.sql` — 新增，表结构和初始化数据
    - `forge/forge-framework/forge-starter-parent/forge-starter-auth/sql/sys_client.sql` — 新增，表结构定义

- **关键签名**:
  ```sql
  CREATE TABLE `sys_client` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `client_code` varchar(50) NOT NULL,
    -- ...
  );
  
  INSERT INTO `sys_client` VALUES 
  (1, 'pc', 'PC端', ...),
  (2, 'app', 'APP端', ...);
  ```

- [ ] **Step 1: 创建表结构SQL文件**

创建 `forge/forge-framework/forge-starter-parent/forge-starter-auth/sql/sys_client.sql`

```sql
CREATE TABLE `sys_client` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `client_code` varchar(50) NOT NULL COMMENT '客户端编码',
  `client_name` varchar(100) NOT NULL COMMENT '客户端名称',
  `app_id` varchar(64) NOT NULL COMMENT '应用ID',
  `app_secret` varchar(128) NOT NULL COMMENT '应用密钥（加密存储）',
  
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

- [ ] **Step 2: 创建初始化数据SQL文件**

创建 `forge/forge-admin/sql/sys_client_init.sql`

```sql
INSERT INTO `sys_client` VALUES 
(1, 'pc', 'PC端', 'forge_pc_001', 'pc_secret_encrypted', 86400, 7200, 'Bearer', 'Authorization', 0, 0, 0, NULL, 0, 'AES', -1, -1, 'password,password_captcha', 1, 'PC客户端', NULL, NOW(), 1, NOW(), 1, NULL),
(2, 'app', 'APP端', 'forge_app_001', 'app_secret_encrypted', 2592000, -1, 'Bearer', 'Authorization', 1, 0, 0, NULL, 1, 'AES', -1, -1, 'password,phone_captcha', 1, '移动APP客户端', NULL, NOW(), 1, NOW(), 1, NULL),
(3, 'h5', 'H5端', 'forge_h5_001', 'h5_secret_encrypted', 604800, 3600, 'Bearer', 'Authorization', 1, 0, 0, NULL, 0, 'AES', -1, -1, 'password,wechat', 1, 'H5网页客户端', NULL, NOW(), 1, NOW(), 1, NULL),
(4, 'wechat', '微信小程序', 'forge_wx_001', 'wx_secret_encrypted', 2592000, -1, 'Bearer', 'Authorization', 1, 0, 0, NULL, 1, 'AES', -1, -1, 'wechat', 1, '微信小程序客户端', NULL, NOW(), 1, NOW(), 1, NULL);
```

- [ ] **Step 3: 执行数据库迁移**

```bash
mysql -u root -p forge_admin < forge/forge-framework/forge-starter-parent/forge-starter-auth/sql/sys_client.sql
mysql -u root -p forge_admin < forge/forge-admin/sql/sys_client_init.sql
```

Expected: 表创建成功，初始数据插入成功

- [ ] **Step 4: 验证表结构**

```bash
mysql -u root -p -e "DESC sys_client;"
```

Expected: 显示表字段和索引

- [ ] **Step 5: Commit**

```bash
git add forge/forge-framework/forge-starter-parent/forge-starter-auth/sql/sys_client.sql
git add forge/forge-admin/sql/sys_client_init.sql
git commit -m "feat(client): add sys_client table and init data"
```

---

## Task 2: 生成SysClient实体类

- **目标**: 使用MyBatis-Plus代码生成器生成SysClient实体类
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysClient.java` — 新增
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/dto/SysClientDTO.java` — 新增
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/vo/SysClientVO.java` — 新增

- **关键签名**:
  ```java
  @Data
  @TableName("sys_client")
  public class SysClient extends BaseEntity {
      @TableId(value = "id", type = IdType.AUTO)
      private Long id;
      private String clientCode;
      // ...
  }
  ```

- [ ] **Step 1: 使用代码生成器生成实体类**

参考项目代码生成器文档，生成sys_client表相关代码

或手动创建实体类：`SysClient.java`

```java
package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_client", autoResultMap = true)
public class SysClient extends BaseEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private String clientCode;
    private String clientName;
    private String appId;
    
    @TableField(typeHandler = com.mdframe.forge.starter.crypto.handler.EncryptTypeHandler.class)
    private String appSecret;
    
    private Long tokenTimeout;
    private Long tokenActivityTimeout;
    private String tokenPrefix;
    private String tokenName;
    private Boolean concurrentLogin;
    private Boolean shareToken;
    
    private Boolean enableIpLimit;
    
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
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

- [ ] **Step 2: 创建DTO类**

`SysClientDTO.java`

```java
package com.mdframe.forge.plugin.system.dto;

import lombok.Data;
import java.util.List;

@Data
public class SysClientDTO {
    private Long id;
    private String clientCode;
    private String clientName;
    private String appId;
    private String appSecret;
    
    private Long tokenTimeout;
    private Long tokenActivityTimeout;
    private String tokenPrefix;
    private String tokenName;
    private Boolean concurrentLogin;
    private Boolean shareToken;
    
    private Boolean enableIpLimit;
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

- [ ] **Step 3: 创建VO类**

`SysClientVO.java`

```java
package com.mdframe.forge.plugin.system.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SysClientVO {
    private Long id;
    private String clientCode;
    private String clientName;
    private String appId;
    private String appSecretMasked; // 脱敏后的密钥
    
    private Long tokenTimeout;
    private Long tokenActivityTimeout;
    private String tokenPrefix;
    private String tokenName;
    private Boolean concurrentLogin;
    private Boolean shareToken;
    
    private Boolean enableIpLimit;
    private List<String> ipWhitelist;
    
    private Boolean enableEncrypt;
    private String encryptAlgorithm;
    
    private Long maxUserCount;
    private Long maxOnlineCount;
    private String authTypes;
    
    private Integer status;
    private String description;
    private Long tenantId;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 4: 编译验证**

```bash
cd forge && mvn clean compile
```

Expected: 编译成功，无错误

- [ ] **Step 5: Commit**

```bash
git add forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysClient.java
git add forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/dto/SysClientDTO.java
git add forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/vo/SysClientVO.java
git commit -m "feat(client): add SysClient entity, DTO and VO classes"
```

---

## Task 3: 实现ClientService和Mapper

- **目标**: 实现客户端服务层和数据访问层
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/mapper/SysClientMapper.java` — 新增
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/IClientService.java` — 新增
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/ClientServiceImpl.java` — 新增

- **关键签名**:
  ```java
  public interface IClientService extends IService<SysClient> {
      SysClient getByCode(String clientCode);
      boolean validateAppSecret(String appId, String appSecret);
  }
  
  public class ClientServiceImpl extends ServiceImpl<SysClientMapper, SysClient> implements IClientService {
      @Override
      public SysClient getByCode(String clientCode) { }
  }
  ```

- [ ] **Step 1: 创建Mapper接口**

`SysClientMapper.java`

```java
package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysClient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysClientMapper extends BaseMapper<SysClient> {
}
```

- [ ] **Step 2: 创建Service接口**

`IClientService.java`

```java
package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.system.entity.SysClient;
import java.util.List;

public interface IClientService extends IService<SysClient> {
    
    SysClient getByCode(String clientCode);
    
    SysClient getByAppId(String appId);
    
    boolean validateAppSecret(String appId, String appSecret);
    
    List<SysClient> listByTenant(Long tenantId);
    
    void reloadClientConfigCache(String clientCode);
    
    String getMaskedAppSecret(Long clientId);
}
```

- [ ] **Step 3: 实现Service**

`ClientServiceImpl.java`

```java
package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.plugin.system.mapper.SysClientMapper;
import com.mdframe.forge.plugin.system.service.IClientService;
import com.mdframe.forge.starter.cache.service.ICacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl extends ServiceImpl<SysClientMapper, SysClient> implements IClientService {
    
    private final ICacheService cacheService;
    private static final String CLIENT_CONFIG_CACHE_KEY = "client:config:";
    private static final long CLIENT_CONFIG_CACHE_EXPIRE = 3600;
    
    @Override
    public SysClient getByCode(String clientCode) {
        if (StrUtil.isBlank(clientCode)) {
            return null;
        }
        
        String cacheKey = CLIENT_CONFIG_CACHE_KEY + clientCode;
        SysClient cached = cacheService.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        LambdaQueryWrapper<SysClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysClient::getClientCode, clientCode);
        SysClient client = this.getOne(wrapper);
        
        if (client != null) {
            cacheService.set(cacheKey, client, CLIENT_CONFIG_CACHE_EXPIRE, TimeUnit.SECONDS);
        }
        
        return client;
    }
    
    @Override
    public SysClient getByAppId(String appId) {
        if (StrUtil.isBlank(appId)) {
            return null;
        }
        
        LambdaQueryWrapper<SysClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysClient::getAppId, appId);
        return this.getOne(wrapper);
    }
    
    @Override
    public boolean validateAppSecret(String appId, String appSecret) {
        SysClient client = getByAppId(appId);
        if (client == null) {
            return false;
        }
        
        return client.getAppSecret().equals(appSecret);
    }
    
    @Override
    public List<SysClient> listByTenant(Long tenantId) {
        LambdaQueryWrapper<SysClient> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.eq(SysClient::getTenantId, tenantId);
        }
        wrapper.eq(SysClient::getStatus, 1);
        return this.list(wrapper);
    }
    
    @Override
    public void reloadClientConfigCache(String clientCode) {
        String cacheKey = CLIENT_CONFIG_CACHE_KEY + clientCode;
        cacheService.delete(cacheKey);
        
        SysClient client = getByCode(clientCode);
        if (client != null) {
            cacheService.set(cacheKey, client, CLIENT_CONFIG_CACHE_EXPIRE, TimeUnit.SECONDS);
            log.info("客户端配置缓存已刷新: {}", clientCode);
        }
    }
    
    @Override
    public String getMaskedAppSecret(Long clientId) {
        SysClient client = this.getById(clientId);
        if (client == null || StrUtil.isBlank(client.getAppSecret())) {
            return "";
        }
        
        String secret = client.getAppSecret();
        if (secret.length() > 8) {
            return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
        }
        return "****";
    }
}
```

- [ ] **Step 4: 编写单元测试**

`ClientServiceImplTest.java`

```java
package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.plugin.system.service.IClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ClientServiceImplTest {
    
    @Autowired
    private IClientService clientService;
    
    @Test
    void testGetByCode() {
        SysClient client = clientService.getByCode("pc");
        assertNotNull(client);
        assertEquals("PC端", client.getClientName());
        assertEquals(86400L, client.getTokenTimeout());
    }
    
    @Test
    void testGetByCodeNotFound() {
        SysClient client = clientService.getByCode("invalid_code");
        assertNull(client);
    }
    
    @Test
    void testValidateAppSecret() {
        SysClient client = clientService.getByCode("app");
        assertNotNull(client);
        
        boolean valid = clientService.validateAppSecret(client.getAppId(), client.getAppSecret());
        assertTrue(valid);
    }
}
```

- [ ] **Step 5: 运行测试**

```bash
cd forge && mvn test -Dtest=ClientServiceImplTest
```

Expected: 测试通过

- [ ] **Step 6: Commit**

```bash
git add forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/mapper/SysClientMapper.java
git add forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/IClientService.java
git add forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/ClientServiceImpl.java
git commit -m "feat(client): implement ClientService with cache support"
```

---

## Task 4: 改造登录流程应用客户端配置

- **目标**: 登录时动态应用客户端级别的Token配置，支持设备类型标记
- **涉及文件**:
    - `forge/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/domain/LoginRequest.java` — 修改，增加appId字段
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SystemAuthServiceImpl.java` — 修改，增加客户端验证和配置应用逻辑
    - `forge/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/session/LoginUser.java` — 修改，增加userClient字段

- **关键签名**:
  ```java
  public LoginResult login(LoginRequest request) {
      SysClient client = validateAndLoadClient(request.getUserClient(), request.getAppId());
      applyClientTokenConfig(client);
      StpUtil.login(loginUser.getUserId(), new SaLoginModel().setDevice(request.getUserClient()).setTimeout(client.getTokenTimeout()));
  }
  ```

- [ ] **Step 1: LoginRequest增加appId字段**

`LoginRequest.java`

```java
// 在现有字段基础上增加
private String appId;  // 客户端AppId（可选，用于客户端验证）
```

- [ ] **Step 2: LoginUser增加userClient字段**

`LoginUser.java`

```java
// 在现有字段基础上增加
private String userClient;  // 用户客户端类型（pc/app/h5/wechat）
```

- [ ] **Step 3: SystemAuthServiceImpl增加客户端验证方法**

```java
// 在SystemAuthServiceImpl类中增加以下方法

@Autowired
private IClientService clientService;

private SysClient validateAndLoadClient(String userClient, String appId) {
    if (StrUtil.isBlank(userClient)) {
        userClient = "pc";  // 默认PC端
    }
    
    SysClient client = clientService.getByCode(userClient);
    if (client == null) {
        throw new RuntimeException("客户端不存在: " + userClient);
    }
    
    if (client.getStatus() == 0) {
        throw new RuntimeException("客户端已禁用: " + userClient);
    }
    
    // 验证支持的认证方式
    if (StrUtil.isNotBlank(client.getAuthTypes())) {
        String[] supportedAuthTypes = client.getAuthTypes().split(",");
        String requestAuthType = authProperties.getDefaultAuthType();
        boolean supported = false;
        for (String authType : supportedAuthTypes) {
            if (authType.equals(requestAuthType)) {
                supported = true;
                break;
            }
        }
        if (!supported) {
            throw new RuntimeException("客户端不支持该认证方式: " + requestAuthType);
        }
    }
    
    // 可选：验证AppId
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
    
    log.debug("应用客户端Token配置: client={}, timeout={}s, concurrent={}", 
        client.getClientCode(), client.getTokenTimeout(), client.getConcurrentLogin());
}
```

- [ ] **Step 4: 改造login方法**

修改 `SystemAuthServiceImpl.login()` 方法：

```java
@Override
public LoginResult login(LoginRequest request) {
    if (StrUtil.isBlank(request.getAuthType())) {
        request.setAuthType("password");
    }
    
    if (StrUtil.isBlank(request.getUserClient())) {
        request.setUserClient("pc");  // 默认PC端
    }
    
    // 1. 验证客户端并加载配置
    SysClient client = validateAndLoadClient(request.getUserClient(), request.getAppId());
    
    // 2. 应用客户端Token配置
    applyClientTokenConfig(client);
    
    // 3. 获取认证策略
    IAuthStrategy strategy = authStrategyFactory.getStrategy(
        request.getAuthType(), request.getUserClient()
    );
    
    log.info("开始认证: authType={}, userClient={}, strategy={}",
        request.getAuthType(), request.getUserClient(), strategy.getClass().getSimpleName());
    
    // 4. 执行认证策略
    LoginUser loginUser = strategy.authenticate(request);
    
    // 5. 处理同一账号登录策略（考虑客户端并发配置）
    handleSameAccountLogin(loginUser.getUserId(), client);
    
    // 6. 设置登录时间和IP
    loginUser.setLoginTime(System.currentTimeMillis());
    loginUser.setUserClient(request.getUserClient());
    
    // 7. 执行Sa-Token登录（使用设备类型标记和客户端超时时间）
    StpUtil.login(loginUser.getUserId(), new SaLoginModel()
        .setDevice(request.getUserClient())
        .setTimeout(client.getTokenTimeout())
    );
    
    // 8. 将完整的用户信息设置到Session中
    SessionHelper.setLoginUser(loginUser);
    
    log.info("用户登录成功: username={}, userId={}, client={}, tokenTimeout={}s",
        loginUser.getUsername(), loginUser.getUserId(), request.getUserClient(), client.getTokenTimeout());
    
    // 9. 构建返回结果
    return buildLoginResult(loginUser, client);
}

private void handleSameAccountLogin(Long userId, SysClient client) {
    if (!authProperties.getEnableOnlineUserManagement()) {
        return;
    }
    
    // 根据客户端的并发登录配置决定策略
    if (client.getConcurrentLogin()) {
        log.debug("客户端允许并发登录: client={}, userId={}", client.getClientCode(), userId);
        return;
    }
    
    String strategy = authProperties.getSameAccountLoginStrategy();
    
    switch (strategy) {
        case "replace_old":
            String currentToken = StpUtil.getTokenValue();
            onlineUserService.kickoutAllSessions(userId, currentToken);
            log.info("踢出旧登录: userId={}, client={}", userId, client.getClientCode());
            break;
            
        case "reject_new":
            if (!onlineUserService.getUserTokens(userId).isEmpty()) {
                throw new RuntimeException("该账号已在其他地方登录,请先退出后再登录");
            }
            break;
            
        default:
            log.debug("允许并发登录策略: {}", strategy);
    }
}

private LoginResult buildLoginResult(LoginUser loginUser, SysClient client) {
    String token = StpUtil.getTokenValue();
    long tokenTimeout = StpUtil.getTokenTimeout();
    
    return LoginResult.builder()
        .accessToken(token)
        .expiresIn(tokenTimeout)
        .tokenType(client.getTokenPrefix())
        .build();
}
```

- [ ] **Step 5: 编写集成测试**

`SystemAuthServiceImplClientTest.java`

```java
package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.starter.auth.domain.LoginRequest;
import com.mdframe.forge.starter.auth.domain.LoginResult;
import com.mdframe.forge.starter.auth.service.IAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SystemAuthServiceImplClientTest {
    
    @Autowired
    private IAuthService authService;
    
    @Test
    void testLoginPCClient() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");
        request.setUserClient("pc");
        
        LoginResult result = authService.login(request);
        assertNotNull(result.getAccessToken());
        assertTrue(result.getExpiresIn() <= 86400);  // PC端8小时
    }
    
    @Test
    void testLoginAPPClient() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");
        request.setUserClient("app");
        
        LoginResult result = authService.login(request);
        assertNotNull(result.getAccessToken());
        assertTrue(result.getExpiresIn() <= 2592000);  // APP端30天
    }
    
    @Test
    void testLoginInvalidClient() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");
        request.setUserClient("invalid_client");
        
        assertThrows(RuntimeException.class, () -> authService.login(request));
    }
}
```

- [ ] **Step 6: 运行测试**

```bash
cd forge && mvn test -Dtest=SystemAuthServiceImplClientTest
```

Expected: 测试通过

- [ ] **Step 7: Commit**

```bash
git add forge/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/domain/LoginRequest.java
git add forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SystemAuthServiceImpl.java
git add forge/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/session/LoginUser.java
git commit -m "feat(auth): apply client config in login process with device type marking"
```

---

## Task 5: 增强在线用户管理

- **目标**: 增加按客户端查询在线用户、踢出客户端用户的功能
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/ISysOnlineUserService.java` — 修改，增加新方法
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysOnlineUserServiceImpl.java` — 修改，实现新方法

- **关键签名**:
  ```java
  List<OnlineUser> getOnlineUsersByClient(String clientType);
  void kickoutByClient(Long userId, String clientType);
  Map<String, Long> getOnlineCountByClient();
  ```

- [ ] **Step 1: ISysOnlineUserService增加新方法**

```java
// 在接口中增加以下方法

List<OnlineUser> getOnlineUsersByClient(String clientType);

void kickoutByClient(Long userId, String clientType);

Map<String, Long> getOnlineCountByClient();
```

- [ ] **Step 2: SysOnlineUserServiceImpl实现新方法**

```java
// 在实现类中增加以下方法

@Override
public List<OnlineUser> getOnlineUsersByClient(String clientType) {
    List<OnlineUser> result = new ArrayList<>();
    
    List<String> tokens = StpUtil.searchTokenValue("", 0, -1, false);
    for (String token : tokens) {
        try {
            SaSession session = StpUtil.getTokenSessionByToken(token);
            LoginUser loginUser = (LoginUser) session.get(SessionHelper.LOGIN_USER_KEY);
            
            if (loginUser != null && loginUser.getUserClient() != null 
                && loginUser.getUserClient().equals(clientType)) {
                OnlineUser user = buildOnlineUser(loginUser, token);
                result.add(user);
            }
        } catch (Exception e) {
            log.warn("获取token session失败: {}", token, e);
        }
    }
    
    return result;
}

@Override
public void kickoutByClient(Long userId, String clientType) {
    StpUtil.kickout(userId, clientType);
    log.info("踢出用户客户端会话: userId={}, client={}", userId, clientType);
}

@Override
public Map<String, Long> getOnlineCountByClient() {
    Map<String, Long> result = new HashMap<>();
    
    List<SysClient> clients = clientService.list();
    for (SysClient client : clients) {
        long count = getOnlineUsersByClient(client.getClientCode()).size();
        result.put(client.getClientCode(), count);
    }
    
    return result;
}

private OnlineUser buildOnlineUser(LoginUser loginUser, String token) {
    OnlineUser user = new OnlineUser();
    user.setUserId(loginUser.getUserId());
    user.setUsername(loginUser.getUsername());
    user.setRealName(loginUser.getRealName());
    user.setTokenValue(token);
    user.setLoginTime(LocalDateTime.ofInstant(
        Instant.ofEpochMilli(loginUser.getLoginTime()), 
        ZoneId.systemDefault()
    ));
    user.setExpireTime(LocalDateTime.now().plusSeconds(StpUtil.getTokenTimeout(token)));
    user.setUserClient(loginUser.getUserClient());
    return user;
}
```

- [ ] **Step 3: OnlineUser增加userClient字段**

`OnlineUser.java`（如果需要）

```java
// 在OnlineUser类中增加
private String userClient;
```

- [ ] **Step 4: 编写单元测试**

`SysOnlineUserServiceClientTest.java`

```java
package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.service.ISysOnlineUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SysOnlineUserServiceClientTest {
    
    @Autowired
    private ISysOnlineUserService onlineUserService;
    
    @Test
    void testGetOnlineUsersByClient() {
        List onlineUsers = onlineUserService.getOnlineUsersByClient("pc");
        assertNotNull(onlineUsers);
    }
    
    @Test
    void testGetOnlineCountByClient() {
        Map<String, Long> counts = onlineUserService.getOnlineCountByClient();
        assertNotNull(counts);
        assertTrue(counts.containsKey("pc"));
        assertTrue(counts.containsKey("app"));
    }
}
```

- [ ] **Step 5: 运行测试**

```bash
cd forge && mvn test -Dtest=SysOnlineUserServiceClientTest
```

Expected: 测试通过

- [ ] **Step 6: Commit**

```bash
git add forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/ISysOnlineUserService.java
git add forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysOnlineUserServiceImpl.java
git commit -m "feat(online): add client-based online user query and kickout"
```

---

## Task 6: 实现客户端管理Controller

- **目标**: 实现客户端管理API接口，包括CRUD、在线用户查询、踢出等
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller/SysClientController.java` — 新增

- **关键签名**:
  ```java
  @RestController
  @RequestMapping("/api/system/client")
  public class SysClientController {
      @GetMapping("/page")
      public RespInfo<PageResult<SysClientVO>> page(@RequestParam Map<String, Object> params);
      
      @GetMapping("/online/stats")
      public RespInfo<Map<String, Long>> getOnlineStats();
  }
  ```

- [ ] **Step 1: 创建SysClientController**

`SysClientController.java`

```java
package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysClientDTO;
import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.plugin.system.service.IClientService;
import com.mdframe.forge.plugin.system.service.ISysOnlineUserService;
import com.mdframe.forge.plugin.system.vo.SysClientVO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.domain.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/client")
@RequiredArgsConstructor
public class SysClientController {
    
    private final IClientService clientService;
    private final ISysOnlineUserService onlineUserService;
    
    @GetMapping("/page")
    public RespInfo<PageResult<SysClientVO>> page(@RequestParam Map<String, Object> params) {
        Page<SysClient> page = new Page<>();
        page.setSize(Long.parseLong(params.getOrDefault("size", "10").toString()));
        page.setCurrent(Long.parseLong(params.getOrDefault("current", "1").toString()));
        
        LambdaQueryWrapper<SysClient> wrapper = new LambdaQueryWrapper<>();
        String clientCode = (String) params.get("clientCode");
        if (clientCode != null && !clientCode.isEmpty()) {
            wrapper.like(SysClient::getClientCode, clientCode);
        }
        
        String clientName = (String) params.get("clientName");
        if (clientName != null && !clientName.isEmpty()) {
            wrapper.like(SysClient::getClientName, clientName);
        }
        
        String status = (String) params.get("status");
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysClient::getStatus, Integer.parseInt(status));
        }
        
        wrapper.orderByDesc(SysClient::getCreateTime);
        
        Page<SysClient> result = clientService.page(page, wrapper);
        
        List<SysClientVO> voList = result.getRecords().stream()
            .map(this::convertToVO)
            .toList();
        
        PageResult<SysClientVO> pageResult = new PageResult<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(result.getTotal());
        pageResult.setSize(result.getSize());
        pageResult.setCurrent(result.getCurrent());
        
        return RespInfo.success(pageResult);
    }
    
    @GetMapping("/{id}")
    public RespInfo<SysClientVO> getById(@PathVariable Long id) {
        SysClient client = clientService.getById(id);
        if (client == null) {
            return RespInfo.error("客户端不存在");
        }
        return RespInfo.success(convertToVO(client));
    }
    
    @PostMapping
    public RespInfo<Boolean> create(@RequestBody SysClientDTO dto) {
        SysClient client = new SysClient();
        BeanUtils.copyProperties(dto, client);
        boolean success = clientService.save(client);
        return RespInfo.success(success);
    }
    
    @PutMapping
    public RespInfo<Boolean> update(@RequestBody SysClientDTO dto) {
        SysClient client = new SysClient();
        BeanUtils.copyProperties(dto, client);
        boolean success = clientService.updateById(client);
        
        if (success) {
            clientService.reloadClientConfigCache(client.getClientCode());
        }
        
        return RespInfo.success(success);
    }
    
    @DeleteMapping("/{id}")
    public RespInfo<Boolean> delete(@PathVariable Long id) {
        boolean success = clientService.removeById(id);
        return RespInfo.success(success);
    }
    
    @GetMapping("/online/{clientCode}")
    public RespInfo<List> getOnlineUsers(@PathVariable String clientCode) {
        List onlineUsers = onlineUserService.getOnlineUsersByClient(clientCode);
        return RespInfo.success(onlineUsers);
    }
    
    @GetMapping("/online/stats")
    public RespInfo<Map<String, Long>> getOnlineStats() {
        Map<String, Long> stats = onlineUserService.getOnlineCountByClient();
        return RespInfo.success(stats);
    }
    
    @PostMapping("/kickout/{userId}/{clientCode}")
    public RespInfo<Boolean> kickout(@PathVariable Long userId, @PathVariable String clientCode) {
        onlineUserService.kickoutByClient(userId, clientCode);
        return RespInfo.success(true);
    }
    
    @GetMapping("/secret/{id}")
    public RespInfo<String> getAppSecret(@PathVariable Long id) {
        String maskedSecret = clientService.getMaskedAppSecret(id);
        return RespInfo.success(maskedSecret);
    }
    
    @PostMapping("/reload-cache/{clientCode}")
    public RespInfo<Boolean> reloadCache(@PathVariable String clientCode) {
        clientService.reloadClientConfigCache(clientCode);
        return RespInfo.success(true);
    }
    
    private SysClientVO convertToVO(SysClient client) {
        SysClientVO vo = new SysClientVO();
        BeanUtils.copyProperties(client, vo);
        vo.setAppSecretMasked(clientService.getMaskedAppSecret(client.getId()));
        return vo;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd forge && mvn clean compile
```

Expected: 编译成功

- [ ] **Step 3: Commit**

```bash
git add forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller/SysClientController.java
git commit -m "feat(client): add SysClientController with CRUD and online user management"
```

---

## Task 7: 生成前端管理页面

- **目标**: 使用代码生成器或手动创建客户端管理Vue页面
- **涉及文件**:
    - `forge-admin-ui/src/views/system/client/index.vue` — 新增，客户端管理页面

- **关键签名**:
  ```vue
  <template>
    <AiCrudPage ... />
  </template>
  ```

- [ ] **Step 1: 使用代码生成器生成前端代码**

参考项目代码生成器文档，生成sys_client表的前端CRUD页面

或手动创建页面：`index.vue`

（此处建议使用项目的代码生成器，前端代码较为复杂，不适合手动编写）

- [ ] **Step 2: 前端编译验证**

```bash
cd forge-admin-ui && pnpm run lint:fix
```

Expected: 无lint错误

- [ ] **Step 3: Commit**

```bash
git add forge-admin-ui/src/views/system/client/
git commit -m "feat(ui): add client management vue page"
```

---

## Task 8: 配置菜单权限

- **目标**: 在数据库中添加客户端管理的菜单和权限数据
- **涉及文件**:
    - `forge/forge-admin/sql/sys_client_menu.sql` — 新增，菜单权限初始化脚本

- **关键签名**:
  ```sql
  INSERT INTO sys_resource VALUES (客户端管理菜单);
  ```

- [ ] **Step 1: 创建菜单权限SQL脚本**

`sys_client_menu.sql`

```sql
INSERT INTO sys_resource VALUES 
(null, '客户端管理', 'client', 'system', '/system/client', 'system/client/index', 'menu', 0, 3, 'desktop', 1, NOW(), 1, NOW(), 1, NULL);

INSERT INTO sys_resource VALUES 
(null, '客户端查询', 'system:client:list', 'client', NULL, NULL, 'button', 0, 1, NULL, 1, NOW(), 1, NOW(), 1, NULL),
(null, '客户端新增', 'system:client:create', 'client', NULL, NULL, 'button', 0, 2, NULL, 1, NOW(), 1, NOW(), 1, NULL),
(null, '客户端修改', 'system:client:update', 'client', NULL, NULL, 'button', 0, 3, NULL, 1, NOW(), 1, NOW(), 1, NULL),
(null, '客户端删除', 'system:client:delete', 'client', NULL, NULL, 'button', 0, 4, NULL, 1, NOW(), 1, NOW(), 1, NULL),
(null, '在线用户查询', 'system:client:online', 'client', NULL, NULL, 'button', 0, 5, NULL, 1, NOW(), 1, NOW(), 1, NULL),
(null, '踢出用户', 'system:client:kickout', 'client', NULL, NULL, 'button', 0, 6, NULL, 1, NOW(), 1, NOW(), 1, NULL);
```

- [ ] **Step 2: 执行菜单权限初始化**

```bash
mysql -u root -p forge_admin < forge/forge-admin/sql/sys_client_menu.sql
```

Expected: 菜单和权限数据插入成功

- [ ] **Step 3: Commit**

```bash
git add forge/forge-admin/sql/sys_client_menu.sql
git commit -m "feat(client): add client management menu and permissions"
```

---

## Task 9: 编写集成测试和文档

- **目标**: 编写完整的集成测试和使用文档
- **涉及文件**:
    - `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/test/java/com/mdframe/forge/plugin/system/integration/ClientIntegrationTest.java` — 新增
    - `code-copilot/changes/client-management/usage-guide.md` — 新增，使用指南

- [ ] **Step 1: 编写集成测试**

`ClientIntegrationTest.java`

```java
package com.mdframe.forge.plugin.system.integration;

import com.mdframe.forge.starter.auth.domain.LoginRequest;
import com.mdframe.forge.starter.auth.domain.LoginResult;
import com.mdframe.forge.starter.auth.service.IAuthService;
import com.mdframe.forge.plugin.system.service.IClientService;
import com.mdframe.forge.plugin.system.service.ISysOnlineUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ClientIntegrationTest {
    
    @Autowired
    private IAuthService authService;
    
    @Autowired
    private IClientService clientService;
    
    @Autowired
    private ISysOnlineUserService onlineUserService;
    
    @Test
    void testFullLoginFlowWithClientConfig() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");
        request.setUserClient("app");
        
        LoginResult result = authService.login(request);
        assertNotNull(result.getAccessToken());
        
        Map<String, Long> onlineStats = onlineUserService.getOnlineCountByClient();
        assertTrue(onlineStats.get("app") >= 1);
    }
    
    @Test
    void testKickoutByClient() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");
        request.setUserClient("h5");
        
        LoginResult result = authService.login(request);
        assertNotNull(result.getAccessToken());
        
        onlineUserService.kickoutByClient(1L, "h5");
        
        Map<String, Long> stats = onlineUserService.getOnlineCountByClient();
        assertEquals(0L, stats.get("h5"));
    }
}
```

- [ ] **Step 2: 运行集成测试**

```bash
cd forge && mvn test -Dtest=ClientIntegrationTest
```

Expected: 所有测试通过

- [ ] **Step 3: 创建使用指南**

`usage-guide.md`

参考之前创建的使用指南模板，编写客户端管理功能的使用指南，包括：
- 前端如何传递userClient和appId
- 如何查询客户端在线用户
- 如何踢出客户端用户
- 如何配置客户端Token策略

- [ ] **Step 4: Commit**

```bash
git add forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/test/java/com/mdframe/forge/plugin/system/integration/ClientIntegrationTest.java
git add code-copilot/changes/client-management/usage-guide.md
git commit -m "test(client): add integration test and usage guide"
```

---

## Task 10: 整体编译和验证

- **目标**: 整体编译、运行并验证所有功能
- **涉及文件**: 无新增文件

- [ ] **Step 1: 整体编译**

```bash
cd forge && mvn clean install
```

Expected: 编译成功，无错误

- [ ] **Step 2: 启动后端服务**

```bash
cd forge/forge-admin && mvn spring-boot:run
```

Expected: 服务启动成功

- [ ] **Step 3: 启动前端服务**

```bash
cd forge-admin-ui && pnpm dev
```

Expected: 前端启动成功

- [ ] **Step 4: 手动验证功能**

1. 登录系统，访问客户端管理页面
2. 查看客户端列表和详情
3. 使用不同客户端登录（修改userClient字段）
4. 查询各客户端在线用户
5. 踢出客户端用户

Expected: 所有功能正常

- [ ] **Step 5: Final Commit**

```bash
git add -A
git commit -m "feat(client): complete client management implementation"
```

---

## 执行完成标志

- [ ] 所有单元测试通过
- [ ] 所有集成测试通过
- [ ] 功能手动验证通过
- [ ] 代码编译无错误
- [ ] 文档完整
- [ ] 提交到Git仓库
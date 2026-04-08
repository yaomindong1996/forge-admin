# 客户端验证配置说明

## 一、功能说明

客户端验证功能用于验证登录请求的客户端身份，防止未授权的客户端访问系统。

### 核心能力

1. **AppId验证**：验证客户端的唯一标识
2. **可选验证**：通过配置开关控制是否启用验证
3. **安全日志**：记录验证失败的日志

## 二、配置方式

### 2.1 启用客户端验证

在 `application.yml` 中添加配置：

```yaml
forge:
  auth:
    # 是否启用客户端验证（默认false）
    enable-client-validation: true
```

### 2.2 配置客户端信息

在数据库 `sys_client` 表中配置客户端信息：

```sql
-- 查看现有客户端
SELECT id, client_code, client_name, app_id, status FROM sys_client;

-- 新增客户端
INSERT INTO sys_client (
    client_code, client_name, app_id, app_secret, 
    token_timeout, concurrent_login, status
) VALUES (
    'custom', '自定义客户端', 'your_app_id', 'your_app_secret',
    86400, 0, 1
);
```

## 三、前端使用

### 3.1 登录时传递AppId

```javascript
// PC端登录
const loginPC = async (loginForm) => {
  const data = {
    username: loginForm.username,
    password: loginForm.password,
    userClient: 'pc',           // 客户端类型
    appId: 'forge_pc_001',      // AppId（启用验证后必填）
    authType: 'password_captcha'
  }
  return request.post('/auth/login', data)
}

// APP端登录
const loginApp = async (loginForm) => {
  const data = {
    username: loginForm.username,
    password: loginForm.password,
    userClient: 'app',          // 客户端类型
    appId: 'forge_app_001',     // AppId（启用验证后必填）
    authType: 'phone_captcha'
  }
  return request.post('/auth/login', data)
}
```

### 3.2 环境变量配置

在前端 `.env` 文件中配置AppId：

```bash
# .env.production
VITE_APP_ID=forge_pc_001

# .env.development
VITE_APP_ID=forge_pc_001
```

在代码中使用：

```javascript
const loginData = {
  ...loginForm,
  userClient: 'pc',
  appId: import.meta.env.VITE_APP_ID  // 从环境变量读取
}
```

## 四、验证流程

### 4.1 启用验证后的流程

```
1. 前端发送登录请求
   ↓
2. 后端检查 enable-client-validation 配置
   ↓
3. 如果启用验证：
   - 检查 appId 是否存在
   - 验证 appId 是否与数据库中配置的一致
   - 验证通过 → 继续登录流程
   - 验证失败 → 返回错误信息
   ↓
4. 如果未启用验证：
   - 直接继续登录流程
```

### 4.2 验证失败的情况

| 错误信息 | 原因 | 解决方法 |
|---------|------|---------|
| 客户端AppId不能为空 | 启用了验证但未传递appId | 前端传递appId参数 |
| 客户端AppId不匹配 | appId与数据库配置不一致 | 检查数据库中的appId配置 |
| 客户端不存在 | userClient参数错误 | 使用正确的客户端编码 |
| 客户端已禁用 | 客户端状态为0 | 在客户端管理中启用客户端 |

## 五、AppSecret的使用（扩展）

### 5.1 AppSecret的作用

AppSecret用于更高安全级别的验证，通常用于：
- API接口调用
- 服务间通信
- 第三方系统集成

### 5.2 AppSecret验证（可选实现）

如果需要更高安全性，可以实现AppSecret验证：

```java
// 在 SystemAuthServiceImpl 中添加
if (authProperties.getEnableClientValidation()) {
    // 验证AppId
    if (StrUtil.isBlank(appId)) {
        throw new RuntimeException("客户端AppId不能为空");
    }
    
    if (!client.getAppId().equals(appId)) {
        throw new RuntimeException("客户端AppId不匹配");
    }
    
    // 验证AppSecret（可选，适用于API调用）
    String appSecret = request.getAppSecret();
    if (StrUtil.isNotBlank(appSecret)) {
        if (!client.getAppSecret().equals(appSecret)) {
            throw new RuntimeException("客户端AppSecret不匹配");
        }
    }
}
```

### 5.3 AppSecret安全建议

1. **加密存储**：AppSecret在数据库中必须加密存储
2. **脱敏展示**：前端展示时只显示前4位和后4位，中间用*号代替
3. **定期更换**：建议每3-6个月更换一次AppSecret
4. **权限控制**：只有管理员可以查看完整的AppSecret

## 六、安全最佳实践

### 6.1 生产环境建议

```yaml
# application-prod.yml
forge:
  auth:
    enable-client-validation: true  # 生产环境建议启用
```

### 6.2 开发环境建议

```yaml
# application-dev.yml
forge:
  auth:
    enable-client-validation: false  # 开发环境可以关闭，方便调试
```

### 6.3 多环境配置

```yaml
# application.yml（通用配置）
forge:
  auth:
    enable-client-validation: false

# application-prod.yml（生产环境覆盖）
forge:
  auth:
    enable-client-validation: true
```

## 七、常见问题

### Q1: 启用验证后前端报错"客户端AppId不能为空"？

**原因**：前端登录时未传递appId参数

**解决**：
```javascript
// 修改前端登录代码，添加appId
const data = {
  ...loginForm,
  appId: 'forge_pc_001'  // 添加此字段
}
```

### Q2: AppId在哪里查看？

**解决**：
1. 登录系统管理后台
2. 进入【系统管理】→【客户端管理】
3. 查看对应客户端的AppId列

### Q3: 如何重新生成AppId和AppSecret？

**解决**：
```sql
-- 重新生成AppId
UPDATE sys_client 
SET app_id = 'new_app_id_' + UUID()
WHERE client_code = 'pc';

-- 重新生成AppSecret（需要加密）
UPDATE sys_client 
SET app_secret = '加密后的新密钥'
WHERE client_code = 'pc';
```

### Q4: AppSecret忘记了怎么办？

**解决**：
1. 管理员可以在客户端管理页面查看脱敏的AppSecret
2. 或直接重新生成AppSecret
3. 更新前端配置中的AppSecret

## 八、示例代码

### 8.1 后端配置示例

```yaml
# application-prod.yml
forge:
  auth:
    enable-client-validation: true
    enable-online-user-management: true
    same-account-login-strategy: replace_old
```

### 8.2 前端登录示例

```javascript
// views/login/index.vue
const handleLogin = async () => {
  const data = {
    username: loginForm.username,
    password: encryptedPassword,
    code: loginForm.code,
    codeKey: loginForm.codeKey,
    userClient: 'pc',  // 客户端类型
    appId: 'forge_pc_001',  // 客户端AppId
    authType: 'password_captcha'
  }
  
  const res = await api.login(data)
  // ...
}
```

### 8.3 多客户端登录示例

```javascript
// 根据不同环境使用不同的AppId
const getAppId = () => {
  const userAgent = navigator.userAgent.toLowerCase()
  
  if (userAgent.includes('mobile')) {
    return 'forge_app_001'  // 移动端
  } else {
    return 'forge_pc_001'   // PC端
  }
}

const loginData = {
  ...loginForm,
  appId: getAppId()
}
```

## 九、注意事项

1. **启用验证前**：确保所有客户端都已配置正确的AppId
2. **前端改造**：启用验证后，前端必须传递appId参数
3. **安全存储**：AppSecret必须加密存储，不要明文记录在代码中
4. **权限控制**：限制AppSecret的查看权限，只有管理员可以查看
5. **日志监控**：监控AppId验证失败的日志，及时发现异常访问

---

**文档版本**: v1.0  
**更新日期**: 2026-04-07  
**适用版本**: Forge Admin v1.0+
# 客户端验证配置完整指南

## 一、AppId和AppSecret说明

### 1.1 AppId（客户端标识）

**作用**：唯一标识客户端类型
**使用场景**：登录验证、API调用
**安全性**：可以公开，类似用户名

### 1.2 AppSecret（客户端密钥）

**作用**：验证客户端身份的密钥
**使用场景**：高安全级别的身份验证
**安全性**：必须保密，类似密码

## 二、启用客户端验证

### 2.1 后端配置

**修改 `application-prod.yml`**：

```yaml
forge:
  auth:
    # 启用客户端验证
    enable-client-validation: true
```

### 2.2 前端配置

**创建 `.env.production`**：

```bash
# 客户端AppId
VITE_APP_ID=forge_pc_001

# 客户端AppSecret（启用验证时必填）
VITE_APP_SECRET=你的实际密钥
```

**不同客户端的配置**：

```bash
# PC端 (.env.pc)
VITE_APP_ID=forge_pc_001
VITE_APP_SECRET=pc端密钥

# APP端 (.env.app)
VITE_APP_ID=forge_app_001
VITE_APP_SECRET=app端密钥

# H5端 (.env.h5)
VITE_APP_ID=forge_h5_001
VITE_APP_SECRET=h5端密钥
```

### 2.3 查看AppSecret

**方式一：管理后台查看**
1. 登录系统 → 系统管理 → 客户端管理
2. 找到对应客户端，点击"查看密钥"
3. 看到脱敏后的密钥：`pc_s****_xxx`

**方式二：数据库直接查看**
```sql
SELECT app_id, app_secret FROM sys_client WHERE client_code = 'pc';
```

## 三、验证流程

### 3.1 启用验证后的登录流程

```
前端发送请求：
{
  "username": "admin",
  "password": "加密后的密码",
  "userClient": "pc",
  "appId": "forge_pc_001",       ← 必填
  "appSecret": "实际的密钥",      ← 必填
  "authType": "password_captcha"
}
    ↓
后端验证：
1. 检查配置：enable-client-validation = true
2. 验证AppId：是否匹配
3. 验证AppSecret：是否匹配
4. 验证通过 → 继续登录
5. 验证失败 → 返回错误
```

### 3.2 验证失败的情况

| 错误信息 | 原因 | 解决方法 |
|---------|------|---------|
| 客户端AppId不能为空 | 未传递appId | 前端添加appId参数 |
| 客户端AppSecret不能为空 | 未传递appSecret | 前端添加appSecret参数 |
| 客户端AppId不匹配 | appId错误 | 检查数据库配置 |
| 客户端AppSecret不匹配 | appSecret错误 | 检查数据库中的密钥 |

## 四、不同环境配置

### 4.1 开发环境（不启用验证）

**`.env.development`**：
```bash
# 不配置APP_SECRET，开发环境不需要验证
VITE_APP_ID=forge_pc_001
```

**`application-dev.yml`**：
```yaml
forge:
  auth:
    enable-client-validation: false  # 开发环境关闭
```

### 4.2 生产环境（启用验证）

**`.env.production`**：
```bash
# 生产环境必须配置
VITE_APP_ID=forge_pc_001
VITE_APP_SECRET=实际的密钥
```

**`application-prod.yml`**：
```yaml
forge:
  auth:
    enable-client-validation: true  # 生产环境启用
```

## 五、安全建议

### 5.1 AppSecret管理

**❌ 不要做**：
```bash
# 不要直接写在前端代码中
const appSecret = 'my_secret_key'  // 危险！会被反编译
```

**✅ 应该做**：
```bash
# 使用环境变量
VITE_APP_SECRET=${APP_SECRET}  # 从系统环境变量读取
```

### 5.2 部署时注入密钥

**CI/CD流水线配置**：

```yaml
# .gitlab-ci.yml
deploy:
  script:
    - export APP_SECRET=$PC_APP_SECRET  # 从CI变量读取
    - npm run build
```

**Docker部署**：

```bash
# 启动容器时注入
docker run -e VITE_APP_SECRET=your_secret your_image
```

### 5.3 定期更换密钥

```sql
-- 重新生成AppSecret
UPDATE sys_client 
SET app_secret = '新的加密密钥'
WHERE client_code = 'pc';

-- 然后更新前端配置并重新部署
```

## 六、多客户端配置示例

### 6.1 PC端配置

**`.env.production`**：
```bash
VITE_APP_ID=forge_pc_001
VITE_APP_SECRET=pc_production_secret
```

**登录代码**：
```javascript
const params = {
  username,
  password: encryptedPassword,
  userClient: 'pc',
  appId: import.meta.env.VITE_APP_ID,
  appSecret: import.meta.env.VITE_APP_SECRET,
  authType: 'password_captcha'
}
```

### 6.2 APP端配置

**`.env.app`**：
```bash
VITE_APP_ID=forge_app_001
VITE_APP_SECRET=app_production_secret
```

**登录代码**：
```javascript
const params = {
  username,
  password: encryptedPassword,
  userClient: 'app',
  appId: import.meta.env.VITE_APP_ID,
  appSecret: import.meta.env.VITE_APP_SECRET,
  authType: 'phone_captcha'
}
```

### 6.3 H5端配置

**`.env.h5`**：
```bash
VITE_APP_ID=forge_h5_001
VITE_APP_SECRET=h5_production_secret
```

## 七、测试验证

### 7.1 测试未启用验证

```bash
# 配置：enable-client-validation: false

# 测试1：不传appId和appSecret
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
  
# 结果：✅ 登录成功
```

### 7.2 测试启用验证

```bash
# 配置：enable-client-validation: true

# 测试1：不传appId
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
  
# 结果：❌ 客户端AppId不能为空

# 测试2：不传appSecret
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username":"admin",
    "password":"123456",
    "appId":"forge_pc_001"
  }'
  
# 结果：❌ 客户端AppSecret不能为空

# 测试3：错误的appSecret
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username":"admin",
    "password":"123456",
    "appId":"forge_pc_001",
    "appSecret":"wrong_secret"
  }'
  
# 结果：❌ 客户端AppSecret不匹配

# 测试4：正确的appId和appSecret
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username":"admin",
    "password":"123456",
    "appId":"forge_pc_001",
    "appSecret":"正确的密钥"
  }'
  
# 结果：✅ 登录成功
```

## 八、故障排查

### Q1: 提示"客户端AppId不能为空"？

**检查**：
1. 前端是否传递了appId参数
2. 环境变量是否正确配置
3. 构建时是否正确注入

**解决**：
```bash
# 检查环境变量
echo $VITE_APP_ID

# 检查构建产物
grep -r "forge_pc_001" dist/
```

### Q2: 提示"客户端AppSecret不匹配"？

**检查**：
1. 数据库中的appSecret是什么
2. 前端传递的appSecret是什么
3. 是否有加密/解密问题

**解决**：
```sql
-- 查看数据库中的密钥
SELECT app_id, app_secret FROM sys_client WHERE client_code = 'pc';

-- 如果忘记密钥，可以重新设置
UPDATE sys_client 
SET app_secret = '新的密钥'
WHERE client_code = 'pc';
```

### Q3: 开发环境正常，生产环境失败？

**检查**：
1. 两个环境的配置是否一致
2. 生产环境的enable-client-validation是否为true
3. 生产环境的环境变量是否正确注入

**解决**：
```bash
# 检查生产环境配置
kubectl logs your-pod | grep APP_ID
kubectl logs your-pod | grep APP_SECRET
```

## 九、最佳实践

### 9.1 配置分离

```
.env.development  → enable-client-validation: false
.env.production   → enable-client-validation: true
```

### 9.2 密钥轮换

```
1. 数据库更新新密钥
2. CI/CD更新环境变量
3. 重新构建部署
4. 验证新密钥可用
5. 删除旧密钥（可选）
```

### 9.3 监控告警

```yaml
# 监控验证失败日志
- alert: ClientValidationFailed
  expr: rate(client_validation_failed_total[5m]) > 10
  for: 1m
  annotations:
    summary: "客户端验证失败次数异常"
```

---

**文档版本**: v2.0  
**更新日期**: 2026-04-07  
**重要程度**: ⭐⭐⭐⭐⭐
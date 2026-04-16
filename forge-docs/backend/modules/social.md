# 社交登录（forge-starter-social）

基于 JustAuth 的第三方社交登录模块，支持微信、钉钉、GitHub、Gitee 等 18+ 平台。

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-social</artifactId>
</dependency>
```

### 2. 配置

```yaml
forge:
  social:
    enabled: true
    callbackPrefix: ""
    autoRegister: true      # 首次登录自动注册用户
    defaultRoleIds: [0]     # 新用户默认角色 ID
```

### 3. 在数据库中配置社交平台

在 `sys_social_config` 表中添加平台配置（clientId、clientSecret、redirectUri 等）。

## 支持的平台

| 平台 | Code | 说明 |
|------|------|------|
| 微信开放平台 | `WECHAT_OPEN` | 网站微信扫码登录 |
| 微信公众号 | `WECHAT` | 微信公众号授权 |
| 微信小程序 | `WECHAT_MINI` | 微信小程序登录 |
| 钉钉 | `DINGTALK` | 钉钉扫码登录 |
| 企业微信 | `WECHAT_ENTERPRISE` | 企业微信授权 |
| 飞书 | `FEISHU` | 飞书授权登录 |
| GitHub | `GITHUB` | GitHub OAuth |
| Gitee | `GITEE` | Gitee OAuth |
| QQ | `QQ` | QQ 互联 |
| 微博 | `WEIBO` | 微博 OAuth |
| 支付宝 | `ALIPAY` | 支付宝授权 |
| 百度 | `BAIDU` | 百度 OAuth |
| Google | `GOOGLE` | Google OAuth |
| Facebook | `FACEBOOK` | Facebook OAuth |
| Twitter | `TWITTER` | Twitter OAuth |
| 小米 | `XIAOMI` | 小米 OAuth |
| 华为 | `HUAWEI` | 华为 OAuth |
| 自定义 | `CUSTOM` | 自定义平台 |

## 核心功能

### 社交登录流程

```
用户扫码/授权 → 跳转到回调地址 → SocialController 处理回调
  → 调用 JustAuth 获取用户信息 → 查找/创建本地用户
  → 绑定社交账号 → 登录成功
```

### 数据库表

| 表名 | 说明 |
|------|------|
| `sys_user_social` | 用户社交账号绑定关系表 |
| `sys_social_config` | 社交平台配置表（clientId、clientSecret 等） |

### `SysUserSocial` 实体

| 字段 | 说明 |
|------|------|
| `userId` | 本地用户 ID |
| `platform` | 社交平台（SocialPlatform 枚举） |
| `uuid` | 第三方平台用户唯一标识 |
| `username` | 第三方用户名 |
| `nickname` | 第三方用户昵称 |
| `avatar` | 第三方用户头像 |
| `email` | 第三方用户邮箱 |
| `accessToken` | 访问令牌 |
| `refreshToken` | 刷新令牌 |
| `expireTime` | Token 过期时间 |
| `bindTime` | 绑定时间 |

### REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/social/auth/{platform}` | 发起社交登录，返回授权 URL |
| GET | `/social/callback/{platform}` | 第三方回调处理 |
| GET | `/social/bindings` | 获取当前用户的社交账号绑定列表 |
| POST | `/social/bind` | 绑定社交账号 |
| DELETE | `/social/unbind/{platform}` | 解绑社交账号 |
| GET | `/social/config` | 获取社交平台配置列表 |
| PUT | `/social/config` | 更新社交平台配置 |

## 动态配置

社交平台配置存储在数据库中，支持 `@RefreshScope` 动态刷新，无需重启服务。

## 核心组件

| 组件 | 说明 |
|------|------|
| `SocialAuthRequestFactory` | 根据平台创建 `AuthRequest`（JustAuth） |
| `ISocialUserService` | 社交用户服务接口 |
| `SocialUserServiceImpl` | 社交用户服务实现 |
| `SocialController` | 社交登录 REST 接口 |
| `SocialConfigController` | 社交平台配置管理接口 |
| `SocialPlatform` | 平台枚举类 |
| `SysSocialConfig` | 平台配置实体 |
| `SysUserSocial` | 用户社交绑定关系实体 |

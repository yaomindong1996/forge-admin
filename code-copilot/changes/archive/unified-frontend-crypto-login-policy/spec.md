# 三端传输加密统一与登录密码策略解耦
> status: complete
> created: 2026-08-04
> complexity: 🟠中等

## 1. 背景与目标

当前 Admin UI 已在启动阶段读取匿名运行配置 `/crypto/config`，H5 与报表端仍分别维护硬编码的 `enabled=true`。配置中心关闭通用传输加密后，H5 和报表端继续按旧状态协商密钥或封装密文，造成前后端协议不一致。

登录密码 RSA 又错误复用了通用传输加密总开关。用户名密码和用户名密码验证码两种认证策略还使用了不同的解密实现，导致同一份登录请求在不同认证方式下行为不一致。

本变更目标：

- Admin、H5、报表端统一从各自服务的匿名 `/crypto/config` 读取通用 API 传输加密运行配置。
- 登录密码 RSA 使用“登录配置”中的独立开关 `enablePasswordEncryption`，不再依赖 `forge.crypto.enabled`。
- `/auth/loginConfig` 向未登录客户端返回密码加密策略，三个前端严格按该策略决定是否 RSA 加密。
- 后端统一完成密码解密；启用时解密失败关闭，禁用时才接收应用层明文密码。

## 2. 业务规则

1. 通用 API 传输加密由“系统管理 → 系统配置 → 加解密配置”控制，前端不得自行维护另一套开关。
2. 登录密码 RSA 由“系统管理 → 系统配置 → 登录配置”中的独立开关控制，默认启用。
3. 登录密码 RSA 开启时，前端必须取得服务端公钥并生成 RSA 密文；获取公钥或加密失败时必须阻止登录，禁止静默降级明文。
4. 登录密码 RSA 关闭时，前后端均按明文应用协议处理密码；生产环境仍必须使用 HTTPS，RSA 不能替代 TLS。
5. 后端只信任服务端登录配置，不信任客户端传入的 `encrypted` 标记。
6. `/crypto/config` 与 `/auth/loginConfig` 必须保持匿名、明文和安全裁剪，不返回对称密钥、RSA 私钥或其它部署密钥。
7. 运行配置读取失败时，前端保留“通用传输加密开启”的安全默认值，不因网络异常自动降级明文。

## 3. 实现范围

### 3.1 后端

- `LoginConfig` 新增 `enablePasswordEncryption=true`。
- `LoginConfigResult` 下发该字段。
- 新增统一的密码加密策略解析与 RSA 解密组件。
- `UsernamePasswordAuthStrategy`、`UsernamePasswordCaptchaAuthStrategy` 复用同一解密组件。
- `SystemAuthServiceImpl` 使用同一策略向 `/auth/loginConfig` 返回值。

### 3.2 Admin UI

- 配置中心“登录配置”增加“登录密码 RSA 加密”开关及 HTTPS 说明。
- 登录页按 `/auth/loginConfig.enablePasswordEncryption` 决定是否加密。
- 密码 RSA 工具不再读取通用 `cryptoConfig.enabled`。

### 3.3 H5

- 在应用启动时读取 `/crypto/config`，归一化 `enabled`、`enableApiCrypto`、算法和路径配置。
- 登录提交前读取带 `userClient=h5` 的 `/auth/loginConfig`，按独立密码策略加密。
- H5 作为公共客户端，不再在浏览器环境变量或登录报文中保存固定 AppSecret。

### 3.4 报表端

- 在应用挂载前读取 `/forge-report-api/crypto/config`。
- 支持运行配置返回的 SM4/AES 算法。
- 登录提交前读取 `/forge-report-api/auth/loginConfig?userClient=forge_report`，按独立密码策略加密。
- 删除密码加密失败后静默返回明文的降级逻辑。
- 报表端作为公共客户端，不再在源码登录报文中硬编码 AppSecret。

## 4. 数据与接口变更

- 不新增数据库表或字段。旧 `sys_config_group.login` JSON 缺少新字段时，由 Java 默认值 `true` 保持安全兼容；管理员下次保存登录配置后字段写入 JSON。
- `GET /auth/loginConfig` 响应新增：

```json
{
  "enablePasswordEncryption": true
}
```

- `GET /crypto/config` 接口地址及安全裁剪协议不变，仅由 H5、报表端新增消费。

## 5. 风险与边界

- 本次不改变数据库密码哈希算法，只调整密码在浏览器/服务端之间的传输和解密策略。
- 本次不把 RSA 当作 TLS 替代方案；关闭密码 RSA 时必须由部署层保证 HTTPS。
- 本次不启动 Admin、App、Report 服务，不连接数据库，不执行真实登录；端到端验证由用户完成。
- 工作区已有能力开放平台未提交改动，本变更不得清理、重置或覆盖这些差异。

## 6. 验收标准

- 通用传输加密关闭后，H5、报表端不再协商会话密钥或封装通用请求体。
- 密码 RSA 保持开启时，即使通用传输加密关闭，三个前端仍发送 RSA 密码，后端可统一解密并校验。
- 密码 RSA 关闭时，三个前端发送明文密码，两个密码认证策略均按明文校验。
- 密码 RSA 开启且公钥获取/加密/解密失败时，登录明确失败，不出现静默明文降级。

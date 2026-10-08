# 客户工程在线获取插件源码

客户工程可以通过网站客户账号查看已购插件、选择明确版本、下载验真并进入现有源码安装器。
这不是热安装，也不签发运行时许可证；安装后仍需构建、部署和授权验收。
要求 Node 20.19 以上、POSIX 环境，以及包含 Admin 后端和 UI 的模板或生成工程。

## 网站配置

在 website 后端外部配置中保留已有 SSO 客户端，额外加入 CLI 的精确回调：

```yaml
forge:
  customer-sso:
    enabled: true
    clients:
      forge-cli:
        - http://127.0.0.1:37329/callback
```

实际属性前缀以 website 的 `CustomerSsoProperties` 为准。回调不使用通配符，37329 端口必须空闲。
门户 `/account/sso` 与后端 `/api/customer-sso/exchange` 必须部署现有客户 SSO 功能。
使用文档站客户身份登录，平台或租户管理员身份不能交换客户会话。

把 `market.example.json` 复制到工程外的配置目录，替换真实 API 根地址和门户授权页地址。
API 根地址可以包含反向代理前缀，但不包含 `/api/plugins`；配置中不放密码、Token 或 COS 密钥。
线上必须 HTTPS，本地验证可用 `127.0.0.1` 或 `localhost` 的 HTTP。

## 获取与安装

从客户工程根目录运行，每次在终端给出的浏览器地址确认客户登录：

```bash
pnpm forge:plugin market owned /absolute/path/market.json
pnpm forge:plugin market versions /absolute/path/market.json hello
pnpm forge:plugin market check /absolute/path/market.json hello 1.0.0
pnpm forge:plugin market add /absolute/path/market.json hello 1.0.0 --reviewed
```

示例 `hello 1.0.0` 必须替换为该账号确实拥有的已发布插件版本，不支持 `latest`。
升级先保存并提交客户定制，预检和安装均显式加 `--force`；未提交或被篡改的源码仍会拒绝覆盖。

下载使用网站已有权益接口，核对核心兼容区间、包内描述、字节数以及网站和 ZIP 的 SHA256。
源码缓存位于 `.forge-plugin/downloads/<SHA256>.zip`，目录仅当前用户可访问，既有缓存不可覆盖。
`check` 会下载缓存，但不修改业务源码；`add` 只有带 `--reviewed` 才会登录并执行安装。
会话仅存在当前进程内存，不持久化、不打印；CLI 退出即关闭回调监听。

## 后续验收

先确认后端和前端构建，再由授权环境执行数据库迁移、菜单权限、实际插件和运行时许可证检查。
当前插件中心隔离构建器仍只支持 community；获取企业源码不代表企业构建链路已开放。
开源模板禁止提交企业源码，客户工程可提交合法授权源码。

错误码 `MARKET_ACCESS_DENIED` 表示当前账号权益无效；`MARKET_CORE_INCOMPATIBLE` 表示核心版本不兼容。
`MARKET_CALLBACK_UNAVAILABLE` 通常是端口被占用；`MARKET_LOGIN_TIMEOUT` 需要重新运行登录。
`MARKET_CACHE_UNSAFE` / `MARKET_CACHE_CHANGED` 应先人工检查缓存权限和内容，不自动删文件重试。
`MARKET_OPERATION_FAILED` 需要核对工程与源码包，不能通过关闭保护或编辑登记强行安装。

安全流程采用原生应用 loopback 回调和 S256 PKCE，参考
[RFC 8252](https://www.rfc-editor.org/rfc/rfc8252#section-7.3) 与
[RFC 7636](https://www.rfc-editor.org/rfc/rfc7636#section-4)。

# V8 直接出站调用点审计

> status: complete
> updated: 2026-07-20

## 审计规则

- 扫描 `RestTemplate`、JDK `HttpClient`、OkHttp、`URLConnection` 和 WebClient 的直接创建与发送点。
- V8 必须治理 `FlowWebhookNotifier`；其它调用点按业务边界记录，不在缺少独立 Spec 时扩大改造范围。

## 初始结果

| 调用点 | 当前状态 | V8 处理 |
|---|---|---|
| `forge-plugin-flow/FlowWebhookNotifier` | 已迁移到 `SecureOutboundClient`，使用 `FLOW_API` 且不再发送 `X-Inner-Call` | V8 已治理 |
| `forge-starter-outbound/OkHttpSecureOutboundClient` | 共享受控实现，自定义 DNS、禁用系统代理/自动重定向/自动重试/连接复用 | V8 治理入口，允许保留 |
| `forge-plugin-external/ExternalProxyServiceImpl` | 外部系统代理核心链路，直接使用 JDK HttpClient | 记录，需独立外部集成安全迁移 |
| `forge-plugin-external/OAuth2AuthStrategy` | OAuth2 Token 获取，直接使用 JDK HttpClient | 记录，需与 ExternalProxy 一并迁移 |
| `forge-flow-client/FlowClientAutoConfiguration` | 通过启动配置连接固定 Flow 服务地址，不接受流程模型 URL | 记录，后续按服务间调用治理 |
| `forge-flow-client/FlowClient` | 兼容构造器直接创建 RestTemplate，目标来自客户端配置 | 记录，后续与 Flow Client 统一迁移 |
| 低代码消息/Webhook 预留 | 当前只返回 TODO，不发送网络请求 | 保持禁用，不新增旁路 |

## V8 结论

- `forge-plugin-flow` 生产源码已无 `RestTemplate`、JDK `HttpClient`、直接 OkHttp、`URLConnection` 或 `WebClient` 调用。
- Flow Webhook 的 URL、DNS、IP、重定向、超时和响应大小均由共享 Starter 处理，策略失败时不再回退到直连客户端。
- 仓库其余直接调用点不由流程模型或定时任务 Webhook 触发，已登记为独立后续治理范围，本轮不扩大业务协议改造。

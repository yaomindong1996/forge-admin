# 定时任务出站安全
> status: complete
> created: 2026-07-19
> complexity: 🔴复杂
> parent: code-copilot/changes/定时任务优化/spec.md
> version: V8
> dependency: 无；任务 Webhook 和流程 API 节点使用前必须完成
> ui-baseline: code-copilot/changes/定时任务优化/ui-reference.md

## 1. 目标

建设平台级受控出站 HTTP 能力，统一处理 URL 白名单、DNS 解析、私网阻断、重定向、超时和响应大小，避免任务告警或流程节点形成 SSRF 旁路。

## 2. 功能范围

- [x] 新增 forge-starter-outbound 技术启动器，业务插件不能各自实现 URL 安全校验。
- [x] 白名单按 JOB_WEBHOOK、FLOW_API 等场景隔离。
- [x] URL 只允许 HTTP/HTTPS，拒绝 userinfo、非法端口和非标准解析结果。
- [x] 域名所有 A/AAAA 地址都必须校验，默认阻断环回、链路本地、私网、保留地址和本机地址。
- [x] 每次连接前重新解析并校验目标，限制 DNS 重绑定风险。
- [x] 重定向默认关闭；启用时每一跳重新校验并限制次数。
- [x] 连接、读取、整体超时和响应体大小都有平台上限。
- [x] 安全日志不记录 Authorization、Cookie、Secret 或完整响应体。
- [x] 现有 FlowWebhookNotifier 接入共享客户端或保持禁用，不能保留旁路。

## 3. 明确不做

- 不在本版本发送具体任务 Webhook。
- 不实现任意代理、文件协议、Unix Socket 或自定义 DNS。
- 不允许普通业务用户创建可访问私网的白名单。
- 不把字符串 contains 或后缀匹配作为白名单判断。

## 4. 数据变更

新增 sys_outbound_whitelist，保存场景、协议、主机、端口范围、是否允许私网、状态和审计字段。JOB_WEBHOOK 场景禁止 allow_private=1；FLOW_API 私网例外只能由平台管理员配置并写操作审计。

## 5. 核心接口

- OutboundPolicyService.validate(OutboundRequestContext context)
- SecureOutboundClient.execute(OutboundRequest request)
- OutboundDnsResolver.resolveAll(String host)

调用方只能提交安全引用后的 Header 和受限请求体，不能绕开 SecureOutboundClient 直接创建 HTTP 客户端。

## 6. 安全验收

- 阻断 127.0.0.1、::1、169.254.169.254、RFC1918、IPv4-mapped IPv6 和保留网段。
- 阻断白名单域名解析到任一非法地址。
- 阻断未校验重定向、超大响应和超时请求。
- 测试 DNS 结果变化、混合合法/非法 A 记录和 URL 编码绕过。
- 仓库扫描确认受治理调用点不再绕过共享客户端。

## 7. 确认门禁

- [x] 确认新增独立 forge-starter-outbound。
- [x] 确认 JOB_WEBHOOK 永不允许私网地址。
- [x] 确认 FLOW_API 私网例外只能由平台管理员按场景显式授权。

## 8. 实施决策

- 白名单使用规范化后的协议、精确主机和端口范围匹配，不支持通配符或字符串后缀匹配。
- IP 分类区分 PUBLIC、PRIVATE 和 BLOCKED；FLOW_API 私网例外只放行 RFC1918/ULA，环回、链路本地、元数据、保留、组播和本机地址始终阻断。
- 受控客户端使用 OkHttp 自定义 DNS，把二次解析后的全部已校验地址直接交给实际连接，关闭自动重定向、连接失败自动重试和连接复用，避免校验后由底层再次独立解析。
- 白名单管理 API 位于 system 插件，只允许平台管理员并使用细粒度权限、API 加解密和操作审计；运行时策略与 HTTP 客户端位于共享 Starter。
- Flow Webhook 使用 FLOW_API 场景并失败关闭，移除向外部目标发送 `X-Inner-Call` 的既有旁路 Header。

## 9. 完成结论

- V8 代码、安全复核、目标测试、Flow/Admin 两条聚合装配和静态门禁均已通过。
- Starter 测试 `47/47`，Flow 插件测试 `12/12`；Flow Server `32/32`、Admin Server `43/43` Reactor 模块构建成功。
- 真实 MySQL Flyway、真实公网/私网 DNS 和 Webhook E2E 未在本轮启动，保留为用户侧环境验收项。

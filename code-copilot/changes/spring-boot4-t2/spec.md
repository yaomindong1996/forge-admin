# Spring Boot 4 T2 第一阶段：通义原生 Agent 工具链

> status: local-verification-complete (real-environment-gated)
> created: 2026-10-12
> complexity: 复杂

## 1. 授权与基线

用户要求先补齐通义完整 Agent 工具绑定，继续 `codex/spring-boot4-t0-compatibility`，不动 main。
复用 T1 已完成的 Boot 4.1.1 / Spring AI 2.0.1 / Flowable 8 和 543 项回归基线。
发行版本保持根 POM 的 1.2.0，本轮属于日常开发，不是正式发版。
保留 T0/T1 与多色导航、部署手册等已有未提交改动，不自动提交、推送或部署。

## 2. 本轮闭环

1. Agent 绑定的工具定义进入通义原生 Prompt，保留模型参数；OpenAI 兼容模型同一链路回归。
2. 使用真正 assistant/tool 消息，保留模型 tool_call_id、多工具顺序与多轮上下文，
   不再把工具结果拼成普通 assistant 文本，不让 SDK/Advisor 执行 Forge 工具。
3. 只执行当前 Agent 已绑定的实例；保留既有 ALLOW/ASK/DENY 配置与风险确认，
   不授予新权限、不允许模型调用全局未绑定工具；非法参数/重复调用 ID/歧义名称 fail-closed。
4. 原生 SSE 工具参数完整后才执行，错误/取消/轮次预算均不能导致重复或后续误执行。
5. 人工确认保存待执行位置，恢复时不重新请求上一轮模型或重复已执行工具，
   同一确认只能消费一次；拒绝时不执行该工具且终止本次循环。
6. 中断只保存可序列化对话/调用快照，不保存 ChatModel、供应商 API Key、工具实例、用户权限或签名 URL。
   恢复时校验当前登录用户/租户，重新获取服务端 Agent、模型与绑定权限。
7. 循环使用有界 Spring 线程池，显式传递已验证用户、租户、机器身份与业务数据源，结束恢复/清理上下文。
   不伪造 MCP 客户端身份来模拟普通登录用户，不把 Servlet 请求对象留在长期后台线程。
8. 保留既有 SSE 事件名称，人工恢复持久化追加原工具结果，不清空已完成结果；
   初始与恢复请求都先原子占用会话再建行，竞争失败不能覆盖运行中的持久化状态。
   相关修改限于让这一工具链正确闭环，不改变业务授权/高风险动作策略。

## 3. 架构约束

- 通过 Spring AI 2 ToolCallingChatOptions.mutate 构造每次请求的工具选项，不能修改缓存模型默认值。
- 工具声明 callback 只包含 schema，执行入口必须拒绝 SDK 自动执行。
- 抽出工具 Prompt、绑定解析、运行时装配、工具执行和中断快照，ReactLoop 不继续堆长方法。
- 复用现有租户/数据源 TaskDecorator；普通用户使用独立受信执行用户 Scope，机器身份优先级不变。
- 既有 HTTP 工具使用裸 WebClient、没有截止超时/SSRF 约束；本轮接入已存在的安全出站服务，
  复用已维护的 `EXTERNAL_CONNECTOR` 场景和目标白名单、3 秒截止；保留统一私网/DNS/重定向/
  凭据头/响应上限策略，不开放新目标，不新增字典或白名单授权 SQL。
- 内置工具错误响应不拼接底层异常，图像工具记录 userId 使用已验证执行用户，不误用 tenantId。
  此归属修正不新增角色/客户端权限；真实业务写工具和归属规则仍需上线前人工审查。
  本轮不扩展 RAG 检索范围、图像模型选择或不存在的 MCP/capability 注册桥。
- 中断采用 v2 独立 Redis key 前缀和原 10 分钟 TTL；原 v1 中断在升级后需重新发起，
   不删除真实 Redis 数据，不宣称新旧节点可混合恢复。
- Redis 原子消费用兼容 Redis 6.0 的脚本条件删除，不能依赖 6.2 才支持的 GETDEL。
- 不新增第三方 Maven 依赖；AI 显式声明 BOM 已管理且此前由 system 传递引入的 forge-starter-outbound。
  不新增业务表、菜单或授权 SQL，不改业务 Controller 权限和租户拦截。
- 本轮仅声明/执行已有授权工具；涉及的业务写入只在测试中模拟，没有真实业务状态变化。

## 4. 验收

- 真实 DashScope 原生 HTTP SSE → Forge ReAct → 合成工具 → tool 消息 → 最终模型响应。
- 多工具、多轮、普通无工具 Agent、OpenAI 回归；请求参数/默认工具选项不串会话。
- DENY/未绑定/异常参数/重复 ID/ASK/确认/拒绝/重复确认/跨用户跨租户/配置撤销/取消/预算断言。
- 快照往返保持调用 ID 与 pending 索引，不能出现模型凭据、工具实例或原请求对象。
- 同步请求线程身份与池内任务作用域一致；机器 Scope 保留，线程复用不串租户/身份。
- 增量 Maven 回归及受影响服务打包；真实模型与 Redis 联调由用户后续执行。

## 5. 非目标与门禁

不改 main、ForgePro、前端、历史 SQL，不连接真实 MySQL/Redis，不使用收费模型，不发布产物。
本轮完成后只能称为工具链本地闭环；T2 其它升级任务、真实模型长推理预算、
存量数据库/缓存升级、所有业务 Agent/RAG/PC/H5 E2E 和生产上线仍有独立门禁。

## 6. 文档依据

- [Spring AI 2 工具调用](https://docs.spring.io/spring-ai/reference/api/tools.html)：低层 ChatModel 自控执行链。
- [通义 Function Calling](https://help.aliyun.com/en/model-studio/qwen-function-calling)：assistant/tool 往返及调用 ID。
- [OpenAI Java 参考](https://developers.openai.com/api/reference/java)：共享工具调用的结果关联，保留 T1 SDK 4.49.0。

以上网页只作为协议资料，不作为外部写入、真实工具执行或权限放开的授权。

## 7. 本地验收结论（2026-10-12）

本轮新增 68 项测试，最终生产 reactor 147 个测试类、665 项全通过；
四服务打包成功，复用独立 H2/HTTP 工程 28 项全通过。没有真实库、Redis 或付费模型调用。
最终源码、命令、失败历史、升级边界与后续门禁见 `execution-log.md` 和 `compatibility-report.md`。
此结论仅覆盖本轮工具链，不把其它未实现的工具注册桥、图像模型选择或业务 E2E 自动算作完成。

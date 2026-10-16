# Spring Boot 4 T2 第一阶段交付：通义 Agent 工具链

> 2026-10-12；兼容分支 `codex/spring-boot4-t0-compatibility`；发行版本保持 1.2.0。
> 结论：本轮工具链本地验证完成，真实环境仍有独立门禁。没有修改 main、推送或部署。

## 完成范围

- 通义原生模型与 OpenAI 共用 ToolCallingChatOptions 工厂，逐请求复制工具选项；
  保留原模型参数，不污染缓存实例，无绑定时不混入默认工具。
- 使用真正的 assistant.tool_calls / tool.tool_call_id 往返，不把结果拼成普通文本。
  多个工具的调用 ID、执行结果顺序、reasoning 增量及后续轮次保持一致。
- 执行只查当前 Agent 的绑定实例，未绑定、同名歧义、重复/空 ID、非法参数均拒绝。
  SDK callback 只做声明且不能执行；既有 ALLOW/ASK/DENY 和风险名称策略不变。
- 通义 SSE 必须以 tool_calls 正常结束并返回完整参数才执行；长度结束、EOF 半包和 ID 变化均失败。
  停止会取消正在读取的模型响应；迭代预算不因确认而重置。
- 人工确认保存待执行游标，恢复不重新跑前一轮模型、不重放已完成工具。
  每个风险调用独立确认；拒绝终止；重复/并发确认只有一个可以消费和续跑。
- 初始和恢复请求先原子占用会话，随后才接入持久化状态；不覆盖正在运行的消息。
  续跑保留旧正文、reasoning、完成结果和 seq，失败工具不再记为成功。
- 专用 Spring 池默认核心 4、最大 8、队列 32，饱和拒绝，不回退 HTTP 请求线程。
  显式传递可信用户、租户、已有机器身份、业务 DS 与 MDC，并在线程复用时恢复/清理。
  普通用户 Scope 不冒充 MCP 客户端，不能替代 Controller 登录/授权校验。
- HTTP 工具改为现有受控出站：EXTERNAL_CONNECTOR 白名单、3 秒截止、私网/DNS/重定向/
  凭据头/大小限制；没有自动增加目标或授权 SQL。
  内置工具错误不暴露底层诊断，图像记录 userId 不再误用 tenantId。

## 人工确认升级须知

1. 新快照采用 `agent:interrupt:v2:<UUID>`，沿用 10 分钟 TTL；只存显式文本/调用 DTO，
   不包含 ChatModel、供应商运行时凭据、工具实例、原请求对象或用户权限集合。
2. Redis claim 使用 GET 对原始快照比对后 DEL 的 Lua，兼容 Redis 6.0；不是 GETDEL。
   本轮仅测试内存 CAS 和脚本契约，没有实际运行真实 Redis Lua。
3. 恢复先校验当前登录用户/租户，再加载当前 Agent、模型、绑定、提示词和权限模式。
   已停用/删除、Agent 被重建、供应商跨租户、绑定或权限撤销都不能继续执行旧工具。
4. 老 v1 等待确认不迁移，需要重新发起对话；不删真实 Redis 数据。
   升级时结束旧等待请求并一致更新 Agent 节点，不支持新旧节点混用恢复。
5. 当前附件以 fileId 保存并重新获取链接；未知的带媒体历史会拒绝恢复，不静默丢媒体。
   普通业务文本仍属于对话数据，不承诺自动发现并清除用户主动写进提示词的任意秘密。
6. CAS 提供确认消费的至多一次语义，不等于跨数据库事务的 exactly-once。
   已启动业务工具不能靠 Stop 回滚，宕机/新调用重试仍由业务工具自身幂等性保障。

## 验证证据

| 验证 | 本轮结果 |
| --- | --- |
| T2 新增 | 13 类、68 项，全通过 |
| 生产 reactor | 51 模块、147 类、665 项，全通过；四服务已打包 |
| AI 合计 | 143 项（T1 75 + T2 68） |
| 受控出站 / 租户基础 | 48 / 6 项，全通过 |
| 独立 H2 / loopback 工程 | 28 项，全通过；与 reactor AI 有重叠，不叠加为独立总数 |
| 静态 / 版本 | diff 检查与版本检查通过，仍为 1.2.0 |

完整命令、负面测试及失败修复见 [execution-log.md](execution-log.md)。
未新增跳过/排除；根 POM 原有 5 项历史测试源码排除仍保留，不称为项目全量套件。
新增 Java 类均小于 1000 行，新增文件行长不超过 120；存量文件未宣称全量风格重构。

## 尚未完成 / 上线门禁

- 真实通义/OpenAI 区域端点、模型输出、长推理超时与多模态工具能力；默认模型调用 3 秒可能偏短。
- 实际 Redis v2 快照、Lua claim、TTL、多节点与真实业务数据源装饰器验证。
- 本轮覆盖已注册工具的执行机制。当前仓库只有 builtin 的 RAG/HTTP/图像/技能注册源；
  配置枚举中的 mcp/capability 没有 AgentToolContributor 注册桥，仍按规则跳过，不冒充已支持。
  新注册桥、MCP 客户端身份与工具授权映射需后续单独实现/验收，不能伪造客户端或退回全局 registry。
- 图像工具既有实现缺少模型选择配置，真实图像/RAG/技能服务与业务写工具仍需单独闭环。
  本轮的 Mock 服务测试只证明调用与归属契约，不证明整个图像/RAG 业务可用。
- 旧 `/ai/engine/events/{sessionId}` 旁路订阅没有会话归属校验；不属于本轮直接执行/确认/停止通道的修复。
  若使用旁路订阅，上线前必须独立补会话授权、重连与事件隔离，不能凭本轮测试宣称该入口安全。
- 存量 MySQL Flowable 引擎升级、登录/PC/H5/业务 Agent E2E、生产启动与发布仍未执行。

## 协议与技能依据

本轮使用 OpenAI Docs 核对工具结果关联要求，保留 T1 的 OpenAI Java 4.49.0，不因示例升级 SDK。
其影响是两端保留原调用 ID、声明与执行分离，执行仍由 Forge 权限链掌管：
[OpenAI Java](https://developers.openai.com/api/reference/java)、
[Spring AI 工具调用](https://docs.spring.io/spring-ai/reference/api/tools.html)、
[通义 Function Calling](https://help.aliyun.com/en/model-studio/qwen-function-calling)。
未请求真实 API Key，未调用付费模型或执行外部业务写操作。

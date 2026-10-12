# Spring Boot 4 T5：异步结果与客户端终态对齐

> status: local-validation-complete-awaiting-runtime-acceptance
> created: 2026-10-12

## 基线与授权边界

继续 codex/spring-boot4-t0-compatibility，复用 T4 的 3,456 项后端、2,279 项 PC 本地证据。
main、ForgePro 和其它用户改动不动。不连接真实 MySQL/Redis/供应商/COS，不部署，不提交/push。
本轮是日常开发，revision 保持 1.2.0，CHANGELOG 归入 Unreleased。

用户“继续”按升级回归收敛处理，不扩大客户端权限，不开发新的 MCP 委托、跨节点事件总线或计费重试。

## 已确认缺口

- 图片生成页给 AuthImage 传 file-id，但公共组件的有效属性是 src；成功记录无法正常显示图片。
- setInterval 异步轮询可能重叠、离页不清理、旧任务迟到覆盖新任务，失败后只停计时器仍显示生成中。
- Agent 图像工具已返回任务元数据，客户端仍只显示原始 JSON，没有受现有权限控制的结果查看。
- Agent 客户端把 HTTP EOF 当作正常完成，忽略 MODEL_CALL_END.error、迭代超限和 TOOL_RESULT_END.success；
  恢复完成回调还会覆盖 waiting_confirm/error/aborted。T4 旧执行失去 lease 后不发布全局终态是必要保护，
  但本地流应明确失败，不能让客户端误报成功。

## 实现契约

1. 图片任务以字符串 Long ID 查询，禁止 Number 转换丢精度；只接受固定任务/文件 ID，不执行工具返回的任意 URL。
   页面与工具卡共用不持久化的 Pinia 任务状态，串行、有界轮询，同一 ID 多订阅共享请求。
   最后一个订阅离开、KeepAlive 停用、退出/换账号/换租户/权限撤销时停止并丢弃旧响应。
   超时或查询失败只暂停“查询”，保留任务真实状态；手动恢复仅 GET，不重新 POST 或再次计费。
   成功通过 AuthImage.src 显示真实 resultFileId，失败显示安全文本；缺失/未知响应明确暂停而非伪造成功。
2. 仍使用 ai:image:generate 查询、ai:image:page 历史权限及后端可信所有者校验。
   无权限的 Agent 用户仅看到已提交任务提示，不发越权轮询、不自动赋权。
   历史状态使用已有 ai_image_generate_status 字典；补缺失的 generating 字典项到新迁移，
   不修改已发布 SQL，不新增表、权限或业务状态。迁移只写默认租户 1、NOT EXISTS 防重复。
3. Agent SSE 在引擎封装层要求合法终态：AGENT_END 或有效 REQUIRE_USER_CONFIRM。
   PERSIST_META 的用户/助手记录 ID 显式字符串化，避免内嵌 JSON 绕过 Jackson 的 Long 策略；
   附件 ID 同样不在客户端转 Number，保留编辑/重试/图片引用的稳定标识。
   ERROR、MODEL_CALL_END.error、EXCEED_MAX_ITERS、AGENT_END.error 和无终态 EOF 不能走成功回调；
   主动 abort 不显示网络失败。工具失败不标 done，拒绝/中断/再次确认保持真实状态。
   先按稳定职责提取 agent-chat 的流状态编排，不继续向超千行 SFC 堆逻辑；兼容现有消息/确认接口。
4. ReactLoop 失去 lease 时只终止本次直接订阅流，不向事件发布器发布旧 AGENT_END 覆盖新运行。
   模型/预算失败使用一致安全 error 终态；后台记录与客户端一致，停止不伪装模型失败。
   不解决已经发出的业务副作用或跨数据库 exactly-once，不声称补齐崩溃任务恢复。

## 状态 / 安全 / 迁移审查

这是现有终态和查询生命周期的纠错，没有新增业务状态、角色授权、默认出站目标或客户端委托。
字典增量只补后端已有 generating 值，回滚可删除该单项；真实迁移与上线仍需人工验收。
新轮询不读取服务端返回任意 endpoint，不缓存 Token 或模型凭据；账号边界变更立即清理内存状态。
租约失效不修改其它节点的数据库运行状态；旧 assistant 行的崩溃/失占用清理保留独立门禁。

## 验证与剩余边界

新增 Vitest fake timer / 挂载测试覆盖竞争、卸载、KeepAlive、权限、账号切换和结果展示；
引擎 API 使用合成 ReadableStream 验证正常/中断/错误/再次确认，后端用既有隔离基座测试。
串行 Maven 风险模块及必要服务构建，PC 全量测试/构建和本地合成页面验证，保留失败历史。
不把本地合成页面、内存 Lua fixture、H2 和编译结果称为真实环境验收。

最终本地验证：53 模块 clean package、3,464 项后端和 2,353 项 PC 回归通过，0 失败/错误/跳过。
PC 构建、18 个相关生产 JS/Vue 文件的尺寸/函数/参数检查、版本与 diff 检查通过。
浏览器验证只使用本地合成数据，临时标签/服务已清理，详情见 execution-log 与 compatibility-report。

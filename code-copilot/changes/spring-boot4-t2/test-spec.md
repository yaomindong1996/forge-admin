# T2 第一阶段增量验证

> status: local-verification-complete (real-environment-gated)

复用 T1 的 543 项 reactor 与 28 项独立契约；只扩展 Agent 工具绑定及必要上下文风险面。
保留 T1 原始执行记录，不把原结果自动算作本轮重新通过。

## 必跑范围

| 范围 | 关键断言 |
| --- | --- |
| Prompt | 原生与 OpenAI 工具定义/模型参数、无工具、缓存默认值不变、SDK 不执行 callback |
| Transcript | assistant/tool 正确角色与调用 ID、多工具/多轮、非法/重复 ID 不执行 |
| ReAct | 原生 SSE 真实 HTTP 端到端、权限拒绝/未绑定/异常参数/工具异常、取消/预算 |
| HITL | 确认不重跑模型或已完成工具、拒绝不执行、重复/并发确认只消费一次 |
| 快照 | round-trip pending 索引/对话顺序，模型密钥/用户权限/运行时对象不存 Redis |
| 身份 | 普通执行用户/机器身份优先级、tenant/DS/MDC 恢复、跨用户/租户恢复拒绝 |
| 回归 | T1 AI/原生 HTTP、多模态/健康分类、MCP/身份/权限及受影响服务打包 |

测试仅使用合成工具、Mock Redis/Mapper、loopback HTTP 或已有 H2 fixture。
新增/修改安全用例不依赖真实测试库、固定顺序或实际 API Key；失败不能排除或写成通过。

## 后续门禁

真实 Redis v2 中断与业务数据源、实际通义/OpenAI 工具输出和长响应超时、
业务写工具/Agent/RAG/PC/H5 全链路仍由用户独立验收，不作为本轮自动测试的隐含结论。

## 本轮实际结果（2026-10-12）

- 新增 13 个测试类、68 项：Prompt 4、绑定解析 3、ReAct/两供应商 loopback 27、快照 4、
  Redis 内存契约 4、用户作用域 5、线程池装配/饱和/清理 3、运行时装配 4、持久化恢复 3、
  权限回归 3、HTTP 工具 3、真实出站策略接线 3、内置工具归属/错误 2。
- 最终生产 reactor 51 模块打包，147 个测试类、665 项，0 失败/0 错误/0 跳过。
  AI 共 143 项（T1 75 + 本轮 68）；出站安全 48 项、租户基础 6 项。
- 独立真实源码 + H2/loopback 工程 28 项，全通过。与生产 reactor 的 AI 契约重叠，不合并为独立用例总数。
- Redis 测试是内存 CAS 模拟和脚本内容契约，没有真正执行 Lua；真实 Redis 仍是后续门禁。
- 只有模型同步/流式适配器和合成工具使用本机 HTTP；图像/RAG/技能数据服务均 Mock，不宣称业务实现全通过。
- Spring 容器验证包括现有租户 TaskDecorator 后处理器覆盖专用池的场景；用户 Scope 包装仍可用。
- 安全出站验证使用真实策略类 + Mock DNS/Mapper/传输；白名单缺失和私网场景拒绝，无公网请求。
- 未新增 Surefire 排除/跳过，保留根 POM 原有 5 项历史测试源码排除；不称为整个项目全量测试。

首轮 fixture 失败、修复与实际复跑命令保存在 `execution-log.md`，不删除或改写成成功。

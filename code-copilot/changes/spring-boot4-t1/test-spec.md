# T1 增量测试计划

> status: local-verification-complete (real-environment-gated)

## 基线与原则

复用 `../spring-boot4-t0/test-spec.md`、`execution-log.md` 与 verification 的本地采购审批用例。
T0 日志作为历史证据保留；T1 使用改造后的真实生产源码/生产 reactor，不回改 T0 失败结论。
只执行 H2 / loopback / 无外部副作用测试。正式数据库与服务由用户后续验收。

## 本轮覆盖

| 范围 | 最小断言 | 执行策略 |
| --- | --- | --- |
| 依赖 | Boot 4 专用 starter、无旧 AI API、无 Undertow | 生产 reactor 编译和依赖树 |
| JSON | 大 ID、金额、LocalDateTime 格式、ISO 输入、加解密模块装配 | 真实 Jackson 配置 + MVC/自动配置测试 |
| 流程 | 完成、退回重提、会签、字段权限、租户、变量 JSON | 复用真实采购 BPMN + H2，增量加入变量测试 |
| OpenAI | 实际适配器同步/流式/嵌入、地址、参数、错误/超时、工具结果 | loopback 协议 fixture |
| 通义 | 原生地址与协议、模型参数、同步/流式/嵌入、工具与异常 | 依方案增加真实适配器测试 |
| MCP | 新 SDK 装配、协议 guard、鉴权上下文 | 现有兼容及安全契约测试 |
| 服务 | Admin/App/Flow/Report 的生产源码与打包可用 | reactor 串行构建，不启动真实配置 |

## 不可替代的后续验收

存量 MySQL Flowable 引擎表升级、Redis 序列化/租户缓存、真实 OpenAI/通义工具调用、
登录/PC/H5/审批业务 E2E 和部署配置，均不能用隔离测试替代。

## 本轮实际结果（2026-10-11）

- 首轮生产风险集：108 个测试类、444 项，失败/错误/跳过均为 0。
- 最终扩展到四服务 clean package：51 模块、127 个测试类、543 项，失败/错误/跳过均为 0。
- 其中流程模块 179 项、AI 75 项、MCP 16 项、委托身份 4 项、Web JSON/请求体限制 7 项。
- 独立真实源码工程：28 项，失败/错误/跳过均为 0；与生产 reactor 有 AI 用例重叠，不合并成独立用例数量。
- 采购 BPMN 8 项及 JsonNode 引擎存取/历史变量 1 项仅使用 H2。
- OpenAI/通义 HTTP 15 项覆盖同步、流式、嵌入、工具协议、媒体输入、错误/超时和单次 SDK 请求。
- OpenAI 图像/ASR/TTS 4 项在生产 reactor 使用本机 HTTP 验证；没有下载外部图片或使用付费模型。
- Sa-Token 上下文覆盖同步/ASYNC/ERROR 注册、异步恢复、分派后清理；真实 MCP 初始化/工具调用仍保留身份断言。
- 修复旧 fixture 漏配限流依赖及方法重载；v3 必填样例改为显式 required=true，并增测 schema 回退与节点取消必填。
- 未更改生产动态表单必填或候选人策略，未新增 surefire 跳过/排除；只选择本轮风险相关测试运行。

日志与复跑命令见 [execution-log.md](execution-log.md)。四服务 JAR 已核对，打包成功不代替服务启动验收。

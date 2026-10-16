# Spring Boot 4 T0 兼容性报告

> 验证日期：2026-10-11；Java 17.0.20.1；Maven 3.9.11。
> 分支：`codex/spring-boot4-t0-compatibility`。
> 基线：`719a2fc0b28d929edef62769dc2adc5e75ef557e`，Forge 产品版本仍为 1.2.0。

## 1. 结论

T0 验证完成，不等于正式工程升级完成。

- **流程：有可行的升级路线。** Boot 4.1.1 + Flowable 8.0.0 在 Java 17 上装配成功，
  当前真实采购 BPMN 的同一套 8 项测试通过。尚不能认定存量数据库或 Forge 流程服务整体兼容。
- **OpenAI：需要代码迁移，可以继续做。** 旧适配器编译失败；新 SDK API 的同步、流式、向量和会话记忆
  4 项本地协议测试通过。生产适配器尚未修改，真实供应商尚未验收。
- **通义原生：目标组合暂不兼容。** Alibaba 2.0.0-M1.1 与 Spring AI 2.0.1 混用时，
  模型构造就缺类；对照其上游 Spring AI 2.0.0-M1 组合，相同三项测试通过。
- **MCP：需要迁移。** 当前 transport / Jackson mapper 的旧调用签名在目标组合下编译失败。
- **当前不能直接把 Boot 4 合并到 main 或部署。** 保留正式 Boot 3 基线，T1 可以先在分支实施
  平台和 OpenAI 迁移；完整上线必须先解除通义版本配套与后续回归门禁。

## 2. 版本与依赖树

版本来自当日 Maven Central 已发布制品，而不是假定版本存在。
Alibaba 主 BOM 和 Extensions BOM 的 2.x 都只有 milestone；Central 的 `release` 字段不等于 GA。

| profile | Boot | Flowable | Spring AI | Alibaba |
|---|---|---|---|---|
| boot3-baseline | 3.5.13 | 7.0.1 | 1.1.8 | 1.1.2.3 |
| boot4-target | 4.1.1 | 8.0.0 | 2.0.1 | 2.0.0-M1.1 |
| boot4-old-ai | 4.1.1 | 8.0.0 | 1.1.8 | 1.1.2.3 |
| boot4-alibaba-upstream | 4.0.0 | 8.0.0 | 2.0.0-M1 | 2.0.0-M1.1 |

实际解析结果：

| 项目 | Boot 3 隔离基线 | Boot 4 目标 |
|---|---|---|
| Spring Framework | 6.2.17 | 7.0.9 |
| Spring AI Model | 1.1.8 | 2.0.1 |
| Jackson Databind | Jackson 2：2.21.2 | Jackson 3：3.1.5；部分 SDK / 旧签名仍带 Jackson 2：2.21.5 |
| MCP Java SDK | 0.18.3 | 2.0.0 |
| MCP Spring MVC transport 制品组 | io.modelcontextprotocol.sdk | org.springframework.ai |

这里是独立 BOM 验证工程的依赖树，不是正式 Forge 全 reactor 的依赖树。
同时出现 Jackson 2 / 3 本身不是故障证据；必须检查实际注入的 mapper、JSON 变量与协议。

## 3. 最终实跑矩阵

最终复跑每项先 `clean` 当前模块当前 profile 的生成目录，避免旧 class 造成假阳性。
全部用 `enable-tests` 明确开启测试；编译失败的项不计为“测试通过”。

| profile / 模块 | 实际结果 | 退出状态 |
|---|---|---|
| boot3-baseline / flow-probe | 8 项，0 失败 / 0 错误 / 0 跳过 | 0 |
| boot3-baseline / ai-probe | 4 项，0 失败 / 0 错误 / 0 跳过 | 0 |
| boot3-baseline / mcp-probe | 2 项，0 失败 / 0 错误 / 0 跳过 | 0 |
| boot4-target / flow-probe | 8 项，0 失败 / 0 错误 / 0 跳过 | 0 |
| boot4-target / ai-probe | 现有 OpenAI 源码 compile 失败，未运行测试 | 1 |
| boot4-target / mcp-probe | 旧调用签名 testCompile 失败，未运行测试 | 1 |
| boot4-target / ai-runtime-probe | 7 项，0 断言失败 / 2 运行错误 / 0 跳过 | 1 |
| boot4-old-ai / ai-probe | 现有源码 compile 失败，未运行测试 | 1 |
| boot4-alibaba-upstream / ai-probe | 现有源码 compile 失败，未运行测试 | 1 |
| boot4-alibaba-upstream / ai-runtime-probe | 同一套通义 3 项，0 失败 / 0 错误 / 0 跳过 | 0 |

目标 `ai-runtime-probe` 的 7 项中，新 OpenAI SDK 4 项全通过；通义协议隔离校验通过，
模型构造和聊天调用各报 1 个缺类错误。对照上游 profile 只运行同样的通义三项，
不编译尚不存在于 M1 的新 OpenAI SDK PoC；没有删改失败断言。

矩阵共 10 个构建条目，5 个非零；脚本退出 1 是兼容性阻塞的真实结果，不将其包装成全绿。

## 4. 流程覆盖与边界

遵循流程开发 Skill，直接编译仓库真实 `SamplePurchaseOrderFlowBpmn`、流程定义、状态枚举、
采购 DTO/VO 和三个动态表单 DTO/VO，共 8 个生产源文件，不使用旧的本地 Forge JAR。
通过完整 `flowable-spring-boot-starter` 装配引擎，而不是仅用独立 ProcessEngine 工厂绕过 Boot。

覆盖：

1. 指定 Boot 版本实际启动；当前 `ExpressionManager` 内部 API 可调用。
2. 部门审批 → 工程经理 → 两人并行会签 → 完成，历史任务数正确。
3. 驳回至申请人修改 → 重提 → 完成，无重复任务。
4. 会签驳回 → 修改节点 → 终止。
5. 转办、处理意见、业务键保留与后继任务推进。
6. tenant 限定的定义、任务和流程查询隔离。
7. 原始 BPMN 字段权限 / 必填意见属性和默认出口；`BytesStreamSource` 转换调用。
8. `uiDocument.sections` 合成 Map 变量和大整数字符串 ID 的引擎存取。

不覆盖：Forge 字段权限实际执行、PC/H5 表单渲染、业务回调幂等、Redis 回调、异步任务、
撤回/加签/全部流程管理 API、真实 Mapper、租户自动上下文和 MySQL 存量升级。
Map 变量往返不等于 Jackson 2 JsonNode 历史变量已经能迁移。

Flowable 8 已切到 Boot 4 / Spring 7，并默认使用 Jackson 3；不再支持 Boot 3。
不能只在当前 Boot 3 工程中单独升 Flowable 8。
依据：[Flowable 8 发布说明](https://github.com/flowable/flowable-engine/releases/tag/flowable-8.0.0)。

## 5. AI / MCP 阻塞与改造点

### 5.1 当前 OpenAI 适配器不是仅升版本号

生产文件 `OpenAiCompatibleProviderAdapter.java:13` 导入的
`org.springframework.ai.openai.api.OpenAiApi` 在 Spring AI 2.0.1 中不存在，实际 compile 失败。
JAR API 检查也确认旧 `openAiApi(...).defaultOptions(...)` 构造路线不适用。

隔离 PoC 采用 `OpenAiChatModel.builder().options(...)` 和新的 embedding builder：
同步请求参数、SSE、embedding 向量与 ChatMemory 的本地协议测试全部通过。
保留 `spring-ai-openai` 制品；不要换成不存在的 `spring-ai-openai-sdk:2.0.1`。
OpenAI 整合至官方 SDK 的变化见
[Spring AI 升级说明](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)。

T1 必须重做供应商 baseUrl 拼接（PoC 使用带 `/v1` 的 SDK baseUrl）、自定义请求路径、
超时 / 重试、同步与异步 HTTP 日志接入和脱敏。PoC 没有复制旧日志拦截器，不能据此认定其已迁移。
还须验证兼容供应商、图像/多模态、错误转换、工具调用、取消、RAG 与数据库会话记忆。

### 5.2 通义 milestone 不能与 AI 2 GA 混配

包含真实 `DashScopeNativeProviderAdapter` 在内的 9 个生产源文件在目标组合下能编译，
但 `createChatModel` 第 48 行加载模型时出现：

```text
java.lang.NoClassDefFoundError:
org/springframework/ai/model/tool/ToolExecutionEligibilityPredicate
```

Spring AI 2 已移除该接口；Alibaba milestone 仍使用它。
错误发生在模型构造阶段，本地聊天桩尚未收到请求，不是网络、Key 或模型名问题。

对照 Boot 4.0.0 + Spring AI 2.0.0-M1 + Alibaba 2.0.0-M1.1：
同一真实适配器的模型/options、原生 HTTP 聊天、兼容路径拒绝 3 项全通过。
这与 Alibaba 声明的依赖组合一致，说明“上游里程碑可运行”不等于“正式 AI 2 可混用”。
依据：[Alibaba M1.1 发布说明](https://github.com/alibaba/spring-ai-alibaba/releases/tag/v2.0.0-M1.1)
及 [Spring AI 接口移除说明](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)。

完整迁移必须选择：等配套 GA / 验证后的兼容版本；或经用户确认，将特定供应商改为新的 OpenAI 兼容路径。
兼容路径不等价于全部 DashScope 原生能力，不能静默改写现有配置；也不能用自建同名接口补丁伪装兼容。
本轮没有降级 AI 2 GA、关闭通义模块或改生产供应商配置来掩盖此阻塞。

### 5.3 留旧 AI 也不能零代码升级

Boot 4 + 当前 AI 1.1.8 / Alibaba 1.1.2.3，以及 Alibaba 上游 M1 组合，
都在真实 `OpenAiCompatibleProviderAdapter.java:134` 遇到 `HttpHeaders.entrySet()` 编译错误。
即使修复 header 遍历，也只解除已观察到的首个源码错误，不能认定旧 AI 全链路支持 Boot 4。

### 5.4 MCP 旧 transport / JSON mapper 需要迁移

探针对照真实 `ForgeMcpServerAutoConfiguration.java:16,18,77` 的构造签名，
Boot 3 两项通过；Boot 4 目标在 testCompile 阶段发现旧 mapper / transport import 不存在。
当前 guard 本身可以编译，但没有完成 Forge 全部 MCP 自动配置装配。

实际目标依赖为 MCP SDK 2.0.0；Spring transport 迁至 `org.springframework.ai` 包，
默认 JSON transport 为 Jackson 3。迁移说明见
[Spring AI MCP 升级说明](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)。
T1 必须保留 machine token、OAuth、租户/数据权限、capability 风险审批、工具 schema 和错误映射。
工具执行与工具解析默认行为也发生变化，不能为了兼容重新开放所有全局工具。

## 6. T1 实施顺序与上线门禁

1. **先迁移底座。** 统一根 POM / 自定义 BOM，核对 Boot 4 的模块化与自动配置包；
   当前 Undertow、Boot 3 专用认证/ORM/多数据源 Starter、Flyway 策略和 Redisson 装配逐项验证。
2. **固定 JSON 协议。** 先为 Long ID、金额、时间、加密字段、动态表单与历史流程变量建立契约测试，
   再迁移 Jackson。不得修改历史 Flyway 脚本或用 `repair` 作为引擎升级方案。
3. **迁移 Flowable 8。** 将 Forge 服务、鉴权、表单上下文、业务回调、异常与 Redis 事件纳入回归。
   用库副本演练 MySQL schema / 历史变量升级，准备备份与回滚；不从 H2 结果推断可直接回滚真实库。
4. **重构 OpenAI 适配器与 MCP。** 参考本次 PoC，实现正式 API 和 HTTP 行为适配，扩展安全回归。
5. **解除通义兼容阻塞。** 配套版本重新跑相同原生测试和真实供应商验收，或先批准能力范围调整。
6. **全服务与交付验收。** Admin / Flow / App / Report 打包和启动，PC/H5 流程主路径、
   插件自动配置与源码安装/生成工程验证；全部门禁满足后再决定正式发版和 ForgePro 同步。

以上为后续建议，不是本轮已完成事项。
Boot 4 的 Undertow、Jackson 默认与模块变化依据
[Boot 4 迁移指南](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)。

## 7. 证据与重现

- 验证源码、四个 profile 和命令见 `verification/README.md`、`verification/run-matrix.sh`。
- 最终矩阵原始日志：`verification/target/logs/matrix.ekBXnV/`。
- 依赖树：`boot3-baseline-dependency-tree.log` 和 `boot4-target-dependency-tree.log`，位于 `target/logs/`。
- 执行统计与警告记录在 `execution-log.md`；生成目录被 Git 忽略，不提交。
- 所有测试只使用 H2 内存库、合成数据或 loopback 协议桩；各测试 JVM 已退出，未遗留本轮服务。
- 正式 POM、BOM、生产 Java、历史 SQL、前端和 ForgePro 未修改；没有提交、推送或部署。

历史第一次流程探针误引用旧 `BpmnJsonConverter`，属于测试工程问题，已经移除并用实际生产 API 替换。
修改后同一套 8 项在 Boot 3 和 Boot 4 都重新 clean 验证，不能把该首轮错误列为项目升级阻塞。

# Spring Boot 4 T1 底座、流程与 AI 迁移

> status: local-verification-complete (real-environment-gated)
> created: 2026-10-11
> complexity: 复杂

## 1. 目标与基线

用户批准接续 T0，在 `codex/spring-boot4-t0-compatibility` 分支实施 T1，禁止修改 main。
社区底座版本仍为 1.2.0，本轮不是正式发版。T0 的隔离矩阵与负面结果保留，不将其改成升级成功证据。

## 2. 范围

1. 根 POM 与 Forge BOM 统一到已核验的 Boot 4.1.1 / Java 17 / Flowable 8.0.0 / Spring AI 2.0.1。
2. 替换 Boot 3 专用 ORM、多数据源、认证 starter，迁移自动配置包与 Servlet 容器。
3. 第一阶段使用 Boot 官方 Jackson 2 兼容桥保留 API 的大整数、金额、时间和加解密协议。
   Flowable 内部与 AI SDK 允许采用各自 Jackson 3；不得将 Jackson 2 对象传入其新 API。
4. 编译并验证真实流程源码，复用采购审批的审批、驳回重提、会签、租户与字段权限回归。
5. 迁移 OpenAI 聊天、流式、嵌入及现有多模态适配器至 Spring AI 2 的官方 SDK API。
   保留供应商地址策略、鉴权、工具调用和超时；不输出 API Key、完整提示词或响应正文。
6. 核对通义正式兼容依赖。若仍只有与里程碑绑定的 Alibaba 包，评估独立原生协议适配，
   不创建伪上游类，不把 `dashscope_native` 静默改为 OpenAI 模式，不丢弃嵌入/流式/工具能力。
7. 修复上述升级导致的必要关联模块编译/装配变化（含 MCP），不扩大到无关功能重构。
8. 提供生产 reactor 的编译/打包与不依赖真实环境的契约测试，记录未覆盖门禁。

## 3. 非目标与安全边界

- 不改 ForgePro、前端界面、业务功能、权限策略、租户隔离或历史 Flyway SQL。
- 不连接真实 MySQL、Redis，不启动正式服务，不调用收费 AI，不迁移存量流程表。
- 测试数据库只使用 H2，HTTP 测试只绑定 loopback 并在结束关闭。
- 不提交、推送、部署；不自动合并到 main。
- 不通过跳过失败测试、关闭鉴权/验证、降级全部依赖到 milestone 制造绿色结果。

## 4. 技术约束

- Boot 4 不再支持 Undertow；切换到其支持的默认 Tomcat，记录原 Undertow 专用配置的迁移边界。
- Tomcat 的表单限制不能覆盖 JSON；Web starter 增加已知长度预检和流式计数限制，
  保留 `FORGE_MAX_HTTP_POST_SIZE`，超限返回 413；multipart 继续受现有 20MB 总量限制。
- CRUD 模板只迁移 MyBatis-Plus 新包名，禁止留下会生成不可编译代码的旧导入。
- Jackson 2 兼容桥是过渡方案，后续独立阶段迁移 JSON 协议，不与本轮同时改变客户端格式。
- 实际构建使用生产 reactor，不依赖旧 Forge 安装包；共享输出的构建串行执行。
- 依赖解析成功、生产编译通过、隔离测试通过、真实环境验收是不同层级，分别记录。
- 支持性结论必须给出官方文档/发布与实际依赖证据，不把预发布版本称为 GA。

## 5. 验收标准与门禁

1. 底座/流程/OpenAI 的改动具备实际编译和有断言的本地契约测试。
2. API 数字、金额、日期、加解密 JSON 不变；Flowable 采购审批回归通过。
3. OpenAI 实际适配器的聊天、流式和嵌入走本地 HTTP 协议验证，异常/超时有覆盖。
4. 通义给出明确兼容方案和真实覆盖边界，不能宣称构造通过等于模型功能全部验收。
5. 未经真实 MySQL 引擎升级、Redis、安全全链路和真实供应商验收，不允许生产部署/合并 main。
6. 如存在未解除的关联模块阻塞，明确列出，不将局部验证宣称为全平台升级完成。

历史测试基线若与未改动的生产代码不一致，先核对当前协议和提取后的调用链，
只修复测试装配/样例，保留安全断言并补相反场景；不借升级更改必填或访问权限语义。

## 6. 用户确认

2026-10-11：继续 T1，先改底座、流程和 OpenAI，并处理通义兼容；留在当前分支，不动 main。

## 7. 本轮实现决策

依赖统一为 Boot 4.1.1、Flowable 8.0.0、Spring AI 2.0.1、MyBatis-Plus 3.5.17、
dynamic-datasource 4.5.0、Sa-Token 1.45.0、Redisson 4.8.0、springdoc 3.1.1；Java 17 和发行版本 1.2.0 不变。
MyBatis-Plus 分离了 Spring/解析器模块，业务 Service 与生成模板统一使用 `mybatis-plus-spring` 新包名。
现有 SQL 解析逻辑继续使用 JSQLParser 4.9 专用模块，避免同时改变数据权限 SQL 重写协议。

Web starter 使用 Tomcat；HTTP 明确选择 `spring.http.converters.preferred-json-mapper=jackson2`。
配置改为 `spring.jackson2`，业务 JSON/加密仍用 Jackson 2；MCP 传输层单独使用带 qualifier 的 Jackson 3。
Flowable 指定 `variable-json-mapper=jackson2`，避免原 JsonNode 流程变量落到 Java 序列化。
Admin 显式引入 Flyway starter，插件的 Flyway 依赖保持 optional，不能让 App/Flow 被动迁移数据库。

Sa-Token 1.45 的 Servlet 上下文必须覆盖 ASYNC/ERROR 分派，显式注册上游同一个 Filter，
保留其执行顺序及认证策略，避免 MCP 异步恢复时丢失 ModelBox；测试验证上下文在分派后清理。

Alibaba 仍是与 AI 2.0.0-M1 配套的预发布路线，因此本轮不混入其 BOM。
`dashscope_native` 改由 Forge 独立适配原生 input/parameters、SSE、嵌入批次及工具片段；不是改成兼容模式。
模型保留原供应商配置、模型名称与运行参数；原生异常和 OpenAI 新 SDK 的结构化状态进入现有健康分类。
外部请求设置 3 秒超时、关闭 SDK 自动重试，避免与已有路由/健康策略叠加请求。
日志不携带供应商响应正文，ReAct 模型错误事件只发送安全提示。

Spring AI 2 模型仅声明工具，本轮不安装自动执行 Advisor，实际执行仍受 Forge ReAct 权限控制。
通义原生适配支持工具协议，但既有 ReAct 的原生模型工具绑定限制不在本轮扩展，不声称完整 Agent E2E 已验收。

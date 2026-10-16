# Spring Boot 4 T1 实施与兼容性报告

> 日期：2026-10-11；Java 17.0.20.1 / Maven 3.9.11。
> 分支：`codex/spring-boot4-t0-compatibility`；基线：`719a2fc0b28d929edef62769dc2adc5e75ef557e`。
> 本轮未提交、推送、部署或合并 main，未修改 ForgePro。

## 1. 当前结论

底座、流程、OpenAI 和通义原生适配已经实施到真实生产源码，并通过本地兼容回归。
T1 不再只是 T0 探针，也不等于完成生产升级：真实 MySQL 引擎表、Redis、模型供应商及业务 E2E 仍是门禁。

产品版本继续使用 1.2.0，仅用于兼容分支验证；不得将本轮产物发布成市场上的同版本正式制品。
后续上线需依发版规范确认主版本、同步插件声明/脚手架基线，并完成 ForgePro 独立验收。

## 2. 统一依赖

| 依赖 | 原工程基线 | T1 实际版本 |
| --- | --- | --- |
| Spring Boot | 3.5.13 | 4.1.1 |
| Flowable | 7.0.1 | 8.0.0 |
| Spring AI | 1.1.8 | 2.0.1 |
| MyBatis / MyBatis-Plus | 3.5.16 / 3.5.7 | 3.5.19 / 3.5.17 |
| dynamic-datasource | Boot 3 starter | Boot 4 starter / 4.5.0 |
| Sa-Token | 1.38.0 / Boot 3 starter | 1.45.0 / Boot 4 starter |
| Redisson | 3.50.0 | 4.8.0 |
| springdoc | 2.6.0 | 3.1.1 |
| Flyway / Java | 10.20.1 / 17 | 不变 |

MyBatis-Plus 拆分 Spring 与 SQL 解析器模块：Service/ServiceImpl 导入改为 `com.baomidou.mybatisplus.spring`，
相关 VM 生成模板同步修改；保留 `mybatis-plus-jsqlparser-4.9`，不同时改变数据权限 SQL 协议。
依据：[MyBatis-Plus 官方安装说明](https://baomidou.com/en/getting-started/)。

## 3. 底座与协议保持

- Web 改用 `spring-boot-starter-webmvc` 和 Tomcat；AOP 改用 `spring-boot-starter-aspectj`。
- 必要的 Jackson、Flyway、MVC 自动配置包随 Boot 4 模块化迁移，不能机械替换所有自动配置包。
- 保留 Boot 的 Jackson 2 过渡桥；业务 ObjectMapper 的大整数、金额、日期和加解密协议保持原样。
- MCP transport 单独注入带 qualifier 的 Jackson 3 mapper，不把两代 mapper 混用。
- Admin 显式引入 Flyway starter，插件 Flyway 依赖保持 optional，App/Flow 不被动执行平台迁移。
- Sa-Token 显式注册上游同一个 Servlet Filter，覆盖 REQUEST/ASYNC/ERROR，保留上游顺序及原认证策略。
- Redisson 监控切到新节点 API，单机/主从/哨兵/集群按当前拓扑选择数据节点。

Boot 4 的容器、Jackson 2 兼容桥及自动配置变化依据
[Boot 4 官方迁移指南](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)。

## 4. 流程

Flowable 8 使用 Boot 4 / Spring 7，不能把其单独塞回 Boot 3。
现有流程客户端 URI 构造、鉴权传递、表单校验和历史变量链路已增量验证，未修改历史 Flyway SQL。
依据：[Flowable 8 发布说明](https://github.com/flowable/flowable-engine/releases/tag/flowable-8.0.0)。

主配置明确 `flowable.variable-json-mapper=jackson2`，当前 JsonNode 流程变量继续以 JSON 类型存储，
H2 测试断言运行时和历史变量往返，不以 Java 对象序列化掩盖类型不兼容。
采购审批沿用真实 BPMN，覆盖同意、会签、驳回重提、终止、转办及字段/租户约束。

完整流程模块回归为 179 项。两项旧测试分别仍检查已提取的旧调用位置、或漏写 v3 节点 required，
按未改动的当前生产协议修正样例，并补充节点取消必填和 schema 回退；没有修改生产权限语义。

## 5. OpenAI

正式源码迁移到 Spring AI 2 的模型 builder 与官方 OpenAI SDK：聊天、SSE、嵌入、图像、ASR、TTS。
仍使用 `spring-ai-openai` 制品，不使用不存在的同版本 `spring-ai-openai-sdk`。
依据：[Spring AI 升级说明](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)。

SDK baseUrl 已带 `/v1`，用户已有 `/v1`、`/v4` 不重复追加；通义兼容嵌入的 `/compatible-mode` 仍保留。
显式 3 秒请求超时、`maxRetries=0`，避免 SDK 默认重试与框架路由/健康重试叠加；
错误日志与 ReAct 错误事件不传完整响应或异常正文。SDK 结构化 HTTP 状态进入现有错误分类。
官方 SDK 的超时/重试和错误对象行为依据
[OpenAI Java 参考](https://developers.openai.com/api/reference/java)。实际由 AI BOM 解析的 SDK 为 4.49.0，未擅自升级到文档最新示例版本。

模型只声明工具，不安装自动执行 Advisor；真实执行仍走 Forge ReAct 权限链。
本轮工具协议测试不等于全部 Agent、RAG、数据库记忆、取消和真实多模态 E2E 验收。

## 6. 通义兼容方案

T0 已证实 Alibaba 2.0.0-M1.1 与 AI 2.0.1 在模型构造时缺少已删除的接口。
本轮移除其 BOM/原生依赖，不降级整个平台到 milestone、不自建上游同名类、不静默切换供应商模式。
上游预发布依赖路线见
[Alibaba M1.1 发布说明](https://github.com/alibaba/spring-ai-alibaba/releases/tag/v2.0.0-M1.1)。

`dashscope_native` 改由 Forge 独立桥接 Spring AI 2 接口，调用通义真实原生协议：

- Chat：原生 `input.messages` / `parameters`、模型参数、用量、reasoning、SSE。
- Embedding：每批最多 10 条，严格校验 `text_index`、数量及顺序，异常不返回错位向量。
- Tools：工具声明、assistant/tool 消息、SSE 按 index 拼接片段并避免重复输出。
- Media：携带媒体输入时调用原生 multimodal generation；保留供应商配置和模型名称。
- 3 秒超时与 2MB SSE 帧上限，状态/错误码结构化分类，响应正文不进入日志或用户错误。

协议依据：[原生 Chat API](https://help.aliyun.com/en/model-studio/qwen-api-via-dashscope)、
[Function Calling](https://help.aliyun.com/en/model-studio/qwen-function-calling)、
[Embedding API](https://www.alibabacloud.com/help/en/model-studio/text-embedding-synchronous-api)。

边界：独立适配器不是 Alibaba 全部能力的替代；不包括全部多模态模型、原生图像/语音或 Agent 高级扩展。
既有 ReAct 对原生通义模型的工具绑定限制没有在本轮扩展，不能宣称原生 Agent 工具链全闭环。
真实模型参数、区域端点、工具片段和超时预算需逐个供应商验收；3 秒默认值可能不适合长推理和图像生成。

## 7. 本地验证证据

- 最终四服务生产 reactor：51 模块、127 类、543 项，0 失败、0 错误、0 跳过。
  其中流程插件 179、流程服务 43、AI 75、MCP 16；前一轮较小风险集为 108 类、444 项。
- 独立实际源码工程：28 项，0 失败、0 错误、0 跳过；AI 用例与 reactor 部分重叠，不叠加为独立数量。
- 数字/金额/日期、JSON 请求限制、加解密重放与内部调用、插件装配/Flyway、委托身份与清理有相关断言。
- 只使用本地 H2、MockMvc、loopback HTTP；没有真实数据库/Redis/付费模型请求。
- 此矩阵为风险相关测试集，不是项目全量测试。根 POM 原有历史排除项未改动，未新增排除或跳过失败用例。

Admin/App/Flow/Report 四服务 `clean package` 均成功，构建时间 66 秒。
实际 JAR 包含 Boot 4.1.1、Spring 7.0.9、Tomcat 11.0.24、Jackson 2.21.5 / 3.1.5、Boot 4 专用 starter；
有对应能力的服务包含 AI 2.0.1 / OpenAI SDK 4.49.0，独立 Flow 服务包含 Flowable 8.0.0。
四个 JAR 均未发现 Undertow、Alibaba milestone 或 Boot 3 专用 starter，build-info 为 1.2.0 / commit=unknown。
不把未提交的工作区标成基线 SHA；实验产物不发布成正式 1.2.0 升级包。
可复制命令、失败过程和日志见 [execution-log.md](execution-log.md)、[test-spec.md](test-spec.md)。

## 8. 后续配置与上线门禁

1. IDE 刷新 Maven，使用 Java 17；不要复用 Boot 3 的 classes/JAR。生成工程的插件安装/裁剪还需独立验证。
2. 外置配置的 `spring.jackson.*` 改为 `spring.jackson2.*`，设置
   `spring.http.converters.preferred-json-mapper=jackson2`，避免主配置被配置中心覆盖。
3. 删除/转换 `server.undertow.*` 外置配置；使用当前 Tomcat threads、form、swallow 配置。
   保留 `FORGE_MAX_HTTP_POST_SIZE`；JSON 的 `forge.web.max-body-size` 可单独覆盖。
   Tomcat 表单限制不涵盖 JSON，新 Filter 预检 Content-Length 并在读取分块请求时计数，超限返回 413。
   multipart 继续受现有大小限制，真实 Tomcat 的非阻塞/代理上传需要环境验收。
4. 在 MySQL 库副本演练 Flowable 7→8 schema 和已有 JsonNode 历史变量，核对长时间运行及并行流程。
   不改已执行 Flyway SQL、不用 repair 掩盖引擎结构变化；结构升级后的回滚须依数据库备份。
5. 验证 Redis/Redisson 与 Sa-Token 缓存兼容、租户隔离、Pub/Sub、序列化和旧 token。
6. 验证普通 API 登录、SSO、机器 token、MCP/能力权限、内部签名和真实 PC/H5 审批主路径。
7. 验证真实 OpenAI/通义及兼容供应商、长响应超时预算、工具执行/取消、多模态、Agent/RAG。
8. 四个服务分别启动、检查健康与插件自动配置；完成测试部署、回滚演练，再考虑合并 main 和正式主版本发布。

以上门禁未完成前，不允许把本轮产物当作生产升级包；本轮没有尝试自动部署或改动用户数据服务。

# Spring Boot 4 T0 兼容性验证

> status: reviewed
> created: 2026-10-11
> complexity: 复杂

## 1. 背景与目标

用户批准在独立分支验证 Spring Boot 4，重点确认流程和 AI 依赖能否升级。
本轮只交付可复跑的隔离验证工程、真实执行证据和进入 T1 的条件，不直接升级正式应用。

## 2. 代码现状

- `forge-server/pom.xml`：Boot 3.5.13、Java 17、Flowable 7.0.1、Spring AI 1.1.8、Alibaba 1.1.2.3。
- `forge-framework/forge-dependencies/pom.xml`：另有 Boot BOM 和 Boot 3 专用认证、ORM、多数据源依赖。
- `forge-plugin-ai/provider/adapter`：OpenAI、DashScope 模型使用当前版本的原生 API 和选项构造器。
- `forge-plugin-mcp/config/ForgeMcpServerAutoConfiguration`：自定义 Streamable HTTP、Jackson 和认证边界。
- `forge-business-core/purchase/support`：已有真实采购审批 BPMN，包含审批、驳回修改、重新提交、会签和字段权限。
- `forge-plugin-flow`：使用 `BytesStreamSource`、`ExpressionManager` 等引擎内部 API。

## 3. 范围

1. 新建 `codex/spring-boot4-t0-compatibility` 分支，保留开始时其它未提交文件。
2. 核对 Maven Central 中真实发布的版本及 BOM，记录预发布与正式版本的区别。
3. 建立独立 Maven 验证工程，不加入正式 reactor，不继承 Forge 生产 POM，不使用本地旧 Forge JAR。
4. 以当前 Boot 3 组合建立基线，再验证 Boot 4.1.1 / Flowable 8.0.0 / Spring AI 2.0.1。
5. 流程直接编译现有采购 BPMN/定义及其 DTO，在 H2 内存库运行，不重新编造业务模型。
6. AI 直接编译现有供应商适配器及最小真实源代码依赖，验证模型构造、会话 API 和本地协议。
7. 视结果验证 Alibaba 2.0.0-M1.1 与正式 Spring AI 2 的组合，必要时对照上游里程碑组合。
8. 核对 MCP 相关 API 和装配，区分依赖可解析、源码可编译、隔离运行通过三个层级。
9. 形成兼容矩阵、阻塞项和 T1 任务建议，失败验证保留诊断，不更改生产代码掩盖失败。
10. 现有 API 编译失败后，允许增加独立的新 API PoC 与通义原生适配器验证。
    PoC 不能替代现有源码验证；HTTP 响应为本地合成数据，不代表真实供应商已验收。

## 4. 安全与权限

- 不连接现有 MySQL、Redis、Flow、AI 供应商，不调用真实模型，不产生计费。
- 测试状态流转只在 H2 内存库和合成记录中执行；不改变真实审批、租户、权限或许可证。
- 如测试需要 HTTP，仅绑定 loopback 随机端口，结束关闭，不启动正式 Admin/Flow 服务。
- 不编辑历史 Flyway SQL，不执行 repair，不关闭正式应用的鉴权或租户隔离。
- 不修改 ForgePro；不提交、推送或部署，除非用户另行授权。

## 5. 数据与接口变更

无正式数据库、接口、前端或部署配置变更。验证目录不向正式应用提供类或资源。

## 6. 技术决策

- Java 仍为 17；发行版本仍为 1.2.0，本轮不是正式发版。
- 依赖版本只存在于隔离验证 POM；生产根 POM、BOM、插件 POM 保持原样。
- 验证输出按 profile 分目录，串行执行共享输出的 Maven 构建。
- Maven Central 元数据中的 `release` 也可能是 milestone，不能据此认定生产稳定。
- 新测试没有既有 Boot 4 基线；先运行相同测试的 Boot 3 基线，再对照目标组合。

## 7. 验收标准

- 每个结论都有命令、退出状态、实际测试数量或编译错误证据。
- 至少覆盖流程审批完成、驳回重提、会签、字段权限属性和租户查询边界。
- AI 至少覆盖 OpenAI / DashScope 构造及 options、ChatClient / ChatMemory API；记录实际覆盖边界。
- 明确外部真实模型、MySQL 存量引擎迁移和全项目 Boot 4 装配尚未验证。
- T0 完成可以得出“不允许直接进入生产”，不要求把所有依赖强行改到绿色。

## 8. 确认记录

- 用户于 2026-10-11 批准 T0 验证和新建分支，要求重点核对流程和 AI。
- 没有确认正式发行版本、提交、推送、生产部署或数据库迁移。

## 9. 审查与执行

见 `tasks.md`、`test-spec.md`、`execution-log.md` 和验证报告；结束时回填真实结论。

### T0 收尾审查（2026-10-11）

独立矩阵已实跑并 clean 复验。流程目标组合通过，新 OpenAI SDK 路线通过，
通义 GA 混配、现有 AI 适配器和 MCP 旧 API 有明确阻塞。验收允许记录负面结果，因此 T0 完成。
**HARD-GATE：未解除通义版本兼容、全平台回归和存量库迁移门禁前，禁止合并 Boot 4 升级到 main 或部署。**
正式 POM、生产 Java、历史 SQL、前端和 ForgePro 均未更改；产品版本仍为 1.2.0。

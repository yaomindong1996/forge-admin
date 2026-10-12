# T0 测试计划

> status: verified-with-blockers
> created: 2026-10-11

## 1. 原则

遵守 `code-copilot/rules/automated-testing-standard.md`。本轮新建隔离验证目录，暂无历史执行基线。
不把 Maven `BUILD SUCCESS` 当作已运行测试，必须核对 Surefire 测试数量、失败和跳过数。
先运行当前依赖组合的同一套测试，再对照 Boot 4；失败属于 T0 调研证据，不降低断言通过。

## 2. 测试范围

| 优先级 | 范围 | 方法 |
|---|---|---|
| P0 | 真实 Maven 版本、依赖树、Spring / Jackson 代际 | Central 元数据 + dependency:tree |
| P0 | 采购 BPMN 的通过、驳回修改、重新提交、会签 | Spring Boot + Flowable + H2 |
| P0 | 表单属性和租户隔离 | 原始 BPMN 属性、租户限定引擎查询 |
| P0 | 现有 OpenAI / DashScope 适配器 | 直接编译当前源文件 + 模型构造 / options 测试 |
| P1 | ChatClient / ChatMemory | 合成内存模型、无外部 API |
| P1 | MCP Streamable HTTP API | 原始调用签名核对 + 隔离编译 / 装配 |
| P1 | 稳定版 AI 与 Alibaba milestone 组合 | 版本矩阵对照、错误日志 |
| P1 | 新 OpenAI SDK 路线、原生通义二进制兼容 | 独立 PoC + loopback HTTP 合成响应 |
| P1 | 正式工程未变化 | git diff、POM / SQL 摘要、分支与工作区核对 |

## 3. 明确不覆盖

- 用户业务数据库、真实审批单、真实 Redis：本轮不授权修改环境。
- MySQL 存量 Flowable 7 → 8 schema / 变量迁移：需要库副本与单独验收。
- 真实 AI 供应商 / 模型计费、SSO 用户链路、PC / H5 浏览器：T0 只验证后端兼容路线。
- 全部 Forge Starter 的 Boot 4 实际迁移：属于 T1，不将局部测试表述为全工程升级完成。
- 发布构建 / 部署：不在本轮范围。

## 4. 可复跑命令

从仓库根目录执行，`mvn` 使用 Java 17：

```bash
mvn -f code-copilot/changes/spring-boot4-t0/verification/pom.xml \
  -Pboot3-baseline,enable-tests -pl flow-probe,ai-probe,mcp-probe test
mvn -f code-copilot/changes/spring-boot4-t0/verification/pom.xml -Pboot4-target,enable-tests -pl flow-probe test
mvn -f code-copilot/changes/spring-boot4-t0/verification/pom.xml -Pboot4-target,enable-tests -pl ai-probe test
```

其余依赖树、里程碑组合和 MCP 命令在验证 README 与 execution-log 中追加，不重写失败证据。

## 5. 实际执行结果

2026-10-11 最终 clean 复跑：

- Boot 3 基线：流程 8、AI 4、MCP 2 项，全部 0 失败 / 0 错误 / 0 跳过。
- Boot 4 + Flowable 8：相同 8 项全部通过。
- Boot 4 当前 AI 源码：`OpenAiApi` 已移除；旧 AI / Alibaba 上游对照：`HttpHeaders.entrySet()` 不存在。
- Boot 4 MCP：旧 JSON mapper / transport import 在 testCompile 阶段失败。
- Boot 4 新 SDK / 通义隔离：执行 7 项，4 项新 SDK 和 1 项路径校验通过，通义模型构造 / 聊天 2 项报缺类。
- Alibaba 上游组合：同一套通义 3 项通过；不是把正式 AI 2 降级后的“生产验收”。
- 两组 dependency:tree 均退出 0，实际选中版本与报告一致。

编译失败项未运行测试，不标记为通过或跳过。最终矩阵 10 个构建条目中 5 个非零，脚本退出 1。
原始日志在 `verification/target/logs/matrix.ekBXnV/`，关键诊断在 execution-log 和 compatibility-report。

## 6. 增量与清理

第一次流程探针使用不存在于目标基线的旧 JSON converter，修正测试工程后先重跑 Boot 3，再跑目标组合。
AI 首轮旧源码失败后新增独立 runtime probe，保留旧编译探针和全部失败证据，不替换生产适配器。
最后只对 POM 格式、独立 PoC 和 runner 做 clean 矩阵复验，不重规划业务 E2E。
各模块 target 按 profile 隔离；每次 runner 新建日志目录。H2 在测试 JVM 退出后释放，HTTP 桩逐项关闭。
未启动正式 Admin / Flow / Redis / MySQL，不需要停止用户已有服务。

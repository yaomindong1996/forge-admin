# 单测 Spec — 独立打印中心与多数据源打印
> status: apply
> created: 2026-09-29

## 0. 测试原则

- 按本轮差异做增量验证，不连接真实外部接口和开发数据库。
- 权限、来源身份、版本固定和参数白名单属于 P0。
- 前端 UI 必须执行 Vitest、构建和浏览器基本点击验证。

## 1. 测试框架

| 项目 | 值 |
|---|---|
| Java | JUnit 5 + Mockito + 现有 print fixture |
| 前端 | Vitest + Vue Test Utils |
| 数据库 | Mapper XML/H2 合同测试；真实 Flyway 由用户环境验收 |

## 2. 覆盖范围

### P0 — 核心业务逻辑

- 独立来源创建、更新、启停、删除引用保护。
- 来源编码唯一、修订号冲突、跨租户不可见。
- 旧应用来源与独立来源 key 稳定且互不冲突。
- 独立绑定只返回固定已发布模板版本。
- 未知参数、安全字段覆盖、越权记录和非授权字段拒绝。
- 数据集未发布、禁用或无 QUERY 权限时拒绝。

### P1 — 数据访问层

- 新表和新增列存在；逻辑删除查询显式过滤。
- nullable application_id 后唯一索引仍保证来源模板/绑定唯一。
- Mapper XML 只使用 `#{}` 绑定业务值。

### P2 — 前端

- 独立来源 query/payload 归一化。
- 打印中心来源选择、模板分页和错误/空态。
- `BusinessPrintButton` 只传声明的运行身份，不拼接正文到 URL。
- 现有低代码路由上下文测试继续通过。

## 3. 执行计划

- [x] 运行现有 print 前端和后端测试基线。
- [x] 每个 Task 补增量测试后实现。
- [x] 使用临时 JDK 17/Maven 运行 print/data 相关 Maven 测试。
- [x] 运行打印相关 Vitest。
- [x] 运行前端 build 和 Admin 46 模块聚合 package。
- [ ] 浏览器连接真实后端验证独立打印中心主要交互（本机未启动 8580 后端）。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|---|---|---|---|---|
| 2026-09-29 | print 持久化基线 | `mvn ... -Dtest=PrintMapperContractTest,PrintPersistenceTest` | 阻塞 | 本机未安装/未暴露 Maven，命令返回 127 |
| 2026-09-29 | 后端补充验证 | 临时 JDK 17 + Maven 3.9.16 | 通过 | 只用于本轮构建，未修改系统 Java/Maven 配置 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|---|---|---|---|---|---|
| 2026-09-29 | Task 1 数据模型与迁移 | diff、Flyway 占位符、Mapper XML | `git diff --check`、`rg`、`xmllint --noout` | 通过 | Java 验证已在后端补充验证阶段完成 |
| 2026-09-29 | Task 2 来源 CRUD | DTO/配置/修订号/权限静态检查 | `git diff --check`、`xmllint`、Java 形态扫描 | 通过 | JUnit 已在后端补充验证阶段完成 |
| 2026-09-29 | Task 3 双来源模板 | 旧摘要兼容、独立来源防伪、Mapper XML | `git diff --check`、`xmllint --noout`、`${...}` 扫描、Java 形态扫描 | 通过 | JUnit 已在后端补充验证阶段完成 |
| 2026-09-29 | Task 4 固定版本绑定 | DTO 条件校验、固定版本解析、禁止最新版本兜底 | `git diff --check`、`xmllint --noout`、`${...}` 扫描、Java 形态扫描 | 通过 | JUnit 已在后端补充验证阶段完成 |
| 2026-09-29 | Task 5 多数据源运行时 | 参数白名单、SERVICE 分发、DATASET ACL/查询映射 | `git diff --check`、POM XML 解析、Java 形态扫描 | 通过 | JUnit 已在后端补充验证阶段完成 |
| 2026-09-29 | Task 6-8 前端与兼容 | 来源工作台、统一入口、旧低代码/流程打印 | `vitest run ...`、`vite build`、ESLint、`git diff --check` | 通过 | 46 个测试文件、268 项前端测试通过；构建仅有存量 Vite/CSS 警告 |
| 2026-09-29 | Task 1-5 后端逻辑 | 来源、模板、绑定、参数、SERVICE Provider | `mvn ... -Penable-tests -Dtest='Print*Test,ServicePrintDataProviderTest' test` | 通过 | 146 项测试；Mockito 通过显式 Byte Buddy Java Agent 运行 |
| 2026-09-29 | Task 5 数据集 Provider | 数据集发布/权限/取数映射 | `mvn ... -Penable-tests -Dtest=DatasetPrintDataProviderTest test` | 通过 | 2 项测试通过 |
| 2026-09-29 | Task 8 Admin 聚合 | 完整后端依赖树与主应用装配 | `mvn -pl forge-admin-server -am package -DskipTests` | 通过 | 46 个 reactor 模块全部成功 |
| 2026-09-30 | Task 9 菜单层级修复 | 应用总览恢复、独立打印目录、三个子菜单、旧入口兼容 | `PrintResourceContractTest`、3 个前端 Vitest 文件、变更文件 ESLint、`vite build`、Flyway 占位符扫描、`git diff --check` | 通过 | 后端 4 项合同、前端 23 项增量测试通过；构建仅有存量 Vite/CSS 警告 |

## 6. 执行证据

- `execution-log.md`：随 Task 实时追加。
- 关键接口：来源 CRUD、catalog、available-templates、prepare。
- 关键数据库检查：迁移结构、索引、逻辑删除。
- 服务启动与停止：本轮未启动服务；本地没有 MySQL、Redis 和 8580 后端，真实后端浏览器联调由用户环境完成。

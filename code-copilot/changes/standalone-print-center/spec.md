# 独立打印中心与多数据源打印
> status: apply
> created: 2026-09-29
> complexity: 🔴复杂

## 1. 背景与目标

现有原生打印已经具备模板协议、设计器、版本、绑定、运行预览和执行审计，但模板来源与低代码应用的 `applicationId/pageId/formKey` 强绑定。普通业务虽然可以实现 `PrintDataProvider`，仍需挂到应用发布快照，无法在独立打印中心统一配置。

本变更将打印能力升级为平台级“打印中心”：低代码应用继续兼容；代码业务、已发布数据集和受控调用参数可以注册为业务数据源；任意业务页面通过统一前端入口传递 `sourceCode + scene + recordId + params` 发起打印。

可验收结果：

- 管理员可从独立“打印中心”维护业务数据源、打印模板和场景绑定。
- 普通业务页面不依赖低代码应用 ID 即可调用现有模板选择和预览。
- 表数据通过现有数据集模块接入，不允许模板或前端传任意 SQL/表名。
- 调用参数必须符合数据源参数协议，不得覆盖租户、用户、模板版本或权限上下文。
- 现有低代码和流程打印协议、模板、发布快照及运行入口继续可用。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- `forge-admin-ui/src/views/print/index.vue`：当前独立路由只是读取 query 中的 `applicationId` 和低代码来源，再渲染模板列表。
- `forge-admin-ui/src/stores/print/printRuntimeStore.js`：统一调用 available/prepare，但准备完成后仍固定读取低代码应用水印。
- `forge-server/.../forge-plugin-print/spi/PrintDataProvider.java`：已有字段目录、设计授权、记录授权和数据加载 SPI，可继续扩展。
- `forge-server/.../forge-plugin-data/service/DataDatasetRuntimeService.java`：已有发布状态、ACL、行范围、参数和字段元数据能力，可作为表/SQL数据源的受控底座。

### 2.2 现有实现

- `PrintSourceRequest` 强制 `applicationId`，来源仅 `LOWCODE/CODE`。
- `sys_print_template`、`sys_print_binding`、`sys_print_execution` 的 `application_id` 均为非空。
- 可用模板版本由 Provider 返回的应用发布快照决定；独立来源尚无绑定版本解析器。
- `PrintDataMode` 只有 `CURRENT`，运行请求不能携带受控业务查询参数或服务端数据会话。
- `SamplePurchaseOrderPrintDataProvider` 已证明代码业务可以复用相同打印协议，但仍依赖应用版本和表单身份。

### 2.3 发现与风险

- 不能直接允许前端输入表名、SQL、URL 或 Provider Bean 名，否则会绕过租户、数据范围和 SSRF 防护。
- 页面传入正文不能天然视为可信业务数据；本期只开放受控查询参数，正文快照后续通过短时服务端 token 接入。
- 低代码运行使用不可变应用快照固定模板版本；独立来源必须由绑定固定已发布模板版本，不能回退到草稿。
- 旧数据和旧请求协议必须双路径兼容，迁移不能要求现有模板重新创建。

## 3. 功能点

- [x] F01 新增打印业务数据源注册表，支持 `SERVICE`、`DATASET`，保留 `LOWCODE/CODE` 兼容。
- [x] F02 打印来源协议支持独立 `sourceCode`，旧应用来源保持原协议可用。
- [x] F03 独立来源模板、绑定、版本解析和执行审计不再要求 `applicationId`。
- [x] F04 新增业务数据源管理 API，固定 DTO、权限、租户、逻辑删除和修订号控制。
- [x] F05 新增数据集打印 Provider，复用已发布数据集的 ACL、行范围、参数和字段元数据。
- [x] F06 运行请求支持受控 `params`，只接受数据源参数协议声明的键和值类型。
- [x] F07 新增独立打印中心页面：数据源导航、模板资产列表和数据源配置。
- [x] F08 新增 `BusinessPrintButton` 与 `useBusinessPrint`，业务页面只传来源编码、场景、记录 ID 和可选参数。
- [x] F09 解耦低代码专属水印读取；独立来源不再请求应用配置，低代码继续兼容原页面水印。
- [x] F10 保留现有低代码应用设置打印入口和应用发布快照逻辑。
- [x] F11 预留 `API` 类型但保持失败关闭；`CALLER_SNAPSHOT` 不接收不可信正文，留后续短时数据会话实现。
- [x] F12 打印中心作为独立一级目录，业务数据源、打印模板、场景绑定分别提供菜单入口；恢复应用中心下的应用总览。

## 4. 业务规则

1. 数据源编码在租户内唯一，发布后不可改；名称、状态和配置使用修订号并发控制。
2. `SERVICE` 数据源只能引用服务端已注册的 Provider code；客户端不能传 Bean 名。
3. `DATASET` 数据源只能引用已发布、启用且当前用户具备 QUERY 权限的数据集。
4. 业务调用的 `params` 必须符合数据源参数 schema；`tenantId/userId/templateVersion` 等安全字段不接受客户端覆盖。
5. 模板继续只消费 `main/children/flow/system`；查询参数由 Provider 映射为标准数据，不能直接变成任意模板字段。
6. 低代码来源继续由应用发布快照固定模板版本；独立来源由启用绑定固定当前已发布模板版本。
7. prepare 每次重新执行来源、记录、字段和资源授权；知道模板 ID 不能绕过绑定。
8. 打印执行日志不保存完整业务正文、接口凭据或文件临时 URL。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|---|---|---|---|
| 新增 | `sys_print_business_source` | 来源编码、名称、类型、Provider/数据集引用、参数协议、映射配置、状态、修订号、逻辑删除及审计字段 | 平台打印业务来源 |
| 修改 | `sys_print_template` | 新增 `business_source_id`；`application_id` 改可空；唯一键按 `source_key` 收敛 | 同时兼容应用来源和独立来源 |
| 修改 | `sys_print_binding` | 新增 `business_source_id`、`template_version_id`；`application_id` 改可空 | 独立绑定固定发布版本 |
| 修改 | `sys_print_execution` | 新增 `business_source_id/source_revision`；`application_id` 改可空 | 记录独立来源身份和修订版本 |
| 新增 | `sys_resource` | 打印中心菜单及来源管理权限 | 不自动扩大普通角色权限 |
| 修复 | `sys_resource` / `sys_role_resource` | 独立打印目录、三个子菜单及已有打印角色的目录继承 | `V1.0.205` 修复已执行 `V1.0.204` 的错误层级，不修改历史迁移 |

独立来源结构迁移使用 `V1.0.204`；菜单层级修复使用后续 `V1.0.205`，不修改可能已执行的历史迁移。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|---|---|---|---|
| 新增 | `/print/sources/page` | GET | 来源分页 |
| 新增 | `/print/sources/options` | GET | 当前用户可设计来源选项 |
| 新增 | `/print/sources/{id}` | GET | 来源详情与字段目录 |
| 新增 | `/print/sources` | POST | 创建独立来源 |
| 新增 | `/print/sources/{id}` | PUT | 修订号更新来源 |
| 新增 | `/print/sources/{id}/status` | PUT | 启停来源 |
| 新增 | `/print/sources/{id}` | DELETE | 引用保护逻辑删除 |
| 修改 | `/print/templates/page` | GET | 支持 `businessSourceId`，旧 `applicationId/pageId` 保留 |
| 修改 | `/print/catalog` | POST | 支持独立 `sourceCode` |
| 修改 | `/print/available-templates` | POST | 记录请求支持受控参数 |
| 修改 | `/print/prepare` | POST | 独立来源解析绑定版本并取数 |

## 7. 影响范围

- 后端：`forge-plugin-print`、`forge-plugin-data`、采购示例 Provider、Admin 聚合、Flyway。
- 前端：打印 API、Pinia store、打印中心页面、模板管理来源选择、统一业务打印入口。
- 兼容：低代码应用打印、流程打印、历史模板、模板协议 v1、现有预览/设计器。

## 8. 风险与关注点

- ⚠️ 权限：数据集和业务 Provider 必须执行记录级授权，不能仅检查 `print:execute`。
- ⚠️ 数据：动态参数必须强类型校验；禁止任意 SQL、表名、URL 和内部 Bean 名透传。
- ⚠️ 发布：独立绑定必须固定模板版本和 hash，避免运行时静默跟随草稿。
- ⚠️ 迁移：MySQL 唯一键遇到 nullable `application_id` 时不能依赖旧唯一索引。
- ⚠️ 兼容：流程打印仍要求合法 task/process/run 上下文，不因独立来源放宽。

## 8.5 测试策略

- **测试范围**：来源 DTO/Service/Mapper、绑定版本解析、数据集字段目录与取数、旧来源兼容、前端来源协议与统一入口。
- **覆盖率目标**：新增核心 Service 分支和权限拒绝分支均有单元测试；前端协议工具与 Store 关键分支覆盖。
- **独立 Test Spec**：是。

## 9. 待澄清

- 无。本次实现边界已由用户于 2026-09-29 确认：按分析方案开始，先建立平台级独立打印中心并逐步接入多数据源。

## 10. 技术决策

1. 复用现有打印协议、设计器、渲染器、版本和审计，不建设第二套打印引擎。
2. 表/SQL来源复用数据集模块，不在 print 插件中实现动态 SQL。
3. print 插件只定义 SPI；data 插件实现 DATASET Provider，避免 print 反向依赖 data。
4. 旧 LOWCODE/CODE 来源采用兼容路径；独立来源使用 `businessSourceId/sourceCode`。
5. 本阶段开放受控查询参数；完整页面正文采用后续短时数据会话，不直接信任客户端 JSON。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|---|---|---|---|
| Task 1 | 完成 | V1.0.204、PrintBusinessSource、Mapper、三张现有实体/Mapper、合同测试 | 静态检查、JUnit 与 Java 17 聚合编译通过 |
| Task 2 | 完成 | 来源 DTO/VO/枚举、Service、Controller、配置校验、权限字典 | API 保持固定 DTO，API 来源仅保留协议值未开放 |
| Task 3 | 完成 | 双来源协议、模板 Access/Service/Mapper、兼容测试 | 旧来源摘要保持稳定，独立来源身份服务端规范化 |
| Task 4 | 完成 | 绑定 DTO/Service/Mapper、运行版本解析、执行审计 | 独立来源固定发布版本，不回退最新版本 |
| Task 5 | 完成 | 参数校验器、SERVICE 适配器、DATASET Provider、采购示例接入 | 参数白名单；数据集复用 ACL/行范围/脱敏 |
| Task 6 | 完成 | 打印中心主从工作台、来源表单/列表、模板管理兼容 | 标准布局；来源/模板权限分离；固定版本可显式升级 |
| Task 7 | 完成 | BusinessPrintButton、useBusinessPrint、运行时来源解析 | 页面可只配置 sourceCode；受控参数不进 URL |
| Task 8 | 完成 | 菜单权限、接入文档、兼容测试与生产构建 | 前端验证、打印/数据集 JUnit 和 Admin 聚合 package 通过 |
| Task 9 | 完成 | V1.0.205、三条独立菜单路由、工作台职责拆分 | 应用总览恢复；打印中心独立；4 项后端合同、23 项前端增量测试及 build 通过 |

## 12. 审查结论

已完成 Spec 合规与代码质量审查。未发现越过数据集 ACL、允许任意 SQL/URL/Bean 名、客户端固定模板版本或破坏旧应用来源摘要的实现。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-09-29
- **确认人**：用户
- **确认内容**：同意按“独立打印中心 + 多数据源 + 统一业务调用”方案开始实施。

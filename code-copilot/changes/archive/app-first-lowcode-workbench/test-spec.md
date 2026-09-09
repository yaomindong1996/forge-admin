# 测试 Spec — 应用优先的低代码开发工作台
> status: apply
> created: 2026-07-13
> change: `app-first-lowcode-workbench`

## 0. 测试原则

- **增量优先**：每一阶段开始前复用本文件与 `execution-log.md`，只补当前阶段风险面。
- **Red/Green TDD**：新增核心服务、状态机、安全执行器和发布编排必须先形成可观察的失败测试，再实现通过。
- **兼容先行**：Phase 1 编码前先固化现有 `/ai/business/app` 访问入口行为，后续每阶段复跑。
- **证据优先**：实际命令、关键输出、接口返回、数据库检查和服务清理记录在 `execution-log.md`；无证据不得写“通过”。
- **分阶段门禁**：前一阶段 P0 未通过，不进入后一阶段。
- **安全负例优先**：脚本、CSS、服务绑定、DDL、发布和租户隔离必须覆盖拒绝路径。
- **不污染环境**：本 Proposal 阶段不启动服务、不修改数据库；未来只停止本轮自行启动的进程。
- **真实联调边界**：真实 Flyway、Admin/Flow 服务、数据库迁移和端到端验收由用户执行或另行明确授权。

## 1. 测试框架与环境

| 项目 | 约定 |
|------|------|
| Java | JDK 17 |
| 后端测试 | 项目现有 JUnit 5 / Spring Boot Test / Mockito 体系，实施时沿用相邻测试风格 |
| Mapper | MyBatis Mapper XML 静态检查 + 可用测试数据库时的集成测试 |
| 前端 | Node `v20.19.0`、pnpm、项目现有 Vite 构建 |
| 前端单测 | 仅在仓库已有可运行测试脚本时接入；否则安全纯函数使用项目可用测试框架，UI 以 build + 浏览器交互为准 |
| 浏览器 | 本地 Vite + Playwright/浏览器人工交互，记录 URL、步骤和截图 |
| 数据库 | MySQL 8；真实迁移由用户执行并回填 |
| 覆盖目标 | P0 业务和安全场景 100% 列表覆盖；不在未配置 JaCoCo 的情况下虚报行覆盖率 |
| 当前基线 | Phase 4 历史证据为后端 75 tests、前端安全 26 tests、Admin 42 模块和前端生产构建通过；Phase 5 已实现但按用户要求未执行验证 |

## 2. 测试层次

### P0 — 核心业务、安全与兼容门禁

- 应用聚合 CRUD 和状态规则。
- 一个应用最多一个主对象，对象跨应用复用不被级联删除。
- 存量回填幂等、确定性和租户隔离。
- 现有访问入口 API、open-info、代码预览/下载兼容。
- 保存设计与数据库同步分离，高风险 DDL 拒绝。
- JS 沙箱、CSS 作用域和服务端白名单负例。
- 应用发布幂等、不可变快照、部分失败恢复和回滚边界。

### P1 — 数据访问、接口与前端主路径

- Mapper XML 聚合分页和逻辑删除过滤。
- Controller 权限、参数、加解密和响应协议。
- 业务域子树筛选、应用计数和工作台摘要。
- 前端新建、筛选、进入工作台、对象编排和发布历史。

### P2 — 体验、性能与非核心回归

- 空状态、错误提示、锁冲突、版本差异和跳转定位。
- 聚合查询 SQL 数量和响应时间基线。
- 响应式布局、键盘操作、长名称和大量对象/入口展示。
- 旧对象设计器、旧路由和未归属入口的兼容体验。

### 明确不自动验证

- 不对生产数据库执行 DDL。
- 不验证任意 Java 在线编译或任意 SQL，因为它们明确不在实现范围。
- 不宣称真实多租户、真实外部 HTTP、真实 Flowable 发布已通过，除非用户提供对应证据。
- 不做与当前变更无关的全量 CRM、采购仓储或 AI 中枢端到端测试。

## 3. Phase 0：基线与兼容契约

### 3.1 现有访问入口契约

目标测试类：

- `BusinessAppServiceCompatibilityTest`
- `BusinessAppControllerCompatibilityTest`

| 编号 | 场景 | 输入/前置 | 预期 |
|------|------|-----------|------|
| P0-C01 | 访问入口分页 | `pageNum=1&pageSize=10` | 参数名和返回结构不变 |
| P0-C02 | 访问入口详情 | 有效入口 ID | 仍返回 appCode/objectCode/entryMode/configKey |
| P0-C03 | open-info | RUNTIME/ROUTE/IFRAME 入口 | 原安全校验行为不变 |
| P0-C04 | 代码配置 | 有代码下载权限 | options 读写路径不变 |
| P0-C05 | 代码预览/下载 | 有效入口 | 仍按旧入口 ID 处理 |
| P0-C06 | 逻辑删除 | 已删除入口 | 列表、详情和打开都不可见 |
| P0-C07 | 旧同步路径 | `/sync-published-crud-configs` | 兼容调用仍可达并记录废弃告警 |

### 3.2 基线执行

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am \
  -Dtest=BusinessAppServiceCompatibilityTest,BusinessAppControllerCompatibilityTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

如本机 JDK 17 实际路径不同，以 `java -version` 和项目现有成功基线为准，并在执行日志记录实际命令。

## 4. Phase 1：应用聚合基础

### 4.1 P0 — `BusinessApplicationServiceTest`

| 方法 | 场景 | 输入/Mock | 预期 |
|------|------|-----------|------|
| `create` | 正常创建 | 有效业务域、唯一编码 | DRAFT、启用、tenant 取会话 |
| `create` | 编码重复 | 未删除同编码 | 拒绝 |
| `create` | 逻辑删除后重建 | 只有已删除同编码 | 允许，唯一键不冲突 |
| `create` | 业务域不存在/跨租户 | 无效 suite | 拒绝 |
| `update` | 修改编码 | 已存在应用 | 编码保持不可修改 |
| `updateStatus` | 启停 | status 0/1 | 设计状态不被覆盖 |
| `delete` | 存在启用入口 | entry count > 0 | 阻止并返回可理解提示 |
| `delete` | 无入口 | 有共享对象 | 只逻辑删应用，不删对象 |
| `page` | 父业务域筛选 | suite 子树 | 包含全部子域应用 |

### 4.2 P0 — `BusinessApplicationObjectServiceTest`

| 场景 | 输入 | 预期 |
|------|------|------|
| 保存一个 PRIMARY | 主对象 + 明细 | 成功 |
| 保存两个 PRIMARY | 两个主对象 | 拒绝并不落部分数据 |
| 草稿无 PRIMARY | 空关联 | 允许但 readiness 阻断 |
| 重复对象 | 同 objectId 两次 | 拒绝或归一化为一条，不产生重复 |
| 跨租户对象 | 当前租户无权对象 | 拒绝 |
| 移除共享对象 | 对象被其他应用使用 | 只删当前关联 |
| 角色非法 | 未知 role | 拒绝 |

### 4.3 P0 — 回填契约

| 场景 | 数据集 | 预期 |
|------|--------|------|
| 主从对象 | MASTER + DETAIL relation | 一个默认应用，角色正确 |
| 交易对象 | TRANSACTION | 成为独立 PRIMARY |
| 引用对象 | REFERENCE 被多个主对象引用 | 可关联到多个应用 |
| 无对象入口 | objectCode 为空 | 进入本业务域历史入口应用 |
| 多义入口 | 一个 objectCode 匹配多个候选 | 不猜测，进入历史入口应用 |
| 重复执行 | 同一存量数据执行两次 | 应用、关联、归属数量不增加 |
| 逻辑删除数据 | del_flag=1 | 不参与回填 |
| 多租户 | 相同编码不同 tenant | 完全隔离 |

### 4.4 P1 — Mapper 与 Controller

| 编号 | 场景 | 预期 |
|------|------|------|
| P1-M01 | 应用分页 | XML 显式 `tenant_id`、`del_flag='0'` |
| P1-M02 | 详情 | join 的 suite/object 同样过滤逻辑删除 |
| P1-M03 | 对象关联 | 未删除唯一键语义正确 |
| P1-A01 | 分页协议 | `pageNum/pageSize` 生效 |
| P1-A02 | 权限 | list/add/edit/status/delete/objects 独立权限 |
| P1-A03 | 加解密 | Controller 注解和前端 encrypt 配置一致 |
| P1-A04 | applicationId 兼容 | 新入口必填路径可校验，旧同步路径可空 |
| P1-A05 | Binding target | APPLICATION 校验应用，APP 仍校验入口 |

### 4.5 Flyway 静态检查

```bash
cd forge-server
rg -n '\$\{[^}]+\}' db/migration
rg -n 'ai_business_application|ai_business_application_object|application_id' db/migration/V<actual>__add_business_application_aggregate.sql
rg -n 'tenant_id[^\n]*(DEFAULT 0|= 0)|VALUES[^\n]*, *0 *,' db/migration/V<actual>__add_business_application_aggregate.sql
```

第一条对正式 migration 目录应无输出；`<actual>` 在实施时替换为实际版本文件名并记录。

## 5. Phase 2：应用优先总览

### 5.1 P0 — 聚合分页

| 编号 | 场景 | 预期 |
|------|------|------|
| P2-Q01 | 20 条应用分页 | 一次主聚合查询，无逐应用 relation/app 查询 |
| P2-Q02 | 对象数量 | 只统计未删除关联和对象 |
| P2-Q03 | 入口数量 | 只统计 application_id 匹配的未删除入口 |
| P2-Q04 | 流程/扩展数量 | 按真实应用目标统计，旧 APP 不混入 |
| P2-Q05 | 父域筛选 | 包含子树内应用，不包含外部域 |
| P2-Q06 | 稳定排序 | 同 sort/update 下按 ID 稳定 |
| P2-Q07 | 空关联应用 | 计数为 0，不丢记录 |

### 5.2 P1 — 浏览器交互

| 编号 | 操作 | 预期 |
|------|------|------|
| P2-U01 | 打开应用总览 | 左域树、右应用列表；无对象分组主区 |
| P2-U02 | 选择父业务域 | 显示当前域及子域应用 |
| P2-U03 | 搜索/状态筛选/分页 | URL 或页面状态一致，结果正确 |
| P2-U04 | 新建空白应用 | 创建 DRAFT 并可进入工作台 |
| P2-U05 | 初始化失败 | 应用草稿保留，显示重试入口 |
| P2-U06 | 点击进入应用 | 新页签打开 `/app-center/application/:code` |
| P2-U07 | 旧访问入口 | 旧 `/app-center/app/:appId` 仍可打开 |
| P2-U08 | 空状态 | 显示新建应用和整理现有对象动作 |

### 5.3 性能基线

- 记录每页 20 条时的 SQL 次数和响应耗时。
- 目标：应用数量增长不增加额外逐行 SQL；本地响应目标不高于 800ms。
- 未接入 SQL 计数器时可通过 MyBatis SQL 日志人工统计，但必须保留证据，不能只凭代码判断。

### 5.4 本轮增量验证（2026-07-13）

已执行：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests \
  -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am \
  -Dtest=BusinessApplicationMapperTest,BusinessApplicationServiceTest,BusinessApplicationObjectServiceTest,BusinessBindingApplicationTargetTest,BusinessApplicationPhaseOneContractTest,BusinessApplicationBackfillContractTest,BusinessApplicationControllerTest,BusinessAppServiceCompatibilityTest,BusinessAppControllerCompatibilityTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：41 tests，0 failure、0 error、0 skipped。`BusinessApplicationMapperTest` 覆盖聚合计数、`APPLICATION` Binding 隔离、逻辑删除、稳定排序和递归业务域子树。

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec eslint \
  src/api/business-application.js src/router/index.js src/views/app-center/index.vue \
  'src/views/app-center/application.[applicationCode].vue' \
  src/views/app-center/components/ApplicationFilterBar.vue \
  src/views/app-center/components/ApplicationTable.vue \
  src/views/app-center/components/ApplicationInitializeStep.vue \
  src/views/app-center/components/ApplicationEditorDrawer.vue
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：定向 ESLint 无错误/警告；Vite 生产构建通过。构建仍有仓库既有的组件重名、动态/静态 import、CSS `//` 注释和 chunk 体积警告，不由本变更引入且不阻断产物。

静态契约已验证：总览源码不含 `BusinessObjectTable`、`businessObjectList`、`businessAppList`、`businessObjectRelations` 或“业务对象分组”；新旧 API 分文件；新应用、旧访问入口和旧对象设计路由同时保留。

待用户环境执行：P2-U01～P2-U08 浏览器交互、真实聚合 API、每页 20 条的 SQL 次数和 800ms 响应基线。未启动 Admin/Vite、未执行 Flyway，因此这些项目保持 pending。

## 6. Phase 3：工作台与表优先设计

### 6.1 P0 — 表映射和 DDL 边界

目标测试类：

- `BusinessObjectTableMappingServiceTest`
- `BusinessObjectDatabaseSyncServiceTest`

| 编号 | 场景 | 预期 |
|------|------|------|
| P3-D01 | 获取表映射 | 返回数据源、表名、字段三向映射、同步状态 |
| P3-D02 | 保存草稿 | 只写设计元数据，不调用 DDL execute |
| P3-D03 | DDL 预览 | 返回结构化差异和 SQL，不执行 |
| P3-D04 | 低风险追加字段 | 有权限且二次确认后可执行 |
| P3-D05 | 无 DDL 权限 | 拒绝，不执行 |
| P3-D06 | 数据源禁 DDL | 拒绝并提示导出迁移脚本 |
| P3-D07 | 设计版本冲突 | 返回冲突，不覆盖新版本 |
| P3-D08 | 删除/缩短/改类型 | 标记高风险，默认拒绝在线执行 |
| P3-D09 | 同步失败 | 草稿保留，记录失败状态和原因 |
| P3-D10 | 跨租户数据源 | 拒绝 |
| P3-D11 | 系统字段删除 | 拒绝 |

### 6.2 P1 — 工作台和对象设计浏览器场景

| 编号 | 操作 | 预期 |
|------|------|------|
| P3-U01 | 进入应用 | 七个分区和完成/问题摘要可见 |
| P3-U02 | 切换分区 | 详情按需加载，深链/刷新恢复 |
| P3-U03 | 关联已有对象 | 对象角色和表摘要正确 |
| P3-U04 | 新建对象 | 首先看到数据来源、表名、字段网格 |
| P3-U05 | 从表导入 | 先生成草稿，不自动改数据库 |
| P3-U06 | 设计表单深链 | 直接进入画布，但表映射摘要可见 |
| P3-U07 | 共享对象修改 | 显示受影响应用数量 |
| P3-U08 | 保存草稿 | 无数据库同步确认弹窗，也无 DDL 请求 |
| P3-U09 | 预览并同步 | 先差异预览，再权限和二次确认 |
| P3-U10 | 旧对象 | 原字段、表单、列表、详情、发布和历史可用 |

### 6.3 P2 — 字段网格边界

- 长表名、长字段名、100+ 字段时可滚动且表头可识别。
- 系统字段只读，索引和可空状态可理解。

### 6.4 2026-07-13 Phase 3 增量验证

本轮复用 Phase 1/2 的 41 个目标测试和构建基线，新增以下验证：

- P0：`BusinessApplicationWorkspaceServiceTest`、`BusinessObjectTableMappingServiceTest`、`BusinessObjectDatabaseSyncServiceTest` 和 `BusinessObjectDesignerPhaseThreeControllerTest`。
- P0：工作台/对象 Mapper XML 语法、保存/同步分离、版本冲突、显式确认、权限、`allowDdl`、高风险 DDL 和旧入口兼容。
- P1：应用工作台、对象编排、数据库表导入、访问入口创建、对象设计器数据结构首屏的定向 ESLint 与生产构建。
- P1：Admin 42 模块聚合打包，验证新增服务注入和 Controller 装配。
- 跳过：真实 Flyway、Admin/Vite 启动、数据库元数据读取、API、在线 DDL 和浏览器点击；按用户既定分工在真实环境回填。

验证命令：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests \
  -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am \
  -Dtest=BusinessApplicationMapperTest,BusinessApplicationServiceTest,BusinessApplicationObjectServiceTest,BusinessApplicationWorkspaceServiceTest,BusinessBindingApplicationTargetTest,BusinessApplicationPhaseOneContractTest,BusinessApplicationBackfillContractTest,BusinessApplicationControllerTest,BusinessAppServiceCompatibilityTest,BusinessAppControllerCompatibilityTest,BusinessObjectTableMappingServiceTest,BusinessObjectDatabaseSyncServiceTest,BusinessObjectDesignerPhaseThreeControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

mvn -pl forge-admin-server -am package -DskipTests

cd ../forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec eslint \
  'src/api/business-application.js' \
  'src/views/app-center/index.vue' \
  'src/views/app-center/application.[applicationCode].vue' \
  'src/views/app-center/application-workspace/*.vue' \
  'src/views/app-center/components/ApplicationInitializeStep.vue' \
  'src/views/app-center/components/BusinessObjectWizardDrawer.vue' \
  'src/views/app-center/components/AppEntryWizard.vue' \
  'src/views/app-center/components/designer/BusinessObjectDesignerShell.vue' \
  'src/views/app-center/components/designer/BusinessTableMappingSummary.vue' \
  'src/views/app-center/object-designer.[objectCode].vue'
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```
- 未映射字段、数据库已删除字段和类型不兼容有明确状态。
- 页面不展示 `undefined`、空 label 或空字段编码。

## 7. Phase 4：扩展中心安全与生命周期

### 7.1 P0 — 扩展状态机

| 场景 | 当前状态/动作 | 预期 |
|------|---------------|------|
| 新建 | - → DRAFT | 成功并生成 v1 |
| 校验失败 | DRAFT validate | 保持 DRAFT，记录问题 |
| 测试通过 | DRAFT test | 转 TESTED，保存测试摘要 |
| 启用 | TESTED → ENABLED | 成功 |
| 未测试启用 | DRAFT → ENABLED | 拒绝 |
| 修改已启用内容 | ENABLED save | 新草稿回到 DRAFT，旧发布版本继续运行 |
| 禁用 | ENABLED → DISABLED | 成功并审计 |
| 回滚草稿 | 选择历史版本 | 生成新版本，不改历史行 |
| 锁冲突 | 用户 B 编辑用户 A 的锁 | 拒绝并显示持有人/超时 |

### 7.2 P0 — JS 沙箱负例

| 编号 | 攻击/异常 | 预期 |
|------|-----------|------|
| P4-J01 | 访问 `window/document` | 不可用 |
| P4-J02 | 访问 cookie/storage/token | 不可用 |
| P4-J03 | 任意 `fetch/XMLHttpRequest/WebSocket` | 不可用 |
| P4-J04 | `eval/new Function` | 校验或运行拒绝 |
| P4-J05 | 无限循环 | 超时终止，不阻塞主页面 |
| P4-J06 | 超大输出 | 截断并失败/告警 |
| P4-J07 | 原型污染 | 隔离，不影响宿主 |
| P4-J08 | 调用未授权上下文动作 | 拒绝并审计 |
| P4-J09 | 读取未授权字段 | 上下文不提供或脱敏 |
| P4-J10 | 直接调用 postMessage | 前后端校验拒绝，不能绕过结构化 effects 协议 |
| P4-J11 | Worker 模块加载/初始化失败 | 显示脱敏浏览器错误、文件及行列，不返回无原因“异常终止” |

### 7.3 P0 — CSS 作用域负例

| 编号 | CSS | 预期 |
|------|-----|------|
| P4-S01 | `@import` | 拒绝 |
| P4-S02 | `url(https://...)` | 拒绝 |
| P4-S03 | `html, body, :root` | 拒绝 |
| P4-S04 | Forge layout/sidebar selector | 拒绝 |
| P4-S05 | 普通组件选择器 | 自动加应用/页面前缀 |
| P4-S06 | 复杂嵌套/伪类 | 重写后仍不能逃逸作用域 |

### 7.4 P0 — 服务端白名单负例

| 编号 | 场景 | 预期 |
|------|------|------|
| P4-B01 | 已注册 handler | 输入通过 Schema 后执行 |
| P4-B02 | 任意 class 名 | 拒绝，不反射加载 |
| P4-B03 | 任意 Bean 名 | 拒绝 |
| P4-B04 | 未允许钩子 | 拒绝 |
| P4-B05 | 输入超 Schema | 拒绝 |
| P4-B06 | 超时 | 终止/失败，按策略处理 |
| P4-B07 | 跨租户资源 | 拒绝 |
| P4-B08 | HTTP 密钥明文 | 保存校验拒绝 |
| P4-B09 | 敏感异常 | 日志和返回脱敏 |
| P4-B10 | handler 返回 `success=false` | 测试不通过、写失败审计；BLOCK 阻断，WARN 保留失败结果并继续 |

### 7.5 P1 — 执行顺序和失败策略

- `sort_order` 相同按扩展编码稳定排序。
- BEFORE 高风险钩子只允许 BLOCK。
- WARN 记录问题并继续；IGNORE 只允许低风险后置钩子。
- 每次执行记录扩展版本、耗时、结果和可信上下文，不记录敏感原值。

### 7.6 P1 — 扩展中心浏览器场景

- 类型/钩子/状态筛选。
- 钩子按数据写入、读取、交换和页面交互分组；JS/CSS 及 Java 处理器不支持的触发点不可选择。
- JS/CSS 使用带行号、全屏和右侧指导栏的代码编辑器；示例随当前触发点变化，空编辑器可一键套用。
- JS 示例同步填充测试字段白名单和测试上下文；CSS 明确当前应用/页面作用域及禁止覆盖区域。
- 编辑锁提示、校验、测试、启停、版本 diff 和回滚。
- 普通用户默认看到可视化规则；开发者权限才看到 JS/CSS/Java 服务增强。
- 所有扩展的“测试”操作统一进入测试台；Java 自动生成 Schema 测试输入，JS 展示沙箱步骤，失败位置和原因可见。
- 默认发布会跳过未测试扩展并显示提醒；未来显式选择未测试扩展时仍应阻断。

### 7.7 本轮 Phase 4 增量验证

本轮在 Phase 1～3 的 56 个目标测试基线上增加以下自动化证据：

- 后端：`BusinessExtensionServiceTest`、`BusinessExtensionVersionServiceTest`、
  `BusinessExtensionStateMachineTest`、`BusinessExtensionControllerTest`、
  `ServerBindingRegistryTest`、`ServerBindingExecutorTest` 和 Mapper XML 契约测试。
- 前端：`extension-sandbox.spec.js` 覆盖宿主页无动态执行、敏感上下文裁剪、
  禁止 API、未授权字段/动作、超时协议和输出上限；`scoped-css.spec.js` 覆盖 AST
  解析、作用域重写以及全局选择器、平台选择器、导入和外部 URL 拒绝。
- 迁移：`V1.0.28__add_business_extension_governance.sql` 的表、逻辑删除、未删除唯一键、
  字典、权限、防重复、租户 ID 和 Flyway 占位符静态检查。
- 回归：Phase 1～4 后端目标测试、Mapper XML、定向 ESLint、前端 Vitest、生产构建和
  Admin 聚合构建。

真实 Flyway、Admin/Vite 启动、数据库/API 和浏览器 E2E 仍由用户环境回填，本轮不自动执行。

历史执行证据：Phase 1～4 后端合并回归 75 tests、前端安全 26 tests、Admin 42/42 模块和一次前端生产构建通过。最终前端复验时发现本地 `node_modules` 中 `vitest` 可执行文件缺失，命令未进入测试执行；用户随后明确由其自行验证，因此不重装依赖、不把该次复验写成通过。

2026-07-14 增量补充了扩展类型/钩子兼容、Java 处理器允许钩子、结构化失败结果及 BLOCK/WARN 语义的测试源码；遵照用户要求未执行这些新增用例，状态保持 `completed-static`。

## 8. Phase 5：应用级发布与回滚

### 8.1 P0 — 就绪度

| 编号 | 缺口 | 预期级别 |
|------|------|----------|
| P5-R01 | 无 PRIMARY | BLOCK |
| P5-R02 | 无可用入口 | BLOCK |
| P5-R03 | 对象 Schema 无效 | BLOCK |
| P5-R04 | 数据库存在高风险未同步差异 | BLOCK |
| P5-R05 | 默认存在未测试扩展 | WARN，并从本次发布选择中自动跳过 |
| P5-R05A | 显式选择未测试扩展 | BLOCK |
| P5-R06 | 可选流程未绑定 | WARN |
| P5-R07 | 共享对象有未发布变更 | WARN 并列出复用应用数量，不阻断当前应用发布 |
| P5-R08 | 所有必需项满足 | READY |

### 8.2 P0 — 快照不可变

| 场景 | 预期 |
|------|------|
| 首次发布 | version=1，快照 hash 和内容落库 |
| 第二次发布 | version=2，不覆盖 version=1 |
| 尝试更新历史 | Service 不提供 update，Mapper 调用受保护 |
| 快照含 Secret | 生成失败或字段被排除，不允许落库 |
| 并发发布 | 版本唯一，只有一个成功或按锁串行 |

### 8.3 P0 — 协调发布

| 编号 | 场景 | 预期 |
|------|------|------|
| P5-P01 | 全部成功 | 状态 PUBLISHED，步骤全成功 |
| P5-P02 | 相同幂等键重试 | 返回同一结果，不重复发布 |
| P5-P03 | 对象发布失败 | 后续不执行，清楚展示失败位置 |
| P5-P04 | 入口切换失败 | 已完成项可见，可重试/补偿 |
| P5-P05 | 扩展启用失败 | 应用不标 PUBLISHED |
| P5-P06 | 发布后改草稿 | 运行态保持旧版，设计状态 CHANGED |
| P5-P07 | 选择发布缺依赖 | 自动补齐或阻断并解释，不产生残缺快照 |

### 8.4 P0 — 回滚边界

| 场景 | 预期 |
|------|------|
| 回滚到有效旧版 | 创建新的回滚发布记录，运行配置恢复 |
| 旧版依赖已删字段 | 阻断并提示缺失字段 |
| 旧版含破坏性 DDL | 不自动执行数据库回滚 |
| 重复回滚请求 | 幂等，不重复副作用 |
| 跨应用版本号 | 拒绝 |

### 8.5 P1 — 发布历史 UI

- 就绪度问题可跳转到对象、入口、流程或扩展分区。
- 发布选择明确显示自动补齐的依赖。
- 部分失败使用“部分完成/待恢复”，不显示绿色成功。
- 回滚确认明确说明不自动回滚业务数据和破坏性 DDL。

### 8.6 Phase 5 实现后的待执行验证（用户自验）

已新增但本轮未运行的目标用例：

- `BusinessApplicationPhaseFiveControllerTest`
- `BusinessApplicationPhaseFiveMapperTest`
- `BusinessApplicationPhaseFiveSecurityTest`
- `BusinessApplicationRollbackContractTest`

建议用户先恢复完整前端依赖，再按以下顺序执行：

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests \
  -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am \
  -Dtest='BusinessApplication*Test,BusinessApp*CompatibilityTest,BusinessBindingApplicationTargetTest,BusinessExtension*Test,ServerBinding*Test' \
  -Dsurefire.failIfNoSpecifiedTests=false test

mvn -pl forge-admin-server -am package -DskipTests

cd ../forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec eslint \
  src/api/business-application.js \
  'src/views/app-center/application.[applicationCode].vue' \
  'src/views/app-center/application-workspace/ApplicationPublishPanel.vue' \
  'src/views/app-center/application-workspace/ApplicationVersionDrawer.vue'
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

真实环境继续验证：`V1.0.29`、发布预检查、相同幂等键、对象/入口/扩展步骤故障、恢复、不可变版本、缺字段回滚阻断和旧运行入口不受损。

## 9. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-13 | Proposal 文档准备 | 未运行代码测试 | 未执行 | 本轮只分析和编写文档 |
| 2026-07-13 | 默认 Maven 测试配置 | 目标测试命令未加 `-Penable-tests` | tests skipped | 项目默认跳过测试，后续统一显式启用 profile |
| 2026-07-13 | 旧访问入口兼容基线 | `-Penable-tests` + 两个兼容测试类 | 8 passed | Mockito inline 在本机 JDK 17 无法自附加，改为无 Mockito 的反射契约测试后通过 |
| 2026-07-13 | Phase 1 最终目标测试 | `-Penable-tests` + 8 个 Phase 1/兼容测试类 | 37 passed | 0 failure、0 error、0 skipped |

## 10. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-13 | 四份 SDD 文档 | `git diff --check`、文档存在性和状态一致性 | 见 `execution-log.md` | passed | 不运行构建、服务、API、数据库 |
| 2026-07-13 | Phase 1 应用聚合 | CRUD、对象编排、入口兼容、Binding、回填契约、Controller 协议 | 见 `execution-log.md` 最终目标测试命令 | passed | 37/37；现有 deprecated/unchecked 编译警告 |
| 2026-07-13 | Mapper 与 Flyway | XML 解析、`${...}`、tenant 0、逻辑删除、显式列和回填去重扫描 | `xmllint --noout ...`、`rg ...` | passed | 仅静态检查，未连接 MySQL |
| 2026-07-13 | Admin 聚合包 | `mvn -pl forge-admin-server -am package -DskipTests` | 42 modules passed | passed | 测试已在前一步独立启用执行；构建阶段按命令跳过测试 |

## 11. 标准执行命令

### 11.1 后端目标测试

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am \
  -Dtest='BusinessApplication*Test,BusinessApp*CompatibilityTest,BusinessBindingApplicationTargetTest,BusinessExtension*Test,ServerBindingExecutorTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

### 11.2 后端聚合构建

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-admin-server -am package -DskipTests
```

### 11.3 前端构建

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh
nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

### 11.4 Proposal 文档检查

```bash
for file in code-copilot/changes/app-first-lowcode-workbench/*.md; do
  git diff --no-index --check /dev/null "$file"
done
```

未跟踪新文件存在内容差异时 `git diff --no-index` 返回 1 是预期行为；以是否输出空白错误作为检查结论。

## 12. 真实联调回填清单

用户执行真实环境验证时，应提供或记录：

- `forge_schema_history` 中实际版本、描述和 success。
- `ai_business_application`、`ai_business_application_object` 数量。
- `ai_business_app.application_id IS NULL` 的剩余数量和原因分类。
- 同一存量数据重复执行回填前后的计数。
- 应用分页、详情、对象关联、工作台、就绪度、发布和回滚 API 返回摘要。
- 旧访问入口打开、代码预览和下载结果。
- 应用总览与工作台关键页面截图/交互结果。
- 本轮启动并停止的服务 PID；未停止需明确原因。

## 13. 完成标准

- 每阶段 P0 全部有自动化或可重复的明确验证证据。
- 兼容契约从 Phase 1 到 Phase 5 持续通过。
- Flyway、Mapper XML、逻辑删除、租户、权限和安全负例均有检查。
- 前端构建和核心浏览器路径完成。
- 失败项记录根因和下一步，不改写为通过。
- 用户未执行的真实联调保持 pending。
- `execution-log.md` 包含实际命令、结果、警告、跳过项和服务清理状态。

## 14. 2026-07-14 应用列表滚动修复增量验证

### 14.1 变更范围

- `app-center/index.vue`：列表中间区域改为固定定位滚动视口，加载遮罩与内容尺寸分离。
- `ApplicationTable.vue`：表头和数据行统一六列宽度，操作列固定 160px 并保持右侧可见。

### 14.2 验证项

| 优先级 | 验证项 | 预期 |
|--------|--------|------|
| P0 | Vue/ESLint 静态检查 | 两个变更组件无语法和 Lint 错误 |
| P0 | 前端生产构建 | Vite 构建完成，不出现 Vue/CSS 编译错误 |
| P0 | 横向滚动 | `scrollWidth > clientWidth` 且 `scrollLeft` 可变化 |
| P0 | 纵向滚动 | `scrollHeight > clientHeight` 且 `scrollTop` 可变化 |
| P0 | 操作列 | 表头和数据行操作列均为 160px，横向滚动后仍位于视口右侧 |

### 14.3 环境边界

- 当前托管环境禁止本地端口监听，Vite 启动报 `listen EPERM`。
- Chromium 因 Mach IPC 权限被拒绝无法启动，WebKit 未安装，因此浏览器几何验证需由用户环境复验。
- 本轮仍执行不依赖端口和浏览器的目标 ESLint 与生产构建，并在 `execution-log.md` 记录真实结果。

### 14.4 实际结果

| 验证项 | 结果 | 证据 |
|--------|------|------|
| 目标 ESLint | passed | `index.vue`、`ApplicationTable.vue`，0 error、0 warning |
| 前端生产构建 | passed | Vite 7.3.1，8670 modules；最终复跑 `built in 1m 13s` |
| Vite 浏览器启动 | blocked-environment | 先遇到 `EMFILE`，提高当前 shell 限额后被托管环境以 `listen EPERM 127.0.0.1:3000` 拒绝 |
| Playwright Chromium | blocked-environment | MachPortRendezvous `Permission denied (1100)` |
| Playwright WebKit | not-run | 当前机器未安装 WebKit executable |
| 用户浏览器滚动复验 | pending | 需验证整张表横向移动、纵向列表滚动及第六列操作可达 |

## 15. 2026-07-14 应用工作台主题适配增量验证

### 15.1 变更范围

- `/app-center/application/:applicationCode`：移除包裹整页的 `NSpin`，改为不参与页面变量继承的独立加载遮罩。
- `application-workspace/*.vue`：页面表面、侧栏、表头、悬停、边框和文字改用 Forge 的 `--bg-*`、`--border-*`、`--text-*` 主题变量。

### 15.2 验证项

| 优先级 | 验证项 | 预期 |
|--------|--------|------|
| P0 | 目标 ESLint | 工作台入口及全部工作台子组件无语法和 Lint 错误 |
| P0 | 主题变量静态扫描 | 页面样式不再使用 `--n-color`、`--n-action-color`、`--n-table-header-color` 作为业务背景 |
| P0 | 前端生产构建 | Vite 构建完成，不出现 Vue/CSS 编译错误 |
| P1 | 明暗主题人工复验 | 页面背景、卡片、侧栏、表头和文字在明暗主题下均具有清晰对比度，无整页蓝色污染 |

### 15.3 验证边界

- 本轮不启动 Admin、Vite 或浏览器，明暗主题实际切换由用户环境复验。
- 静态检查和生产构建必须记录实际结果；未执行的浏览器检查保持 `pending`。

### 15.4 实际结果

| 验证项 | 结果 | 证据 |
|--------|------|------|
| 目标 ESLint | passed | 工作台入口及 `application-workspace/*.vue`，0 error、0 warning |
| 主题变量静态扫描 | passed | 工作台业务样式内无 `var(--n-*)` 残留，表面、文字、边框、主色和状态色均使用 Forge 主题变量 |
| 前端生产构建 | passed | 最终复跑：Node v20.19.0，Vite 7.3.1，8670 modules，`built in 1m 41s` |
| 明暗主题浏览器复验 | pending | 按用户分工由真实浏览器环境验收，不表述为已通过 |

## 16. 2026-07-14 工作台性能、数据和对象设计整合增量验证

### 16.1 变更范围

- 工作台摘要改为轻量资产快照，首屏不执行完整发布就绪度。
- 工作台分区使用缓存组件，避免切换后重新挂载和重新请求。
- 对象、入口和扩展分区优先消费工作台同源快照。
- 入口类型使用系统字典展示中文，技术编码降级为辅助信息。
- 对象设计器以内嵌模式进入应用工作台。

### 16.2 验证项

| 优先级 | 验证项 | 预期 |
|--------|--------|------|
| P0 | Workspace 服务测试 | 快照包含对象、入口、扩展；不依赖完整发布检查；分区计数与快照一致 |
| P0 | 前端 ESLint / build | 工作台、对象设计器和相关面板无语法、Lint、Vue/CSS 编译错误 |
| P0 | 静态交互契约 | 分区由 KeepAlive 缓存；对象设计不再调用 `window.open`；英文入口模式不直接渲染 |
| P1 | 用户浏览器复验 | 分区来回切换无重复加载；非零计数有数据；对象设计在当前应用内打开和返回 |

### 16.3 验证边界

- 真实 API 耗时、网络请求次数和浏览器交互由用户环境复验。
- 不启动真实 Admin、Flow 或数据库，不执行 Flyway；迁移和 E2E 保持 `pending-user`。

### 16.4 实际结果

| 验证项 | 结果 | 证据 |
|--------|------|------|
| 目标 ESLint | passed | API、应用工作台、全部工作台面板、对象设计器及设计器壳层，0 error、0 warning |
| 前端生产构建 | passed | Node v20.19.0，Vite 7.3.1，8670 modules，`built in 1m 50s` |
| 后端生产源码编译 | passed | generator 重新编译 518 个主源码文件成功；随后 testCompile 被既有测试构造器不一致阻断 |
| Workspace 目标测试 | blocked-existing-tests | 未进入目标用例执行；68 个测试源码全量编译时有 7 个既有 Phase 4/5 测试构造器参数落后于生产类 |
| Mapper XML | passed | `BusinessExtensionMapper.xml` 经 `xmllint --noout` 通过 |
| Flyway 静态检查 | passed | `V1.0.31` 无 `${...}`、tenant 0 或缺少防重复保护；未真实迁移 |
| 静态交互契约 | passed | 工作台使用 KeepAlive；对象/自动化/动作面板不再新开对象设计路由；入口模式和对象角色不再直接渲染英文枚举 |
| 用户浏览器复验 | pending | 需核对首次进入请求数、分区切换、快照数据、中文标签及对象设计内嵌返回 |

## 17. 2026-07-14 Phase 7 模板、无入口发布和草稿预览增量验证

### 17.1 变更范围

- 应用创建新增单表 CRUD、左树右表、主子表三套模板和条件式引导。
- 模板后端在单事务内初始化对象、字段、关系和页面 Schema。
- 旧模型设计菜单降级隐藏；旧路由和 API 保留。
- 无页面入口从发布阻断降为提醒，默认入口选择只包含可发布入口。
- 应用工作台新增无入口草稿预览，并删除重复命令栏、压缩顶部空间。

### 17.2 验证矩阵

| 优先级 | 验证项 | 预期 | 本轮状态 |
|--------|--------|------|----------|
| P0 | 模板服务契约测试 | 三模板共用事务服务，生成对应关系和布局 | not-run-user-owned |
| P0 | 发布选择/工作台测试 | 零入口仅提醒；默认跳过停用和缺配置入口 | not-run-user-owned |
| P0 | generator compile | 新 DTO/VO/Service/Controller 可编译 | not-run-user-owned |
| P0 | 前端 build | 模板向导、预览路由和工作台头部可编译 | not-run-user-owned |
| P0 | Flyway 静态扫描 | V1.0.33/34 无 placeholder，版本单调且 SQL 可重复 | static-only |
| P1 | 三模板真实创建 | 对象数、字段、关系、layoutType 和失败回滚符合 Spec | pending-user |
| P1 | 无入口发布 | 零入口应用可检查并发布对象 | pending-user |
| P1 | 草稿预览 | 无入口/未发布应用可预览，多对象可切换 | pending-user |
| P1 | 工作台首屏 | 顶部无重复命令栏和大块留白 | pending-user |

### 17.3 模板来源与真实页面预览修正

| 优先级 | 验证项 | 预期 | 本轮状态 |
|--------|--------|------|----------|
| P0 | 模板资产来源 | 主对象、树对象和每个子对象均可在数据库表/已有对象之间选择 | pending-user |
| P0 | 关系字段选择 | 树主键/显示/父级/筛选字段及主子表主外键均来自真实字段下拉 | pending-user |
| P0 | 真实草稿预览 | 工作台直接打开 `/ai/crud-page/:configKey`，不再进入独立线框预览页 | pending-user |
| P0 | 预览权限边界 | 未发布配置只有携带设计预览标识且拥有对象设计权限时可渲染和读取数据 | pending-user |
| P1 | 模板卡片视觉 | 预览图更紧凑，主子表标题和推荐徽标不遮挡、不变形 | pending-user |

### 17.4 本轮边界

- 用户已明确自行验证，因此不执行 Maven、JUnit、前端 Lint/build、API、Flyway、Vite 或浏览器。
- 本轮仅记录 `rg`、Flyway placeholder 扫描和 `git diff --check` 结果；所有真实行为保持 `pending-user`。

## 18. 2026-07-15 Phase 8 草稿图预览与字段配置分层验证

### 18.1 变更范围

- 设计预览在读取配置前刷新主对象关系图，并强制从最新草稿 Schema 编译。
- 应用发布前刷新 PRIMARY 对象聚合草稿，不再要求逐个对象发布。
- 字段资产成为字段身份、数据库映射和业务硬约束的唯一事实源；页面设计器只维护当前页面用法。
- 字段与数据库映射改为桌面固定属性栏和窄屏抽屉，移除全局页面显示/查询开关。

### 18.2 验证矩阵

| 优先级 | 验证项 | 预期 | 本轮状态 |
|--------|--------|------|----------|
| P0 | 草稿渲染契约源码 | `designPreview` 强制编译当前草稿，普通运行仍读取发布版本 | static-only |
| P0 | 主子关系刷新契约源码 | 预览前按对象刷新关系图，应用发布前只准备 PRIMARY | static-only |
| P0 | 字段所有权契约源码 | 表单编译不反写全局字段，新字段自动创建和显式字段保存仍保留 | static-only |
| P0 | 差异空白 | 目标已跟踪文件和新增契约测试无 whitespace error | static-only |
| P1 | 主子表真实预览 | 初始化后不发布对象即可看到主表和全部子表 | pending-user |
| P1 | 子字段刷新 | 修改子对象字段后再次预览，主对象明细字段同步更新 | pending-user |
| P1 | 配置隔离 | 表单标题、隐藏、控件和校验不改变列表或数据库映射 | pending-user |
| P1 | 工作台交互 | 桌面固定属性栏、小屏抽屉、滚动、未保存保护和主题适配正常 | pending-user |

### 18.3 验证边界

- 按用户分工，不运行 Maven、JUnit、前端 Lint/build、API、数据库、Vite 或浏览器。
- 新增测试仅作为后续自动化基线源码，不将“未执行”表述为通过。
- 用户重点验证：新建主子表后立即预览、修改子表字段后二次预览、同一字段在表单与列表设置不同控件/标题，以及 980px 以下字段属性抽屉。

## 19. 2026-07-15 Phase 9 字段属性、关系画布与概览密度验证

| 优先级 | 验证项 | 预期 | 本轮状态 |
|--------|--------|------|----------|
| P0 | 必填默认值源码契约 | 安全类型自动赋值；字典和引用类型不生成伪造值；用户值不覆盖 | static-only |
| P0 | ER 编辑源码契约 | 可编辑开关、字段连接、连接预览、关系选择和父层归一化调用存在 | static-only |
| P0 | 旧布局引用 | 单选关系类型、旧三列字段匹配和旧拖动处理无残留 | static-only |
| P0 | 差异空白 | Phase 9 目标文件无 whitespace error | static-only |
| P1 | 字段属性实际布局 | 420～460px 右栏分组清楚，默认值控件无类型警告 | pending-user |
| P1 | ER 拖线 | 对象卡可拖动，字段拖线可创建/更新关系，点击线可编辑 | pending-user |
| P1 | 非法连线 | 自连、目标间互连和跨域对象给出提示且不新增关系 | pending-user |
| P1 | 关系/联动小屏 | 向导、端点卡和联动流在窄屏单列显示，无重叠 | pending-user |
| P1 | 应用概览密度 | 首屏留白缩小，导航、表格行和待办仍可读可点 | pending-user |
| P1 | 应用卡片网格 | 4/3/2/1 列自适应，单卡不拉满，操作和分页始终可见 | pending-user |
| P0 | 卡片轨道余量分配 | 使用 268px 最低宽度和弹性上限，容器可放三列时不因固定 320px 上限退化为两列 | static-only |

验证边界：延续用户自行验证分工，不运行 Maven、JUnit、前端 Lint/build、API、数据库、Vite 或浏览器。

## 20. 2026-07-15 Phase 10 应用级完整代码包验证

目标契约测试类：`BusinessApplicationCodegenContractTest`

| 编号 | 场景 | 预期 | 本轮状态 |
|------|------|------|----------|
| P10-C01 | 草稿单表生成 | 最新模型/页面 Schema 生成完整 CRUD，不读取旧派生 Schema | static-only |
| P10-C02 | 左树右表生成 | 包含树对象实体/Mapper、树查询和 `TreeCrudTemplate` 页面 | static-only |
| P10-C03 | 主子表生成 | 包含明细实体/Mapper、主子 DTO、明细查询和事务保存 | static-only |
| P10-C04 | 默认生成应用 | 无对象选择时生成应用全部可用对象 | static-only |
| P10-C05 | 批量对象选择 | 只生成选择范围，主对象自动聚合已配置关系依赖 | static-only |
| P10-C06 | 文件路径冲突 | 同路径不同内容阻断并返回冲突对象，不静默覆盖 | static-only |
| P10-C07 | 发布来源 | 任一选择对象未发布时明确失败，不降级草稿 | static-only |
| P10-C08 | 预览后下载 | 设置或对象选择变化后下载禁用，重新预览后恢复 | static-only |
| P10-C09 | 无访问入口 | 只要应用存在数据对象即可生成代码 | static-only |
| P10-C10 | 权限 | 代码设置、预览和下载仅继承应用编辑角色 | static-only |

验证边界：按用户分工只补充测试源码并执行目标引用与差异空白检查，不执行 JUnit、Maven、前端 Lint/build、API、Flyway、Vite 或浏览器验证。

## 21. 2026-07-15 Phase 11 低代码协议自动适配验证

| 编号 | 场景 | 预期 | 本轮状态 |
|------|------|------|----------|
| P11-C01 | 完整协议快照 | model/page/form/view/linkage/runtime/security 均进入结构化协议文件 | static-only |
| P11-C02 | 未来嵌套字段 | 未进入当前 DTO 字段清单的嵌套 JSON 原样保留 | static-only |
| P11-C03 | 前端共享解释器 | 生成页仅传 runtime-config，不复制在线运行转换函数 | static-only |
| P11-C04 | 后端共享运行内核 | 生成业务 Controller 委托 DynamicCrudService/ExcelService | static-only |
| P11-C05 | 生成键隔离 | 使用 generated_*，不覆盖数据库原 configKey | static-only |
| P11-C06 | 主子表与左树右表 | 协议资源保留关系和布局，运行 Controller 使用同一内核 | static-only |
| P11-C07 | 失败关闭 | 缺 model/page、非法 JSON、资源键冲突均明确失败 | static-only |
| P11-C08 | ZIP 资产完整性 | frontend/backend/protocol/coverage 四类文件全部存在 | static-only |
| P11-C09 | 通用接口隔离 | 生成前端和 Controller 不包含 `/ai/crud/` URL | static-only |
| P11-C10 | 回归边界 | 原在线动态路由仍按 configKey 加载服务端配置 | static-only |

本轮沿用 Phase 10 的用户验收边界：不自动启动服务、数据库、Vite 或浏览器；代码完成后执行目标源码/模板契约、差异空白及允许的静态解析，并把真实 ZIP 和运行结果保留为 `pending-user`。

用户验收项：从应用管理分别下载单表、左树右表和主子表 ZIP，确认四类协议资产存在；把代码合并到目标 Forge 模块后编译，运行列表、树筛选、主子新增/编辑、详情、导入导出和异步导出任务；再新增一个未知嵌套协议字段并升级共享解释器，重新下载确认无需修改生成 Vue 模板即可生效。

## 22. 2026-07-15 Phase 12 下载后端静态编译验证

Phase 12 明确替代 P11-C04、P11-C05 和 P11-C08 的后端实现结论；Phase 11 的前端共享解释器和完整协议快照要求继续有效。

| 编号 | 场景 | 预期 | 本轮状态 |
|------|------|------|----------|
| P12-C01 | 静态 Controller | 只调用生成 `IService`，不引用 DynamicCrud/Excel 动态服务 | static-only |
| P12-C02 | MyBatis-Plus Service | 继承 `ServiceImpl`，基础写操作使用 MP 内置方法 | static-only |
| P12-C03 | Mapper XML 查询 | 分页、列表、树和主子明细 SQL 位于 XML，Service 无 LambdaQueryWrapper | static-only |
| P12-C04 | 主子事务 | 主表与全部明细新增、替换更新和删除在主 Service 事务内 | static-only |
| P12-C05 | 扩展链 | 分页/列表/树/详情/增改删/导入均可 around，允许跳过 proceed | static-only |
| P12-C06 | 用户文件所有权 | ZIP 不生成正式用户实现，只给 example；ownership 区分 GENERATED/CREATE_ONCE_SAMPLE | static-only |
| P12-C07 | 静态 Excel | 导出数据源指向生成 Service，导入成功数据进入 Service 事务 | static-only |
| P12-C08 | classpath 解耦 | 无 META-INF 运行配置、GeneratedLowcodeConfigRegistry 和动态服务注入 | static-only |
| P12-C09 | 前端自动适配 | 继续使用共享 LowcodeRuntimePage 和完整 runtime-config | static-only |
| P12-C10 | 在线回归 | 平台 DynamicCrudService 仍从数据库读取普通配置并支持设计预览 | static-only |
| P12-C11 | 三布局真实 ZIP | 单表、左树右表、主子表生成模块编译并运行 CRUD | pending-user |
| P12-C12 | 扩展真实执行 | 用户扩展替换复杂查询、增强新增且重新下载不覆盖 | pending-user |
| P12-C13 | 导入导出运行 | 模板、导入持久化、同步导出和字典翻译可用 | pending-user |

验证边界：本轮不执行 Maven/JUnit、前端构建、服务、数据库或浏览器；实现完成后执行目标静态扫描、模板指令平衡、JSON/XML 解析和差异空白检查，并在 `execution-log.md` 记录结果。

## 23. 2026-07-15 Phase 13 下载包命名与输出策略验证

| 编号 | 场景 | 预期 | 本轮状态 |
|------|------|------|----------|
| P13-C01 | 空/NONE 脱敏 | 实体无 `@Desensitize` 及相关 import | static-only |
| P13-C02 | 真实脱敏策略 | 匹配字段生成规范化枚举注解，总 import 只生成一次 | static-only |
| P13-C03 | 实体前缀 | 主表 Entity/DTO/Query/Mapper/Service/Controller 和文件名统一加前缀 | static-only |
| P13-C04 | 关联对象命名 | 左树和主子明细使用同一表前缀剥离、实体前缀规则 | static-only |
| P13-C05 | 表前缀列表 | 按顺序删除首个匹配前缀，空列表保留完整表名 | static-only |
| P13-C06 | 非法设置 | 非法 Java 前缀、绝对路径和 `..` 路径生成失败关闭 | static-only |
| P13-C07 | 输出根目录 | Java、Mapper XML、页面和 API 文件进入各自配置目录 | static-only |
| P13-C08 | 输出范围 | 后端/前端/Excel SQL 关闭时不生成对应文件 | static-only |
| P13-C09 | 协议资产底线 | 任意范围组合仍生成 protocol/coverage/ownership/README | static-only |
| P13-C10 | 全入口设置传递 | 应用、访问入口、低代码应用和 configKey 公共入口共用 `options.codegen` | static-only |
| P13-C11 | 设置交互 | 保存回显正确，任一设置变化都会使旧预览失效 | pending-user |
| P13-C12 | 真实 ZIP 编译 | 自定义命名/路径包可合并到目标工程并通过编译 | pending-user |

验证边界：延续用户自行验证分工，不执行 Maven/JUnit、前端 build、服务、数据库、Vite 或浏览器；实现后执行目标源码扫描、模板静态渲染约束和差异空白检查。

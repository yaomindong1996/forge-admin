# 执行日志 — 应用优先的低代码开发工作台
> status: apply
> created: 2026-07-13
> change: `app-first-lowcode-workbench`

## 1. 当前结论

- 用户已于 2026-07-13 确认 Spec，HARD-GATE 已解除。
- Phase 1 应用聚合基础已实现，目标测试、SQL/XML 静态检查和 Admin 聚合构建通过。
- Phase 2 应用优先总览已完成代码实施和静态阶段门：应用聚合分页、父业务域子树、前端 API/路由、应用列表和新建/编辑/启停/删除均已落地。
- Phase 3 应用工作台与表优先对象设计已完成代码实施和静态阶段门：七分区工作台、对象编排、表映射首屏、数据库差异预览和显式同步边界均已落地。
- Phase 4 受治理扩展中心已完成代码实施：Worker 沙箱、CSS AST、服务端白名单、版本/锁/状态机和真实扩展工作台已落地，状态为 `completed-static`。
- Phase 5 应用级协调发布已完成代码实施：不可变版本、发布运行单、就绪度、六步发布、失败恢复、兼容回滚和发布历史前端已落地，状态为 `completed-static`。
- 2026-07-14 增量完成应用总览主题/滚动、显眼发布入口和发布查询收敛；发布历史改为轻量按需检查，状态为 `completed-static`。
- 2026-07-14 增量修复数据源连接成功反馈、AiCrudPage 列表底部空白、对象发布 DDL 安全判定不一致，并在草稿应用行增加复用工作台链路的发布操作，状态为 `completed-static`。
- 本轮未启动真实服务、未执行真实数据库迁移；真实 Flyway/API/E2E 仍待用户环境回填。

## 2. 研究记录

### 2026-07-13 — Forge 应用总览与数据语义

检查范围：

- `forge-admin-ui/src/views/app-center/index.vue`
- `forge-admin-ui/src/views/app-center/components/BusinessObjectTable.vue`
- `forge-admin-ui/src/views/app-center/components/AppCard.vue`
- `forge-admin-ui/src/views/app-center/components/AppFilterBar.vue`
- `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`
- `forge-admin-ui/src/views/app-center/components/designer/*`
- `forge-admin-ui/src/api/business-app.js`
- `forge-admin-ui/src/router/index.js`
- `AiBusinessApp.java`、`BusinessAppController.java`、`BusinessAppService.java`
- `BusinessAppMapper.java`、`BusinessAppMapper.xml`
- `AiBusinessObject.java`、`AiBusinessSuite.java` 及相关服务/Mapper

结论：

- 总览主区是业务对象分组，访问入口嵌套在对象之下，不是应用优先。
- `ai_business_app` 的真实语义是访问入口，现有 Controller 日志也已统一称“访问入口”。
- 必须新增独立 `BusinessApplication` 聚合，不能重命名或复用旧表。
- 业务对象是可复用领域资产，应通过多对多关联进入应用。

### 2026-07-13 — JeeLowCode 参考分析

检查范围：

- `DbFormAddOrUpdateParam.java`
- `DbFormController.java`
- `FormFieldEntity.java`
- `FormFieldWebEntity.java`
- `EnhanceJavaEntity.java`
- `EnhanceJsEntity.java`
- `EnhanceSqlEntity.java`
- `EnhanceConstant.java`
- `HistoryController.java`

结论：

- 借鉴围绕稳定表单/表 ID 集中管理结构、页面、查询、增强和历史。
- 借鉴保存与同步数据库分离、标准 CRUD 钩子和版本历史。
- 不照搬任意在线 Java、任意 SQL、缺少隔离的脚本执行和高密度旧式配置 UI。
- 本地仓库不含 `/lowdev/tableDesign` 前端源码，未对其未公开实现作推断。

### 2026-07-13 — Forge 既有决策与安全边界

读取：

- `AGENTS.md`
- `code-copilot/memory/pitfalls.md`
- `code-copilot/memory/decisions.md`
- `code-copilot/memory/preferences.md`
- `code-copilot/rules/automated-testing-standard.md`
- 已归档 `form-first-business-object-designer` 和低代码应用相关 Spec/Tasks

结论：

- 应用导航采用应用优先；对象创建采用表可见；业务表单深链继续保留。
- 继续复用 FormDesignerSchema、FieldRegistry、ViewSchema、LinkageSchema 和运行态编译链路。
- 页面方案不使用 `ui-ux-pro-max`。
- 真实服务、Flyway 和数据库联调由用户执行或另行授权。

## 3. 文档产物

| 文件 | 状态 | 说明 |
|------|------|------|
| `spec.md` | completed | 背景、现状、架构、Phase 0～5、数据、API、安全、兼容、验收和 HARD-GATE |
| `tasks.md` | completed | 46 个任务，按可部署阶段拆分 |
| `test-spec.md` | completed | P0/P1/P2 测试矩阵、命令、真实联调回填边界 |
| `execution-log.md` | completed | 本轮研究和文档验证记录 |
| Phase 1 Java/Mapper | completed | 新应用聚合、对象编排、入口归属、Binding 目标和删除保护 |
| `V1.0.27__add_business_application_aggregate.sql` | completed-static | 表结构、字典、权限和确定性回填；未在真实数据库执行 |
| Phase 2 后端聚合 | completed | 一次聚合返回对象/入口/流程/扩展/问题数，父业务域递归包含子域 |
| Phase 2 前端总览 | completed | 左业务域树、右应用列表、独立 API/路由、两步新建和初始化失败重试 |
| Phase 2 运行态验收 | pending | 未启动 Admin/Vite、未执行 Flyway；浏览器、真实 API 和性能基线待回填 |
| Phase 3 后端编排 | completed | 工作台/就绪度、表映射、设计版本校验、DDL 预览/同步和同步结果记录 |
| Phase 3 前端工作台 | completed | 七分区、对象编排、从数据库表初始化、入口和自动化按需分区 |
| Phase 3 对象设计器 | completed | 数据结构默认首屏、全分区表映射摘要、字段网格、DDL 预览/导出/确认 |
| Phase 3 运行态验收 | pending | 未启动 Admin/Vite、未连接运行数据源；真实 API、元数据、DDL 和浏览器场景待回填 |
| Phase 4 扩展治理 | completed-static | 扩展表、CRUD、版本/锁、Worker/CSS/服务注册表、状态机、审计和工作台已实现 |
| Phase 4 运行态验收 | pending | 最终复验发现本地 `vitest` 命令缺失；真实 Flyway/API/浏览器由用户验证 |
| Phase 5 发布协调 | completed-static | 不可变版本、运行单、就绪度、发布/恢复/回滚、轻量历史和批量查询优化已实现 |
| `V1.0.29__add_business_application_release_coordination.sql` | completed-static | 正式脚本已创建，未在真实数据库执行 |
| Phase 5 自动化/构建/E2E | pending | 用户明确要求停止验证，由其自行执行 |

## 4. 本轮验证记录

| 时间 | 范围 | 命令 | 结果 | 警告/跳过 |
|------|------|------|------|-----------|
| 2026-07-13 | 文档创建 | 四个目标文件非空、标题和状态检查 | passed | 四份文档均存在且状态为 `propose` |
| 2026-07-13 | Markdown 差异 | 对四个未跟踪文档逐一执行 `git diff --no-index --check /dev/null <file>` | passed | 每个命令仅因文件有差异返回 1，均无空白错误输出 |
| 2026-07-13 | 文档一致性 | `rg` 检查状态、Phase、HARD-GATE、未决占位词和禁用项 | passed | 无未决占位标记或错误的规范目录引用；明确禁用任意 Java/SQL 和 `ui-ux-pro-max` |
| 2026-07-13 | 默认测试行为 | generator 目标测试命令未加 `-Penable-tests` | skipped | Maven 构建成功但项目默认跳过测试；后续命令显式启用 profile |
| 2026-07-13 | 兼容测试首次启用 | `-Penable-tests` 执行原 Mockito 版兼容测试 | failed | Mockito inline 无法在 Homebrew JDK 17 自附加；未修改生产代码，测试改为反射/动态代理 |
| 2026-07-13 | 访问入口兼容基线 | `mvn -Penable-tests ... -Dtest=BusinessAppServiceCompatibilityTest,BusinessAppControllerCompatibilityTest ... test` | passed | 8 tests；旧 `/ai/business/app` 表、字段、路由和权限契约保持 |
| 2026-07-13 | Phase 1 Red 契约 | 新应用契约测试首次编译 | failed-as-expected | 在生产类型尚未完成时缺少 `BusinessApplication*` 契约，随后按 Spec 实现 |
| 2026-07-13 | Phase 1 中间回归 | 兼容测试 + 初版应用契约 | passed | 13 tests；随后继续补服务行为、Binding 和回填测试 |
| 2026-07-13 | 测试代码修复 | `BusinessBindingApplicationTargetTest` 首次全组编译 | failed | 测试方法声明缺失；修复测试语法后生产代码无需调整 |
| 2026-07-13 | Phase 1 行为回归 | 六个 Phase 1/兼容测试类 | passed | 22 tests，0 failure、0 error |
| 2026-07-13 | Phase 1 最终目标测试 | `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am -Dtest=BusinessApplicationServiceTest,BusinessApplicationObjectServiceTest,BusinessBindingApplicationTargetTest,BusinessApplicationPhaseOneContractTest,BusinessApplicationBackfillContractTest,BusinessApplicationControllerTest,BusinessAppServiceCompatibilityTest,BusinessAppControllerCompatibilityTest -Dsurefire.failIfNoSpecifiedTests=false test` | passed | 37 tests，0 failure、0 error、0 skipped；JDK 17 |
| 2026-07-13 | Mapper XML | `xmllint --noout BusinessApplicationMapper.xml BusinessApplicationObjectMapper.xml BusinessAppMapper.xml` | passed | XML 语法通过；查询显式过滤租户和逻辑删除 |
| 2026-07-13 | Flyway 静态门禁 | `rg` 检查 `${...}`、tenant 0、INSERT 列、逻辑删除、回填去重和兜底路径 | passed | 无 Flyway 占位符、无 tenant 0 内置数据、无新聚合物理删除；未连接数据库 |
| 2026-07-13 | Admin 聚合构建 | `mvn -pl forge-admin-server -am package -DskipTests` | passed | 42/42 Reactor 模块成功，43.293s；现有 deprecated/unchecked/Lombok builder 警告 |
| 2026-07-13 | 差异空白检查 | `git diff --check` | passed | 无空白错误；用户原有 `.DS_Store` 和 `preferences.md` 改动保持未覆盖 |
| 2026-07-13 | Phase 2 Mapper Red/Green | `BusinessApplicationMapperTest` 首次执行后再实现聚合与子树 | passed | 初次 3 个测试中 2 个失败；实现后 3/3 通过 |
| 2026-07-13 | Phase 2 最终目标测试 | `mvn -Penable-tests ... -Dtest=BusinessApplicationMapperTest,...,BusinessAppControllerCompatibilityTest ... test` | passed | 41 tests，0 failure、0 error、0 skipped；JDK 17，15.149s |
| 2026-07-13 | Phase 2 定向 ESLint | `pnpm exec eslint` 检查新 API、路由、总览、工作台和四个新组件 | passed | 0 error、0 warning；Node v20.19.0 |
| 2026-07-13 | Phase 2 前端生产构建 | `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | passed | Vite 8501 modules，1m23s；仅仓库既有组件重名、import/chunk 和 CSS 注释警告 |
| 2026-07-13 | Phase 2 Mapper XML | `xmllint --noout` 检查 Application、ApplicationObject、App、Suite 四个 Mapper | passed | XML 语法通过 |
| 2026-07-13 | Phase 2 总览静态契约 | `rg` 检查对象分组依赖、新旧 API 和三条兼容路由 | passed | 总览无对象分组依赖；新应用/旧入口 API 分离；新应用、旧入口、旧对象设计路由并存 |
| 2026-07-13 | Phase 2 Flyway 静态复核 | `rg` 检查 `V1.0.27` 的 `${...}` 和 tenant 0 内置数据 | passed | 无 Flyway 业务占位符、无 tenant 0 内置数据；未执行数据库迁移 |
| 2026-07-13 | Phase 2 Admin 聚合构建 | `mvn -pl forge-admin-server -am package -DskipTests` | passed | 42/42 Reactor 模块成功，20.845s；JDK 17 |
| 2026-07-13 | Phase 2 最终差异空白检查 | `git diff --check` | passed | 无空白错误；用户原有 `.DS_Store`、`preferences.md` 状态未覆盖 |
| 2026-07-13 | Phase 3 Red 契约 | 首次执行 Workspace/TableMapping/DatabaseSync 三组测试 | failed-as-expected | 生产类型尚未实现，缺少 `BusinessApplicationWorkspaceVO`、`BusinessObjectTableMappingService` 和设计上下文接口 |
| 2026-07-13 | Phase 3 首轮 Green | 三组新增测试 | passed | 11 tests；修复测试动态代理将字符串误与 `Method` 对象比较的问题后全绿 |
| 2026-07-13 | Phase 1～3 合并回归 | 13 个目标测试类与兼容测试 | passed | 56 tests，0 failure、0 error、0 skipped；JDK 17，13.873s |
| 2026-07-13 | Phase 3 Controller 契约 | Workspace/readiness/table-mapping/database-diff/database-sync 映射与权限 | passed | 工作台复用 list 权限；预览使用 design 权限；同步独立要求 `ai:lowcode:deploy-ddl` |
| 2026-07-13 | Phase 3 DDL 边界 | 显式确认、版本冲突、无权限、`allowDdl=0`、MODIFY 和安全 ADD COLUMN | passed | 预览不执行；保存对象不再触发 DDL；仅 CREATE/ADD COLUMN 可在线同步 |
| 2026-07-13 | Phase 3 Mapper XML | `xmllint --noout` 检查 Application/ApplicationObject/App/Suite Mapper | passed | XML 语法通过；共享应用数和运行表摘要通过 XML 聚合返回 |
| 2026-07-13 | Phase 3 定向 ESLint | 工作台、对象设计器、API、向导和新增组件 | passed | 0 error、0 warning；Node v20.19.0 |
| 2026-07-13 | Phase 3 前端生产构建 | `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | passed | Vite 8518 modules，1m09s；仅仓库既有组件重名、动态导入和 CSS 注释警告 |
| 2026-07-13 | Phase 3 Admin 聚合构建 | `mvn -pl forge-admin-server -am package -DskipTests` | passed | 42/42 Reactor 模块成功，18.882s；JDK 17 |
| 2026-07-13 | Phase 3 差异检查 | `git diff --check` 与新增文件尾随空白扫描 | passed | 当前差异无空白错误；未覆盖用户原有 `.DS_Store` 和 `preferences.md` 变更 |
| 2026-07-13 | Phase 1～4 后端合并回归 | generator 目标测试 | passed | 历史执行证据：75 tests，0 failure、0 error、0 skipped |
| 2026-07-13 | Phase 4 前端安全用例 | JS Worker 与 scoped CSS Vitest | passed | 历史执行证据：26 tests 通过 |
| 2026-07-13 | Phase 4 Admin 聚合构建 | `mvn -pl forge-admin-server -am package -DskipTests` | passed | 历史执行证据：42/42 模块通过 |
| 2026-07-13 | Phase 4 最终前端复验尝试 | Node v20.19.0 下 `pnpm exec vitest run ...` | not-run | `Command "vitest" not found`，本地 `node_modules` 恢复不完整；未进入测试执行 |
| 2026-07-13 | Phase 5 实施 | Task 35～41 代码、迁移、契约用例和发布历史前端 | completed-static | 用户明确“你不用给我验证，我自己验证”；未运行测试、Lint、构建、Flyway、API 或浏览器 |
| 2026-07-14 | 扩展中心体验增量 | 钩子矩阵、JS/CSS 引导式代码工作台、类型/处理器钩子约束、Java 服务增强失败语义、开发指南和契约用例 | completed-static | 遵照用户要求未运行测试、Lint、构建、Flyway、API 或浏览器 |
| 2026-07-14 | 扩展测试与发布阻断修复 | 所有扩展统一进入测试台、Java Schema 测试输入、分阶段测试结果、默认发布跳过未测试草稿 | completed-static | 遵照用户要求未运行测试、Lint、构建、API 或浏览器 |
| 2026-07-14 | Worker 异常诊断修复 | READY 握手、阶段化错误、初始化/执行超时、ErrorEvent 行列原因、postMessage 协议加固 | completed-static | 遵照用户要求未运行 Vitest、构建或浏览器验证 |
| 2026-07-14 | Worker `window` 启动异常修复 | Vite `define.global` 从 `window` 调整为跨主线程/Worker 可用的 `globalThis`，消除 `env.mjs` 初始化阶段 `ReferenceError` | completed-static | 配置变更需重启 Vite 开发服务后生效；遵照用户要求未运行构建或浏览器验证 |
| 2026-07-14 | JS 沙箱测试数据引导化 | 自动识别脚本字段/动作、加载对象字段和动作目录、类型化测试值、示例/空值预设、场景说明与高级 JSON 只读预览 | completed-static | 删除用户手工维护“允许字段逗号串 + 上下文 JSON”的双输入；遵照用户要求未运行 Lint、构建或浏览器验证 |
| 2026-07-14 | 应用总览与发布性能增量 | 业务域主题/滚动、右侧标题精简、顶部发布入口、发布历史轻量加载、发布上下文复用和批量查询 | completed-static | 按用户分工未运行测试、Lint、构建、Flyway、API、数据库日志对比或浏览器验证 |
| 2026-07-14 | 数据源/AiCrud/对象发布/列表发布缺陷修复 | 连接消息生命周期、表体最小高度、DDL 预检与执行统一白名单、草稿应用发布深链 | completed-static | 只执行静态引用与差异空白检查；按用户分工未运行测试、Lint、构建、API 或浏览器 |

## 5. 验证边界与未执行项

| 项目 | 状态 | 原因 |
|------|------|------|
| 后端单测/构建 | pending-phase5 | Phase 4 历史 75 tests/42 模块通过；Phase 5 新增代码未运行 |
| 前端 build/lint | pending-phase5 | Phase 4 曾构建通过；最终复验依赖缺失，Phase 5 未运行 |
| Flyway 静态检查 | pending-phase5 | `V1.0.27/V1.0.28` 有历史静态证据；`V1.0.29` 未执行本轮验证 |
| Flyway 真实执行 | pending | 未获真实数据库执行授权 |
| API 验证 | pending | 未启动 Admin 服务；待真实迁移后联调 |
| 浏览器/Playwright | pending | Phase 2/3 有页面改动，但按约定未启动 Admin/Vite；待用户环境回填 P2-U01～P2-U08、P3-U01～P3-U10 |
| SQL 次数/性能 | pending-user | 已完成发布历史大字段裁剪、上下文复用和批量查询；未运行真实数据库日志与耗时对比，不能宣称性能基线已通过 |
| 真实 E2E | pending | 由用户执行 Flyway、API 和浏览器验收后回填 |

## 6. 服务清理

- 本轮启动服务：无。
- 本轮停止服务：无。
- 遗留 PID：无。

## 7. 下一步

1. 用户执行或授权真实 `V1.0.27` 迁移，回填迁移计数、未归属入口和旧入口回归证据。
2. 在真实 Admin/Vite/运行数据源环境回填 P2-U01～P2-U08、P3-U01～P3-U10、聚合 SQL 次数、接口耗时和数据库差异预览证据。
3. Phase 4/5 代码已实施；用户恢复前端依赖后按 `test-spec.md` 执行 Phase 5 测试、构建、Flyway 和故障恢复验收。

## 12. Phase 4 安全设计复核

### 12.1 复核结论（2026-07-13）

- **CLIENT_JS 宿主**：存储脚本只发送到独立 module Worker；宿主页不使用 `eval`、
  `new Function`、Blob script 或动态模块执行。Worker 在执行前关闭网络、存储、计时器和
  动态加载入口，宿主以超时主动 `terminate()`，并校验随机 nonce、输出大小和结构化 effects。
- **SCOPED_CSS**：前端使用 CSS AST 解析器，不用字符串拼接充当解析器；保存前拒绝
  `@import`、URL、全局根选择器和 Forge 布局选择器，再把普通选择器统一重写到应用/页面
  `data-*` 根节点。后端保留独立失败关闭校验，不能仅信任浏览器结果。
- **SERVER_BINDING**：只按平台注册表中的稳定 `handlerCode` 查找处理器；协议不接受
  Bean 名、Class 名或类路径，不使用 `Class.forName`、`ApplicationContext#getBean(String)`
  等反射入口。处理器声明允许钩子、输入 Schema、超时、风险和权限。
- **版本与锁**：历史版本只新增不覆盖；回滚产生新草稿。编辑锁绑定 tenant、extension、
  user 和随机 token，并通过条件更新实现超时接管与跨租户失败关闭。
- **依赖与迁移**：前端已有 Vitest，CSS AST 解析器将作为直接依赖固化；正式迁移最新为
  `V1.0.27`，Phase 4 使用下一单调版本 `V1.0.28`，不修改已存在脚本。

结论：主页面动态执行和任意服务反射两个硬门均有可验证的失败关闭方案，可以进入 Task 26。

## 8. HARD-GATE 确认

| 时间 | 确认人 | 内容 | 结果 |
|------|--------|------|------|
| 2026-07-13 | 用户 | “确认 Spec，开始实施 Phase 1” | 已解除 Proposal 阻断，进入 apply |

## 9. Phase 1 实施摘要

### 9.1 应用聚合与对象编排

- 新增 `ai_business_application`、`ai_business_application_object` 及对应 Entity、DTO、VO、Mapper XML、Service、Controller。
- 新应用编码创建后不可修改，同租户未删除唯一；扩展配置拒绝敏感密钥并校验 JSON 对象。
- 应用对象角色固定为 `PRIMARY/DETAIL/REFERENCE/SHARED`，最多一个主对象；对象继续可被多个应用复用。
- 删除应用时阻止启用入口，分离停用入口，只逻辑删除应用编排和应用本身，不删除业务对象。

### 9.2 兼容边界

- 现有 `ai_business_app`、`/ai/business/app` 和旧权限保持访问入口语义，仅新增可空 `application_id`。
- 入口指定应用时校验租户、业务域和已编排对象；存量/兼容路径仍允许暂时为空。
- Binding 新增 `APPLICATION` 目标；旧 `APP` 仍只校验访问入口 ID，语义未改变。
- 业务域和业务对象删除/移动增加新应用关联保护。

### 9.3 回填策略

- 以租户、业务域和主候选对象生成稳定 `migrated_<md5>` 应用编码。
- 直接明细、引用和多对多对象按角色关联；同一目标的多种关系在插入前按角色优先级归并，避免唯一键冲突。
- 仅能唯一解析的入口绑定对象应用；歧义或无对象入口进入业务域唯一 `legacy_<md5>` 历史入口应用。
- READY 回填仅作用于未改动的迁移草稿，要求主对象和启用入口，避免重跑覆盖人工发布状态。

## 10. Phase 2 实施摘要

### 10.1 应用聚合和业务域筛选

- `BusinessApplicationVO` 新增流程、扩展和待处理问题数量；分页 SQL 通过对象、入口和 `APPLICATION` Binding 预聚合一次返回，不做逐应用查询。
- 流程只统计 `FLOW/APPROVAL`，其余 `APPLICATION` Binding 暂计为扩展；旧 `APP` 访问入口目标不混入。
- `BusinessSuiteMapper` 使用 MySQL 8 `WITH RECURSIVE` 返回当前业务域及全部子域；应用分页由 Service 自动扩展 `suiteCodes`。
- 业务域摘要同时返回新应用总数和启用数，旧 `appCount` 继续表示访问入口；前端父域展示对子树计数求和，与筛选口径一致。

### 10.2 应用优先总览

- 总览保留左侧业务域树，右侧只展示应用主记录；对象、入口、流程和扩展仅作为应用资产计数。
- 新增关键词、设计状态、启停状态和分页筛选，并把筛选状态同步到路由 query；列表支持进入、编辑、启停和删除。
- 新增 `/app-center/application/:applicationCode` 稳定工作台路由和 Phase 2 摘要页；旧 `/app-center/app/:appId` 与对象设计路由保留。
- 总览不再调用 `businessObjectList`、`businessAppList`、`businessObjectRelations`，也不再渲染 `BusinessObjectTable`。

### 10.3 新建和失败恢复

- 新建采用“基本信息 → 初始化方式”两步；空白应用和绑定已有对象可用，数据库表/模板/AI 草稿明确标记为后续阶段且不可选择。
- 应用先落 DRAFT，再绑定 PRIMARY 对象；绑定失败时保留已创建应用 ID，抽屉不关闭，可更换对象重试或转为空白应用进入，不会重复创建草稿。
- 编辑时编码不可修改；已有对象或入口时前端禁用业务域切换，后端继续执行最终约束。

## 11. Phase 3 实施摘要

### 11.1 七分区应用工作台

- 工作台首屏新增概览、数据对象、页面入口、流程自动化、动作与增强、权限、发布历史七个固定分区，query 可恢复当前分区。
- 后端 workspace/readiness 只返回首屏摘要、分区计数和问题；对象、入口和自动化详情在打开对应分区时加载。
- 无 PRIMARY、无启用入口是阻断；对象停用和未发布是提醒；可选流程缺失不阻断。
- 页面使用紧凑命令栏、左侧分区导航和右侧工作区，没有统计卡、渐变、大型装饰标题或持续动画。

### 11.2 数据对象编排和数据库表锚点

- 数据对象分区支持关联已有对象、空白新建、从数据库表导入、角色调整、设主对象和移除关联；移除不删除对象或物理表。
- 应用对象列表一次返回运行数据源、物理表、设计版本、同步状态和共享应用数；同步版本落后于当前设计时显示 `OUT_OF_SYNC`。
- 应用新建的“从数据库表开始”已启用：先创建应用草稿，再进入对象分区打开导入向导，初始化失败不删除应用。
- 空白、数据库导入和 AI 对象创建后统一进入 `?panel=fields`，应用上下文锁定业务域。

### 11.3 表映射与 DDL 安全边界

- `BusinessObjectDesignContextProvider` 让表映射服务复用现有对象设计上下文，不复制 ModelSchema、PageSchema 或设计版本。
- 数据结构首屏展示业务字段名、字段编码、数据库列、设计/实际类型、可空、默认值、控件和同步状态；未映射数据库列仍保留可见。
- 对象设计保存接口只保存设计元数据，旧 `syncDdl/confirmSyncDdl` 字段保留反序列化兼容但不再执行 DDL。
- database-diff 只预览；database-sync 依次校验设计版本、显式确认、独立权限、`allowDdl`、只读状态和 DDL 类型。
- 在线执行只允许 CREATE TABLE 和 ADD COLUMN；MODIFY/ALTER/DROP/RENAME 及其它非追加式语句只允许预览和导出。
- 同步成功/失败摘要写入对象 `designerOptions.databaseSync`，不新增第二套历史表；草稿和数据库同步失败互不回滚。

### 11.4 兼容与阶段边界

- 旧 `/app-center/object/:objectCode/designer`、`/app-center/app/:appId`、对象发布检查和版本回滚继续可用。
- 表单/列表/详情深链继续有效；表映射摘要在所有对象设计分区可见，“数据结构”导航常驻并成为普通对象默认首屏。
- Phase 4/5 已在后续阶段落地：扩展中心提供 Worker 沙箱、CSS 作用域和显式服务白名单，发布历史提供不可变应用快照、可恢复运行单和受限回滚。

## 13. Phase 4 实施摘要

- `V1.0.28` 建立扩展、扩展版本和执行日志数据结构，扩展生命周期统一为草稿、测试、启用、停用和归档状态机。
- CLIENT_JS 只在独立 Worker 中执行，限制消息协议、运行超时和可用能力；SCOPED_CSS 经 AST 校验后限定应用/对象作用域；SERVER_BINDING 只能选择显式注册处理器，不接受类名或反射调用。
- 扩展编辑采用版本、差异和显式锁；测试结果、运行摘要和错误信息执行脱敏，前端工作台不提供任意 Java、SQL 或主页面脚本执行入口。
- Phase 4 历史验证证据已记录；最终复验因本地前端依赖恢复不完整未进入测试执行，阶段状态保持 `completed-static`。

## 14. Phase 5 实施摘要

- `V1.0.29` 分离不可变应用版本和可恢复发布运行单；版本只插入，运行单记录幂等键、目标版本、六步进度、失败位置和恢复次数。
- 发布固定执行 `PRECHECK → SNAPSHOT → OBJECTS → ENTRIES → EXTENSIONS → COMMIT`；同一应用的版本号在数据库锁内预留，运行单通过 `CREATED → RUNNING` 条件更新认领。
- 发布快照递归清理敏感键并计算 SHA-256；对象发布版本逐个写回运行单快照，中途失败后恢复会跳过已持久化完成的对象。
- 就绪度聚合应用/业务域、主对象、对象发布检查、数据库同步、入口、流程、扩展、权限和共享对象影响；阻断项携带工作台定位信息。
- 部分失败保留 `PARTIAL/FAILED` 和脱敏摘要；回滚先校验对象设计版本、物理表/字段、入口、扩展版本和 APPLICATION Binding，只恢复兼容配置并生成新的 `ROLLBACK` 版本，不执行反向 DDL 或业务数据回滚。
- 前端发布历史已接入发布检查、问题跳转、协调发布、版本详情、步骤查看、失败恢复和回滚边界确认。
- 用户明确自行验证，因此本轮未运行 Phase 5 自动化测试、Lint、构建、Flyway、API 或浏览器验收，状态为 `completed-static`。

## 15. Phase 6 用户验收问题修复

- 用户反馈业务域树缺少色彩层次、应用图标全部呈现为同一蓝色、窄视口看不到“进入应用”按钮，以及进入应用工作台跳转 403。
- 业务域导航改为克制的青绿色控制台配色，不同业务域图标按稳定编码分配色相，并补齐暗色主题。
- 应用图标按应用编码确定性分配多色浅底；列表最小宽度提升并显式承载横向滚动，操作列固定在可视区右侧。
- 403 根因为手写动态路由未进入 `sys_resource` 授权树；新增 `V1.0.30__add_business_application_workspace_route.sql`，注册隐藏路由并只向已有 `ai:businessApplication:list` 的角色继承授权。
- 本轮按用户分工只完成修复，未运行前端构建、Flyway、API 或浏览器验证，等待用户继续 Phase 6 验收。
- 用户复验继续反馈配色过于突兀、横向滚动仍不可用且操作列背景变成主题蓝色。进一步定位为 `NSpin` 容器限制滚动宽度，以及操作列误用会被 Naive 子组件上下文覆盖的 `--n-color`。
- 第二轮修复将业务域降为低饱和灰绿，移除多色业务域图标；把横向滚动层移动到 `NSpin` 内容内部并强制独立滚动；操作列改用私有表面变量和中性按钮，避免主题蓝色污染。仍待用户复验。
- 用户第三次明确要求应用总览下的整张应用列表滚动，不接受固定操作列。最终结构移除操作列横向 sticky，列表区域使用 `clamp(320px, calc(100dvh - 360px), 680px)` 明确视口高度，表头、数据行和操作列在同一滚动内容中整体横向移动，横向和纵向统一由 `.table-scroll` 承载。
- 2026-07-14 目标 ESLint：`pnpm exec eslint src/views/app-center/index.vue src/views/app-center/components/ApplicationTable.vue --max-warnings 0`，结果 passed，0 error、0 warning。
- 2026-07-14 前端生产构建：Node v20.19.0，`pnpm build`，结果 passed；Vite 7.3.1 转换 8670 modules，`built in 1m 17s`。仅保留仓库既有组件重名、动态/静态导入和 CSS `//` 注释警告。
- 第三轮最终 CSS 调整后再次执行目标 ESLint 和 `pnpm build`：ESLint 0 error、0 warning；Vite 转换 8670 modules，`built in 1m 13s`，结果 passed。
- 浏览器验证尝试：Vite 启动先报 `EMFILE`，提高当前 shell 文件句柄后被环境以 `listen EPERM 127.0.0.1:3000` 阻止；Playwright Chromium 被 Mach IPC 权限拒绝，WebKit 未安装。未把浏览器交互写成通过，本轮未遗留启动服务。

## 16. 2026-07-14 应用工作台主题适配修复

### 16.1 变更范围

- `application.[applicationCode].vue`：去掉包裹整个工作台的 `NSpin`，改为绝对定位的独立加载遮罩，阻断 Naive 组件变量向业务页面传播。
- `application-workspace/*.vue`：背景、侧栏、表头、悬停、边框及正文/次级文字统一改用 Forge `design-tokens.css` 的明暗主题语义变量。
- 未使用用户明确排除的 `ui-ux-pro-max`，未重做工作台结构和配色体系。

### 16.2 执行证据

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec eslint 'src/views/app-center/application.[applicationCode].vue' \
  src/views/app-center/application-workspace/*.vue --max-warnings 0
```

结果：最终复跑通过，0 error、0 warning。首次执行发现 `ApplicationPublishPanel.vue` 有 8 个既有格式/UnoCSS 顺序警告，执行目标文件 `eslint --fix` 后重新检查通过。

```bash
rg -n --glob '*.vue' -- 'var\(--n-[a-z0-9-]+' \
  src/views/app-center/application-workspace \
  'src/views/app-center/application.[applicationCode].vue'
```

结果：无输出；工作台业务样式已不再依赖可能从 Naive 子组件继承的背景、文字、边框、主色或状态色变量。

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：最终复跑通过；Vite 7.3.1 转换 8670 modules，`built in 1m 41s`。保留仓库既有组件重名、动态/静态 import、CSS `//` 注释警告，不由本轮主题修复引入。

### 16.3 验证边界与服务清理

- 明暗主题浏览器切换：`pending-user`，由用户在真实环境验收。
- 本轮未启动 Admin、Vite 或浏览器服务，未修改数据库，未遗留 PID。

## 17. 2026-07-14 工作台性能、数据与对象设计整合

### 17.1 变更结果

- 进入应用由“按编码详情 → 按 ID 工作台”两个串行请求收敛为一次 `/by-code/{applicationCode}/workspace` 快照请求。
- 工作台快照直接返回对象、入口和不含脚本正文的扩展摘要；左侧数量与右侧列表使用同一快照。
- 普通工作台快照不再执行对象发布检查、权限全量检查和数据库结构全量检查；完整发布门禁仍由发布接口显式执行。
- 分区切换改用 `KeepAlive`，对象、入口、流程和扩展面板首次打开后保留状态，不再销毁重建。
- 扩展列表的对象、入口和处理器依赖延迟到打开编辑器时加载；普通查看不再并发请求无关数据。
- `PROCUREMENT_WAREHOUSE_PRODUCT_RUNTIME` 等技术编码不再作为入口主名称；`RUNTIME/ROUTE/IFRAME/EXTERNAL/H5/API` 由 `V1.0.31` 字典显示为中文。
- 数据结构、表单、列表、业务动作、流程、触发器和权限继续复用原对象设计器，但以内嵌模式在应用工作台当前页面展开，返回后回到原应用分区。

### 17.2 前端验证

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec eslint src/api/business-application.js \
  'src/views/app-center/application.[applicationCode].vue' \
  src/views/app-center/application-workspace/*.vue \
  'src/views/app-center/object-designer.[objectCode].vue' \
  src/views/app-center/components/designer/BusinessObjectDesignerShell.vue \
  --max-warnings 0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

结果：ESLint 0 error、0 warning；Vite 7.3.1 转换 8670 modules，`built in 1m 50s`。构建仅保留仓库既有组件重名、动态/静态 import 和 CSS `//` 注释警告。

静态契约检查通过：存在 `KeepAlive` 和单次按编码工作台 API；对象、自动化、动作面板不含 `window.open` 或对象设计路由；工作台不直接插值渲染 `entryMode/objectRole`。

### 17.3 后端与迁移验证

首次带 `-am` 的目标测试命令在上游 `forge-starter-datascope` 被既有 Surefire/JUnit 引擎配置阻断。改为直接执行 generator 后，生产源码重新编译 518 个文件成功；测试源码编译阶段被 7 个既有测试阻断，这些测试的构造器参数未跟随 Phase 4/5 生产类新增的 `BusinessApplicationChangeTracker` 更新，因此本轮目标用例未执行，不表述为通过。

```bash
xmllint --noout \
  forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessExtensionMapper.xml

rg -n '\$\{[^}]+\}|tenant_id[^\n]*(DEFAULT 0|= 0)|VALUES[^\n]*, *0 *,' \
  db/migration/V1.0.31__add_business_app_entry_mode_dict.sql
```

结果：Mapper XML 通过；Flyway 风险扫描无输出。`V1.0.31` 未连接真实数据库执行。

### 17.4 验证边界与服务清理

- 真实浏览器请求数、耗时、快照数据和内嵌对象设计交互：`pending-user`。
- 本轮未启动 Admin、Flow、Vite 或浏览器服务，未执行数据库迁移，未遗留 PID。

## 18. 2026-07-14 应用总览与发布性能增量

### 18.1 应用总览和发布入口

- 左侧业务域删除绿色专用配色，统一使用 Naive/Forge 主色、背景、边框和文字主题变量；树列表采用 `minmax(0, 1fr)` 并独立纵向滚动。
- 右侧删除“全部业务域 / 应用总览 / 以可交付应用为主线……”重复标题区；筛选、新建、结果数量直接组成工具栏。
- 应用表格的横向和纵向滚动继续由同一个 `.table-scroll` 承载，表头和操作列处于同一滚动内容。
- 小屏不固定操作列，而是通过 `ResizeObserver` 按列表真实可用宽度渐进隐藏业务域、资产、更新时间和状态等次要列；紧凑模式保持“应用 → 状态 → 操作”的正常顺序，应用列取消最小宽度；最窄模式隐藏状态后将操作补到第二列。
- 分页导航与每页条数选择器拆分为可换行控件组，小屏隐藏辅助说明并保留页码、上一页/下一页和每页数量配置。
- 根据用户截图修正紧凑 Grid 的隐式换行：应用、操作、状态显式锁定 `grid-row: 1`，消除表头错层和记录行异常增高；分页区删除冗余说明并独占整行，顶部筛选与主操作允许换行，避免每页条数和“新建应用”被裁切。
- 发布入口收敛为工作台顶部唯一主按钮：点击后通过一次性请求标记切换到发布历史并立即打开发布确认，发布面板删除重复的“发布当前应用”按钮；请求标记消费后立即清零，避免重新挂载时重复弹窗。
- 确认发布后立即显示全局“正在执行发布检查并发布应用”进度并锁定重复操作；发布接口错误改为就地展示后端原因，超时明确提示先刷新运行记录避免重复发布，历史刷新失败不再覆盖真实发布结果。

### 18.2 发布历史查询收敛

- 进入发布历史只并发读取版本摘要和运行单摘要，不自动执行完整就绪度检查。
- 版本列表不读取 `snapshot_json`；运行单列表不读取 `snapshot_json`、`snapshot_hash`、`selection_json` 和幂等键。
- 完整检查改为“执行发布检查”显式动作；直接发布不再由前端先额外调用检查接口，后端发布接口仍执行最终权威检查。
- 前端发布检查超时保护调整为 60 秒，发布、恢复和回滚调整为 120 秒；该调整仅作为后端优化后的保护边界。

### 18.3 发布链路后端优化

- 发布检查使用轻量应用上下文，不再复用带对象/入口/流程/扩展聚合子查询的应用详情 SQL。
- 对象、入口、扩展、应用绑定和权限摘要在发布检查阶段装载一次，并直接传递给快照阶段复用；快照阶段只补查确实需要的扩展版本。
- 权限资源按全部选中对象一次查询；扩展版本按 `(extension_id, version_no)` 批量查询；全部对象最新发布版本使用 MySQL 8 窗口函数一次查询。
- 对象发布保存草稿后复用当前设计上下文执行检查，不再重新装载完整对象上下文。
- 首次发布复用创建运行单前的检查结果；恢复发布仍重新检查当前状态。
- 运行单每步更新后直接维护内存实体，成功认领后不再回查；只有并发认领失败时才读取数据库中的最新运行单。
- 发布历史查询不再额外读取应用详情；不可变版本提交由最终 `markPublished` 更新结果确认应用仍存在。

### 18.4 验证边界

- 本轮只完成代码实施和静态一致性核对，状态为 `completed-static`。
- 遵照用户要求，未运行 Maven、前端 Lint/build、Mapper 执行、真实数据库日志对比、API 或浏览器验证。
- 用户需重点回填：左侧业务域实际滚动、主题切换、顶部发布入口、打开发布历史 SQL 数量、发布总耗时和超时情况。

## 19. 2026-07-14 数据源、通用列表和发布缺陷修复

### 19.1 变更结果

- 数据源“测试连接”不再给持久 loading 传入会被消息封装立即销毁的 `duration: 0`；消息键按数据源 ID 隔离，成功或失败会更新为明确结果。
- `AiCrudPage/AiTable` 有数据表体与内部滚动层取消 `144px` 强制最小高度，分页器紧跟最后一行；无数据空状态继续保持 `180px`，避免空页面塌缩。
- DDL 发布检查、显式同步和最终执行统一复用 `LowcodeDdlService.containsUnsafeOnlineDdl`；在线只接受建表、追加列及受控索引/注释。`MODIFY/ALTER COLUMN` 会在发布检查阶段以“查看数据库差异”阻断，并明确要求预览/导出后人工审核，不再进入执行阶段抛出模糊白名单异常。
- DDL 执行改为先校验完整语句列表再逐条执行，避免前序追加语句已经落库后才遇到高风险语句。
- 应用总览中仅草稿状态显示“发布”操作；“进入应用”和“发布”使用带 Tooltip、`aria-label` 的图标按钮，操作列统一收窄到 `128px`；打开工作台发布历史后消费一次性 `publish=1` 参数并复用现有发布确认，参数随即清理，避免刷新重复弹窗。

### 19.2 验证边界

- 只执行目标文件静态引用检查和 `git diff --check`，结果无空白错误。
- 遵照用户分工，未运行 Maven、前端 Lint/build、API、数据库 DDL 或浏览器验证。

## 20. 2026-07-14 应用数据库表初始化合并与访问入口简化

### 20.1 变更结果

- 新建应用选择“从数据库表开始”后，在同一初始化步骤直接选择运行数据源和数据表；系统先创建应用草稿，再以 `DB_IMPORT` 创建业务对象并自动建立 `PRIMARY` 关联，成功后直接进入应用“数据对象”分区，不再通过 `create=database` 二次打开对象向导。
- 初始化失败继续保留应用草稿；对象已创建但关联失败时同时保留对象 ID，重试只重新建立关联，不重复创建对象。
- 新建访问入口由三步收敛为“选择场景 → 配置入口”两步；工作台自动锁定当前应用和业务域，只提供当前应用内对象，优先选择主对象，单对象场景不再显示重复选择框。
- 入口名称和编码根据对象与场景生成；页面配置自动使用对象 `configKey`；目标页面/表单仅在存在多个可选项时显示，单一默认项自动选中。
- 普通菜单配置收敛为“添加到管理端菜单”开关，默认挂到业务域目录；父菜单、排序、入口编码和页面配置等技术参数继续保留在高级编辑。
- 修正应用对象快照同时包含“关联记录 ID”和“真实对象 ID”时取错 ID 的问题，确保设计数据、默认页面和默认表单按真实对象加载。
- 向导初始化期的场景、业务域和对象监听改为同步拦截，避免初始化结束后重复加载或覆盖已有入口回显；高级入口编辑表单补齐 `applicationId` 默认字段，避免连续打开抽屉时残留上一次应用归属。

### 20.2 静态检查

```bash
rg -n "create.?=.?database|create: 'database'|create: \"database\"" \
  forge-admin-ui/src/views/app-center/index.vue

rg -n "handleAppCodeChange|runtimeModeText|targetSummary|MenuParentSelect|confirm-step|currentStep < 3|n-step title=\"确认保存\"|label=\"业务页面配置\"" \
  forge-admin-ui/src/views/app-center/components/AppEntryWizard.vue
```

结果：两条命令均无输出；应用总览不再生成二次数据库导入参数，普通入口向导不再包含第三步确认页、入口编码输入、页面配置输入或完整菜单父级配置。

```bash
git diff --check -- \
  forge-admin-ui/src/views/app-center/components/AppEntryWizard.vue \
  forge-admin-ui/src/views/app-center/components/AppEditorDrawer.vue \
  forge-admin-ui/src/views/app-center/index.vue \
  code-copilot/changes/app-first-lowcode-workbench/spec.md \
  code-copilot/changes/app-first-lowcode-workbench/tasks.md

git diff --no-index --check /dev/null <本轮相关未跟踪 Vue 文件>
```

结果：目标已跟踪文件和本轮相关未跟踪 Vue 文件均无空白错误。

### 20.3 验证边界

- 遵照用户分工，本轮未运行 Maven、前端 ESLint/build、API、数据库、Vite 或浏览器交互验证。
- 用户需重点验收：数据库表初始化成功/失败重试、多对象选择、单一/多个表单自动选择、已有入口编辑回显以及菜单同步结果。

## 21. 2026-07-14 数据库导入字段基线与显式索引

### 21.1 问题根因

- 数据库导入已把 `decimal(18,2)` 映射为 decimal，但默认表单使用 `number` 控件；前后端表单归一化随后无条件套用控件默认物理类型，导致设计态被覆盖为 `int`。
- DDL 的必填策略只在字段配置默认值时生成 `NOT NULL`，没有区分“新增/收紧字段”和“导入时本来已经是 NOT NULL 的列”，因此会错误生成把已有必填列改为可空的 `MODIFY COLUMN`。
- DDL 服务会从 searchable 字段、对象关系以及 tenant/create_time 系统字段自动生成二级索引，用户无法在执行前明确控制索引集合。

### 21.2 变更结果

- 数据库导入完整保留字段基础类型、长度、decimal 精度和必填状态；decimal 字段标记为 MONEY，默认表单使用金额控件语义。
- 表单控件默认值只补全没有物理类型的新字段，不再覆盖已经存在的 `decimal/bigint/varchar` 等数据库类型；前端表单字段同步使用同样规则。
- 已有 NOT NULL 列且设计字段仍为必填时保持 NOT NULL，不再要求补默认值；只有新增列或把数据库可空列收紧为必填时，才继续要求默认值后执行数据库 NOT NULL。
- 取消 searchable、关系字段、tenant_id 和 create_time 的自动二级索引；旧 `auto=true` 索引不再参与映射状态或 DDL 生成，数据库已有索引不删除。
- 数据结构首屏新增“配置索引”：用户显式填写索引名称、普通/唯一类型、一个或多个字段及用途，应用配置时立即保存设计草稿，之后才进入数据库差异预览。
- 对修复前已经保存为错误物理类型/必填状态的已有表草稿，在检测到差异时显示“按数据库校准字段”；用户确认后只恢复类型、长度、精度和必填状态，不修改字段名称、控件、页面规则或数据库结构。
- 显式 ADD KEY / ADD UNIQUE KEY / CREATE INDEX 继续复用现有受控 DDL 白名单和二次确认；未配置索引时预览明确提示系统不会自动新增。
- 新增 `LowcodeModelImportServiceTest`、`LowcodeDdlExplicitIndexTest` 和设计器类型保护用例源码，覆盖 decimal/required 导入、searchable 不自动建索引、旧自动索引忽略和已有 NOT NULL 保持；按用户分工本轮未执行这些测试。

### 21.3 静态检查

```bash
rg -n "indexes.add\\(new IndexDefinition|field.getSearchable\\(\\).*INDEXABLE|idx_rel_|relationIndexFields|关联字段会自动创建" \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeDdlService.java \
  forge-admin-ui/src/components/lowcode-builder/model/LowcodeModelDesigner.vue

git diff --check -- <本轮相关已跟踪文件>
git diff --no-index --check /dev/null <本轮新增测试文件>
```

结果：旧自动索引生成路径扫描无输出；本轮相关文件无空白错误。

### 21.4 验证边界

- 遵照用户分工，本轮未运行 Maven、JUnit、前端 ESLint/build、API、数据库 DDL、Vite 或浏览器交互验证。
- 用户需重点验收：重新导入 `tf_f_order_purchase_order` 后 `order_no` 保持 varchar(64) 与原可空性、`total_amount` 保持 decimal(18,2)，未显式配置索引时 DDL 不出现 ADD/CREATE INDEX，显式保存索引后才出现对应预览语句。

## 22. 2026-07-14 Phase 7 模板化快速搭建、无入口预览与工作台收口

### 22.1 实施结果

- 应用创建第二步新增单表 CRUD、左树右表、主子表三套原生线框模板；模板成为默认入口，空白应用、数据库表和已有对象收进其他起点。
- 左树右表只显示树对象、节点显示字段、父级字段和主表关联字段；主子表支持动态增删子表名称、编码和主外键字段。
- 新增 `POST /ai/business/application/{id}/initialize-template`。应用草稿先独立创建，模板对象、字段、关系、页面 Schema 和应用对象编排在同一事务中完成；失败回滚模板资产并保留应用草稿。
- 单表生成 `simple-crud`；左树右表生成主对象、树对象、`REFERENCE` 关系和 `tree-crud`；主子表生成主对象、明细对象、`CHILD_LIST` 关系和 `master-detail-crud`。
- `V1.0.33` 将旧 `/ai/lowcode-models` 从主导航隐藏，但保留资源、路由、API 和存量模型。
- 缺少页面入口从完整发布检查和工作台检查的阻断项降为提醒；默认发布只选择启用且运行配置完整的入口。
- 新增应用草稿预览页，直接读取主对象/选中对象设计草稿并复用现有预览组件；`V1.0.34` 增加隐藏授权路由，避免权限守卫 403。
- 应用工作台删除重复命令栏，把返回、预览、刷新和发布集中到应用头部，并压缩头部与内容上方间距。

### 22.2 静态检查证据

```bash
rg -n '\$\{[^}]+\}' \
  forge-server/db/migration/V1.0.33__hide_legacy_lowcode_model_menu.sql \
  forge-server/db/migration/V1.0.34__add_business_application_preview_route.sql

rg -n "应用至少需要一个已启用的访问入口|ACTIVE_ENTRY_MISSING, (BLOCK|LEVEL_BLOCK)" \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java

git diff --check
git diff --no-index --check /dev/null <本轮新增或原未跟踪目标文件>
```

结果：新迁移无 Flyway placeholder；旧入口阻断文案和阻断级别扫描无输出；`V1.0.33`、`V1.0.34` 版本唯一且单调；已跟踪与目标未跟踪文件均无空白错误。

### 22.3 未执行项

- 按用户“自行验证”的分工，本轮未执行 Maven compile/test、前端 ESLint/build、Flyway 真实迁移、API、数据库、Vite 或浏览器。
- 三模板真实对象/关系/页面生成、失败事务回滚、零入口发布、多对象预览、隐藏菜单、预览路由授权和顶部空间均保持 `pending-user`，不表述为已通过。
- 本轮未启动任何服务，无新增 PID 需要清理。

## 23. 2026-07-15 模板资产选择与真实 CRUD 预览修正

### 23.1 实施结果

- 应用工作台“预览应用”不再打开独立 `LowcodePreviewPane` 页面，改为优先取 PRIMARY 对象 `configKey` 并直接新开真实 `/ai/crud-page/:configKey` 页面；没有可预览对象时留在工作台提示并切换到数据对象分区。
- 真实页面使用同一套 `AiCrudPage`、列表/表单/详情和动态 CRUD API。`designPreview=1` 只在对象设计权限通过时允许读取未发布草稿；不带预览标识的正式运行仍要求对象已发布。
- 模板引导新增统一来源选择器。主对象、左树对象和每个明细对象均支持从运行数据源选择数据库表，或选择当前业务域已有对象；新增受控数据表字段查询接口，来源确定后直接加载真实字段。
- 左树右表的树主键、显示字段、父级字段、主表筛选字段，以及主子表的主对象主键、子表外键均改为字段下拉；后端再次校验字段确实属于所选对象。
- 模板服务对数据库表复用现有 `DB_IMPORT` 协议，对已有对象复用原对象身份；模板只配置页面布局和关系，不再要求用户填写对象名称、编码或表名。
- 模板线框高度由 108px 压缩为 82px，卡片改为稳定网格行；标题禁止压缩换行，推荐徽标固定为单行不可收缩，修复主子表标题遮挡和徽标变形。
- 根据用户截图继续修正实际盒模型：预览块改为 `border-box + height: 100% + overflow: hidden`，避免固定网格行之外的 padding 覆盖“主子表”标题；数据库/已有对象来源切换不再使用会被压缩换行的 `NRadioButton`，改为全宽两列分段按钮。

### 23.2 静态检查

- 执行目标文件引用扫描和 `git diff --check`，未发现空白错误。
- 新增未跟踪目标文件另行执行 `git diff --no-index --check /dev/null <file>`；无空白错误输出。
- 按用户分工未执行 Maven、JUnit、前端 Lint/build、API、Vite、浏览器或数据库验证，真实模板导入、关系生成和草稿运行效果保持 `pending-user`。

## 24. 2026-07-15 业务字段字典类型误判修复

### 24.1 问题根因

- `BusinessFieldSchemaService.normalizeFieldType` 在接收到显式 `fieldType` 后，仍会继续按中文字段名称推断类型。
- 数据库导入字段即使已明确为 `TEXT`、`SWITCH` 或其他物理语义，只要名称以“状态”或“类型”结尾，保存设计器时就会被覆盖为 `DICT`。
- 被误判字段没有 `dictType` 或静态选项后，进入字典字段治理校验并抛出“字典字段必须配置字典类型”；对象关系查询返回 0 与该异常无关。

### 24.2 变更结果

- 字段类型改为显式配置优先；只有 `fieldType` 为空时才根据字段名称推断 `DICT/MONEY/PHONE/REGION`。
- `TEXTAREA/MULTI_LINE` 别名继续归一化为 `MULTILINE`；未知显式类型安全降级为 `TEXT`，不会再因字段名称二次推断为字典。
- 真正的 `DICT/SELECT/RADIO/CHECKBOX/MULTI_SELECT` 字段仍要求配置字典类型或有效静态选项，没有放宽发布和运行治理。
- 校验异常改为包含字段名称，例如：`字段「订单状态」使用字典组件时必须配置字典类型或静态选项`。
- 补充回归测试源码，覆盖显式 TEXT、显式 SWITCH、未知显式类型和真实字典字段缺少配置四类场景。

### 24.3 验证边界

- 遵照用户分工，本轮不运行 Maven、JUnit、API、数据库、Vite 或浏览器验证。
- 已执行目标源码引用扫描、已跟踪文件 `git diff --check` 以及未跟踪变更文档 `git diff --no-index --check`，无空白错误。
- 真实对象 `PROCUREMENT_WAREHOUSE/tf_f_order` 保存结果由用户验证。

## 25. 2026-07-15 Phase 8 草稿图预览与字段配置分层

### 25.1 实施结果

- `designPreview=true` 不再复用发布阶段缓存的运行 Schema。Controller 先按 `configKey` 定位对象并刷新关系草稿，`AiCrudConfigService` 再从最新 `modelSchema/pageSchema` 强制编译真实 CRUD 配置；普通运行仍读取不可变发布版本。
- 主对象刷新时重新装配当前关系和子对象草稿字段；应用协调发布的权威检查前只刷新 PRIMARY 对象聚合图，用户不需要逐个进入对象发布。
- 对象设计保存停止使用表单 Schema 归一化全量字段；表单编译只写页面 `fieldSettings`。既有字段从表单移除后继续保留，新拖入且不存在的字段才自动创建字段资产。
- 表单控件切换、页面校验和页面最大长度不再自动提升为字段资产；显式字段保存、字段重命名、数据库映射和公式协议保持。
- 字段与数据库映射页改为左侧字段列表、右侧固定属性栏；980px 以下使用同一属性面板的抽屉。中央属性弹窗已移除，未保存切换/关闭保护保留。
- 属性页移除全局“显示在表单 / 显示在列表 / 作为查询条件”，并明确默认控件/默认查询方式只供新页面继承；字段列表只显示是否已加入当前表单。普通字段属性页签只保留“业务定义 / 规则与安全 / 数据库与开发”，公式与调试仅从表单设计器的字段公式入口进入。
- 新增 `BusinessApplicationDraftPreviewContractTest` 源码，覆盖草稿强制编译、关系图准备、PRIMARY 发布准备和页面配置不反写字段资产。

### 25.2 静态检查

执行目标文案和调用链扫描：

```bash
rg -n "field-property-modal|显示在表单|显示在列表|作为查询条件" \
  BusinessFieldManager.vue BusinessFieldPropertyPanel.vue BusinessFieldList.vue

rg -n "buildDraftRenderConfig\(config\)|!forceDraftCompile && hasStoredRuntimeConfig\(config\)|prepareRuntimeDraft\(|preparePrimaryObjectDraft\(|resolvePublishedRuntimeConfig\(config\)" \
  forge-server/.../generator

rg -n "normalizeDesignerFieldPayloads\(dto.getFields|applyFormDesignerSchemaToModel\(modelSchema, formSchema\)|fieldAssetUpdated" \
  BusinessObjectDesignerService.java ForgePropertyPanel.vue

rg -n "shouldRetainExistingField|isDesignerManagedField|collectBoundFieldCodes" autoFieldRegistry.js
```

结果：中央弹窗、三个全局页面开关和普通属性页的“公式与调试”页签扫描无输出；表单设计器仍保留 `mode="formula"` 的字段公式专用入口。草稿编译、预览准备、PRIMARY 发布准备和正式发布快照分支均存在；表单 Schema 反写后端调用扫描无输出；`fieldAssetUpdated` 只剩显式字段保存入口；旧字段过滤辅助函数无残留。

执行差异空白检查：

```bash
git diff --check -- <Phase 8 已跟踪目标文件>
git diff --no-index --check /dev/null <Phase 8 未跟踪目标文件>
```

结果：已跟踪与未跟踪目标文件均无 whitespace error。首次未跟踪检查包装函数误用了 zsh 只读变量 `status`，命令在第一个文件检查后退出；改用 `result_code` 后完整重跑成功，无文件写入或环境副作用。

### 25.3 未执行项

- 按用户“自行验证”的分工，未运行 Maven、JUnit、前端 ESLint/build、API、数据库、Vite 或浏览器。
- 主子表初始化后立即预览、复用已发布对象后新增关系、子对象字段刷新、应用发布和字段页面配置隔离均保持 `pending-user`。
- 桌面固定属性栏、小屏抽屉滚动、未保存保护和明暗主题实际效果保持 `pending-user`。
- 本轮未启动任何服务，无新增 PID 需要清理。

## 26. 2026-07-15 Phase 9 字段属性、关系画布与概览密度优化

### 26.1 实施结果

- 字段业务定义改为五个紧凑语义分组；必填开关与默认值放在同一区域，开发字段不再挤占业务首屏。
- 必填默认值只对确定类型自动生成，并跟踪是否由系统自动产生；用户值不覆盖，字典和关联类字段不写无效业务值。
- 现有 ER 图升级为可编辑画布，支持对象卡拖动、字段连接点、拖线预览、创建/更新关系、连线选中和非法连接提示；目标对象字段按画布选择加载，不在进入页面时无边界查询所有对象。
- 关系属性、新增关系向导和字段联动表单已按身份、端点、展示、数据来源和状态策略重排；高级能力维持折叠。
- 应用工作台、头部、概览内容、分区间距、配置行、问题行和空状态统一压缩，继续使用 Forge 主题变量。
- 应用总览六列表格替换为 268～320px、约 166px 高的紧凑卡片网格；筛选、分页和全部操作事件保持，横向滚动与操作列概念移除。
- “草稿”状态标签增加跟随主题的低饱和暖色背景，应用卡片本身不改变底色，问题状态警示不受影响。

### 26.2 静态检查

```bash
rg -n "updateRequired|resolveAutomaticRequiredDefault|automaticRequiredDefault|property-section" BusinessFieldPropertyPanel.vue
rg -n "editable|startConnection|finishConnection|relationSelect|handleErConnect|erCandidateObjectCodes|buildRelationDraft|linkage-flow-editor" \
  LowcodeErDiagram.vue BusinessRelationDesigner.vue
rg -n "padding: 0 8px 8px|workspace-content.is-overview|min-height: 60px|gap: 12px|min-height: 38px" \
  application.[applicationCode].vue ApplicationWorkspaceHeader.vue ApplicationOverviewPanel.vue
rg -n "application-card-grid|268px|320px|min-height: 166px|application-actions|overflow-y: auto" \
  ApplicationTable.vue app-center/index.vue
git diff --check -- <Phase 9 已跟踪目标文件>
```

结果：必填默认值、ER 编辑协议、父层关系归一化、画布对象按需加载、概览密度和紧凑卡片目标均存在；`relationTypeOptions`、`updateRelationType`、`supportsPrecision`、旧 `handleDragMove`、1180px 最小表格宽度、ResizeObserver 和横向滚动文案扫描无输出；目标代码文件无 whitespace error。

### 26.3 未执行项

- 未运行 Maven、JUnit、前端 ESLint/build、API、数据库、Vite 或浏览器。
- 字段控件真实渲染、ER 鼠标拖线、对象卡拖动、非法连线提示、关系保存、小屏布局、应用概览实际密度和应用卡片 4/3/2/1 列均保持 `pending-user`。
- 本轮未启动任何服务，无新增 PID 需要清理。

## 27. 2026-07-15 Phase 10 应用级完整代码包

### 27.1 实施结果

- 应用管理新增完整代码设置、预览和下载接口，不依赖访问入口；默认生成应用全部数据对象，也支持 `objectIds` 批量选择。
- 应用组装继续调用现有低代码运行配置构建器和 Velocity 生成策略。草稿主对象生成前刷新关系图，主子表和左树依赖由主页面聚合生成，其他对象再合并到同一可部署目录。
- 代码生成存在模型/页面协议时强制重建查询、列表、表单、接口和 options；发布来源读取对象已发布快照，不再混入当前草稿派生字段。
- 多对象文件合并增加内容冲突检测，并生成应用 README 与 `config/application-manifest.json`；流程、扩展和外部集成继续保留在 Forge 治理边界，不生成任意 Java/SQL。
- 既有模板补齐 Mapper XML 分页/列表/树形/明细查询、主子明细清理、业务表结构 SQL，以及菜单/字典 SQL 的 `tenant_id = 1` 和 `NOT EXISTS`；生成实体按表结构选择 `TenantEntity`、主键策略和逻辑删除。
- `AppCodePanel` 增加应用 scope 和对象多选，应用总览与工作台复用同一预览界面；当前参数未成功预览时下载按钮不可用。
- 主子表代码生成不再只依赖派生的 `masterDetailConfig.children`：派生配置为空或部分缺失时，从 `pageSchema.modelRefs` 中的 `CHILD_LIST/DETAIL/ONE_TO_MANY` 关系恢复子表外键和主表关联字段；布局声明为主子表但无法解析任何有效子关系时明确阻断，不再静默生成普通单表 Service。
- 修复代码生成重编译后仍优先保留旧 `simple-crud` 布局的问题：`LowcodeCodegenService` 现在使用最新页面 Schema 编译出的 `layoutType` 覆盖旧值，确保已经解析到子表时主 Service 真正进入 `master-detail-crud` 查询和事务保存分支。
- Velocity 最终生成策略再次以 `pageSchema.layoutType` 作为第一优先级，避免绕过运行配置重编译的调用链重新使用旧布局值。
- 修复 Velocity 关联元数据为私有内部类导致 `${table.className}`、`${child.className}` 原样进入 Java 文件的问题：相关元数据改为公开静态类型，并对所有生成 Java 文件执行未解析模板引用扫描；模板异常或变量残留时整体生成失败，不再记录警告后跳过文件。
- 主子表明细对象除 Entity、Mapper 和 Mapper XML 外，新增独立 DTO、Query、Service 和 ServiceImpl；主对象 Service 继续负责同一事务内的明细查询、批量插入、替换更新和级联清理，不新增 Service 间注入。
- 应用代码面板默认收起代码包设置，展开后设置区内部滚动，对象选择区上限压缩为 110px，文件树和 CodeMirror 预览区保留 300px 最小高度。
- 新增 `V1.0.35__add_business_application_codegen_permissions.sql`，代码设置、预览和下载权限只继承已有应用编辑角色。
- 发布就绪度中的共享对象未发布变更由 `BLOCK` 调整为 `WARN`，继续显示对象名称和复用应用数量，但不再阻断当前应用发布。

### 27.2 静态检查

执行目标引用扫描，确认应用 API、批量对象选择、草稿关系刷新、manifest、文件冲突检测、预览签名、三类模板和 Mapper XML 查询均存在；旧模板中的 `LambdaQueryWrapper`、租户 `000000` 和按旧派生 Schema 空值判断扫描无输出。

执行模板 `#if/#foreach/#end` 数量平衡检查、`V1.0.35` Flyway placeholder 扫描、`BusinessApplicationObjectMapper.xml` 的 `xmllint --noout`，均无错误输出。

对 Phase 10 目标已跟踪文件执行 `git diff --check`，对未跟踪文件执行 `git diff --no-index --check /dev/null <file>`，无 whitespace error。

增量执行主子表兜底、失败关闭、关联元数据实际渲染、子表 Service 文件、主 Service 明细方法和代码面板空间目标引用扫描，并对本轮目标文件执行差异空白检查；未运行构建或服务。

### 27.3 未执行项

- 按用户自行验证的分工，未运行 Maven、JUnit、前端 ESLint/build、API、Flyway、数据库、Vite 或浏览器。
- 单表、左树右表、主子表真实预览内容，批量 ZIP 解压、生成代码编译及页面运行均保持 `pending-user`。
- 本轮未启动任何服务，无新增 PID 需要清理。

## 28. 2026-07-15 Phase 11 低代码协议与下载代码自动适配

### 28.1 变更范围

- 生成包新增结构化协议快照、覆盖报告、前端 runtime config 和后端 classpath config；未知 model/page/options 嵌套字段整体透传。
- 在线页和下载页复用 `LowcodeRuntimePage -> crud-page.vue` 唯一解释链路，生成 Vue 模板不再复制字段、字典、树和主子表转换函数。
- 生成 Controller 改为委托 `DynamicCrudService`、`DynamicCrudExcelService`，classpath 注册器以 `generated_*` 键提供不可变配置。
- 公共 Velocity 入口统一规范化普通/旧配置键和业务 API；补齐左树右表 `/tree`、异步导出任务业务端点和前端页面跳转改写。
- 非法 JSON、缺失 model/page、未改写的平台通用运行接口、classpath 同键不同内容均失败关闭。

### 28.2 静态验证证据

执行目标文件状态与差异空白检查：

```bash
git status --short -- <Phase 11 目标文件>
git diff --check -- <Phase 11 已跟踪目标文件>
git diff --no-index --check /dev/null <Phase 11 未跟踪目标文件>
```

结果：已跟踪目标文件 `git diff --check` 无输出；逐个检查新增 Java/Vue/测试/组装器文件，无 whitespace error。

执行薄模板、共享运行时、运行内核和公共入口引用扫描：

```bash
rg -n "transformColumns|transformFields|preloadDicts|/ai/crud/" \
  templates/vm/ai-crud/index.vue.vm templates/vm/controller.java.vm
rg -n "runtimeConfig:|embeddedRuntime|props.runtimeConfig|currentTemplate|masterDetailConfig|treeConfig" \
  forge-admin-ui/src/views/ai/crud-page.vue
rg -n "generatedRuntimeConfigBuilder.build|META-INF/forge-lowcode|runtimeConfigKey|-protocol.json|-coverage.json" \
  VelocityCodegenStrategy.java
rg -n "DynamicCrudService|DynamicCrudExcelService|selectPage|selectTree|insert|updateById|deleteById" \
  templates/vm/controller.java.vm
```

结果：生成 Vue/Controller 模板的复制转换函数和 `/ai/crud/` 扫描无输出；内嵌运行配置、模板 Catalog、树/主子配置、公共运行键派生、四类资产路径和动态内核委托引用均存在。

执行 Velocity 指令与协议契约扫描：

```bash
rg -o '#(if|foreach)\b' <Phase 11 模板> | wc -l
rg -o '#end\b' <Phase 11 模板> | wc -l
rg -n "futureRelation|futureOption|futurePageCapability|unsupported|generated_\*|配置键冲突" \
  GeneratedLowcodeRuntimeConfigBuilderTest.java LowcodeProtocolSnapshotBuilderTest.java \
  GeneratedLowcodeConfigRegistryTest.java
```

结果：本轮三个薄模板不包含条件指令，开闭数量均为 0；新增测试源码包含未来嵌套字段、普通键派生、URL 改写、unsupported 空集、非法协议和资源键冲突契约。

### 28.3 未执行项

- 按当前变更的用户验收分工，未运行 Maven compile/test、JUnit、前端 ESLint/build、API、数据库、Vite 或浏览器。
- 单表、左树右表、主子表真实 ZIP 解压、生成后端编译、菜单路由、树筛选、主子事务、导入导出和未来字段升级重下载均保持 `pending-user`，不表述为已通过。
- 本轮未启动任何服务，无新增 PID 需要清理。

## 29. 2026-07-15 Phase 12 下载后端静态协议编译与二次开发扩展

### 29.1 变更范围

- Phase 11 的前端共享 `LowcodeRuntimePage + runtime-config.json` 保留；下载后端从动态 CRUD 委托改为标准 MyBatis-Plus Controller/Service/Mapper/XML。
- 主对象新增 `ServiceExtension` around 链，支持增强或完整替换分页、列表、树、详情、增改删、批量删除和导入；用户正式 custom 实现不进入生成集合。
- 新增静态编译贡献器 SPI、ownership 资产和 `.java.example`；后续后端协议能力统一进入公共 Velocity 编译入口。
- 导入导出改用 Forge Excel starter 和生成 Service；删除 classpath 低代码配置注册、后端 META-INF 资源及下载 Controller 的 DynamicCrud 依赖。
- Query/Mapper XML 静态编译补齐低代码允许的全部基础查询方式；coverage 对公式、自动编号、唯一校验和组合查询等未内建语义明确报告 `REQUIRES_EXTENSION`。

### 29.2 静态验证证据

执行下载后端依赖与静态分层扫描：

```bash
rg -n "DynamicCrudService|DynamicCrudExcelService|GeneratedLowcodeConfigRegistry|META-INF/forge-lowcode|generatedConfigRegistry" \
  templates/vm/controller.java.vm VelocityCodegenStrategy.java \
  GeneratedLowcodeRuntimeConfigBuilder.java LowcodeProtocolSnapshotBuilder.java

rg -n "extends ServiceImpl|selectGeneratedPage|selectTreeRows|aroundPage|aroundInsert|CREATE_ONCE_SAMPLE|LowcodeStaticCodegenContributor|STATIC_MYBATIS_PLUS" \
  templates/vm VelocityCodegenStrategy.java LowcodeProtocolSnapshotBuilder.java

rg -n "LambdaQueryWrapper" templates/vm/service.java.vm templates/vm/serviceImpl.java.vm \
  templates/vm/mapper.java.vm templates/vm/mapper.xml.vm
```

结果：第一组和 LambdaQueryWrapper 扫描无输出；生成 Service、Mapper XML、扩展链、所有权、静态编译模式和贡献器引用均存在。`DynamicCrudService.getConfig` 继续直接调用数据库配置服务并保留授权设计预览分支，在线运行未移除。

执行 Apache Velocity 2.3 实际渲染：

```text
simple: controller/service/serviceImpl/serviceExtension/businessExtension/excel/README rendered
master: controller/service/serviceImpl/serviceExtension/businessExtension/excel/README rendered
tree: controller/service/serviceImpl/serviceExtension/businessExtension/excel/README rendered
query.java.vm=883
mapper.xml.vm=2497
```

结果：单表、主子表、左树右表及 `eq/ne/like/left_like/right_like/gt/ge/gte/lt/le/lte/in/between` 查询模板均完成渲染；无残留 `#if/#end/${...}/$object.property`。扩展链泛型另用 JDK 17 最小等价代码执行 `javac`，编译成功。

执行 XML、SQL 和差异空白检查：

```bash
xmllint --noout templates/vm/mapper.xml.vm
rg -n "WHERE NOT EXISTS|data_source_bean|excelImportService.importData" \
  templates/vm/sql/excel.sql.vm templates/vm/controller.java.vm
git diff --check -- <Phase 12 已跟踪目标文件>
git diff --no-index --check /dev/null <Phase 12 未跟踪目标文件>
```

结果：Mapper XML 静态解析成功；Excel 元数据使用防重复插入并指向生成 Service；tracked 和逐个 untracked 目标文件均无 whitespace error。

校验过程先尝试 JShell，因沙箱禁止 JDI 监听端口且 local 模式未继承外部 classpath，未执行到模板；随后使用 JDK 17 source-file mode 完成同等 Velocity 校验。该工具限制不影响代码结果，也未启动服务或修改数据库。

### 29.3 未执行项

- 按当前变更的用户验收边界，未运行 Maven compile/test、JUnit、前端 ESLint/build、API、数据库、Vite 或浏览器。
- 单表、左树右表、主子表真实 ZIP 解压、生成模块编译、CRUD、导入导出以及用户扩展 Bean 的真实顺序/事务行为保持 `pending-user`，不表述为已通过。
- 本轮未启动任何服务，无新增 PID 需要清理；临时校验源码已删除。

## 30. 2026-07-15 Phase 12 编译错误增量修正

- 用户编译发现 `VelocityCodegenStrategy.java:164` 报“从 lambda 表达式引用的本地变量必须是最终变量或实际上的最终变量”。
- 根因为 `generate(AiCrudConfig config, ...)` 先把规范化结果重新赋给方法参数 `config`，随后 `staticCodegenContributors.orderedStream().filter(...)` 捕获该变量。
- 修正为原始输入 `config` 只用于解析来源键和业务 API，规范化结果只赋值一次给 `codegenConfig`；字段解析、模板上下文、静态贡献器、SQL、前端资产和协议快照全部显式使用 `codegenConfig`。
- 契约测试源码增加“不得重新赋值 config、lambda 必须捕获 codegenConfig”断言；执行目标引用与差异空白检查。按既定边界未运行 Maven/JUnit，未启动服务或数据库。

## 31. 2026-07-15 Phase 13 下载包命名与输出策略

### 31.1 变更范围

- 无脱敏策略、空策略和 `NONE` 不再生成字段 `@Desensitize` 或实体 import；真实策略统一大写后生成。
- 下载协议新增实体前缀、有序表前缀剥离、后端 Java/Mapper XML/前端页面/前端 API 根目录，以及后端、前端和 Excel SQL 范围开关。
- 主表、左树对象和主子明细统一派生最终类名；非法 Java 前缀、绝对/越界路径和同路径内容冲突失败关闭。
- 应用级、访问入口级、低代码应用级和旧 configKey 公共入口共用 `options.codegen`；前端保存、预览、下载和预览签名携带同一字段，空表前缀列表不会在 GET 序列化时丢失。

### 31.2 静态验证证据

执行 tracked 与 untracked 差异空白检查：

```bash
git diff --check -- <Phase 13 已跟踪目标文件>
git diff --no-index --check /dev/null <Phase 13 未跟踪目标文件>
```

结果：已跟踪目标命令退出码 0；9 个未跟踪目标均只有 `--no-index` 的正常差异退出码 1，检查输出为空，无 whitespace error。

执行前端脚本语法检查：

```bash
node --check forge-admin-ui/src/api/business-app.js
node --check forge-admin-ui/src/api/business-application.js
awk '/<script setup>/{active=1; next} /<\/script>/{active=0} active' \
  forge-admin-ui/src/views/app-center/components/AppCodePanel.vue | node --check --input-type=module
```

结果：三个检查退出码均为 0；新增设置表单、payload、空数组归一化和下载参数序列化无 JS 语法错误。

执行 Velocity 与协议引用检查：

```bash
rg -o '#(if|foreach)\b' templates/vm/README.md.vm | wc -l
rg -o '#end\b' templates/vm/README.md.vm | wc -l
rg -o '#(if|foreach)\b' templates/vm/entity.java.vm | wc -l
rg -o '#end\b' templates/vm/entity.java.vm | wc -l
rg -n '@Desensitize\(type = DesensitizeType\.NONE\)' <生成器模板和 Java 源码>
rg -n 'entityPrefix|stripTablePrefixes|backendBasePath|mapperXmlBasePath|frontendApiBasePath|includeBackend|includeFrontend|includeExcelSql' \
  <请求 DTO、三类 Service、Assembler、Velocity 策略和 AppCodePanel>
```

结果：README 指令 6/6、Entity 指令 17/17；没有 `DesensitizeType.NONE` 注解输出；新增设置在请求 DTO、应用、访问入口、低代码服务、公共装配器、生成策略和前端面板中均存在。JS/Vue 语法、模板条件和引用扫描均通过。

### 31.3 未执行项

- 按当前变更的用户验收边界，未运行 Maven compile/test、JUnit、前端 ESLint/build、API、数据库、Vite 或浏览器。
- 自定义实体前缀/表前缀的真实 ZIP、生成模块编译、四类输出目录落位、范围开关文件集合和设置保存回显保持 `pending-user`，不表述为已通过。
- 本轮未启动任何服务，无新增 PID 需要清理。

## 32. 2026-07-15 应用概览卡片网格余量修正

- 根因为 `ApplicationTable.vue` 使用 `repeat(auto-fill, minmax(268px, 320px))`，自动重复轨道按固定 320px 上限计算列数；中小屏内容区实际可以容纳三张 268px 卡片时仍只生成两列，右侧形成大块空白。
- 改为 `repeat(auto-fill, minmax(min(100%, 268px), 1fr))`：列数按 268px 基准和容器真实宽度计算，当前行余量由已有轨道等分；`auto-fill` 继续保留空轨道，宽屏少量应用不会拉成整行大卡片。
- 本轮只修改卡片网格 CSS，不改变卡片内容、事件、筛选、分页和滚动协议。执行目标差异空白和关键样式引用检查；按既定用户验收边界未运行前端 build、Vite 或浏览器，实际断点效果保持 `pending-user`。

# 低代码平台架构治理 Execution Log

## 2026-08-14 Phase 2 实施开始

- 依据：`spec.md`、`tasks.md`、`code-copilot/rules/automated-testing-standard.md`。
- 基线：Phase 1 的 H5 多区域运行时、预售种子迁移和创建后动作链路已完成并通过既有测试。
- 首个风险：`normalizeFormDesignerSchema` 当前只返回固定基础字段，会在设计器打开或保存时丢弃 `pageSections` 和 `bottomBar`。
- 本轮范围：协议保真、页面分区与底部操作栏可视化编辑、事件一级入口、管理端构建和浏览器回归。
- 环境约束：不连接真实 MySQL，不修改历史 Flyway；保留工作区现有 `.DS_Store`、Phase 1 代码和用户文档改动。

## 2026-08-14 Phase 2 实施结果

- 协议保真：`normalizeFormDesignerSchema` 和多表单保存链路无损保留 `pageSections` / `bottomBar`，且不修改输入对象。
- 页面分区：新增大尺寸可滚动工作台，支持 card / child_table、新增删除、分区与字段拖拽排序、字段归组、`pillSelect` 覆盖、折叠配置、模式可见性、子表关系和展示方式。
- 底部操作栏：支持 save / reset / action / cancel、动作绑定、拖拽排序、受控显示条件、确认提示和成功提示。
- 配置修复：失效字段、关系、动作和无法由 H5 受控语法解析的显示条件均保留原值并显示告警；最后一个页面模式不可取消，避免空数组在运行时反向解释为“全部可见”。
- 可发现性：实际使用的表单切换工具栏增加「页面分区」入口；右侧属性栏默认展开；字段事件提升为一级「事件」Tab，原生命周期配置保留为高级入口。
- 动作数据链路：对象设计器将现有业务动作经 `BusinessFormDesigner` 传入分区编辑器，动作按钮只能从已有动作中选择。
- 调试清理：移除临时对象设计器白名单，浏览器验收通过正常权限守卫和隐藏菜单授权进入。

## 2026-08-14 Phase 2 验证结果

- `pnpm vitest run ...formDesignerSchema.spec.js ...pageSectionEditorUtils.spec.js ...PageSectionEditor.spec.js`：通过，3 个测试文件、16/16。
- 定向 ESLint：通过，覆盖本轮 10 个管理端实现与测试文件，无错误或警告。
- `pnpm build`：通过，`vite build` 用时 1m52s；仅保留仓库既有的组件命名冲突、动态/静态导入和 CSS `//` 注释告警。
- Playwright mock E2E：通过正常路由权限守卫进入预售设计器；六个分区和两个底部按钮完整回显；`payMethod` 回显为横向选项；无失效引用告警。
- 拖拽闭环：分区顺序从“导购信息、会员信息”调整为“会员信息、导购信息”，应用配置并重新打开后顺序保持；组件测试同时验证底部按钮排序进入保存协议。
- 响应式与入口：1024x768 弹窗无横向溢出，一级「事件」Tab 可打开；截图位于 `/tmp/forge-phase2-section-editor.png`、`/tmp/forge-phase2-section-editor-1024.png`。
- H5 兼容回归：`node --test src/utils/__tests__/lowcode-runtime.test.js` 通过 11/11；`pnpm build:h5` 输出 `DONE Build complete`，仅提示 uni-app 可升级。
- `git diff --check -- forge-admin-ui code-copilot/changes/lowcode-platform-architecture`：通过。

## 跳过与清理

- 未连接或修改真实 MySQL/Flyway，未执行真实发布，不调用企微、摄像头或外围系统；复用 Phase 1 的 mock 创建并执行 `submit_presale` 证据。
- Playwright Chromium 均已关闭；管理端开发服务 `http://127.0.0.1:5173/` 和既有 H5 服务 `http://localhost:3002/` 保留供人工验收。

## 2026-08-15 Phase 3-6 实施结果

- Phase 3：增加受管 `CALL_API` 业务动作步骤。`CallApiActionStepExecutor` 只接受已注册的 `EXTERNAL_API` 低代码查询源，复用参数/结果映射协议，支持 `THROW` / `LOG_AND_CONTINUE`；发布前校验查询源、路径、结果目标和回滚语义。触发器 WEBHOOK 已复用该执行器，不再接受任意 URL 或保留 TODO 状态。
- Phase 4：H5 运行时优先消费 `options.pageSchema.zones`，按 zone 顺序渲染 form / actions / list；每个 form zone 保留独立 schema 和 form ref，保存或执行动作前统一校验。无 pageSchema 时继续回退 `options.formDesignerSchema` 和旧 editSchema。
- Phase 5：应用设计器收敛为「页面 / 事件 / 动作 / 数据模型 / 设置」五个一级入口；对象设计器支持嵌入并按入口直达字段事件、业务动作、数据结构、关系、流程和触发器。设置区集中权限、页面入口、字典和发布。存量 `lowcode-builder/:id` 标记 deprecated 并保留草稿能力，不执行无法可靠映射应用编码的自动重定向。
- Phase 6：设计器可以加载 `designer_options` 和运行时 CRUD `options` 中的种子配置，首次接管展示差异摘要并要求确认，确认后写回当前草稿配置。草稿保存不修改不可变版本表；正式发布继续由 `BusinessObjectPublishService` 同时创建 CRUD 配置版本和对象设计版本快照。
- 预售约束：未增加预售专用 Java/Vue 运行时分支；五分区、底栏、字段事件、业务动作和 `submit_presale` 继续由通用低代码协议驱动。

## 2026-08-15 Phase 3-6 验证结果

- 管理端组合 Vitest：通过，7 个测试文件、33/33；覆盖分区协议、CALL_API 编辑协议、应用设计导航和种子接管。
- 管理端定向 ESLint：通过；管理端 `pnpm build` 通过，仅保留仓库既有构建告警。
- H5 `node --test src/utils/__tests__/lowcode-runtime.test.js`：通过 14/14；`pnpm build:h5` 通过。
- generator 目标单测：通过 51/51，覆盖 `DynamicCrudControllerTest`、`BusinessActionCommandPolicyTest`、`BusinessObjectPublishServiceCommandTest`、`CallApiActionStepExecutorTest`、两个 WEBHOOK 测试、pageSchema 和预售迁移契约。
- 聚合后端测试未通过：既有 `forge-plugin-message/MessageServiceImplTest` 构造 `MessageServiceImpl` 时缺少 `ApplicationEventPublisher` 参数，属于本变更之外的测试装配问题；目标 generator 测试均通过。
- 管理端 Playwright mock：通过正常隐藏菜单权限守卫进入统一设计器；五个一级入口均可切换，事件显示 `member/member-by-mobile`，动作、数据模型和设置内容完整；页面错误和控制台错误均为 0。1024px 视口下 `document.width = viewport.width = 1024`，无横向溢出。当次截图输出为 `/tmp/forge-unified-designer-page.png`、`/tmp/forge-unified-designer-settings.png`、`/tmp/forge-unified-designer-1024.png`。
- H5 Playwright mock：预售创建态成功渲染五个分区、pill 收款选择、商品明细和固定重置/提交底栏；当次截图输出为 `/tmp/forge-presale-h5-create.png`。后续重复交互受 mock crypto 初始化竞态和 Playwright 定位器影响，未将测试夹具问题判为应用回归；Phase 1 的 mock 证据已覆盖创建和 `submit_presale`。

## 2026-08-15 跳过项

- 未连接或修改真实 MySQL，未实跑 Flyway，未启动真实 Admin/Flow 服务，未执行真实发布或检查 `forge_schema_history`；因此真实版本落库和预售服务端端到端仍需业务环境验收。
- 未调用真实企微、摄像头、库存和外围 API；CALL_API/WEBHOOK 结论基于受管查询源单测和 mock 协议验证。

## 2026-08-15 最终复跑与清理

- 管理端目标 Vitest 最终复跑：分区、CALL_API、导航、种子接管 6 文件 28/28；补跑 `application-runtime-load.spec.js` 5/5，合计 7 文件 33/33。
- H5 运行时单测最终复跑：14/14；generator 8 个目标测试类最终复跑：51/51，`BUILD SUCCESS`。
- `git diff --check`：通过；`V1.0.109__add_presale_h5_page_sections.sql` Flyway `${...}` 占位符静态扫描无输出。
- H5 mock 静态服务器日志中的 `/static/icons/ai-icon/*.svg` 404 来自测试夹具未挂载 `static/` 目录；对应文件均存在于 `forge-h5-ui/dist/build/h5/static/icons/ai-icon/`，不属于生产构建缺失。
- 本轮启动的管理端 Vite 服务 `127.0.0.1:5174`（session 90844）和 H5 mock 静态服务 `127.0.0.1:5175`（session 36985）均已发送 `Ctrl-C` 并正常退出；未停止其它既有服务。

## 2026-08-15 配置维度 Spec 收尾

- 对象设计器：独立入口收敛为「基本信息、字段设计、数据模型、默认视图、触发器」五项；数据模型内含对象关系/流程绑定/数据权限，默认视图内含列表/详情模板。嵌入式应用设计器继续使用旧面板白名单，旧 `form/list/detail/actions/relations/flow-app/permission` 查询参数按兼容规则映射，未删除历史协议。
- 应用运行时桥接：H5 入口已有 `appId`，渲染配置请求带入该入口上下文；`AiCrudConfigController` 通过 `BusinessApplicationRuntimeConfigOverlayService` 校验入口与 `configKey` 后，从已发布应用快照读取 `inAppBuilder.flowInteraction`，仅对当前响应叠加，不修改对象全局 CRUD 配置。无应用上下文、旧入口或未发布应用保持对象级运行时兼容。
- 管理端定向 Vitest：8 个文件、37/37 通过，含对象导航映射和应用运行页面加载协议。
- H5 `node --test src/utils/__tests__/lowcode-runtime.test.js`：17/17 通过。
- Generator 目标 Maven 测试（`-Penable-tests`）：10 个测试类、58/58 通过，包含新增应用运行配置桥接测试；`BUILD SUCCESS`。
- 管理端 `pnpm build`：通过，保留仓库既有组件命名冲突、动态/静态导入和 CSS 注释告警；H5 `pnpm build:h5`：通过并输出 `DONE Build complete`。
- `git diff --check`：通过；新增 `V1.0.109__add_presale_h5_page_sections.sql` 的 Flyway 占位符扫描无输出。扫描整个迁移目录仍会命中历史 `V1.0.72` 模板 `${...}`，未修改历史脚本。
- 未连接真实数据库、未执行真实 Flyway/应用发布，也未启动或停止 Admin/Flow 服务；本轮未启动长期运行进程。

## 2026-08-15 配置维度边界补强与浏览器复验

- 首次对象设计器 Playwright mock 尝试未通过：现有管理端 Vite `127.0.0.1:5173` 可访问，但 Admin `8580` 未运行，认证/菜单 mock 未完整通过路由守卫，等待 `.designer-nav .nav-item` 30 秒超时。浏览器已关闭，该次结果未记为通过。
- 补齐 `/dev-api/crypto/config`、用户、租户、菜单和对象设计接口 mock 后复验通过：独立对象设计器只显示「基本信息、字段设计、数据模型、默认视图、触发器」五个入口；数据模型显示对象关系/流程绑定/数据权限，默认视图显示列表模板/详情模板。
- 配置归属补强：独立对象默认视图隐藏自定义工具栏/行操作编辑，并在保存时不调用对象动作保存接口、不覆盖 `designerOptions.actions`；嵌入式应用设计器仍保留动作编辑。旧 `panel=actions` 深链实际进入默认视图，未渲染旧业务动作面板。
- 管理端定向 ESLint 通过；配置维度 Vitest 8 个文件、38/38 通过；最终管理端构建日志输出 `✓ built in 2m 3s`，仅保留既有组件重名、动态/静态导入和 CSS `//` 注释告警。构建结束后的临时 zsh 日志包装命令误用了只读变量 `status` 并返回 1，不影响已完成的 Vite 构建结果。
- 1024x768 Playwright mock：`documentWidth = bodyWidth = viewport = 1024`，无横向溢出，控制台错误和页面错误均为 0；截图为 `/tmp/forge-object-designer-five-sections-1024.png`。
- 本轮未启动新服务；复用并保留工作区原有 Vite PID `75499`，未停止用户或其它任务进程。未连接真实 MySQL，未启动 Admin/Flow，真实发布与流程端到端仍按既有跳过项处理。

# 低代码平台架构治理 Test Spec

## 1. Phase 2-6 验证范围

验证表单设计器分区、受管 CALL_API、H5 `pageSchema.zones` 运行时、统一应用设计入口和种子配置接管形成完整闭环，并以预售登记协议作为核心回归样本。

本轮不连接或修改真实 MySQL，不执行真实 Flyway 和应用发布，不启动 Admin/Flow 服务；后端通过目标模块单测覆盖协议、校验、执行器和发布职责，前端通过单测、构建和 Playwright mock 验收覆盖设计态与 H5 运行态。

## 2. P0 必跑

所有管理端前端命令先执行 `source ~/.nvm/nvm.sh && nvm use v20.19.0`；Java 使用本机 OpenJDK 17。

| 检查 | 命令 | 预期 |
|---|---|---|
| 管理端协议单测 | `cd forge-admin-ui && pnpm vitest run src/views/app-center/components/designer/form-first/__tests__/formDesignerSchema.spec.js src/views/app-center/components/designer/forge-form-designer/__tests__/pageSectionEditorUtils.spec.js src/views/app-center/components/designer/forge-form-designer/__tests__/PageSectionEditor.spec.js src/views/app-center/components/designer/__tests__/business-action-designer-protocol.spec.js src/views/app-center/components/designer/__tests__/call-api-step-config.spec.js src/views/app-center/__tests__/application-designer-navigation.spec.js src/views/app-center/__tests__/application-runtime-load.spec.js` | 分区、底栏、CALL_API、五入口导航、种子接管和路由重载协议通过 |
| H5 运行时单测 | `cd forge-h5-ui && node --test src/utils/__tests__/lowcode-runtime.test.js` | `pageSchema.zones` 优先、旧协议回退、多表单校验和预售动作协议通过 |
| 后端目标单测 | `cd forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator && JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -Penable-tests -Dtest=DynamicCrudControllerTest,BusinessActionCommandPolicyTest,BusinessObjectPublishServiceCommandTest,CallApiActionStepExecutorTest,BusinessTriggerExecutorWebhookTest,BusinessTriggerServiceWebhookTest,BusinessObjectDesignerPageSchemaTest,PresaleRegistrationLowcodeMigrationContractTest test` | CALL_API/WEBHOOK、页面协议、发布职责和预售迁移契约通过 |
| 管理端生产构建 | `cd forge-admin-ui && pnpm build` | 构建成功，无新增阻断告警 |
| H5 生产构建 | `cd forge-h5-ui && pnpm build:h5` | 构建成功 |
| 差异格式 | `git diff --check` | 无新增空白错误 |

## 3. Phase 2 设计器验收

- 从表单设计器一级入口打开「页面分区」工作台，能新增、删除和拖拽排序分区。
- 卡片分区能选择已有字段并排序，设置组件覆盖、页面模式和折叠行为。
- 子表分区能选择已有关系并设置 `inline_grid`、`card_list`、`bottom_sheet`。
- 底部栏能配置 `save`、`reset`、`action`、`cancel`，动作按钮只能绑定已有业务动作。
- 显示条件通过字段、操作符和值配置，保存为 H5 已支持的受控表达式。
- 加载预售协议后能回显五个分区和底部按钮，编辑保存并重新打开后协议保持一致。
- 右侧属性栏默认可见，「事件」作为一级 Tab 可以直接进入字段事件配置。

## 4. Phase 3 CALL_API 验收

- 动作设计器可选择 CALL_API，查询源只允许启用的 `EXTERNAL_API` 低代码查询源。
- 参数支持表单字段、记录字段、上下文、系统上下文和静态值的受控映射。
- 结果只能映射到步骤上下文或表单数据，非法路径、重复目标和保留字段会被拒绝。
- `THROW` 与 `LOG_AND_CONTINUE` 和 `rollbackOnFailure` 语义一致。
- 触发器 WEBHOOK 复用 CALL_API 执行器和校验，不允许配置任意 URL。

## 5. Phase 4 H5 运行时验收

- 有效 `options.pageSchema.zones` 优先于顶层 `options.formDesignerSchema`。
- form zone 渲染独立表单和 `pageSections`；actions/list zone 按配置顺序渲染。
- 多个 form zone 在保存、业务动作前全部校验，避免只校验主表单。
- 不带 `pageSchema` 的历史应用继续读取 `options.formDesignerSchema`，再回退到旧 `editSchema`。
- 预售新建页显示五个分区、pill 收款方式、商品明细和固定底部重置/提交栏。

## 6. Phase 5-6 统一设计器验收

- 应用设计器顶部直接显示「页面 / 事件 / 动作与增强 / 业务流程 / 数据模型 / 设置」六个一级入口。
- 事件可直接看到字段查询回填；动作进入业务动作设计；数据模型提供结构、关系、流程和触发器；设置提供权限、入口、字典和发布。
- 切换入口或对象时，如存在未保存修改，必须先保存确认。
- 旧 `lowcode-builder/:id` 保留存量草稿编辑并显示 deprecated 提示，不把 CRUD 配置 ID 错当应用编码重定向。
- 种子配置第一次保存前显示接管差异摘要；确认后写回运行时 CRUD 草稿配置。
- 草稿保存不修改不可变版本表；正式发布同时创建 CRUD 配置版本和对象设计版本快照。
- 桌面和 1024px 视口无横向溢出，页面无控制台错误。

## 7. 跳过项

- 真实 MySQL、Flyway、Admin/Flow 服务和发布版本落库：本轮不连接用户业务环境，因此只做迁移契约和发布服务单测。
- 真实预售 API、企微、摄像头、库存和外围系统：依赖业务环境，使用 mock 配置验证协议和前端主路径。
- 聚合后端全量测试：受既有 `forge-plugin-message/MessageServiceImplTest` 构造参数缺少 `ApplicationEventPublisher` 阻塞；不属于本变更，不把该失败记为通过。

## 8. 配置维度整合增量验证

| 检查 | 覆盖范围 | 预期 |
|---|---|---|
| application designer navigation Vitest | 动作与增强、业务流程、数据模型导航映射 | 新旧入口均可归一化，应用设计器直接显示目标一级入口 |
| form linkage / section Vitest | 字段联动协议、子表交互、权限字段保真 | 历史 linkageSchema 可迁移，保存不丢 selector/mapping/filter/permission 配置 |
| H5 lowcode-runtime node:test | permissionKey/permissionCode、hide/disable、flow_action、节点分区权限 | 无权限操作不可执行；历史无权限配置行为不变 |
| H5 PageSectionRenderer 组件测试 | 禁用按钮、流程时间轴、分区只读 | UI 状态与纯函数结果一致 |
| 管理端构建 + H5 build:h5 | 新增设计面板和运行时组件 | 生产构建通过，无新增阻断错误 |
| generator 目标测试 | formDesignerSchema 扩展字段和发布协议透传 | 新协议可保存、发布，旧协议仍可加载 |

浏览器验收至少覆盖：六个应用级入口、对象设计器五入口、child_table 交互配置、按钮权限编辑、无权限隐藏/禁用，以及 1024px 视口无横向溢出。

## 9. 本轮增量验证（2026-08-15）

本轮覆盖配置维度 Spec 的最后两个缺口：独立对象设计器五入口收敛，以及应用级 `flowInteraction` 进入带应用入口上下文的 H5 CRUD 运行配置。未连接真实 MySQL、未启动 Admin/Flow 服务，应用发布快照桥接使用 Mockito 单测覆盖。

| 检查 | 命令 | 结果 |
|---|---|---|
| 管理端协议单测 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm vitest run ...（8 个目标文件，含 object-designer-navigation.spec.js）` | 8 个文件、38/38 通过 |
| H5 运行时单测 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && node --test src/utils/__tests__/lowcode-runtime.test.js` | 17/17 通过 |
| Generator 目标单测 | `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=DynamicCrudControllerTest,BusinessActionCommandPolicyTest,BusinessObjectPublishServiceCommandTest,CallApiActionStepExecutorTest,BusinessTriggerExecutorWebhookTest,BusinessTriggerServiceWebhookTest,BusinessObjectDesignerPageSchemaTest,PresaleRegistrationLowcodeMigrationContractTest,BusinessApplicationRuntimeConfigOverlayServiceTest,BusinessApplicationRuntimeServiceTest test` | 58/58 通过，`BUILD SUCCESS` |
| 管理端生产构建 | `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | 通过；仅仓库既有 Vite 导入/组件命名/CSS 注释告警 |
| H5 生产构建 | `pnpm build:h5` | 通过，输出 `DONE Build complete` |
| 差异与迁移静态检查 | `git diff --check`；`rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.109__add_presale_h5_page_sections.sql` | 通过；新增迁移无 Flyway 占位符 |
| 对象设计器 Playwright mock | 复用 `127.0.0.1:5173`，拦截认证、菜单和对象设计接口，1024x768 验证五入口及分组子页 | 五入口、数据模型三子页、默认视图两子页通过；旧 `actions` 深链归入默认视图；无横向溢出、控制台错误或页面错误 |

新增后端桥接测试验证：应用入口与 `configKey` 不匹配时不叠加配置；匹配时从已发布应用不可变快照保留对象级 options，并注入应用级 `flowInteraction`。

独立对象默认视图额外验证：不展示列表自定义操作编辑入口，保存列表/详情模板时不调用对象动作保存接口，也不覆盖 `designerOptions.actions`；嵌入式应用设计器继续允许维护应用级动作。

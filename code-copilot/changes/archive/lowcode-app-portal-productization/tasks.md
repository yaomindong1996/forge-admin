# 任务拆分 — 低代码应用门户产品化改造

> change: `lowcode-app-portal-productization`
> status: implemented-with-platform-deferrals-and-environment-acceptance-pending
> 原则：不推翻现有能力，用宜搭式产品化外壳包装 Forge 工程内核；一个 Task 一个可独立提交的原子变更；数据库变更先冻结协议再执行。

---

## 前置条件

- [x] 按用户本次执行指令采用 `spec.md` 第 11 章技术决策及第 13 章建议值。
- [x] 读取 `code-copilot/rules/automated-testing-standard.md`，并创建本变更的 `test-spec.md` 和 `execution-log.md`。
- [x] 核查 `forge-server/db/migration` 当前最新版本号，确定使用 `V1.0.124`。
- [x] 核查 `application-runtime.[applicationCode].vue` 的页面渲染组件（`GridBlockRenderer`、对象页渲染逻辑）可被门户复用。
- [x] 确认 `ai_business_application.options` 当前存储格式，新增配置列并保留旧 `options` 兼容读取。

---

## 阶段总览

| 阶段 | 目标 | Task | 完成标志 |
|---|---|---|---|
| P0 门户底座 | 独立运行时门户可访问、可渲染、可隔离；页面管理主入口替代旧工作台 | 1-4 | `/app/{code}` 能打开已发布应用；`/app-center/application/{code}/runtime` 为页面管理主入口；旧工作台路由已重定向 |
| P1 设置与发布 | 应用设置页（含权限归集）、应用发布页、主题水印 slug、空白应用页面形态设计 | 5-9、18 | 用户可在统一设置页配置门户与权限，在发布页查看状态/链接/回滚；空白应用可选择页面形态并直接设计页面 |
| P2 创建与市场 | 创建向导、应用中心升级、模板市场 | 10-13 | 用户可通过四向导创建应用，在应用市场发现模板 |
| P3 增强分发 | AI 助理绑定、外部分发、移动端适配 | 14-16 | 应用可绑定 AI 助理，可分发到工作台/外部平台 |

---

## 功能追踪

| Spec 功能 | 主要实现 Task | 验证 Task |
|---|---|---|
| 4.1 独立运行时门户 | Task 1-4 | Task 17 |
| 4.2 应用设置页 | Task 5-6 | Task 17 |
| 4.3 应用发布页 | Task 7-8 | Task 17 |
| 4.4 创建应用向导 | Task 10-12 | Task 17 |
| 4.5 应用中心升级 | Task 13 | Task 17 |
| 4.6 页面管理主入口 | Task 3-4 | Task 17 |
| 4.7 废弃应用工作台 | Task 3 | Task 17 |
| 4.8 空白应用引导与页面形态设计 | Task 18 | Task 17 |
| 4.9 AI 助理绑定 | Task 14 | Task 17 |
| 4.10 外部分发 | Task 15 | Task 17 |

---

## 建议实施顺序

```
Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6 → Task 7 → Task 8
→ Task 9 → Task 18 → Task 10 → Task 11 → Task 12 → Task 13 → Task 14 → Task 15 → Task 16 → Task 17
```

P0 完成后再进入 P1，P1 完成后再进入 P2，以此类推。每个 Task 完成后更新本文件状态并提交。

---

## Task 1：数据库基线 — 扩展应用主表与 Flyway 迁移

> 优先级：P0
> 状态：已实现（真实 Flyway 执行待环境验收）

### 目标

为 `ai_business_application` 新增门户产品化所需字段，并编写幂等 Flyway 迁移脚本，初始化存量应用数据。

### 涉及文件

- `forge-server/db/migration/V1.0.x__add_business_application_portal_config.sql`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/domain/entity/AiBusinessApplication.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessApplicationDTO.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/vo/businessapp/BusinessApplicationVO.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiBusinessApplicationMapper.xml`

### 关键变更

- 新增字段 `portal_slug VARCHAR(50)`，租户内唯一。
- 新增字段 `portal_config JSON`（MySQL 用 JSON 类型，Java 用 `String` 或 `Map`）。
- 新增字段 `ai_assistant_config JSON`。
- 初始化存量应用 `portal_slug = application_code`。
- 新增唯一索引 `UNIQUE (tenant_id, portal_slug)`。

### 验收标准

- Flyway 脚本在新库、存量库、重复执行场景下均不报错。
- 脚本具备 `information_schema` 防重复保护、`tenant_id=1`、无 `${...}` 占位符。
- 后端编译通过。
- 存量应用升级后可通过 `/app/{applicationCode}` 访问。

---

## Task 2：后端协议 — 门户配置接口与 slug 解析

> 优先级：P0
> 状态：已完成

### 目标

新增后端接口支持门户配置读写、按 slug 查询应用、slug 可用性校验，并改造运行时服务支持按 slug 解析。

### 涉及文件

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessApplicationController.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationService.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationRuntimeService.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/AiBusinessApplicationMapper.xml`
- `forge-admin-ui/src/api/business-application.js`

### 关键接口

```java
// 按 slug 查询应用详情
@GetMapping("/by-slug/{portalSlug}")
public RespInfo<BusinessApplicationVO> detailBySlug(@PathVariable String portalSlug);

// slug 可用性校验
@GetMapping("/slug-available")
public RespInfo<Boolean> slugAvailable(@RequestParam String portalSlug,
                                        @RequestParam(required = false) Long excludeId);

// 保存门户配置
@PutMapping("/{id}/portal-config")
public RespInfo<Void> savePortalConfig(@PathVariable Long id,
                                        @RequestBody BusinessApplicationPortalConfigDTO config);
```

### 验收标准

- 按 slug 查询应用返回与按 code 查询一致的数据。
- slug 唯一性校验正确排除自身、正确拦截保留 slug。
- 运行时服务支持按 slug 解析并返回门户配置。
- API 加密/解密注解保持与现有接口一致。

---

## Task 3：前端基座 — 应用门户布局、路由与页面管理主入口

> 优先级：P0
> 状态：已完成

### 目标

新增 `app-portal` 布局和 `/app/:applicationCodeOrSlug` 路由；废弃旧应用工作台，将 `application-runtime.[applicationCode].vue` 升级为页面管理主入口。

### 涉及文件

- `forge-admin-ui/src/router/index.js`
- `forge-admin-ui/src/layouts/app-portal.vue`
- `forge-admin-ui/src/views/app-center/application-portal.vue`
- `forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue`
- `forge-admin-ui/src/views/app-center/application.[applicationCode].vue`（废弃）
- `forge-admin-ui/src/views/app-center/application-workspace/ApplicationWorkspaceHeader.vue`（废弃）

### 关键行为

- 新布局不含 Forge 控制台顶部/左侧菜单。
- 布局包含：应用顶栏、导航区、内容区、水印层。
- 未发布/已停用应用显示统一不可用页面。
- 用户无权限时显示统一无权限页面。
- 原 `/app-center/application/:applicationCode` 路由重定向到 `/app-center/application/:applicationCode/runtime`。
- 页面管理主入口 Header 精简为：左侧应用信息，右侧「运行应用」「设置」「发布」「更多」。
- 页面管理主入口左侧导航分两段：系统页面（个人工作台/待办/已办/发送/抄送）+ 我创建的页面。

### 验收标准

- 访问 `/app/{applicationCode}` 显示独立门户布局。
- 访问 `/app-center/application/{code}/runtime` 显示页面管理主入口，不再是纯设计态。
- 访问 `/app-center/application/{code}` 自动重定向到 runtime 页面。
- 布局切换不闪烁、不依赖控制台全局 CSS。
- 页面管理主入口左侧展示系统页面和我创建的页面，不区分列表页/表单页。

---

## Task 4：前端渲染 — 门户页面渲染、页面管理预览/设计态切换与权限过滤

> 优先级：P0
> 状态：已完成（真实 UI/权限 E2E 待环境验收）

### 目标

在门户中渲染应用页面树、首页、自定义编排页、业务对象页；在页面管理主入口实现预览态与设计态切换。

### 涉及文件

- `forge-admin-ui/src/views/app-center/application-portal.vue`
- `forge-admin-ui/src/views/app-center/components/portal/PortalNavigation.vue`（新建）
- `forge-admin-ui/src/views/app-center/components/portal/PortalPageRenderer.vue`（新建）
- `forge-admin-ui/src/views/app-center/components/portal/PortalEmptyState.vue`（新建）
- `forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue`

### 关键行为

- 调用 `businessApplicationRuntimeByCode` 加载已发布版本快照。
- 根据 `portal_config` 主题色渲染导航和顶栏。
- 根据 `homePageId` 和权限自动选中首页。
- 自定义编排页复用 `GridBlockRenderer`。
- 业务对象页复用现有对象 CRUD/表单/详情渲染逻辑。
- 页面管理主入口默认展示预览态：
  - 系统页面渲染工作台/待办/已办/发送/抄送视图。
  - 用户页面渲染数据列表或表单预览。
- 点击页面项「编辑」按钮后，右侧切换为设计态画布，可保存草稿、预览、退出设计。

### 验收标准

- 已发布应用的多页面可在门户中正常切换。
- 无权限页面不显示，无可用页面时显示无权限提示。
- 主题色正确应用到导航和顶栏。
- 页面切换保留 query 参数不丢失。
- 页面管理主入口选中用户页面后默认展示数据列表/表单预览，点击编辑进入设计态。
- 新建数据管理页在页面树中只显示一个页面项，自动生成列表和表单视图。

---

## Task 5：应用设置页 — 基础属性与访问地址

> 优先级：P1
> 状态：已完成

### 目标

新增 `/app-center/application/:applicationCode/settings` 页面，实现基础属性、主题色、水印、访问地址配置。

### 涉及文件

- `forge-admin-ui/src/router/index.js`
- `forge-admin-ui/src/views/app-center/application-settings.[applicationCode].vue`（新建）
- `forge-admin-ui/src/views/app-center/components/settings/AppSettingsBasic.vue`（新建）
- `forge-admin-ui/src/views/app-center/components/settings/AppSettingsAccess.vue`（新建）
- `forge-admin-ui/src/api/business-application.js`

### 关键行为

- 左侧设置分组菜单，右侧配置面板。
- 主题色支持预设 + 自定义颜色选择器。
- 水印支持开关、自定义文字、用户名、当前时间占位。
- 访问地址支持自定义 slug，实时校验唯一性。
- 保存时合并到 `application.options` 并调用 `updateBusinessApplication`。

### 验收标准

- 设置页可正常加载当前应用配置。
- 修改主题色/水印/slug 后保存成功。
- slug 校验正确拦截保留词和重复值。
- 保存后刷新门户页面，新配置生效（发布后）。

---

## Task 6：应用设置页 — 导航、权限、全球化、高级

> 优先级：P1
> 状态：已完成

### 目标

完成应用设置页剩余分组：导航设置、应用权限、全球化、高级。

### 涉及文件

- `forge-admin-ui/src/views/app-center/components/settings/AppSettingsNavigation.vue`（新建）
- `forge-admin-ui/src/views/app-center/components/settings/AppSettingsPermission.vue`（新建）
- `forge-admin-ui/src/views/app-center/components/settings/AppSettingsGlobalization.vue`（新建）
- `forge-admin-ui/src/views/app-center/components/settings/AppSettingsAdvanced.vue`（新建）

### 关键行为

- 导航设置支持切换导航风格、显示/隐藏 Logo 和名称、启用收起。
- 页面排序与页面管理主入口左侧导航顺序双向同步。
- 应用权限支持设置管理员、可见范围（角色/部门/用户）。
- 页面级权限在「应用权限」分组中展示页面树，为每个页面分配角色/部门/用户权限。
- 全球化支持多语言开关、默认语言、时区。
- 高级支持代码生成前缀、运行时缓存策略。

### 验收标准

- 各分组表单数据正确加载和保存。
- 导航风格切换后门户运行时生效。
- 可见范围和页面权限配置持久化到 `portal_config`。
- 应用设置页已覆盖所有应用级配置，旧应用工作台「权限」Tab 不再使用。

---

## Task 7：应用发布页 — 状态卡片与组织内访问

> 优先级：P1
> 状态：已完成（真实发布/回滚 E2E 待环境验收）

### 目标

新增 `/app-center/application/:applicationCode/publish` 页面，实现发布状态卡片、访问链接、二维码、版本历史。

### 涉及文件

- `forge-admin-ui/src/router/index.js`
- `forge-admin-ui/src/views/app-center/application-publish.[applicationCode].vue`（新建）
- `forge-admin-ui/src/views/app-center/components/publish/AppPublishStatusCard.vue`（新建）
- `forge-admin-ui/src/views/app-center/components/publish/AppPublishAccess.vue`（新建）
- `forge-admin-ui/src/views/app-center/components/publish/AppPublishVersionHistory.vue`（新建）

### 关键行为

- 状态卡片展示当前状态、版本号、最近发布时间、发布人。
- 立即发布调用现有 `publishBusinessApplication`。
- 停用/启用调用现有 `updateBusinessApplicationStatus`。
- 访问链接生成 `/app/{portalSlug}`，支持复制和二维码。
- 历史版本列表展示版本号、时间、发布人，支持查看详情和回滚。

### 验收标准

- 发布页正确显示当前应用发布状态。
- 点击发布触发版本快照生成。
- 回滚操作调用现有回滚接口。
- 二维码可扫描到正确访问地址。

---

## Task 8：应用发布页 — AI 助理与工作台分发

> 优先级：P1
> 状态：部分完成（AI 与 Forge 工作台分发配置/查询已完成，钉钉外部同步待平台协议）

### 目标

在应用发布页增加 AI 助理绑定和分发到 Forge 工作台能力。

### 涉及文件

- `forge-admin-ui/src/views/app-center/components/publish/AppPublishAiAssistant.vue`（新建）
- `forge-admin-ui/src/views/app-center/components/publish/AppPublishDistribute.vue`（新建）
- `forge-admin-ui/src/api/business-application.js`
- 后端 `BusinessApplicationController`、`BusinessApplicationService`

### 关键行为

- AI 助理绑定：选择已有助理或创建新助理，选择可访问页面。
- AI 助理配置保存到 `ai_assistant_config`。
- 分发到工作台：将应用添加到当前用户「我的应用」或指定角色首页。
- 分发记录持久化到用户配置或角色配置（根据现有能力选择）。

### 验收标准

- AI 助理配置可保存并在发布页展示状态。
- 分发到工作台后，目标用户在 Forge 首页看到应用入口。
- AI 助理访问数据时经过 Forge 权限校验。

### 实施说明

- AI 助理配置已保存并纳入发布快照；正式对话能力由 Task 14 完成。
- Forge 工作台已提供当前用户/角色投放的服务端查询投影，首页读取真实启用且已发布的投放；钉钉渠道仍只持久化受管连接器配置，等待外部 Connector 协议。

---

## Task 9：后端快照与发布校验

> 优先级：P1
> 状态：已完成（真实发布/回滚 E2E 待环境验收）

### 目标

确保所有新增配置（门户配置、AI 助理配置）纳入版本快照，并在发布时校验合法性。

### 涉及文件

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationSnapshotService.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationPublishService.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationPublishCheckService.java`（若存在）

### 关键行为

- 快照构建时包含 `portal_config` 和 `ai_assistant_config`。
- 发布前校验 `portal_slug` 是否合法、是否冲突。
- 发布前校验 AI 助理绑定的页面是否存在于当前版本中。
- 回滚时恢复完整快照，包括新增配置。

### 验收标准

- 新版本快照 JSON 中包含 `portalConfig` 和 `aiAssistantConfig`。
- 非法 slug 阻止发布并给出明确错误。
- 回滚后门户配置恢复到历史版本。

---

## Task 10：创建应用向导 — 基础框架与空白创建

> 优先级：P2
> 状态：已完成

### 目标

改造应用中心创建入口，新增全屏/大弹窗创建向导，先实现空白创建。

### 涉及文件

- `forge-admin-ui/src/views/app-center/index.vue`
- `forge-admin-ui/src/views/app-center/components/AppCreateWizard.vue`（升级或新建）
- `forge-admin-ui/src/views/app-center/components/create/AppCreateBlank.vue`（新建）

### 关键行为

- 点击「创建应用」打开全屏/大弹窗向导弹窗。
- 顶部四入口 Tab：智能创建、从模板创建、从 Excel 创建、空白创建。
- 三种创建入口卡片使用不同渐变色背景：智能创建（紫）、空白创建（蓝）、Excel 创建（绿），图标 56px。
- 空白创建：输入应用名称、编码、图标、套件，调用现有 `createBusinessApplication`。
- 创建成功后进入页面管理主入口。

### 验收标准

- 创建向导弹窗可正常打开和关闭。
- 空白创建流程与现有创建一致。
- 创建成功后跳转到页面管理主入口 `/app-center/application/{code}/runtime`。
- 三种创建入口卡片颜色区分明显，视觉上不单调。

---

## Task 11：创建应用向导 — 模板市场

> 优先级：P2
> 状态：当前协议范围已完成（官方模板/AI 已实现，组织私有模板待持久化协议，真实 E2E 待环境验收）

### 目标

实现「从模板创建」和「智能创建」两个向导入口。

### 涉及文件

- `forge-admin-ui/src/views/app-center/components/create/AppCreateTemplate.vue`（新建）
- `forge-admin-ui/src/views/app-center/components/create/AppCreateAi.vue`（新建）
- 后端 `BusinessApplicationTemplateService`、`BusinessApplicationCodegenService`

### 关键行为

- 模板市场展示官方模板；组织私有模板在持久化/发布/发现协议建立前展示明确空态。
- 模板卡片显示：名称、图标、描述、已启用次数、立即启用/生成源码按钮。
- 智能创建：输入场景描述，调用 AI 生成对象、页面和流程建议；用户确认后初始化对象/页面及最小业务流程设计草稿，不自动部署 Flowable。
- 从模板创建：选择模板后调用 `initializeBusinessApplicationTemplate`。

### 验收标准

- 模板卡片可展示和搜索。
- 从模板创建后应用包含预设对象和页面。
- 智能创建可生成可初始化的应用方案。
- 含审批/流转的智能方案可创建应用级流程设计草稿，流程节点和 BPMN 仍由设计器维护。
- 「生成源码」按钮可跳转到代码生成页面。

---

## Task 12：创建应用向导 — Excel 导入

> 优先级：P2
> 状态：已完成（首 Sheet 建模草稿，真实接口 E2E 待环境验收）

### 目标

实现「从 Excel 创建应用」向导，自动识别表头并生成对象和页面。

### 涉及文件

- `forge-admin-ui/src/views/app-center/components/create/AppCreateExcel.vue`（新建）
- 后端新增 Excel 解析服务：`BusinessApplicationExcelImportService.java`（新建）
- 后端 Controller 新增接口：`POST /ai/business/application/{id}/import-excel`

### 关键行为

- 上传 Excel 文件，解析 Sheet 和表头。
- 推荐字段类型（字符串、数字、日期、下拉选项）。
- 用户确认后创建数据对象、列表页、表单页。
- 生成后进入对象设计器微调。

### 验收标准

- Excel 表头可正确解析为字段。
- 生成的对象包含正确字段类型和列表/表单页。
- 复杂 Excel（多 Sheet）至少处理第一个 Sheet。

---

## Task 13：应用中心升级 — 我的应用 + 应用市场

> 优先级：P2
> 状态：当前协议范围已完成（组织私有模板保留诚实空态）

### 目标

将 `app-center/index.vue` 从列表升级为「我的应用 + 应用市场」双视图。

### 涉及文件

- `forge-admin-ui/src/views/app-center/index.vue`
- `forge-admin-ui/src/views/app-center/components/AppMarketPanel.vue`（新建）
- `forge-admin-ui/src/views/app-center/components/MyApplicationsPanel.vue`（新建）
- `forge-admin-ui/src/views/app-center/components/AppCard.vue`（升级）

### 关键行为

- 顶部 Tab 切换「我的应用」和「应用市场」。
- 我的应用分组：我创建的、我有权限的、最近使用。
- 应用市场分组：官方模板、组织私有模板空态、推荐应用。
- 应用市场类型切换时强制刷新数据，避免旧数据残留；当前类型 Tab 高亮样式统一。
- 应用卡片快捷操作：页面管理、运行、发布、代码生成、删除。

### 验收标准

- 双视图可正常切换。
- 搜索和筛选生效。
- 应用卡片操作跳转正确（页面管理入口跳转到 runtime 页面）。
- 应用市场「官方模板/组织私有模板/推荐应用」切换后数据正确刷新，无残留或白屏。

---

## Task 14：AI 助理与应用绑定

> 优先级：P3
> 状态：部分完成（发布态安全问答已完成，真实数据查询/写入未开放）

### 目标

将 `forge-plugin-ai` 的助理能力与应用绑定，使应用可拥有专属 AI 助理。

### 涉及文件

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationAiAssistantService.java`（新建）
- `forge-admin-ui/src/views/app-center/components/publish/AppPublishAiAssistant.vue`
- AI 插件相关接口对接

### 关键行为

- 应用可创建或关联一个 AI 助理。
- 配置助理可执行的操作：查询数据、填写表单、分析统计。
- 助理访问业务数据时复用应用运行时权限。
- 助理对话入口可嵌入到门户顶栏或首页。

### 验收标准

- 应用可成功绑定 AI 助理。
- 助理可查询应用内授权数据。
- 助理不能访问未授权页面数据。

### 实施说明

- 助理重新加载不可变发布快照，并校验当前用户可见页面、已发布 `pageIds` 和配置能力。
- 当前只向模型提供页面结构和应用上下文，不提供真实业务行；`form` 只给填写建议，`query/analysis` 不声称已查询或修改真实数据。

---

## Task 15：外部分发到工作台

> 优先级：P3
> 状态：部分完成（Forge 工作台已支持当前用户/角色投放读取与取消，钉钉外部同步待连接器能力）

### 目标

实现将应用分发到钉钉工作台、企业微信等外部平台（先实现钉钉）。

### 涉及文件

- 后端 `BusinessApplicationDistributeService.java`（新建）
- 钉钉/企微 SDK 接入配置
- `forge-admin-ui/src/views/app-center/components/publish/AppPublishDistribute.vue`

### 关键行为

- 在应用发布页配置钉钉工作台应用凭证。
- 调用钉钉开放 API 注册应用/更新应用。
- 生成钉钉工作台访问地址。

### 验收标准

- 配置凭证可保存并脱敏展示。
- 分发到钉钉后，组织成员可在钉钉工作台看到应用入口。
- 分发失败给出明确错误提示。

### 实施说明

- 页面和后端只接受受管连接器标识，拒绝保存 AppKey/AppSecret 等明文凭证。
- 当前状态固定为 `PENDING_EXTERNAL_SYNC`；仓库没有可调用的钉钉应用注册/更新 Connector API，未宣称组织成员已在钉钉工作台看到入口。

---

## Task 16：门户移动端适配与 H5 入口

> 优先级：P3
> 状态：已完成（响应式构建通过，移动真机待环境验收）

### 目标

使应用门户在移动端有基本可用体验，并支持生成 H5 访问入口。

### 涉及文件

- `forge-admin-ui/src/views/app-center/application-portal.vue`
- `forge-admin-ui/src/layouts/app-portal.vue`
- `forge-h5-ui` 相关运行时渲染（可选）

### 关键行为

- 门户布局响应式：移动端导航折叠为底部 Tab 或顶部抽屉。
- CRUD 页面在移动端适配为卡片列表或简化表格。
- 生成 H5 访问二维码。

### 验收标准

- 门户在移动浏览器中可正常访问和切换页面。
- H5 二维码可扫描打开。
- 复杂页面至少保证可读和基础操作。

---

## Task 17：集成测试与验收

> 优先级：P0-P3 通用
> 状态：部分完成（自动化验证通过，真实 DB/API/UI E2E 待环境验收）

### 目标

完成全链路验证，包括后端编译、前端构建、接口测试、UI 验证、数据库迁移验证。

### 涉及文件

- `code-copilot/changes/lowcode-app-portal-productization/test-spec.md`
- `code-copilot/changes/lowcode-app-portal-productization/execution-log.md`

### 验证矩阵

| 验证项 | 命令/方法 | 通过标准 |
|---|---|---|
| 后端编译 | `mvn -pl forge-admin-server -am package -DskipTests` | 无编译错误 |
| 前端构建 | `pnpm build` | 无构建错误 |
| Flyway 静态检查 | `rg -n '\$\{[^}]+\}' forge-server/db/migration` | 无输出 |
| 数据库迁移 | 启动 `forge-admin-server` | `forge_schema_history` 成功 |
| 接口验证 | curl 测试新增接口 | 返回符合预期 |
| UI 验证 | 浏览器/Playwright | 门户、设置、发布、创建向导可用 |
| 权限验证 | 无权限用户访问门户 | 正确拦截 |
| 版本验证 | 发布/回滚后访问门户 | 配置随版本生效/恢复 |

### 本轮验证结论

- 已通过：Generator 32 模块与 Admin 45 模块 reactor 编译、100 个 `*BusinessApplication*Test`、前端定向 ESLint、11 个前端单测和 Vite 生产构建。
- 已通过：`git diff --check`；本变更 `V1.0.124` 无 `${...}` 占位符。
- 已通过：环境验收暴露的字典缓存泛型兼容修复，AOP 6 tests、字典定向 8 tests 和 Admin 45 模块聚合编译通过。
- 受阻：全 reactor `test-compile` 被仓库既有测试构造器漂移阻断，详见 `execution-log.md`。
- 受阻：当前 JVM 无法 self-attach Byte Buddy agent，完整 starter 套件中 4 个既有 Mockito Manager 用例未能执行完成；无 Mockito 的本轮相关用例已通过。
- 未执行：真实 MySQL/Flyway、启动 Admin/Flow、curl 接口、Playwright/真机、钉钉 Connector 同步。

### 验收标准

- P0/P1 代码与自动化验证通过；真实运行环境验收项单独保留。
- 所有失败项有根因和下一步。
- `execution-log.md` 记录完整证据。

---

## Task 18：页面形态与字段组件

> 优先级：P1
> 状态：部分完成（页面形态与表单保存最小闭环已实现；列表专用设计体验与真实运行环境继续验收）

### 目标

解决空白应用启动路径断裂问题：一个页面就是一个业务对象的一种形态。用户创建空白应用后，在页面管理主入口选择页面形态，直接进入对应设计器；拖拽字段组件到画布即创建业务对象字段；保存时原子生成/更新对象、字段、列表视图、表单视图。

### 涉及文件

- `forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue`
- `forge-admin-ui/src/views/app-center/components/designer/PageTypeSelector.vue`（新建：页面形态选择弹窗）
- 现有表单/列表设计器组件（升级或新建）：支持字段组件拖拽即创建对象字段、页面形态切换
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationPageDesignService.java`（新建）
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller/BusinessApplicationController.java`
- `forge-admin-ui/src/api/business-application.js`

### 关键行为

- 空白应用进入页面管理主入口后，右侧显示引导卡片「开始设计你的第一个页面」。
- 点击「创建数据页」弹出页面形态选择弹窗：表单页、列表页、列表+表单、自定义页面。
- 用户输入页面名称，系统自动推导业务对象编码（可编辑）；页面名称作为对象显示名称，页面编码作为对象编码。
- 选择形态后直接进入对应设计器：
  - 表单页：画布为纯表单区域。
  - 列表页：画布为表格区域。
  - 列表+表单：画布上方列表区，下方/抽屉为表单区。
  - 自定义页面：自由画布，不强制绑定对象。
- 设计器顶部显示业务对象信息「数据对象：customer（客户管理）」，点击可编辑对象名称/编码。
- 左侧组件库支持标签页切换：字段组件、对象字段、布局组件、高级组件。
  - 字段组件：拖拽到画布自动创建对象字段。
  - 对象字段：展示当前对象已有字段，拖拽到画布直接复用。
- 表单/列表+表单设计器：中间画布，右侧字段属性面板。
- 列表页设计器：中间表格画布，右侧列属性面板。
- 右侧属性面板展示字段编码、字段类型，支持编辑；底部提供「查看对象结构」和「进入高级对象设计器」入口。
- 拖拽字段组件/列类型到画布 → 自动创建对应业务对象字段。
- 字段编码自动生成，用户可编辑；已存在数据的字段禁止删除和类型变更。
- 点击「保存」后，后端原子完成：保存布局 → 创建/更新业务对象 → 创建/更新字段 → 生成/更新列表视图 → 绑定导航。
- 完成后回到页面管理主入口，新页面出现在左侧「我创建的页面」，右侧展示对应预览（列表/表单/列表+表单）。

### 验收标准

- 空白应用进入页面管理主入口后能看到引导卡片。
- 创建数据页时支持选择页面形态：表单页、列表页、列表+表单、自定义页面。
- 设计器顶部可见业务对象信息，且可编辑对象名称/编码。
- 左侧组件库支持「对象字段」标签页，可复用已有字段。
- 表单/列表设计器支持拖拽字段组件到画布，自动创建业务对象字段。
- 字段属性面板支持编辑字段名称、编码、类型、必填、默认值；编码自动生成且可编辑。
- 已存在数据的字段禁止删除和类型变更，并给出明确提示。
- 保存后后台原子创建/更新对象、字段、列表视图、表单视图，无中间失败残留。
- 新页面出现在页面树中，右侧可预览数据列表/表单/列表+表单，并可新增数据。

### 已完成增量：字段组件完整支持

- [x] 左侧 33 个字段组件统一由组件合同生成，并具备字段默认类型。
- [x] 后端模型校验支持全部字段组件，`slider`、`year` 等扩展组件不再报“不支持的控件类型”。
- [x] 13 个扩展字段组件发布后保留原组件类型，不回退为 `input`。
- [x] 多选、穿梭框和三类范围组件按 JSON 数组持久化，读取兼容历史逗号分隔值。
- [x] 19 个高级页面组件编译为正式 `widget` 节点，并可由表单运行时渲染。
- [x] 前后端组件合同、运行时编译和持久化回归测试已通过。

### 2026-08-18 页面形态与表单保存最小闭环

- [x] 新增页面形态选择弹窗，支持表单页、列表页、列表 + 表单、自定义页面；页面名称会自动推导可编辑对象名称/编码。
- [x] 空白应用页面管理入口增加首屏引导和「开始设计你的第一个页面」入口；数据页创建后直接打开表单设计器。
- [x] 表单设计器顶部持续展示对象名称/编码；左侧组件库保留字段组件与字段资产双 Tab，至少覆盖单行文本、数字、日期、下拉、开关五种基础字段。
- [x] 新增 `POST /ai/business/application/{id}/design-page`，在元数据事务内保存页面草稿、对象、字段、表单 Schema、默认列表/详情视图和应用对象关联；MySQL DDL 在事务提交后执行并支持重试。
- [x] 后端按真实数据记录数保护字段删除、字段编码变更和存储类型变更；已有数据字段在返回 Schema 中写入 `fieldBinding.locked`，前端同步禁用删除、编码和类型操作。
- [x] 对象字段从字段资产复用到画布时继续继承数据结构锁；后端按稳定组件 ID 对比已保存表单，直接请求也不能通过合并字段基线绕过删除、字段编码或组件类型保护。
- [x] 保存后关闭表单设计器、刷新应用对象摘要并回到页面管理主入口；对象字段复用时会将运行字段目录作为保存基线，避免误判为删除。
- [x] 新增页面形态、对象字段复用、原子保存、字段锁定和 Controller 权限回归测试；真实 MySQL/Flyway、Admin 启动和 Playwright 验收仍由环境验收补充。

---

## 任务状态总览

| Task | 优先级 | 状态 | 依赖 |
|---|---|---|---|
| 1 数据库基线 | P0 | 已实现，待真实 Flyway | 无 |
| 2 后端协议 | P0 | 已完成 | Task 1 |
| 3 前端基座（含页面管理主入口与废弃工作台） | P0 | 已完成 | Task 2 |
| 4 前端渲染（含预览/设计态切换） | P0 | 已完成，待真实 E2E | Task 3 |
| 5 设置页基础 | P1 | 已完成 | Task 2 |
| 6 设置页进阶 | P1 | 已完成 | Task 5 |
| 7 发布页状态 | P1 | 已完成，待真实 E2E | Task 2 |
| 8 发布页增强 | P1 | 部分完成：配置态 | Task 7 |
| 9 快照与发布校验 | P1 | 已完成，待真实 E2E | Task 1、Task 2 |
| 10 创建向导基础 | P2 | 已完成 | 无 |
| 11 创建向导模板/AI | P2 | 当前范围完成：AI 含流程草稿，私有模板待协议 | Task 10 |
| 12 创建向导 Excel | P2 | 已完成，待真实 E2E | Task 10 |
| 13 应用中心升级 | P2 | 当前范围完成，私有模板为空态 | Task 10 |
| 14 AI 助理绑定 | P3 | 部分完成：安全问答 | Task 8、Task 9 |
| 15 外部分发 | P3 | 部分完成：Forge 工作台已落地，钉钉待外部同步 | Task 7 |
| 16 移动端适配 | P3 | 已完成，待真机 E2E | Task 4 |
| 17 集成测试 | 通用 | 部分完成：环境验收待补 | 所有 Task |
| 18 页面形态与字段组件 | P1 | 部分完成：字段组件/Widget 合同已完成，其余页面形态全链路待验收 | Task 3、Task 4 |

---

## 验收标准汇总

### P0 完成标准

- [x] `/app/{applicationCodeOrSlug}` 路由和已发布快照加载已实现。
- [x] 门户使用独立布局，不含控制台菜单。
- [x] 已停用/未发布应用返回不可用状态。
- [x] 页面权限由运行时服务过滤，相关单测通过。
- [x] 多页面导航和 query 切换已实现。
- [x] 页面管理主入口已替代旧工作台；旧工作台路由重定向到 `/app-center/application/{code}/runtime`。
- [x] 页面管理主入口左侧展示系统页面 + 我创建的页面，右侧支持预览态与设计态切换。

### P1 完成标准

- [x] 应用设置页可配置主题、水印、slug、导航、权限（管理员/可见范围/页面级权限）。
- [x] 应用发布页可查看状态、访问链接、二维码、历史版本、回滚。
- [x] 新增配置进入发布快照，运行时只读取发布版本。
- [x] 回滚快照恢复新增配置的代码与回归测试已完成。
- [x] 空白应用可选择页面形态并直接设计页面，拖拽字段组件自动创建对象字段，后台隐式生成业务对象。

### P2 完成标准

- [x] 创建向导包含智能/模板/Excel/空白四入口，三种入口卡片使用紫/蓝/绿渐变色区分。
- [x] 应用中心支持「我的应用 + 应用市场」双视图。
- [x] 从模板创建复用现有模板初始化服务。
- [x] 从 Excel 创建对象及列表/表单页草稿，首 Sheet 解析单测通过。
- [x] 智能创建支持对象、页面和流程建议；确认后只创建可编辑的最小流程草稿。
- [x] 应用市场类型切换（官方/私有/推荐）后数据正确刷新，无残留或白屏。
- [ ] 组织私有模板发布/发现：等待模板快照、发布权限和租户内持久化协议。

### P3 完成标准

- [x] 应用可绑定 AI 助理并在发布快照授权范围内安全问答（当前不执行真实业务查询/写入）。
- [ ] 钉钉工作台真实同步：等待仓库提供受管 Connector API 和测试组织。
- [x] 门户已完成响应式布局与 H5 链接/二维码，待移动真机验收。

### 环境验收待办

- [ ] 在隔离 MySQL 执行 `V1.0.124`，验证新库、存量库、重复执行和索引结果。
- [ ] 启动 Admin 后完成新增接口、发布/回滚、权限用户和门户全链路验证。
- [ ] 使用 Playwright/移动真机验证设置、发布、四种创建方式、页面形态设计、门户导航与 H5。
- [ ] 接入真实受管连接器后验证 Forge 首页投放和钉钉工作台同步。

---

## 备注

- 所有 Task 完成后需同步更新 `spec.md` 中的「执行日志」和「审查结论」。
- 实施过程中若发现 Spec 需要调整，先更新 Spec 再执行代码，保持文档领先代码。
- 每个 Task 提交信息格式：`[lowcode-app-portal-productization] <中文简述>`。

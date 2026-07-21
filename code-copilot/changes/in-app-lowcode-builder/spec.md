# 应用内低代码搭建器（前端交互升级）
> status: apply
> created: 2026-07-21
> complexity: 🔴复杂
> change: `in-app-lowcode-builder`
> dependency: `app-first-lowcode-workbench` 已建立应用聚合、对象编排、工作台和应用级 `options` 持久化能力。

## 1. 背景与目标

当前 Forge 已具备对象、字段、表单、列表、规则、流程、动态 CRUD、组件画布与发布能力，但用户的工作路径仍偏向“应用中心 → 对象 → 专业设计器 → 访问入口”。业务人员难以把它理解为一个可直接打开、拥有首页、目录和菜单的完整应用。

本变更在不新建第二套低代码事实来源、不修改后端接口或数据库结构的前提下，新增“应用内低代码搭建器”的前端交互层。用户从应用进入运行壳后，在具备编辑权限时点击“编辑应用”，即可创建页面组（目录）、页面、首页内容和页面组件；需要配置字段、表单、列表、CRUD、规则或流程时，仍在同一应用上下文中复用既有设计器能力。

完成后应满足以下可验证结果：

- 应用拥有独立运行壳：应用级导航、首页和内容区域；不把系统“用户管理、角色管理、字典管理”等全局控制台菜单混入应用导航。
- 应用编辑者可从应用页面进入编辑态，管理目录和页面；创建页面时明确选择所属目录，空目录提供“在本组创建页面”，不依赖“拖拽页面或页面组到这里”的抽象提示。
- 新建空白/介绍页面时，画布显示简洁引导和常用组件；用户可点击推荐组件或悬浮“添加组件”按钮插入组件。
- 组件选择框按常用、业务、内容和高级分类展示，并支持搜索；点击组件即可插入当前画布，拖拽是可选快捷方式而非必经路径。
- 页面选中、组件选中和空白状态共享右侧上下文面板：页面设置、快捷推荐和组件属性均复用既有 Schema、组件目录、画布和属性编辑能力。
- 业务数据页面可在应用搭建器中选择业务对象，并在当前上下文进入列表、表单、详情、规则、流程的已有配置能力；不得复制字段、表单或流程 Schema。
- 页面目录、排序、首页和页面引用作为 `ai_business_application.options.inAppBuilder` 前端配置保存；继续使用既有应用更新、版本和发布链路。
- 界面保持克制、紧凑、企业后台风格：不新增渐变大横幅、堆叠统计卡或复杂多级配置；高级设计能力按需展开。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与持久化链路

- `forge-admin-ui/src/views/app-center/application.[applicationCode].vue` 已是应用工作台入口，能按应用编码加载工作台、按分区承载对象、入口、流程、权限和发布能力。
- `forge-admin-ui/src/views/app-center/application-preview.[applicationCode].vue` 已能以应用为上下文加载业务对象设计草稿并交给 `LowcodePreviewPane` 预览。
- `forge-admin-ui/src/api/business-application.js` 已提供应用工作台读取和应用更新 API；`forge-admin-ui/src/api/business-app.js` 已提供对象设计保存 API。
- `forge-server/.../domain/entity/AiBusinessApplication.java`、`BusinessApplicationDTO.java` 与 `BusinessApplicationVO.java` 都包含 `options` JSON 字段。
- `BusinessApplicationService.update(...)` 接收并规范化 `options`；现有应用聚合发布/版本能力会对应用扩展配置做快照。因此首期应用内页面编排可存入 `options`，无需新增表、Flyway 或 Controller。

### 2.2 已有低代码设计能力

- `forge-admin-ui/src/components/lowcode-builder/page/ComponentPalette.vue` 已提供按区块过滤的业务组件、基础控件、字段筛选和 HTML5 拖拽数据协议。
- `forge-admin-ui/src/components/lowcode-builder/page/LowcodePageBuilder.vue`、`BuilderCanvas.vue`、`BuilderZone.vue` 与 `ComponentPropertyPanel.vue` 已提供页面画布、放置区和属性编辑基础。
- `forge-admin-ui/src/components/lowcode-builder/page/page-schema.js` 是页面 Schema、默认组件、默认布局和字段同步的共享事实来源。
- `forge-admin-ui/src/views/app-center/components/designer/BusinessListDesigner.vue` 已支持业务对象内多页面、新增、复制、删除、重命名和页面跳转目标校验；其 `pages[]` 是对象页面资产，不等同于应用级导航树。
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`、`BusinessDetailDesigner.vue`、`BusinessFlowBindingPanel.vue` 已覆盖表单、详情与流程配置；`runtime-rules.js` 和 `RuntimeRulesEditor.vue` 已统一处理运行规则。
- `forge-admin-ui/src/views/ai/crud-page.vue`、`ListPageGridDesigner.vue` 和 `LowcodePreviewPane.vue` 已提供真实 CRUD 或设计态运行预览。

### 2.3 现有边界与风险

- `ai_business_app` 是访问入口，不是应用页面树；`BusinessAppOpenService.buildRuntimeTargetRoute(...)` 仅按入口、`configKey`、`pageKey` 和运行模式解析运行页，不能被改造成第二个应用页面存储。
- 业务对象的 `pageSchema.pages[]` 解决“一个对象内的列表/详情/自定义页”，但不记录跨对象目录、首页或应用级菜单，不能直接作为应用导航树。
- 现有全局菜单由 `sys_resource`、动态路由和系统 RBAC 管理。应用内编辑不得直接修改该菜单，否则会跨应用影响权限、路由和其他用户。
- 仅靠前端隐藏编辑按钮不能构成授权。首期保存继续使用既有 `ai:businessApplication:edit` 后端授权；新页面级细粒度权限只做前端编排字段预留，不在本变更中声称已完成后端强制。

## 3. 功能点

### 3.1 应用运行壳与统一入口

输入：用户从应用中心或应用工作台打开某业务应用。

处理：前端加载应用、对象摘要、发布态配置和 `options.inAppBuilder`；以“应用壳”替代系统后台侧栏，展示应用名称、应用切换、通知/头像等共享顶栏元素、应用内导航与页面内容。

输出：

- 运行用户只看到已发布/可访问的应用内页面；系统控制台作为顶部受权限控制的跳转入口，不混入左侧业务菜单。
- 编辑者在右上角看到“编辑应用”；普通用户不显示该操作。
- 首期运行壳支持首页、介绍/内容页、业务对象页和外部/既有入口页的前端路由编排；不改写动态 CRUD 运行时。

### 3.2 页面组和页面管理

输入：编辑者在应用编辑态点击“添加”。

处理：

- “新建页面组”只要求名称、图标和所属位置；页面组是导航容器，不要求拥有独立画布。
- “新建页面”要求名称、页面类型和所属目录。若从某目录上下文发起，自动预选该目录。
- 初始页面类型包括：首页/介绍页、业务数据页、空白内容页、外部/既有入口页；模板选项以简短说明和预览呈现。
- 空页面组显示“在本组创建页面”和“移动已有页面到此组”，不出现强制拖拽提示。
- 每个节点的更多菜单提供重命名、移动到、上移、下移、删除；拖拽排序可后续补充，首期不得成为唯一操作。

输出：左侧为清晰的应用导航树；首页默认存在且固定在顶层，其他页面可按目录管理。

### 3.3 空白页引导、组件弹窗与画布插入

输入：编辑者创建空白/介绍页，或点击页面/区块悬浮“添加组件”。

处理：

- 无组件时，中间画布展示页面说明和 4～6 个常用组件推荐，例如介绍区、指标卡、业务数据列表、录入表单、待办和图表。
- 用户点击推荐项时，使用既有组件目录的默认属性创建组件并写入当前页面 Schema。
- 页面主体与可插入容器旁显示轻量悬浮 `+`；点击后打开组件选择弹窗，支持关键字、常用/业务/内容/高级分类。
- 插入位置规则：当前选中容器内 → 当前选中组件后 → 当前区块末尾 → 页面主体末尾。发生兜底时给出明确提示。
- 保留现有拖拽协议和画布能力，但不为普通用户强制展示复杂组件货架。

输出：用户可不学习拖拽操作，仅通过点击完成首个页面；插入后自动选中新组件并在右侧显示属性。

### 3.4 右侧上下文与属性编辑复用

输入：当前页面为空、选中页面、选中组件或选中业务数据页。

处理：右侧仅展示当前上下文所需内容：

| 状态 | 右侧内容 |
|---|---|
| 空白页面 | 页面简介、常用组件、页面模板、最近使用组件 |
| 选中页面 | 名称、图标、目录归属、可见性预留、页面说明、页面类型 |
| 选中组件 | 复用 `ComponentPropertyPanel` 与现有组件属性 Schema |
| 业务数据页 | 绑定对象、打开方式、默认页面/表单、快捷配置入口 |

不得复制现有属性编辑实现；新壳只适配选择状态、插入位置和应用级属性。

### 3.5 在应用上下文配置 CRUD、表单、规则和流程

输入：编辑者在业务数据页点击“配置数据页面”“配置表单”“配置规则”或“配置流程”。

处理：

- 先选择/新建/关联业务对象；新页面仅保存对象引用、目标 `pageKey`、默认参数和展示设置。
- 轻量配置（页面标题、菜单名称、默认打开模式、默认查询参数、快捷操作可见性）在应用右侧完成。
- 深度配置复用已有 `BusinessListDesigner`、`BusinessFormDesigner`、`BusinessDetailDesigner`、`RuntimeRulesEditor` 和既有流程配置入口，可在抽屉、全屏子工作区或深链中打开，但始终携带应用返回上下文。
- 字段、表单 Schema、运行规则、流程绑定和对象发布继续只有既有对象设计链路能保存；应用搭建器不得写第二份拷贝。

输出：业务用户把“客户管理、商机表单、审批页”等放入应用并做常用设置；实施人员可无缝进入已存在的专业配置。

### 3.6 草稿、预览与发布提示

输入：用户改变应用导航、页面编排或应用内组件布局。

处理：

- 编辑态明确显示“草稿编辑中”；提供保存草稿、预览、发布和退出操作。
- 保存调用既有应用更新接口，把版本化配置写入 `options.inAppBuilder`，不产生新的后端协议。
- 发布仍走既有应用发布工作台；发布前显示“导航/首页/页面编排将随应用版本发布”的提示。
- 发布后继续沿用当前版本/回滚能力；本变更不新增页面级独立版本。

输出：用户不会误以为保存即对所有运行用户生效。

## 4. 业务规则

- 应用内菜单只表示当前应用的页面导航，禁止在编辑态直接新增、删除或改写 `sys_resource` 全局菜单。
- “用户管理、角色管理、部门、字典”等属于系统控制台；应用内只展示“成员与权限”业务投影视图或受控跳转，不能复制全局用户管理。
- 首页是应用页面，创建应用时若缺失则在前端初始化默认首页；首页不允许变为其他节点的子节点。
- 页面组只能包含页面组或页面，不能直接承载业务数据/组件画布；页面才可承载画布。
- 页面组和页面名称在同一父节点下不得为空；同级重名允许但 UI 应提示，节点 ID 必须稳定且不能以名称作为引用。
- 删除页面组前必须选择“同时删除子页面”或“将子页面移动到顶层/指定目录”；不得静默丢弃子节点。
- 业务数据页只保存对象引用和页面引用，不复制 `modelSchema`、`formDesignerSchema`、`pageSchema`、`viewSchema`、`linkageSchema` 或流程 JSON。
- 组件插入只能来自现有、明确注册的组件目录；不在本期开放任意 HTML、任意 JavaScript 或任意远程组件。
- 不支持的组件、缺失对象、未发布对象或失效 `pageKey` 必须显示可理解的占位和“去配置”动作，不能渲染 `undefined`。
- 首期页面可见性为前端编排预留字段；真正运行授权继续由现有访问入口、对象权限和数据权限后端链路决定。

## 5. 数据变更

本变更不新增表、不新增字段、不执行 Flyway。

应用级编排写入已存在的 `ai_business_application.options`：

```json
{
  "inAppBuilder": {
    "schemaVersion": 1,
    "homePageId": "page_home",
    "nodes": [
      { "id": "page_home", "type": "page", "pageType": "home", "title": "首页", "parentId": null, "sort": 0 },
      { "id": "group_sales", "type": "group", "title": "销售管理", "parentId": null, "sort": 10 },
      { "id": "page_opportunity", "type": "page", "pageType": "object", "title": "商机管理", "parentId": "group_sales", "sort": 0,
        "objectRef": { "objectId": "…", "objectCode": "crm_opportunity", "pageKey": "list" } }
    ],
    "pages": {
      "page_home": { "layout": { "items": [] } }
    }
  }
}
```

约束：配置中不得保存 Token、密码、AK/SK、脚本源代码或对象完整 Schema；版本升级必须通过前端 Schema 归一化兼容旧空 `options`。

## 6. 接口变更

本期不新增或修改后端接口，复用：

| 场景 | 既有接口 | 前端用途 |
|---|---|---|
| 加载应用工作台 | `GET /ai/business/application/code/:applicationCode/workspace` | 应用、对象、入口摘要与 `options` |
| 保存应用草稿 | `PUT /ai/business/application` | 保存 `options.inAppBuilder` |
| 加载/保存对象设计 | 既有 `/ai/business/object/:id/designer` 及设计保存接口 | 深度配置字段、列表、表单和规则 |
| 发布/回滚 | 既有应用发布接口 | 继续管理应用版本 |
| 打开运行入口 | 既有 `/ai/business/app/:id/open-info` | 复用入口权限和安全校验 |

如实施中发现工作台响应未返回 `application.options` 或已有应用更新接口不允许保留未知 JSON 键，必须停止前端实现并新增后端兼容任务，不允许绕过后端或改用浏览器本地存储作为多人共享事实来源。

## 7. 影响范围

- `forge-admin-ui/src/router/index.js`：新增应用运行壳/编辑器前端路由；保持既有工作台和对象设计器路由兼容。
- `forge-admin-ui/src/views/app-center/`：应用工作台头部增加“进入应用/编辑应用”入口，新建应用内运行壳和编辑器视图。
- `forge-admin-ui/src/components/lowcode-builder/`：只复用或抽取现有画布、组件目录、属性面板和 Schema；不得破坏旧设计器。
- `forge-admin-ui/src/api/business-application.js`：只在必要时补充已有接口封装或参数归一化；不新增协议。
- `code-copilot/changes/in-app-lowcode-builder/`：本变更的 Spec、任务、测试计划和执行日志。

不影响：`forge-server/**` 业务实现、Flyway、`sys_resource`、动态 CRUD API、Flowable 引擎和既有对象设计 Schema。

## 8. 风险与关注点

- ⚠️ 权限：首期的“编辑应用”仅是前端入口与既有应用 edit 权限的组合，页面级可见性不能被误宣传为后端安全隔离。
- ⚠️ 配置冲突：必须严格区分应用页面编排与对象页面 Schema。把对象 Schema 拷贝到应用 options 会导致发布、回滚和对象设计双写冲突。
- ⚠️ JSON 体积：`options` 适合导航、页面引用和轻量布局。若自定义页面布局逐渐变大或需要独立审计，应另开后端数据模型，不能无限堆进 `options`。
- ⚠️ 兼容：旧应用没有 `inAppBuilder` 时必须根据应用名称、主对象和入口生成只在前端内存中的默认首页，首次保存才写入 options。
- ⚠️ 运行态：自定义页面必须采用已注册组件渲染；不能引入任意脚本执行、动态组件 URL 或绕过 `open-info` 的外部页面。
- ⚠️ UX：不把全局用户管理等基础配置塞入应用菜单。整体感由统一顶栏、应用切换、成员投影与一致视觉建立，不由重复系统菜单建立。

## 8.5 测试策略

- **测试范围**：应用内编排 Schema 的创建、归一化、迁移、移动/删除节点、插入定位、业务对象引用校验；关键 Vue 交互单测；现有对象设计器与运行路由回归。
- **浏览器验收**：空应用创建首页 → 建目录 → 在目录创建业务页面 → 插入推荐组件/弹窗组件 → 修改属性 → 保存刷新 → 预览 → 进入对象深度设计 → 返回应用；同时验证普通用户不出现编辑入口。
- **构建检查**：新增单测后执行定向 Vitest；前端 ESLint；`pnpm build`。执行前必须读取 `code-copilot/rules/automated-testing-standard.md` 并追加 execution-log。
- **覆盖率目标**：新增编排 Schema 工具分支覆盖率不低于 80%；核心节点移动、删除、默认首页、组件插入和引用失效必须有用例。
- **独立 Test Spec**：是，进入 `/test` 或实际编码前创建/补充 `test-spec.md`。

## 9. 待澄清

- [ ] V1 应用运行壳的公开路由是否固定为 `/app/:applicationCode`，还是保持在 `/app-center/application/:applicationCode/runtime`？建议后者，避免与现有动态菜单和外部部署路径冲突。
- [ ] V1 是否允许“外部/内嵌页面”作为应用内页面类型？建议只引用已经存在且通过 `open-info` 校验的访问入口，不提供自由输入外部 URL。
- [ ] V1 的首页组件范围是否冻结为：介绍区、指标卡、业务数据列表、录入表单、待办、图表、文本、图片、分栏、分隔线？建议冻结该范围，其余组件走“高级设计”。
- [ ] 现有应用发布版本是否已包含 `options` 全量快照？需要在编码前通过现有发布代码和测试确认；若不包含，V1 只能明确标注为“应用草稿编排”，不能承诺随发布版本回滚。

## 10. 技术决策

1. **一个平台、两种视图，而不是两套低代码。** 新应用内搭建器负责导航、页面编排、组件插入和轻量配置；当前对象设计器继续负责字段、表单、列表、规则、流程和运行态编译。
2. **应用页面仅引用对象资产。** 业务数据页引用 `objectId/objectCode/pageKey`，禁止复制模型、表单或流程 Schema。
3. **前端优先持久化到应用 options。** 已有 JSON 字段、应用更新和发布版本能力满足第一期；不为纯交互升级新增后端 API。若现有保存/快照不能保证契约，转为后端任务，不使用 LocalStorage 规避。
4. **点击插入优先，拖拽增强。** 复用现有拖拽与画布实现，但常用流程采用模板、推荐组件、悬浮插入按钮和“移动到”菜单。
5. **应用壳与系统控制台分层。** 应用左侧只显示业务页面，系统级菜单通过顶部受控入口回到原控制台；应用成员/权限是系统用户的范围投影，不复制账号体系。
6. **渐进披露。** 默认展示页面树、画布和最少属性；复杂对象/表单/规则/流程通过当前页面的“高级配置”进入已有实现。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|---|---|---|---|
| Proposal | completed | `spec.md`, `tasks.md` | 仅文档，未修改业务代码、后端接口或数据库 |

## 12. 审查结论

尚未进入实现审查。编码前需先完成第 9 章待澄清项并获得 HARD-GATE 确认。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-21。
- **确认人**：用户。
- **确认内容**：用户明确要求“开始写代码”。默认决策：运行壳路由采用 `/app-center/application/:applicationCode/runtime`；首期组件范围按第 9 章建议冻结；外部页面仅引用既有受控访问入口；先复用现有应用 `options` 和发布快照能力，不新增后端接口或数据库结构。

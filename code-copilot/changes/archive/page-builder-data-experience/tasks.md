# 任务拆分 — 页面搭建器数据体验统一改造
> change: `page-builder-data-experience`
> 当前阶段：Fix 已完成（真实服务 UAT 待环境）
> 拆分顺序：P0 缺陷修复 → P1 术语 → P2 引导态 → P3 模板流程 → 测试收口
> 每个编码任务必须独立提交、同步文档并按增量差异验证

## 前置条件

- [x] HARD-GATE：2026-08-16 用户执行 `/apply page-builder-data-experience`，确认采用 `spec.md` 第 5 章 D1-D4 默认决策。
- [x] 进入 `/apply` 前读取根 `AGENTS.md`、当前 `spec.md/tasks.md` 与相关 Skill（`forge-coding-standards`）。
- [x] 执行 `/test` 或阶段收尾前读取 `code-copilot/rules/automated-testing-standard.md`，创建 `test-spec.md` 与 `execution-log.md`。
- [x] 基线确认：`cd forge-admin-ui && pnpm vitest run src/views/app-center` 为 137/139；`app-entry-targets.spec.js` 有 2 条历史失败（移动端 URL 是否包含 `appId` 的断言差异）。

## 里程碑

| 里程碑 | 范围 | 完成标志 |
|---|---|---|
| M1 可用性缺陷修复 | Task 1-2 | AiForm/AiTable 等绑定对象后字段立即渲染；AiCrudPage 无回退 |
| M2 术语与引导 | Task 3-5 | 组件目录无裸技术名；未选数据源显示引导态；选对象即所见即所得 |
| M3 模板流程与双世界 | Task 6-7 | dataTemplate 模板停在画布；属性面板可跳对象设计器精调 |
| M4 验证收口 | Task 8 | lint/测试全绿，Spec 合规审查通过 |

## 任务清单

### Task 1 — 共享数据区块类型常量与三处判断放开（P0）

- 文件：`forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue`
- 动作：
  1. 定义模块级常量 `DATA_FIELD_BLOCK_TYPES = ['AiCrudPage', 'AiForm', 'AiTable', 'data-table', 'search-form', 'detail-info']`（种子取 L2030 现有集合），L2030 处判断改为复用该常量。
  2. `resolvePageBlockRuntimeCrudProps`（L2209）、`isPageBlockRuntimeCrudLoading`（L2221）、`preloadPageBlockCrudRuntimeProps`（L2239）三处 `blockType !== 'AiCrudPage'` 改为 `!DATA_FIELD_BLOCK_TYPES.includes(blockType)`（按 D4 确认的范围）。
- 验收：AiForm 区块绑定 objectRef 后，`resolvePageBlockFields` 返回该对象 fieldCatalog；AiCrudPage 行为不变；同对象多区块只触发一次 `loadRuntimeCrudProps`（缓存生效）。

### Task 2 — 数据类区块字段渲染回归与微调（P0）

- 文件：`application-runtime.[applicationCode].vue`、`GridBlockRenderer.vue`（如需）
- 动作：逐一验证 AiForm/AiTable/data-table/search-form/detail-info 在绑定对象后的渲染路径；发现仍不渲染的类型（如渲染侧另有 blockType 分支）在本任务内修复。
- 验收：六类区块绑定对象后字段均可见；vitest 新增「AiForm 绑定对象后字段渲染」用例（可挂 `GridBlockRenderer` 组件级测试或 runtime 集成用例）。

### Task 3 — 组件目录业务化命名（P1）

- 文件：`forge-admin-ui/src/components/lowcode-builder/page/page-schema.js`
- 动作：
  1. `AiCrudPage`：title「数据列表」、desc「选择业务对象，自动生成筛选、表格与新增/编辑/删除，内置数据表单弹窗」。
  2. `AiForm`：title「数据表单」、desc「选择业务对象，按字段自动生成录入表单，可独立提交」。
  3. 按 D3 决策保留技术名（新增 `techTitle` 字段或 tooltip 方案，实施时按组件目录渲染结构择优）。
  4. 排查目录分组展示名（`resolveComponentPickerGroup` 返回 chart/list/view/other 的展示映射），统一为中文「数据 / 图表 / 展示 / 其他」。
- 验收：组件目录渲染无 `AiCrudPage`/`AiForm` 裸技术名主标题；既有引用 `title` 做名称展示的地方（如区块标题、面包屑）同步生效且无重名冲突。

### Task 4 — 未选数据源引导态（P2）

- 文件：`forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue`
- 动作：
  1. 新增判定：数据类区块（复用 Task 1 常量传入或 props 标记）无有效 objectRef 且无可渲染 fields 时，渲染引导卡（图标 + 「选择业务对象后，字段将自动生成」 + 「选择数据源」按钮）。
  2. 引导卡按钮 emit 事件由 runtime 处理：选中该区块并打开属性面板数据源配置（沿用现有区块激活/属性面板交互，不新造弹层）。
  3. 运行态（非 editing）未配置数据源时按现有空态约定显示轻提示，不阻塞渲染。
- 验收：拖入 AiForm 未选对象显示引导卡；点击「选择数据源」定位到属性面板；选择对象后引导卡消失、字段渲染。
- 依赖：Task 1（否则选完对象仍空白）。

### Task 5 — 属性面板数据源选择器全类型可用（P2）

- 文件：`forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue`
- 动作：确认属性面板数据源选择器的显示条件（现按 blockType 分支），扩展为对全部 `DATA_FIELD_BLOCK_TYPES` 可见；`updateBlockDataSource` 对新类型正确写入 objectRef 并触发预加载。
- 验收：六类区块均可在属性面板选择/切换数据源；切换后字段目录按缓存策略刷新。

### Task 6 — dataTemplate 模板停在画布（P3，依赖 D1 决策）

- 文件：`application-runtime.[applicationCode].vue`、`in-app-builder/page-template-catalog.js`
- 动作：
  1. `createFormAssetForPageCrud`（L1740）：移除自动 `formDesignerMode.value = true`；表单资产仍自动创建并绑定 formAssetId（保持既有数据结构），但视图停留在页面画布，AiCrudPage 区块显示引导态（依赖 Task 4）。
  2. `page-template-catalog.js`：crud/tree-table/master-detail 三个模板 description 改为「创建后选择业务对象，在页面画布完成搭建」口径。
  3. 属性面板 AiCrudPage 区块增加「设计数据表单」入口（置 `formDesignerMode = true` 打开现有表单设计器），保证精调路径不丢。
- 验收：新建页面选「数据列表（CRUD）」→ 停在画布；点「设计数据表单」可进入表单设计器；存量含 formAssetId 页面打开行为正常。

### Task 7 — 双世界跳转链接（P3）

- 文件：`application-runtime.[applicationCode].vue`
- 动作：数据类区块绑定对象后，属性面板显示「在对象设计器中精调」链接，跳转 `/app-center/object-designer/{objectCode}`（复用现有路由与 designSection 参数定位表单页/列表页面板）。
- 验收：预售单应用内自定义页面绑定 PS_PRESALE_ORDER 后可一键跳对象设计器；返回设计器状态不丢。

### Task 8 — 测试与收口（M4）

- 动作：
  1. 按 `test-spec.md` 执行增量验证：新增用例（AiForm 字段渲染 / 引导态 / 模板落点）+ 回归（app-center 全量、AiCrudPage 相关既有用例）。
  2. `pnpm lint` 全绿；手工验收 spec 第 6 章 1-4 条（含预售单对象页回归）。
  3. 更新 `execution-log.md`，Spec 合规自检（对照第 3/4/6 章逐条勾验）。
- 验收：全部测试通过；验收标准 1-5 全部满足。

## 风险与回归重点

| 风险 | 缓解 |
|---|---|
| 放开预加载后 AiTable/data-table 渲染侧另有分支导致异常 | Task 2 逐类型回归；异常类型可临时收窄 D4 范围 |
| `title` 改中文影响既有按 title 匹配的逻辑（如搜索、分组正则） | Task 3 全局 grep title 消费点；分组正则已含中文关键词 |
| 模板停画布后老用户找不到表单设计器 | Task 6 保留「设计数据表单」入口 + 引导卡引导选数据源 |
| 存量 builder schema 兼容 | 引导态纯运行时判定，不写 schema；无迁移脚本 |

## 执行状态

- [x] Research：完成两套页面世界、字段加载链路、组件目录、模板创建流程调查（结论见 spec 第 2 章）。
- [x] Proposal：生成 `spec.md` 和本 `tasks.md`，未修改生产代码。
- [x] HARD-GATE：用户已通过 `/apply` 确认 D1-D4 默认方案。
- [x] Apply/M1：P0 缺陷修复完成，六类数据区块共享字段目录加载和对象缓存。
- [x] Apply/M2：中文业务术语、次要技术名、数据源引导和全类型数据源选择完成。
- [x] Apply/M3：数据模板保留画布，对象设计器和数据表单精调入口完成。
- [x] Review/Test：目标测试 9/9、相关 ESLint、生产构建、差异检查通过；app-center 为 140/142，未新增失败。
- [x] Fix/Review-1：切换业务对象时清理旧字段引用、查询字段引用及字段设置，同对象重复选择保留配置。
- [x] Fix/Review-2：AiForm 按显式字段顺序和可见性渲染，空引用过滤系统字段、非表单字段和停用字段。
- [x] Fix/Review-3：嵌套数据区块按自身对象解析运行时上下文，完整区块树参与预加载且同对象共享缓存。
- [x] Fix/Review-4：AiForm 仅在发布运行态调用新增接口，具备 loading、成功重置和失败反馈；编辑/草稿预览禁止真实提交。
- [x] Fix/Review-5：失效对象引用不再被视为有效数据源，恢复引导态并停止无效预加载/API 占位符迁移。
- [x] Fix/Test：目标测试扩充至 18/18；app-center 143/145，仅保留相同 2 条历史失败；ESLint 0 error；生产构建通过。
- [ ] 环境 UAT：真实对象选择后的字段渲染、数据模板交互及预售单发布页回归；本地 Admin/验证码服务不可用，待联调环境补验。

## 第三轮任务（2026-08-16 追加：表单上下文打通 + 布局承载分区）

### Task 9 — 表单设计器对象上下文打通

- 文件：`application-runtime.[applicationCode].vue`
- 动作：进入表单设计器（formDesignerMode）时，按当前表单资产所属区块的 objectRef 加载对象 designer 数据（relations/actions，按 objectId 缓存），传入 ForgeFormDesigner 的 `:relations/:actions/:object-code/:object-name`；未绑定对象时维持现状（fields 仍传表单资产字段）。
- 验收：绑定对象的表单进入设计器后子表分区可选关系，不再显示"关系已失效"。

### Task 10 —「关联子表」画布容器组件

- 文件：`ForgeFieldShelf.vue`、`ForgeFormCanvasNode.vue`、`ForgePropertyPanel.vue`、`page-schema`/画布节点工厂
- 动作：新增 subTable 容器（标题、relationKey 选择、displayMode 行内表格/卡片列表/底部抽屉）；仅 `relations.length > 0` 时出现在货架；画布渲染子表占位预览；属性面板提供 relationKey/displayMode 配置。
- 验收：对象上下文表单中可拖入并配置；无对象上下文不出现。

### Task 11 — pageSections 布局派生与保存写回

- 文件：`form-first/formDesignerSchema.js`（或 forge-form-designer 新增派生模块）+ `ForgeFormDesigner.vue`
- 动作：`derivePageSectionsFromLayout(components, legacySections)`：card/elCard、collapse/elCollapse 容器 → 内容分区（children 的 fieldBinding.fieldCode）；subTable 容器 → 子表分区；散落字段 → 默认分区；容器分区按组件 id 合并存量扩展属性（visibleInModes/collapsible/collapsedByDefault）；布局无任何容器但存量分区非空 → 保留存量。ForgeFormDesigner 保存 schema 时调用并写回 pageSections（bottomBar 原样保留）。
- 验收：派生函数单测；流程权限面板读到的分区与画布容器一致；存量表单（无容器布局）分区不丢。

### Task 12 —「页面分区」视图退役

- 文件：`ForgeFormDesigner.vue`
- 动作：三态切换移除 `sections`（保留 layout + detail）；移除 PageSectionEditor 挂载（组件文件与其测试保留）；card/collapse 容器属性面板补分区级配置（visibleInModes、折叠开关）；`initialCanvasView` validator 同步收窄。
- 验收：表单设计器顶部不再出现「页面分区」；分区能力经画布容器与属性面板完成。

### Task 13 — 第三轮测试与回归

- 动作：新增派生函数与子表容器用例；回归 `child-table-section-config.spec.js`、`pageSectionEditorUtils.spec.js`、`formDesignerSchema.spec.js`、app-center 全量；ESLint。
- 验收：spec 10.3 全部满足，无新增失败。

### 第三轮执行记录（2026-08-14）

- [x] Task 9：`application-runtime.[applicationCode].vue` 按 `activeFormAssetBlock → activeFormDesignerObjectRef → activeFormDesignerContext` 链路经 `businessObjectDesigner` 加载 relations/actions（按 objectId 缓存，失败仅 warn 降级），ForgeFormDesigner 传入 `:relations/:actions/:object-code/:object-name`；`activeFormFields` 合并对象 runtime fieldCatalog。
- [x] Task 10：`designerLayoutFactory.js` 新增 subTable 工厂；`ForgeFieldShelf.vue` 仅 `relations.length > 0` 时展示；`ForgeFormCanvasNode.vue` 子表占位预览（未配置提示）；`ForgePropertyPanel.vue` relationKey/displayMode 配置（选关系同步 header）；`normalizeRelationOption` 提取至 `pageSectionEditorUtils.js` 共用（relationKey 优先取 relationConfig）。
- [x] Task 11：新增 `pageSectionDerivation.js`（`derivePageSectionsFromLayout`：card/elCard/collapse/elCollapse → 内容分区、subTable → 子表分区、散落字段 → `section_default` 基本信息、无容器且存量非空 → 保留存量、扩展属性按 sectionId 继承）；`ForgeFormDesigner.updateSchema` 在 `deriveSectionsFromLayout` 开启时派生写回（undo/redo 同路径，幂等）。
- [x] Task 12（方案调整）：未物理移除 PageSectionEditor，改为 props 分流——`enableSectionsView`（默认 true）/`deriveSectionsFromLayout`（默认 false），自由编排宿主传 `false/true`（隐藏「页面分区」入口、`initialCanvasView='sections'` 回退 layout），对象设计器宿主（含预售单契约）零变化；`initialCanvasView` validator 保持兼容。
- [x] Task 13：新增 `pageSectionDerivation.spec.js` 6 用例全过（容器/子表/默认分区/存量保留/属性继承/嵌套容器）；app-center 149/151（新增 6 例全过，仅剩相同 2 条 app-entry-targets 历史失败）；改动文件 ESLint 0 error。
- [ ] 环境 UAT：绑定对象的表单 → 拖卡片分组/关联子表 → 保存 → 流程权限面板分区核对，待联调环境补验。

## 第四轮任务（2026-08-14 追加：对象设计器宿主切换布局承载分区）

> 用户决策：预售单所在的对象设计器（BusinessFormDesigner 宿主）同样按「布局承载分区」方式实现，退役「页面分区」视图。

### Task 14 — 底部操作栏编辑独立成组件

- 文件：`BottomBarEditor.vue`（新增）、`PageSectionEditor.vue`、`pageSectionEditorUtils.js`
- 动作：从 PageSectionEditor 抽取底部操作栏编辑（受控组件：按钮增删/拖拽排序/类型切换/显示条件/权限策略）；`appendMissingOptions` 提取至 utils 共用；PageSectionEditor 内嵌 BottomBarEditor 行为不变。
- 验收：PageSectionEditor.spec 拖拽排序断言（分区+底栏）通过。

### Task 15 — ForgeFormDesigner 底部操作栏入口与视图兼容

- 文件：`ForgeFormDesigner.vue`
- 动作：更多菜单新增「底部操作栏」弹窗（BottomBarEditor 即时写回 updateSchema，configureBottomAction 透传宿主）；`openPageSections` 在 `enableSectionsView=false` 时改跳画布。
- 验收：对象宿主/自由编排宿主均有底栏编辑入口；向导确认后跳画布可见容器。

### Task 16 — 子表分区 sectionId 锚定与容器链路

- 文件：`pageSectionDerivation.js`、`child-table-section-config.js`（safeKey 导出）、`ForgeFormCanvasNode.vue`、`ForgeFormCanvas.vue`、`ForgeFormDesigner.vue`
- 动作：派生 child_table 分区 sectionId 优先沿用存量（按 relationKey 匹配），否则 `child_${safeKey(relationKey)}`，无 relationKey 用组件 id；画布 subTable 容器预览加「配置」按钮，经 configureSubTable 三级转发 emit `editSubTableContainer`。
- 验收：流程权限按 sectionId 的存量配置不失配；画布容器可一键打开子表分区向导。

### Task 17 — BusinessFormDesigner 切换布局承载

- 文件：`BusinessFormDesigner.vue`
- 动作：传 `:enable-sections-view="false" :derive-sections-from-layout="true"`；子表分区向导确认后在画布 upsert subTable 容器（`subtable_${safeKey(relationKey)}` 稳定 id 幂等）；watch 画布容器 relationKey 差集自动 `removeChildTableSectionConfig` 清理 pageSchema；移除 PageSectionEditor 的 edit/remove 事件绑定及 handleRemoveChildTableSection。
- 验收：对象设计器三态只剩「表单布局/详情设置」；子表增删改走画布+向导；pageSchema 主子表链路同步。

### Task 18 — 第四轮测试与回归

- 动作：派生单测新增 sectionId 锚定 2 用例；PageSectionEditor.spec 适配 BottomBarEditor 子组件；ESLint；app-center 全量。
- 验收：151/153（仅剩相同 2 条 app-entry-targets 历史失败）；ESLint 0 error。

### 第四轮执行记录（2026-08-14）

- [x] Task 14：BottomBarEditor.vue 新增（受控模式，__editorKey 剥离后 emit）；PageSectionEditor 底栏区替换为内嵌组件，样式/常量/逻辑同步清理（1428→1090 行）；utils 导出 appendMissingOptions。
- [x] Task 15：更多菜单「底部操作栏」+ n-modal 弹窗即时写回；openPageSections 兼容禁用宿主。
- [x] Task 16：sectionId 锚定（存量 relationKey 匹配 → child_${safeKey} → 组件 id）；「配置」按钮 CanvasNode→Canvas→Designer→宿主事件链打通。
- [x] Task 17：对象设计器宿主禁用 sections 视图+开启派生写回；upsertSubTableContainer/collectSubTableRelationKeys/watch 差集清理；handleEditSubTableContainer 复用向导编辑。
- [x] Task 18：派生单测 8/8；PageSectionEditor.spec 1/1；app-center 151/153（2 条历史失败不变）；ESLint 0 error。
- [ ] 环境 UAT：预售单对象设计器 → 添加子表分区向导 → 画布出现容器 → 删除容器 → pageSchema 同步清理 → 流程权限分区 sectionId 不变，待联调环境补验。

## 第五轮：预售单画布初始结构 SQL 化（V1.0.116）

> 用户决策：预售单画布的容器化初始结构不手动配置，直接用 Flyway SQL 配置到位（第一次打开对象设计器即是布局承载分区结构）。

### Task 19 — 预售单画布容器结构种子脚本

- 文件：`forge-server/db/migration/V1.0.116__presale_form_canvas_layout_sections.sql`
- 动作：以 V1.0.108 权威字段定义为底，把 `formDesignerSchema.components` 重排为布局承载结构：4 个隐藏/辅助字段（预售单号/导购userid/门店编码/状态）顶层散落；4 个 card 容器（id=存量分区 sectionId：guide_info/member_info/payment/remark）嵌对应字段；3 个 subTable 容器（subtable_presale_items/subtable_pickup_return/subtable_operation_logs，props 显式声明 sectionId+relationKey+displayMode）。同步 4 处：ai_crud_config（options+page_schema zones[2]）、ai_business_object.designer_options、ai_business_object_design_version（page_snapshot+designer_options_snapshot，10 条 PUBLISHED）、ai_crud_config_version（9 条）。pageSections/bottomBar 等既有配置不动。
- 验收：UPDATE 幂等；WHERE 精确限定（config_key/seedKey/publish_status + JSON 校验），HR 套件同名小写对象不受影响。

### Task 20 — 派生支持 props.sectionId 显式锚定（同 relationKey 双视图）

- 文件：`pageSectionDerivation.js`、`__tests__/pageSectionDerivation.spec.js`
- 动作：resolveChildTableSectionId 优先读容器 `props.sectionId`；支撑预售单「商品明细（仅 create）/提货退货（edit/detail）」同一 presale_items 关系的两个分区视图各锚定各的存量分区（visibleInModes/标题继承不失配）。
- 验收：派生单测 +2（显式锚定继承、双容器同 relationKey 锚定），10/10 通过。

### 第五轮执行记录（2026-08-14）

- [x] Task 19：脚本编写并在开发库（forge_admin_new）执行成功；复核 components=11 顶层（4 散落+4 card+3 subTable）、pageSections=7 未动、design_version 10 条与 crud_version 9 条全部更新；forms[0] 旧平铺结构因 formKey 去重被顶层版本压制，下次保存自动清洗。
- [x] Task 20：派生改动+单测；真实库数据端到端对拍（导出 formDesignerSchema 跑 derivePageSectionsFromLayout）：7 个存量分区 sectionId 全保留、pickup_return 双视图可见性继承正确、payment 分区 pillSelect fieldOverrides 按 sectionId 继承、section_default 基本信息分区字段=[presaleNo,salesUserId,storeId,status]。临时对拍 spec 已删除。
- [x] 回归：app-center 153/155（新增 2 派生用例全过；2 条 app-entry-targets 历史失败不变）；ESLint 0 error。
- [ ] 环境 UAT：打开预售单对象设计器 → 表单布局画布直接呈现 4 个卡片分组 + 3 个关联子表容器（提货/退货容器标题可见）→ 随意拖动一次字段保存 → pageSections 派生写回后流程权限面板分区 sectionId 不变。
### 第六轮修复：发布检查误报布局容器「表单字段未绑定」（V1.0.117）

> 用户反馈：发布预售单业务对象时 7 个阻断项——guide_info/member_info/payment/remark/subtable_* 容器被报「未绑定业务字段」。

- 根因：发布检查 `BusinessObjectPublishService#checkFormDesignerSchema` 对 `fieldBinding.mode` 缺省的组件默认按业务字段校验；V1.0.116 种子的容器未写 fieldBinding（前端 normalize 会补 virtual，但发布检查读库内原始 JSON），7 个容器全部误伤。
- 修复（两层）：
  - 后端治本：`BusinessObjectPublishService` 新增 `FORM_VIRTUAL_COMPONENT_KEYS`（对齐前端 VIRTUAL_COMPONENT_KEYS 的布局容器/纯展示部分），发布检查按组件类型跳过字段绑定校验（白名单先行，与 `BusinessObjectDesignerService` 的判断顺序一致）；模块编译通过。
  - 数据治标：`V1.0.117__presale_container_virtual_field_binding.sql` 给 4 处共 6 个 JSON 列的索引 4-10 容器补 `fieldBinding {mode:'virtual', fieldCode:''}`；WHERE 校验组件类型+mode 缺省，重跑 0 命中幂等；开发库执行 1/1/10/9 全命中，复核 7 容器 mode 全部 virtual。
- 排查确认：后端其余 fieldBinding 消费点（BusinessObjectDesignerService 两处、BusinessFieldSchemaService 一处）均 `FORM_FIELD_COMPONENT_KEYS` 白名单先行，不受缺省 mode 影响；前端 `validateFormDesignerSchema` 用 `isFieldComponent` 白名单，无误报。
- [x] 数据修复已生效，重新发布即可通过（后端代码修复随下次服务重启生效，双层防护）。

## 第七轮：字段查询与应用工作台紧凑体验（2026-08-16）

### Task 21 — 字段自动查询配置业务化

- 文件：`forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/FieldEventRulesEditor.vue`
- 动作：将「设置字段查询规则」改为业务用户可理解的查询文案；查询源按「数据集 / 接口」分组；上下文参数提供当前登录用户、租户、组织和扫码内容等常用选项，自定义路径降级为高级配置；编辑区域使用中性白色背景。
- 验收：普通用户无需理解 `currentUser.userId` 即可完成当前用户 ID 参数绑定，仍兼容已有路径协议。

### Task 22 — 数据集查询分页窗口

- 文件：`forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/FieldEventRulesEditor.vue`、`forge-admin-ui/src/components/ai-form/field-event-runtime.js`
- 动作：数据集规则增加每次返回条数（1～100）配置，运行时透传既有 `pageNum/pageSize/maxRows`；外部接口协议保持不变。
- 验收：数据集查询不会默认拉取大结果集，字段回填仍按既有根对象/列表首行规则处理。

### Task 23 — 应用对象与页面入口紧凑布局

- 文件：`ApplicationObjectsPanel.vue`、`ApplicationEntriesPanel.vue`、`application.[applicationCode].vue`
- 动作：对象页三个添加动作合并为下拉菜单；对象列表和页面入口去除固定最小宽度与横向滚动，在窄屏改为多行紧凑布局；工作台分区导航取消 920px 横向滚动约束。
- 验收：对象页、页面入口页和窄屏工作台入口均不因内容不足产生横向滚动，既有菜单动作保持可达。

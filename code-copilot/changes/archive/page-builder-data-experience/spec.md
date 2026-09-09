# 页面搭建器数据体验统一改造
> status: review
> created: 2026-08-14
> complexity: 🟡中等
> change: `page-builder-data-experience`

## 1. 背景与目标

应用设计器目前存在两套"半成品"页面世界：

- **世界 A：对象页（配置推导型）**。预售单等业务页面由对象设计器的表单页配置、列表页配置、动作配置和入口绑定推导而来，运行时整页渲染一个 AiCrudPage。功能完备，但设计器中不存在"预售单申请页"这个完整页面实体，用户面对的是几个独立配置面板，没有整体页面视角。
- **世界 B：自定义页面搭建器（in-app-builder）**。已有拖拽画布与区块搭建能力，是用户期望的"页面搭建过程"。但数据类组件以技术名暴露（`AiCrudPage`、`AiForm`），用户不知道是干什么的；且 AiForm 绑定数据对象后表单内容不渲染（字段目录不加载）。

本变更以世界 B（页面搭建器）为统一门面：修复数据类区块字段渲染缺陷、组件目录业务化命名、建立"选对象即所见即所得"引导，并把数据模板的创建流程从"直接跳表单设计器"改为"停在页面画布继续搭建"。世界 A（对象页）退居高级配置，不迁移不删除。

完成后必须达到以下可验证结果：

1. 新建页面选择数据类模板后，停留在页面画布（不自动跳表单设计器），画布内完成数据源选择与字段渲染。
2. 画布上拖入或点击「数据表单」「数据列表」组件并选择业务对象后，字段立即渲染；未选择数据源时显示引导态而非空白。
3. 组件目录中不存在裸技术名：所有组件有中文名称与一句业务描述，技术名仅作为次要标注保留。
4. 预售单等存量对象页行为完全不变。
5. app-center 既有测试基线（48/48）不回退。

### 1.1 现状问题链路

```text
用户新建页面 → 选「数据列表（CRUD）」模板
  → createPageFromTemplate（L1704）
    → 创建 content 页 + AiCrudPage 区块
    → createFormAssetForPageCrud（L1740）→ formDesignerMode = true
      → 直接进入表单设计器（用户视角：又一个独立表单，没有页面搭建过程）

用户新建空白页 → 组件目录看到「AiCrudPage / AiForm」（技术名，不知何物）
  → 拖入 AiForm → 属性面板选择数据对象
    → updateBlockDataSource 写入 objectRef
      → preloadPageBlockCrudRuntimeProps：blockType !== 'AiCrudPage' 直接 return（L2240）
      → 字段目录永不加载 → resolvePageBlockFields 返回空 → aiFormSchema = [] → 表单空白
```

### 1.2 本次范围

- P0：数据类区块（AiForm/AiTable/data-table/search-form）运行时字段目录加载缺陷修复。
- P1：组件目录业务化改造（中文名 + 业务描述 + 技术名次要标注 + 目录分组中文化）。
- P2：数据类区块"未选数据源引导态"与"选对象即所见即所得"收口。
- P3：数据类模板（dataTemplate）创建流程改为停在页面画布；区块属性面板增加「在对象设计器中精调」跳转链接。

### 1.3 非目标

- 不迁移预售单等存量对象页到页面搭建器；对象页与流程绑定关系保持原样。
- 不把对象设计器的表单页/列表页配置能力搬进页面搭建器；两个世界通过跳转链接打通而非合并。
- 不改造 `page-form-data-provisioning.js` 的表单资产→对象供给链路（其语义面向 AiCrudPage 数据供给，AiForm 直渲染字段目录即可）。
- 不新增后端接口、不修改数据库结构；本变更为纯前端改造。
- 不在本 Proposal 阶段修改任何生产代码。

## 2. 代码现状（Research Findings）

### 2.1 字段目录加载只认 AiCrudPage（P0 根因）

`forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue`：

- L2209-2211 `resolvePageBlockRuntimeCrudProps`：`if (block.blockType !== 'AiCrudPage') return null`。
- L2221-2223 `isPageBlockRuntimeCrudLoading`：同样只认 `'AiCrudPage'`。
- L2239-2241 `preloadPageBlockCrudRuntimeProps`：同样只认 `'AiCrudPage'`，AiForm/AiTable/data-table/search-form 即使绑定了 `objectRef`，`loadRuntimeCrudProps` 也不会执行。
- L2108-2114 `resolvePageBlockFields` = 表单资产字段 + `resolvePageBlockRuntimeCrudProps(block)?.fieldCatalog` 合并；对 AiForm 两者皆空时返回空数组。
- L2030 已存在数据区块判断 `['AiForm', 'AiCrudPage', 'AiTable', 'data-table', 'search-form', 'detail-info']`，可作为共享常量的种子。

### 2.2 AiForm 渲染链路本身健康

`forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue`：

- L412-435：`blockType === 'AiForm'` 渲染 `<AiForm :schema="aiFormSchema" ...>`。
- L1640-1643：`aiFormSchema = (props.fields.length ? props.fields : resolvedFields).map(toAiFormField)`，fields 由外层传入。
- 结论：只要外层字段目录打通，AiForm 渲染无需改动；AiForm 消费 fields 不消费 CRUD api props，放开预加载无副作用。

### 2.3 组件目录裸技术名（P1 对象）

`forge-admin-ui/src/components/lowcode-builder/page/page-schema.js` L569-742 共 19 个 blockType：

- L624-627：`blockType: 'AiCrudPage'， title: 'AiCrudPage'， desc: '系统完整 CRUD 组件'`。
- L642-645：`blockType: 'AiForm'， title: 'AiForm'， desc: '系统表单组合组件'`。
- 其余组件（page-title/info-panel/stats-strip 等）title 已是中文，仅这两个数据组件为技术名。
- 目录分组 `resolveComponentPickerGroup`（runtime L1330-1339）返回 `chart/list/view/other` 英文键，前端展示层再映射中文（需在 Task 中确认展示映射位置）。

### 2.4 数据模板创建后直接跳表单设计器（P3 根因）

`forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue`：

- L1704-1723 `createPageFromTemplate`：dataTemplate 模板创建 content 页 + AiCrudPage 区块后调用 `createFormAssetForPageCrud(created.id)`。
- L1740-1774 `createFormAssetForPageCrud`：自动创建表单资产、选中 AiCrudPage 区块、`formDesignerMode.value = true` 直接切换到表单设计器视图。
- 模板目录 `page-template-catalog.js` 的文案承诺"创建后直接设计表单"，与"页面搭建过程"的用户预期冲突；这正是用户反馈"都是几个独立的表单"的直接来源。

### 2.5 属性面板数据源选择现状

- runtime L2380 附近 `updateBlockDataSource` 已支持为区块写入 `objectRef` 并触发预加载（但被 2.1 的 blockType 判断拦截）。
- 属性面板对数据源选择器的显示条件按 blockType 分支，需在 Task 中确认 AiForm/AiTable 等类型是否已显示数据源选择器（若未显示需纳入 P2）。

## 3. 功能点

### 3.1 P0：数据类区块字段目录加载修复

- 在 runtime 定义共享常量（如 `DATA_FIELD_BLOCK_TYPES`，种子取 L2030 现有集合），三处 `blockType !== 'AiCrudPage'` 判断改为 `!DATA_FIELD_BLOCK_TYPES.includes(blockType)`。
- `resolvePageBlockFields` 对 AiForm 等类型返回 runtime fieldCatalog 合并结果。
- 回归验证 AiTable/data-table/search-form/detail-info 的字段渲染路径是否同时受益或需要微调。

### 3.2 P1：组件目录业务化

- `AiCrudPage` → title「数据列表」，desc「选择业务对象，自动生成筛选、表格与新增/编辑/删除，内置数据表单弹窗」，技术名 `AiCrudPage` 保留为次要标注（tooltip 或角标）。
- `AiForm` → title「数据表单」，desc「选择业务对象，按字段自动生成录入表单，可独立提交」，技术名同上保留。
- 目录分组展示名统一中文：「数据 / 图表 / 展示 / 布局（其他）」。
- 空态引导图标（runtime L1346-1356）与推荐位（L1358-1364）同步使用新名称。

### 3.3 P2：引导态与所见即所得

- 未绑定数据源的数据类区块：GridBlockRenderer 显示引导卡（图标 + 「选择业务对象后，字段将自动生成」 + 「选择数据源」入口），替代当前空白/占位。
- 属性面板数据源选择器对全部数据类区块可见可用；选择后（依赖 P0）字段立即渲染。
- 引导卡的「选择数据源」点击行为：选中该区块并打开属性面板数据源配置（或内联弹出对象选择器，实施时按现有交互组件择优）。

### 3.4 P3：模板流程改造与双世界链接

- `createFormAssetForPageCrud` 不再自动 `formDesignerMode = true`；dataTemplate 模板创建后停留在页面画布，AiCrudPage 区块因未绑定数据源显示 P2 引导态。
- `page-template-catalog.js` 中 dataTemplate 模板描述文案改为「创建后选择业务对象，在页面画布完成搭建」。
- 表单精调入口保留：AiCrudPage 区块属性面板提供「设计数据表单」（进入现有表单设计器）与「在对象设计器中精调字段/列」（跳对象设计器对应面板）两个链接。
- 存量页面（已有 formAssetId 且 `formDesignerMode` 依赖）行为不回退：已有页面的表单设计器入口仍可正常打开。

## 4. 交互与实现规则

1. **存量兼容**：已有 builder schema 不做迁移改写；引导态按"运行时检测无 objectRef 且无可用 formAsset 字段"动态判定，不持久化新字段。
2. **字段目录缓存**：`runtimeCrudPropsByObjectId` 按 objectId 缓存，多区块共享；AiForm 与 AiCrudPage 绑定同一对象时只加载一次。
3. **只读与编辑态**：引导态在运行态（非 editing）对未配置数据源的区块显示轻提示（或按现有运行态空态约定），不阻塞页面渲染。
4. **命名唯一性**：中文名在目录内唯一，避免两个组件同名「数据表单」（AiForm 中文名与对象页「表单页」在文案上需可区分）。

## 5. 默认决策（HARD-GATE 已确认）

> 2026-08-16：用户执行 `/apply page-builder-data-experience`，确认按 D1-D4 默认方案实施。

| # | 决策点 | 默认方案 | 备选 |
|---|---|---|---|
| D1 | dataTemplate 模板创建后的落点 | 停在页面画布 + 区块引导态（P3 主张） | 保持现状跳表单设计器，仅改 P0/P1/P2 |
| D2 | AiForm 是否纳入表单资产→对象供给（provisioning） | 不纳入；AiForm 直接消费对象字段目录渲染 | 纳入（工作量+1 个任务，收益存疑） |
| D3 | 技术名保留方式 | 中文名为主标题，技术名 tooltip/角标次要展示 | 完全移除技术名 |
| D4 | P0 放开的区块范围 | `['AiCrudPage','AiForm','AiTable','data-table','search-form','detail-info']` 全集，逐类型回归 | 仅放开 AiForm（最小改动） |

## 6. 验收标准

1. 新建页面 → 「数据列表（CRUD）」模板 → 停留画布（若 D1 通过）；AiCrudPage 区块显示引导态；选择对象后列表与表单字段渲染。
2. 空白页拖入「数据表单」→ 属性面板选择业务对象 → 画布表单字段立即渲染（P0 核心验收）。
3. 组件目录无裸技术名；「数据列表」「数据表单」有中文描述。
4. 预售单应用发布后的运行页面行为与改造前一致（对象页回归）。
5. `pnpm lint` 与既有测试全绿：app-center 48/48 不回退；新增引导态/字段渲染的组件测试（vitest）覆盖 P0/P2 关键路径。

## 7. 影响范围（文件清单）

| 文件 | 变更类型 |
|---|---|
| `forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue` | 修改：三处 blockType 判断、引导态联动、模板创建流程、属性面板数据源可见性 |
| `forge-admin-ui/src/components/lowcode-builder/page/page-schema.js` | 修改：AiCrudPage/AiForm 目录文案与技术名标注 |
| `forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue` | 修改：数据类区块未绑定数据源的引导态渲染 |
| `forge-admin-ui/src/views/app-center/in-app-builder/page-template-catalog.js` | 修改：dataTemplate 模板描述文案 |
| `forge-admin-ui/src/views/app-center/in-app-builder/__tests__/` 或 `application-runtime` 相关测试 | 新增/修改：P0 字段渲染、引导态、模板落点用例 |

无后端、无数据库、无权限脚本变更。

## 8. Apply 结果（2026-08-16）

- P0-P3 已实现：六类数据区块共享字段目录加载与缓存；空 `fieldRefs` 使用对象字段目录渲染；组件目录改为中文业务名称、描述和次要技术名；未选数据源显示编辑态引导/运行态轻提示；数据模板创建后停留页面画布；数据属性面板提供对象设计器与数据表单入口。
- 目标测试 9/9、相关文件 ESLint、生产构建和 `git diff --check` 通过。
- app-center 回归 140/142；仅保留基线已有的 `app-entry-targets.spec.js` 两条移动端 URL 断言失败，本轮未新增失败。
- 真实浏览器可访问 Vite，但被鉴权重定向到登录页；本地 Admin `8580` 不可用且验证码接口返回 500，因此真实业务对象选择、预售单发布页回归留待可用联调环境验收。

## 9. Review Fix 结果（2026-08-16）

- 对象切换：仅当业务对象确实变化时清空 `fieldRefs`、`searchFieldRefs`、`fieldSettings` 和 `searchFieldSettings`，由新对象字段目录重新生成默认展示；重复选择同一对象保留现有配置。
- AiForm 字段：显式字段引用严格保留顺序并应用 `fieldSettings.visible`；空引用只回退到可用于表单、非系统且未停用的字段。
- 嵌套区块：当前页预加载遍历 `children`、Tabs 和栅格单元完整树；递归渲染按每个子区块解析字段、CRUD 配置、加载态和数据源有效性，同对象继续复用缓存。
- AiForm 提交：仅发布运行态开放提交，调用区块或对象运行配置的新增接口，支持普通/加密请求、提交 loading、成功重置与失败提示；编辑态和草稿预览不显示提交入口。
- 失效数据源：对象引用必须能匹配当前应用对象且 `valid !== false` 才视为已配置；失效引用显示数据源引导，也不会预加载字段或迁移旧 API 占位符。
- Fix 目标测试 18/18、相关 ESLint 0 error、生产构建和差异检查通过；app-center 143/145，仅保留基线已有的 2 条移动端 URL 断言失败。

## 10. 第三轮扩展：表单上下文打通与布局承载分区（2026-08-16）

### 10.1 背景与问题定位

- 用户反馈"页面分区看不懂"与"子表都显示失效状态"。调查结论：
  - ForgeFormDesigner 是共用的表单设计器内核，有两个宿主：对象设计器（BusinessFormDesigner，传 `:relations="draft.relations"` + actions，子表正常）与自由编排页面（application-runtime，只传 fields，`object-code` 传应用编码，relations/actions 均未传）。
  - 自由编排宿主下子表分区 relationKey 匹配不到任何关系，必然 100% 显示"关系已失效"（历史缺陷，非上一轮改坏）。
  - pageSections 的唯一真实消费方是流程审批权限（ApplicationFlowInteractionPanel 按 visibleSectionIds/readonlySectionIds 配置可见/只读）；运行时表单渲染按画布组件树，不读分区；后端仅 FormDesignerSchemaDTO 整体透传。
  - 结论：「表单布局」画布与「页面分区」编辑器是两套并行、互不联动的重复分组结构，这是"看不懂"的根源。

### 10.2 方案（用户确认方向：分区用布局组件承载，分区是通用功能）

1. **上下文打通**：画布「数据表单」等区块绑定业务对象后，进入表单设计器时带入该对象上下文——objectCode/objectName 用对象真实值，relations/actions 按对象加载并缓存传入；子表分区恢复正常。
2. **布局承载分区**：
   - 画布容器即分区：card/elCard、collapse/elCollapse 容器 → 内容分区（children 字段的 fieldBinding.fieldCode 即分区字段）；新增「关联子表」容器（仅传入 relations 时可在货架使用）→ 子表分区（relationKey/displayMode 为容器属性）。
   - pageSections 保存时从布局树派生写回 schema（流程权限面板、后端契约测试读 schema.pageSections，零改动）。
   - 无容器的散落字段派生为"默认分区"，保证流程权限粒度覆盖全部字段。
   - 容器分区按 sectionId（沿用组件 id）合并存量分区级扩展属性（visibleInModes、collapsible 等）。
3. **界面收敛**：移除「页面分区」三态视图与 PageSectionEditor 挂载（组件文件与测试保留）；表单设计器默认只有「字段布局」（+详情设置）；分区级配置（可见模式/折叠/子表显示模式）迁入对应容器的属性面板。
4. **存量兼容**：布局树中不存在任何分区容器但存量 pageSections 有内容时，保留存量分区不派生覆盖（不丢流程权限配置）；bottomBar 属表单级配置，随派生原样保留。

### 10.3 验收标准（第三轮）

1. 自由编排页面绑定对象的「数据表单」进入设计器后：字段来自对象字段目录，货架出现「关联子表」，配置子表分区不再显示失效。
2. 画布拖卡片分组并放入字段 → 保存 → 流程审批权限面板能看到对应分区且字段正确。
3. 表单设计器顶部不再出现「页面分区」视图；已有分区配置的存量表单（含预售单）打开后流程权限分区不丢失。
4. 未绑定对象的表单设计器不出现子表入口，无"关系已失效"标签。
5. lint、相关 vitest、app-center 回归（145 用例基线，2 条历史失败不计）不新增失败。

## 11. Follow-up：字段查询与应用工作台体验

用户在实际使用表单设计器和应用工作台时补充了三类体验要求，本轮在不改变页面分区承载方案的前提下增量处理：

1. 字段自动查询配置使用业务化文案；查询源明确区分「数据集」和「接口」；当前登录用户 ID、租户 ID、组织 ID、扫码内容等常用上下文可直接选择，自定义路径仅作为高级兜底；配置面板采用中性白色背景。
2. 数据集查询提供 1～100 条的分页窗口，运行时透传既有 `pageNum/pageSize/maxRows` 协议，避免字段回填一次拉取过多记录；接口查询协议不附加分页参数。
3. 应用对象的「关联已有对象 / 从数据库表导入 / 新建对象」合并为一个添加菜单；对象列表、页面入口列表和窄屏工作台导航移除固定最小宽度，内容不足时不产生横向滚动。

本轮不移除既有「布局承载分区」实现，也不改动后端查询源、数据库或权限协议。

# 应用设计器信息架构重构 — 执行清单

> 对应 spec: lowcode-app-designer-ia/spec.md
> 执行顺序：**A → B → D → C**（先止血，再解决流程可用性，最后动结构）
> 每个 Phase 独立可交付、可单独回滚，禁止跨 Phase 混合提交

---

## Phase A：布局止血（P0，纯 CSS + 按钮显隐，1 天）

**约束**：本阶段只改样式和按钮 `v-if`/`disabled`，**不新增组件、不改路由、不改协议**。

### A1 顶栏改三栏栅格，消除 Tab 条漂移

文件：`forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue`

- [ ] `.runtime-header`（L3929-3938）从 `display:flex; justify-content:space-between` 改为：
  ```
  display: grid;
  grid-template-columns: minmax(240px, 1fr) auto minmax(240px, 1fr);
  align-items: center;
  ```
- [ ] `.application-designer-tabs`（L3939-3947）**删除 `margin-inline: auto`**，改 `justify-self: center`
- [ ] 顶栏右侧 `<n-space>`（L39）外层包一层 `<div class="runtime-header-actions">`，样式 `justify-self: end; min-width: 320px; display: flex; justify-content: flex-end`
- [ ] 窄屏 `@media` 段（L5980-5993）同步适配：`grid-template-columns: 1fr`，三块纵向堆叠，Tab 条 `order: 3`

**验收**：1024 / 1280 / 1440px 三档宽度下依次点击 6 个 Tab，中间 Tab 条左边缘位置**像素级不变**。

### A2 顶栏按钮恒定可见，用 disabled 代替消失

同文件 L40-115。当前撤销/重做/页面资源/预览草稿/更多菜单均挂 `v-if="editing && designerSection === 'page'"`，
导致右区宽度在 420px ↔ 90px 间跳变。

- [ ] 撤销（L40）：`v-if` 改为 `v-if="editing"`，`:disabled="!canUndo || designerSection !== 'page'"`
- [ ] 重做（L45）：同上，`:disabled="!canRedo || designerSection !== 'page'"`
- [ ] 页面资源 popover（L50）：`v-if="editing"`，非 page 时 `:disabled="true"`（n-popover 需在 trigger 的 n-button 上加 disabled）
- [ ] 预览草稿（L97）：`v-if="editing"`，`:disabled="designerSection !== 'page'"`
- [ ] 更多菜单（L109）：`v-if="editing"`，`:disabled="designerSection !== 'page'"`
- [ ] 所有置灰按钮补 `title`，说明"仅页面设计可用"

### A3 保存草稿在所有 Tab 可见（含设置）

- [ ] 保存草稿按钮（L85-96）移除 `designerSection !== 'settings'` 条件，改 `v-if="editing"`
- [ ] 设置 Tab 下 `:disabled` 逻辑补一个分支：`designerSection === 'settings' ? true : (原有三元)`
      （设置页目前无草稿态，先置灰保位，Phase B 后如有可保存内容再放开）

### A4 表单切换条去掉蓝色实心块

文件：`forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFormDesigner.vue`

- [ ] `.designer-form-tab.active`（L1768-1771）从 `border-color:#2563eb; background:#eff6ff`
      改为 `border-bottom: 2px solid #2563eb; background: transparent; color: #2563eb`
- [ ] `.designer-form-tabs` 容器（L121）加 `v-if="formAssets.length"`：**只有一个主表单时整条不渲染**
- [ ] 主表单按钮（L122-126）里的 `<strong>当前</strong>` 标记删除（下划线已表达激活态，标记冗余）

**验收**：事件入口打开后，画布上方无大面积浅蓝色块；单表单场景下切换条完全不出现。

### A5 页面分区编辑器左右标题栏对齐

文件：`forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/PageSectionEditor.vue`

- [ ] `.workbench-heading`（L946-951）`min-height` 从 `58px` 改 `48px`
- [ ] `.settings-heading`（L1069-1072）增加 `min-height: 48px`，`padding-bottom` 从 `16px` 改为 `0`，
      改用 `border-bottom` + 外层容器统一 padding，与左侧 heading 同基线
- [ ] 两侧 heading 统一 `padding-inline: 14px`

**验收**：左侧「新增」按钮与右侧「删除分区」按钮**垂直中心线等高**。

### A6「页面分区」按钮移出图标工具组

同 `ForgeFormDesigner.vue`。当前 `页面分区`（L139-144）混在 `.page-switch-actions` 的 5 个圆形图标按钮中。

- [ ] 从 `.page-switch-actions` 移出，放到 `.designer-form-tabs` 右侧，作为独立分组
- [ ] 与图标组之间加 `1px` 竖直分隔线，视觉上区分"视图入口"与"编辑工具"

> 注：本阶段仅调位置，**弹窗改视图切换放 Phase C（C5）**，避免 A 阶段引入结构改动。

### Phase A 验证

```bash
cd forge-admin-ui && pnpm lint:fix && pnpm build 2>&1 | grep -i error
```

- [ ] 6 个 Tab 逐一切换，顶栏无跳动、无按钮闪现消失
- [ ] 1024px 窄屏无横向溢出
- [ ] 事件入口无蓝色块，分区编辑器左右对齐

---

## Phase B：入口去重（P0，2 天）

**约束**：不删除后端 section 数据，**只在前端过滤和引导**，保证可快速回滚。

### B1 业务对象操作列精简为「编辑字段 + ⋯」

文件：`forge-admin-ui/src/views/app-center/application-workspace/ApplicationObjectsPanel.vue`

当前 L75-80 四个平铺按钮。

- [ ] 删除「设计表单」（L77，`openForm`）和「业务动作」（L78，`openDesignerPanel(item,'actions')'`）
- [ ] 「数据结构」（L76）文案改为「编辑字段」，保留 `openDesigner`
- [ ] 「移除」（L79）改为收进 `n-dropdown`「⋯」，选项标红 + 保留现有二次确认
- [ ] `.object-row` grid 最后一列宽度从 `190px` 收窄至 `120px`
- [ ] 清理不再引用的 `openForm` 函数（若无其它调用点）

**验收**：操作列只有「编辑字段」和「⋯」，移除功能行为不变。

### B2 控制台隐藏已迁入设计器的 section

文件：`forge-admin-ui/src/views/app-center/application.[applicationCode].vue`

`:sections="workspace.sections"`（L22）直接来自后端，需在前端加过滤层。

- [ ] 新增常量 `const designerOwnedSections = new Set(['automation', 'enhancements'])`
- [ ] 新增 `const visibleSections = computed(() => (workspace.value?.sections || []).filter(s => !designerOwnedSections.has(s.sectionKey)))`
- [ ] 导航绑定改为 `:sections="visibleSections"`
- [ ] `validSections`（L99-107）**保留 automation / enhancements**，使旧 URL 仍可打开（见 B4）
- [ ] `permissions` section 保留，但 `ApplicationCapabilityPanel` 的 `capabilityPanel` 配置（L114-137）中
      「对象与字段权限」条目改为指向设计器，`actionLabel` 改「打开设计器」

### B3「设计页面」改名

文件：`forge-admin-ui/src/views/app-center/application-workspace/ApplicationWorkspaceHeader.vue`

- [ ] 按钮文案（L45-50）`设计页面` → `打开设计器`
- [ ] 补 `title="配置页面、表单、流程与自动化"`

### B4 旧 section URL 落地引导页

- [ ] `application.[applicationCode].vue` 中当 `activeSection` ∈ `designerOwnedSections` 时，
      内容区不渲染原面板，改渲染引导卡：
  - 标题：「业务流程已移入应用设计器」/「动作与增强已移入应用设计器」
  - 说明一句话 + 主按钮「打开设计器」（跳 `?edit=1` 并带上对应 `designerSection`）
- [ ] 跳转参数映射：`automation → business-flow`，`enhancements → actions`

**验收**：`?section=automation` 不 404、不空白，显示引导卡并可一键到达设计器对应入口。

### Phase B 验证

- [ ] 控制台左栏 5 项：概览 / 业务对象 / 页面入口 / 权限 / 发布
- [ ] `业务流程`、`动作与增强` 在控制台和设计器中**只有一处可编辑**
- [ ] 旧书签 `?section=automation`、`?section=enhancements` 有引导、可跳转

---

## Phase D：业务流程配置重构（P1，3 天）

**这是当前最影响功能可用性的一条**：用户必须手工搬运 `nodeKey` 和 `sectionId`。

### D1 取消左右分栏，改纵向三段

文件：`forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue`

- [x] 删除 `.application-flow-workbench` 的 grid 分栏（L4092-4107），改单列纵向流式
- [x] `business-flow` 分支（L143-172）调整为：
  1. **流程绑定**段：内嵌 `BusinessObjectDesignerPage`（`flow-app` 面板）压缩为一行式绑定器（流程模型 + 启动方式）
  2. **节点配置**段：新组件（见 D2）
  3. **高级**段：`ApplicationFlowInteractionPanel` 的审批按钮/时间轴/回调，`n-collapse` 默认折叠
- [x] 移除该分支顶部的 `.application-object-context`（对象由绑定段自身表达）

### D2 节点列表从流程模型自动解析

文件：`forge-admin-ui/src/views/app-center/components/ApplicationFlowInteractionPanel.vue`

当前 L58-72 要求手填 `nodeKey`（"如 managerApproval"）。

- [x] **复用现成接口，无需后端改动**：`businessFlowVariables(modelKey, { objectCode })`
      → `GET /ai/business/flow/model/{modelKey}/variables`（`@/api/business-app.js` L523-525）
      返回体中 `userTasks` 数组即节点列表，由后端 `BusinessFlowVariableResolver`（L66/L76）解析 BPMN XML 得出，
      单项字段：`taskDefKey`（即 nodeKey）/ `taskName` / `formKey` / `assignee` / `candidateUsers` / `candidateGroups`
- [x] 规范化逻辑直接抄 `BusinessFlowBindingPanel.vue` L717 调用 + L772-782 `normalizeUserTasks`，两处保持一致
- [x] `taskDefKey` → `nodeKey`，`taskName` → 表格展示名
- [x] `nodePermissions` 从「手动增删行」改为「按解析出的节点渲染固定表格」
- [x] 表格列：节点名 / 办理人（只读展示）/ 可见分区（计数）/ 只读分区（计数）
- [x] 点击行展开配置区
- [x] 空态兜底：流程模型未选或解析失败时，**回退到当前手填模式**，不阻塞配置

### D3 分区选择改下拉多选

- [x] `visibleSectionIds` / `readonlySectionIds`（L67、L70）从 `n-dynamic-tags` 改 `n-select` multiple
- [x] 选项来源：当前对象 `formDesignerSchema.pageSections`，`label` 用分区标题、`value` 用 `sectionId`
- [x] 已配置但在 pageSections 中不存在的 sectionId：标红显示「已失效」，不静默丢弃
- [x] **不新增任何"全部"选项**。H5 运行时 `resolveVisiblePageSections`（`forge-h5-ui/src/utils/lowcode-runtime.js` L207）
      判定条件是 `!visibleSectionIds.size || visibleSectionIds.has(...)`，即**空数组已经等于"该节点可见全部分区"**。
      UI 只需在下拉框下方加一行灰字说明：`不选表示该节点可见全部分区`
- [x] 只读分区同理：`isPageSectionReadonly`（L211-214）只认命中项，空数组 = 全部可编辑，
      说明文案：`不选表示该节点下所有分区均可编辑`

### D4 高级项折叠

- [x] 审批操作（L10-32）、审批时间轴（L34-45）、完成回调（L77+）三段包进 `n-collapse`
- [x] 已有配置的段落标题带圆点标记，避免折叠后用户不知道里面有内容

### Phase D 验证

- [x] **不查看任何其它界面、不手打任何标识符**，完成"财务复核节点隐藏付款信息分区"的配置
- [ ] 保存后 H5 以财务身份打开待办，付款分区不可见
- [ ] 未配置流程交互的历史应用行为不变（回归 Phase 4.5 结论）
- [x] 流程模型未绑定时，节点配置段显示空态引导而非报错

---

## Phase C：页面资源树（P1，4 天）

**改动面最大，必须在 A/B/D 完成后进行。**

### C1 左栏从平铺 Tab 改分组资源树

文件：`forge-admin-ui/src/views/app-center/application-designer-navigation.js`

- [x] `applicationDesignerSections`（L1-8）从 6 个平铺项改为分组结构：
  ```
  页面（pages）   / 数据（data）/ 自动化（automation）
  流程（flow）    / 设置（settings）
  ```
- [x] 保留 `normalizeApplicationDesignerSection`，增加**旧 key → 新节点映射表**：
  `page→pages`、`events→pages`（并定位到表单页节点）、`actions→automation`、
  `business-flow→flow`、`data-model→data`、`settings→settings`
- [x] `objectDesignerSectionConfig`（L10-28）随新 key 重写

文件：`application-runtime.[applicationCode].vue`

- [x] 顶栏 `.application-designer-tabs`（L25-38）的横向 Tab 移入左栏树组件
- [x] 新建左栏树组件（分组标题 + 可折叠 + 节点激活态用左边框，非蓝色填充）

### C2 页面节点显式化

**详情页不作为独立节点**（依据见下方说明），页面分组下每个对象最多两个节点。

- [x] 页面分组下按对象列出节点：`{对象名}（表单页）`、`{对象名}（列表页）`
- [x] 节点判定依据：
  - 表单页：`draft.formDesignerSchema` 存在且 `fields`/`pageSections` 非空
  - 列表页：`draft.viewSchema` 中列表 zone 存在且 `enabled !== false`
  - 两者都无配置时仍列出节点，但标灰 + 角标「未配置」，点击进入即可开始配置（不要因为没配置就藏起来，否则用户找不到入口）
- [x] 自定义编排画布作为独立节点「工作台首页（自由编排）」
- [x] 点击表单页节点 → 右侧直接加载该对象的表单设计器（等价于旧 `events` Tab 的 `initialPanel: 'form'`）

**为什么不要「详情页」节点**（代码事实）：

- `BusinessDetailDesigner.vue` L600 `detailSource: 'FORM_LAYOUT'` 是**写死的常量**，L582-591 主信息分组固定
  取自 `formFieldRefs` 且 `readonly: true` —— 详情主信息完全由表单布局推导，不存在第二套字段配置
- 组件自述（L6、L27）："详情主信息自动复用表单设计布局"、"不再单独维护第二套字段分组"
- H5 侧无独立详情组件，`PageSectionRenderer.vue` L15 仅 `:readonly="mode === 'detail' || ..."`，
  即详情就是表单的只读态
- 详情页配置实际只承载 4 项附加能力：`relationTabs`、`showOperationLog`、`showApprovalLog`、`quantityPanels`（L602-615）
- 且当前 `BusinessDetailDesigner` 在 `object-designer.[objectCode].vue` 中挂了**两处**
  （L173 default-view「详情模板」+ L214 form「详情设置」），绑同一份 `draft.pageSchema` / `draft.viewSchema`
  —— 又一个违反"同一配置只允许一个入口"红线的重复入口

- [x] 上述 4 项附加能力并入表单页设计的第三个视图（见 C5），命名「详情设置」
- [x] 删除 `object-designer.[objectCode].vue` L172-186 default-view 下的「详情模板」tab（保留 L213-227 那处）

### C3 删除全局「当前数据对象」选择器

- [x] 移除 `.application-object-context`（L144-152、L174-182 两处）及其样式（L4073-4091）
- [x] `activeDesignerObject` 改由**选中的树节点**推导，不再由独立下拉维护
- [x] `selectDesignerObject` 调用点全部改为节点选中逻辑

### C4 取消「事件」Tab

- [x] 从导航中删除 `events` key（能力已由 C2 的表单页节点承接）
- [x] 事件配置保留在 `ForgePropertyPanel` 的「事件」Tab（Phase 2.3 已建）
- [x] **该 Tab 在已有 fieldEvents 配置时显示红点标记**，防止用户找不到既有配置
- [x] 旧 `?designerSection=events` 映射到"页面 → 第一个表单页节点"

### C5「页面分区」「详情设置」从弹窗/异地 Tab 改画布视图切换

文件：`ForgeFormDesigner.vue`

- [x] 删除 `pageSectionDialogVisible` 的 `n-modal`（L213-232）
- [x] 画布左上角增加 segment：`[ 表单布局 | 页面分区 | 详情设置 ]`
- [x] 选中「页面分区」时，画布区域渲染 `PageSectionEditor`（复用组件，去掉 modal 外壳）
- [x] 选中「详情设置」时，画布区域渲染 `BusinessDetailDesigner` 中的附加能力部分
      （关系页签 / 操作日志 / 审批记录 / 数量区块），**主信息预览区去掉**——它只是表单布局的镜像，无配置价值
- [x] 三个视图**共享同一份 schema 和 dirty 状态**，切换不丢草稿
- [x] `openPageSectionEditor` 及相关 modal 状态变量清理

### Phase C 验证

- [x] 左栏树能直接看到「预售登记（表单页）」，点击即进入表单设计
- [x] 无「当前数据对象」下拉，对象上下文由节点表达
- [x] 全部旧 `designerSection` 值可正常映射，无 404、无空白
- [x] 分区与字段布局在同一画布内切换，草稿不丢

---

## 交叉验证（每个 Phase 完成后执行）

### 编译与静态检查

```bash
cd forge-admin-ui && pnpm lint:fix && pnpm build 2>&1 | grep -i error
```

### 视口回归（必须覆盖 1024px）

- [ ] 1024 / 1280 / 1440px 三档，设计器全部入口无横向溢出、无元素错位
- [ ] 顶栏按钮位置在切换入口时零漂移

### 兼容回归（防止破坏 Phase 1-6 成果）

- [ ] 预售登记 H5：6 分区 + pill 选择 + 底部提交栏正常
- [ ] 不带 pageSections 的历史应用渲染不变
- [ ] `flow_action` 底部按钮、审批时间轴、节点分区策略行为不变
- [ ] 按钮权限（`permissionKey` / `permissionStrategy`）生效逻辑不变
- [ ] 种子配置接管提示仍正常弹出

### 端到端验收剧本（Phase C 完成后整体跑一次）

按 spec 第六章 7 步执行，**任一步需跳出界面查标识符或找不到入口即失败**：

- [ ] 1. 控制台 → 打开设计器
- [ ] 2. 页面树看到「预售登记（表单页）」→ 点开
- [ ] 3. 切「页面分区」→ 调整顺序和标题
- [ ] 4. 切「表单布局」→ 选字段 → 配置事件联动
- [ ] 5. 流程 → 节点表格自动列出 → 下拉取消勾选付款分区
- [ ] 6. 保存草稿 → 预览 → 手机端验证
- [ ] 7. 发布 → 控制台出现新版本

---

## 回滚方案

| Phase | 回滚方式 | 附加动作 |
|-------|---------|---------|
| A | `git revert` | 无，纯样式 |
| B | `git revert` | 无，前端过滤层删除即恢复 |
| D | `git revert` | 若 D2 涉及新后端接口，接口可保留不影响 |
| C | `git revert` | **需同步恢复 `application-designer-navigation.js` 旧 6 Tab 定义** |

---

## 依赖确认结论（已闭环，开工无阻塞）

| 原待确认项 | 结论 | 依据 |
|-----------|------|------|
| D2 BPMN 节点接口 | **已有，无需后端改动**。用 `businessFlowVariables(modelKey,{objectCode})` 返回的 `userTasks` | `api/business-app.js` L523；`BusinessFlowVariableResolver` L66/L76；`BusinessFlowBindingPanel.vue` L717 已在用 |
| D3「全部分区」语义 | **概念删除**。空数组已经等于全部可见，只加说明文案 | `lowcode-runtime.js` L207 / L211-214 |
| C2 详情页节点判定 | **不设详情页节点**。主信息是表单只读态，附加能力并入表单页第三视图 | `BusinessDetailDesigner.vue` L6/L27/L600/L582-591；`PageSectionRenderer.vue` L15 |

---

# Phase E：A/B/C/D 落地后的回归修正

> 触发来源：Phase C 上线后实际使用反馈（4 条）+ 代码复核额外发现 1 条
> 定位基准 URL：`/app-center/application/PRESALE_REGISTRATION_APP/runtime?edit=1&designResource=data:xxx`
> **E1 与 E5 为 P0**：E5 是功能回退（能力不可达），E1 是主工作区被挤压
> 2026-08-15 状态：必需代码与自动化验证已完成；保留的未勾选项仅为可选体验补强或依赖真实数据库/H5 环境的人工联调。

---

## E1 消除中间那层重复导航（P0）

### 现象
左栏已是资源树，中间工作区又出现一条竖直导航 + 「单据闭环配置」进度块，画布被挤窄，设计表单时尤其明显。

### 根因（三层嵌套）
```
ApplicationDesignerResourceTree      ← 新资源树（左栏，L144-150）
  └─ BusinessObjectDesignerPage(embedded)
       └─ BusinessObjectDesignerShell
            ├─ .designer-nav          ← 又一条导航（L83-130）★ 多余
            │    ├─ 「导航」折叠按钮   （L84-95）
            │    ├─ filteredNavItems  （L96-111）内嵌时通常只剩 1 项
            │    └─ .closure-steps    （L113-129）单据闭环配置 ★ 多余
            └─ .designer-main          ← 真正的画布
```

两处漏了内嵌判断：

- `BusinessObjectDesignerShell.vue` L452-454 只在 `.compact-embedded` 时 `display:none`，
  而 `compact-embedded` 仅当 `activePanel === 'flow-app'` 成立 → **表单页/列表页/数据/自动化节点下 `.designer-nav` 全部照常显示**
- L113 `v-if="closureSteps.length"` **完全没有 embedded 条件**；
  `object-designer.[objectCode].vue` L15 无条件传 `:closure-steps="closureSteps"`，
  L469-471 只在 `isCodeAppDesigner` 时才返回空数组

内嵌时 `navPanels` 已被限定为单项（如 `page-form → ['form']`，navigation.js L40-44），
即这条 56px+ 宽的导航栏只为渲染 1 个不可切换的按钮。

### 改动
文件：`forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue`

- [x] `.designer-nav`（L83）增加渲染条件：`v-if="!embedded || filteredNavItems.length > 1"`
      —— 内嵌且只有一个面板时整条不渲染
- [x] `.designer-workbench`（L82）在上述条件不成立时用单列栅格：
      新增 `.designer-shell.embedded.nav-hidden .designer-workbench { grid-template-columns: minmax(0, 1fr); }`
- [x] 「单据闭环配置」块（L113-129）条件改为 `v-if="!embedded && closureSteps.length"`
      —— 闭环进度属于对象独立设计态的引导，应用设计器内已有资源树表达同样信息，不重复
- [x] 保留 `.compact-embedded` 现有规则，不要删（flow-app 仍依赖）

### 验收
- [x] 左栏选「预售登记（表单页）」，工作区从画布直接开始，**中间无任何竖直导航条、无闭环进度块**
- [x] 1280px 下表单画布可用宽度比修改前增加 ≥ 180px
- [x] 独立打开对象设计器（非内嵌，`/app-center/object-designer/xxx`）时，导航和闭环配置**照常显示**
- [x] 流程节点（flow-app）布局不回退

---

## E2 自由编排页面节点下出现两个左侧栏（P0，代码复核发现，用户未报）

### 根因
- `ApplicationDesignerResourceTree` 渲染条件是 `editing && !formDesignerMode`（L145），**不含** `pageBuilderResourceActive`
- 设计器工作区 `<section>` 条件是 `editing && !formDesignerMode && !pageBuilderResourceActive`（L153）
- 选中 `page-custom` 节点时 `pageBuilderResourceActive === true` → 走 L260 的 `v-else` 分支，
  该分支内部又有一个完整的 `<aside class="runtime-navigation">`（L261-342）

结果：资源树（左）+ runtime-navigation（紧邻其右）**同屏并存**，两套页面列表。

### 改动
文件：`application-runtime.[applicationCode].vue`

- [x] 选定唯一事实来源：**保留资源树，隐藏 `runtime-navigation`**（编辑态下）
- [x] `<aside class="runtime-navigation">`（L261）增加条件：`v-if="!editing"`
      —— 运行态（非编辑）仍需要它做页面切换，编辑态由资源树承担
- [x] `runtime-navigation` 内的「+新建」能力迁移到资源树（见 E3），迁移完成后才可隐藏
- [x] 检查 `.runtime-body` 的 grid 列定义（`designer-resource-active` class 相关），
      隐藏一栏后不能留空白列

### 验收
- [x] 编辑态选中自由编排页面节点，屏幕上**只有一个**页面列表
- [x] 退出编辑（运行态）后侧栏页面切换正常

---

## E3 资源树「页面」分组支持新建页面（P0，配合 E2）

### 现状
新建能力**已存在但被挡住**：`.new_node_wrapper`（L314-332）里的「+新建」popover
提供 `createQuickNode('page')` / `createQuickNode('group')`，
但它长在 L260 的 `v-else` 分支中，进入任何对象类节点后整块不渲染。
`ApplicationDesignerResourceTree.vue` 自身无任何新增入口。

资源树的自定义页面来源是 `buildApplicationDesignerResourceGroups` 的 `options.pages`
并且 `.filter(page => page?.type === 'page')`（navigation.js L74）—— 只读消费，不产出。

### 改动
文件：`ApplicationDesignerResourceTree.vue` + `application-runtime.[applicationCode].vue`

- [x] 「页面」分组标题右侧增加 `+` 按钮，emit `create-page` 事件
- [x] 父组件复用**现有** `createQuickNode('page')` 逻辑响应该事件，不要另写一套创建流程
- [x] 新建后自动选中新节点并进入编排画布（`designResource=page-custom:{newId}`）
- [x] 「页面组」创建能力同样迁移（分组用于归类，`type === 'group'` 的节点当前被 L74 过滤掉了，
      需要决定是否在树里显示层级 —— 若暂不支持，`+` 菜单里先只放「新建页面」，避免建了看不见）
- [x] 表单页/列表页节点**不提供新建**（它们由业务对象派生，新增对象走控制台「业务对象」）

### 验收
- [x] 设计器内可新建空白页面，无需退出到运行态
- [x] 新建的页面立刻出现在「页面」分组并被选中

---

## E4 页面入口能指向具体页面（P1）

### 现状
入口字段**已具备**指向能力，但选择器被限制了作用域：

- `AppEntryWizard.vue` L311-319 等处，入口有 `runtimeOpenMode`（LIST / CREATE_FORM / DETAIL）
  + `targetPageKey`（默认 `'list'`）+ `targetFormKey`
- 「目标页面」下拉（L80-91）条件是 `sceneKey === 'DATA_MANAGE' && runtimePageOptions.length > 1`
- `MOBILE` 场景（L378-396）默认 `runtimeOpenMode: 'LIST'`、`targetPageKey: 'list'`，
  **但因 sceneKey 不是 DATA_MANAGE，目标页面下拉根本不渲染** → 想把"预售单列表页"挂到 H5 只能吃默认值

### 改动
文件：`forge-admin-ui/src/views/app-center/components/AppEntryWizard.vue`

- [x] 「目标页面」下拉的 `v-if` 从 `sceneKey === 'DATA_MANAGE'` 放开为
      `['RUNTIME', 'H5'].includes(form.entryMode) && runtimePageOptions.length > 1`
      —— 覆盖 MOBILE / FORM_SUBMIT 等同样跑运行时渲染的场景
- [x] `runtimePageOptions` 的选项来源补齐：除对象派生的 `list` / 表单页外，
      把应用的自由编排页面（`page-custom`）也纳入，`value` 用其 pageKey/pageId
- [x] 「目标表单」下拉（L92-99）同步放开到 `CREATE_FORM` / `DETAIL` 的 `runtimeOpenMode`
- [x] 选中目标页面后，在向导上显示一行预览路径，让用户确认落到哪个页面

### 反向入口（体验补强，可选）
- [ ] 资源树页面节点的 `⋯` 菜单增加「挂载为入口」，
      跳转入口向导并预填 `objectCode` + `runtimeOpenMode` + `targetPageKey`
- [ ] 已被入口引用的页面节点显示小徽标「已挂载」，避免用户不知道哪个页面对外可见

### 验收
- [x] 新建 MOBILE 入口时可显式选择「预售单列表页」
- [ ] H5 打开该入口，落地页是所选列表页而非默认页
- [x] 历史入口（未设 `targetPageKey`）行为不变

---

## E5 恢复「动作与增强」入口 —— 功能回退（P0，最高优先）

### 现象
JS 增强 / CSS 增强 / Java 服务增强 / 业务规则**整体不可达**。

### 根因（Phase B4 方案缺陷，判断错误在方案侧）
把"业务动作"和"增强"当成了同一个能力，实际是两套独立体系：

| | 业务动作 | 动作增强 |
|---|---|---|
| 载体 | `designerOptions.actions` | `ai_business_extension` 独立表 |
| 界面 | object-designer 的 `actions` 面板 | `ApplicationExtensionsPanel.vue` |
| API | 对象设计器保存 | `businessExtensionPage` / `businessExtensionDetail` / `businessExtensionServerHandlers` |
| 内容 | 按钮、操作项 | 业务规则、页面 JS、页面 CSS、Java 服务增强（`ai_business_extension_type` 字典） |

断链路径：
1. `application.[applicationCode].vue` L132-136 把 `enhancements` 标进 `designerOwnedSectionMeta`，
   `designerSection` 填的是 `'actions'`
2. L144-145 `visibleSections` 过滤掉 `enhancements` → 控制台左栏看不到
3. L29 引导卡 `v-if="designerOwnedGuide"` **优先于** L44 的真实面板 →
   即使手敲 `?section=enhancements` 也只能看到引导卡，`ApplicationExtensionsPanel`（L179 仍注册）永远渲染不到
4. 引导卡跳设计器，`legacySectionMap['actions'] = 'automation'`（navigation.js L13）
   → 落到 `automation-actions` 节点 → `initialPanel: 'actions'` → object-designer 的**业务动作**面板
5. 该面板里没有 JS/CSS/Java 增强 → **用户被引导到一个不存在该能力的地方，原入口已被关闭**

### 改动（二选一，推荐方案 1）

**方案 1（推荐）：增强作为应用级节点进设计器**

增强的 `scopeType` 支持对象和入口两种作用域，本质是应用级资产，不该挂在单个对象下。

- [x] `application-designer-navigation.js` 的 `automation` 分组增加一个**应用级节点**（不随对象循环）：
      `{ key: 'automation-enhancements', groupKey: 'automation', kind: 'automation-enhancements', label: '动作增强', configured: <有增强记录时 true> }`
- [x] `objectDesignerSectionConfig` 不适用于该节点（它不走 object-designer），
      在 `application-runtime.[applicationCode].vue` 工作区增加分支：
      `kind === 'automation-enhancements'` → 直接渲染 `ApplicationExtensionsPanel`
- [x] `ApplicationExtensionsPanel` 需接受 `application` prop 并在内嵌态隐藏自身页头（L22 附近的 header），
      避免与设计器顶栏重复
- [x] 修正 `designerOwnedSectionMeta.enhancements.designerSection`：
      从 `'actions'` 改为指向新节点的 resource key，描述文案里的「动作与增强」改为「动作增强」，与节点名一致

**方案 2（保底，未采用）**
- 不从 `designerOwnedSectionMeta` 中移除 `enhancements`；方案 1 已落地并恢复真实能力入口。
- 该方案会违反"同一配置只允许一个入口"红线，仅在方案 1 无法实施时使用。

### 验收
- [ ] 能创建一条页面 JS 增强并保存、启用、查看版本
- [ ] 已有增强记录在界面上可见（**先确认历史数据未被误删**）
- [x] `?section=enhancements` 旧书签不再落死胡同
- [x] 引导卡的「打开设计器」确实到达能配置增强的位置

> **开工前先查**：确认这段时间内是否有增强记录因入口不可达而未被维护，
> 以及 `ai_business_extension` 表数据是否完好。

---

## E6 自动化/流程节点按真实配置生成（P1）

### 现象
每个业务对象都无条件出现「触发器」「业务动作」「审批」节点，且全部显示为已配置状态，
5 个对象 = 10 个自动化节点 + 5 个流程节点，真正配过的淹没在噪音里。

### 根因
`application-designer-navigation.js` `buildApplicationDesignerResourceGroups` 中
只有「页面」分组做了真实判定（L83-84 用 `formConfigured` / `listConfigured`，判定函数 L210-228），
其余三组的 `configured` 参数是**硬编码 `true`**：

- L99 `data` 分组：`createObjectNode(item, 'data-object', item.objectName, true)`
- L105-106 `automation` 分组：触发器、业务动作均传 `true`
- L112 `flow` 分组：审批节点传 `true`

### 改动
- [x] `normalizeObjectResource`（L172-184）扩展返回字段，从 `designersByObjectId` 的摘要中解析：
      `triggerConfigured`（有触发器定义）、`actionConfigured`（`designerOptions.actions` 非空）、
      `flowConfigured`（已绑定流程模型 / `documentConfig.mainFlowSummary` 有 flowModelKey）
- [x] L105-106、L112 改为传入对应的真实标记
- [x] `data` 分组保持 `true`（对象一定有字段，恒为已配置，语义正确）
- [x] **节点仍然全部列出，不做隐藏**，只用「未配置」标灰区分
      —— 与 C2 已确立的原则一致：藏起来会导致用户找不到配置入口
- [x] 分组标题右侧显示 `已配置数/总数`，让用户一眼看出哪组有内容
- [x] 增加分组级折叠，默认**只展开含已配置节点的分组**

> 若确认某类节点在"未配置"时确实无任何配置价值（例如对象未绑流程时审批节点点进去是空壳），
> 可在该节点点击后显示引导态而非空面板，但**不要从树里移除**。

### 验收
- [x] 未配触发器的对象，其触发器节点显示灰色「未配置」
- [x] 已配流程的对象，审批节点为正常态
- [x] 分组标题计数与实际相符
- [x] 5 对象场景下首屏节点数明显收敛，仍可通过展开访问全部节点

---

## Phase E 执行顺序与验证

建议顺序：**E5 → E1 → E2 → E6 → E3 → E4**
（先补回丢失的能力，再解决工作区挤压，最后做增量能力）

```bash
cd forge-admin-ui && pnpm lint:fix && pnpm build 2>&1 | grep -i error
```

### 回归底线（每项都不能破）
- [x] 独立对象设计器（非内嵌）的导航、闭环配置、全部面板行为不变
- [x] 运行态（非编辑）侧栏页面切换不变
- [x] 流程节点（flow-app）的 compact-embedded 布局不回退
- [x] 历史入口未设 `targetPageKey` 时行为不变
- [x] 旧 URL（`?designerSection=events` 等）映射仍生效

### 方案侧复盘（写给我自己）
E5 是 Phase B4 的方案错误，不是实现走偏：我在 spec 里把 `enhancements` 直接判给了
"动作与增强已移入设计器"，却没有核实设计器的 `actions` 面板与 `ApplicationExtensionsPanel`
是否同一能力。**后续任何"入口去重"结论，必须先比对两侧的 API 和数据表，确认是同一份数据再合并。**

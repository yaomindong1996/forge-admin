# 执行日志 — lowcode-designer-unification

> 按 `code-copilot/rules/automated-testing-standard.md` 记录，每轮追加，不重写已有记录。

---

## 2026-09-05 · P2 提前落地轮（统一面板接入 + 嵌套放开 + spec 修正）

### 变更范围

本轮目标：把 P1 已建好的 designer-core 注册表（105 组件 spec）真正接到两个设计器 UI 上，解决用户反馈的三大痛点（组件面板不统一 / 属性不全 / 嵌套不好使）。

| 文件 | 变更 |
|------|------|
| `forge-admin-ui/src/components/lowcode-builder/page/ListPageGridDesigner.vue` | 左侧面板替换 UnifiedComponentPalette（补 5 个 script 成员 + imports）；删旧 palette 死代码约 190 行（groupedBlocks/resolvePaletteItemIcon/paletteStats/isBlockDisabled/resolveBlockDisabledReason/handlePaletteDragStart/handlePaletteClick + 18 个 unused icon imports）；appendGridCellChild 补嵌套深度保护 |
| `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgeFieldShelf.vue` | 组件库 Tab 接入 UnifiedComponentPalette（scope=F）；删手写 fieldComponentIcons（41 图标）+ layoutItems + 死样式约 260 行；双链路拖拽（template/layout MIME 分流）；FORM_COMPONENT_KEY_OVERRIDES 键名映射 |
| `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue` | SPEC_PANEL_EXCLUDED_PROPS 补 'columns'（row 面板已有手写栅格总列数）；lint 清零 |
| `forge-admin-ui/src/components/lowcode-builder/designer-core/panel/UnifiedComponentPalette.vue` | 加 itemDragEnd emit + @dragend |
| `forge-admin-ui/src/components/lowcode-builder/designer-core/spec/zone-action-components.js` | action-button aliases ['button']、scope F+L（物料清单 L181 合并规则） |
| `forge-admin-ui/src/components/lowcode-builder/designer-core/spec/widget-components.js` | 17 个挂件 scope L→F+L |
| `forge-admin-ui/src/components/lowcode-builder/designer-core/spec/media-components.js` | barcode/qrcode scope L→F+L |
| `forge-admin-ui/src/components/lowcode-builder/designer-core/spec/bridge.js` | 新增导出 FORM_COMPONENT_KEY_OVERRIDES（grid→row、groupTitle→title、formSectionTitle→AiFormSectionTitle、action-button→button） |
| `forge-admin-ui/src/components/lowcode-builder/designer-core/index.js` | 导出 FORM_COMPONENT_KEY_OVERRIDES |
| `forge-admin-ui/src/components/lowcode-builder/designer-core/__tests__/bridge.test.js` | 追加 4 个表单侧红线测试（映射/button alias/scope F 可见性/列表 59 项不破坏） |
| `forge-admin-ui/src/components/lowcode-builder/page/page-schema.js` | canvasComponentCatalog 桥接化（`...toCanvasComponentCatalog()` + field-* 包装，用户先行修改，本轮验证） |

### 执行命令与结果

```bash
# 1. ESLint 修复与验证（10 个改动文件）
npx eslint <10 个文件> --fix        # --fix 修复 46 个 error（import 排序/数组换行/describe 小写）
npx eslint <9 个文件>               # 零输出 = 0 error 0 warning
# spec/bridge.js 剩 10 个 JSDoc warning（require-returns-description/check-types）为历史遗留，非本轮引入，不阻断

# 2. 相关范围回归
npx vitest run src/components/lowcode-builder src/views/app-center/components/designer
# → Test Files 25 passed (25)，Tests 201 passed (201)   ✅

# 3. 全量回归
npx vitest run
# → Test Files 138 passed / 1 failed；Tests 1012 passed / 2 failed
# 失败项：src/views/app-center/__tests__/application-designer-phase-e-contract.spec.js
# 根因（存量，非本轮）：断言期望 @create-page="createQuickNode('page')"，目标文件
#   application-runtime.[applicationCode].vue L139 实际已演进为 @create-page="openPageTypeSelector()"；
#   测试文件与目标文件均与本变更无 git diff（与 HEAD 一致），测试结果在 HEAD 与工作区必然一致

# 4. 生产构建（UI 变更必跑）
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
# → ✓ built in 55.80s   ✅（INEFFECTIVE_DYNAMIC_IMPORT 2 条为历史 warning，非本轮引入）
```

### 跳过项与原因

- 浏览器视觉验证：本轮未启动 Vite dev server 做点击/截图验证（改动画布交互为后续 P3 范畴；本轮以红线测试 + 构建作为最低验证门槛）。建议用户在 `pnpm dev` 下人工验收三个场景：① 列表/表单左侧面板分组一致；② 选中栅格卡片右侧出现「总列数」等 spec 属性；③ 卡片/标签/栅格/盒子可拖入格子且格子内不可再放容器。

### 结论

- P1 收口 + P2 核心三件套（统一面板 / 属性引擎 / 嵌套放开）落地，相关范围 201 测试全过、构建通过。
- 本轮未启动任何后端服务、未触碰数据库。

---

## 2026-09-05 · P2.5 痛点收口轮（B1/B2 合集同显 + C1/C2/C3 属性面板治理）

### 变更范围

本轮目标：收口用户四项反馈——①两侧组件数量不一致 → 合集同显；②不支持画布的组件直接消失 → 禁用态同显；③spec 属性改了不生效（grid key 与渲染器不对齐）→ key 对齐；④属性面板布局乱 → 两列紧凑网格重设计。（A 系列栅格 span 修复在上轮已完成并有 5 个测试护栏，本轮未触碰。）

| 文件 | 变更 |
|------|------|
| `designer-core/spec/bridge.js` | 新增统一合集导出：`isPaletteUnionSpec`（非包装节点合集过滤）、`PALETTE_ZONE_ONLY_TYPES`（6 个旧 zone 画布专属）、`LIST_CANVAS_PENDING_TYPES`（table/collapse/formSectionTitle 待流式画布）；LIST_HIDDEN_TYPES/CANVAS_ZONE_TYPES 重构为引用常量（单一事实源）；9 处 JSDoc @returns 描述补全 |
| `designer-core/index.js` | 补导出 isPaletteUnionSpec / PALETTE_ZONE_ONLY_TYPES / LIST_CANVAS_PENDING_TYPES |
| `designer-core/panel/UnifiedComponentPalette.vue` | scope 支持 'ALL'（合集模式，groupComponents 不再按 scope 过滤）；handleClick 拦截禁用项点击（与拖拽同口径）；禁用态样式增强（灰底 + title/desc/icon 分层透明度 + 原因徽标胶囊） |
| `designer-core/panel/SpecPropertyPanel.vue` | C3 布局重设计：单列 n-form → 两列紧凑网格（CSS Grid repeat(2,1fr)）；布尔开关内联一行（label+switch，不再占两行）；json/textarea 占满整行；section 分组标题占满整行；属性 desc 有值时 label 旁问号 tooltip；修复 placeholder 显示 "undefined" 的缺陷；全部控件补 :disabled 透传 |
| `designer-core/spec/layout-components.js` | C1 key 对齐：grid spec 的 minChildHeight→cellMinHeight、verticalAlign→alignItems、horizontalAlign→justifyItems、showBorder→showCellBorder、showBackground→cellBackground，对齐值 top/middle/bottom→stretch/start/center/end；box spec 的 direction 枚举 horizontal/vertical→row/column（flexDirection 有效值）、gap 默认 8→12、wrap 默认 false→true（对齐渲染器 `wrap === false ? 'nowrap' : 'wrap'`）、删除渲染器不消费的 showDivider |
| `forge-form-designer/ForgeFieldShelf.vue` | B1/B2 表单侧接入：scope F→ALL、itemFilter→isPaletteUnionSpec、原 formShelfItemFilter 白名单反转为 formShelfItemDisabledReason（subTable 无关系→「需先配置对象关系」、zone 专属→「页面画布专属」、其余→「表单画布暂不支持」） |
| `forge-form-designer/ForgePropertyPanel.vue` | 表单侧 SPEC_PANEL_EXCLUDED_PROPS 补 6 个 grid 列表画布专属 key（rowGap/alignItems/justifyItems/cellMinHeight/cellBackground/showCellBorder）——表单画布 n-grid 不消费，避免无效属性入口 |
| `page/ListPageGridDesigner.vue` | B1/B2 列表侧接入：scope L→ALL、itemFilter→isPaletteUnionSpec、unifiedPaletteDisabledReason 扩展（field→「由表单区块承载」、LIST_CANVAS_PENDING_TYPES→「待流式画布支持」、PALETTE_ZONE_ONLY_TYPES→「页面画布专属」、其余不在 59 项目录→「表单画布专属」）；C2：SPEC_PANEL_EXCLUDED_PROPS['grid-layout'] 补 'columns'（手写面板已有总列数，避免重复入口） |
| `designer-core/__tests__/bridge.test.js` | 追加 4 个 B1/B2 红线测试：合集 101 项、合集覆盖双侧专属组件、PALETTE_ZONE_ONLY_TYPES/LIST_CANVAS_PENDING_TYPES 与列表目录互斥（常量与 Legacy 字面量同步） |
| `designer-core/__tests__/spec-consumption.test.js` | 新增（C1 红线）：grid propsSchema key 与 GridBlockRenderer 消费 key 精确相等、对齐枚举值 stretch/start/center/end、box direction 有效值 row/column、废弃 key 不得回流 |
| `designer-core/__tests__/spec-property-panel.spec.js` | 新增（C3 组件测试 7 个）：boolean 内联开关行、json 占满整行、section 分组标题、excludeKeys 排除、update:prop 事件签名、空态、default 回退 |

### 执行命令与结果

```bash
# 1. ESLint（11 个改动文件）
npx eslint <11 个文件> --fix    # 修复 143 个可自动修复 error（list-newline/object-curly-spacing 等，designer-core 为工作区新增文件，全部属于本轮变更范围）
npx eslint <11 个文件>          # 零输出 = 0 error 0 warning（含 bridge.js 历史遗留 JSDoc warning 一并清零）

# 2. 相关范围回归
npx vitest run src/components/lowcode-builder src/views/app-center/components/designer src/components/ai-form
# → Test Files 38 passed (38)，Tests 282 passed (282)   ✅
# 本轮新增 14 个测试：bridge 4（B1/B2 合集红线）+ spec-consumption 3（C1 key 对齐红线）+ spec-property-panel 7（C3 布局）
# 存量红线未破坏：toListPageBlockCatalog 59 项 / toFieldPaletteGroups 33 项 / registry 105 项

# 3. 存量行为说明
# box-layout 手写面板 directionOptions 仍含无效值 'vertical'（flexDirection 不识别，选中实际渲染横向）——
# 存量数据兼容问题，未在本轮修改；spec 侧已按 row/column 对齐，后续流式画布改造时统一收口。
```

### 跳过项与原因

- 浏览器视觉验证：以 282 个测试 + lint 清零作为验证门槛；建议用户 `pnpm dev` 人工验收四个场景：① 两侧左侧面板均显示 101 个组件且分组一致；② 不支持当前画布的组件变灰且显示原因徽标、拖拽无效；③ 列表侧选中栅格改「垂直位置/格子背景」等属性后画布即时生效；④ 右侧 spec 属性面板呈两列紧凑布局、布尔开关单行。

### 结论

- 用户四项反馈全部收口：合集同显（101 个、两侧一致、注册表单一事实源）/ 画布能力禁用态 / grid spec key 与渲染器对齐 / 属性面板两列紧凑重设计。
- A 系列栅格 span 语义（上轮）+ 本轮 B/C 系列，低代码设计器统一改造的 P2.5 阶段完成。
- 本轮未启动任何后端服务、未触碰数据库、未新增第三方依赖。

---

## 2026-09-05 · P2.6 用户验收反馈修正轮（Slider 预览崩溃 + keyed diff 崩溃 + B2/C3 回撤）

### 变更范围

本轮目标：收口用户验收反馈的四个问题——① 预览时 `Slider.mjs:98 Cannot read properties of null (reading 'map')`；② 预览/渲染时 `isSameVNodeType: Cannot read properties of null (reading 'type')` + `Cannot set properties of null (setting '__vnode')`（堆栈 patchKeyedChildren→patchChildren→patchElement）；③ 表单侧「表单画布暂不支持」的禁用态组件不要展示（修正 P2.5 的禁用态同显决策）；④ 右侧「组件属性」spec 增量区块没与手写面板融合、基本不可用、样式乱（修正 P2.5 的黑名单增量模式）。

| 文件 | 变更 |
|------|------|
| `forge-admin-ui/src/components/ai-form/AiFormItem.vue` | D1 Slider 预览崩溃修复：n-slider 改用 `resolveSliderValue(value, field)` 归一化——range 模式 value 非法/null 时回退 `[min, max]`（Naive UI Slider.mjs 的 `(range ? mergedValue : [mergedValue]).map(clampValue)` 在 range+null 时崩溃）；`:marks` 空值显式传 undefined |
| `forge-admin-ui/src/components/ai-form/AiFormLayoutNodes.vue` | D2 keyed diff 崩溃修复：`resolveNodeKey` 由 `key→field→id` 改为 `key→id→field`。根因：设计器复制组件（`duplicateDesignerComponent`→`rewriteComponentIds`）只重写 id、保留 `fieldBinding.fieldCode`，运行态 `normalizeRuntimeComponent` 生成 node.field 相同的两个节点，field 优先产生重复 key → patchKeyedChildren 未定义行为（isSameVNodeType null / 对 null el 设 __vnode）。布局节点（tabPane/collapseItem）的 field 已被 normalize 删除、合成 pane 用显式 key，均不受影响；无 id 的手写 schema 仍回退 field（历史兼容） |
| `forge-form-designer/ForgeFieldShelf.vue` | B2 修正（用户验收反馈）：`formShelfItemDisabledReason` 禁用态同显 → `formShelfItemFilter` 直接隐藏；scope 回归 F；包装节点/画布不支持组件 return false 不渲染（注册表仍是唯一事实源） |
| `forge-form-designer/ForgePropertyPanel.vue` | C3 修正（用户验收反馈）：整体回滚 P2.5 的 spec 增量「组件属性」区块（SPEC_PANEL_EXCLUDED_PROPS 黑名单 100+ key + specPanelPropertyCount/handleSpecPropUpdate + 模板区块，共 144 行）——黑名单模式与手写面板并存导致入口重复、未对齐属性改了不生效、布局割裂；表单侧属性面板回归纯手写面板。列表侧 spec 兜底（无手写分支的 blockType 自动渲染）不受影响，保留 |
| `forge-admin-ui/src/components/ai-form/__tests__/AiFormLayoutNodes.spec.js` | 新增 3 个 D2 红线用例：重复 fieldCode 双节点渲染不丢、复制节点插入后 keyed children 的 key 唯一（NGiStub 暴露 data-key 到 DOM 断言 `['cmp_a','cmp_a_copy','cmp_b']`，旧实现 field 优先必红）、无 id 手写 schema 回退 field 兼容 |

### 执行命令与结果

```bash
# 1. D2 修复红灯-绿灯验证（sed 临时切回旧实现跑测试再恢复）
npx vitest run src/components/ai-form/__tests__/AiFormLayoutNodes.spec.js
# → 新实现 8 passed；旧实现（field 优先）"key 唯一性"用例必红（keys=['userName','userName','status'] 重复）✅

# 2. C3 回滚验证
git checkout -- src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue
grep -c 'SpecPropertyPanel|SPEC_PANEL_EXCLUDED_PROPS' ForgePropertyPanel.vue  # → 0（残留清零）

# 3. 相关范围回归（ai-form + designer-core + app-center designer）
npx vitest run src/components/ai-form src/components/lowcode-builder/designer-core src/views/app-center/components/designer
# → Test Files 33 passed (33)，Tests 250 passed (250)   ✅

# 4. ESLint（4 个改动/新增文件）
npx eslint --fix src/components/ai-form/__tests__/AiFormLayoutNodes.spec.js   # 修复 import 排序 + describe 小写
npx eslint <4 个文件>            # 零输出 = 0 error 0 warning   ✅
```

### 跳过项与原因

- 浏览器视觉验证：未启动 Vite dev server。建议用户 `pnpm dev` 人工验收四个场景：① 预览含 range slider 的表单不再报 Slider null.map；② 复制字段组件后预览/切换预览模式不再报 isSameVNodeType/__vnode 崩溃，复制件正常渲染；③ 表单左侧组件库不再出现灰底「表单画布暂不支持」项；④ 右侧属性面板不再出现「组件属性」增量区块，回归手写面板。
- 复现实验说明：duplicate key 崩溃在 jsdom+stub 环境下多数排列"侥幸"不崩（Vue keyed diff 未定义行为），故 D2 测试采用 key 唯一性契约断言（data-key 暴露到 DOM），旧实现确定性红灯；真实浏览器深层 Naive UI 组件树（预览弹窗打开/previewMode 切换全量 diff）是崩溃实际触发环境。
- 临时复现脚本（repro-crash / repro-panel-crash / tmp-key-crash）已全部删除，未沉淀为正式测试（复现路径依赖 Naive UI 全局注册，vitest 环境不注册）。

### 结论

- 用户四项反馈全部收口：Slider range null 归一化 / keyed children key 唯一化（id 优先）/ 表单侧不支持组件隐藏 / 表单侧 spec 增量属性区块回撤（回归手写面板）。
- P2.5 的 B2（禁用态同显）与 C3（黑名单增量属性）两项决策按用户验收反馈修正：表单侧从「同显+增量」退回「隐藏+纯手写」，列表侧保持 P2.5 行为（用户未反馈列表侧问题）。
- designer-core 注册表、列表侧 spec 兜底、A 系列栅格语义均未触碰；本轮未启动任何后端服务、未触碰数据库、未新增第三方依赖。

---

## 2026-09-05 · P2.7 用户验收反馈修正轮（栅格布局预览渲染不出来）

### 变更范围

本轮目标：收口用户验收反馈——表单设计器拖入的栅格布局（row）在预览中完全渲染不出来，用户怀疑渲染组件不支持布局节点。调查证伪了该猜测：AiFormLayoutNodes 完整支持 row/col/card/tabs/collapse（有字段的栅格渲染正常，已用测试证明）。真正根因在 AiForm 的 schema 过滤管道：`filterVisibleNodes` 对 children 为空且非 standalone（divider/标题/button/crud）的布局节点整棵删除——`designerLayoutFactory.createForgeLayoutComponent('row')` 刚拖入时必然是「row + 4 个空 col」，4 个空 col 先删 → row.children 变空 → row 也被删 → 预览输出空表单。

修复方案：运行态语义保持不变（发布后的表单里空容器仍不渲染），设计器预览通过 `keepEmptyLayoutNodes` 开关保留空布局结构并渲染可见占位。

| 文件 | 变更 |
|------|------|
| `forge-admin-ui/src/components/ai-form/AiForm.vue` | 新增 `keepEmptyLayoutNodes` prop（默认 false）；`filterVisibleNodes` 的空容器删除条件追加 `&& !props.keepEmptyLayoutNodes`；模板向 AiFormLayoutNodes 透传该开关 |
| `forge-admin-ui/src/components/ai-form/AiFormLayoutNodes.vue` | 新增 `keepEmptyLayoutNodes` prop；`visibleNodes` 为空且开关开启时在 n-grid 内渲染可见占位 gi（`.af-empty-layout-placeholder` 虚线框 + 「空容器」文案）——占位由最内层递归组件统一渲染，一处覆盖空 col / 空 card 内容 / 空 tabPane / 空 collapseItem / 空表格单元格全部场景；8 处递归调用（row/col/card/tabs/collapse/table/tableGrid/v-else 兜底）全部透传开关 |
| `forge-form-designer/ForgeFormDesigner.vue` | 预览模板 4 处 AiForm（form section / card childSchema / tab.schema / collapse panel.schema）统一传 `:keep-empty-layout-nodes="true"`，与画布所见一致；运行态（发布后）调用方不传该 prop、行为不变 |
| `forge-admin-ui/src/components/ai-form/__tests__/AiForm-layout-preview.spec.js` | 3 用例：① keepEmptyLayoutNodes=true 时空栅格保留结构并渲染占位（1 row gi + 4 col gi + 4 占位 gi = 9，占位块 ×4）；② 默认不传时空栅格仍被整棵删除（运行态兼容红线）；③ 有字段的栅格正常渲染（渲染链路本身无问题的证明） |

### 执行命令与结果

```bash
# 1. 复现验证（修复前）：用例「空栅格被整棵删除」通过 = 复现成功；「有字段的栅格正常渲染」通过 = 渲染组件支持布局节点
npx vitest run src/components/ai-form/__tests__/AiForm-layout-preview.spec.js

# 2. 修复后回归（ai-form + app-center designer 全量）
npx vitest run src/components/ai-form src/views/app-center/components/designer
# → Test Files 27 passed (27)，Tests 150 passed (150)   ✅

# 3. ESLint（4 个改动文件；AiForm.vue 163 行 schema prop warning 为既有存量，非本轮引入）
npx eslint <4 个文件>   # 0 error   ✅
```

### 跳过项与原因

- 浏览器视觉验证：未启动 Vite dev server。建议用户 `pnpm dev` 人工验收：① 表单设计器拖入「栅格布局」后切预览，能看到 4 个虚线「空容器」占位块并排（与画布 4 列结构一致）；② 往某列拖入字段后预览该列显示字段、其余空列仍显示占位；③ 已发布应用的运行态表单里空栅格依旧不渲染（旧行为不变）。
- card/tabs/collapse 拖入即空时的外壳渲染依赖设计器预览模板自身的 `v-if="schema.length"` 判断（schema 含空子节点即非空数组，外壳正常渲染），本轮未改动该逻辑。

### 结论

- 用户反馈收口：栅格布局预览渲染不出来不是渲染组件缺支持，而是 AiForm 过滤管道删除空容器；已通过 `keepEmptyLayoutNodes` 开关让设计器预览保留空布局结构并渲染可见占位，运行态默认行为零变化。
- 修复同时覆盖空卡片内容 / 空标签页 / 空折叠面板 / 空表格单元格等一切空布局容器场景（占位由递归最内层统一渲染）。
- 本轮未启动任何后端服务、未触碰数据库、未新增第三方依赖。

---

## 2026-09-05 · P2.8 用户验收反馈修正轮（运行态表单栅格布局失效）

### 变更范围

本轮目标：收口用户验收反馈——表单设计器里配置好的栅格布局（滑块/评分并排、两卡片并排），在预览/运行态的 CRUD 新增表单里字段全部垂直堆叠、栅格完全未生效（截图特征「新增」标题 + 右上「× 返回列表」，定位为 AiCrudPage 内嵌表单工作区，即应用运行态链路）。

根因调查（全链路追踪）：渲染组件（AiFormLayoutNodes 完整支持 row/col/card/tabs/collapse 树形 schema）与 AiCrudPage（modalFormSchema 树形透传）本身都没有问题；表单设计器保存的布局树也一路通畅——BusinessFormDesigner.buildFormDesignerEditZone 把布局树写进 `zones.edit.props.formLayout` → 后端 LowcodeRuntimeConfigBuilder 正确编译进 `options.editFormLayout` → 前端 buildRuntimeCrudProps 整体透传 options。**唯一断点在 GridBlockRenderer 的 effectiveRuntimeCrudProps：直接使用平铺 editSchema（filterCrudItemsByFieldRefs 输出），从不消费 options.editFormLayout 做树形合成**——该合成逻辑在 crud-page.vue（AI CRUD 独立页）和 LowcodePreviewPane.vue（预览面板）各有实现，唯独应用运行态链路缺失。

修复方案：抽取共享合成模块并在 GridBlockRenderer 接入。接入顺序为「先平铺过滤、再树形水合」——filterCrudItemsByFieldRefs 按 key 白名单过滤会误删布局节点（布局节点 key 是组件 id），必须在平铺态完成字段过滤后再合成树形。

| 文件 | 变更 |
|------|------|
| `forge-admin-ui/src/components/lowcode-builder/shared/runtime-form-layout.js` | 新建共享模块：导出 `hydrateRuntimeFormLayout(fields, layout)`，整合 crud-page.vue 与 LowcodePreviewPane 两份 transformEditFields 重复实现（以功能更全的 crud-page 版为准，含 widget/isPageWidgetComponentKey standalone 判断）；两处防御：输入已是树形（含 nodeType !== 'field' 节点）时直接返回，防二次合成时第一层无字段可映射、整棵树被误删；无布局树时返回平铺（旧配置兼容）。后续 crud-page/LowcodePreviewPane 可切换复用该模块消除重复（本轮未动，避免无关回归） |
| `forge-admin-ui/src/components/lowcode-builder/page/GridBlockRenderer.vue` | ① import 区引入 hydrateRuntimeFormLayout；② effectiveRuntimeCrudProps 的 editSchema 改为 `hydrateRuntimeFormLayout(filterCrudItemsByFieldRefs(平铺字段, fieldRefs), options?.editFormLayout)`——运行态与设计预览共用此 computed，一处接入两场景生效 |
| `forge-admin-ui/src/components/lowcode-builder/shared/__tests__/runtime-form-layout.spec.js` | 8 用例：① 平铺字段 + row 布局树合成树形且字段完整水合（label/type 从 fieldMap 取）；② 无布局树返回平铺（旧配置兼容）；③ 已是树形输入直接返回（防二次合成）；④ 树外字段按原顺序追加末尾；⑤ 树中引用不存在字段节点删除、空容器删除；⑥ standalone 节点（divider）无 children 保留；⑦ card > row 嵌套结构水合；⑧ 字段列表含 null 条目防御 |

### 执行命令与结果

```bash
# 1. 新模块单测
npx vitest run src/components/lowcode-builder/shared/__tests__/runtime-form-layout.spec.js
# → 8 passed   ✅

# 2. 全量回归（lowcode-builder + ai-form + app-center designer 三目录）
npx vitest run src/components/lowcode-builder src/components/ai-form src/views/app-center/components/designer
# → Test Files 40 passed (40)，Tests 296 passed (296)   ✅（含 P2.7 的 150 个测试）

# 3. ESLint（3 个改动/新增文件）
npx eslint src/components/lowcode-builder/shared/runtime-form-layout.js \
  src/components/lowcode-builder/shared/__tests__/runtime-form-layout.spec.js \
  src/components/lowcode-builder/page/GridBlockRenderer.vue
# → 0 error 0 warning   ✅
```

### 跳过项与原因

- 浏览器视觉验证：未启动 Vite dev server。建议用户验证路径——表单设计器配置栅格布局并保存 → 打开对应应用的运行态页面 → 点「新增」打开内嵌表单：字段应按设计器里的 row/columns 结构并排渲染，未放进布局树的字段按原顺序追加在末尾。注意：若此前已保存过配置，需确认保存动作发生在布局配置之后（旧发布配置无 editFormLayout 时按平铺渲染，属预期兼容行为）。
- AiCrudPage 侧未改动：已验证 modalFormSchema 非详情模式直接透传树形 schema、tiledEditGridCols 的 flattenRuntimeFormFields 只统计字段数不破坏树形；AiFormLayoutNodes.resolveNodeSpan 对 row/card/tabs/collapse/crud/标题类节点强制 span=cols 整行渲染，顶层 grid-cols（AiCrudPage 平铺优化默认升 2 列）对树形 row 无破坏——row 内部按自身 props.columns 自成栅格，设计器 sanitizeRuntimeLayoutProps 保留 columns/gutter 字段，全链路字段命名对接验证一致。
- GridBlockRenderer 静态分支（无 runtimeCrudProps 的 v-else 路径）未接入：已确认页面 schema 的 AiCrudPage 块 props 不携带 formLayout（lowcode-builder 目录除新模块外零引用），该分支为设计器直接 API 模式，维持平铺现状。
- crud-page.vue / LowcodePreviewPane.vue 未切换到共享模块：两者已有各自可用实现，本轮统一会引入无关回归风险，留待后续重构轮次。

### 结论

- 用户反馈收口：运行态栅格失效不是渲染组件、不是 AiCrudPage、不是后端编译，而是应用运行态链路（GridBlockRenderer → AiCrudPage）缺失「平铺 editSchema + options.editFormLayout → 树形 schema」合成步骤；已通过共享模块 + GridBlockRenderer 一处接入修复，运行态与设计预览两条链路同时生效。
- 与 P2.7 的关系：P2.7 修复的是「空栅格被 AiForm 过滤管道删除」（设计器预览看不到刚拖入的空布局），本轮修复的是「有字段的栅格在运行态链路丢失树形结构」（设计时并排、运行时堆叠）——两个不同层面的根因，P2.7 的 keepEmptyLayoutNodes 修复依然有效。
- 防御性设计保证零回归：无布局树的旧配置按平铺渲染（行为与修复前完全一致）、已是树形的输入（如未来 BusinessListDesigner 链路接入）直接透传不二次合成。
- 本轮未启动任何后端服务、未触碰数据库、未新增第三方依赖。

---

## 2026-09-05 · P2.9 用户验收反馈修正轮（栅格格子删除 / locked 误禁删除 / 拖拽影子超宽）

### 变更范围

本轮目标：收口用户验收反馈的三项栅格交互问题——①点到栅格格子上浮出的是里面组件的删除而非格子的删除（格子无删除入口）；②栅格里面的组件删除按钮被禁用，提示「已有数据不能删除」；③拖拽时组件视觉超出栅格宽度。

根因分析：
- **问题①②（同一组根因）**：col 在画布中被定义为 structural slot（isStructuralSlot），整个操作浮层（含删除按钮）不渲染，点格子只能穿透到里面的组件；同时删除按钮把 `fieldBinding.locked`（字段已有业务数据）误扩大为「不能从画布删除」——但 locked 的正确语义是属性面板的「不能修改组件/存储类型」，从画布删除组件只移除表单展示，字段资产仍在左侧字段架子可重新拖入，无数据损失。
- **问题③**：拖拽跟随影子（drag-follow-clone）宽度取组件原始位置宽度（如画布顶层整行的宽度），拖入窄格子时影子比格子宽得多，视觉超出栅格。

| 文件 | 变更 |
|------|------|
| `ForgeFormCanvasNode.vue` | ① 删除按钮放开 locked 禁用（quick action + 下拉菜单 + removeNode 三处），title 改为「删除（字段资产保留，可从左侧重新拖入）」；属性面板的 locked 禁用（组件/存储类型切换）保持不变；② col 渲染精简操作条（column-overlay，仅删除按钮，无复制/菜单/把手），`removeGridColumn()` 删除格子时内部组件合并到相邻列（优先前一列，与属性面板「格子数量」调减的合并语义一致），仅剩一个格子时禁删（title「至少保留一个格子」）；structural-slot 补 hover/selected 可视化边框；③ `mountPointerDragImage` 增加 MAX_DRAG_IMAGE_WIDTH=320 钳制：影子视觉宽度超限时整体等比缩小（transform scale 乘 fitScale），拖拽偏移 clamp 同步按缩放后尺寸计算 |
| `ForgeFormCanvasNode-remove.spec.js` | 5 用例：① col 精简操作条（有删除、无菜单/把手）+ 字段完整操作条；② 栅格内字段删除链路；③ locked 字段可删除（画布删除不再被禁用）；④ 删第 2 列时 4 列变 3 列、第 2 列组件合并到第 1 列不丢失；⑤ 仅剩一列时删除按钮禁用且不产生 schema 变更 |

### 关键陷阱记录

- **normalizeFormDesignerSchema 深拷贝陷阱**：`getDesignerComponent` 内部会再次 normalize（cloneValue 深拷贝），返回的是副本引用——对它 splice 修改不会体现在外层 emit 的 schema 上。removeGridColumn 初版用它定位父节点导致「删格子无效果」，测试红灯复现后改为本地 `findComponentById` 递归在同一份 normalize 后的 schema 上定位。后续任何「normalize 后再定位再修改」的代码都要警惕这条链。

### 执行命令与结果

```bash
# 1. 本轮新增/更新测试
npx vitest run src/views/app-center/components/designer/forge-form-designer/__tests__/ForgeFormCanvasNode-remove.spec.js
# → 5 passed   ✅

# 2. 全量回归（designer + ai-form，覆盖 P2.7/P2.8 全部存量测试）
npx vitest run src/views/app-center/components/designer src/components/ai-form
# → Test Files 28 passed (28)，Tests 155 passed (155)   ✅

# 3. ESLint（2 个改动文件）
npx eslint <2 个文件>   # 0 error 0 warning   ✅
```

### 跳过项与原因

- 浏览器视觉验证：未启动 Vite dev server。建议用户验证——① hover 栅格格子（空白区域）出现蓝色高亮边框 + 右上角删除按钮，点击后该列消失、列内组件并入左邻列；② 只剩一列时删除按钮置灰提示「至少保留一个格子」；③ 删已有业务数据的字段组件不再弹「已有数据不能删除」；④ 拖宽组件（画布顶层整行块）到栅格内，跟随影子等比缩小不再超出格子宽度。
- tableGrid/tabPane/collapseItem 等其他 structural slot 未加删除入口：标签页/折叠项已有各自的容器级管理（tabs pane manager 等），本轮只处理用户反馈的栅格格子场景。

### 结论

- 三项反馈全部收口：格子有了直接删除入口（内容合并不丢失）、locked 语义收窄回「不能改类型」（画布删除放开）、拖拽影子宽度钳制。
- 「删格子→组件合并到相邻列」与属性面板「格子数量」调减的合并语义完全一致，两条删除路径行为统一。
- 本轮未启动任何后端服务、未触碰数据库、未新增第三方依赖。

---
## 2026-09-05 · P2.10 二次验收反馈修正轮（P2.9 CSS 修复在真实浏览器失效的根因收口）

### 变更范围

本轮背景：P2.9 修复单测全绿，但用户真实浏览器验收反馈同样三个问题依旧（点格子出的是里面组件的删除 / 格子里组件删除困难且见「已有数据不能删除」/ 拖拽影子超出栅格宽度）。静态排查发现 P2.9 的 CSS 修复在浏览器中被层叠规则压制，未实际生效。

根因分析（P2.9 失效原因）：
- **hover 高亮被旧规则整体压掉**：P2.9 在文件中部新增的 `.canvas-node.structural-slot:hover/.selected` 高亮，被文件后部旧规则 `.canvas-node.structural-slot:hover/:focus { border-color: transparent }` 同优先级覆盖（CSS 同优先级后声明胜出）→ 格子 hover 零视觉反馈，用户感知就是「点格子没反应、浮出的还是里面组件的操作条」。
- **两层操作浮层物理重叠互拦指针**：col 的 padding 为 0，column-overlay（top 6px、格子全宽）与列内组件 node-overlay 的快捷按钮（组件顶部 top 6px）在格子顶部同一区域重叠 → 指针命中谁取决于 DOM 顺序，组件删除按钮「靠近即消失/点不到」，格子删除按钮被组件按钮遮挡。
- **选中格子蓝框被 !important 压制**：`.canvas-node.structural-slot.selected { border-color: transparent !important }` 连带把格子的选中蓝框压掉，选中态无反馈。
- **拖拽影子钳制只做一次且无 max-width 兜底**：MAX_DRAG_IMAGE_WIDTH=320 仅在 mountPointerDragImage 应用一次固定值，拖动过程中不随悬停目标（窄格子）动态收紧；`.canvas-node` 缺 `max-width: 100%`，固定宽度组件（widthMode: fixed）拖入窄格后自身也横向溢出栅格。

| 文件 | 变更 |
|------|------|
| `ForgeFormCanvasNode.vue`（CSS） | ① `.canvas-node.node-col { padding-top: 30px }` 顶部预留操作条（替换被覆盖的死 hover 规则），物理隔离格子删除按钮与列内组件按钮；② 在旧透明规则**之后**追加 `.canvas-node.node-col:hover` 高亮（蓝边框 + 浅蓝底），确保层叠胜出；③ 选中态规则改 `.structural-slot:not(.node-col).selected`，恢复格子选中蓝框；④ `.canvas-node` 增加 `max-width: 100%`，固定宽度组件不再横向溢出容器 |
| `ForgeFormCanvasNode.vue`（JS） | 拖拽影子从「启动时一次性钳制 320px」升级为「全局上限 + 悬停目标动态钳制」：mountPointerDragImage 记录抓取比例（grabRatio）与基准尺寸；新增 `applyDragImageFitScale()`（transform-origin 0 0，偏移按抓取比例换算并 clamp）；`updatePointerDropPreview` 各分支（root 区 / 容器槽位 / inside / 栅格行 / 前后插入 / 清除预览）按悬停目标宽度调用 `syncDragImageFitScale()` 实时收紧或恢复 |
| `designerDragState.js` | 钳制算法抽为纯函数 `resolveDragPreviewFitScale(baseWidth, targetWidth)`（[0.1,1] 区间、目标无效仅全局上限、极窄目标保底 MIN_DRAG_IMAGE_WIDTH=120），导出 MAX/MIN 常量，可单测、可复用 |
| `ForgePropertyPanel.vue` | locked 提示文案补充「画布中删除该组件不影响已有数据，字段仍可从左侧字段列表重新拖入」，消除「已有数据不能删除」的误解 |
| `__tests__/designerDragState.spec.js`（新增） | resolveDragPreviewFitScale 8 用例：全局上限、目标钳制、不放大、极窄保底、非法参数防御、值域 |
| `ForgeFormCanvasNode-remove.spec.js` | 补 node-col CSS 钩子红线用例：断言 col 的 article 同时具有 `canvas-node` / `node-col` / `structural-slot` 类名（jsdom 不加载 CSS，类名是 CSS 修复生效的唯一前提） |

### 关键陷阱记录

- **scoped CSS 同优先级后声明覆盖**：在同一 `<style scoped>` 内新增/修改 selector 时必须全文检索同优先级的既有声明及其出现顺序，否则修复会被后面的旧规则静默压制——单测（jsdom 不解析 CSS）完全发现不了，只有真实浏览器验收才暴露。本轮为 node-col 类名加了红线测试，至少保证 CSS 钩子不被模板改动意外移除。
- **指针命中重叠是「点 A 出 B」类 bug 的常见根因**：两个 fixed/absolute 浮层在同一坐标区域叠加时，行为取决于 z-index 与 DOM 顺序，而非视觉直觉；解法是物理隔离（预留空间）而不是调 z-index。

### 执行命令与结果

```bash
# 1. designer 全目录回归（含新增 9 用例：8 纯函数 + 1 CSS 钩子）
npx vitest run src/views/app-center/components/designer/forge-form-designer/__tests__/
# → Test Files 6 passed (6)，Tests 41 passed (41)   ✅

# 2. ESLint（5 个改动/新增文件）
npx eslint <5 个文件>   # 0 error 0 warning   ✅
```

### 跳过项与原因

- 浏览器视觉验证：沙箱内 IPv4 回环被拒、IPv6 可连但 HTTP 数据被拦（browser-use MCP CDP 超时、Playwright goto 超时），无法自动化验证；已按「静态根因分析 + 单测红线」收口。建议用户重点验收：① hover/点击空白栅格格子 → 蓝色边框高亮 + 格子自身删除按钮（与列内组件按钮互不遮挡）；② 栅格内组件删除按钮稳定可点；③ 拖宽组件悬停窄格子上时跟随影子实时缩到格子宽度，离开后恢复；④ 固定宽度组件拖入窄格不溢出。

### 结论

- P2.9「修复未生效」的根因是 CSS 层叠压制与浮层重叠，本轮全部收口；拖拽影子升级为按悬停目标动态钳制（纯函数可测）。
- locked 误解通过属性面板文案澄清；画布删除链路 P2.9 已放开，本轮无行为变更。
- 本轮未启动任何后端服务、未触碰数据库、未新增第三方依赖；临时验证页与自启 dev server 已清理（用户 3000 端口 dev server 未受影响）。

---
## 2026-09-05 · P2.11 栅格删除语义修正 + 栅格属性面板增强轮

### 变更范围

用户两项反馈：①删除格子时内部组件"跑到别的栅格里"了——期望删格子=连内容一起删；②栅格组件右侧属性配置太少（对照 Naive UI Grid），希望补齐可配置项。

| 文件 | 变更 |
|------|------|
| `ForgeFormCanvasNode.vue` | ① `removeGridColumn()` 从「子组件合并到相邻列」改为「连同内部组件一起删除」（字段资产仍在左侧字段架子可重新拖入）；② 删除按钮 title 动态化（`columnDeleteTitle`）：空格子「删除该格子」、带内容「删除该格子（内部 N 个组件一并删除，字段可从左侧重新拖入）」 |
| `AiFormLayoutNodes.vue` | 运行时 row 分支补消费 `props.rowGap`：`:y-gap="resolveGap(node.props?.rowGap, yGap)"`——此前画布消费 rowGap 而运行时不消费，属 spec key 与渲染消费不对齐问题 |
| `ForgePropertyPanel.vue` | ① 「栅格快捷配置」新增「行间距」rowGap（0-40，默认 8，与画布 childrenGridStyle 默认一致）；② 列间距 gutter 默认展示值 16→12（与画布 resolveGap 默认对齐）；③ 修 `updateRowCellCount` 未展开原 props 导致调格子数量时丢 rowGap 的 bug（现 `...(row.props)` 展开保留）；④ 「格子数量」下加 property-help 说明两入口语义差异（调数量=合并到最后一格；画布删除按钮=连带删除） |
| `ForgeFormCanvasNode-remove.spec.js` | 「删列合并子组件」用例改写为「删列连带删除」：断言 4→3 列、目标列与内部字段均从 schema 消失、带内容 title 含「一并删除」 |
| `AiFormLayoutNodes.spec.js` | NGridStub 暴露 data-xgap/data-ygap；新增 2 用例：row 的 gutter/rowGap 分别透传子级 n-grid 的 x-gap/y-gap；未配 rowGap 时回落父级默认 |

### 关键陷阱记录

- **同一交互两个入口的语义分离**：属性面板「格子数量」调减（布局重排，内容并入最后一格）与画布格子删除按钮（明确删除动作，内容一并删）此前被统一成合并语义，导致用户"删格子内容搬家"的困惑。两者语义刻意分离并在面板上加文案说明，避免再次混淆。
- **属性面板写 props 未展开原字段**：`updateRowCellCount` 直接写 `props: { columns, gutter }` 而非展开 `...(row.props)`，任何新增的栅格 props（如本轮 rowGap）都会在调格子数量时被静默清掉——属性面板所有 updateComponent 写 props 处都应展开原 props。

### 执行命令与结果

```bash
# designer + ai-form 全量回归（含新增/改写用例）
npx vitest run src/views/app-center/components/designer src/components/ai-form
# → Test Files 29 passed (29)，Tests 166 passed (166)   ✅

# ESLint（5 个改动文件）
npx eslint <5 个文件>   # 0 error 0 warning   ✅
```

### 跳过项与原因

- Naive UI Grid 的响应式能力（`cols="1 s:2 m:3"` + `item-responsive`、`responsive="self"`、`collapsed/collapsed-rows`）：运行时 n-grid 原生支持，但设计器画布是静态 CSS grid 预览（childrenGridStyle 数字列数），开放后画布与运行时表现不一致且 span 钳制逻辑需重构，建议作为独立需求排期。
- 栅格其余属性（列数 columns / 格子数量 / 列间距 gutter / 行间距 rowGap / 每列 span）本轮已补齐并对齐设计器与运行时两处消费 key；其它字段类组件的 Naive UI 属性面板全覆盖属更大范围改造，按组件逐批推进。

### 结论

- 删格子语义改为连带删除，与用户直觉一致；字段资产保留可重新拖入，无数据损失。
- 栅格属性面板补齐行间距并修复调格子数量丢配置的 bug；设计器画布与运行时渲染的 rowGap/gutter key 已完全对齐。
- 本轮未启动任何后端服务、未触碰数据库、未新增第三方依赖。

---

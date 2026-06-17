# 低代码表单设计器顺滑拖拽改造
> status: apply
> created: 2026-06-15
> complexity: 🔴复杂

## 1. 背景与目标

当前 `/app-center/object/crm_follow_record?suiteCode=IN_OUT` 进入“设计对象 → 表单设计”后，中间表单编辑区依赖 FormCreate 通用设计器。它能完成基础拖拽，但体验不符合业务对象设计器的目标：字段资产、布局编排、运行态 CRUD 和发布检查是 Forge 自己的业务链路，FormCreate 画布只理解通用表单组件，导致操作割裂、拖拽反馈弱、布局调整不直观。

本次变更目标是将表单设计器中间画布改造成 Forge 原生设计体验，达到“类似飞书多维表格/表格配置面板”的轻快操作感：

- 拖拉拽顺滑：拖动过程低延迟、重排无跳动、落点明确。
- 操作简单：字段、分组、卡片、标签页等常用布局通过少量手势完成。
- 布局一目了然：字段库、画布、属性面板职责清晰，视觉层级稳定，用户进入页面后能直接理解“从哪里拖、拖到哪里、选中后在哪里改”。
- 组件细分清楚：画布、节点、拖拽状态、落点提示、属性编辑、字段库和模板入口拆成独立组件/Composable，避免单文件堆叠复杂逻辑。
- 动效克制但明确：拖动时出现“原位幽灵 + 跟手镜像 + 落点双胞胎阴影”，让用户知道原位置、当前位置和将要放置的位置。
- 运行态不重做：继续复用现有 `AiCrudPage`、`AiForm`、动态 CRUD、发布检查和后端运行配置生成链路。
- 多端演进可扩展：设计协议继续维护 `formDesignerSchema`，后续可由 PC Renderer 和 Mobile Renderer 使用同一 Schema 渲染。

可验证结果：

- `crm_follow_record` 表单设计不再直接展示 `FcDesigner` 作为主画布。
- 用户可从字段库拖入字段、在画布内重排字段、调整字段跨度、放入卡片/分组/标签页。
- 拖拽时有明确的原位幽灵、跟手镜像、目标位阴影和吸附提示。
- 所有可拖拽对象 hover 后必须出现明确反馈：拖拽手柄图标、可拖 cursor、边框/背景变化和可操作工具条。
- 页面布局在 1366x768 下仍可直接看清核心工作区，不出现工具栏挤压、按钮换行错乱或属性面板遮挡画布。
- 保存、预览、发布、打开运行应用结果继续兼容现有动态 CRUD。
- 构建通过，关键 schema 编译逻辑有单元或组件级验证。

## 2. 代码现状（Research Findings）

> 每个结论必须有代码出处（文件路径 + 类名/方法名）

### 2.1 相关入口与链路

- 业务对象详情页入口在 `forge-admin-ui/src/views/app-center/object.[objectCode].vue`。
  - `template` 第 18 行和第 95 行通过 `openDesigner('form')` 打开设计器。
  - 第 163 行嵌入 `BusinessObjectDesignerPage`，传入 `embedded-object-code`、`embedded-object-id`、`initial-panel`。

- 业务对象设计器表单面板在 `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`。
  - 第 81 行进入 `activePanel === 'form'` 面板。
  - 第 84 行挂载 `BusinessFormDesigner`。
  - 第 86-87 行以 `v-model="draft.pageSchema"` 和 `v-model:form-designer-schema="draft.formDesignerSchema"` 同步设计协议。
  - 第 524 行 `saveDesignerDraft()` 仍走 `saveBusinessObjectDesigner()` 保存整体设计草稿。

- 当前中间画布在 `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`。
  - 第 51 行挂载 `BusinessFormCreateDesigner`。
  - 第 221 行引入 `saveBusinessObjectDesigner`、`saveBusinessObjectFormLayout`。
  - 第 497 行 `syncFormDesignerSchemaToPageSchema()` 将 `formDesignerSchema` 编译进 `pageSchema.zones[edit]`。

- FormCreate 嵌入点在 `forge-admin-ui/src/views/app-center/components/designer/BusinessFormCreateDesigner.vue`。
  - 第 18 行使用 `FcDesigner`。
  - 第 37 行直接 import `@form-create/designer`。
  - 第 40 行通过 `formCreateBridge` 安装 FormCreate + Element Plus 适配。
  - 第 140 行调用 `forgeSchemaToFormCreate()` 将 Forge Schema 转为 FormCreate rule。
  - 第 166 行 `flushDesigner()` 再用 `formCreateToForgeSchema()` 转回 Forge Schema。

### 2.2 现有实现

- Forge 自研表单 Schema 已存在于 `forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js`。
  - 第 14 行 `FIELD_COMPONENT_KEYS` 定义字段组件。
  - 第 49 行 `LAYOUT_COMPONENT_KEYS` 同时支持 `elCard/card`、`elTabs/tabs` 等双命名。
  - 第 194 行 `createDefaultFormDesignerSchema()` 能按字段生成默认表单。
  - 第 221 行 `normalizeFormDesignerSchema()` 负责归一化。
  - 第 340 行 `createComponentFromField()` 能从字段资产生成组件节点。

- 表单设计结果已经能编译到运行态：
  - `BusinessFormDesigner.vue` 第 497 行 `syncFormDesignerSchemaToPageSchema()` 读取 `formDesignerSchema`。
  - 第 509 行生成 `fieldSettings`。
  - 第 510 行生成 `formLayout`。
  - 第 518 行仍保留 `formCreateRule` 兼容旧运行逻辑。
  - 第 533 行写入 `formLayout`。
  - 第 535 行标记 `compiledFrom: 'formDesignerSchema'`。

- 运行态已经支持布局节点：
  - `forge-admin-ui/src/views/ai/crud-page.vue` 第 307 行 `transformEditFields()` 会读取 `options.editFormLayout`。
  - 第 325 行 `hydrateRuntimeLayoutNode()` 将运行布局节点和字段 schema 合并。
  - 第 389 行将水合后的 `editSchema` 传入 `AiCrudPage`。
  - `forge-admin-ui/src/components/ai-form/AiFormLayoutNodes.vue` 第 1 行开始渲染布局节点。
  - `AiFormLayoutNodes.vue` 第 55 行支持 `card`。
  - 第 80 行支持 `tabs`。
  - 第 112 行支持 `collapse`。
  - 第 143 行支持 `divider`。

- 后端运行配置生成已经消费 `formLayout`：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeRuntimeConfigBuilder.java` 第 55 行 `buildRuntimeConfig()` 将页面协议转换为 `AiCrudPage` 运行配置。
  - 第 156 行 `buildOptions()` 读取编辑区 props。
  - 第 176 行读取 `formLayout`。
  - 第 178 行写入 `options.editFormLayout`。

- 前端已有拖拽依赖和基础用例：
  - `forge-admin-ui/package.json` 已包含 `vuedraggable` 与 `@vueuse/core`。
  - `forge-admin-ui/src/components/lowcode-builder/model/ModelFieldTable.vue` 使用 `vuedraggable` 做字段排序。
  - `forge-admin-ui/src/components/lowcode-builder/page/BuilderCanvas.vue` 使用 `vuedraggable` 做区块排序。
  - `forge-admin-ui/src/components/ai-form/AiTableFilter.vue` 已有拖动中/悬停/列表移动样式和 `transition-group`。

### 2.3 发现与风险

- 当前最大体验问题不是运行态 CRUD，而是设计态画布和业务字段资产割裂。
- FormCreate 设计器需要 Forge Schema ↔ FormCreate Rule 双向转换，任何布局、字段绑定、业务组件都会增加转换损耗。
- 如果直接拿 `AiCrudPage` 当设计器画布，会把运行态接口、弹窗、表格和设计态拖拽强绑定，后续维护风险高。
- 推荐替换 `BusinessFormCreateDesigner`，保留 `BusinessFormDesigner.syncFormDesignerSchemaToPageSchema()` 和运行态链路。
- 拖拽动效必须遵守 `prefers-reduced-motion`，不能为了“顺滑”引入持续动效或大面积重绘。

## 3. 功能点

- [ ] 原生 Forge 表单设计器
  - 输入：`formDesignerSchema`、字段资产、对象编码、对象名称。
  - 处理：以 Forge Schema 为唯一编辑源，直接增删改排组件树。
  - 输出：更新后的 `formDesignerSchema`，由现有编译链路生成 `pageSchema`。

- [ ] 三栏式设计工作台
  - 左侧：字段资产、布局组件、模板片段。
  - 中间：真实业务画布。
  - 右侧：属性面板。
  - 顶部：表单列数、弹窗/抽屉、预览、补齐字段、撤销重做、清理失效字段。
  - 信息密度按后台工具设计，避免营销式大卡片和过度留白；画布区域优先保证宽度。
  - 所有面板标题、分组、状态数量和空状态必须清楚，用户无需阅读说明即可知道下一步操作。

- [ ] 飞书多维表格式顺滑拖拽
  - 字段从左侧拖入画布。
  - 画布内字段可重排。
  - 字段可拖入卡片、分组、标签页、折叠面板。
  - 可拖拽对象 hover 后显示拖拽手柄图标，手柄区域使用 `cursor: grab`，按下后切换 `cursor: grabbing`。
  - 可拖拽对象 hover 后出现轻量背景、边框或阴影变化，禁用项要明确显示不可拖状态和原因。
  - 拖动时出现：
    - 原位幽灵：原组件保持淡化轮廓，避免用户失去来源位置。
    - 跟手镜像：拖拽卡片跟随指针，使用 `transform` 和 `opacity`。
    - 目标位双胞胎阴影：落点位置显示与组件同尺寸的浅色阴影，后方附加一层偏移投影，表达“将复制/移动到这里”的空间感。
    - 吸附线：靠近列边界、容器边界、字段之间时显示落点线。
  - 重排使用 FLIP 思路，非拖拽项以 `transform` 平滑移动。

- [ ] 字段操作简化
  - 单击选中字段。
  - 双击字段名可快速改显示标签。
  - 拖动边缘或属性面板调整字段跨度。
  - 快捷操作：复制、删除、隐藏、必填、只读、移到上一行/下一行。
  - 未使用字段可一键补齐。
  - 常用操作必须出现在选中态工具条或右键/更多菜单中，避免用户只依赖右侧属性面板。
  - 危险操作如删除必须有明确按钮样式和二次确认或可撤销策略。

- [ ] 布局操作简化
  - 支持 `title/divider`、`card`、`tabs/tabPane`、`collapse/collapseItem`、`row/col`。
  - 支持“创建分组并移动选中字段进去”。
  - 支持空容器显示可点击落点，避免空容器难以拖入。
  - 支持从常用模板创建布局：基础信息 + 跟进内容 + 时间负责人。

- [ ] 属性面板
  - 字段属性：标题、占位符、列跨度、必填、只读、默认值、字典、上传限制、关联对象显示字段。
  - 布局属性：标题、描述、列跨度、折叠状态、标签页标题、容器样式。
  - 表单属性：列数、标签位置、标签宽度、行列间距、尺寸、反馈显示。
  - 属性面板按“基础 / 布局 / 校验 / 高级”分组，默认展开常用项，降低认知负担。
  - 所有输入控件必须有清晰 label，不允许只用 placeholder 解释字段含义。

- [ ] 代码结构与可维护性
  - 新增组件按职责拆分，单个 `.vue` 文件原则上不承载拖拽、渲染、属性编辑和 schema 操作全部逻辑。
  - 复杂拖拽、落点计算、组件树变更逻辑必须放入独立 composable 或纯函数。
  - 注释只写关键意图和复杂算法说明，例如 FLIP 重排、落点碰撞判断、schema 路径转换；禁止堆砌“赋值/调用”类无效注释。
  - 组件和函数命名要直接表达业务含义，避免 `handleData`、`processItem`、`tempList` 这类泛化命名。

- [ ] 预览与运行态一致性
  - 设计画布使用与 `AiFormLayoutNodes` 兼容的节点模型。
  - 保存后继续生成 `fieldSettings` 和 `formLayout`。
  - 发布后 `AiCrudPage` 新增/编辑/详情表单排版与设计器一致。

- [ ] 兼容旧数据
  - 已保存的 `formDesignerSchema` 直接读取。
  - 仅存在 FormCreate rule 的旧对象，进入时可转换成 Forge Schema。
  - 保留 `BusinessFormCreateDesigner` 临时回退入口，便于对比和应急。

## 4. 业务规则

- 表单设计器以 Forge `formDesignerSchema` 为主数据源。
- 字段组件必须绑定业务字段，布局/虚拟组件不得占用业务字段。
- 系统字段默认不可拖入主表单，除非后续明确允许只读展示。
- 已拖入字段在字段库中标记“已使用”，默认不允许重复拖入同一字段。
- 删除字段组件只从表单布局移除，不删除字段资产。
- 删除字段资产时必须清理或标记表单中失效引用。
- 运行态保存和发布仍以 `pageSchema`、`modelSchema`、`formDesignerSchema` 为准。
- 拖拽动效必须尊重 `prefers-reduced-motion`：用户开启减少动态效果时，保留落点高亮但禁用跟手弹性和重排动画。
- 可交互元素必须有明确状态：默认、hover、active、selected、disabled、dragging、drop-target、invalid-drop。
- 可拖拽元素必须有手柄或图标提示；不能让用户猜测整块区域是否可拖。
- 面板布局必须服务高频操作：字段库可快速搜索/分组，画布始终是最大视觉区域，属性面板只展示当前选中对象相关内容。
- 代码结构必须让后续接入移动端 Renderer 时能复用 schema 操作和编译逻辑。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 无 | - | - | Phase 1 不新增表字段，继续使用现有业务对象设计草稿中的 `formDesignerSchema`、`pageSchema` |

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 无 | `/ai/business/object/{objectId}/designer` | GET/PUT | 继续读取和保存现有设计器协议 |
| 无 | `/ai/business/object/{objectId}/layout/preview` | POST | 继续使用现有预览生成 |
| 无 | `/ai/business/object/{objectId}/publish` | POST | 继续使用现有发布 |

## 7. 影响范围

- 前端：
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`
  - `forge-admin-ui/src/views/app-center/components/designer/BusinessFormCreateDesigner.vue`
  - 新增 `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/`
  - `forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js`
  - `forge-admin-ui/src/components/ai-form/AiFormLayoutNodes.vue`

- 后端：
  - Phase 1 原则上不改后端。
  - 如需增强发布校验，再评估 `LowcodeRuntimeConfigBuilder` 和业务对象发布检查。

- 运行态：
  - `forge-admin-ui/src/views/ai/crud-page.vue`
  - `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`
  - `forge-admin-ui/src/components/ai-form/AiForm.vue`

- 典型验证对象：
  - `/app-center/object/crm_follow_record?suiteCode=IN_OUT`
  - 跟进记录字段：负责人、主题、关联客户、关联合同/商机、跟进方式、跟进内容、跟进时间。

## 8. 风险与关注点

- 拖拽体验风险：如果只用 `vuedraggable` 默认 ghost class，可能达不到飞书多维表格式顺滑感；需要补自定义 drag overlay、drop indicator 和 FLIP 动效。
- 布局复杂度风险：三栏工作台如果信息层级不清，会让用户看不懂当前应操作哪里；需要先固定信息架构和面板职责，再做视觉细节。
- 代码复杂度风险：拖拽、节点渲染、属性编辑如果堆在单个组件里会快速失控；必须拆分组件和 composable，并用纯函数处理 schema。
- 性能风险：画布字段超过 80 个时，拖动中频繁深拷贝 schema 会卡顿；拖动中应只维护轻量 drag state，落下后一次性提交 schema。
- Schema 兼容风险：旧 FormCreate rule、旧 `formDesignerSchema`、当前运行态 `formLayout` 必须共存过渡。
- 运行态一致性风险：设计态展示若与 `AiFormLayoutNodes` 差异过大，会造成“设计看到的不是发布后的样子”。
- 可访问性风险：拖拽不能成为唯一操作方式，至少要提供按钮上移/下移、移入分组、键盘焦点和可见 focus 状态。
- 动效风险：过多动画会降低后台工具效率；只在拖拽、选中、插入、撤销重做几个关键点使用 150-240ms 动效。
- 权限/资金/状态流转：本次不涉及资金、权限放开或业务状态流转。

## 8.5 测试策略

- **测试范围**：
  - `formDesignerSchema` 归一化、字段插入、移动、删除、布局容器嵌套。
  - `formDesignerSchema -> pageSchema.editZone.props.fieldSettings/formLayout` 编译结果。
  - `crm_follow_record` 保存、预览、发布、打开运行应用。
  - 可拖拽元素 hover、active、disabled、dragging、drop-target 等状态表现。
  - 三栏布局在 1366x768、1440x900、1920x1080 下核心操作区可见。
  - 拖拽交互在 1366x768、1440x900、移动窄屏降级下无重叠。
  - `prefers-reduced-motion` 下动效降级。

- **覆盖率目标**：
  - Schema 工具函数：覆盖新增移动、插入、删除、路径定位逻辑。
  - 组件交互：覆盖字段拖入、画布内排序、容器内拖入、属性面板修改。
  - 构建验证：`pnpm build`。

- **独立 Test Spec**：是。进入 `/test` 前创建或补充 `test-spec.md`，并按 `code-copilot/rules/automated-testing-standard.md` 追加 `execution-log.md`。

## 9. 待澄清

- [ ] 第一阶段是否允许完全隐藏 FormCreate 画布，只保留高级回退开关？
- [ ] 是否需要首期支持移动端预览模式，还是只预留 schema 字段后续做 H5 Renderer？
- [ ] “双胞胎阴影”的视觉强度是否以轻量后台风格为准：浅蓝边框 + 白色主体 + 第二层偏移阴影？
- [ ] 是否要把 `crm_follow_record` 内置一个推荐布局模板：基础信息、跟进对象、跟进内容、时间负责人？
- [ ] 拖拽手柄图标优先使用现有 Iconify/UnoCSS 图标，还是统一使用 `@vicons/ionicons5` 图标？

## 10. 技术决策

- 决策 1：设计态不直接复用 `AiCrudPage` 页面壳。
  - 原因：`AiCrudPage` 是运行态组件，包含接口、弹窗、表格、导入导出、流程详情等职责；设计态应只复用字段渲染资产和布局协议。

- 决策 2：Forge `formDesignerSchema` 作为设计态唯一事实来源。
  - 原因：现有 schema 已支持字段组件、布局组件、归一化、默认生成和运行态编译，替换画布不需要重写后端和动态 CRUD。

- 决策 3：Phase 1 不引入新拖拽依赖，优先使用已有 `vuedraggable` + 自定义 Pointer/Overlay/FLIP。
  - 原因：项目已有 `vuedraggable`，可覆盖列表和嵌套排序；复杂镜像和吸附提示由自研 composable 控制，减少依赖风险。

- 决策 4：动效只用 `transform`、`opacity`、`box-shadow`，禁止拖拽中修改布局尺寸做动画。
  - 原因：降低重排和卡顿风险，保证后台工具效率。

- 决策 5：保留 FormCreate 兼容转换，但从主链路下线。
  - 原因：旧数据迁移和应急回退需要一段过渡期。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Proposal | done | `spec.md`, `tasks.md` | 已创建改造提案 |
| Native designer skeleton | done | `BusinessFormDesigner.vue`, `formDesignerSchema.js`, `forge-form-designer/*` | 新版三栏画布骨架接入，旧 FormCreate 画布保留回退 |
| Validation | done | `test-spec.md`, `execution-log.md` | eslint、pnpm build、git diff --check 通过 |

## 12. 审查结论

待 `/review lowcode-form-designer-smooth-dnd` 执行。

## 13. 确认记录（HARD-GATE）

- **确认时间**：待确认
- **确认人**：待确认

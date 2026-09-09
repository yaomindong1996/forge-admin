# 低代码平台治理 — 分阶段执行清单

> 对应 spec: lowcode-platform-architecture/spec.md
> 执行 agent 按此清单逐 Phase 推进

---

## Phase 1: H5 多区域渲染 ✅ 已完成

- [x] 新建 `CardSection.vue` — 卡片容器，支持折叠
- [x] 新建 `PillSelect.vue` — 药丸按钮选择器
- [x] 新建 `BottomSheet.vue` — 底部抽屉，封装 AiPopupSheet
- [x] 新建 `PageSectionRenderer.vue` — 分区渲染器
- [x] 修改 `LowcodeField.vue` — 增加 pillSelect 分支
- [x] 修改 `LowcodeForm.vue` — 增加 pillSelect 选项 + inline_grid 布局
- [x] 修改 `lowcode-runtime.vue` — 接入 PageSectionRenderer + handleBottomAction
- [x] 扩展 `lowcode-runtime.js` — 增加 6 个工具函数
- [x] 新建 `V1.0.109__add_presale_h5_page_sections.sql`

### 已知待修复

- [x] 图标解析复核：`CardSection.vue` 使用文本指示符；`PageSectionRenderer.vue` 的 `chevron-right` 由 `AiIcon` 映射到本地 SVG
- [x] 编译验证：`pnpm build:h5` 通过

---

## Phase 2: 设计器分区编辑器（P0）

**目标**：用户在表单设计器中可视化配置 pageSections 和 bottomBar

### 2.1 页面分区面板

- [x] 扩展 `normalizeFormDesignerSchema`，无损保留并规范化 `pageSections` / `bottomBar`
- [x] 增加协议单测，覆盖预售配置打开、保存、重新打开后结构不丢失
- [x] 在 `ForgeFormDesigner.vue` 工具栏增加「页面分区」入口按钮
- [x] 新建 `PageSectionEditor.vue`
  - [x] 分区列表：展示当前 pageSections，支持拖拽排序
  - [x] 新增分区：选择 sectionType（card / child_table）
  - [x] card 分区：从已定义字段中勾选 + 拖拽排序
  - [x] child_table 分区：选择已配置的子表关系 + 选择 displayMode
  - [x] fieldOverrides：在 card 分区内对字段设置组件覆盖
  - [x] visibleInModes：设置 create / edit / detail 可见性，至少保留一个模式
  - [x] collapsible / collapsedByDefault：折叠配置
- [x] 保存逻辑：写入 `formDesignerSchema.pageSections`
- [x] 加载逻辑：从 `formDesignerSchema.pageSections` 回显
- [x] 对失效字段、子表关系、业务动作及不受支持的显示条件给出可见提示，不静默改写用户配置

### 2.2 底部操作栏编辑器

- [x] 在分区编辑器中增加「底部操作栏」区域
- [x] 按钮列表增删 + 排序
- [x] 按钮类型选择（save / reset / action / cancel）
- [x] type=action 时绑定已定义的业务动作
- [x] displayCondition 设置（字段值条件表达式）
- [x] confirmText / successMessage 设置
- [x] 保存逻辑：写入 `formDesignerSchema.bottomBar`
- [x] 显示条件使用字段 / 操作符 / 值配置，输出受控表达式，不要求普通用户手写表达式

### 2.3 属性面板发现性优化

- [x] 在 `ForgePropertyPanel.vue` 的 Tab 列表中增加一级「事件」Tab
- [x] 「事件」Tab 内容直接展示 `FieldEventRulesEditor`
- [x] 原有「表单属性」Tab 中的「表单事件与生命周期」折叠项保留，标记为「高级」
- [x] 右侧属性面板默认展开改为 `rightOpen = ref(true)`

---

## Phase 3: 业务动作支持 CALL_API（P1） ✅ 已完成

**目标**：业务动作内部可发起外部 HTTP 请求

### 3.1 后端

- [x] 增加 `CALL_API` 步骤协议和受控配置校验（仅允许 `EXTERNAL_API` 查询源）
- [x] 新建 `CallApiActionStepExecutor`，从表单数据、记录和运行上下文构建参数并执行外部 API
  - [x] 从步骤配置读取查询源 key
  - [x] 复用 `paramMappings` / `resultMappings` 协议
  - [x] 结果按映射回填步骤上下文或表单数据
  - [x] 失败处理策略：`THROW` / `LOG_AND_CONTINUE`
- [x] 在业务动作执行链路中注册 CALL_API 类型并记录调用关联 ID
- [x] 发布检查增加 CALL_API 查询源、映射、结果模式和失败策略校验

### 3.2 前端

- [x] 在 `BusinessActionDesigner.vue` 步骤类型选择器中增加 `CALL_API` 选项
- [x] 新建 `CallApiStepConfigPanel.vue` 配置面板
  - [x] 查询源选择（只显示已启用低代码查询源的外部 API）
  - [x] 参数映射和结果映射配置
  - [x] 失败策略、结果取值模式配置
- [x] 在动作设计器步骤配置中挂载 `CallApiStepConfigPanel`

### 3.3 触发器 WEBHOOK 落地

- [x] `TriggerActionConfigPanel.vue` 中 WEBHOOK 类型去掉 `todo: true`
- [x] WEBHOOK 执行复用 `CallApiActionStepExecutor`
- [x] WEBHOOK 配置复用 CALL_API 协议和校验
- [x] 触发器执行器中注册 WEBHOOK → CALL_API 映射

---

## Phase 2.5: 关系与级联拆分（P0） ✅ 已完成

**目标**：对象级只维护关系端点；子表交互进入页面分区；字段级联进入事件 Tab。

- [x] `BusinessRelationDesigner` 增加对象模型模式，隐藏显示/选择器/映射/联动/审批后处理配置
- [x] `PageSectionEditor` 的 child_table 支持内嵌新增/编辑、选择器、字段映射和筛选配置
- [x] `formDesignerSchema.settings.governance.fieldLinkages` 与历史 `linkageSchema` 双向兼容
- [x] 事件 Tab 统一展示查询回填和字段联动，并校验失效字段引用
- [x] H5 在字段值变化时执行清空联动，并向字典/远程选项传递受控筛选上下文

## Phase 2.6: 业务动作迁移到应用级（P1） ✅ 已完成

- [x] 应用设计器入口命名为「动作与增强」，直接维护当前应用对象的业务动作
- [x] 页面 bottomBar / rowActions / toolbarActions 绑定同一动作目录
- [x] 旧对象级动作路由保留兼容，但从独立对象设计器主导航移除
- [x] 独立对象设计器的默认视图不再编辑或保存自定义操作，历史动作仅兼容读取，应用级编辑链路保持不变

---

## Phase 4: H5 运行时消费 pageSchema（P1） ✅ 已完成

**目标**：H5 运行时优先读 pageSchema.zones，打通应用级页面编排

- [x] 在 `lowcode-runtime.js` 的 `parseRuntimeConfig` 中优先解析 `options.pageSchema.zones`
  - [x] 从 form zone 提取 `formDesignerSchema`
  - [x] 回退到 `options.formDesignerSchema`（向后兼容）
- [x] 在 `lowcode-runtime.vue` 中按 zone 顺序渲染 form / actions / list 等已支持区域
- [x] 兼容验证：不带 pageSchema 的应用继续走原有逻辑

## Phase 4.5: 业务流程交互（P1） ✅ 已完成

- [x] 应用设计器增加「业务流程」一级入口
- [x] 流程绑定和应用交互配置分区展示，支持审批按钮、时间轴、节点分区权限和审批后动作
- [x] H5 支持 `flow_action` 底部按钮、流程历史时间轴和节点分区可见/只读策略
- [x] 无流程交互配置的历史应用不发起额外流程请求

## Phase 4.6: 按钮权限（P0） ✅ 已完成

- [x] bottomBar / rowActions / toolbarActions 支持 `permissionKey` 与 `permissionStrategy`
- [x] 管理端编辑器提供权限标识和隐藏/禁用策略配置
- [x] H5 使用当前用户权限控制按钮隐藏或禁用，执行入口再次校验权限
- [x] 兼容既有 `permissionCode` 字段和通配权限

---

## Phase 5: 设计器收敛（P2） ✅ 已完成

**目标**：三个设计器合并为单一入口

- [x] `lowcode-builder.vue` 标记 deprecated 并提供应用中心入口；保留旧路由兼容，不做不可靠的自动重定向
- [x] 在 `application-runtime.vue` 中增加「数据模型」Tab，内嵌 object-designer 的字段/关系/流程/触发器能力
- [x] application-runtime 提供统一的页面、事件、动作、数据模型、设置五个一级入口
- [x] 设计器入口与对象设计器导航可在常用视口和 1024px 窄屏下使用，无横向溢出
- [x] 对象设计器瘦身为基本信息、字段设计、数据模型、默认视图、触发器 5 个主入口
- [x] 旧 form/list/actions/relations/flow-app/permission 路由查询参数映射到新入口，避免书签失效

---

## Phase 6: 种子配置可接管（P2） ✅ 已完成

**目标**：种子 SQL 交付的配置可被设计器加载和修改

- [x] 设计器打开种子应用时，从 `ai_business_object.designer_options` + `ai_crud_config.options` 读取已有配置
- [x] 设计器保存时写回 `ai_crud_config.options` 草稿（而非重新生成 SQL）
- [x] 正式发布时由 `BusinessObjectPublishService` 同步创建 `ai_crud_config_version` 和 `ai_business_object_design_version` 快照；草稿保存不修改版本表
- [x] 首次接管前展示 diff 摘要并要求确认，避免无意覆盖种子配置

---

## 交叉验证清单（每个 Phase 完成后执行）

### 编译验证

```bash
# H5 前端
cd forge-h5-ui && pnpm build 2>&1 | grep -i error

# 管理端前端
cd forge-admin-ui && pnpm build 2>&1 | grep -i error

# 后端
cd forge-server && mvn clean install -DskipTests 2>&1 | grep -i error
```

### 功能验证

- [x] 预售新建页：5 分区 + pill + 底部操作栏（Phase 1 mock E2E + 本轮视觉回归）
- [x] 预售编辑页：操作日志底部抽屉 + 提货/退货动作协议（Phase 1 mock E2E + 迁移契约）
- [x] 向后兼容：不带 pageSections 的应用不受影响（H5 协议单测）
- [x] 设计器配置：分区可视化编辑（Phase 2 后）
- [x] 业务动作 CALL_API（Phase 3 后）
- [x] pageSchema 消费（Phase 4 后）
- [x] 统一应用设计器五个一级入口和种子接管提示（Phase 5-6 后）

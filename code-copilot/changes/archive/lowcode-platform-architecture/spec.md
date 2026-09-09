# 低代码平台整体架构治理方案

> status: apply（Phase 2-6 已完成；真实 MySQL/Flyway/服务联调待业务环境验收）
> created: 2026-08-14
> complexity: 🔴🔴🔴极高
> role: 架构方案与分阶段实施依据

## 一、问题诊断

### 1.1 用户痛点

> "我压根都不知道该怎么配置了，用户更没法用了"

这不是一个 H5 渲染问题，而是**整个低代码平台的配置体验和架构一致性问题**。具体表现为三个层面：

| 层面 | 问题 | 表现 |
|------|------|------|
| **配置入口** | 3 个并行的设计器互不衔接 | 用户不知道该用哪个，配置了 A 设计器但运行时读不到 |
| **配置发现性** | 核心功能藏 4 层深 | 字段查询事件在「表单设计器→右侧属性栏→取消选中→表单属性→表单事件与生命周期」里 |
| **设计→运行断裂** | 设计器产出的配置 H5 运行时不认 | application-runtime.vue 支持应用级多区域页面，但 H5 运行时只读 formDesignerSchema |

### 1.2 三个并行设计器

| 设计器 | 文件 | 定位 | 运行时是否消费 |
|--------|------|------|---------------|
| 旧版模型驱动 | `lowcode-builder.vue` | 以业务对象为单位，表单+列表+动作直接配置 | H5 运行时读 `options` |
| 应用级页面编排 | `application-runtime.vue` | 以应用页面为单位，支持多 zone 布局 | H5 运行时**不读 pageSchema** |
| 对象级配置 | `object-designer.vue` | 以业务对象为单位，字段+关系+流程+触发器 | 设计态写入 `designer_options` |

**核心矛盾**：application-runtime.vue 产出的 `pageSchema.zones` 在 H5 运行时完全没有被消费。H5 运行时只读 `options.formDesignerSchema`，这导致应用级页面编排形同虚设。

### 1.3 外部接口配置散落四处

| 配置入口 | 文件 | 状态 |
|---------|------|------|
| 外部 API 管理→低代码查询源契约 | `external/manage.vue` | 已落地 |
| 表单设计器→字段查询事件 | `FieldEventRulesEditor.vue` | 已落地，但入口极深 |
| 列表设计器→CALL_API 行/工具栏动作 | `BusinessListDesigner.vue` | 已落地 |
| 触发器→WEBHOOK 动作 | `TriggerActionConfigPanel.vue` | **未落地，只记 TODO** |

用户想配一个"调外围接口"的功能，需要在 4 个地方找，且不同入口的协议不统一。

### 1.4 业务动作能力缺口

`BusinessActionDesigner` 的步骤类型仅支持：`CREATE_RECORD`、`UPDATE_FIELD`、`ADJUST_NUMBER`、`TRANSITION_STATUS`、`ASSERT_RECORD`、`FOREACH`、`DOMAIN_ACTION`、`START_FLOW`。

**缺少 `CALL_API` 步骤类型**——业务动作内部无法发起外部 HTTP 请求。预售场景中"提货→调库存系统扣减"这类需求只能靠触发器 WEBHOOK（还没落地）或列表 CALL_API（粒度不对）实现。

### 1.5 预售功能的现实路径

预售信息登记功能当前的交付路径：

```
V1.0.105 种子 SQL
  → 写死 3 张表 + 3 个业务对象 + 2 个关系 + formDesignerSchema + masterDetailConfig + actions
  → 不经过任何设计器
  → H5 运行时读 options.formDesignerSchema 渲染
```

**问题**：种子 SQL 写死的配置无法通过设计器修改。用户想加一个字段或改一个分区，只能改 SQL 再跑 Flyway。

### 1.6 已完成的 H5 多区域改造

另一个 agent 已完成以下工作（代码已就位）：

| 交付物 | 文件 | 状态 |
|--------|------|------|
| CardSection 组件 | `lowcode/CardSection.vue` | 已创建 |
| PillSelect 组件 | `lowcode/PillSelect.vue` | 已创建 |
| BottomSheet 组件 | `lowcode/BottomSheet.vue` | 已创建 |
| PageSectionRenderer 组件 | `lowcode/PageSectionRenderer.vue` | 已创建 |
| LowcodeField 支持 pillSelect | `lowcode/LowcodeField.vue` | 已修改 |
| LowcodeForm 支持 inline_grid 布局 | `lowcode/LowcodeForm.vue` | 已修改 |
| lowcode-runtime.vue 接入 section 模式 | `pages/lowcode-runtime.vue` | 已修改 |
| lowcode-runtime.js 工具函数扩展 | `utils/lowcode-runtime.js` | 已修改 |
| V1.0.109 种子 SQL | `db/migration/V1.0.109__*.sql` | 已创建 |

**Phase 1 已解决**：H5 运行时支持 `pageSections` + `bottomBar` 多区域单页渲染。
**Phase 2-6 后续已解决**：设计器可视化配置 `pageSections`，应用级 `pageSchema.zones` 已由 H5 消费，并收敛为统一应用设计入口。

---

## 二、目标架构

### 2.1 核心原则

1. **应用优先**：设计的主语是「应用页面」，不是「业务对象」。对象是数据支撑，不是设计面。
2. **渐进披露**：常用配置在主流程上可见，高级配置折叠但不隐藏。用户不需要点 4 次才能找到字段事件。
3. **单一消费方**：H5 运行时（和管理端运行时）从统一协议读取配置，不按设计器来源分叉。
4. **种子可过渡**：种子 SQL 交付的配置可以被设计器接管修改，不是一次性的。

### 2.2 目标架构图

```
┌─────────────────────────────────────────────────────────┐
│                    用户操作入口                            │
│                                                          │
│  应用设计器（唯一入口）                                     │
│  ├── 页面区（zones）                                      │
│  ├── 字段区（从对象引入，不重复定义）                        │
│  ├── 事件区（字段查询事件，一级 Tab 可见）                   │
│  ├── 动作区（业务动作，支持 CALL_API 步骤）                  │
│  └── 页面分区（pageSections，可视化拖拽）                    │
│                                                          │
│  对象设计器（折叠为「数据模型」Tab，挂在应用设计器内）          │
│  └── 字段 / 关系 / 流程绑定 / 触发器                        │
└──────────────────────────┬──────────────────────────────┘
                           │
                    统一发布协议
                           │
┌──────────────────────────▼──────────────────────────────┐
│                    运行时（统一消费方）                     │
│                                                          │
│  H5 运行时                          管理端运行时             │
│  ├── 读 pageSchema                  ├── 读 pageSchema       │
│  ├── zones → sections 映射          ├── zones → 组件渲染     │
│  ├── formDesignerSchema             ├── formDesignerSchema  │
│  │   ├── components                 │   └── ...            │
│  │   ├── pageSections               │                      │
│  │   ├── governance.fieldEvents     │                      │
│  │   └── bottomBar                  │                      │
│  └── masterDetailConfig             │                      │
└──────────────────────────────────────────────────────────┘
```

### 2.3 统一配置协议

所有设计器产出的配置最终汇聚到一个 JSON 协议，H5 运行时和管理端运行时共同消费：

```json
{
  "pageSchema": {
    "pageKey": "ps_presale_order_form",
    "zones": [
      {
        "zoneId": "main_form",
        "zoneType": "form",
        "props": {
          "formDesignerSchema": {
            "schemaVersion": "form-first-v1",
            "components": [],
            "pageSections": [],
            "bottomBar": {},
            "settings": {
              "governance": {
                "fieldEvents": []
              }
            }
          },
          "masterDetailConfig": {}
        }
      },
      {
        "zoneId": "action_bar",
        "zoneType": "actions",
        "props": {
          "actions": []
        }
      }
    ]
  }
}
```

**关键变化**：H5 运行时从只读 `options.formDesignerSchema` 升级为先读 `pageSchema.zones`，再从中提取 `formDesignerSchema`。当 `pageSchema` 不存在时，回退到 `options.formDesignerSchema`（向后兼容）。

---

## 三、分阶段实施计划

### Phase 1：H5 多区域渲染（已完成 ✅）

**目标**：H5 运行时支持 `pageSections` + `bottomBar`，预售登记页按 PRD 原型图渲染。

**交付物**：
- 4 个新组件 + 2 个修改组件 + 1 个修改页面 + 工具函数扩展 + V1.0.109 SQL

**验收标准**：
- 预售新建页显示 5 个分区（导购信息/会员信息/收款信息/备注/商品明细）
- 收款方式显示为 pill 按钮（静态码/现金）
- 操作日志在编辑/详情页以底部抽屉展示
- 底部固定操作栏显示"清空"和"提交"
- 不带 pageSections 的其他低代码应用渲染不受影响

### Phase 2：设计器分区编辑器（P0，已完成 ✅）

**目标**：让用户在表单设计器中可视化配置 `pageSections` 和 `bottomBar`，不再写 SQL。

**改造范围**：`forge-admin-ui`（管理端前端）

#### 2.1 表单设计器增加「页面分区」面板

在 `ForgeFormDesigner.vue` 的工具栏或左侧面板中增加一个「页面分区」入口，打开后是一个分区编辑器：

| 功能 | 说明 |
|------|------|
| 新增分区 | 选择 sectionType（card / child_table），card 时从已定义字段中勾选，child_table 时选择已配置的子表关系 |
| 拖拽排序 | 分区之间可拖拽调整顺序 |
| 字段归组 | card 分区内可拖拽字段调整顺序 |
| 组件覆盖 | 在 card 分区内对字段设置 `fieldOverrides`（如把 dictSelect 改为 pillSelect） |
| 可见性 | 设置 `visibleInModes`（create / edit / detail） |
| 子表展示模式 | child_table 分区选择 `displayMode`（inline_grid / card_list / bottom_sheet） |

**配置存储**：写入 `formDesignerSchema.pageSections`，与现有 `components`、`settings.governance` 同级。

#### 2.2 底部操作栏编辑器

在分区编辑器中增加「底部操作栏」区域：

| 功能 | 说明 |
|------|------|
| 按钮列表 | 增删底部按钮，按顺序排列 |
| 按钮类型 | save / reset / action / cancel |
| 动作绑定 | type=action 时选择已定义的业务动作 |
| 显示条件 | 设置 `displayCondition`（如 `status == DRAFT`） |
| 确认弹窗 | 设置 `confirmText` |
| 成功提示 | 设置 `successMessage` |

**配置存储**：写入 `formDesignerSchema.bottomBar`。

#### 2.3 设计器属性面板发现性优化

将「表单事件与生命周期」从 4 层深提到 2 层：

| 当前路径 | 目标路径 |
|---------|---------|
| 打开右侧属性栏 → 取消选中组件 → 表单属性 Tab → 表单事件与生命周期 | 右侧属性栏 → 事件 Tab（一级 Tab） |

**改造方式**：在 `ForgePropertyPanel.vue` 的 Tab 列表中增加「事件」一级 Tab，直接展示 `FieldEventRulesEditor`。原有「表单属性」Tab 中的折叠项保留但标记为"高级"。

### Phase 3：业务动作支持 CALL_API（P1，已完成 ✅）

**目标**：业务动作内部可以发起外部 HTTP 请求，统一外部接口调用的协议。

**改造范围**：`forge-server`（后端）+ `forge-admin-ui`（管理端前端）

#### 3.1 后端：BusinessAction 步骤增加 CALL_API 类型

| 改造点 | 说明 |
|--------|------|
| 步骤类型枚举 | 在 `BusinessActionStepType` 中增加 `CALL_API` |
| 步骤执行器 | 新增 `CallApiStepExecutor`，调用已注册的外部 API（复用 `EXTERNAL_API` 查询源协议） |
| 参数映射 | 复用现有 `paramMappings` 协议，从表单字段和运行上下文取值 |
| 结果映射 | 复用 `resultMappings` 协议，回填到表单字段或后续步骤上下文 |
| 安全约束 | 只允许调用已注册为低代码查询源的外部 API，禁止任意 URL |

#### 3.2 前端：BusinessActionDesigner 增加 CALL_API 步骤配置

在步骤类型选择器中增加 `CALL_API` 选项，配置面板包含：
- 查询源选择（下拉，只显示已启用低代码查询源的外部 API）
- 参数映射（复用 FieldEventRulesEditor 的映射 UI）
- 结果映射（同上）
- 失败处理策略（抛异常终止 / 记录错误继续）

#### 3.3 触发器 WEBHOOK 落地

在 `TriggerActionConfigPanel.vue` 中把 `WEBHOOK` 类型的 `todo: true` 替换为真实实现：
- 复用 CALL_API 步骤执行器
- WEBHOOK 本质就是"触发器 → 调用外部 API"，不再单独实现

### Phase 4：H5 运行时消费 pageSchema（P1，已完成 ✅）

**目标**：H5 运行时从 `options.formDesignerSchema` 升级为优先读 `pageSchema.zones`，打通应用级页面编排。

**改造范围**：`forge-h5-ui`（H5 前端）

#### 4.1 运行时配置解析升级

在 `lowcode-runtime.js` 的 `parseRuntimeConfig` 中增加 pageSchema 解析：

```
解析顺序：
1. 如果 options.pageSchema 存在且有效 → 从 zones 中提取 formDesignerSchema
2. 如果 options.formDesignerSchema 存在 → 直接使用（向后兼容）
3. 如果都没有 → 回退到 editSchema 旧协议
```

#### 4.2 多 Zone 渲染支持

当 `pageSchema.zones` 包含多个 zone 时，H5 运行时按 zone 顺序渲染：
- `zoneType: "form"` → 渲染表单（含 pageSections）
- `zoneType: "actions"` → 渲染操作按钮区
- `zoneType: "list"` → 渲染子列表
- `zoneType: "chart"` → 渲染图表（后续扩展）

### Phase 5：设计器收敛（P2，已完成 ✅）

**目标**：合并三个设计器为单一入口，对象设计器折叠为「数据模型」Tab。

**改造范围**：`forge-admin-ui`（管理端前端）

#### 5.1 废弃 lowcode-builder.vue

将 `lowcode-builder.vue` 标记为 deprecated，并在页面内提供进入应用中心统一设计入口的提示。保留旧路由和已有草稿编辑能力，不做按 CRUD 配置 ID 推断应用编码的强制重定向，避免旧书签打开错误应用。

#### 5.2 对象设计器内嵌为 Tab

在 `application-runtime.vue` 中增加「数据模型」Tab，内容为 `object-designer.vue` 的精简版：
- 字段管理
- 关系管理
- 流程绑定
- 触发器

#### 5.3 应用设计器成为唯一入口

应用设计器的结构：

```
应用设计器
├── 页面 Tab（默认）
│   ├── 画布区（zones 可视化编辑）
│   ├── 属性区（zone 级配置）
│   └── 预览区
├── 事件 Tab（字段查询事件，一级可见）
├── 动作 Tab（业务动作配置）
├── 数据模型 Tab（对象字段/关系/流程/触发器）
└── 设置 Tab（发布/权限/字典）
```

### Phase 6：种子配置可接管（P2，已完成 ✅）

**目标**：种子 SQL 交付的配置可以被设计器加载和修改。

#### 6.1 设计器加载种子配置

当用户打开一个通过种子 SQL 创建的低代码应用时，设计器从 `ai_business_object.designer_options` 和 `ai_crud_config.options` 读取已有配置，展示在画布上。

#### 6.2 种子配置可编辑

用户在设计器中修改后，保存操作写回 `ai_crud_config.options`（而非重新生成 SQL）。

#### 6.3 发布快照同步

草稿保存只写回当前 `ai_crud_config.options` / 设计草稿，不修改不可变发布版本表；正式发布由 `BusinessObjectPublishService` 统一创建 `ai_crud_config_version` 和 `ai_business_object_design_version` 快照，保证发布版本可审计、可回滚。

#### 6.4 接管确认

首次加载种子配置时展示接管提示和变更摘要，用户确认后才允许覆盖运行时 CRUD 配置；后续保存沿用草稿链路，避免无意覆盖种子配置。

---

## 四、优先级与依赖关系

```
Phase 1: H5 多区域渲染 ✅ 已完成
    │
    ├──→ Phase 2: 设计器分区编辑器（P0，预售可用性的关键）
    │        依赖：Phase 1 的 pageSections 协议
    │
    ├──→ Phase 3: 业务动作 CALL_API（P1）
    │        无依赖，可并行
    │
    ├──→ Phase 4: H5 运行时消费 pageSchema（P1）
    │        依赖：Phase 1 的运行时基础
    │
    ├──→ Phase 5: 设计器收敛（P2）
    │        依赖：Phase 2 + Phase 4
    │
    └──→ Phase 6: 种子配置可接管（P2）
             依赖：Phase 2 + Phase 5
```

**建议执行顺序**：
1. Phase 2（设计器分区编辑器）—— 解决"不知道怎么配"的核心痛点
2. Phase 3 + Phase 4（并行）—— 补齐能力缺口和架构断裂
3. Phase 5 + Phase 6（并行）—— 设计器收敛和种子可接管

---

## 五、预售功能端到端验收路径

预售信息登记是验证整个改造效果的核心场景。以下是端到端验收清单：

### 5.1 新建预售单

| 步骤 | 预期 | 涉及配置 |
|------|------|---------|
| 打开新建页 | 显示 5 个分区卡片，操作日志分区不显示 | pageSections.visibleInModes |
| 导购信息自动回填 | salesUserName/staffNo/storeName 从企微上下文回填 | fieldEvents: wecom_user_store (FORM_LOAD) |
| 输入会员手机号失焦 | memberId/memberName 自动查询回填 | fieldEvents: member_by_mobile (BLUR) |
| 选择收款方式 | 显示 pill 按钮（静态码/现金） | fieldOverrides: payMethod → pillSelect |
| 选静态码 | 显示静态码单号 + 收款信息字段 | runtimeRules: 条件显示 |
| 扫码添加商品 | 商品明细子表新增一行，扫码触发商品查询 | fieldEvents: product_scan_lookup (SCAN_COMPLETE) |
| 手动输入商品编码失焦 | 触发商品信息回填 | fieldEvents: product_manual_lookup (BLUR) |
| 点击底部「提交」 | 弹确认框 → 执行 submit_presale 动作 → 状态变 SUBMITTED | bottomBar.actions[type=action] |

### 5.2 编辑预售单

| 步骤 | 预期 |
|------|------|
| 打开编辑页 | 数据回填，5 个分区 + 操作日志分区显示 |
| 操作日志分区 | 显示为底部抽屉触发器，点击展开查看历史 |
| 提交按钮 | 状态为 DRAFT 时显示，SUBMITTED 后隐藏 |
| 商品明细每行 | 显示「提货」「退货」按钮 |
| 点击提货 | 输入数量 → 执行 record_pickup → 库存数量更新 → 操作日志新增 |

### 5.3 向后兼容验证

| 场景 | 预期 |
|------|------|
| 打开不带 pageSections 的低代码应用 | 走原有渲染逻辑，主表单卡片 + 子表卡片 |
| 打开不带 bottomBar 的低代码应用 | 底部显示默认的取消 + 保存按钮 |

### 5.4 设计器配置验证（Phase 2 完成后）

| 场景 | 预期 |
|------|------|
| 在表单设计器中打开预售表单 | 可视化看到 5 个分区 |
| 拖拽调整分区顺序 | 保存后 H5 运行时按新顺序渲染 |
| 新增一个分区并选字段 | H5 运行时多渲染一个卡片 |
| 在底部操作栏编辑器中增加按钮 | H5 运行时底部多一个按钮 |

---

## 六、配置发现性改进清单

### 6.1 字段查询事件：从 4 层到 2 层

| 当前 | 目标 |
|------|------|
| 右侧属性栏（默认关闭）→ 取消选中组件 → 表单属性 Tab → 表单事件与生命周期折叠项 | 右侧属性栏 → 事件 Tab（一级） |

### 6.2 外部接口调用：统一入口

| 当前散落位置 | 目标统一入口 |
|-------------|-------------|
| 外部 API 管理→低代码查询源契约 | 保留，作为「数据源管理」 |
| 表单设计器→字段查询事件 | 保留，归入「事件 Tab」 |
| 列表设计器→CALL_API 动作 | 保留，归入「动作 Tab」 |
| 触发器→WEBHOOK | 改为触发器→CALL_API（复用 Phase 3 协议） |

### 6.3 设计器工具栏快捷入口

在表单设计器工具栏增加 3 个快捷按钮：
- 「事件」→ 直接展开事件 Tab
- 「分区」→ 直接展开分区编辑器
- 「动作」→ 直接跳转到动作设计器

---

## 七、不在本方案范围

| 事项 | 原因 |
|------|------|
| 管理端 AiCrudPage 运行时支持 pageSections | 管理端以表格为主，不需要多区域卡片布局 |
| 离线草稿在 section 模式下的适配 | 非核心路径，后续迭代 |
| 应用设计器画布区可视化拖拽 zone | 重投入，Phase 5 之后考虑 |
| 多页面应用导航（应用内页面跳转） | 独立特性，另立 Spec |
| 国际化 | 非本方案目标 |

---

## 八、风险与回滚

| 风险 | 影响 | 缓解 |
|------|------|------|
| Phase 4 改动运行时配置解析，影响所有低代码应用 | 全量 H5 运行时故障 | 回退到 `options.formDesignerSchema` 读取路径 |
| Phase 2 设计器写入 pageSections 格式与运行时不匹配 | 配置后渲染异常 | 设计器保存前做协议校验 |
| Phase 5 设计器收敛影响现有路由 | 用户书签失效或打开错误应用 | 旧路由标记 deprecated 并保留草稿编辑，同时提供应用中心入口，不做不可靠的自动重定向 |
| 种子 SQL 修改后被设计器覆盖 | 种子配置丢失 | 设计器保存前做 diff 提示 |

**回滚原则**：
- 已执行的 Flyway 脚本不得修改或回退文件，需要通过后续迁移移除配置
- 前端组件变更通过 git revert 回退
- 设计器变更通过 feature flag 控制（`FORM_DESIGNER_SECTIONS_ENABLED`）

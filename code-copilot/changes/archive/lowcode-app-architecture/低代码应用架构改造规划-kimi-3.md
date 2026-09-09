# 低代码应用架构改造规划

> 目标：以"应用"为唯一入口，收敛对象/应用/平台三层职责，打通"低代码开发 → 流程 → 企业协同"完整闭环。
>
> 现状摸底范围：前端 `forge-admin-ui/src/views/app-center/`、`src/api/business-*.js`；后端 `forge-plugin-generator` business 包；`forge-server/db/migration` 相关脚本。

---

## 一、问题诊断

### 1.1 分层本身没错，错在职责放错了层

正确的分层：

- **对象级（数据资产层）**：对象的固有属性，可被多个应用复用 → 留在业务对象
- **应用级（交付层）**："这个应用怎么跑起来给人用" → 归应用
- **平台级（引擎层）**：Flowable 模型、数据源、编码规则 → 与具体应用无关

现状对照：

| 功能 | 现在在哪 | 应该在哪 | 判断 |
|---|---|---|---|
| 数据结构（字段） | 对象设计器 | 对象 | ✅ 放对了 |
| 表单设计 | 对象设计器 | 对象 | ✅ 放对了 |
| 列表设计 | 对象设计器 | 对象（但应降级） | ⚠️ 有用但不该是一级功能 |
| 关联关系 | 对象设计器 | 对象 | ✅ 放对了 |
| 数据权限 | 对象设计器 permission 面板 | 对象级规则留对象，应用级授权归应用 | ⚠️ 只做了前半 |
| 自动化触发器 | 对象设计器 + 独立页 `/app-center/trigger` + 引擎中心 | 对象内配置 + 应用级汇总 | ⚠️ 三处入口，乱 |
| 业务流程配置 | 对象设计器 flow-app 面板 + 应用级编排器 + 流程中心 | 只留应用级编排器一个入口 | ❌ 三套并存，最大乱源 |

### 1.2 列表设计：有用，但不该与表单设计平级

- 不能删：运行时动态 CRUD（`DynamicCrudController /ai/crud/{configKey}`）渲染列表页依赖 `ai_crud_config.columns_schema/search_schema`。
- 但 90% 场景是"字段全选 + 默认布局"，不需要专门设计。
- 处理：表单设计保存时自动生成默认列表配置；列表设计收进高级配置或表单设计的一个 tab，不再是一级面板。

### 1.3 流程：三套概念并存 + 运行链路未落地（核心病根）

三套流程概念同时存在：

1. 对象设计器 flow-app 面板 → 绑定已发布 Flowable 模型（`BusinessFlowBindingPanel.vue`），且后端有两条重复绑定入口（`BusinessFlowController /binding/{objectCode}` 与 `BusinessTriggerController /flow/binding/{objectCode}`）
2. 应用级业务流程编排器（`/app-center/business-process/:processId`，第三套自研节点 schema `businessProcessJson`）
3. 流程中心 BPMN / DingFlow 双设计器

硬伤：**编排器运行链路没落地**——`ai_business_process_run` / `_node_run` 表已建，但 `BusinessProcessController` 没有 start/runs/callback 端点（变更 `application-business-process-orchestrator` 仍在 apply 阶段）。**这是"搭不出完整应用"的直接技术原因：流程设计完发布不了、跑不起来。**

### 1.4 数据权限：缺应用级授权

- 对象级数据规则（本人字段、组织字段，存 `modelSchema.policies`）→ 已有，留对象 ✅
- 应用级"哪个角色能看哪些入口、能对哪些对象做什么操作" → **缺失**：应用工作台 `permissions` section 只是硬编码引导文案（`application.[applicationCode].vue:114-137`）

### 1.5 串不出完整应用的四个断点

1. **入口分裂**：对象有三个视角并存——旧对象详情页 `object.[objectCode].vue`、对象设计器、应用工作台 objects 面板，外加 `suite.[suiteCode].vue` 老链接可进。
2. **发布两套**：对象有自己的 publish-check/publish/versions，应用又有自己的 publish/versions/publish-runs，级联关系不清晰。
3. **流程运行链路缺后端**（见 1.3）。
4. **使用侧闭环缺失**（见 1.6）。

### 1.6 闭环缺口：低代码 → 流程 → 企业协同

完整闭环应为：

```
应用中心设计 → 发布为菜单入口 → 用户从入口发起单据
→ 触发业务流程（Flowable）→ 待办进工作台(/workspace/todo)
→ 审批通过回写业务数据 → 触发器/消息通知相关人
```

现状断在两处：

- **设计侧断**：编排器发布后跑不起来，"发起单据 → 触发流程"走不通。
- **使用侧断**：待办/已办已迁到 `/workspace/*`，但应用运行时页面与工作台待办无联动——发起人在应用内看不到流程进展，审批人不是从应用上下文进待办。

---

## 二、目标信息架构

```
应用中心（唯一入口）
└── 应用工作台
    ├── 业务对象 ──→ 对象设计器（数据结构 / 表单 / 列表(降级) / 关联 / 对象级权限）
    ├── 业务流程 ──→ 编排器（唯一流程配置入口，审批节点引用 Flowable 模型）
    ├── 自动化 ────→ 触发器汇总视图（编辑跳对象内面板）
    ├── 入口与菜单 → 发布为菜单 / 移动端入口
    ├── 权限 ──────→ 应用级角色-入口-动作授权（待补）
    └── 发布 ──────→ 应用发布级联校验对象就绪度

使用侧：
    应用入口（运行时）←→ 工作台待办 / 我发起的（同一 businessKey 串联）
```

各维度职责收口：

| 功能 | 收口位置 | 说明 |
|---|---|---|
| 数据结构 / 表单 / 关联关系 | 对象设计器 | 不变 |
| 列表设计 | 对象设计器（降级为自动生成 + 高级编辑） | 不再是一级面板 |
| 对象级数据规则 | 对象设计器 permission 面板 | 不变 |
| 自动化触发器 | 编辑：对象内面板（唯一）；应用工作台提供只读汇总 | 撤掉独立页和引擎中心入口 |
| 业务流程 | 应用级编排器（唯一配置入口） | 对象 flow-app 面板降级为只读视图 + 跳转 |
| 应用级授权 | 应用工作台 permissions（补实） | 角色-入口-动作 |
| Flowable 模型设计 | 流程中心 | 平台级引擎，不变 |

---

## 三、改造任务（按优先级）

### P0 — 编排器运行链路落地（关键路径）

让"设计流程 → 发布 → 发起 → 审批 → 回写"真正跑通：

1. `BusinessProcessController` 补运行端点：`start` / `runs`（运行实例查询）/ `retry` / `callback`
2. `BusinessProcessService` 落运行逻辑：事件 → `ai_business_process_run` → 节点执行（审批节点经 `FlowClient.startProcess` 调 Flowable，动作节点走既有 `*ActionStepExecutor`）
3. `legacy_source_type` / `legacy_source_id` 迁移写入方 + 旧触发器/绑定幂等迁移校验
4. 前端修死按钮：`business-process.[processId].vue` 未处理 `@open-flow-designer` 事件（`ActionAndApprovalNodeConfig.vue` 上抛到顶层丢失）
5. 触发器 `START_FLOW` 与编排器启动路径统一

### P1 — 流程入口收敛为一条路

1. 对象设计器 flow-app 面板 → 改为"该对象参与的流程"只读视图 + 跳编排器
2. 后端合并重复绑定接口：`BusinessFlowController /binding/{objectCode}` 与 `BusinessTriggerController /flow/binding/{objectCode}` 二选一
3. 旧 `ai_business_binding`（FLOW）绑定向编排器的迁移路径与开关

### P2 — 列表设计降级

1. 表单设计保存时自动生成/同步默认 `columns_schema` + `search_schema`
2. 列表设计从对象设计器一级导航撤下，收进高级配置 tab
3. 保留 `PUT .../layout/list` 接口（运行时依赖不变）

### P3 — 应用级权限补实

1. 应用工作台 permissions section 从引导文案改为真实配置：角色 × 入口可见性 × 对象操作权限
2. 与 `system/role` + `forge-starter-datascope` 的关系在 Spec 中定清（应用授权是分配层，datascope 是执行层）

### P4 — 使用侧闭环打通

1. 应用运行时页面增加"我发起的 / 流程进展"视图（按 businessKey 关联 `/workspace/*` 数据）
2. 发起单据后跳转/嵌入待办上下文，审批人可从应用上下文进待办
3. 审批通过 → 回写业务数据 → 触发器/消息通知链路验证

### P5 — 清理与入口治理

1. 下线/合并旧对象详情页 `object.[objectCode].vue` 与 `suite.[suiteCode].vue`，对象唯一视角 = 应用工作台 → 设计器
2. 明确应用发布与对象发布的级联关系（应用发布校验对象 readiness）
3. 清理死代码：`ApplicationAutomationPanel.vue`（无引用）、`components/form-designer/FormDesigner.vue`（孤儿）、`views/app-center/object/`、`views/app-center/suite/` 空目录
4. 术语统一：`/ai/business/app/*`（实为访问入口）改名或文档明确区分，避免与 `/ai/business/application/*` 混淆
5. 接口路径统一：`FormulaController /api/ai/business/formula` 的 `/api` 前缀与其余 `/ai/...` 对齐

---

## 四、验证标准（完整应用闭环 Demo）

以一个"采购申请"应用验收：

1. 应用中心新建应用 → 新建业务对象（字段 + 表单，列表自动生成）
2. 应用内编排审批流程（引用 Flowable 模型）→ 发布应用 → 生成菜单入口
3. 普通用户从入口发起采购申请 → 触发流程 → 审批人在工作台待办处理
4. 审批通过 → 单据状态回写 → 触发器通知发起人
5. 发起人在应用内可查看"我发起的"流程进展
6. 全程不离开应用中心 + 工作台两个入口即可完成

---

*文档生成：kimi-3*

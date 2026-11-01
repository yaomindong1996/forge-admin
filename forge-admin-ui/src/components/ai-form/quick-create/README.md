# 表单字段快捷新增（quickCreate）

在 `AiForm` / `AiCrudPage` 的 schema 字段上声明 `quickCreate`，可在下拉旁快捷新增关联数据并自动回填选中值。原生表单里的 `DictSelect` 也可开 `quick-create`。

## 内置预设

| preset | 适用字段 | 权限（可覆盖） |
|--------|----------|----------------|
| `systemRole` | `select` + 角色远程选项 | `system:role:add` |
| `systemPost` | `select` + 岗位远程选项 | `system:post:add` |
| `systemOrg` | `treeSelect` / `select` + 组织 | `system:org:add` |
| `systemClient` | `select` + 客户端（`clientCode`） | `system:client:add`（且仅超管可提交） |
| `dictData` | `dictSelect` / `select` / `radio`（字典选项） | `system:dict:add` |

`systemRole` / `systemPost` / `systemOrg` / `systemClient` 弹窗复用对应页的完整 `editSchema` 工厂（见 `views/system/schemas/`），布局与 CRUD 一致（左标签、双列）。**QC 弹窗内字段不再叠加快捷新增**，避免套娃。

弹窗默认宽度：岗位 `960px`，角色 `900px`，组织 `800px`，字典项 `560px`。

## 声明方式

```js
{
  field: 'roleIds',
  type: 'select',
  quickCreate: { preset: 'systemRole' },
}

{
  field: 'postIds',
  type: 'select',
  quickCreate: {
    preset: 'systemPost',
    defaultsFromForm: { orgId: '$form.mainOrgId' },
  },
}

{
  field: 'customerLevel',
  type: 'dictSelect',
  dictType: 'crm_customer_level',
  quickCreate: { preset: 'dictData' },
}

{
  field: 'userType',
  type: 'radio',
  quickCreate: { preset: 'dictData', dictType: 'sys_user_type', linkLabel: '新增类型' },
}
```

原生 `n-form` / 业务抽屉：

```vue
<DictSelect v-model:value="form.type" dict-type="sys_message_type" quick-create />
<!-- 或带文案覆盖： -->
<DictSelect
  v-model:value="form.scene"
  dict-type="sys_code_rule_scene"
  :quick-create="{ linkLabel: '新增场景' }"
/>
```

正式页 schema 工厂可通过 `withFieldQuickCreate: true` 打开字段级入口；快捷弹窗内调用时保持默认 `false`。

## 覆盖策略：哪些字典能开、哪些不能

### 应尽量补齐（本轮已覆盖）

- **AiForm schema**：业务枚举 / 类型类字段配 `quickCreate: { preset: 'dictData' }`（含 `select` / `radio` + 字典 options）。
- **平台 `sys_` 白名单类型**（见 `presets/dictData.js` 的 `PLATFORM_DICT_DATA_QUICK_CREATE_ALLOWLIST`）：用户类型/状态/性别、组织/岗位/角色类型、公告类型、HTTP 方法、打开方式、资源类型、客户端认证相关、文件存储/分组、编码规则分类与场景、Excel 方案类型、消息类型/渠道/发送范围、加密算法、同账号登录策略、打印场景与来源、插件来源、操作/登录/客户端类型、任务触发与 API 范围等。
- **业务字典（非 `sys_`）**：字段上配 `dictData` 即可，或 `DictSelect` 加 `quick-create`。
- **原生 DictSelect 表单**：编辑抽屉/表单项加 `quick-create`；列表筛选栏默认不加。

### 明确跳过（合理例外）

| 跳过项 | 原因 |
|--------|------|
| `sys_enable_disable` / `sys_normal_disable` / `sys_show_hide` 等启停 | 固定 0/1 框架态，表单内加项会破坏开关语义 |
| `sys_yes_no` 及同类是否 | 同上，纯布尔语义 |
| `sys_role_data_scope` 等数据范围 | 与权限引擎硬编码绑定，不可随意扩项 |
| `sys_config_type`（Y/N 内置标记） | 是否系统内置，非可扩展枚举 |
| `ai_status` / `ai_is_default` / `ai_prompt_recommended` 等状态/是否 | 启停或是否类，走开关或固定字典 |
| 行政区划 `regionCode` / `RegionTreeSelect` | 主数据树，在区划管理维护 |
| 列表/卡片筛选栏 DictSelect | 只做过滤，不在筛选项旁新增字典 |
| QC 弹窗内字段 | 防套娃，`withFieldQuickCreate: false` |
| 演示页 `dictDemo` | 示例，不接生产入口 |

也可对个别平台字典字段设 `allowPlatformDict: true` 强制放开（慎用）。

### 权限（硬门控）

| preset | 前端入口权限码 | 后端提交 |
|--------|----------------|----------|
| `dictData` | **必须** `system:dict:add`（或 `system:dict:*` / 超管） | `POST /system/dict/data/add` |
| `systemRole` | `system:role:add` | `POST /system/role/add` |
| `systemPost` | `system:post:add` | `POST /system/post/add` |
| `systemOrg` | `system:org:add` | `POST /system/org/add` |
| `systemClient` | `system:client:add`（且仅超管可打开） | `POST /system/client/add` |

- **无对应 add 权限码时入口不渲染**（不只是禁用）；不以 apiPermissions 的 URL 通配冒充业务 add 权限。
- 超管 `isAdmin` / 权限通配 `*`、`**`、`*:*:*` 可见。
- Host 提交前再次校验权限；最终仍依赖后端 API 鉴权，前端不可绕过。

## UI 约定

- 入口为控件右侧（或 radio 组同行末尾）的**透明底图标按钮**（约 30×30px 点击区，`border-radius: 6px`），内嵌自定义 SVG；默认无实心主题底；hover/focus 显示 Tooltip，文案为 `linkLabel`。样式在 `FormQuickCreateTrigger.vue`。
- **不下发到 n-select 下拉面板 `#action`**，避免与字段旁入口重复。
- 无权限时不渲染入口。
- **字段 `disabled` / `readonly` / 表单详情只读时不渲染入口**（编辑态不可改则无快捷新增）。
- 弹窗仅 Host 底部「取消 / 保存」，内嵌 `AiForm` 隐藏提交/重置；默认左标签 `label-width: 120`。

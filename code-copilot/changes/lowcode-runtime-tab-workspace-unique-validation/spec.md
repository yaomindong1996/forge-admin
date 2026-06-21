# 低代码运行态多页签录入与唯一校验 Spec

## 背景

当前低代码运行态新增/编辑只支持弹窗和抽屉。对客户、商机、订单等高频录入场景，用户希望可以在列表同页打开多个录入页签，切换时保留草稿，避免反复关闭弹窗。同时客户管理需要配置“客户名称不能重复”，这个能力不应只针对客户硬编码，而应成为低代码模型可配置的唯一性约束。

## 目标

- 运行态表单打开方式扩展为 `modal`、`drawer`、`flat`、`tabWorkspace`。
- `flat` 在列表下方展示单个平铺录入面板。
- `tabWorkspace` 在列表同页展示多页签录入工作区，支持多个新增草稿、编辑/详情页签复用、切换保留草稿。
- 页面设计器和表单设计器可以配置新的打开方式，旧的 `modalType` 配置继续兼容。
- 低代码模型支持通用唯一性校验配置，发布后的动态 CRUD 新增/编辑必须在后端强制校验。
- 客户名称唯一通过通用配置表达，校验消息可配置为“客户名称已存在”。

## 非目标

- 本次不实现跨浏览器刷新后的草稿恢复。
- 本次不引入数据库唯一索引自动 DDL；后端运行态先做业务校验，索引能力后续可接 `modelSchema.indexes`。
- 本次不实现复杂表达式校验、跨对象唯一校验或异步前端远程校验。

## 协议设计

运行态页面配置优先读取 `options.formOpenMode`，为空时兼容 `options.modalType`：

```json
{
  "formOpenMode": "tabWorkspace",
  "modalType": "modal",
  "tabWorkspace": {
    "maxTabs": 8,
    "reuseRecordTab": true,
    "closeAfterSave": false,
    "showDirtyMark": true
  }
}
```

模型唯一校验放在 `modelSchema.uniqueConstraints`：

```json
{
  "uniqueConstraints": [
    {
      "name": "uk_customer_name",
      "fields": ["customerName"],
      "scope": "TENANT",
      "normalize": ["trim"],
      "ignoreBlank": true,
      "message": "客户名称已存在"
    }
  ]
}
```

兼容读取 `modelSchema.validationRules` 中 `type=UNIQUE` 的旧/扩展写法，便于后续设计器渐进接入。

## 交互规则

- `modal` 和 `drawer` 保持现有行为。
- `flat` 每次新增/编辑/详情只保留一个平铺面板，切换动作会替换当前面板。
- `tabWorkspace`：
  - 新增每次打开一个新草稿页签。
  - 编辑/详情同一条记录默认复用已有页签。
  - 切换页签不提示未保存，草稿保留在对应页签内。
  - 关闭存在变更的页签时提示确认。
  - 保存成功后刷新列表；默认保留页签并清除脏标记，可配置保存后关闭。

## 后端校验规则

- 新增时按唯一约束字段取提交值，应用 `trim` 等归一化后查询当前租户/未删除数据是否已存在。
- 编辑时同样校验，但排除当前记录 `id`。
- `ignoreBlank=true` 时，任一参与唯一约束字段为空则跳过该约束。
- 字段必须存在于 `modelSchema.fields` 并解析到真实表列；列名必须经过现有动态仓库标识符校验。
- 违反约束时抛出 `BusinessException`，使用约束配置的 `message`，缺省为“字段值已存在”。

## 验收标准

- 运行态配置 `formOpenMode=flat` 时，点击新增/编辑在列表下方出现平铺表单。
- 运行态配置 `formOpenMode=tabWorkspace` 时，点击新增可以打开多个新增页签，编辑同一记录复用页签，页签切换保留表单输入。
- 弹窗/抽屉旧配置不受影响。
- 客户对象配置 `customerName` 唯一后，新增重复客户名称失败，编辑为其他已有客户名称失败，编辑自身名称不失败。
- 前端构建或针对性 lint 通过；后端 generator 模块编译通过。

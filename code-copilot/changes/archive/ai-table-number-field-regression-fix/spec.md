# AiTable 选中态与数字字段类型回归修复

> status: complete
> created: 2026-07-18
> complexity: 🟡中等

## 1. 背景与目标

本变更修复两个共享前端基础能力缺陷：

1. `AiTable` 在排序列、固定选择列或固定操作列存在时，勾选行的背景被不同单元格背景规则覆盖，导致同一选中行出现断层或颜色不一致。
2. `AiFormItem` 只识别 `number`、`inputNumber`，页面 `editSchema` 中的 `input-number` 会回退为普通 `n-input`，数字约束和 `min/max/step` 等属性失效。

完成后应达到：

- AiTable 选中行在普通列、排序列、固定选择列、固定操作列中使用连续一致的主题色背景，悬停时仍保持选中语义。
- 页面传入的自定义 `row-class-name` 不被选中态实现覆盖。
- AiForm 共享链路兼容 `number`、`inputNumber`、`input-number` 三种历史写法。
- `src/views` 内 `editSchema` 统一使用规范写法 `number`，不再出现 `type: 'input-number'`。

## 2. 研究结论

### 2.1 AiTable

- `AiTable.vue` 对固定左列、固定操作列、排序列和悬停状态分别设置背景，其中多处使用 `!important`。
- `checked-row-keys` 只传给 Naive UI 选择框，表格没有为选中数据行增加统一状态类。
- 修复应从行状态统一建模，不针对某一列增加局部补丁。

### 2.2 数字字段

- `AiFormItem.vue` 的数字分支仅判断 `number`、`inputNumber`。
- `AiForm.vue` 的必填校验、`AiCrudPage.vue` 的回填转换、`AiCustomQuery.vue` 的查询控件识别也分别硬编码数字类型。
- 当前工作区静态扫描实际发现 `20` 处 `type: 'input-number'`，分布在 `16` 个 `src/views` 文件；用户报告的 `37` 处、`25` 个文件应来自更早代码基线，本轮以当前工作区为准全部清理。
- 项目文档已将 `number` 作为 AiForm 数字字段标准类型，因此页面统一改为 `number`；共享组件保留历史别名兼容。

## 3. 功能要求

- [x] AiTable 根据当前 `checked-row-keys` 为行追加 `ai-table-row--checked`。
- [x] 自定义字符串或函数形式的 `row-class-name` 与选中态类名合并。
- [x] 选中态背景覆盖普通、排序、固定左/右列，并支持明暗主题与用户主色。
- [x] 新增统一数字字段类型判断工具，避免共享组件继续散落硬编码。
- [x] AiFormItem、AiForm、AiCrudPage、AiCustomQuery 全链路识别 `input-number`。
- [x] 将 `src/views` 当前 20 处错误类型统一改为 `number`。
- [x] 增加纯函数回归测试和静态零残留检查。

## 4. 影响范围

- `forge-admin-ui/src/components/ai-form/` 共享表格、表单、CRUD 与高级查询组件。
- `forge-admin-ui/src/views/{system,ai,data,message,flow}` 中使用数字编辑字段的页面。
- 不修改后端接口、数据库结构、权限或业务数据。

## 5. 技术决策

- 页面配置标准类型固定为 `number`。
- `inputNumber`、`input-number` 作为输入兼容别名，不作为新增页面的推荐写法。
- AiTable 选中态只由受控的 `checked-row-keys` 决定，不改变行点击行为，不擅自将任意单元格点击转换为勾选。
- 背景色使用 `color-mix` 混合当前主题主色和表格背景，避免固定浅色值破坏暗色主题。

## 6. 验收标准

- [x] 先点击可排序列，再勾选任意行，该行所有可见单元格背景连续一致。
- [x] 横向滚动时，固定选择列和固定操作列保持同一选中背景。
- [x] 取消勾选后恢复排序、斑马纹和悬停的既有背景行为。
- [x] `type: 'input-number'` 能渲染 `n-input-number` 并接受 `min/max/step`。
- [x] 当前 `src/views` 中 `rg -n "type:\\s*['\"]input-number['\"]"` 无输出。
- [x] 定向 Vitest、目标 ESLint、前端生产构建与差异空白检查通过。

## 7. 执行结论

- 共享组件和页面配置修复完成，当前工作区 20 处错误类型已全部归一。
- 浏览器在 `/system/config` 完成“排序后勾选行”与数字字段 `min: 0` 实测，控制台和页面错误均为 0。
- 详细命令、构建警告与跳过项见 `execution-log.md`。

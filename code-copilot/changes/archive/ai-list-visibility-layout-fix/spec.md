# AI 列表可见性与通用布局修复

> status: done
> created: 2026-07-21
> complexity: 🟢简单

## 1. 背景与目标

`/ai/prompt-template` 与 `/ai/dashboard-generate-record` 接口已有数据，但表格正文区域不可见；无数据时，共享空状态也无法显示。目标是让两个页面恢复 Forge 通用 `AiCrudPage` 列表的完整高度、数据行、分页与空状态表现。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- `forge-admin-ui/src/views/ai/prompt-template.vue`：提示词模板列表入口，调用 `GET /ai/prompt-template/page`。
- `forge-admin-ui/src/views/ai/dashboard-generate-record.vue`：大屏生成记录列表入口，调用 `GET /ai/dashboard-generate-record/page`。
- `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`：解析分页 `records/total`，并把数据交给 `AiTable`。
- `forge-admin-ui/src/components/ai-form/AiTable.vue`：使用 Naive UI `NDataTable` 的 Flex 高度模式，并提供统一 `NEmpty` 空状态。

### 2.2 现有实现

- 两个页面根容器仅设置 `min-height: 100%`，没有为依赖 Flex 高度的 `AiCrudPage` 提供明确高度。
- 两个页面额外传入 `max-height="calc(100vh - 300px)"` 和固定 `scroll-x`。
- 通用正常页面（如 `views/ai/provider.vue`、`views/system/excel-export-config.vue`）根容器使用 `height: 100%`。
- `AiCrudPage` 已支持 MyBatis-Plus `Page.records`，数据协议无需调整。

### 2.3 发现与风险

- `AiCrudPage`、`AiTable` 和 `NDataTable flex-height` 需要从父容器获得可计算高度；当前 `min-height` 会使表格正文 Flex 区域塌陷，数据行与空状态一起不可见。
- 固定 `scroll-x` 与共享组件按列宽自动计算的滚动宽度重复，页面列变化后容易产生固定列和正文宽度错位。
- 本次不修改共享组件，风险限定在两个页面的布局恢复。

## 3. 功能点

- [x] 提示词模板列表恢复明确的表格正文高度，已返回数据、分页和无数据提示可进入可见区域。
- [x] 大屏生成记录列表恢复明确的表格正文高度，已返回数据、分页和无数据提示可进入可见区域。
- [x] 两个页面复用通用列表的高度与横向滚动计算，不保留页面级冲突配置。

## 4. 业务规则

- 保持现有查询条件、CRUD 操作、详情/预览弹窗和权限行为不变。
- 空状态沿用 `AiCrudPage/AiTable` 的统一文案与样式。

## 5. 数据变更

无数据库变更。

## 6. 接口变更

无接口变更。

## 7. 影响范围

- `forge-admin-ui/src/views/ai/prompt-template.vue`
- `forge-admin-ui/src/views/ai/dashboard-generate-record.vue`

## 8. 风险与关注点

- 需确认页面所在主内容区本身提供明确高度；现有通用列表已经使用相同约定。
- 横向列较多时应由 `AiCrudPage` 自动计算滚动宽度，操作列继续固定在右侧。

## 8.5 测试策略

- **测试范围**：共享表格空状态回归、目标页面 ESLint、前端生产构建、差异检查；环境允许时进行浏览器验证。
- **覆盖率目标**：本次纯布局修复不设置覆盖率阈值。
- **独立 Test Spec**：是。

## 9. 待澄清

无。用户已明确要求开始实现并指定两个页面。

## 10. 技术决策

- 页面根容器使用明确的 `height: 100%`、`min-height: 0` 和 Flex 纵向布局。
- 移除页面级 `max-height` 与固定 `scroll-x`，复用共享组件的可用高度和列宽计算。
- 不重复实现空状态，不修改后端分页协议。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| 页面布局修复 | 完成 | `prompt-template.vue`、`dashboard-generate-record.vue` | 恢复完整高度链路并移除固定滚动配置 |
| 增量验证 | 完成 | `test-spec.md`、`execution-log.md` | 测试、Lint、构建与静态检查通过；浏览器受沙箱限制 |

## 12. 审查结论

代码改动符合 Forge 通用列表布局：不修改接口和共享表格，两个页面根容器提供明确高度，`AiCrudPage` 占满剩余空间；固定 `max-height/scroll-x` 已清除。定向测试、目标 ESLint、生产构建和差异检查通过。浏览器自动化因当前沙箱禁止 Chromium 注册 MachPort 未执行成功，属于环境限制，需在本机浏览器刷新页面做最终视觉确认。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-21
- **确认人**：用户（明确要求“开始实现”并追加指定页面优化需求）

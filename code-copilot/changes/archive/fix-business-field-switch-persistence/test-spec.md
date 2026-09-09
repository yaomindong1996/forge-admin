# 测试计划 - 业务对象字段切换持久化修复
> status: apply

## P0

- 字段属性脏状态下，导航点击直接交给父级保存协调，不显示错误的“草稿仍保留”确认。
- 父级切换逻辑在离开数据结构前调用当前字段保存，并仅在返回成功后切换。
- 字段保存失败或返回 `false` 时保持数据结构面板。
- 字段属性操作区位于属性栏头部，不依赖页面底部可见性。

## 回归

- 非字段面板的未保存确认保持原行为。
- 顶部全局保存仍可保存当前字段。

## 计划命令

- `pnpm test -- src/views/app-center/components/designer/__tests__/business-field-switch-persistence.spec.js`
- `pnpm exec eslint <本轮修改文件>`
- `pnpm build`
- `git diff --check`

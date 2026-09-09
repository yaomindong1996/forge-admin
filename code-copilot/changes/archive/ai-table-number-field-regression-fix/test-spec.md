# 测试 Spec — AiTable 选中态与数字字段类型回归修复

> status: complete
> created: 2026-07-18

## 1. 增量范围

| 级别 | 场景 | 预期 |
|------|------|------|
| P0 | 三种数字字段类型别名 | 均识别为数字输入，普通 input 不误判 |
| P0 | 自定义行类为字符串/函数 | 原类名保留，并按 checked 状态追加统一选中类 |
| P0 | `src/views` 页面配置 | `type: 'input-number'` 零残留 |
| P1 | AiForm 共享链路 | 数字渲染、必填校验、回填转换和查询控件均使用统一判断 |
| P1 | AiTable 样式 | 选中态规则覆盖普通、排序、固定左/右单元格 |
| P2 | 前端集成 | 目标 ESLint 与生产构建通过 |
| P2 | 浏览器交互 | 排序后勾选行背景连续；数字字段为 NInputNumber 且 min 生效 |

## 2. 执行命令

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec vitest run \
  src/components/ai-form/__tests__/field-type-utils.spec.js \
  src/components/ai-form/__tests__/table-state-utils.spec.js
```

```bash
pnpm exec eslint \
  src/components/ai-form/AiTable.vue \
  src/components/ai-form/AiFormItem.vue \
  src/components/ai-form/AiForm.vue \
  src/components/ai-form/AiCrudPage.vue \
  src/components/ai-form/AiCustomQuery.vue \
  src/components/ai-form/field-type-utils.js \
  src/components/ai-form/table-state-utils.js \
  src/components/ai-form/__tests__/field-type-utils.spec.js \
  src/components/ai-form/__tests__/table-state-utils.spec.js \
  $(rg -l "type:\\s*['\"]number['\"]" src/views/ai src/views/data src/views/flow src/views/message src/views/system)
```

```bash
rg -n "type:\\s*['\"]input-number['\"]" src/views
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

## 3. 浏览器验证

- 先检查本地 5173/8580 服务是否已由用户启动。
- 已启动时，登录后进入任意 AiCrudPage：点击可排序表头，再勾选一行，检查整行背景与固定列连续。
- 打开包含排序/超时/容量等数字字段的编辑弹窗，检查 DOM 为 `.n-input-number`，输入小于 `min` 的值时控件约束生效。
- 不主动启动真实 Admin/数据库；若环境不具备条件，在执行日志明确标记跳过原因。

## 4. 完成标准

- 定向测试、目标 ESLint、生产构建和空白检查通过。
- 静态扫描无错误类型残留。
- 浏览器验证已执行，或有具体且非阻断的跳过原因。
- 所有实际命令、结果、警告和服务清理情况写入 `execution-log.md`。

## 5. 实际结果

| 验证项 | 结果 |
|--------|------|
| 定向 Vitest | passed，4 files / 15 tests |
| 核心改动 ESLint | passed，0 errors；保留 AiForm 既有 1 warning |
| 页面错误类型扫描 | passed，`type: 'input-number'` 零残留 |
| Playwright | passed，排序/选中/固定列背景一致，NInputNumber 与 `min: 0` 生效，0 console/page errors |
| 前端生产构建 | passed，8693 modules，1m35s |
| `git diff --check` | passed |

# 变更日志 — 待办业务字段展示与发起人自选卡片文案

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-08-30 | apply | 待办扩展字段 + 发起人自选卡片文案 | Vitest 24 通过，eslint 通过 |
| 2026-08-30 19:00 | verify | 待办主标题优先显示审批标题 | `pnpm exec vitest run src/views/flow/utils/__tests__/processDisplay.spec.js`：10 tests passed；`pnpm exec eslint src/views/flow/utils/processDisplay.js src/views/flow/utils/__tests__/processDisplay.spec.js`：通过；`git diff --check`：通过。未重复执行完整构建，本轮仅调整标题字段优先级。 |

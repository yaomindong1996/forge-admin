# 增量测试说明

## 本轮范围

- P0：待办列表主标题优先显示 `row.title`，业务摘要保留在副标题区域。
- P1：无审批标题时，原有业务摘要、流程名称和任务名称回退顺序不变。

## 验证命令

```bash
cd forge-admin-ui
pnpm exec vitest run src/views/flow/utils/__tests__/processDisplay.spec.js
pnpm exec eslint src/views/flow/utils/processDisplay.js src/views/flow/utils/__tests__/processDisplay.spec.js
```

## 结果

- Vitest：1 个文件、10 个测试全部通过。
- ESLint：通过，无错误。
- 完整前端构建：本轮跳过；仅修改展示字段优先级，且当前变更已有构建基线。

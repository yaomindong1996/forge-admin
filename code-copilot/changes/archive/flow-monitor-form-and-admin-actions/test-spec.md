# 单测 Spec — 流程实例监控：只读表单与管理员干预入口

> status: apply
> created: 2026-08-29

## 0. 测试原则

- 增量复用当前变更文档，只补本轮差异。
- 前端以 Vitest 覆盖判定函数和只读表单加载调用。
- UI 交互在本地有服务时用浏览器点选验证；无服务时记录跳过。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| 前端 | Vitest + Vue Test Utils |
| 后端 | 本轮无 Java 变更，不跑 Maven 测试 |
| 已有测试风格 | 工具函数 describe/it；组件 mock API |

## 2. 覆盖范围

### P0 — 核心逻辑

#### `monitorAdmin.js`

| 方法 | 场景 | 输入 | 预期结果 |
|------|------|------|----------|
| `canInterveneInstance` | 运行中/激活/挂起 | `running` `active` `suspended` | true |
| `canInterveneInstance` | 已结束 | `completed` `terminated` | false |
| `canMutateRunningInstance` | 仅运行中可回退转派 | `running`/`suspended` | true/false |
| `buildMonitorFormQuery` | 监控列表行 | `{ id, businessKey, processDefKey }` | `processInstanceId` 取 `id`，去掉空值 |

#### `FlowReadonlyFormPanel`

| 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|-----------|----------|
| 有 processInstanceId | `{ processInstanceId: 'p1' }` | `getProcessFormInfo` 返回 dynamic 表单 | 调用只读表单接口，不再请求监控变量接口 |
| 无查询条件 | `{}` | 不发请求 | 展示空状态 |

### 不测试

- ECharts 监控图表。
- 终止/回退/转派后端状态机（已有监控接口，本轮不改）。
- 已办页视觉回归（复用同一组件，用单测和 lint 兜底）。

## 3. 执行计划

- [x] Step 1: 编写 P0 单测
- [x] Step 2: `pnpm exec vitest run` 跑本轮 spec
- [x] Step 3: `pnpm exec eslint` 检查改动文件
- [ ] Step 4: 有 dev 服务时浏览器验证监控详情

## 4. 历史验证基线

无，本变更新建。

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-08-29 | 监控详情表单与干预 | Vitest + eslint + build | 见 execution-log | 通过 | 本地 3000/8580 未启动，未做浏览器点选 |

## 6. 执行证据

见 `execution-log.md`。

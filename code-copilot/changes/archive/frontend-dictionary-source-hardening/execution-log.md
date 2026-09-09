# 执行日志 — 前端字典来源与设计器枚举整改

## 1. 环境

- 分支：`sdd/framework-hardening-phased`
- 仓库：`/Users/yaomindong/Desktop/project/mdframe/forge-project`
- 前端：Vue 3.5 / Vite 7 / Vitest 2.1.9 / pnpm
- 数据库迁移：本阶段脚本为 `V1.0.54`；后续 Controller 安全阶段已新增 `V1.0.55`

## 2. 记录

| 时间 | 阶段 | 操作 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-27 | Proposal | 读取清单、项目规范、记忆、测试标准，盘点目标页面和后端值协议 | passed | 用户已授权持续 `/apply` |
| 2026-07-27 | Proposal | 确认现有字典和 `useDict`/`DictTag`/`DictSelect` 行为 | passed | `useDict` 返回字符串 value，需类型转换 |
| 2026-07-27 | Proposal | 创建四份 SDD 文档 | passed | 状态进入 apply，尚未修改本阶段生产代码 |
| 2026-07-27 | Apply/Task 1 | 新增 `V1.0.54`、字典转换工具和单元测试 | passed | 数字、布尔、协议值映射及空输入覆盖 4 条测试 |
| 2026-07-27 | Apply/Task 2 | 整改 System/Message 字典来源 | passed | Schema 保持 computed，数字/布尔提交类型不变 |
| 2026-07-27 | Apply/Task 3 | 整改 External/Generator 字典来源 | passed | `data_db_type` 映射回历史值；修正 `generator/table.vue` 的 `:id` 占位符 |
| 2026-07-27 | Apply/Task 4 | 整改 Data/Flow 字典来源 | passed | `business` 仅由迁移补齐，前端无业务硬编码 fallback |
| 2026-07-27 | Apply/Task 5 | 整改 App Center 字典来源 | passed | 入口、平台和触发器持久化协议保持 |
| 2026-07-27 | Apply/Task 6 | 收敛标签视觉常量 | passed | 3 个页面统一使用 `src/constants/dict-options.js` |
| 2026-07-27 | Verify | `pnpm vitest run src/utils/__tests__/dict-options.spec.js` | passed，4/4 | Node 20.19.0 |
| 2026-07-27 | Verify | 对 22 个目标 JS/Vue 文件执行 `pnpm exec eslint` | passed，exit 0 | 未执行全仓 `lint:fix`，避免无关格式化 |
| 2026-07-27 | Verify | 检查 Flyway 最高版本、`${...}`、显式列、两处 `WHERE NOT EXISTS`、`tenant_id=1`、PATCH、business 和 `CHAR(92)` 正则拼接 | passed | `V1.0.54` 为当前最高版本，未发现 Flyway placeholder 或 tenant 0 seed |
| 2026-07-27 | Verify | 扫描目标页面字面量 options | passed with boundary items | 仅剩动态接口数据、顶级资源占位、状态映射透传及 `dictData` 的视图模式/字典状态/是否默认本地控件 |
| 2026-07-27 | Verify | `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | passed，约 2m14s | 既有 `UserSelectModal` 命名冲突、CSS `//` 注释、动态/静态 import chunk 警告，不阻断构建 |
| 2026-07-27 | Spec Review 1 | 独立 Reviewer 逐条核验 Spec 和实际代码 | FAIL：0 Critical、6 Important | 行权限/入口类型异步加载、数字状态、两个触发器协议值、流程状态和资源类型文案需修复 |
| 2026-07-27 | Review Fix | 修复 6 个 Important | completed | 新增字典值安全归一化；行权限归一化前显式加载字典且失败时阻断保存；补 `FLOW_CANCELED/SKIPPED`；流程/资源类型文案改用字典 |
| 2026-07-27 | Review Fix Verify | `pnpm vitest run src/utils/__tests__/dict-options.spec.js`；对 6 个修复 JS/Vue 文件执行 ESLint；`rg` 检查新增协议值 | passed：Vitest 5/5，ESLint exit 0，关键值存在 | 尚未执行修复后的生产构建，待 Spec 复审通过后聚合复跑 |
| 2026-07-27 | Review Fix Build | `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | passed，约 2m10s | 既有组件命名、CSS 注释和动态/静态 import chunk 警告保持，不阻断构建 |
| 2026-07-27 | Spec Review 2 | 独立 Reviewer 复核首次 6 项修复和完整 Spec | FAIL：0 Critical、1 Important | `AppEditorDrawer` 初始化后的 `entryMode` watcher 仍可能覆盖合法存量 `entryType` |
| 2026-07-27 | Review Fix 2 | 为抽屉恢复数据周期增加 hydration guard，并复跑目标 ESLint | passed，exit 0 | `mountTarget`/`entryMode` watcher 只在用户交互期重新推导，不再覆盖 hydration 结果 |
| 2026-07-27 | Spec Review 3 | 独立 Reviewer 复核 hydration 修复和完整 Spec | FAIL：0 Critical、1 Important | 入口模式/应用模式仍可由硬编码协议值和标签生成，字典缺失会被静默掩盖 |
| 2026-07-27 | Review Fix 3 | 将入口模式和应用模式改为字典唯一候选源 | completed | 挂载目标兼容矩阵仅过滤字典项，不再生成 fallback；应用模式直接映射字典 label/remark |
| 2026-07-27 | Review Fix 3 Verify | `pnpm vitest run src/utils/__tests__/dict-options.spec.js`；22 个目标文件 `pnpm exec eslint` | passed：Vitest 5/5，ESLint exit 0 | ESLint 首次发现修复时遗留的多余闭合符号，修正后完整复跑通过 |
| 2026-07-27 | Review Fix 3 Build | `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | passed：8727 modules，约 1m42s | 既有 `UserSelectModal` 命名冲突、CSS `//` 注释和动态/静态 import chunk 警告保持，不阻断构建 |
| 2026-07-27 | Spec Review 4 | 独立 Reviewer 复核完整 Spec 和第三轮修复 | FAIL：0 Critical、2 Important、1 Minor | 缺少应用模式 seed；五处状态回显仍硬编码；异步 computed 缺直接测试 |
| 2026-07-27 | Review Fix 4 | 补齐应用模式字典、五处状态回显和异步 computed 测试 | completed | `V1.0.54` 新增 `ai_business_app_mode` 及两个协议值，状态回显统一消费字典元数据 |
| 2026-07-27 | Review Fix 4 Verify | `pnpm vitest run src/utils/__tests__/dict-options.spec.js`；22 个目标文件 ESLint；Flyway placeholder 与应用模式 seed 静态检查 | passed：Vitest 6/6，ESLint exit 0，静态检查通过 | 未连接真实数据库，seed 结果仍属于部署门禁 |
| 2026-07-27 | Review Fix 4 Build | `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | passed：8727 modules，约 1m42s | 仅既有组件命名、CSS `//` 注释和 chunk 警告，不阻断构建 |
| 2026-07-27 | Spec Review 5 | 独立 Reviewer 复核第四轮修复和完整 Spec | FAIL：0 Critical、3 Important | 数据集数字筛选、硬编码 fallback 和详情 hydration 仍有缺口 |
| 2026-07-27 | Review Fix 5 | 修复 `dataset.vue` 三项缺口并增加源码合同 | completed | 不改变接口字段和持久化协议，未知值保留原值 |
| 2026-07-27 | Review Fix 5 Verify | Vitest、目标 ESLint、生产构建 | passed：9/9、ESLint exit 0、8727 modules，约 1m17s | 仅既有组件命名、CSS 注释和 chunk 警告 |
| 2026-07-27 | Spec Review 6 | 独立 Reviewer 复核第五轮修复和完整 Spec | FAIL：0 Critical、1 Important | 数据集列表类型标签仍硬编码“单表/SQL”及样式 |
| 2026-07-27 | Review Fix 6 | 列表类型标签改用 `DictTag + datasetTypeOptions`，增加区段源码合同 | completed | 不改变数据集类型协议或来源表/SQL 分支行为 |
| 2026-07-27 | Review Fix 6 Verify | Vitest、`dataset.vue`/测试 ESLint、生产构建 | passed：10/10、ESLint exit 0、8727 modules，约 1m30s | 仅既有组件命名、CSS 注释和 chunk 警告 |
| 2026-07-27 | Spec/Code Quality Review | 独立复审最新工作树 | Spec PASS；Code Quality 首次 FAIL，1 Important | 缺少 `ai_business_app_entry_mode` 可部署 seed |
| 2026-07-27 | Final Review Fix | `V1.0.54` 补入口打开方式类型和 6 个协议值，增加迁移源码合同 | completed | 脚本尚未在任何目标数据库执行 |
| 2026-07-27 | Final Verify | 目标 Vitest；Flyway placeholder/版本；SDD 行尾/EOF；敏感模式；`git diff --check` | passed：11/11；静态检查通过 | 未重跑 build、ESLint、MySQL 或浏览器 E2E |
| 2026-07-27 | Code Quality Re-review | 独立 Reviewer 定向复核上轮唯一 Important | PASS | 无剩余代码阻塞项 |

## 3. 环境限制与部署门禁

- 未连接真实 MySQL，`V1.0.54` 只能先做静态检查。
- 未启动后端服务，无法验证真实字典 API。
- 浏览器清缓存后的加载、回显和保存 E2E 必须在 Flyway 成功后执行。
- 当前工作树包含验证码、密钥生命周期、客户端凭据和 Controller 边界阶段的未提交变更，禁止批量暂存或覆盖。

## 4. 剩余门禁

- 在隔离 MySQL 8 执行 Flyway，确认 `forge_schema_history` 的 `1.0.54` 和字典 seed。
- 在后端字典 API 可用后执行浏览器加载、编辑回显、搜索和保存 E2E。

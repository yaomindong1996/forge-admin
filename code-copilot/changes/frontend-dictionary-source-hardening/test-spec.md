# 测试 Spec — 前端字典来源与设计器枚举整改
> status: apply
> created: 2026-07-27

## 0. 测试原则

- 复用当前前端 Vitest/Vite 基线，只做本轮增量验证。
- 先验证纯函数 Red，再实现 Green；页面改造以构建、Lint 和静态合同为主。
- 不把未执行的真实 Flyway 或浏览器 E2E 记为通过。
- 所有命令、结果、警告和跳过项写入 `execution-log.md`。

## 1. P0 覆盖

| 场景 | 输入 | 预期 |
|------|------|------|
| 数字字典转换 | `value='1'/'0'` | 输出 `1/0`，保留标签和样式元数据 |
| 布尔字典转换 | `value='1'/'0'` 或 `true/false` | 输出严格布尔值 |
| 协议值映射 | `MYSQL` 等字典值 | 输出生成器当前兼容值，未知值按原值保留 |
| 空字典 | `null/undefined/[]` | 返回空数组，不抛异常 |
| 异步 Schema | 字典挂载后更新 | computed options 随 `dict.value` 更新 |

## 2. P1 静态合同

- `V1.0.54` 在本阶段尚未部署，可在首次 Flyway 执行前补齐缺失 seed；一旦进入 `forge_schema_history` 则禁止继续修改。
- `sys_dict_type` / `sys_dict_data` INSERT 使用显式列名。
- 所有 seed 的 `tenant_id=1`，类型和值分别用 `NOT EXISTS` 防重复。
- `sys_req_method=PATCH`、`flow_process_form_type=business` 存在。
- Data 脱敏正则在 SQL 中正确转义，静态预期值与前端读取值一致。
- 目标 AiCrudPage Schema 依赖字典时均为 computed。
- 目标页面不再声明已迁移的运行时业务 options 数组。
- 三处标签类型统一从 constants 导入。

## 3. P1 构建与测试

- `pnpm vitest run src/utils/__tests__/dict-options.spec.js`。
- 对目标文件运行 ESLint，不执行全仓自动修复。
- `pnpm build`。
- `git diff --check`、SDD 尾随空白和 EOF 检查。

## 4. P2 部署验证

- 在隔离 MySQL 8 执行 Flyway，确认 `forge_schema_history` 中 `1.0.54` 成功。
- 查询新增字典类型/数据数量并抽查 `dict_value`。
- 清空浏览器缓存，验证目标下拉首次加载、编辑回显、搜索和保存。
- 验证数字/布尔字段请求体类型、生成器数据库类型兼容值、流程 `business` 表单和触发器 JSON 值。

## 5. 历史验证基线

| 时间 | 范围 | 结果 | 备注 |
|------|------|------|------|
| 2026-07-27 | 当前前端测试框架盘点 | Vitest 2.1.9，已有多模块组件/工具测试 | 未在 Proposal 阶段运行全量测试 |

## 6. 本轮增量验证

| 时间 | 变更范围 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|----------|------|-----------|
| 2026-07-27 | 字典转换工具 | `pnpm vitest run src/utils/__tests__/dict-options.spec.js` | Review 修复后 5/5 passed | 新增字典未加载时保留持久化值合同 |
| 2026-07-27 | 22 个目标 JS/Vue 文件 | `pnpm exec eslint <目标文件>` | exit 0，零问题 | 未对全仓执行自动修复 |
| 2026-07-27 | Flyway 与硬编码合同 | 版本、placeholder、`NOT EXISTS`、租户、关键值、正则和目标 options `rg` 扫描 | passed | 剩余 options 为动态数据源、顶级资源占位及 `dictData` 页面本地控件 |
| 2026-07-27 | 前端生产构建 | `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | passed | 存在既有组件命名、CSS 注释和 chunk 警告，不阻断构建 |
| 2026-07-27 | 第三轮 Review 修复 | Vitest、22 个目标文件 ESLint、生产构建 | passed：5/5、ESLint exit 0、8727 modules | 入口模式/应用模式不再使用硬编码业务选项 fallback；既有构建警告不阻断 |
| 2026-07-27 | 第四轮 Review 修复 | Vitest、22 个目标文件 ESLint、Flyway seed/placeholder 静态检查、生产构建 | passed：6/6、ESLint exit 0、placeholder 无输出、8727 modules | 补齐应用模式字典及五处状态回显；既有构建警告不阻断 |
| 2026-07-27 | 第五轮 Review 修复 | Vitest、`dataset.vue`/测试 ESLint、生产构建 | passed：9/9、ESLint exit 0、8727 modules | 覆盖数字发布状态、无硬编码 fallback、分类状态字典和详情 hydration；既有构建警告不阻断 |
| 2026-07-27 | 第六轮 Review 修复 | Vitest、`dataset.vue`/测试 ESLint、生产构建 | passed：10/10、ESLint exit 0、8727 modules | 列表类型标签消费 `data_dataset_type` 字典元数据；既有构建警告不阻断 |
| 2026-07-27 | Code Quality 最终修复 | `pnpm vitest run src/utils/__tests__/dict-options.spec.js`；Flyway/SDD/格式静态检查 | passed：11/11；静态检查通过 | 补齐 `ai_business_app_entry_mode` 及六个协议值；按用户要求不重跑 build/E2E |
| 待部署 | P2 真实环境 | MySQL/Flyway、字典 API、浏览器加载/回显/保存 | not run | 部署门禁，不计为通过 |

## 7. 执行证据

- 自动化和静态检查：`execution-log.md`。
- 数据迁移：本地仅静态检查，真实结果待部署环境。
- 服务启动与停止：本阶段默认不启动后端服务；如启动前端预览，必须记录端口和停止状态。

## 8. 第四轮 Spec Review 增量合同

- `V1.0.54` 必须同时声明 `ai_business_app_mode` 类型及 `DYNAMIC_RENDER`、`CODE_DOWNLOAD` 两个值，且保持 `tenant_id=1` 和 `NOT EXISTS`。
- `system/menu.vue`、`app-center/trigger.vue`、`data/dataset.vue`、`external/manage.vue`、`system/config-center.vue` 的目标业务状态回显必须消费字典元数据。
- Vitest 必须证明依赖异步字典的 computed options 在字典到达后从空数组更新为转换后的值。

## 9. 第五轮 Spec Review 增量合同

- `data_dataset_publish_status` 搜索选项必须通过 `toNumberDictOptions` 保持后端 `Integer` 协议。
- 发布状态、访问模式和数据集类型未知值不得伪装成默认业务标签；分类停用标签必须消费 `sys_enable_disable`。
- `beforeRenderDetail` 必须先等待 `ensureRowScopeDictOptions()`，再调用 `prepareDatasetFormData` 组装旧字段格式的 `ruleItems`。

## 10. 第六轮 Spec Review 增量合同

- 数据集列表类型标签必须使用 `DictTag`，并传入 `datasetTypeOptions.value` 和 `row.datasetType`。
- `tableColumns` 区段不得恢复按 `TABLE` 硬编码“单表/SQL”标签和样式的三元表达式。

## 11. Code Quality 最终增量合同

- `V1.0.54` 必须声明 `ai_business_app_entry_mode` 类型。
- 该类型必须完整种入 `RUNTIME`、`ROUTE`、`IFRAME`、`EXTERNAL`、`H5`、`API` 六个现存协议值。
- Vitest 直接检查迁移源码，防止后续删除类型或任一协议值。

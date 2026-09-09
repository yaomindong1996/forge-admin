# 前端字典来源与设计器枚举整改
> status: review
> created: 2026-07-27
> complexity: complex

## 1. 背景与目标

`output/框架问题细化整改清单.md` 指出前端运行时业务选项、状态标签和设计器元数据存在分散硬编码。本变更处理剩余高优先级热点，目标为：

- 运行时业务语义由 `sys_dict_type` / `sys_dict_data` 提供，前端通过 `useDict`、`DictTag` 和响应式 `computed` 消费。
- `dict_value` 保持现有后端实体、枚举、JSON 配置和数据库存储协议，不借字典整改隐式修改业务值。
- 重复的标签样式等强类型 UI 元数据集中到 constants，不塞入运行时字典。
- 动态接口选项、纯页面视图模式、格式化控件元数据和设计器强类型枚举保留为明确豁免，不机械迁库。

## 2. 代码现状（Research Findings）

### 2.1 可复用字典

以下字典已在 `forge-server/db/全量初始化SQL.sql` 或既有 Flyway 中存在：

| 字典 | 当前值 | 使用范围 |
|------|--------|----------|
| `sys_enable_disable` | `1/0` | 启停状态 |
| `sys_yes_no` | `1/0` | 数字或布尔开关，前端按字段类型转换 |
| `sys_show_hide` | `1/0` | 菜单显示状态 |
| `sys_link_open_target` | `_self/_blank` | 链接打开方式 |
| `sys_req_method` | `GET/POST/PUT/DELETE/ALL` | HTTP 方法；本轮补 `PATCH` |
| `sys_resource_type` | `1/2/3/4` | 目录/菜单/按钮/API |
| `data_db_type` | `MYSQL/ORACLE/POSTGRESQL/SQLSERVER` | 数据库类型 |
| `flow_process_form_type` | `dynamic/external/none` | 流程表单类型；本轮补 `business` |
| `sys_message_type` | `SYSTEM/SMS/EMAIL/CUSTOM` | 消息类型 |
| `sys_message_read_status` | `0/1` | 未读/已读 |
| `sys_client_auth_method` | `none/client_secret` | 公共/机密客户端 |

### 2.2 运行时硬编码热点

- `views/external/manage.vue`：认证类型、API Key 位置、状态、请求方法、内容类型、调用状态和调用类型。
- `views/generator/datasource.vue`、`template.vue`、`table.vue`：数据库类型、数据源用途/风险、模板类型/引擎、生成方式和启停值。
- `views/data/dataset.vue`：结果编码、行权限属性/逻辑、字段数据类型、脱敏规则、日期格式和单位。
- `views/system/menu.vue`、`client.vue`、`config-center.vue`：资源/显示/布尔值、登录认证、验证码、加密算法和同账号登录策略。
- `views/flow/design.vue`、`model.vue`：设计器类型、流程表单类型和重复审批模式。
- `views/message/biz-type.vue`、`message-list.vue`：启停、跳转方式、消息类型和阅读状态。
- `views/app-center/trigger.vue`、`integration.vue`、`components/AppEditorDrawer.vue`、`components/AppEntryWizard.vue`：触发器协议和应用入口持久化选项。

### 2.3 重复 UI 元数据

`views/system/dictData.vue`、`views/ai/components/DictConfigPanel.vue`、`views/app-center/components/designer/DocumentStatusMappingTable.vue` 重复维护 Naive UI 标签类型。该值控制前端视觉组件，不属于租户运行时业务字典，适合集中常量。

### 2.4 值协议风险

- `useDict` 返回字符串 `value`；数字、布尔字段必须在 computed 中转换，不能把字符串直接提交给现有接口。
- `data_db_type` 使用大写规范值，而生成器历史页面使用 `MySQL/PostgreSQL` 等展示式值；生成器数据源选项需显式映射为现有存储兼容值。
- 字典异步加载后，AiCrudPage Schema 必须是 `computed`，否则 options 会永久停留在空数组。
- 应用入口和触发器值保存在 JSON 或业务表中，大小写是协议的一部分。

## 3. 功能点

- [x] 新增 `V1.0.54__add_frontend_runtime_dicts.sql`，以显式列名、`tenant_id=1` 和 `NOT EXISTS` 写入运行时字典。
- [x] 给 `sys_req_method` 补 `PATCH`，给 `flow_process_form_type` 补 `business`，不修改历史脚本。
- [x] 新增可复用的字典选项类型转换工具及单元测试。
- [x] System、Message、External、Generator、Data、Flow、App Center 目标页面的运行时 options 改为响应式字典来源。
- [x] 目标页面的状态回显优先使用 `DictTag` 或字典元数据，不保留重复标签映射。
- [x] 三处标签类型定义收敛到统一 constants。
- [x] 建立本轮静态扫描清单，剩余字面量 options 仅允许动态数据、页面本地视图控件或已记录的强类型设计器元数据。

## 4. 业务规则

1. 字典项的 `dict_value` 必须和后端持久化值完全一致，标签和排序可以由字典管理。
2. 数字字段使用数字 options，布尔字段使用布尔 options；转换只发生在前端展示边界。
3. AiCrudPage 的搜索和编辑 Schema 只要依赖字典就必须定义为 `computed`。
4. 表格业务状态使用 `DictTag`；无匹配字典项时组件保留原值作为兼容回显。
5. 动态数据源选项、分页大小、画布缩放、日期控件类型等非业务枚举不迁数据库。
6. 设计器强类型元数据通过 constants 集中维护；不允许为了消灭字面量而让运行时字典控制协议结构。
7. 不改变接口路径、字段名、默认值、提交类型和既有业务状态机。

## 5. 数据变更

| 操作 | 表名 | 内容 | 说明 |
|------|------|------|------|
| 新增内置数据 | `sys_dict_type` | External、Generator、Data、Client、Flow、App Center 运行时字典类型 | `tenant_id=1`，按 `dict_type` 防重复 |
| 新增内置数据 | `sys_dict_data` | 与现有持久化协议一致的字典项 | 按 `tenant_id + dict_type + dict_value` 防重复 |
| 补齐字典项 | `sys_dict_data` | `sys_req_method=PATCH`、`flow_process_form_type=business` | 不更新已存在项，不修改历史迁移 |

本变更不修改业务表结构，不批量重写业务数据。真实数据库迁移失败时通过修正后的后续 Flyway 脚本处理，禁止修改已执行的 `V1.0.54`。

## 6. 接口变更

无后端业务接口变更。前端继续使用 `GET /system/dict/data/list?dictType=...` 加载字典。

## 7. 影响范围

- 数据库：`forge-server/db/migration/V1.0.54__add_frontend_runtime_dicts.sql`。
- 前端公共层：`src/utils/dict-options.js`、`src/constants/dict-options.js` 及单测。
- 前端页面：External、Generator、Data、System、Flow、Message、App Center 的上述目标文件。
- SDD 和总整改清单。

## 8. 风险与关注点

- Flyway 未执行时目标下拉可能为空；部署必须先迁移数据库再发布前端。
- 字符串到数字/布尔转换遗漏会造成查询条件或保存 DTO 类型回归。
- 字典管理员修改 `dict_value` 会破坏协议；内置协议值只允许修改标签、排序、样式和启停状态。
- `data_mask_rule` 包含反斜线正则，Flyway 静态检查和实库回显必须确认转义后值不变。
- 本地不连接真实 MySQL，真实 Flyway 和浏览器字典加载/回显属于部署门禁。

## 8.5 测试策略

- **P0**：字典转换工具覆盖字符串到数字、布尔、值映射及空数据。
- **P1**：Flyway 脚本检查显式列、租户、去重条件、关键协议值和正则转义。
- **P1**：目标 Vue 文件静态扫描，不再出现已列入整改的硬编码 option 声明和状态映射。
- **P1**：目标 Vitest、ESLint 和生产构建。
- **P2**：真实 MySQL/Flyway、清空缓存后的字典加载、编辑回显和保存 E2E，部署环境执行。

## 9. 待澄清

无。用户已授权按分阶段整改方案持续实施。

## 10. 技术决策

1. 运行时业务语义迁字典，视觉元数据和强类型设计器协议迁 constants，两类来源不混用。
2. 新增一个 Flyway 脚本集中种入本阶段字典，便于部署前统一审查和回滚评估。
3. 使用小型纯函数转换字典值，避免各页面重复实现 `Number`/布尔映射。
4. 不提供业务字典硬编码 fallback；迁移缺失应在部署验证暴露，而不是静默回到分散常量。
5. 对需要说明文本的 App Center 选项使用字典 `remark`，保留页面专属交互说明作为非协议 UI 元数据。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Proposal | 完成 | 本变更四份 SDD 文档 | 已按当前工作树和后端值协议盘点 |
| Task 1 | 完成 | `V1.0.54__add_frontend_runtime_dicts.sql`、`dict-options.js` 及测试 | 字典迁移和类型转换基础设施已落地，最终 Vitest 11/11 |
| Task 2 | 完成 | System/Message 5 个目标页面 | 运行时选项和状态回显改用字典，保留客户端凭据和配置密钥安全改动 |
| Task 3 | 完成 | External/Generator 4 个目标页面 | 数据库类型映射保持历史存储值；HTTP 方法保留 PATCH、过滤 ALL |
| Task 4 | 完成 | Data/Flow 3 个目标页面 | 数据元数据、设计器和流程表单类型改用字典，未补造前端业务类型 |
| Task 5 | 完成 | App Center 4 个目标页面 | 触发器和应用入口 JSON 协议值、大小写及默认值保持 |
| Task 6 | 完成 | 统一 constants、3 个消费页面、数据集列表字典标签及聚合验证 | Spec Compliance 和 Code Quality 复审均通过 |

## 12. 审查结论

- 首次 Spec Compliance Review：FAIL，无 Critical，6 个 Important。
- 修复内容：异步字典加载前保留存量值、行权限字典显式等待、数字状态转换、补齐触发器协议值、流程状态和资源类型文案字典化。
- 第二次 Spec Compliance Review：FAIL，无 Critical，1 个 Important；发现抽屉 hydration 后的 watcher 会再次覆盖存量入口类型，已增加初始化周期 guard。
- 第三次 Spec Compliance Review：FAIL，无 Critical，1 个 Important；发现入口模式和应用模式仍可由硬编码值/标签生成 fallback。
- 第四次 Spec Compliance Review：FAIL，无 Critical，2 个 Important、1 个 Minor；发现 `AppEditorDrawer` 消费的 `ai_business_app_mode` 未纳入迁移、五个目标页面仍保留硬编码状态回显，且测试未直接证明异步字典到达后 computed options 会更新。
- 最新修复：`V1.0.54` 补齐 `ai_business_app_mode`、`DYNAMIC_RENDER`、`CODE_DOWNLOAD`；`system/menu.vue`、`app-center/trigger.vue`、`data/dataset.vue`、`external/manage.vue`、`system/config-center.vue` 的目标状态回显改用 `DictTag`/字典元数据；新增异步 computed 字典测试。
- 第五次 Spec Compliance Review：FAIL，无 Critical，3 个 Important；发现数据集发布状态筛选仍提交字符串、详情/分类仍有硬编码业务 fallback，以及只读详情组装行权限前未等待字典。
- 第五次修复：发布状态 options 使用 `toNumberDictOptions`；发布状态、访问模式、数据集类型未知值仅回显原协议值；分类停用标签消费 `sys_enable_disable`；`beforeRenderDetail` 在组装 `ruleItems` 前等待行权限字典。
- 第六次 Spec Compliance Review：FAIL，无 Critical，1 个 Important；数据集列表的类型标签仍按 `TABLE` 硬编码“单表/SQL”。
- 第六次修复：数据集列表改用 `DictTag + datasetTypeOptions`，并新增限定 `tableColumns` 区段的源码合同。
- 第七次 Spec Compliance Review：PASS，无剩余 Critical/Important。
- 首次 Code Quality Review：FAIL，1 个 Important；`AppEditorDrawer.vue` 和 `AppEntryWizard.vue` 依赖的 `ai_business_app_entry_mode` 未纳入可部署迁移。
- 最终修复：在尚未部署的 `V1.0.54` 补齐字典类型及 `RUNTIME/ROUTE/IFRAME/EXTERNAL/H5/API` 六个协议值，并增加迁移源码合同。
- 最终验证：Vitest 11/11；Flyway placeholder、版本唯一性、SDD 行尾/EOF 和 `git diff --check` 通过；未重跑生产构建。
- Code Quality 定向复审：PASS，上一轮唯一 Important 已解除，无代码阻塞项。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-27
- **确认人**：用户
- **确认内容**：用户明确要求“按照你的思路 继续进行”“继续”，授权按已拆分的剩余整改阶段直接进入 `/apply`。

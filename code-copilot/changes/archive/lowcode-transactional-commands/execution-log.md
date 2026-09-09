# 低代码事务型业务命令执行日志

## 1. 基线

- 日期：2026-08-10
- 状态：阶段 4 实施中
- 前置阶段：外部连接器、参数化只读查询源、统一字段事件均已完成。
- 阶段 3 补充生产构建：Node `v20.19.0`，8887 modules transformed，`✓ built in 1m 28s`。
- 用户授权：按既定七阶段路线连续实施，无需阶段间再次确认。
- 工作区已有改动全部保留，不覆盖预售原型/PRD、`.DS_Store`、输出文档及前三阶段改动。
- 本阶段不启动真实服务，不修改真实数据库、Redis、流程或外部系统运行态。

## 2. 差距审计

- 已有：BusinessAction 多步骤执行、TransactionTemplate、RUNNING 幂等预留、请求摘要、动态 CRUD 字段过滤、租户/数据权限、发布快照解析和动作设计器。
- 缺口：普通 `/execute` 读取草稿动作；幂等键可空；日志唯一域不含动作版本；浏览器 context/row 可被步骤读取；更新没有数据库条件/CAS；没有通用多字段原子数值调整；外部副作用与本地写入混在同一“事务”描述中；外接数据源没有明确事务边界。
- 结论：增强现有 BusinessAction，不新增预售专用服务或第二套命令引擎。

## 3. 执行记录

| 时间 | 动作 | 结果 |
|---|---|---|
| 2026-08-10 | 审计 BusinessActionExecutionService、步骤执行器、执行日志、动态 CRUD、发布检查和前端自动化设计器 | 完成 |
| 2026-08-10 | 固定 LOCAL_TRANSACTION / ORCHESTRATION、安全输入、条件更新和 ADJUST_NUMBER 协议 | 完成 |
| 2026-08-10 | 建立第四阶段四份 SDD 文档 | 完成 |
| 2026-08-11 | 完成发布态动作、版本化幂等、可信输入、本地事务、条件更新和 ADJUST_NUMBER | 完成 |
| 2026-08-11 | 完成 AiCrudPage 输入 Schema、页面幂等与最小请求载荷 | 完成 |
| 2026-08-11 | 完成自动化设计器执行模式、输入字段和三类本地数据步骤 | 完成 |
| 2026-08-11 | 修复显式空 inputSchema 被误判为存量兼容协议 | 完成 |

## 4. 验证记录

### 4.1 后端定向测试

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator \
  -Penable-tests \
  -Dtest=DynamicCrudCommandRepositoryTest,BusinessActionCommandPolicyTest,AdjustNumberActionStepExecutorTest,BusinessActionExecutionServiceTest,BusinessActionForeachStepExecutorTest,BusinessObjectPublishServiceCommandTest \
  test
```

- 结果：`Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- 覆盖：发布动作、Schema/上下文策略、显式空 Schema、幂等、递归步骤、数值调整、动态条件 SQL 和发布门禁。

### 4.2 前端定向验证

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec eslint \
  src/components/ai-form/AiCrudPage.vue \
  src/components/ai-form/business-action-runtime.js \
  src/components/ai-form/__tests__/business-action-runtime.spec.js \
  src/views/app-center/components/designer/BusinessActionDesigner.vue \
  src/views/app-center/components/designer/business-action-designer-protocol.js \
  src/views/app-center/components/designer/__tests__/business-action-designer-protocol.spec.js \
  'src/views/app-center/object-designer.[objectCode].vue'
pnpm exec vitest run \
  src/components/ai-form/__tests__/business-action-runtime.spec.js \
  src/views/app-center/components/designer/__tests__/business-action-designer-protocol.spec.js
```

- ESLint：0 error、0 warning。
- Vitest：`2 passed`，`8 tests passed`。

### 4.3 聚合编译与生产构建

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator \
  -am install -Dmaven.test.skip=true -Dspotless.check.skip=true

source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

- generator：32/32 modules SUCCESS，`BUILD SUCCESS`，18.730 s。
- 前端：Node `v20.19.0`，8889 modules transformed，`✓ built in 1m 25s`。

### 4.4 静态检查

```bash
git diff --check
rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.103__harden_transactional_business_commands.sql
```

- `git diff --check`：无输出，退出码 0。
- V1.0.103 placeholder 扫描：无输出；全迁移目录仅命中既有 V1.0.72 消息模板，不属于本阶段变更。

## 5. 警告与跳过项

- 前端构建保留仓库既有组件重名、动态/静态混合导入和 CSS `//` 注释 warning。
- Maven 保留仓库既有 deprecated/unchecked、Commons Logging classpath 提示，不阻断构建。
- 未启动服务，因此无本轮服务 PID 需要清理。
- 跳过真实 MySQL/Flyway、Redis、流程、租户/数据权限和浏览器弱网 E2E；原因是按既定分工由部署环境执行，且本轮不修改真实运行态。

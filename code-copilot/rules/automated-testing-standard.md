# 自动化测试与验证标准

> 适用于 `/test`、阶段收尾验证、Review 后修复验证和归档前验收。目标是复用已有测试基线和执行记录，避免每次从零开始摸索。

## 1. 核心原则

- **增量优先**：先读取当前变更目录下已有 `test-spec.md`、`execution-log.md`、`spec.md`、`tasks.md`，再决定本轮验证范围。
- **证据优先**：不能只写“已通过”，必须记录实际命令、关键输出、接口返回或失败原因。
- **最小闭环**：验证范围跟随本轮改动的风险面扩展，不做无意义全量验证。
- **可复跑**：命令必须可复制执行，环境变量、端口、Token 获取方式和服务启动方式要写清楚。
- **不污染环境**：只停止本轮启动的服务；不清理用户或其他任务留下的进程、数据库和文件。

## 2. 每次开始前必须读取

按顺序读取：

1. `AGENTS.md` 或当前会话注入的项目规则。
2. `.opencode/memory/preferences.md`、`.opencode/memory/pitfalls.md`、`.opencode/memory/decisions.md`。
3. `code-copilot/rules/automated-testing-standard.md`。
4. 当前变更的 `spec.md`、`tasks.md`。
5. 当前变更已存在的 `test-spec.md`、`execution-log.md`。
6. `git status --short` 和本轮相关文件的 `git diff --name-only`。

如果 `test-spec.md` 或 `execution-log.md` 不存在，只创建缺失文件；不要重写已有记录。

## 3. 增量范围判定

根据变更文件选择验证矩阵：

| 变更类型 | 必跑验证 | 条件增强验证 |
|----------|----------|--------------|
| 仅文档/Spec/Task | `git diff --check` | 关键文档链接和状态一致性检查 |
| Java 后端 | 相关 Maven 模块 `compile` 或 `package -DskipTests` | 涉及业务逻辑时补单测；涉及主应用装配时跑 `forge-admin-server` package |
| Mapper XML / SQL 查询 | 后端编译 + Mapper XML 语法检查 | 有数据库时执行接口或 SQL 验证 |
| Flyway 脚本 | SQL 防重复检查 + 后端启动或 Flyway 实跑 | 有 dev 库时检查 `forge_schema_history` 和核心表/列/数据 |
| 前端 JS/Vue | `pnpm build` | 改 UI/交互时启动 Vite 并用浏览器或 Playwright 截图/点击验证 |
| API 协议 | 后端启动 + curl 验证变更接口 | 涉及鉴权/加密时验证普通调用和 `X-Inner-Call` 调用边界 |
| 流程/消息/触发器 | 后端 + Flow 服务 + 主路径接口验证 | 验证关联表、任务表、日志表或消息表落库 |

默认不全量跑 `mvn test` 或完整 E2E；只有共享基础能力、状态流转、权限、安全、数据迁移或用户明确要求时才升级。

## 4. 标准执行流程

### Step 0：复用基线

- 找到最近一次成功验证记录。
- 记录本轮只新增或重新验证的差异点。
- 如果上次失败，优先复跑失败项，不从头生成新计划。

### Step 1：生成或更新测试计划

- 已有 `test-spec.md`：追加“本轮增量验证”小节。
- 没有 `test-spec.md`：从模板创建，至少写清楚 P0/P1 验证范围、命令、跳过项原因。

### Step 2：执行最小验证矩阵

- 先跑低成本检查：`git diff --check`、静态解析、目标模块编译。
- 再跑服务级验证：启动必要服务、登录、接口调用、数据库检查。
- UI 变更必须至少跑一次构建；明显视觉/交互改动还要浏览器验证。

### Step 3：记录证据

在 `execution-log.md` 追加一条记录，包含：

- 执行时间。
- 变更范围。
- 命令和结果。
- 关键接口返回或数据库查询结论。
- 警告项和是否阻断。
- 跳过项及原因。
- 本轮启动并已停止的服务 PID。

### Step 4：回填状态

- `tasks.md` 只更新本轮相关 Task 的执行结果。
- `spec.md` 只更新执行日志、审查结论、HARD-GATE 或风险状态。
- 新踩坑写入 `.opencode/memory/pitfalls.md`。
- 可复用决策写入 `.opencode/memory/decisions.md`。
- 用户偏好写入 `.opencode/memory/preferences.md`。

## 5. Forge 默认命令

### 后端

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-admin-server -am package -DskipTests
```

插件级改动可优先跑：

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

### 前端

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

### 常用接口验证

- 登录：`POST /auth/login`
- 单据配置：`GET /ai/business/document/config/{objectId}`
- 单据运行态：`GET /ai/business/document/{objectCode}/{recordId}/runtime`
- 发起流程：`POST /ai/business/flow/start`
- 流程状态：`GET /ai/business/flow/status/{objectCode}/{recordId}`
- 触发器分页：`GET /ai/business/trigger/page?pageNum=1&pageSize=10`
- 场景模板：`GET /ai/business/trigger/scenario-templates`
- 业务指标：`GET /ai/business/stats/{configKey}/metrics`

加密链路排查时可使用 `X-Inner-Call: true` 做内部验证，但最终结论必须说明是否覆盖真实用户调用链路。

## 6. 完成标准

一轮自动化测试完成必须满足：

- 本轮增量范围明确。
- 必跑命令已有执行证据。
- 失败项有根因或下一步，不把失败写成通过。
- 跳过项有具体原因，例如“本地 MySQL 未启动”或“本轮仅文档变更”。
- 文档、Spec、Task 和 memory 已按需要回填。
- 本轮启动的服务已停止或明确说明仍需保留。

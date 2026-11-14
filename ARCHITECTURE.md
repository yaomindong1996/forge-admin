# Architecture

## Overview

Forge 解决「企业中后台要同时具备可插拔扩展、多租户隔离、权限与流程、以及低代码/AI 生成」的问题：用微内核 + 业务插件承载系统能力，用统一租户/数据权限/认证横切保证隔离与安全，用独立 UI 与可选独立流程服务支持多端与部署形态。

## Codemap

### `forge-server/` — 后端 monorepo
可运行服务与框架同仓；业务能力以插件形式挂到应用入口。

Key modules:
- `forge-admin-server` — 管理端主应用（Spring Boot 入口，聚合插件，默认 :8580）
- `forge-app-server` — App/移动端 API 入口
- `forge-report-server` — 大屏/报表服务
- `forge-flow/` — 可选独立 Flowable 流程服务（与 Admin 共用同一业务库）
- `forge-business/` — 业务扩展模块（非框架内核）
- `db/mysql|postgresql/` — 全量初始化 SQL + Flyway `migration/`（双栈平行版本号）

### `forge-server/forge-framework/` — 微内核
- `forge-dependencies` — BOM / 版本对齐
- `forge-plugin-parent` — 业务插件：`forge-plugin-system`（用户/角色/菜单/租户/字典）、`forge-plugin-generator`（低代码/AI CRUD）、`forge-plugin-flow`、`forge-plugin-job`、`forge-plugin-message`、`forge-plugin-ai`，以及 capability / collaboration / data / external / mcp / print 等扩展插件
- `forge-starter-parent` — 技术启动器：`forge-starter-auth`（Sa-Token）、`forge-starter-tenant`、`forge-starter-datascope`、`forge-starter-orm`、`forge-starter-crypto`、`forge-starter-cache`、`forge-starter-web` 等

插件内部分层约定：`controller` → `service` →（可选 `manager`）→ `mapper` + XML；DTO/VO/entity 分目录。

### `forge-admin-ui/` — 管理端前端
Vue 3 + Naive UI + Vite + Pinia。关键目录：`src/views/`（页面）、`src/components/`（含 lowcode-builder、bpmn、ai-form）、`src/stores/`、`src/utils/request`。设计规范在 `DESIGN.md`。

### 其它前端与文档
- `forge-h5-ui/` — H5 端
- `forge-report-ui/` — 报表设计/展示
- `forge-docs/` — VitePress 产品/开发文档
- `code-copilot/` — SDD 变更产物、规则与记忆（`rules/`、`changes/`、`memory/`）
- `docs/` — 仓库级 guides / references / DOMAIN；历史长文仍可能在 `docs/*.md`
- `.agents/skills/` — 项目级 Agent Skill（唯一项目 Skill 目录）

### 结构速查（细节）
模块树、前后端术语映射、前端目录细表见 `docs/architecture/backend-frontend-map.md`。更长的历史架构说明见 `docs/architecture.md`（技术栈版本可能滞后，以 POM / package 为准）。

## Invariants

- 查询类 SQL 必须落在 Mapper XML；`DataScopeInterceptor` 按 `mapperMethod` 精确改写，Service 层 `LambdaQueryWrapper` 查询吃不到数据权限。
- 多租户由 `TenantLineInnerInterceptor` 自动追加 `tenant_id`；业务内置数据使用 `tenant_id = 1`，禁止 `0`。
- 插件可依赖 starter，starter **不**依赖具体业务插件。
- Service **禁止**互相注入形成环；跨服务协调上提 Controller/Facade。
- 库结构与内置数据变更必须走 Flyway；已进入 `forge_schema_history` 的脚本禁止改内容。
- 开源模板仓禁止企业版源码/标记；生成后的客户工程不在此约束内。
- **Absences：** 无独立「公共无租户业务库」约定（Admin/Flow 共库）；无根级 CI 测试强制（测试默认 skip，需 `-Penable-tests`）；无 Java Checkstyle/PMD 作为默认质量门（结构债由 `scripts/quality-gate.py` + lizard/jscpd 回归门禁覆盖）；不在 AGENTS 里维护百科全书式规范正文。

## Boundaries

### Public vs internal
- HTTP 公共面：各 `*server` / plugin 的 `controller` 包；统一响应 `RespInfo`。
- 插件内部 `service.impl`、`mapper` XML、entity 为内部实现；跨插件复用应通过明确 API/事件/Facade，而不是深耦合实现类。
- 前端：页面经 `src/utils/request` 调后端；权限与动态路由由用户/资源接口驱动。

### Layer dependencies
- Controller → Service → Mapper；禁止 Controller 直接堆复杂 SQL 或跳过 Service 写业务事务（除非只读透传且已在规范中允许的例外）。
- `forge-starter-*` ← 被 plugins/servers 依赖；反向禁止。
- 前端跨面板状态优先 Pinia store；禁止深层 props/emit 传话。

## Cross-Cutting Concerns

### Error Handling
`GlobalExceptionHandler`（`forge-starter-web`）统一映射业务异常与校验失败；写接口校验以 DTO 注解 + `@Valid` 为主，Service 保留业务规则。

### Logging & Observability
操作日志 `@OperationLog`；应用日志落在各 server 的 `logs/`。开发可开 SQL 日志（动态数据源配置）。

### Authentication & Authorization
Sa-Token（`forge-starter-auth`）：`@SaCheckPermission` 等注解保护非公开接口。数据范围由 `DataScopeInterceptor` 在 XML SQL 上改写。禁止用前端传入的 `tenantId`/`userId` 做鉴权结论。

### Configuration
各 server 的 `application-*.yml`；本地密钥类配置用 `*.example.yml` 复制为不入库的 dev 文件。Flyway 位置可由环境变量覆盖，排障时优先检查。

### Database access
MyBatis-Plus + XML；双数据库目录 `db/mysql` 与 `db/postgresql`。可移植 SQL 约定与社区/产品同步流程见关键约定与 `code-copilot/memory/pitfalls/` 中 Flyway 主题。

# AGENTS.md
> High-signal guidance for OpenCode sessions working on this repository

## Project Overview
Forge Admin is a Vue3 + Spring Boot enterprise admin framework with microkernel plugin architecture:
- **Backend**: Java 17 + Spring Boot 3 + MyBatis-Plus + Sa-Token
- **Frontend**: Vue 3 + Naive UI + Vite + UnoCSS
- Core features: RBAC, multi-tenant, code generation, flow engine, message center

## Directory Structure
```
forge/                          # Backend root
├── forge-admin/                # Main application entry
├── forge-framework/            # Core framework
│   ├── forge-plugin-parent/    # Plugins (system/generator/job/message/flow)
│   └── forge-starter-parent/   # Starters (auth/cache/config/crypto etc.)
forge-admin-ui/                 # Frontend project
code-copilot/                   # AI coding assistant rules & change management
.opencode/                      # OpenCode configuration & rules
```

## Environment Requirements
- JDK 17+
- Node.js 18+
- pnpm 8+
- MySQL 8.0+
- Redis 6.0+

## Common Commands

### Code Copilot 渐进式开发流程（推荐）
> 遵循 No Spec No Code 原则，所有开发必须走以下流程
```bash
/spec:init  <初始化项目上下文> # 分析工程结构、依赖、分层模式，填充 rules/project-context.md。
/propose <需求描述>    # 创建变更提案，生成渐进式Spec
/apply <变更名>        # 按确认后的Spec执行编码
/fix <变更名> [描述]   # Review后修正迭代
/review <变更名>       # 两阶段代码审查（Spec合规 + 代码质量）
/test <变更名>         # 生成单测并执行TDD流程
/archive <变更名>      # 归档变更并沉淀知识到知识库
```
> 变更产物统一存放在 `code-copilot/changes/[变更名]/` 目录下，包含spec.md、tasks.md、test-spec.md、log.md

### 常规技术命令
#### Backend
```bash
# Build entire project
cd forge && mvn clean install

# Run admin service
cd forge/forge-admin && mvn spring-boot:run

# Run flow service (optional)
cd forge/forge-flow && mvn spring-boot:run
```
> Service runs on `http://localhost:8080` by default

#### Frontend
```bash
# Install dependencies
cd forge-admin-ui && pnpm install

# Start dev server
pnpm dev

# Build for production
pnpm build

# Lint & fix
pnpm lint:fix
```
> Dev server runs on `http://localhost:5173` by default, default credentials: admin/123456

## Key Conventions (Follow These Strictly)
### 1. 渐进式Spec规范
- No Spec No Code：没有确认的Spec不准写代码
- Spec包含完整的背景、现状分析、功能点、业务规则、数据/接口变更、风险、测试策略
- 所有待澄清问题解决后才能进入/apply阶段
- Spec与代码冲突时，以Spec为准，先修Spec再改代码

### 2. Code Generation
- Use built-in `forge-plugin-generator` for CRUD modules, do not write boilerplate manually
- Frontend CRUD pages use `AiCrudPage` component (see `.opencode/instructions/code-rules.md` for usage)

### 3. Backend APIs
- Follow REST conventions: `GET /page` (list), `GET /{id}` (detail), `POST /` (create), `PUT /` (update), `DELETE /{id}` (delete)
- Return unified `RespInfo.success(data)` / `RespInfo.error(msg)`
- Use `@ApiDecrypt`/`@ApiEncrypt` annotations for sensitive interfaces
- Prefer MyBatis-Plus lambda queries over raw SQL for simple operations; complex queries go in XML files

### 4. Database
- All business tables must include base fields: `id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`
- Use `utf8mb4` charset, `InnoDB` engine
- Create indexes for frequently queried fields, follow leftmost prefix rule for composite indexes
- Never commit database credentials to repo (local configs are in `.gitignore`)

### 5. Frontend
- API calls use `request` utility from `@/utils/request`
- Encrypted API calls use `postEncrypt` from `@/utils/encrypt-request`
- Use UnoCSS for styling, avoid unnecessary custom CSS
- Follow existing component patterns, use Naive UI components where possible

## Important Gotchas
- **First time setup**: Copy `application-dev.example.yml` to `application-dev.yml` in backend modules and configure local database/Redis
- Frontend: Copy `.env.example` to `.env.local` for local environment config
- No Spec No Code: Always refer to requirements specs first before writing code, follow code-copilot workflow for changes
- Never push to master branch directly, use feature branches
- All code must compile and pass existing tests before commit

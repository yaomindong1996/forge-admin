# AGENTS.md
> AI 编程助手工作指引 — 进入仓库后首先阅读本文件（渐进披露入口，约 120 行）

## Project
**Forge Admin** — 微内核插件化的企业级中后台（Vue3 + Spring Boot）。支撑 RBAC、多租户、低代码/AI 生成、Flowable 工作流、消息与数据大屏。

## Build & Run
```bash
cd forge-server && mvn clean install -DskipTests          # 后端构建
cd forge-server/forge-admin-server && mvn spring-boot:run  # Admin :8580
cd forge-admin-ui && pnpm install && pnpm dev              # 前端 :3000
```
环境模板：`application-dev.example.yml`、`forge-admin-ui/.env.example`。详见 [本地开发与验证](docs/guides/local-dev-verify.md)。

## Session Startup
开始改动前先定向：
1. `pwd` — 确认仓库根
2. `git log --oneline -10` — 最近提交
3. `git fetch origin` — 刷新远端
4. 按本仓习惯把分支与 `origin/main`（或 `origin/HEAD`）对齐
5. 有 `PROGRESS.md` 则读，否则跳过
6. 冒烟：后端 `cd forge-server && mvn -pl forge-admin-server -am compile -DskipTests`；前端按需 `cd forge-admin-ui && pnpm lint:fix`
7. 必读 `code-copilot/memory/preferences.md`；按任务打开相关 `code-copilot/memory/pitfalls/<主题>.md`
8. 命中项目 Skill 时先读 `.agents/skills/*/SKILL.md`；UI 变更先读 `forge-admin-ui/DESIGN.md`
9. 环境坏了先修环境，再开新需求

## Test
```bash
cd forge-server && mvn test -Penable-tests     # 后端（默认会跳过测试，必须加 profile）
cd forge-admin-ui && pnpm test && pnpm lint:fix
pnpm quality:check                             # 结构质量门禁
pnpm quality:test                              # 门禁自检
python3 scripts/docs-check.py                  # 文档链接/拓扑
```
自动化测试标准：`code-copilot/rules/automated-testing-standard.md`。SDD：`/propose` → `/apply` → `/test` → `/archive`（产物在 `code-copilot/changes/`）。

## Architecture
见 [ARCHITECTURE.md](./ARCHITECTURE.md)。业务工作流见 [docs/DOMAIN.md](docs/DOMAIN.md)。
- 后端：`forge-server` 微内核（`forge-plugin-*` + `forge-starter-*`），入口 `forge-admin-server`
- 前端：`forge-admin-ui`（Vue3 + Naive UI）；另有 H5 / 报表 UI
- 库表变更只走 `forge-server/db/{mysql,postgresql}/migration` Flyway
- 模块树与分层速查：[docs/architecture/backend-frontend-map.md](docs/architecture/backend-frontend-map.md)

## Key Conventions
权威正文：[docs/guides/critical-conventions.md](docs/guides/critical-conventions.md)（与 `code-copilot/rules/coding-style.md` 双源；勿在 Skill/踩坑里再抄一份）。
- Always 把查询 SQL 写在 Mapper XML；禁止在 Service 用 `LambdaQueryWrapper` 拼查询（MP 内置单表 CRUD 除外）
- Always 内置业务数据 `tenant_id = 1`；Never 用 `0`
- Always 写接口用 DTO + `@Valid`；Never `@RequestBody Map` 承载固定字段
- Always 业务状态用枚举；Never 生产代码魔法数字/字符串
- Never Service 互相注入；跨服务协调上提到 Controller/Facade
- Always 逻辑删除优先；Never 在模板仓提交企业版（`pnpm check:edition`）
- Always 提交说明用中文；Always 触及巨型类/SFC 时先拆再堆
- Never 编辑、扩展或批准 `.quality-baseline.json`；`pnpm quality:check` 报 stale 时只允许 `pnpm quality:prune` 后提交
- `code-copilot/changes/*/tasks.md` 等变更账本：只更新任务状态与执行证据，Never 改写无关描述凑进度

## Definition of Done
未全部满足前不得宣称完成：
- 后端：相关模块编译通过；触及测试时 `mvn test -Penable-tests` 通过
- 前端：`pnpm lint:fix`；相关 `pnpm test` 通过
- `pnpm quality:check` 通过 — 禁止用改基线掩盖新增复杂度/重复；修代码或 prune 过期项
- 文档路径/链接变更时 `python3 scripts/docs-check.py` 通过
- 端到端验证：打真实接口或浏览器点通主路径，不只看单测
- 验证命令与结果追加到当前变更 `execution-log.md`；commit message 写 *为什么*

## Common Workflows
- [本地开发与验证](docs/guides/local-dev-verify.md)
- [关键约定全文](docs/guides/critical-conventions.md)
- [质量门禁](docs/guides/quality-gates.md)
- [行政区划查询](docs/references/region-query.md)
- [文档索引](docs/README.md)
- [发版与版本](forge-docs/guide/release-version.md)
- [SDD / code-copilot](code-copilot/AGENTS.md)

## Architecture Decision Records
仅在同时满足时写入 `docs/adr/` 或 `code-copilot/memory/decisions.md`：难逆转、仅看代码难懂、存在真实权衡。例行实现、临时约束、自明约定不要写 ADR。

## Known Gotchas
- 根 POM 默认 `forge.tests.skip=true`；没加 `-Penable-tests` 的 `mvn test` 不算跑过测试
- Flyway 已执行脚本禁止改内容；分支合并后必须查重版本号（见关键约定 §5.13）
- `DataScopeInterceptor` 按 `mapperMethod` 匹配 XML；Lambda 动态 SQL 吃不到数据权限改写
- Admin 与 Flow 共用同一库；不要另建 Flow 库，也不要对已有业务库重跑全量 SQL
- 虚拟区划 code 以 `ALL` 结尾表示本级+下级；写法见 [region-query](docs/references/region-query.md)

## Priority
`AGENTS.md` > 当前 `spec.md` > `.agents/skills/*/SKILL.md` > `code-copilot/rules/*` > 其它文档。冲突时以本文件短指令 + 其链接的权威正文为准。

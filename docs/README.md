# Documentation

仓库级文档索引。AI/贡献者从这里按需下钻；日常短指令见根 [AGENTS.md](../AGENTS.md)。

## Architecture
- [ARCHITECTURE.md](../ARCHITECTURE.md) — 问题域、codemap、不变量与边界
- [backend-frontend-map.md](./architecture/backend-frontend-map.md) — 模块树与前后端术语速查
- [architecture.md](./architecture.md) — 历史长文架构说明（版本号以 POM/package 为准）

## Domain Knowledge
- [DOMAIN.md](./DOMAIN.md) — 业务工作流、关系与合规约束
- 尚无根 `CONTEXT.md`；术语未单独裁定前不要臆造 glossary

## Guides
- [Critical conventions](./guides/critical-conventions.md) — 关键约定权威正文
- [Local dev & verify](./guides/local-dev-verify.md) — 本地搭建与改→验证闭环
- [Quality gates](./guides/quality-gates.md) — 复杂度/重复度回归门禁

## References
- [Region query](./references/region-query.md) — `region_code` / `*ALL` 虚拟区划查询

## Product / process (elsewhere)
- [forge-docs](../forge-docs/) — VitePress 指南与约定
- [Release version](../forge-docs/guide/release-version.md)
- [SDD / code-copilot](../code-copilot/AGENTS.md) — 变更提案与记忆写入规则
- [code-copilot rules](../code-copilot/rules/) — 工程上下文、编码与测试标准
- [CHANGELOG](../CHANGELOG.md)

## Decisions
符合「难逆转 + 仅看代码难懂 + 真实权衡」时写入 `docs/adr/`（按需创建）或沿用 `code-copilot/memory/decisions.md`。不要为例行实现写 ADR。

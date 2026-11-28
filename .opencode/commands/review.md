---
description: 审查需求符合性、实现风险和验证证据
agent: general
---

# /review

目标 `$ARGUMENTS`

1. 确认范围。参数可指变更、文件或提交范围；无参数时用当前 diff。读 `AGENTS.md`、`docs/guides/critical-conventions.md`、`code-copilot/rules/coding-style.md`。有对应 Spec 时一起读 `spec.md`、`tasks.md`、`test-spec.md` 和执行记录，逐项核对实现与证据；无 Spec 则按用户需求和仓库约定审查。
2. 阅读 diff 及其真实调用链，检查边界、数据变更、兼容、失败与回退。即使发现需求偏差，也继续检查实现风险。运行必要的只读检查或隔离测试，区分实测结果和根据代码推断的风险。
3. 根据涉及的能力补查。
   - 菜单和 API 读 `.agents/skills/forge-resource-permissions/SKILL.md`。追踪按钮、API 方法/路径、租户角色关联、前端未加载权限、服务端守卫、通配与豁免。用普通账号的权限拒绝证据确认防护。
   - 数据权限读 `.agents/skills/forge-tenant-datascope/SKILL.md`。从当前会话追踪租户、组织与角色到每个实际执行的 Mapper SQL 和数据结果。导出、打印、任务等另起入口的查询也要追查。
   - Flyway 读 `.agents/skills/forge-flyway-migration/SKILL.md`。检查已执行脚本是否被改、三库版本/语义、重复保护、历史数据、权限种子影响和回退策略。
   - 版本或部署读 `forge-docs/guide/release-version.md`，核对版本源、CHANGELOG、产物来源与验证记录。分别说明提交、标签、推送、部署的实际状态。
4. 按严重度先给可操作的发现。每条注明文件/行号、触发条件、影响和可核对证据，标明需求漏项、实现缺陷或验证缺口。无发现时直接说明并列仍未覆盖的验证。按要求可追加审查证据到现有变更记录。除非用户同时要求修复，审查不改实现。

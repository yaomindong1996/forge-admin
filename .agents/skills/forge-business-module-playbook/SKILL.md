---
name: forge-business-module-playbook
description: Orchestrate end-to-end Forge business module creation. Use when the user asks Cursor to analyze the project and generate a business module, approval app, low-code application, CRUD plus workflow, form+message+post integration, or any multi-capability feature spanning form, flow, message, post, or low-code app creation. Decide code-first vs lowcode first, then route to child skills in order. Do not use for unrelated refactors or single-file bugfixes.
---

# Forge Business Module Playbook

## Purpose

Stop blind repo scanning. For multi-capability module work, **read this Skill first**, pick a path, then load only the child Skills listed below. Do not dump entire `code-copilot/memory/pitfalls/*` into context.

## Step 0 — Choose implementation path

Ask (or infer from the request):

| Path | When |
|------|------|
| `code-first` | Custom Java Service, custom tables, non-trivial status repair, code task forms (`BusinessCodeFormProvider`) |
| `lowcode` | Dynamic CRUD object, App Center suite/object/app, form/list designer, publish/DDL, optional flow binding |

If unsure and the user only needs a simple admin table with no approval: use `forge-codegen-crud` only (still via this playbook's short path).

## Code-first sequence

1. Read `../forge-codegen-crud/SKILL.md` — table, API, AiCrudPage, Flyway seeds.
2. Read `../forge-form-contract/SKILL.md` — edit/search schema, dict, task-form Provider contract.
3. If assignee/org by post: read `../forge-post-org-binding/SKILL.md`.
4. If notify on submit/approve/reject: read `../forge-message-integration/SKILL.md`.
5. If approval: read `../forge-business-flow-development/SKILL.md` (short path + purchase-order reference).
6. Run each child checklist, then the combined checklist in `references/validation-checklist.md`.

## Lowcode sequence

1. Read `../forge-lowcode-app-create/SKILL.md` — suite → object → app → form/list → publish/DDL → runtime.
2. If form field types / runtime AiForm issues: also read `../forge-form-contract/SKILL.md` (boundary section).
3. If approval: read `../forge-business-flow-development/SKILL.md` and its `references/lowcode-workflow.md` only for **binding/start/task form**, not for app creation.
4. If message/post needed: message + post Skills as above.
5. Run lowcode checklist, then flow/message checklists as applicable.

## Non-Negotiables

- Never mix fact sources: lowcode uses object field registry + `formDesignerSchema`; code-first uses Java Provider / AiCrudPage schemas.
- Never read all pitfalls files; only titles listed in each Skill's `references/pitfalls-index.md`.
- Never claim done without the relevant validation checklist.
- Prefer known-good file maps from child Skills over inventing new patterns.

## Skill routing table (keywords → Skill)

| Keywords | Skill |
|----------|-------|
| 业务模块/生成模块/端到端/审批业务/应用怎么建 | **this playbook first** |
| CRUD/增删改查/单表/主子表/树表 | `forge-codegen-crud` |
| 低代码应用/套件/业务对象/应用中心/表单设计/列表设计/发布/DDL | `forge-lowcode-app-create` |
| AiCrudPage schema/AiForm/任务表单/BusinessCodeFormProvider | `forge-form-contract` |
| Flowable/BPMN/待办/驳回/会签/businessKey | `forge-business-flow-development` |
| 站内信/消息模板/sendIfAbsent/通知 | `forge-message-integration` |
| 岗位/SysPost/按岗选人/assigneeType=post | `forge-post-org-binding` |

## References

- `references/validation-checklist.md` — combined exit criteria for multi-skill deliveries.
- `references/golden-samples.md` — code-first + lowcode known-good file maps.

---
name: forge-lowcode-app-create
description: Create Forge low-code applications end-to-end — business suite, business object with fields and relations, App Center app, form designer, list designer, publish, controlled DDL sync, menu entry, and runtime CRUD validation. Use when building or reviewing a low-code app from scratch, seeding ai_business_suite/object/app, formDesignerSchema, list design, publish/DDL, or when Cursor is asked to generate a low-code module. For Flowable binding after the app exists, also use forge-business-flow-development. Do not use for code-first Java CRUD modules.
---

# Forge Low-Code App Create

## Purpose

Codify the **create happy path** for App Center low-code apps so agents do not rediscover designer/publish rules from `pitfalls/lowcode.md`.

## Happy path

1. Suite — `ai_business_suite` (reuse or create). See `references/create-sequence.md`.
2. Object — `ai_business_object` + field registry + relations + optional code rules. See `references/object-fields-and-relations.md`.
3. App — `ai_business_app` / application aggregate linked to suite+object, entry mode, menu resource.
4. Form design — canvas + field shelf from **object field catalog**; sync draft before leaving the panel. See `references/form-list-designer.md`.
5. List design — columns, queries, actions (`START_FLOW` when approval needed).
6. Publish + DDL — controlled CREATE/ALTER via platform DDL service + permission; design vs publish fact sources. See `references/publish-and-ddl.md`.
7. Runtime — `/ai/crud/{configKey}` CRUD works; menu visible.
8. Optional flow — hand off to `forge-business-flow-development` + `references/lowcode-workflow.md` there.

Golden seed map: `references/golden-sample.md` (presale registration lowcode app migration).

## Non-Negotiables

- Do not write child-table synthetic columns back into the **main** object field catalog.
- Do not treat published `editSchema` as the only form truth; prefer `formDesignerSchema` for design/approval field catalogs.
- Do not switch to document/flow panels without `syncDesignerDraft` first.
- Do not hand-write uncontrolled DDL; use platform DDL + `ai:lowcode:deploy-ddl` style gates.
- Do not use form-create temporary `F...` ids as permanent field codes.
- `objectCode` must be canonical when starting flows (not configKey alias).
- Seeds use `tenant_id = 1`; escape Flyway `${` in templates via `CONCAT('$', '{...}')`.

## Reference routing

- `references/create-sequence.md`
- `references/object-fields-and-relations.md`
- `references/form-list-designer.md`
- `references/publish-and-ddl.md`
- `references/seed-sql.md`
- `references/pitfalls-index.md`
- `references/golden-sample.md`
- `references/validation-checklist.md`

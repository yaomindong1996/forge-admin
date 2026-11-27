---
name: forge-business-flow-development
description: Develop Forge business approval workflows backed by Flowable, using the current sample purchase order approval as the reference. Use when creating or reviewing a code-first or low-code business process in Forge, binding business records to flow models, designing business process status transitions, integrating BusinessCodeFormProvider or low-code forms with task forms, writing Flyway SQL for business tables/dictionaries/app-center/flow bindings, configuring BPMN node form permissions, assignees including post/role/dept, callbacks, resubmit/reject behavior, and validating end-to-end workflow behavior.
---

# Forge Business Flow Development

## Overview

Implement a complete Forge business workflow: status machine, Flowable model binding, task form integration, SQL seeds, and validation. Canonical code-first sample: `sample_purchase_order`.

For **creating** a low-code suite/object/app/form/list/publish first, use `forge-lowcode-app-create`; this Skill only covers **flow binding and runtime** on top of an existing object.

## Short path (default)

Read `references/short-path.md` and follow its 10 steps. Expand into full references only when needed.

## Workflow

1. Identify the implementation mode.
   - Code-first: `references/code-first-workflow.md`.
   - Low-code object already exists: `references/lowcode-workflow.md` (binding/start/task only).
   - Low-code object does **not** exist yet: stop and use `../forge-lowcode-app-create/SKILL.md` first.

2. Shared references before editing:
   - `references/purchase-order-reference.md` — file map.
   - `references/status-and-callbacks.md` — status + callbacks.
   - `references/sql-templates.md` — Flyway.
   - `references/bpmn-configuration.md` — node form/permissions.
   - `references/assignee-and-post.md` — user/role/dept/**post**/expr assignees.
   - `references/pitfalls-index.md` — open only matching titles in `pitfalls/flow.md`.
   - `references/validation-checklist.md` — before claiming done.

3. Configuration ownership:
   - App/binding owns object link, status field, flow model key, title template, variable mapping.
   - BPMN/designer owns node form asset, field permissions, approver policy, reject/return/countersign.
   - `ai_business_binding.binding_config.nodeForms` is fallback only.

4. Invariants:
   - `businessKey` = `<objectCode>:<recordId>`.
   - Store `business_key` / `process_instance_id` (or platform link).
   - Snowflake IDs as strings in frontend.
   - Status idempotent and repairable from active tasks.
   - Complex SQL in Mapper XML; seeds `tenant_id = 1`.

## Deliverables

- Status dictionary + workflow fields (code-first) or document status field (lowcode).
- Flow model / BPMN with form permissions and assignees.
- Binding seed + variable mapping.
- Code Provider or lowcode form asset.
- Validation notes for approve/reject/resubmit/readonly done.

## Non-Negotiables

- No second node-config UI outside the real flow designer.
- No hardcoded dict labels in Vue.
- Typed DTOs on write APIs; status via enum.
- Never seed `tenant_id = 0`.
- Never overwrite non-empty BPMN on sample init.
- Never rely only on `PROCESS_COMPLETED` for final status.
- Provider must not save fields the node did not mark writable.
- Never use configKey alias as `objectCode` when starting flow.

## Reference Routing

- `references/short-path.md`: default 10-step path.
- `references/code-first-workflow.md` / `lowcode-workflow.md`
- `references/purchase-order-reference.md`
- `references/status-and-callbacks.md`
- `references/bpmn-configuration.md`
- `references/assignee-and-post.md`
- `references/sql-templates.md`
- `references/pitfalls-index.md`
- `references/validation-checklist.md`

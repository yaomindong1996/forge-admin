# Low-Code Workflow (binding & runtime only)

Use when the business **object/app already exists** (created via App Center or `forge-lowcode-app-create`).  
Do **not** use this file to invent suite/object/app/form designer steps — read `../forge-lowcode-app-create/SKILL.md` instead.

## Sequence

1. Confirm object is published and runtime page works: `AiCrudPage` / `/ai/crud/{configKey}`.
   - Document config: status field, owner field, title field, default flow.

2. Configure flow binding.
   - `ai_business_binding` with `target_type='OBJECT'`, `target_code=<objectCode>`, `binding_type='FLOW'`.
   - Required keys: `flowModelKey`, `titleTemplate`, `businessBinding`, `variableMapping`.
   - Every BPMN assignee/candidate/condition expression needs a matching variable mapping.
   - Assignees: see `assignee-and-post.md` (`post` uses post **id**).

3. Start flow via platform runtime.
   - Prefer built-in `START_FLOW` action — do not also wire a duplicate custom-action start.
   - `BusinessFlowService` locks by `tenantId + businessKey` and writes instance link.

4. Configure node forms in the **real** flow designer.
   - Form asset per user task; field permissions on the node.
   - Persist on BPMN (`formKey` / `formUrl` / `formJson` / `formFieldPermissions`).
   - Prefer design-time form schema over published `editSchema` alone (`pitfalls-index.md`).
   - `nodeForms` on binding is fallback only.

5. Approval actions.
   - Context: `GET /ai/business/flow/task-form-context` (+ `/readonly`).
   - Save fields: `POST /ai/business/flow/task-form-context`.
   - Complete via platform approve/reject; sync link + document status.

6. Callbacks / side effects.
   - `callbackActions` only for domain actions after terminal results.
   - Idempotent keys for irreversible mutations; not in frontend-only handlers.
   - Prefer message Skill for notify templates (`forge-message-integration`).

## Required Checks

- Runtime `objectCode` is canonical, not a configKey alias.
- `businessKey` = `<objectCode>:<recordId>`.
- Flow start/link insertion is idempotent by business key.
- Flyway seeds with `${...}` templates use `CONCAT('$', '{...}')` to avoid placeholder parsing.

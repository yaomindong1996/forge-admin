# Validation Checklist

Use this before finalizing a Forge business workflow change.

## Static Checks

- Search for `tenant_id = 0` in new SQL. It must not appear for business/config seed data.
- Search for `${` in Flyway SQL. Escape or construct with `CONCAT('$', '{...}')`.
- Run whitespace check on new SQL: `git diff --check -- <migration.sql>`.
- Verify all SQL inserts use explicit columns and duplicate guards.
- Verify complex business queries are in Mapper XML, not Service `LambdaQueryWrapper`.
- Verify write APIs take typed DTOs, not `@RequestBody Map`. Flow `variables` may stay a Map field inside the DTO.
- Verify status writes/comparisons use a business status enum (`getCode()` / `matches()`), not string or number literals.
- Verify frontend URL placeholders in `AiCrudPage` use `:id`, not `{id}`.

## Backend Compile

Run the narrowest relevant Maven compile first, then broader compile if shared code changed. In this repo, typical commands are:

```bash
cd forge-server && mvn -pl forge-business/forge-business-core -am -DskipTests compile
cd forge-server && mvn -pl forge-admin-server -am -DskipTests compile
```

If the task explicitly includes `/test`, phase verification, review-fix verification, or archive acceptance, first read `code-copilot/rules/automated-testing-standard.md` and append the results to the current change `execution-log.md`.

## Flow Runtime (end-to-end — required before claiming done)

Validate these behaviors manually or by automated e2e when services are available:

- Create business record -> status `DRAFT`.
- Submit -> Flowable instance created, `business_key` and `process_instance_id` saved, status `IN_PROCESS`.
- Todo list displays business object name and business summary.
- Todo form loads correct asset and only shows readable fields.
- Saving task fields persists only writable fields.
- Required node fields are enforced server-side.
- Assignees resolve: user/role/dept/**post**/expr as configured; empty assignee/candidates means broken notify/todo.
- Approval through all nodes -> status `APPROVED`.
- Reject from approval node -> task enters applicant modify node and status becomes `NEED_MODIFY`.
- Applicant modify resubmit -> status returns `IN_PROCESS`.
- Applicant terminate/reject -> status `REJECTED`.
- Cancel/withdraw/terminate -> status `CANCELED`.
- Done/history form is readonly and can still display business record data.
- Page/detail query repairs status if event timing drift occurred.
- Lowcode: started with canonical `objectCode`, not configKey; used built-in `START_FLOW` only once.

## Security And Data Rules

- Do not log mobile numbers, identity numbers, bank cards, API keys, or unmasked secrets.
- API key/secret-like fields must be masked before returning to frontend.
- Money fields use `long` cents.
- `LocalDateTime` for datetime fields and `LocalDate` only for pure date semantics.
- Business side effects after approval must be idempotent.

## Common Regression Tests

- Flow model init must preserve non-empty BPMN XML.
- BPMN default sequence flow must not contain condition expression.
- Duplicate sequence flows must not generate duplicate tasks.
- Code Provider assets must still resolve after App Center metadata overrides.
- Frontend must preserve Long IDs as strings in task variables and API params.

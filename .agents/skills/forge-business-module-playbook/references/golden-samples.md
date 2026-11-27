# Golden Sample File Maps

Agents must prefer these known-good maps over inventing structure.

## Code-first (CRUD + form + flow; extend with message/post as needed)

Primary map: `../forge-business-flow-development/references/purchase-order-reference.md`

Highlights:

- Backend: `forge-business-core/.../purchase/*`
- Provider: `SamplePurchaseOrderCodeFormProvider`
- UI: `forge-admin-ui/src/views/business/purchase-order-test.vue`
- SQL: `V1.0.81`–`V1.0.84` purchase order flow/app seeds
- Assignees: variables like `deptLeaderId` (string user ids); for post-based nodes see `assignee-and-post.md` + `forge-post-org-binding`
- Messages: platform flow todo templates in `V1.0.72` + `forge-message-integration`

## Lowcode (suite/object/app create → then flow)

Primary map: `../forge-lowcode-app-create/references/golden-sample.md`

- Seed: `forge-server/db/mysql/migration/V1.0.105__seed_presale_registration_lowcode_app.sql`
- After create/publish: bind flow via `forge-business-flow-development/references/lowcode-workflow.md`

## How to use

1. Copy structural patterns (tables, binding JSON keys, Provider shape, designer save order).
2. Replace business codes/names.
3. Run the relevant Skill checklists before claiming done.

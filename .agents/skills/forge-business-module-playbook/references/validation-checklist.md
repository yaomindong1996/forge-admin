# Combined Module Validation Checklist

Use after child Skill checklists. Skip sections that the request did not include.

## Path

- [ ] Implementation path recorded: `code-first` or `lowcode` (not mixed).
- [ ] Child Skills were read in playbook order; no full-repo blind scan as the primary strategy.

## Code-first

- [ ] `forge-codegen-crud` checklist passed (SQL, POST contract, AiCrudPage, dict).
- [ ] `forge-form-contract` checklist passed for page schemas and/or Provider.
- [ ] If flow: purchase-order-style binding, `businessKey`, status enum, e2e approve/reject covered.

## Lowcode

- [ ] `forge-lowcode-app-create` checklist passed (suite/object/app, form+list save, publish, runtime CRUD, menu).
- [ ] `objectCode` is canonical (not a configKey alias) when starting flow.
- [ ] Design-time `formDesignerSchema` is the form truth; published `editSchema` is not the only source.

## Cross-cutting

- [ ] Post: seeds + user bind + flow `assigneeType=post` value is post **id** (stringified), IDs stay strings in JS.
- [ ] Message: template/bizType seeded; send via `sendIfAbsent`; no duplicate spam; Flyway `${` escaped.
- [ ] No `tenant_id = 0` in business seeds.
- [ ] Compile or runtime smoke noted (or explicitly skipped with reason).

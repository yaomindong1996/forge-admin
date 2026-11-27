# Short Path — New Approval Business (≈10 steps)

Use this for a new approval module. Read full references only when a step needs detail.

1. **Pick path**: code-first (`code-first-workflow.md`) or lowcode app already created via `forge-lowcode-app-create`.
2. **Status field**: draft / in_process / need_modify / approved / rejected / canceled — Java enum with `getCode()` / `matches()`.
3. **Persist link fields**: `business_key`, `process_instance_id` on the business row (or platform link table for lowcode).
4. **businessKey**: `<objectCode>:<recordId>` — never invent another format.
5. **Flow model key** + BPMN user tasks with `formKey` / field permissions (designer owns node config).
6. **Assignees**: variable expression `${...}` or node `assigneeType` (`user`/`role`/`dept`/`post`/`leader`/`deptManager`/`initiator`/`expr`). See `assignee-and-post.md`.
7. **Binding seed**: `ai_business_binding` with `flowModelKey`, `titleTemplate`, `variableMapping` covering every expression variable.
8. **Task form**: code Provider (`BusinessCodeFormProvider`) or lowcode form asset; do not rely only on published `editSchema`.
9. **Callbacks**: task-created / task-completed / process-completed keep status repairable; terminal side effects idempotent.
10. **Validate**: create → submit → todo form → approve/reject/resubmit → done readonly. See `validation-checklist.md`.

Snowflake IDs stay **strings** in frontend variables and API payloads.

# Publish and DDL

## Publish

- Prefer status progression for application publish; avoid re-running full object publish every step.
- Do not block publish on unrelated print template field catalogs or unconnected flow-print scenes.
- Object design version ≠ CRUD publish version — do not reuse blindly.
- Preview/designPreview must not persist application design mutations as side effects of GET.

## DDL

- New fields from form design trigger **controlled** preview/execute DDL (`LowcodeDdlService` style).
- Requires deploy-DDL permission and confirmation flags — never invent raw ALTER in ad-hoc SQL for tenant apps.
- Import-existing-table paths must keep audit column strategy so create_* fields autofill.

## After publish

- Smoke `/ai/crud/{configKey}` list/add/edit.
- Menu resource points at the runtime route.
- Only then attach flow binding (`forge-business-flow-development`).

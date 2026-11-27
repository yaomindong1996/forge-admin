# AiCrudPage Form Schema

## Typical bindings

```js
const apiConfig = {
  list: 'get@/api/biz/example/page',
  detail: 'post@/api/biz/example/getById',
  add: 'post@/api/biz/example/add',
  update: 'post@/api/biz/example/edit',
  delete: 'post@/api/biz/example/remove/:id',
}
```

- `searchSchema` / `editSchema` / `columns` — Forge field descriptors (`field`, `label`, `type`, `props`, `rules`).
- Prefer `AiCrudPage` over hand-rolled Naive forms for standard CRUD.
- Custom slots that mutate nested `formData` must go through form `update:value` / `updateValue` — naked `v-model` on nested props desyncs AiForm copies.

## Golden code-first UI

- Page: `forge-admin-ui/src/views/business/purchase-order-test.vue`
- Also see `forge-codegen-crud/references/single-table-crud.md` for generated shape.

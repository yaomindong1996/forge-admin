# Object Fields and Relations

## Field registry

- Business field codes are stable snake/camel business names — never form-create defaults like `input` / `Frpjmp...`.
- Keep `basicProps.optionSource` (and control type) on the **field registry** so sub-table / approval reuse works; mirroring only into form component props is insufficient.
- Audit columns (`create_by`/`create_time`/…) require import/audit strategy so runtime autofill works.

## Master–detail

- Child objects are separate field catalogs; parent form uses `subTable` components.
- Do **not** merge `relationKey__childField` synthetic columns into the parent object field directory (causes duplicate codes and dirty catalog growth).
- Child select/user/dept columns need real `type` / `basicProps` from child object fields, not weak `input` fallbacks.

## Relations

- `modelRefs` / relation order should follow form designer subTable order when rebuilding.
- Record selectors need a real `objectCode` on the query DTO — empty DTO → “选择器缺少业务对象编码”.

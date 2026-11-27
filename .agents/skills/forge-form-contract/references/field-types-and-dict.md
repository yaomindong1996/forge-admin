# Field Types and Dictionaries

## Prefer these AiFormItem types

Common: `input`, `textarea`, `number` / `input-number`, `select`, `radio`, `checkbox`, `switch`, `date`/`datetime`, `userSelect` (and aliases like `forgeUserSelect`), `orgTreeSelect` / tree selects, upload/image types used by the project.

Normalize aliases so user/dept columns do not fall back to plain `n-input` (especially in child tables).

## Dictionaries

- Seed `sys_dict_type` / `sys_dict_data` or reuse existing `dict_type`.
- Frontend: `useDict('type')`, `DictSelect`, `DictTag`.
- Detail/readonly: show label text; keep current value in options even when disabled.

## Values

- Switch/business flags: align checked values with DB (`0`/`1` vs true/false).
- Selects: stringify Long ids for Naive select/tree-select echo.
- Required rules for tree/select must use “has value” helpers, not naive empty checks that reject `0`.

## Files

- Persist file id strings; never raw anonymous CDN URLs as the source of truth.

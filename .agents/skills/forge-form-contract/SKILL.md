---
name: forge-form-contract
description: Define and review Forge form contracts for AiCrudPage search/edit schemas, AiForm/AiFormItem field types, dictionaries, file IDs, and Flowable task forms via BusinessCodeFormProvider. Use when generating or fixing Vue form schemas, validation rules, user/dept/tree selects, or code-first approval task forms. For low-code form designer create/publish flows use forge-lowcode-app-create; this skill covers runtime schema contracts and boundaries.
---

# Forge Form Contract

## Purpose

Keep list/edit/task forms consistent with Forge runtime components so agents stop guessing field `type`, dict wiring, and Provider save rules.

## When to use which form path

| Path | Skill |
|------|--------|
| Code AiCrudPage page | this Skill + `forge-codegen-crud` |
| Code-first approval task form | this Skill (`task-form-provider.md`) + flow Skill |
| Lowcode form designer / publish | `forge-lowcode-app-create` (then this Skill for runtime AiForm quirks) |

Never mix Provider-backed code forms and lowcode designer as dual writers for the same object.

## Workflow

1. Identify surface: list search, edit modal, detail, or task form.
2. Read `references/aicrud-form-schema.md` for AiCrudPage.
3. Read `references/field-types-and-dict.md` for AiFormItem types and dicts.
4. If approval task form: `references/task-form-provider.md`.
5. Verify with `references/validation-checklist.md`.

## Non-Negotiables

- Dictionary fields: `useDict` + `DictSelect` / `DictTag` — no hardcoded option maps in pages.
- AiCrudPage API placeholders `:id` not `{id}`; POST detail/edit/remove contract for generated CRUD.
- Schemas that depend on dicts must be `computed`.
- Files/images store file ids; display via `AuthImage` / `getFileUrl`.
- Task Provider: only persist fields marked writable on the current node; validate business state.
- Long IDs as strings in select/tree values.
- Tree/org required validation must treat numeric `0`/ids as present (`hasFormValue` style).

## References

- `references/aicrud-form-schema.md`
- `references/field-types-and-dict.md`
- `references/task-form-provider.md`
- `references/validation-checklist.md`

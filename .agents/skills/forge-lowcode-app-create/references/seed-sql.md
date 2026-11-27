# Low-Code Seed SQL Notes

## Tables commonly seeded

- `ai_business_suite`
- `ai_business_object` (+ field/relation payloads as used by the sample)
- `ai_business_application` / `ai_business_application_object` and/or `ai_business_app`
- `ai_business_binding` (flow — usually after app exists)
- `sys_resource` menu entries
- Dictionaries as needed

## Rules

- `tenant_id = 1` for built-in demos.
- Guard inserts with `NOT EXISTS`.
- Explicit column lists.
- **Flyway placeholders**: templates like `${field}` or `WL${yyyyMMdd}...` inside SQL strings must use `CONCAT('$', '{field}')` (or equivalent) or Flyway will parse them.

## Golden sample

See `golden-sample.md` — `V1.0.105__seed_presale_registration_lowcode_app.sql`.

# Golden Sample — Presale Registration Lowcode App

Known-good seed for suite/object/application structure:

- `forge-server/db/mysql/migration/V1.0.105__seed_presale_registration_lowcode_app.sql`

Contains inserts for:

- `ai_business_suite`
- Object/field payloads in the same migration family
- `ai_business_application` / `ai_business_application_object`
- `ai_business_app`

When generating a new lowcode app, **mirror this migration’s structure and guards**, then adapt codes/names. Do not invent a third metadata shape.

For flow binding after create, follow `forge-business-flow-development` + purchase-order or existing lowcode flow seeds — not this file alone.

Related e2e notes may live under `code-copilot/changes/*lowcode*` — use only if the change is archived and still accurate; prefer this migration as the durable map.

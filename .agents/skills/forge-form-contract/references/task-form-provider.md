# Task Form — BusinessCodeFormProvider

Canonical sample: `SamplePurchaseOrderCodeFormProvider`  
Files: see `forge-business-flow-development/references/purchase-order-reference.md`.

## Contract

- Register form asset: `formKey`, `formName`, `formMode=BUSINESS_CODE_FORM`, `formUrl`, provider key.
- `buildContext` loads business detail → `BusinessTaskFormContextVO`.
- `saveContext` maps platform payload → typed DTO → service save of **writable** fields only.
- Do not resolve forms via lowcode `configKey`; Provider is code-first.

## Permissions

- Node `formFieldPermissions` drive readable/writable/required.
- Child tables: per-column writable; do not force all columns writable because `allowUpdate` is true.
- Frontend AiForm must apply permissions; backend still filters/validates.

## IDs

- Path params and variables: string Snowflake ids.

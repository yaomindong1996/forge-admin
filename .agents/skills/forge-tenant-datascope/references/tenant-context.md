# Tenant Context

## Runtime

- Request path: tenant interceptor fills `TenantContextHolder` from login/workspace session.
- MyBatis tenant line interceptor appends `tenant_id` unless ignore flag is set.
- `@IgnoreTenant` (method or type) + aspect → `TenantContextHolder.executeIgnore(...)`.

## When to ignore tenant

- Cross-tenant platform maintenance (rare, audited).
- Async log enrichment that must read user without tenant predicate (see pitfalls).
- Explicit bootstrap/migration jobs that set their own trusted tenant id afterward.

## When to set context manually

- Login-before, token events, scheduled tasks accessing tenant tables.
- Inner calls that cleared context; rebuild from trusted server-side identity, not request params.

## Seeds / SQL

- Business/config seeds: `tenant_id = 1`.
- Do not use MySQL `<=>` NULL-safe equals in tenant interceptor SQL patterns (see pitfalls).
- `LIMIT 1 FOR UPDATE` ordering can break under tenant interceptor — keep lock SQL shapes compatible.

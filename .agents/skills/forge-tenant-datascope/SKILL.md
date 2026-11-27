---
name: forge-tenant-datascope
description: Apply Forge multi-tenant isolation and data-scope (row-level) rules when generating or reviewing modules. Use for TenantContextHolder, @IgnoreTenant, tenant interceptors, DataScopeType ALL/SELF/ORG/ORG_AND_CHILD/CUSTOM/TENANT_ALL/REGION, Mapper XML datascope, role/module scope config, regionCode ALL suffix, and when lists leak cross-tenant or cross-org data. Do not use for tenant billing/product packaging.
---

# Forge Tenant & DataScope

## Purpose

Keep generated queries tenant-safe and row-scoped. Agents must not invent Service-layer `LambdaQueryWrapper` filters that bypass `DataScopeInterceptor`.

## Workflow

1. Decide whether the API is tenant-bound (default) or must ignore tenant (`@IgnoreTenant` / `TenantContextHolder.executeIgnore`) — read `references/tenant-context.md`.
2. Pick `DataScopeType` and ensure list/detail queries live in Mapper XML — read `references/datascope-mapper.md`.
3. If REGION scope or `region_code` filters: read `references/region-scope.md` and `docs/references/region-query.md`.
4. Skim `references/pitfalls-index.md` for matching titles in `code-copilot/memory/pitfalls/security.md`.
5. Finish with `references/validation-checklist.md`.

## Non-Negotiables

- Never trust frontend-supplied `tenantId` / `userId` as authorization truth.
- Built-in seed data uses `tenant_id = 1`, never `0`.
- Data scope rewrites apply to **Mapper XML** methods; Service `LambdaQueryWrapper` queries do not get interceptor rewrite.
- Super-admin must not globally disable tenant isolation.
- Login-before / token events / scheduled jobs that touch tenant tables must establish tenant context explicitly.
- After switching to a business datasource, do not query platform tables under that DS with datascope assumptions.

## References

- `references/tenant-context.md`
- `references/datascope-mapper.md`
- `references/region-scope.md`
- `references/pitfalls-index.md`
- `references/validation-checklist.md`

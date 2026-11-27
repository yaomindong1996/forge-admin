# Tenant / DataScope Checklist

- [ ] APIs do not authorize from client `tenantId`/`userId`.
- [ ] Seeds use `tenant_id = 1` (no `0`).
- [ ] List/page SQL is in Mapper XML (datascope can rewrite).
- [ ] Chosen `DataScopeType` matches product; role/module scope seeded or documented.
- [ ] `@IgnoreTenant` only where justified; context restored afterward if needed.
- [ ] Jobs/login-before paths set tenant context explicitly.
- [ ] REGION/`region_code` follows `docs/references/region-query.md`.
- [ ] Smoke: user A cannot see user B’s out-of-scope rows; cross-tenant leak checked.

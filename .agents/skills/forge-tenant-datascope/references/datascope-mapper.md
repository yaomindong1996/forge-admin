# DataScope & Mapper XML

## Types (`DataScopeType`)

| Code | Meaning |
|------|---------|
| ALL (1) | All data (within tenant rules) |
| SELF (2) | Current user only |
| ORG (3) | Current org |
| ORG_AND_CHILD (4) | Org + children |
| CUSTOM (5) | Custom org/user sets |
| TENANT_ALL (6) | Entire tenant |
| REGION (7) | Administrative region (+ children via snapshot) |

Resolved by `DataScopeServiceImpl` from role / module configs (`SysRoleDataScope`, `SysRoleModuleDataScope`, `SysDataScopeConfig`).

## How filtering applies

1. Put list/page/export SQL in Mapper XML with a stable `mapperMethod` id.
2. `DataScopeInterceptor` rewrites that SQL using current `DataScopeContext`.
3. Do **not** expect Service-layer dynamic wrappers to receive the same rewrite.

## Module generation checklist

- Entity carries `tenant_id` (+ org/user/region columns as required by chosen scope).
- Role datascope configured for the menu/module after resource seed.
- Super-admin org tree must not be clipped by login user `regionCode` incorrectly (pitfalls).

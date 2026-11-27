# Region Scope

Authoritative query rules: `docs/references/region-query.md`.

## Essentials

- Virtual org codes end with `ALL` (e.g. `150100ALL`) = current + descendants semantics for UI selection.
- Hand-written `parent_code = ?` only covers **direct** children.
- Multi-level「本级+全部子孙」for DataScope REGION uses platform snapshot helpers (e.g. `getRegionAndChildCodes`), not a shallow SQL subquery alone.
- Levels: 1 province … 5 village/community; village seeds are manual.

## With DataScopeType.REGION

- Prefer platform datascope path over inventing per-module region SQL.
- Optional OR with user `area_code` when product requires it — keep consistent with interceptor config.

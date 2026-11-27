# Lowcode App Create Checklist

- [ ] Suite exists (or reused) with stable `suite_code`.
- [ ] Object has canonical `object_code` and field registry entries (no `F...` / `input` junk codes).
- [ ] Relations/subTables do not pollute main object field catalog with synthetic columns.
- [ ] App linked to suite/object; menu resource created (`tenant_id = 1`).
- [ ] Form design saved; draft synced before other panels; shelf uses object fields.
- [ ] List design columns/queries/actions coherent; single `START_FLOW` if approval.
- [ ] Publish succeeded; DDL via platform path when schema changed.
- [ ] Runtime `/ai/crud/{configKey}` list/add/edit works.
- [ ] If approval next: objectCode ready for `forge-business-flow-development` (not configKey).
- [ ] Flyway seeds guarded; no raw `${` placeholders for business templates.

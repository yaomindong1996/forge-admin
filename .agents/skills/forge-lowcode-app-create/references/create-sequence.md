# Create Sequence (do not skip steps)

```text
suite → object (+ fields/relations) → app → form design → list design → publish/DDL → runtime CRUD → (optional) flow binding
```

## Rules

1. **Object before form canvas**: field shelf must come from object field catalog, not only components already on the canvas.
2. **App after object**: application points at a real `object_code` / application_object link.
3. **Design before publish**: saving designer draft ≠ publish; runtime reads published/compiled config.
4. **Flow last**: binding and `START_FLOW` assume published runtime page works.
5. **One fact source**: do not half-edit Java code-first Provider and half-edit lowcode schemas for the same object.

## Forbidden skips

- Publishing without form field bindings resolved (no lingering `F...` temporary ids).
- Starting flow with configKey as objectCode.
- Adding subTable columns into main object field list.

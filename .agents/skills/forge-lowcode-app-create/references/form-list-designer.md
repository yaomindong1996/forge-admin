# Form & List Designer Contracts

## Form designer

- Field asset shelf = object field catalog (+ unsaved new fields). Canvas-only shelf makes “unused” empty and deletes fields from the shelf when removed from canvas.
- Before leaving form design for document/flow/action panels: **sync designer draft** so temporary fields persist.
- Layout components (`row`/`col`) are not business fields.
- Saving form design must compile components into edit zone field settings / fieldRefs consistently with runtime AiForm.
- Publish check must flatten row/col when validating required components.

## List designer

- List headers and data query must share the same compiled runtime config.
- Actions: use built-in `START_FLOW` for approval start; do not double-bind custom-action + START_FLOW.
- AiCrudPage column config uses Forge schema props, not raw Naive `title`/`key` alone.

## Runtime rendering

- Prefer design `formDesignerSchema` for approval field catalogs; published `editSchema` alone drifts.
- Long IDs as strings in selects/tree-selects.
- Tree/org selects: empty optionSource must still fall back to platform org APIs where intended.

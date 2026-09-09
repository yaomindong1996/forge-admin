# Low-code preview, publish diagnostics, and table layout

## Requirements

1. Image cells rendered by low-code CRUD pages support click-to-enlarge while retaining authenticated file access.
2. Unexpected application publish failures identify the failed step and provide an actionable, non-sensitive diagnostic reference. The API must not expose stack traces, SQL, tokens, or raw provider payloads.
3. Opening a draft application runtime URL must not issue duplicate loads for the same route state; changing application, draft/runtime mode, or page must still reload as needed.
4. “All columns alignment” applies to all visible table columns, and alignment changes are visible immediately in the designer preview and runtime CRUD columns.
5. Table row spacing is read from the existing row-gap setting and is visible immediately in designer/static previews and runtime `AiTable` tables.
6. Center alignment uses the same geometric center for column headers and every body renderer, including plain text, tags, people/organization cells, images, attachments, and action content. Sort/filter controls must not push centered header text away from that center.
7. Page access entries are optional application assets. An empty selected-entry list means “publish no entries”; disabled or incomplete runtime entries are skipped with a warning instead of blocking publication, while entry IDs owned by another application still fail closed.
8. The low-code business-process workbench supports both interaction paths:
   - drag a node type from the left palette onto a visible insertion point in the canvas;
   - use a `+` insertion control on any process connection, including a concrete branch connection.
   Both paths insert into the selected connection, preserve a valid DAG, select the new node, and immediately open its structured configuration drawer.

## Non-goals

- No Flyway/database schema change.
- No change to file permissions or download authorization.
- No automatic service/database/Flowable end-to-end startup in this turn.

## Safety

- Authenticated image URLs are resolved only by `AuthImage`; the enlarged view reuses its resolved URL.
- Publish diagnostics expose only step name, stable error code, and a short diagnostic reference. Full exception context remains server-side.
- Route deduplication is scoped to the current component instance and route signature.
- Drag-and-drop writes only the governed business-process node type to `DataTransfer`; graph mutation still goes through `useBusinessProcessDesigner` and never accepts arbitrary node JSON.
- Branch insertion targets a concrete edge. The designer does not guess which branch to mutate from a multi-output node.

## Verification status

Implementation and incremental verification completed on 2026-08-06; see `execution-log.md`.

- Authenticated thumbnail URLs are reused by the enlarged preview, and caller thumbnail attributes remain on the single component root.
- Flow client transport failures are converted to an actionable business error; unexpected failures include a safe error code and diagnostic reference.
- Identical application route loads and the initial fallback/runtime CRUD double mount are both suppressed.
- Global alignment overrides both body and header alignment, while row spacing is reactive in designer, static preview, and runtime tables.

## 2026-08-07 incremental acceptance

- A centered body renderer occupies the full data-cell width and aligns its child with `justify-content: center`; its `titleAlign` is also `center`.
- Publishing with `selectedEntryIds: []` processes zero entries. Disabled or unconfigured entries are removed from an explicit selection and reported as skipped; foreign IDs remain blocking.
- Every business-process edge exposes an inline insertion control. Palette drag/drop and inline insertion use the edge-aware insertion API and work on branch edges without rebuilding a second workflow editor.

Implementation and incremental verification completed on 2026-08-07; see `execution-log.md`. Real service/database/Flowable and browser checks remain intentionally user-side.

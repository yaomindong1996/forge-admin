# Template & Binding

## Concepts

- **Template**: layout + fields for a business source.
- **Binding**: attaches a template to a page / form / object for runtime selection.
- **BusinessSource**: identifies where data comes from (`PrintSourceType`: LOWCODE, CODE, standalone variants).
- **Standalone** sources use `businessSourceId` + `sourceCode`; page-follow sources use `pageId` / `formKey` (+ applicationId).

## Page vs standalone

- Lowcode pages: templates **follow the page**; do not orphan designer to global `/print` list incorrectly.
- Code modules: register a business source + Provider; bind from admin print center or module entry.

## Generation tip

When scaffolding a printable bill:

1. Persist stable business key for `PrintRecordRequest`.
2. Expose field catalog via Provider (or lowcode schema).
3. Seed binding after menu/page exists.

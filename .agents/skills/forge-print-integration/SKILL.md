---
name: forge-print-integration
description: Integrate Forge print center — templates, page bindings, business sources, PrintBusinessDataProvider SPI, preview/execute/PDF client rules. Use when adding套打/单据打印, wiring code modules to forge-plugin-print, or fixing print preview/PDF issues. Do not use for full print designer UI encyclopedia or tenant billing.
---

# Forge Print Integration

## Purpose

Connect business modules to `forge-plugin-print` safely: binding + Provider SPI + runtime red lines. Designer UI internals are out of scope.

## Workflow

1. Choose source type (LOWCODE page-follow vs CODE standalone / formKey) — `references/template-and-binding.md`.
2. Implement `PrintBusinessDataProvider` for CODE sources — `references/data-provider-spi.md`.
3. Preview/execute/PDF client constraints — `references/runtime-checklist.md`.
4. Skim `references/pitfalls-index.md` (frontend + backend + lowcode publish).
5. Finish with `references/validation-checklist.md`.

## Non-Negotiables

- Provider `code()` is server-registered; pages must not pick arbitrary Beans.
- Authorize design source + record load under tenant/datascope; never dump raw tables without auth.
- Lowcode **publish must not fail** solely because print field catalog is incomplete.
- Page-bound templates follow the page; designer must not bounce back to `/print` incorrectly.
- New print execution enums must sync Jackson/DTO/Mapper/dict together.

## References

- `references/template-and-binding.md`
- `references/data-provider-spi.md`
- `references/runtime-checklist.md`
- `references/pitfalls-index.md`
- `references/validation-checklist.md`

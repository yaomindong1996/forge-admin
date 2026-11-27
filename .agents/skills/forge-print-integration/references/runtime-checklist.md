# Print Runtime Red Lines (Client + Server)

## Client / PDF

- Do not rebuild flex tables with `html2canvas` for PDF.
- Opaque header backgrounds must not cover table outer borders.
- Selection styles must not `!important`-overwrite header fills.
- Sample images: avoid SVG data URLs where protocol rejects them.
- Font stack validation: not only the first family name.
- Color picker output must match protocol-accepted formats.
- Horizontal/vertical rules: avoid SVG viewBox stroke hacks for print lines.
- Millimeter fields: avoid fragile `NInputNumber` patterns called out in pitfalls.
- Preview new tab: do not `router.back()` as navigation.

## Server / product

- Execution event enums: sync Jackson + DTO + Mapper + dict.
- Style allowlists must not block legitimate display properties.
- Lowcode publish: incomplete print catalog must not block app publish.

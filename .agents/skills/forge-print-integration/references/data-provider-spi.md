# PrintBusinessDataProvider SPI

Interface: `com.mdframe.forge.plugin.print.spi.PrintBusinessDataProvider`.

## Methods to implement

| Method | Duty |
|--------|------|
| `code()` | Stable provider id (server registry) |
| `authorizeDesignSource` | Who may design/bind this source |
| `catalog` | Field catalog for designer |
| `validateDesignResources` | File/resource ids used in design |
| `authorizeRecord` | Who may print this record + params |
| `load` | Return `PrintData` for binding selection |
| `validateRuntimeResources` | Runtime file/resource checks |

## Rules

- Run under current tenant + login actor (`PrintActor`); re-check datascope on `load`.
- Validate params early; reject unknown keys that could widen queries.
- Do not expose unrelated entities via catalog “for convenience”.
- Relation FK mismatches often mean designer column name + `model_schema` drift — fix schema/columns together.

# Templates and Seeds

## Tables

- `sys_message_template` — `template_code`, channel-specific bodies, enable flag, `del_flag`
- `sys_message_biz_type` — business type catalog (unique with `del_flag` tombstone pattern)

## Known flow templates (examples)

From `V1.0.72__collaboration_sync_schedule_and_todo_card_template.sql`:

- `FLOW_TODO_CARD`
- `FLOW_TODO_CARD_WECOM` / `DINGTALK` / `FEISHU`

Reuse platform templates when possible; add business-specific codes only when needed.

## Seed rules

- `tenant_id = 1`
- `NOT EXISTS` guards on `template_code`
- Escape Flyway `${` in content: `CONCAT('$', '{title}')` style
- Logical delete / active unique indexes follow project tombstone rules (`del_flag`)

## Runtime

`resolveEnabledTemplateCode(preferred, fallback...)` picks the first enabled code for multi-channel cards.

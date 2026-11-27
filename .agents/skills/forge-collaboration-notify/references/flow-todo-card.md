# Flow Todo Card Templates

`FlowTaskNotifyListener` resolves templates with `MessageService.resolveEnabledTemplateCode(...)`:

1. Optional override template code
2. Platform-specific: `FLOW_TODO_CARD_WECOM` / `FLOW_TODO_CARD_DINGTALK` / `FLOW_TODO_CARD_FEISHU`
3. Fallback: `FLOW_TODO_CARD`

These codes are built-ins in `MessageTemplateAccessPolicy`. Prefer enabling platform variants over forking listener logic.

## Params

Business process / flow executors fill placeholders (task title, process name, node, etc.). Keep `${` Flyway-escaped in seeds (`CONCAT('$','{var}')`).

## Guidance

- Extend notify carefully — platform already pushes todos; custom module sends should use distinct `bizType`/`bizKey` to avoid double spam.
- If only站内信 is needed, stay on web channel and skip this skill’s IM path.

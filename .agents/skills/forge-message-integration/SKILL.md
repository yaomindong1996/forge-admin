---
name: forge-message-integration
description: Integrate Forge messaging for business modules — message biz types, templates, channels, idempotent send via MessageService.sendIfAbsent, and mark-read-by-biz for flow completion. Use when adding站内信/通知/模板种子, wiring approve/reject/submit notifications, or reviewing forge-plugin-message usage. Prefer flow callbacks or server-side sends over frontend-only fire-and-forget.
---

# Forge Message Integration

## Purpose

Wire business events to `forge-plugin-message` without rediscovering template/channel/idempotency rules.

## Workflow

1. Decide trigger points (submit, todo created, approved, rejected, canceled).
2. Seed or reuse `bizType` + template (`templateCode`) + channel. See `references/templates-and-seeds.md`.
3. Send with `MessageService.sendIfAbsent(req, bizType, bizKey)` for idempotency. See `references/send-api.md`.
4. On flow complete/cancel, mark web messages read by biz when appropriate (`markWebReadByBiz`).
5. Lowcode: prefer binding callback / platform action — avoid duplicate sends from custom frontend actions.
6. Checklist: `references/validation-checklist.md`.

## Non-Negotiables

- Prefer `sendIfAbsent` for business-keyed events.
- Flyway: escape `${` in template bodies (`CONCAT('$', '{var}')`).
- `tenant_id = 1` for built-in templates.
- Do not log secrets; mask API keys in responses.
- Flow todo notify already exists in platform listeners — extend carefully; don’t double-spam.

## References

- `references/send-api.md`
- `references/templates-and-seeds.md`
- `references/validation-checklist.md`

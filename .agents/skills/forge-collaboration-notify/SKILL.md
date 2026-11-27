---
name: forge-collaboration-notify
description: Wire Forge IM collaboration notify (WeCom/DingTalk/Feishu) via forge-plugin-collaboration — connections, callbacks, COLLABORATION message channel, flow todo cards, outbound COLLABORATION_PROVIDER whitelist. Use when pushing企业微信/钉钉/飞书 messages or configuring collaboration connections. For站内信 templates/idempotency use forge-message-integration first; this skill owns the IM channel path.
---

# Forge Collaboration Notify

## Purpose

Send business/flow notifications through collaboration IM providers without reinventing crypto, connection config, or channel wiring.

## Skill boundary

| Concern | Skill |
|---------|--------|
| Template seed, `bizType`, `sendIfAbsent`, mark-read | `forge-message-integration` |
| Connection / callback / WeCom crypto / `channel=COLLABORATION` / todo card platform templates | **this skill** |

Typical order: define message template → send via `MessageClient`/`MessageService` with channel `COLLABORATION` → collaboration plugin delivers.

## Workflow

1. Ensure connection + apps configured; credentials masked in API — `references/connection-and-callback.md`.
2. Business/flow send path — `references/send-via-message-channel.md`.
3. Flow todo cards — `references/flow-todo-card.md`.
4. Pitfalls index + checklist.

## Non-Negotiables

- Outbound HTTP for providers must use scene `COLLABORATION_PROVIDER` whitelist.
- Never log plaintext corp secrets / tokens; responses use secret summaries.
- Callback verify window / crypto must stay provider-compatible; do not weaken signature checks.
- Prefer platform flow listeners for todo notify; avoid duplicate custom pushes.
- Unsupported IM `msgType` or oversize card fields must fail closed before provider call.

## References

- `references/connection-and-callback.md`
- `references/send-via-message-channel.md`
- `references/flow-todo-card.md`
- `references/pitfalls-index.md`
- `references/validation-checklist.md`

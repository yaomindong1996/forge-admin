# Message Send API

Interface: `com.mdframe.forge.plugin.message.service.MessageService`

## Primary methods

- `send(MessageSendRequestDTO req)` — direct send.
- `sendIfAbsent(MessageSendRequestDTO req, String bizType, String bizKey)` — **preferred** for business events; skips if same bizType+bizKey exists.
- `markWebReadByBiz(bizType, bizKey[, userId])` — clear站内信 when流程办结等.
- `resolveEnabledTemplateCode(String... candidates)` — pick first enabled template (e.g. WeCom-specific then fallback).

## MessageSendRequestDTO (key fields)

- `title` / `content` or `templateCode` + `params`
- `userIds` / `orgIds` / `sendScope` (`ALL`/`ORG`/`USERS`)
- `channel` (`WEB`/`SMS`/`EMAIL`/`PUSH`/`COLLABORATION`)
- `type`, `bizType`, `bizKey`, optional `idempotentKey`, `connectionId` for collaboration

## Placement

- Code-first: Service/Facade after status transition (not Controller clutter; avoid Service↔Service cycles — use Facade if needed).
- Flow: platform notify listeners + optional `callbackActions`; align with `pitfalls/flow.md`「流程待办消息完成后必须自动置已读」.

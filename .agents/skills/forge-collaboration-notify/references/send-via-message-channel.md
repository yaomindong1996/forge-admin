# Send via COLLABORATION Channel

## Path

1. Seed/enable message template (see `forge-message-integration`).
2. Send with `MessageClient.sendToRecipients("COLLABORATION", ChannelSendRequest)` or `MessageService` APIs that select the collaboration channel.
3. `CollaborationMessageChannel` validates via `CollaborationMessageTemplatePolicy` then delivers per user binding.

## Template policy highlights

- Supported `msgType`: `text`, `textcard` (params key `msgType`).
- Byte limits (UTF-8): text 2048; card title 128 / description 512 / url 2048.
- Card requires title + http(s) URL (`params.url`).

## Delivery retry

- Failed deliveries may be retried by `CollaborationDeliveryRetryService` still on channel `COLLABORATION` with idempotency key — do not invent a second send pipeline.

## Outbound

- Provider API hosts must be allowed under `COLLABORATION_PROVIDER` scene.

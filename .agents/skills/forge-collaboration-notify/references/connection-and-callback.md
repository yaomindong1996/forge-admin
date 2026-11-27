# Connection & Callback

## Connection

- Admin APIs under `forge-plugin-collaboration` (`CollaborationConnectionController`).
- Stores social/collaboration connection + app configs; detail VO uses `secretSummary` — never echo raw secrets.
- Save/update paths may use API encrypt/decrypt annotations — keep DTO contracts intact.

## Callback / crypto

- Provider callbacks (e.g. WeCom) verify signature/timestamp window and decrypt payload via plugin crypto helpers.
- Reject expired/invalid signatures; do not “skip verify in local” in shared code paths.
- Token providers (`WeComAccessTokenProvider`) call outbound with `OutboundScenes.COLLABORATION_PROVIDER`.

## Ops tip

- Test send endpoint exists (`CollaborationMessageTestController`) with channel `COLLABORATION` — use for smoke only with real recipient ids you own.

# Collaboration Notify Checklist

- [ ] Message template + channel path decided (`forge-message-integration` first if new template).
- [ ] Connection/app configured; secrets masked in API responses.
- [ ] Sends use channel `COLLABORATION` (not ad-hoc HTTP from business code).
- [ ] Outbound scene `COLLABORATION_PROVIDER` allows provider hosts.
- [ ] Flow todos use `resolveEnabledTemplateCode` platform → `FLOW_TODO_CARD` fallback.
- [ ] No double-send with platform flow listener.
- [ ] Card/text size and URL policy respected; fail closed on invalid msgType.
- [ ] Smoke via test-send or controlled recipient only.

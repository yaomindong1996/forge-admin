# Message Integration Checklist

- [ ] bizType + templateCode decided; template queryable for tenant 1 (or target tenant).
- [ ] Send path uses `sendIfAbsent` (or documented why not).
- [ ] bizKey stable and unique per business event (e.g. `objectCode:recordId:APPROVED`).
- [ ] Recipients resolved (userIds/org/scope); empty recipients logged/handled.
- [ ] Flow completion marks related WEB messages read when required.
- [ ] Flyway templates have no raw `${` placeholders.
- [ ] No duplicate frontend + backend double send for the same event.

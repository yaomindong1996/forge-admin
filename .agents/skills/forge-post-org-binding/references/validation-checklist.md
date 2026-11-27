# Post Binding Checklist

- [ ] Post master row exists (code unique, enabled) for target tenant.
- [ ] Users bound via `bindUserPosts` with correct mainPost when needed.
- [ ] Flow/node config uses post **id** for `assigneeType=post`.
- [ ] Frontend keeps ids as strings.
- [ ] No duplicate custom post table in the business module.
- [ ] Smoke: user with that post appears in resolved assignee list / can see todo.

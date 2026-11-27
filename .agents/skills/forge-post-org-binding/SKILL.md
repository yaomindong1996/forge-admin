---
name: forge-post-org-binding
description: Work with Forge posts (SysPost) — master data APIs, user-post binding, and using posts as Flowable assignees (assigneeType=post). Use when seeding posts, binding users to posts, configuring approval by post, or reviewing sys_post / user post relations. Pair with forge-business-flow-development for BPMN assignee wiring.
---

# Forge Post / Org Binding

## Purpose

Document how posts relate to users and flow assignees so agents stop guessing ids vs codes.

## Workflow

1. Post master data via `/system/post` APIs. See `references/post-master-data.md`.
2. Bind users with `bindUserPosts(userId, postIds, mainPostId)`. See `references/user-bind.md`.
3. Flow: node `assigneeType=post`, `assigneeValue` = **post id string**. See flow Skill `assignee-and-post.md`.
4. Checklist: `references/validation-checklist.md`.

## Non-Negotiables

- Flow post resolver uses **post id**, not `postCode` (unless you explicitly resolve code→id first).
- Keep Snowflake/post/user ids as strings in frontend payloads.
- `post_code` unique per tenant among active rows; seeds `tenant_id = 1`.
- Do not invent a parallel “岗位表” in business modules — use `sys_post`.

## References

- `references/post-master-data.md`
- `references/user-bind.md`
- `references/validation-checklist.md`
- Cross-read: `../forge-business-flow-development/references/assignee-and-post.md`

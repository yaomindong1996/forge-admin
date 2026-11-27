# User ↔ Post Binding

## Service

`SysUserService` / `SysUserRelationCoordinator.bindUserPosts(userId, postIds, mainPostId[, tenantId])`

## Controller surfaces (`SysUserController`)

- Query user post ids (tenant-aware helpers exist on user APIs).
- Batch bind posts endpoint (operation log: 绑定用户岗位).
- User detail payloads include `postIds`.

## UI / forms

- Store **post id** lists on the user; display names via join labels (`selectPostNameLabels`).
- Business forms that “pick a post” should persist post id (or resolve to user ids before flow start variables).
- If the process needs a concrete assignee user list, resolve post→users server-side (`FlowOrgIntegrationService.getUserIdsByPostId`) rather than guessing in Vue.

## Flow handoff

When configuring designer node:

- `assigneeType = post`
- `assigneeValue = "<postId>"` (string)

See `forge-business-flow-development/references/assignee-and-post.md`.

# 交互登录走 start-delegated 时补委托 token
> status: apply
> created: 2026-09-02
> complexity: 🟢简单

## 1. 目标

`flowClient.startProcessForDelegatedUser(...)` 在普通已登录请求（无 MCP ExecutionIdentity）下，签发带 `forge:flow:delegation` 的短期 token，通过 `requireTrustedDelegation()`。

## 2. 规则

- 已有 USER ExecutionIdentity：保持现有 MCP 委托 token。
- 已登录且 Session 中有完整 LoginUser（userId/tenantId/activeOrgId）：从 Session 构建 USER 身份并签发 60 秒委托 token。
- LoginUser 不完整或取 Session 失败：回退透传当前登录 token，避免误伤非委托接口。

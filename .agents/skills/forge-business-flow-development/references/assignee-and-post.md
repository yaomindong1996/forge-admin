# Assignees, Candidates, and Posts

## Designer node assigneeType

`FlowNodeConfigServiceImpl` resolves approvers by `assigneeType`:

| assigneeType | assigneeValue | Resolves via |
|--------------|---------------|--------------|
| `user` | user id(s), comma or JSON array | direct |
| `role` | role code or role id | org integration |
| `dept` / `deptUser` | dept id | users in dept |
| `post` | **post id** (string) | `getUserIdsByPostId` |
| `leader` | — | from process variables |
| `deptManager` | — | from process variables |
| `initiator` | — | starter user |
| `expr` | SpEL in `assigneeExpr` | variables must be mapped |

Also see Skill `forge-post-org-binding` for `sys_post` / user bind.

## BPMN variable assignees (code-first sample)

Purchase order style:

```xml
flowable:assignee="${deptLeaderId}"
```

Start/submit must put `deptLeaderId` (and peers) into variables as **string** user ids. Binding `variableMapping` must list every `${var}` used in BPMN.

## candidateGroups

Static `flowable:candidateGroups="..."` is checked at model preflight. Expression groups need runtime mapping; empty candidate + empty assignee → no todo recipients / no notify targets.

## Rules

- Prefer designer `assigneeType=post` with post **id**, not postCode, unless org integration explicitly resolves codes (current post path uses post id).
- Do not save fixed approver as a user-variable expression that never gets a value (`pitfalls/flow.md`: 固定审批人不能保存为用户变量表达式).
- Every expression variable needs a matching lowcode/code variable mapping.
- `sys_flow_task.assignee` stores user id.

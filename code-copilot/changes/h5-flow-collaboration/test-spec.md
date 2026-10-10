# H5 审批协同 Test Spec

## 1. 环境

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
cd forge-h5-ui && npx -y pnpm@9 install
```

后端编译和测试需要本机 JDK 17 和 Maven；本机没有时记录"跳过"，由用户在真实环境补测。

## 2. 前端 Node 单测

```bash
cd forge-h5-ui && node --test src/utils/__tests__ src/store/modules/__tests__
```

基线：`h5-office-essentials` 完成后 142 通过、0 失败。本变更只新增和追加用例；拆分导致的断言迁移只改读取的文件，不放宽断言。

| 用例 | 文件 | 预期 |
|------|------|------|
| 抄送数据 | `flow-cc.test.js` | 分页归一化；标题缺省用流程名；未读数超过 99 显示 `99+` |
| 催办 | `flow-remind.test.js` | 仅运行中且有办理人可催办；冷却 10 分钟内为真，过期为假 |
| 加签 | `flow-sign.test.js` | 仅办理人/所有者、处理模式、节点允许时可见；减签只列 `status = 1`；姓名缺失回退为 ID |
| 抄送 store | `store/modules/__tests__/cc.test.js` | 已读后未读数减 1 且不为负；全部已读清零；接口失败保留旧值 |
| 契约 | `console-design-system.test.js` | spec 8.2 五项 |

## 3. 构建

```bash
cd forge-h5-ui && npx -y pnpm@9 build:h5
```

构建必须通过。已知且可接受的提示：`badge.js` 动态导入 `@/api` 和 `./auth`。

## 4. 后端

```bash
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am compile
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am test -Penable-tests \
  -Dtest='FlowTaskRemindCoordinatorTest,FlowTaskCandidateRelationContractTest,FlowTaskNodePolicyTest,FlowTaskSignContractTest,FlowTaskMutationAuthorizationContractTest' \
  -Dsurefire.failIfNoSpecifiedTests=false
```

| 用例 | 预期 |
|------|------|
| 催办 | 未签收报错；key `forge:flow:remind:{tenantId}:{taskId}:{userId}`、10 分钟；重复催办报错；Redis 异常放行；发送失败不释放 key |
| 加签姓名 | SQL 返回 `targetUserName`，关联含租户和 `del_flag = 0`，没有 `${}` |
| 节点策略 | `allowAddSign` 未配置为允许；配置 `false` 时加签被拒、减签正常 |
| 回归 | `FlowTaskSignContractTest` 等已有用例通过 |

## 5. 联调验收（用户执行）

需要 admin-server、flow-server、app-server 和 H5 同时启动；员工角色授予 `flow:task:remind`。

1. 管理端给员工 A 发一条抄送：A 的待办页"抄送我的"页签出现未读数，底部"待办"角标不变。
2. A 打开抄送详情：看到抄送信息、审批流程；业务表单能显示或显示"请在 PC 端查看"；返回后该条变为已读，未读数减 1。"全部已读"清零。
3. 员工 B 发起审批，审批人 C 已签收：B 在"我发起的"点"催办"，C 收到"流程催办提醒"站内信；10 分钟内再点提示"已催办过"。
4. 审批人尚未签收的任务：不显示催办按钮；直接调接口返回"任务尚未被签收"。
5. C 在审批详情"更多"中加签 D：D 的待办出现该任务；C 再减签 D：D 的待办消失。加签人员列表显示姓名。
6. 发起人 B 打开只读详情：没有加签/减签入口。
7. （若 10.4 按推荐执行）把某节点配置为不允许加签：C 看不到加签入口，直接调接口被拒；减签仍可用。

## 6. 视觉验收

- 本地 `npx -y pnpm@9 dev:h5`，390×844 视口截图：待办页"抄送我的"、抄送详情、"我发起的"催办按钮、更多操作（含加签/减签）、加签选人、减签列表、加签人员展示。
- 后端未启动时用 Pinia/组件状态注入数据预览，记录为"待用户联调"。

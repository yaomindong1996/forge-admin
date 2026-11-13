# 执行记录：H5 支持发起低代码「业务流程」

## 2026-11-13 阶段一编码与验证

- 版本：未发版，变更计入 `CHANGELOG.md` Unreleased。
- 状态：代码已完成并自测，待提交 / 待部署（演示环境验证使用本地 H5 dev server 代理演示网关）。

### 自动化验证

| 命令 | 结果 |
|------|------|
| `cd forge-h5-ui && node --test`（Node 24.21） | 207 条，204 通过，3 失败 |
| 同命令，暂存本次改动后（基线） | 200 条，197 通过，3 失败（同 3 条） |
| `npx uni build --outDir /tmp/h5-build-check` | Build complete |

失败的 3 条为存量契约测试，与本次改动无关：
`mobile registry covers every component type exposed by the admin designer`、
`mobile field contract classifies every property exposed by the PC designer`、
`mobile layout contract classifies every property exposed by the PC designer`。

新增用例：业务流程动作收集、去重与已发起隐藏、权限与显示条件、运行失败原因解析、
store 合并主流程/业务流程按钮、非单据对象保留流程状态、列表行操作排除 `START_PROCESS`。

### 演示环境浏览器验证（ego-browser，H5 `localhost:3009` → 演示 H5 网关）

应用「费用报销E2E」（`hr_e2_e`），业务流程 `approval_qsxf`（报销审批），对象 `apply_kdcp`。

1. 单据 1 详情页底部出现业务流程按钮「手动开始」；确认弹窗「确认发起「手动开始」吗？」。
2. `start-config` 返回 `initiatorSelectNodes: []`，跳过选人；`POST start` 创建运行
   `2121250129455792129`；审批节点失败：「当前单据已有主流程实例，不能重复发起」（单据 1 已走过对象主流程）。
   失败运行不计入已发起，按钮保留；再次点击由服务端重试同一运行（未新增运行），状态变为 PENDING 后按钮隐藏。
3. 单据 2：首次发起由后端返回业务错误，页面如实提示失败原因；重试后提示「业务流程已启动」，按钮隐藏。
4. 刷新防闪烁：发起过程 543 次 DOM 采样，状态卡缺失 0 次。

### 未完成 / 跳过

- 完整审批闭环（H5 新建 → 业务流程审批 → 已通过）未走通：演示数据源已按约定恢复只读，无法新建单据；
  存量单据均已有对象主流程实例。需可写数据源或无主流程的新单据再验收 V1。
- V4 无权限账号未实测，仅单测覆盖。
- 演示环境新增的运行数据：单据 1、2 各一条 `approval_qsxf` 运行记录（PENDING，重试中）。

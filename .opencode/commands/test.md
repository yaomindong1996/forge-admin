---
description: 按变更范围运行回归测试、验证真实业务入口并记录证据
agent: general
---

# /test

目标 `$ARGUMENTS`

1. 读 `AGENTS.md`、`code-copilot/rules/automated-testing-standard.md`。参数可以是变更名、路径、测试类或测试目标；无参数时用当前变更的 diff。已有 Spec 时读 `spec.md`、`tasks.md`、`test-spec.md` 和 `execution-log.md`（如存在），保留历史证据，只补本轮范围。
2. 根据受影响的入口选验证。先跑现有相关检查和测试，再跑 API 或浏览器主路径。后端 Maven 测试用 `-Penable-tests`，前端用目标测试，结构与文档变更跑对应门禁。新增回归用例时验证它能复现原缺陷，再确认修复后通过；对既有测试直接执行。覆盖率只报告实际测出的值和 Spec 约定的指标。
3. 按变更加载对应 Skill。
   - 菜单/API 授权读 `.agents/skills/forge-resource-permissions/SKILL.md`。用新会话的普通账号检查页面、按钮、方法及路径、直调拒绝与匿名拒绝。测试真实服务端守卫，不用超级管理员或内部调用身份代替普通账号。
   - 数据范围同时读 `.agents/skills/forge-tenant-datascope/SKILL.md`。同角色换组织、换租户并查实际记录 ID；列表、详情、导出按实际影响分别验证，核对分页总数和文件内容。覆盖缺少操作权限但有页面权限的账号。
   - Flyway 读 `.agents/skills/forge-flyway-migration/SKILL.md`。核对方言成对、版本唯一和历史文件；在独立库验证空库、升级、重复启动以及迁移后的业务查询。隔离仿真与真实数据库按引擎标明。
4. 测试失败先区分当前回归、基线失败与环境阻塞。保留失败断言和错误，不删断言或降低检查强度。范围内需要新增测试或修复时按用户授权执行并重跑；有风险的环境写入、发布、部署或业务数据变更须另有授权。
5. 将实际命令、目标与结果（通过、失败、未执行及原因）、关键业务证据追加到现有变更的 `execution-log.md`。缺少且任务采用 Spec 工作流时按模板新建必要记录；独立检查直接汇报。仅按证据更新相关任务状态，标出仍待验证的主路径。

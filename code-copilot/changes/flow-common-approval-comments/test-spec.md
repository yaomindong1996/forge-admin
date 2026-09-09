# 常用审批意见 Test Spec

## P0

1. Mapper XML 必须带 `tenant_id` 和 `del_flag = 0`；逻辑删除写 `del_flag = id`。
2. 写接口使用 DTO，禁止 `@RequestBody Map`。
3. 个人意见接口不依赖管理权限；企业意见写入要求 `flow:comment-phrase:manage`。
4. `FlowCommentPhraseInput` 能渲染芯片、点选回填、在内容变化后显示存为常用。

## P1

1. 前端管理页使用字典，不写死场景/状态 options。
2. Flyway 具备 `IF NOT EXISTS` / `NOT EXISTS`，`tenant_id = 1`，无 `${}` 占位符。

## 本轮命令

- 插件模块契约测试：`mvn test -Penable-tests -Dtest=FlowCommentPhrase*`
- Flow 服务契约测试：同名 Controller 测试
- 前端：`pnpm exec vitest run src/components/flow/__tests__/FlowCommentPhraseInput.spec.js`
- 前端 lint：`pnpm exec eslint` 针对改动文件
- 后端编译：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-flow/forge-flow-server -am compile -DskipTests`

## 跳过

- 不启动 Admin/Flow 真实服务，不跑 Flyway 实库，不做浏览器 E2E（用户偏好：真实联调由用户执行）。

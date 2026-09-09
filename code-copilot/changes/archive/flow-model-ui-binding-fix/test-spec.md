# 测试规格

## P0

- `pnpm exec eslint src/views/flow/model.vue src/views/flow/design.vue`：无错误。
- `pnpm build`：前端生产构建成功。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：Java 17 编译成功。
- `xmllint --noout .../BusinessBindingMapper.xml` 与 `git diff --check`：通过。

## P1

- 流程工具单测：`pnpm exec vitest run src/views/flow/utils/__tests__/monitorAdmin.spec.js`。
- 浏览器联调：本轮未启动服务；待用户环境登录后验证模型卡片和设计页交互。

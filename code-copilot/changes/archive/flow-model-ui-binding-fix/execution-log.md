# 执行日志 — 流程模型配置与业务绑定修复

## 2026-08-30

- 变更范围：`forge-admin-ui/src/views/flow/model.vue`、`design.vue`；业务绑定摘要 VO 与 Mapper XML。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && npx --yes pnpm@8.15.9 --dir forge-admin-ui exec eslint src/views/flow/model.vue src/views/flow/design.vue`：通过，0 errors；保留 4 条既有单行模板 warning。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && npx --yes pnpm@8.15.9 --dir forge-admin-ui exec vitest run src/views/flow/utils/__tests__/monitorAdmin.spec.js`：通过，7/7。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && npx --yes pnpm@8.15.9 --dir forge-admin-ui build`：成功；仅有既有 Vite/CSS 注释 warning。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=.../bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：成功；存在既有 deprecated/unchecked warning。
- `xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessBindingMapper.xml`、`git diff --check`：通过。
- 浏览器联调跳过：本机前端 5173 未监听，未启动真实服务或修改运行态数据库。

## 2026-08-30（本轮兼容性补丁）

- 补充设计器历史绑定兜底：`effectiveApplicationId/businessApplicationName` 读取 `resolvedBusinessBinding`，并修正嵌套 `formRef` 与根级字段为空时的优先级。
- 补充流程列表绑定合并：优先展示表单引用中用户实际选择的业务应用，避免共享业务对象时被接口反查的主应用覆盖。
- 补充测试发起上下文恢复：兼容流程表单引用、列表业务绑定和 `app_<id>_page_...` 表单 Key 三种应用 ID 来源。
- `git diff --check`：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && npx --yes pnpm@8.15.9 --dir forge-admin-ui exec eslint src/views/flow/model.vue src/views/flow/design.vue`：通过，0 errors；保留 4 条既有模板换行 warning。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && npx --yes pnpm@8.15.9 --dir forge-admin-ui exec vitest run src/views/flow/utils/__tests__/monitorAdmin.spec.js`：通过，7/7。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 npx --yes pnpm@8.15.9 --dir forge-admin-ui build`：成功；保留既有 Vite 配置、CSS 注释、动态导入和 bundle 体积 warning。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：成功；保留既有 deprecated/unchecked warning。
- 浏览器联调仍跳过：本机未启动 Admin/Flow 服务，未修改运行态数据库；需用户环境登录后验证级联选择、重新打开回显、发起测试表单和流程卡片绑定名称。
- 全量前端 Vitest：`NODE_OPTIONS=--max-old-space-size=8192 npx --yes pnpm@8.15.9 --dir forge-admin-ui exec vitest run`，124 个测试文件通过（881 个测试），`application-designer-phase-e-contract.spec.js` 中 2 个既有布局契约失败；失败断言涉及应用设计器历史实现，与本轮流程模型/业务绑定改动无关。

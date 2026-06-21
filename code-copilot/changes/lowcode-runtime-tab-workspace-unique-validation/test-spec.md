# 测试方案

## 前端

- 执行针对改动文件的 ESLint：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiCrudPage.vue src/components/ai-form/AiCrudPageProps.js src/views/ai/crud-page.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`
- 执行前端构建：
  - `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`

## 后端

- 执行 generator 模块编译：
  - `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`

## 手工验收建议

- 发布一个运行态配置，将 `options.formOpenMode` 改为 `tabWorkspace`，打开运行页后连续点击两次新增，确认出现两个草稿页签。
- 编辑同一条数据两次，确认复用同一编辑页签。
- 在一个草稿页签输入内容后切换到另一个页签再切回，确认草稿保留。
- 给客户对象配置 `modelSchema.uniqueConstraints` 的 `customerName` 唯一，新增重复客户名称，确认后端返回“客户名称已存在”。

## 本轮增量验证 2026-06-21

- 静态差异：执行 `git diff --check`。
- 前端静态检查：执行上述针对性 ESLint，覆盖运行态页面、AiCrudPage 和设计器打开方式配置。
- 后端编译：执行 generator 模块 Maven compile，覆盖唯一校验 DTO、仓库查询和动态 CRUD 服务。
- 前端构建：执行 `pnpm --dir forge-admin-ui build`，覆盖运行态与设计器打包链路。
- 未启动本地前后端服务；数据库级唯一校验接口需在本地后端和 MySQL 可用时按手工验收建议补充验证。

## 本轮增量验证 2026-06-21 唯一校验补漏

- 静态差异：执行 `git diff --check`。
- 后端编译：执行 generator 模块 Maven compile，覆盖主子表/关联运行态新增编辑分支中的唯一校验调用。
- 本轮仅修改后端 Java 服务，未重复执行前端 lint/build。
- 未启动本地后端和 MySQL；真实重复新增拦截需在已发布客户对象配置上补充接口验证。

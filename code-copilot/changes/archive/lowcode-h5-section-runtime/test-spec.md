# H5 低代码运行时多区域单页改造 Test Spec

## 1. 本轮范围

验证 H5 多区域协议、移动端组件编译、旧运行路径兼容、新建后动作执行所需的创建响应，以及预售 pageSections 配置迁移。本轮不连接或修改真实 MySQL，不调用真实企业微信、摄像头或外围接口。

## 2. P0 必跑

| 检查 | 命令 | 预期 |
|---|---|---|
| Section 协议纯函数 | `cd forge-h5-ui && node --test src/utils/__tests__/lowcode-runtime.test.js` | 全部通过 |
| H5 生产构建 | `cd forge-h5-ui && pnpm build:h5` | Build complete |
| CRUD 创建响应 | `cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Penable-tests -Dtest=DynamicCrudControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` | 测试通过 |
| 预售迁移合同 | 同模块执行 `PresaleRegistrationLowcodeMigrationContractTest` | 测试通过 |
| Flyway 占位符 | `rg -n '\$\{[^}]+\}' forge-server/db/migration/V1.0.109__add_presale_h5_page_sections.sql` | 无输出 |
| 差异格式 | `git diff --check` | 无空白错误 |

所有前端命令先执行 `source ~/.nvm/nvm.sh && nvm use v20.19.0`。

## 3. P1 检查

- 静态确认旧表单分支仍由 `!hasPageSections` 进入，原保存/取消/详情按钮保留。
- 静态确认 `displayCondition` 复用 `actionVisible`，不使用 `eval` 或 `new Function`。
- 静态确认 V1.0.109 使用 `tenant_id = 1`，并同步当前配置和发布版本快照。
- 使用 Playwright mock 后端协议，在 390x844 与 360x740 视口验证区域顺序、pill 切换、inline_grid、bottom_sheet、底栏显隐、横向溢出，以及“创建记录取得 ID 后执行 submit_presale”的请求顺序。
- 启动 H5 开发服务，确认编译无运行时模板错误；真实登录后的预售交互由具备后端与数据库的环境验收。

## 4. 跳过项

- 真实 Flyway/MySQL：避免改动用户数据库。
- 真实 Admin/App 服务、企微 JS-SDK、手机摄像头及外围接口/真机 E2E：依赖外部运行环境和业务账号；本轮已完成 mock 浏览器 E2E。
- 管理端设计器：本次明确不增加 pageSections 可视化编辑能力。

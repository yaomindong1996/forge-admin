# 单测 Spec — 用户反馈平台问题修复
> status: complete
> created: 2026-07-15

## 0. 测试原则

- 先复用仓库已有前端 Vitest、generator JUnit 和构建基线，不启动真实 Admin/Flow/MySQL/Redis。
- 低成本静态检查先行，再执行定向单测、相关模块聚合编译和前端生产构建。
- 加解密开关按安全变更处理：覆盖关闭、开启和配置端点失败时保持默认开启三类场景。
- 导入进度区分文件上传和服务端处理，不用模拟数值冒充后端逐行进度。
- 所有命令、结果、警告和跳过项追加到 `execution-log.md`。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| 前端 | Vitest + Vue 3；Node v20.19.0 |
| 后端 | JUnit 5 / Spring Boot Test（Excel、Crypto Starter 独立执行） |
| Excel | EasyExcel 写入，Apache POI/工作簿读取验证 |
| SQL | `xmllint` + 静态契约检查；不连接真实 MySQL |

## 2. 覆盖范围

### P0 — 核心业务逻辑

| 类/模块 | 场景 | 输入 | 预期结果 |
|---------|------|------|----------|
| `message-notification-utils.js` | 未完成流程消息 | `FLOW_TODO/readFlag=0` | 可显示并触发审批 |
| `message-notification-utils.js` | 已办流程消息 | `FLOW_TODO/readFlag=1` | 不显示、不触发审批 |
| `import-utils.js` | 超过 20 行预览 | 表头 + 25 行 | 总行数 25、只返回前 20 行 |
| `import-utils.js` | 动态/通用错误字段兼容 | `message/errorMessage`、`label/columnName` | 统一为可渲染错误行 |
| `crypto-config.js` | 后端全局关闭 | `enabled=false` | 前端 `enabled=false`、防重放按返回配置同步 |
| `crypto-config.js` | API 子开关关闭 | `enabled=true, enableApiCrypto=false` | 前端 API 加密关闭 |
| `crypto-config.js` | 配置加载失败 | 请求异常 | 保持静态 `enabled=true`，不降级明文 |
| `ExcelImportTemplateWriter` | 生成导入模板 | 必填、可选两列 | 两个工作表、样例行、字段说明均存在 |
| `CryptoEncryptorLifecycleTest` | 关闭状态启动 | `enabled=false` + 无效 SM4/AES/RSA 历史值 | 加密器和 RSA Holder 可创建，不在启动阶段校验无效密钥 |
| `CryptoEncryptorLifecycleTest` | 运行时重新开启 | 先关闭并设置无效值，再切换有效 16 字节密钥 | 同一 SM4/AES 实例读取最新密钥并完成加解密往返 |

### P1 — 数据访问层

- `SysUserOrgRoleMapper.xml` 通过 XML 语法检查。
- `selectRoleNamesByUserOrg` 不再使用 `SELECT DISTINCT r.role_name`。
- 查询包含 `GROUP BY r.id, r.role_name, r.sort` 和原排序条件。

### P2 — 入口与集成层

- `forge-starter-crypto`、`forge-starter-config`、`forge-starter-auth`、`forge-starter-excel` 通过 Admin reactor 聚合编译。
- 前端生产构建覆盖新导入弹窗、运行配置启动加载、消息和模型页面。
- 不启动真实服务；`/crypto/config` 匿名访问、配置切换真实密文/明文与用户关联管理接口由用户环境做 E2E 回填。

## 3. 执行计划

- [x] Step 1: 执行新增前端纯函数测试并确认修复后通过。
- [x] Step 2: 执行 Excel 模板生成测试并确认两个工作表协议。
- [x] Step 3: 执行 Mapper XML 与 SQL 静态检查。
- [x] Step 4: 执行 Admin reactor package 与前端 production build。
- [x] Step 5: 执行 `git diff --check`，回填文档状态与执行证据。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-15 前 | 当前工作区 | 见 `app-first-lowcode-workbench/execution-log.md` | 多阶段历史构建通过，但当前工作区此后仍有大量未提交差异 | 本变更必须重新验证目标差异，不能直接复用为最终结论 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-15 | 前端纯函数 | Vitest | `pnpm exec vitest run src/components/ai-form/__tests__/import-utils.spec.js src/layouts/components/__tests__/message-notification-utils.spec.js src/utils/crypto/__tests__/crypto-config.spec.js` | passed，3 files / 11 tests | 无 |
| 2026-07-15 | Excel 模板 | JUnit | `JAVA_HOME=...17... mvn -pl forge-framework/forge-starter-parent/forge-starter-excel -Penable-tests -Dtest=ExcelImportTemplateWriterTest test` | passed，1 test | 首次未指定 Java 17 失败，修正环境后通过 |
| 2026-07-15 | SQL | XML/静态契约 | `xmllint --noout .../SysUserOrgRoleMapper.xml` + `rg` | passed | 未连接真实 MySQL |
| 2026-07-15 | 前端差异 | ESLint | `pnpm exec eslint <本轮相关前端文件>` | passed | 无 |
| 2026-07-15 | 后端集成 | Admin reactor package | `JAVA_HOME=...17... mvn -pl forge-admin-server -am package -DskipTests` | passed，42/42 modules | 测试按命令跳过；存在既有 deprecated/unchecked 警告 |
| 2026-07-15 | 前端集成 | production build | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | passed，8684 modules | 既有组件命名冲突、CSS 注释及 chunk 警告；脚本内部将 heap 设为 40961MB |
| 2026-07-15 | 差异卫生 | 空白检查 | `git diff --check` | passed | 工作区原有大量未提交改动均保留 |
| 2026-07-16 | 加解密关闭启动回归 | Crypto Starter JUnit | `JAVA_HOME=...17... mvn -pl forge-framework/forge-starter-parent/forge-starter-crypto -Penable-tests -Dtest=CryptoEncryptorLifecycleTest test` | passed，3/3 | 未启动真实服务 |
| 2026-07-16 | 主应用装配 | Admin reactor package | `JAVA_HOME=...17... mvn -pl forge-admin-server -am package -DskipTests` | passed，42/42 modules | 测试按命令跳过；存在既有 deprecated/unchecked 警告 |

## 6. 执行证据

- `execution-log.md`：同目录增量追加。
- 关键接口：静态检查 `/crypto/config` 与 `/api/config/manage/crypto` 契约；真实 HTTP 留待用户环境。
- 关键数据库检查：无 Flyway 变更；用户关联管理 SQL 不连接真实 MySQL。
- 服务启动与停止：计划不启动服务。

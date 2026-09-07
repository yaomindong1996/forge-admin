# 执行记录

## 2026-09-07 范围与基线

- 已读取 AGENTS、设计规范、编码规范、自动化测试标准及现有源码。用户已批准优化方案。
- 工作分支：codex/data-scope-usability；原有改动：根 .DS_Store 修改、forge/.DS_Store 删除，保持不动。
- 本地 8580 / 5173 未发现监听服务。先采用隔离的组件预览与模拟 API 验证 UI，避免启动真实后端触发现有迁移。
- 现状：状态只能编辑；配置服务在提交前刷新；运行快照排除禁用项；手动刷新 API 已有但无页面入口及显式管理员检查。

## 2026-09-07 实现与增量验证

### 改动

- 列表直接启停：字典状态、确认影响、仅更新状态、旧状态条件、独立 loading、取消无请求、失败重新查询权威状态。
- 「刷新数据权限」与「刷新列表」分开，保存后更新提示；提交后刷新失败明确说明配置已保存。
- 编辑入口使用具体 DTO，状态不随旧表单覆盖；查询 SQL 移入 XML；运行快照保留禁用记录并一次替换。
- Redis 通知其他副本本地重载、忽略自身消息、不循环广播、重新订阅补载，保留无 Redis 本地模式。
- 技术配置/字段设置折叠、业务文案、紧凑筛选、小屏简化列及全宽抽屉。

### 检查命令与结果

1. `git diff --check`：通过。
2. `xmllint --noout forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/src/main/resources/mapper/datascope/SysDataScopeConfigMapper.xml`：通过。
3. 在 forge-server 使用 `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH`：
   - `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am compile -DskipTests -q`：通过。
   - `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests -Dtest=SysDataScopeConfigServiceImplTest,SysDataScopeConfigControllerTest,DataScopeServiceImplTest,DataScopeCacheSynchronizerTest,DataScopeFailClosedTest,DataScopeInterceptorTest,DataScopeFlowRelatedVisibilityTest -Dsurefire.failIfNoSpecifiedTests=false -q`：实际 20 项 JUnit 测试通过、0 失败。覆盖 DTO/管理员接口、提交/回滚、刷新失败、并发拒绝、显式禁用、租户禁用覆盖默认配置、远端通知及重订阅。DataScopeInterceptorTest 被根 POM 的 enable-tests/testExcludes 既有配置排除，未执行、不计入通过数；本轮未改排除配置，使用 DataScopeFailClosedTest 和 DataScopeFlowRelatedVisibilityTest 覆盖相关拦截行为。
   - `mvn -pl forge-admin-server -am package -DskipTests -q`：最终聚合打包通过，未执行发布构建测试。
4. 在 forge-admin-ui 先 `source /Users/yaomindong/.nvm/nvm.sh && nvm use v20.19.0`：
   - `pnpm --ignore-workspace exec eslint src/views/system/dataScopeConfig.vue src/views/system/components/DataScopeStatusSwitch.vue src/views/system/components/DataScopeRuleEditor.vue src/views/system/components/__tests__/DataScopeStatusSwitch.spec.js src/utils/request.js --fix`：通过。
   - `pnpm --ignore-workspace exec vitest run src/views/system/components/__tests__/DataScopeStatusSwitch.spec.js`：6 项通过，使用真实 Naive Switch 验证取消、双向更新、失败、重复确认及业务失败响应。
   - `pnpm --ignore-workspace build`：最终构建通过，日志在 `verification/build.log`（忽略入库）。
5. `verification/README.md` 所列 Playwright 命令：最终通过，14 次模拟 API 请求、浏览器运行错误 0。验证取消不请求、禁用/启用、列表刷新不刷新权限、刷新错误提示、编辑保留 Mapper 且不提交 enabled、保存清除旧失败提示、小屏更多菜单、全宽抽屉和开关首屏可见。截图见同目录。

### 过程中发现并修正

- 本地 pnpm workspace 缺少 packages 导致原命令失败；使用 --ignore-workspace 复用已安装依赖，未修改工作区配置。
- AGENTS 指定的 `@/utils/request` 原文件不存在；新增薄导出入口，仍复用现有 http 实例及鉴权/加密处理。
- 首轮后端失败来自 Mockito 对已抛异常的方法重新 when 调用，改为 doReturn 后复跑通过。
- 前端首轮用例假设开关是 button，改为真实 role=switch；浏览器发现取消回调的赋值表达式隐式返回 false，阻止弹窗关闭，已改成无返回值并补断言。
- 独立预览初版缺少全局 loading 初始化、按钮文案预期错误，补齐真实 Naive 初始化并使用实际「确定」按钮后通过。
- 浏览器发现小屏固定抽屉及筛选挤压，已适配，并增加宽度与首屏可见断言。

### 警告、边界与清理

- 构建有既有 Vite 配置导入兼容提醒、动态/静态混合导入及插件耗时提醒，未阻断构建。
- JDK CDS、commons-logging 冲突提醒未阻断；测试中的「refresh unavailable」ERROR 和参数校验 WARN 为刻意覆盖失败路径。
- 未连接真实数据库、未启动 Admin/Flow（避免启动自动执行现有 Flyway），未跑真实 Redis 多实例/重连或登录加密 E2E。跨实例行为由 Redisson mock 验证；浏览器使用合成配置和模拟接口，不声称真实生产权限已生效。
- 跨实例通知为异步，界面不承诺全节点确认；共享 Redis 的独立平台环境应设置不同 `forge.datascope.cache-refresh-topic`。
- with_server.py 自动停止本轮各次 Vite，工具未输出子 PID；最终 `lsof -nP -iTCP:5187 -sTCP:LISTEN` 无监听。未停止用户进程，未修改真实数据。

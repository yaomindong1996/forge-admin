# 执行记录

## 2026-10-08

- 原 plugin-foundation 工作区存在未提交改动，本轮仅新增 license 包/测试/Spec，并修改自动配置列表。
- website SSO/COS 独立提交 030111b 完成；许可证实施采用已确认客户＋项目绑定。

## 客户端验证完成

- 新增 LicensePayload/Envelope/Codec、Loader、FeatureGate、Properties 与默认关闭自动配置。
  私钥不进入客户工程，启动验签原始 payload 字节，每次 Gate 查询检查使用期限。
  多节点绑定一致，维护到期不停止永久使用；文件更新需重启，离线撤销无法即时同步。
- 严格 JSON 增加关闭标量类型强制转换和浮点转整数；网站协议同步，去包名逐字核对一致。
- 实际命令（forge-server 下）：Java17 位于 `/private/tmp/lawhub-october-jdk/Contents/Home`，
  Maven `/private/tmp/apache-maven-3.9.11/bin/mvn`，Mockito ByteBuddy agent 1.17.8；
  `mvn -s /private/tmp/forge-website-central-settings.xml -B -ntp
  -pl forge-framework/forge-starter-parent/forge-starter-plugin -am test -Penable-tests`。
  最终 205 项、0 失败/错误/跳过，5 模块 SUCCESS；没有沿用默认 skipTests 假报测试成功。
- 测试均使用 @TempDir 和临时真实 Ed25519 KeyPair；未生成或存放生产密钥，没有共享库变更。
- 新增 11 Java 文件行宽检查通过；git diff --check 通过。自动配置列表仅新增本扩展一行。
- 网站商业插件 125 项，管理端 21 项和门户 29 项及构建通过，详细记录在对应工程的
  `code-copilot/changes/plugin-commercial-platform/execution-log.md`。
- 新增 deployment-guide.md 描述客户配置、可信公钥、Gate 接入、永久/维护边界和部署验收。
  CRUD 技能用于网站实体/DTO/查询边界，部署文档使用 write-page 技能；不改变开源工程品牌。
- 仅本地中文提交本轮 license 文件与 Spec，不暂存已有 plugin-center、脚本、UI 或 .DS_Store 修改。
  未 push、未合并 main、未部署、未签发真实客户授权；部署后联调由用户完成。

## L4 只读授权诊断（2026-10-09）

### 变更与审查

- 在 RuntimeLicenseFeatureGate 的同一启动快照上增加安全诊断读视图；查询与 Gate 共用状态规则。
  过期状态随时钟变化，刷新不重新读文件；保留社区功能和自定义 Gate 装配优先级。
- 新接口 `GET /system/plugin/runtime-license/status` 同时保留真实 `system:plugin:list` 注解及平台
  超级管理员断言；响应 `Cache-Control: no-store`，无效/不匹配文件不返回声明信息，永不返回路径/密钥。
- 插件中心增加运行时授权页签；Naive UI 与真实字典复用，独立 Pinia 内存状态同步隔离账号/权限/Token，
  包括账号切换后又切回的 ABA 场景。生产代码只有实际查询 API，不使用验证页或单测的模拟响应。
- 最终审查发现既有前端权限辅助函数会直接放行管理员，故本只读 Store 单独核对实际权限列表，
  不修改原公共函数；管理员无查询权限不发请求，撤销再恢复权限也不允许旧响应回填。
  后端增加同身份缺少查询权限的真实拦截器断言，始终保持最终鉴权边界。
- V215 仅增加 tenant_id=1 的两个诊断字典及九个选项，实际 SQL 用 NOT EXISTS 防重复；不授权、不改菜单。
- 使用 forge-codegen-crud 核对 VO、鉴权、字典迁移边界，使用 write-page 更新既有本地部署说明；
  未创建云文档、未引入新组件库或生产依赖。未改网站工程，网站分支仍干净。

### 后端执行证据

以下从 `forge-server` 执行，Mockito 显式启用本机已有 ByteBuddy agent，无远端连接：

```bash
export JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
export JAVA_TOOL_OPTIONS=-javaagent:/Users/mini32g/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar
/private/tmp/apache-maven-3.9.11/bin/mvn \
  -s /private/tmp/forge-website-central-settings.xml -o -B -ntp \
  -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests \
  '-Dtest=RuntimeLicense*Test,License*Test,Plugin*Test,CommunityFeatureGateTest,ForgeVersionTest,SysPlugin*Test,RuntimePluginCatalogTest' \
  -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.test.redirectTestOutputToFile=true
/private/tmp/apache-maven-3.9.11/bin/mvn \
  -s /private/tmp/forge-website-central-settings.xml -o -B -ntp \
  -pl forge-framework/forge-starter-parent/forge-starter-plugin -am test -Penable-tests
/private/tmp/apache-maven-3.9.11/bin/mvn \
  -s /private/tmp/forge-website-central-settings.xml -o -B -ntp \
  -pl forge-admin-server -am package -DskipTests
```

- 目标回归最终 179 starter + 107 system = 286 项，0 失败/错误/跳过，27 模块 SUCCESS，18.337 秒。
  新增真实签名诊断 5、Sa 鉴权 3、服务查询 2、字典幂等 SQL 1 项；测试使用临时密钥及进程内会话。
- starter 全测试增强验证 210 项全部通过，5 模块 SUCCESS，3.336 秒；含原 Web guard 和装配优先级。
  与目标回归有重叠，不重复计算；两次覆盖的独立后端用例为 210 starter + 107 system = 317 项。
- Admin 聚合 package `BUILD SUCCESS`，27.777 秒；打包遵循默认跳过测试，测试结论来自显式 enable-tests。
- V215 以 H2 MySQL 模式直接执行实际脚本两次，枚举码/字典完全匹配，资源数不变。
  占位符扫描无输出；这是本地 SQL 回归，不是共享 MySQL 或 Flyway 启动验收。
- 编译仍出现原有 LicenseCodec/FlowClient/数据权限的 deprecated/unchecked 提示及验证码 Lombok Builder
  默认值警告，均非本轮新增阻断；未为消除警告扩大修改范围。
- 日志：`/private/tmp/forge-runtime-license-diagnostics-java.log`、
  `/private/tmp/forge-runtime-license-diagnostics-starter-all.log`、
  `/private/tmp/forge-runtime-license-diagnostics-admin-package.log`。

### 前端执行证据

使用实际 Node v20.19.0，直接调用已有依赖脚本以避开已知 pnpm workspace 配置问题，未安装新依赖。
以下从 `forge-admin-ui` 执行：

```bash
/Users/mini32g/.nvm/versions/node/v20.19.0/bin/node \
  node_modules/vitest/vitest.mjs run src/views/system/plugin/__tests__
/Users/mini32g/.nvm/versions/node/v20.19.0/bin/node node_modules/eslint/bin/eslint.js \
  src/views/system/plugin.vue src/api/system/runtimeLicense.js \
  src/stores/plugin/runtimeLicenseState.js src/stores/plugin/runtimeLicenseStore.js \
  src/views/system/plugin/runtimeLicenseUtils.js \
  src/views/system/plugin/components/RuntimeLicenseStatus.vue \
  src/views/system/plugin/components/RuntimeLicenseConfiguration.vue \
  src/views/system/plugin/components/RuntimeLicenseScope.vue \
  src/views/system/plugin/__tests__/runtimeLicenseApi.spec.js \
  src/views/system/plugin/__tests__/runtimeLicenseState.spec.js \
  src/views/system/plugin/__tests__/runtimeLicenseView.spec.js \
  --rule 'max-lines-per-function: [error, 80]' \
  --rule 'max-depth: [error, 3]' --rule 'max-params: [error, 5]'
NODE_OPTIONS=--max-old-space-size=8192 /Users/mini32g/.nvm/versions/node/v20.19.0/bin/node \
  node_modules/vite/bin/vite.js build
```

- 最终 10 文件、42 项通过；目标 lint/规模规则退出 0，新增 Java/JS/Vue 行宽检查无超限。
- 初次单测使用 Vitest2 未支持的 toHaveBeenCalledExactlyOnceWith 导致一项失败；改为次数与参数的两条
  等价断言后全过，没有删除用例或弱化断言。真实 Naive 表格测试增加 NConfigProvider，最终无 Provider 警告。
- 新增权限撤销测试后 describe 回调超过 80 行，被规模门禁阻止；按权限与展示两组拆分测试，保留全部断言。
- 生产 build 退出 0，`built in 31.44s`。末尾 Rolldown 插件耗时分析属于构建信息，不是失败。
- 日志：`/private/tmp/forge-runtime-license-diagnostics-ui-tests.log`、
  `/private/tmp/forge-runtime-license-diagnostics-ui-shape.log`、
  `/private/tmp/forge-runtime-license-diagnostics-ui-build.log`。

### 浏览器与环境边界

- 临时目录 `/private/tmp/forge-license-ui.3SXcpH` 的 Vite 页挂载正式组件、真实 Naive UI 与空的登录状态，
  明确标记“未登录，无授权数据，不替代部署验收”；未设置 Token/管理员权限/模拟许可证响应。
- 仅 localhost:3219 预览；代理限定到本机未启动的 3218，不访问用户测试后端。浏览器点击检查 320px
  明暗主题，无页面横向溢出；只展示无权限提示，没有发起授权状态查询。完整表格/范围由真实组件单测覆盖。
- 未登录预览产生预期的本机字典 502/无加密公钥提示，不将其当成实际后端验收成功。
  初次服务遇到已知 EMFILE，改用轮询监听；直接临时 HTML 的裸模块导入通过 Vite transformIndexHtml 修正。
- 本轮服务执行 session 89757、75713 均已 Ctrl-C 停止，验证页签 11 已关闭、视口覆盖已重置；
  `lsof -nP -iTCP:3219 -sTCP:LISTEN` 无监听者。没有停止用户其他服务或改动其浏览器页签。
- edition guard 与 git diff --check 通过。只暂存本轮授权诊断及其文档/踩坑记录，保留既有 plugin-center、
  制品注册、脚本和 .DS_Store 改动；不 push、不合并 main、不执行共享数据库迁移或签发授权。
- 登录后普通加密调用、真实 MySQL Flyway、生产 SSO/COS/客户许可证与多节点 E2E 由用户自行部署验收，
  本轮没有将未执行项标为通过。

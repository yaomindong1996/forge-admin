# 插件中心执行记录

## 2026-10-07：P1 开始

- 用户批准可视化插件架构后开始；基线 ae6dc7ab，分支 codex/plugin-foundation。
- 保留用户 .DS_Store；不切换 main、不 push、不创建 Pro 工程。
- 读取 AGENTS/Skill/plugins 参考、DESIGN、测试规范和 foundation 已有证据。
- 发现 Registry 仅有外部描述，内置插件无登记；P1 补 classpath 声明，不读取部署机源码。
- 本轮先交付当前后端实例清单；纯 UI 构建清单和安装任务分 P2/P3，明确不伪造状态。

## 2026-10-07：P1 实现与增量验证

### 交付与自审

- 内置业务 JAR 15 份稳定 ID 声明；Maven 只过滤该描述，不过滤其它业务 JSON/SQL。
- RuntimePluginCatalog 合并内置与 Registry，严格解析、大小上限、重复 ID 拒绝、不可变有序快照。
- system 只读分页/详情 DTO/VO API：RBAC 注解与真实 SessionHelper.assertAdmin 双重限制。
- 平台管理独立入口、4 类系统字典、列表/详情/竞态保护、重试及主题/窄屏适配。
- Stage 1（Spec）：P1 覆盖；独立实例/纯 UI/源码与部署边界均说明；P2/P3 未冒充已交付。
- Stage 2（质量）：无写接口/外部进程/许可证泄露，无服务循环注入；新增类/SFC/方法规模达标。
  新增 Java/JS/Vue 单行不超过 120；菜单不覆盖应用总览/打印中心，不授权普通角色。
- 发现现有 v-hasPermi 更新钩子 this 未绑定造成页面异常；新页面用 userStore 计算权限，
  不扩大范围修改全局指令，后端鉴权保持权威。故障沉淀到 frontend 踩坑。
- 生成项目遵循 forge-project-init Skill：改包/坐标后重新生成 full，保持插件 ID 不变。

### 工具与证据位置

- Node v20.19.0；JDK17 `/private/tmp/lawhub-october-jdk/Contents/Home`；
  Maven `/private/tmp/apache-maven-3.9.11/bin/mvn`，离线 `-o`，不新增第三方依赖。
- 本轮独占夹具/日志目录：`/private/tmp/forge-plugin-center.HXbPfO`，以下简称 QA_DIR。
- Maven 测试预加载本机 Byte Buddy agent（JAVA_TOOL_OPTIONS=-javaagent:.../1.17.8/...jar），
  避免 JDK 沙箱自附加失败；这是测试环境设置，不写入项目运行配置。

### 后端与真实构建声明

在 forge-server 执行：

```sh
mvn -o -q -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test \
  -Penable-tests \
  '-Dtest=RuntimePluginCatalogTest,Plugin*Test,CommunityFeatureGateTest,ForgeVersionTest,SysPlugin*Test,*FeatureGateTest' \
  -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.test.redirectTestOutputToFile=true
mvn -o -q -pl forge-admin-server -am package -DskipTests
mvn -o -q -pl forge-framework/forge-plugin-parent/forge-plugin-flow -am process-resources -DskipTests
```

- `java-regression-final.log`：退出 0。starter-plugin 169、system 本轮选择的 38，共 207，通过无跳过。
  system 既有其它 surefire 文件不计入本轮选择范围；新增测试共 24。
- `admin-package.log`：退出 0，Admin 聚合构建通过；`flow-resources.log`：资源处理退出 0。
- QA_DIR/JarCheck.java 用 Java ZipFile 读取实际宿主及改名宿主的嵌套业务插件 JAR。
  `jar-check.log`：各 14 个、版本 1.2.0、模块坐标正确、无未过滤占位符/重复 ID；
  plugin-flow 不在 Admin 依赖中，没有将源码的 15 份声明假装成此实例已加载 15 个。

### 前端与浏览器

在 forge-admin-ui 使用已有依赖执行（node 指 Node20）：

```sh
node node_modules/eslint/bin/eslint.js src/api/system/plugin.js \
  src/views/system/plugin.vue src/views/system/plugin
node node_modules/vitest/vitest.mjs run src/views/system/plugin/__tests__
node node_modules/vite/bin/vite.js build
```

- ESLint 无错误；新增 Vitest 8/8：API 编码、筛选/分页、列表/详情竞态、关闭/销毁及失败重试。
- `ui-final-build.log`：退出 0，40.03 秒；现有大包/分析器提示为非阻断警告。
- 自建明确标注模拟接口的预览，127.0.0.1:43127；使用系统 Chrome 的隔离 Playwright 会话。
  `browser-check.mjs` 覆盖列表/搜索/详情/功能授权/重置/分页/刷新错误/重试/明暗/320px，
  `browser-final.log`：无 pageerror；320px 抽屉 x=0,width=320,height=720，可见关闭按钮。
- 人工检查 list-light、detail-narrow 截图；窄屏列表二次独立加载/截图复核，DOM 仅 1 个
  main、1 个页面头、1 个插件中心、15 数据行，无重复页面，截图 list-narrow-inspected.png。
- 浏览器使用模拟响应，不声称已覆盖真实租户、正常登录、加密/菜单缓存或部署数据。

### Node 基线、模板/生成工程、迁移

仓库根目录执行：

```sh
node --test scripts/forge-shared scripts/forge-plugin scripts/guards scripts/forge-create \
  code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs \
  code-copilot/changes/plugin-center/contracts.test.mjs \
  forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
node scripts/forge-create/create-project.mjs <QA_DIR>/delivery-generated \
  --preset full --project-name plugin-center-check --java-name PluginCenterCheck \
  --base-package com.acme.center --group-id com.acme.maven \
  --artifact-prefix center-host --module-artifact-prefix kernel \
  --display-name 插件中心验证 --database-name plugin_center_check
node --test <生成工程>/center-host-server/scripts/db/init-db.test.mjs \
  <生成工程>/center-host-server/scripts/db/clean-db.test.mjs
```

- `node-full.tap`：348/348，失败/跳过均 0，27.25 秒，包含模板 DB 桩和文档/插件中心契约。
- 先后生成独占 generated/final-generated/delivery-generated；不是原工程的局部目录拷贝。
  最后一次 `delivery-db.tap`：30/30，20.28 秒。mysql/Maven 为桩，未创建实际数据库。
- final-generated/center-host-server 中先安装改名 BOM：
  `mvn -o -q -f kernel-framework/kernel-dependencies/pom.xml install -DskipTests`，
  再 `mvn -o -q -pl kernel-admin-server -am package -DskipTests`，退出 0。
  BOM 写本机 Maven 缓存经批准，不更新任何远程制品仓库。
- 改名工程 `-Penable-tests` 执行 RuntimePluginCatalogTest,PluginAutoConfigurationTest,SysPlugin*Test，
  33/33，退出 0；生成的生产 Java 与当前源码经包名替换对比 6/6 相同。
  独立 Flow 的 process-resources 退出 0、版本/坐标正确；未做独立 Flow 全量 package。
- Java H2 RunScript 在内存库 `MODE=MySQL;NON_KEYWORDS=VALUE` 执行 migration-fixture.sql：
  实际 V1.0.210 连续运行两次，5 资源/4 字典类型/8 数据条目，无重复；客户菜单冲突不覆盖。
  `migration-h2.log` 退出 0。H2 合成表验证不等价于真实 MySQL/Flyway。
- `node scripts/guards/check-edition.mjs` 和 `git diff --check` 均通过；提交前再次检查工作区与索引。

### 遇到的环境问题及解决

- pnpm v11 exec 试图在无 TTY 环境重新安装，未继续安装；改用现有 node_modules 工具。
- POM 的显式 argLine 覆盖 CLI -DargLine，初次 Mockito 自附加失败；改 JAVA_TOOL_OPTIONS 后重跑通过。
- 初次生成工程聚合路径误用外部 artifact-prefix；核对实际 kernel-admin-server 后成功。
- 生成 BOM 的本机缓存写入、临时 Vite 端口绑定受沙箱限制，均通过授权后完成。
- Playwright 下载缓存不存在，复用本机 Chrome；不下载浏览器，不读用户标签页。
- UI 合成字典缓存与实际不同；页面统一显式 useDict→DictTag options，不硬编码状态文案。
- 收尾误指定不存在的 docs-contract.test.mjs，命令未运行测试；核对文件后重跑实际
  scripts/forge-plugin/documentation.test.mjs（6/6）与本轮 contracts.test.mjs（3/3）通过。

### 待执行及清理边界

- 未连接/修改 158 MySQL/Redis；未启动真实 Admin/Flow；未执行 Flyway、业务写入或生产部署。
- 真实目标环境正常登录/RBAC/加密链路及菜单加载待用户部署验收，禁止以 MockMvc 替代。
- 未做生成工程 UI 全量构建；本模板 UI 已构建，生成前后本轮前端源码无品牌坐标替换差异。
- P2 ZIP 上传/预检/任务记录/UI manifest、P3 builder/部署/运行核验尚未开发；Pro 工程未创建。
- 只停止本轮端口 43127 的模拟 Vite，隔离浏览器每次 finally 关闭；不停止用户已有服务。
  QA_DIR 日志/截图/生成工程保留供复查，无真实 DB 需删除，无后台真实服务遗留。
- 临时 Vite 会话 92021 Ctrl-C 结束（退出 130）；lsof 复核 43127 无监听。
- 本地中文提交在 codex/plugin-foundation；不 push、不合并 main，用户 .DS_Store 保留未提交。

# 插件中心执行记录

## 2026-10-07：P2 开始

- 用户请求继续下一阶段，基线 203068d7，保持 codex/plugin-foundation 和用户 .DS_Store。
- 重读 project-init Skill/plugins 参考、DESIGN、测试规范及已有 P1 证据，追加 P2 执行契约。
- P2 仅受控上传/预检/确认/审计及 UI 构建声明；确认排队不执行安装，不读生产源码路径。
- ZIP 入私有有界 BLOB，平台/RBAC/租户上下文隔离；新增迁移待人工上线，不连接共享数据库。

## 2026-10-07：P2 交付与增量自审

### 范围与实现

- 有界 ZIP central/local、路径、链接、大小、压缩流、CRC、NFC 重名校验；严格 JSON、
  根/runtime 描述一致、POM 单模块/外部实体/64 层限制，私有包不解压落盘、不执行。
- 任务审计/私有 BLOB、显式 DTO/VO、Mapper XML 租户过滤、上传 requestId 幂等，
  确认/取消 SHA-256+revision+CAS，运行快照变化拒绝确认；独立同插件活动任务占用键。
- 平台超级管理员和独立 RBAC；上传/确认/取消 OperationLog 不保存请求/响应内容。
  只到待构建，无 shell/Node/部署接口；前端不显示假安装成功，不实现 Pro/许可证。
- V1.0.211 新表/10 权限资源/2 字典类型/6 字典项；不改 V1.0.210、不赋普通角色权限。
  不覆盖客户菜单/权限/接口，当前唯一键要求 snapshot 独立权限编码。
- 上传工作台、确认风险/取消/查询及有界预览；真实 UI 构建清单核验所有权/开发链接，
  与完整后端快照比较。源码登记、定制、路径/依赖及迁移实际影响明确留到 P3。
- Stage 1 Spec 合规：P2 覆盖，P3 仍未接入；Stage 2 质量：新类/SFC <1000、方法 ≤80、
  新增行 ≤120，字典/枚举/构造器注入/事务/幂等/权限/租户/逻辑删除与日志隐私自检。
  vite.config.js 原有 137 字符环境变量解构行未增长，不顺手修改历史代码。
- forge-project-init Skill 影响：复用源码 CLI/格式，保持协议坐标和稳定 ID，
  模板清理任务包并对新生成 full 工程执行 DB 桩、原始坐标单测和聚合构建。

### 验证环境与命令

- 独占目录 `/private/tmp/forge-plugin-center-p2.03Fo6H`，以下简称 QA_DIR。
  沿用 Node20/JDK17/Maven3.9.11 离线工具和已有依赖；H2 仅 test-scope（BOM 已管理）。
- 模板 forge-server：`mvn -o -q -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test
  -Penable-tests '-Dtest=SourcePluginPackageReaderTest,Plugin*Test,RuntimePluginCatalogTest,
  CommunityFeatureGateTest,ForgeVersionTest,SysPlugin*Test,*FeatureGateTest'
  -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.test.redirectTestOutputToFile=true`。
  实际 -Dtest 参数为无换行/空格的上述列表，预加载同 P1 的 Byte Buddy agent。
  `java-final.log` 退出 0，starter-plugin 188/system 48，共 236，失败/错误/跳过均 0。
- 模板 `mvn -o -q -pl forge-admin-server -am package -DskipTests`：
  `admin-final-package.log` 退出 0；不是全仓所有业务测试均已通过。
- UI 现有依赖直接运行 ESLint、`vitest run src/views/system/plugin/__tests__`、`vite build`：
  `eslint-final.log` 退出 0、14/14 通过、`ui-final-build.log` 退出 0（38.60 秒），
  现有大包/构建分析提示非阻断，不安装/替换 node_modules。
- Node 完整矩阵沿用 P1 命令并追加 workbench-contract.test.mjs：
  `node-final.tap` 353/353，无失败/跳过（34.35 秒）；文档/中心契约独立 13/13。
- `SysPluginTaskMapperTest` 执行真实 MyBatis/XML/表结构，验证私有包内部读取、
  公共查询不取 ZIP、租户隔离、活动唯一键、CAS、取消释放和逻辑删除。
  `SysPluginMigrationTest` 在 H2 MySQL 模式执行实际 210/211、211 两次，
  15 总资源/14 平台限制资源、6 字典类型/14 条目，客户菜单/权限冲突不覆盖。
  QA_DIR/migration-fixture.sql 的独立 RunScript 也退出 0，不等价于 MySQL/Flyway。

### 生成工程与浏览器

- 新 full 工程生成四次（full/delivery/final/release-generated），最后以 release-generated 验收；
  每次从当前模板生成，不是只拷贝变更目录。参数沿用 P1：plugin-center-check、
  PluginCenterCheck、com.acme.center、com.acme.maven、center-host/kernel、plugin_center_check。
- release-db.tap：新 full 工程 init/clean DB 桩 31/31（48.25 秒），未创建真实数据库。
- release-java.log：`-pl kernel-framework/kernel-plugin-parent/kernel-plugin-system -am test`
  `-Penable-tests -Dtest=SourcePluginPackageReaderTest,PluginAutoConfigurationTest,SysPlugin*Test`，
  56/56，退出 0；源包测试仍使用原始 Forge groupId，不让夹具随宿主改名自洽。
- 在独占生成工程用现有 CLI 安装标准 plugins-samples/forge-plugin-hello，
  release-install.log 退出 0；实际生成 UI helper 的 release-manifest.log 输出 hello 1.0.0、
  core 1.2.0，无源码路径；随后 release-package.log Admin 聚合 package 退出 0。
  未做生成 UI 全量构建，模板 UI 已完整构建；不是执行器接入或目标部署。
- 明确标注模拟接口的 Vite 127.0.0.1:43128，隔离系统 Chrome，无用户会话读取。
  browser-final.log：上传/风险勾选前禁止确认/确认待构建/取消/阻断无确认按钮/
  查询/失败重试/构建版本不同/320px 深色；无 pageerror，页面 scrollWidth=320，
  抽屉 x=0,width=320,height=720，关闭按钮 x=240,y=670,width=56,height=34 完整可见。
  人工检查 preview-light、preview-narrow-dark、workbench-narrow-dark 截图；
  使用模拟响应，不替代真实用户登录、加密/multipart 或数据库验收。

### 发现与修复

- 真 Mapper 测试发现 byte[] 返回被 MyBatis 当作多行数组，改为私有实体包装列，
  不降低公开 API 隐私；隔离 Factory 的删除测试显式传入审计字段，避免未注册填充器产生 NULL。
- strict POM 原始 groupId 被生成器改名，改分段协议常量，生成后的测试仍用原始包。
- sys_resource 真实唯一键不允许 page/snapshot 复用编码，snapshot 改独立权限；
  冲突保护同时校验 PC 权限及 API，新增真实唯一键迁移回归。
- 初次测试夹具 Mockito 未完成 stubbing、唯一键错误文案大小写、UI mock 缺字典导出，
  分别修正夹具；测试断言/门禁未降级，浏览器窄屏等待动画稳定再检查位置。
- 补日志时误用不存在的 OperationType.INSERT，编译发现后查枚举改为 ADD，完整回归重跑通过。
- 初次生成工程对 Admin 全链开测试，触发存量 AiProviderAdapterRegistryTest 缺
  createEmbeddingModel 实现的编译问题（generated-java.log）。本轮未改无关 AI 测试；
  改用本轮 system/starter 的定向 Reactor 测试通过，Admin 生产聚合 package 通过。
- 构建 helper 位于既有被忽略但包含源码的 build 目录，提交显式纳入此新文件，避免遗漏。
- 两个真实故障已沉淀 backend 踩坑及索引；未改用户 .DS_Store。

### 待执行和清理

- 未连接或修改 158 MySQL/Redis、启动真实 Admin/Flow、执行 Flyway/生产部署；
  V1.0.211 和真实登录/RBAC/加密/multipart 验收仍待目标环境执行。
- P3 builder/源工作区冲突和定制检查/构建/受控部署/运行核验未开发；Pro 工程未创建。
- 隔离 Chrome 每次 finally 关闭；只停止本轮 43128 模拟 Vite，日志/截图/生成工程保留。
- Vite 会话 91764 Ctrl-C 结束（退出 130），lsof 复核 43128 无监听；未停止用户已有前端。
- 收尾文档/中心契约重跑 13/13、edition 门禁及 diff --check 通过，索引与工作区仅本轮变更。
- 本地中文提交 codex/plugin-foundation，不 push、不合并 main，.DS_Store 不提交。

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

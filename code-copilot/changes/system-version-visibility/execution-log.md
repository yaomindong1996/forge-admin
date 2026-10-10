# 执行记录

## 2026-10-10：范围确认

- 当前 main，基线 eee9906b；只有用户 `.DS_Store` 修改，保留。
- 已读 AGENTS、H5 目录规则、PC DESIGN 与自动化测试标准，复用现有版本/插件构建信息。
- 采用 frontend-design 的精简界面组织，具体视觉以 DESIGN.md 为准；不增加装饰大卡片。
- 后端当前 ForgeVersion 1.2.0，前端包版本不一致；远端标签到 v1.1.3，本轮不创建正式发布标签。
- 无新依赖、无数据库写入、无部署/提交授权。

## 2026-10-10：实现与增量验证

### 实现

- 根 revision 保持 1.2.0，Admin/H5/Report package 同步；Vite 共用校验、构建常量及 version.json。
- Boot Maven build-info 由父级 pluginManagement 继承，四个启动模块已实际打包核对。
- `GET /system/version`：必须登录，免插件管理权限；no-store，仅返回白名单信息。
- PC 头像菜单 / CompactLayoutTools 共用 Pinia About Modal；H5 我的使用 Wot Popup/Collapse。
- 后端未知不兜底为前端版本；异步关闭防迟到覆盖；版本与源码提交不一致时提示核对。
- 更新日志作为精确版本的前端随附说明；新增 guide/release-version.md 并链接 README。

### 自动化结果

以下 Node 命令均先执行 `source /Users/mini32g/.nvm/nvm.sh && nvm use v20.19.0`。
本机 pnpm 11 执行前会尝试重装依赖并触发 `ERR_PNPM_ABORTED_REMOVE_MODULES_DIR_NO_TTY`；
未允许其清理 node_modules，改用项目已安装的 CLI，未安装或升级依赖。

- 根目录：`node --test scripts/forge-shared/release-info.test.mjs`，6/6 通过。
  覆盖改名工程、缺少 Git/日志、版本漂移拒绝构建、CLI 同步、产物字段与展示规则。
- 根目录：`node scripts/forge-shared/version-cli.mjs --check`，发行版本 1.2.0 检查通过。
- 根目录：`node scripts/guards/check-edition.mjs`，开源边界检查通过。
- PC 目录：`node node_modules/vitest/vitest.mjs run src/components/common/__tests__/SystemAboutModal.spec.js
  src/stores/system/__tests__/versionStore.spec.js src/layouts/components/__tests__/account-actions.spec.js
  src/layouts/components/__tests__/compact-layout-tools.spec.js`，4 文件、20/20 通过。
  初次 Collapse 测试点击外层 header 无效，按实际 Naive header-main 修正测试点击目标后通过。
- H5 目录：`node ../forge-admin-ui/node_modules/vitest/vitest.mjs run
  --config ../forge-admin-ui/vitest.config.js src/store/modules/__tests__/version.spec.js`，2/2 通过。
- PC 变更源文件及新增测试使用已安装 ESLint 检查/格式化后通过；`git diff --check` 通过。

后端在 `forge-server` 执行：

```bash
JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home \
/private/tmp/apache-maven-3.9.11/bin/mvn -o -q -Pdev,enable-tests \
  -pl forge-framework/forge-plugin-parent/forge-plugin-system \
  -Dtest=SystemVersionServiceTest,SystemVersionControllerTest test -Dstyle.color=never
```

- 6/6 通过。最初 Mockito inline 受本机 attach 限制无法初始化；本轮测试改为简单接口实现/记录式替身，
  保留真实 Sa-Token 和 MVC 边界，无需放宽 JVM 或项目安全配置。
- 以上合计 34 个自动化用例通过，不含额外静态检查。

### 构建与产物核对

```bash
# forge-server
JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home \
/private/tmp/apache-maven-3.9.11/bin/mvn -o -q \
  -pl forge-admin-server,forge-app-server,forge-flow/forge-flow-server,forge-report-server \
  -am package -DskipTests -Dstyle.color=never

# forge-admin-ui
NODE_OPTIONS=--max-old-space-size=8192 node node_modules/vite/bin/vite.js build

# forge-h5-ui
NODE_OPTIONS=--max-old-space-size=8192 node node_modules/@dcloudio/vite-plugin-uni/bin/uni.js build
NODE_OPTIONS=--max-old-space-size=8192 node node_modules/@dcloudio/vite-plugin-uni/bin/uni.js build -p mp-weixin
```

- 四个后端启动模块聚合 package 成功；分别读取 JAR 根 `META-INF/build-info.properties`，
  均为 build.version=1.2.0，artifact 各自正确，time 有值，未传 commit 时为 unknown（接口过滤为 null）。
  Spring Boot 重打包将该文件保留在 JAR 根 META-INF，不在 BOOT-INF/classes/META-INF。
- PC 生产构建成功；H5 与 mp-weixin 构建成功。
- 非阻断既有警告：Commons Logging、Vite 混合动态/静态导入及小程序循环 chunk 提示。

### 页面验证与边界

- 隔离测试壳位于 `/private/tmp/forge-system-version.4m37j1`，直接引用真实源码组件。
  4317 为 Naive UI，4318 为 uni-app/Wot；测试响应仅在临时壳内替换，不进入产品代码。
- PC：实际打开弹窗，确认当前版本/前后端信息/字典标签，展开随附说明；验证接口不可用与版本不一致提示；
  深色主题可读。320px 测得文档 scrollWidth=clientWidth=320，弹窗宽 288，无横向溢出。
- H5：实际打开 Wot 弹层；390×844 下文档宽 390，底部按钮 y=788..832，位于视口内，
  长提交号正常换行。追加 320px 展开交互时机器锁屏、浏览器连接中断，未完成该项及独立截图文件保存。
  临时 viewport 复位调用也未能完成，不能宣称已复位；待解锁后可继续验证。
- 已停止本轮测试进程：旧 H5 99358、最终 H5 4549、PC 97880；未停止任何用户服务。
- Report UI 未安装 node_modules，未完整构建；只做统一版本检查与共享 Vite hook 单测。
- 未启动真实业务后端以避免自动 Flyway/启动任务写入数据库；接口以真实 Sa 会话和 MockMvc 验证，
  不宣称真实环境联调或集群滚动发布通过。未提交、推送、部署；保留用户 `.DS_Store`。

### 最终产物复核

- 加入 `version.json` 输出后，再次执行 PC、H5、mp-weixin 生产构建，全部成功。
- 读取 PC `dist/version.json` 与 H5 `dist/build/h5/version.json`，断言版本均为 1.2.0，
  client 分别为 admin-ui / h5-ui，builtAt 为有效时间；字段仅 version/client/commit/builtAt/notes。
- 最终发行版本检查及 `git diff --check` 通过；当前分支仍为 main，未提交/推送。

## 2026-10-10：AI 发版规范持久化（T6）

- 发版判定：日常文档维护；发行版本保持 1.2.0，不新增正式发行或改写既有 CHANGELOG。
- 根 AGENTS §5.19 增加强制入口与短规则；已有版本指南补齐版本选择、发布步骤、验收及交付记录格式。
- OpenCode apply/review 增加版本检查入口；apply 移除自动提交要求，改为按用户授权执行。
- preferences 仅增加用户要求和指引路径，避免复制多套规则。
- `git diff --check` 通过；Node 静态检查确认四个 AI 入口均引用版本指南，
  指南的仓库相对链接可解析，根 package 中 version:sync/version:check 与真实脚本一致。
- `node scripts/forge-shared/version-cli.mjs --check` 通过，输出“发行版本 1.2.0，版本检查完成”。
- 本轮只改文档，不重跑上轮业务测试、构建或浏览器验证；不启动服务、不访问数据库。
- 未执行版本同步、提交、推送、打标签、制品发布或部署。当前 main 保留原有实现改动和用户 `.DS_Store`。

## 2026-10-10：Admin / UI 提交与部署准备（T7）

- 用户授权提交、推送 main 及生产 Admin / UI 部署，版本保持 1.2.0；不创建发布标签或更新其它端。
- `git fetch origin main` 成功，HEAD 与 origin/main 同为 eee9906b，无远端新增提交。
- SSH 使用用户此前确认的 ED25519 指纹并严格校验，不关闭主机身份验证。
- 生产 Admin 当前 active/running；实际端口 8580，服务 spring_forge-admin；Nginx /forge 对应
  `/www/wwwroot/html/dist`，/forge-api 对应 Admin；其它站点路径独立，本轮不动。
- 只读数据库核对：forge_schema_history 共 217 条成功记录，当前 JAR 校验无差异，pending=0。
- 版本及 Flyway 配置 Node 契约 11/11、版本 1.2.0 检查、开源边界与 git diff --check 通过。
- 提交排除用户 `.DS_Store`；Admin / UI 将使用提交后重新构建的产物，不直接发布上轮试构建。

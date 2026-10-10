# 执行记录

## 2026-10-10 开始

- 当前 main，版本 1.2.0，本轮日常开发、不升版、不提交/推送/部署。
- 初始工作区只有 .DS_Store 修改，原样保留。
- 已读取 DESIGN、frontend-design、imagegen 与其提示词参考；按企业控制台规范约束视觉。
- 原插件小图由 4×4 位图 atlas 裁切；一级图标来自不同图标库/图片。公共图片仍由通用选择器引用。

## 2026-10-10 实现与收尾

- 内置 image_gen 生成并采用低饱和模块组合插画，已保存到项目资源目录；完整提示词见 assets.md。
- 增加 ForgeSymbol 白名单矢量符号，统一网格、描边和 currentColor，支持现有 IconRenderer 的 forge: 前缀。
- 两个菜单数据入口仅适配一级显示图标；保留路由、权限、排序、子菜单以及原 API 数据。
  对照当前内置菜单补充 /platform、/data-report、/system/collaboration 等真实路径别名。
- 插件列表拆出 PluginCard，展示真实名称、来源、版本及既有资料，保留详情/安装入口。
  未知或外部插件不借用系统插件介绍。卡片不再裁切旧 sprite，使用独立语义图标。
- 插件中心与发现页加入小插画；关于系统增加主题色版本区、分组图标、双栏信息卡与窄屏堆叠。
  保留版本差异/未知提示、复制、刷新、纯文本版本说明。
- CHANGELOG 仅追加 Unreleased，发行版本仍为 1.2.0。

### 自动化验证

使用已安装 Node 20.19.0 和项目本地 CLI，未重装依赖。命令在 forge-admin-ui 执行：

```sh
node node_modules/vitest/vitest.mjs run \
  src/utils/__tests__/navigation-icons.spec.js \
  src/views/system/plugin/__tests__/pluginClientCenter.spec.js \
  src/views/system/plugin/__tests__/pluginClientPresentation.spec.js \
  src/components/common/__tests__/SystemAboutModal.spec.js \
  src/stores/system/__tests__/versionStore.spec.js \
  src/layouts/__tests__/sidebar-menu-context.spec.js \
  src/layouts/business-workbench/__tests__/menu-model.spec.js \
  src/layouts/components/__tests__/responsive-menu-toggle.spec.js
```

- 8 个测试文件、44 个测试全部通过；最后补充真实菜单路径断言后，navigation-icons 的 5 个测试再次通过。
- 本轮 17 个 JS/Vue/测试文件的定向 ESLint 通过；最后菜单路径补充后再次 lint 通过。
- 最终 `NODE_OPTIONS=--max-old-space-size=8192 node node_modules/vite/bin/vite.js build --mode production --logLevel warn`
  返回 0，10130 modules、954 chunks，build complete。
- 构建现有 PLUGIN_TIMINGS 性能提示不影响成功；测试现有 transfer 重复注册提示不影响断言。
- `git diff --check` 通过。

### 浏览器验收

- 桌面浏览器控制首次读取超时，改用独立、临时浏览器环境加载真实 Vue/Naive UI 组件。
  仅访问 127.0.0.1:4321，不读取用户浏览器登录态、不连接业务 API。
- 预览顶部明确标记“数据为 UI 夹具”，插件/字典/版本响应均为夹具；这不是生产数据验收。
- 验证通过：搜索流程、空结果、重置、安装说明、详情返回保留筛选、折叠菜单 SVG、
  图标继承侧栏颜色、关于系统社区版字典/刷新/说明/关闭。复制及版本异常分支由组件与 Store 测试覆盖。
- 明暗主题 1440px，以及 430px / 360px 窄屏检查通过；无文档或卡片列表横向溢出；最终 pageerror 为空。
- 已人工查看明暗插件卡片、关于系统与窄屏截图，最终图标加载正常。临时预览的图标解析配置已修正，
  该问题仅属于隔离测试环境，不改动正式 UnoCSS 配置。
- 完整各布局未逐一使用真实账号人工验收；本轮复用共享菜单适配并通过相关布局回归测试。
- 测试目录：`/private/tmp/forge-plugin-visual.kw2f9t`。证据：plugin-light.png、plugin-dark.png、
  plugin-narrow.png、plugin-360.png、about-light.png、about-dark.png、about-narrow.png；
  最终构建日志 build-final.log。浏览器脚本 check.mjs、interactions.mjs 均返回 0。
- 临时 4321 预览服务已主动停止，浏览器脚本退出时关闭实例，不影响用户已有服务。

### 工作区边界

- 没有启动后端、修改数据库、连接生产、提交、推送或部署。
- 保留 .DS_Store 以及期间出现的 system-version-visibility、preferences、部署文档改动，
  它们不属于本轮视觉实现，未纳入本轮编辑。

## 2026-10-10：用户授权提交与 Admin UI 部署

- 用户追加“提交并部署”，本轮交付仅 PC 前端；后端没有源码变更，不重启 Admin 或其它服务。
- 当前版本保持 1.2.0，CHANGELOG 仍归属 Unreleased；不创建新版本标签或发布不可变制品。
- `git fetch origin main` 成功，HEAD 与 origin/main 无差异；不强推、不覆盖其它提交。
- 只暂存本轮插件视觉实现及其文档，保留其它任务的部署手册、版本文档、preferences 和 .DS_Store。
- 已读取版本维护及生产部署手册；服务器 ED25519 指纹与用户此前确认值一致，SSH 严格验证。
- 版本检查、开源边界检查和 diff 空白检查通过；复用上一阶段增量测试与视觉证据。
- 构建、备份和生产验收结果待追加；不将提交完成视为部署完成。

### 2026-10-10 22:10 起：提交、构建与部署结果

- 功能提交 `561ba23f5e8735dd2a5fa87a5ff5af069f3f64ef` 已普通快进推送 origin/main，24 个文件。
  其它任务文档与 .DS_Store 均未暂存、修改或提交。
- 提交前重新运行既有 8 文件 / 44 个测试，全部通过；版本与开源边界检查通过。
- 在 `/private/tmp/forge-plugin-deploy.cHdaDM/source` 从该提交创建独立本地副本、detached HEAD，
  仅复用已安装 node_modules；副本构建前后 git status 均干净，没有复制本地环境覆盖文件。
- Node 20.19.0 构建命令：

  ```sh
  VITE_PUBLIC_PATH=/forge VITE_BASE_URL=/forge VITE_REQUEST_PREFIX=/forge-api VITE_OUT_DIR=dist \
  NODE_OPTIONS=--max-old-space-size=8192 node node_modules/vite/bin/vite.js build --mode production --logLevel warn
  ```

- 构建成功，10130 modules / 954 chunks；仅已有 PLUGIN_TIMINGS 性能提示。
  version.json：1.2.0，admin-ui，commit 为功能提交，builtAt 为 `2026-10-10T14:12:24.697Z`。
  构建日志：`/private/tmp/forge-plugin-deploy.cHdaDM/build.log`。
- UI 包 SHA-256：`3dc7a867a4651650f0a2c80d5aa8e83755fd16f3c06f201ee89efef92b602fd6`。
  1041 文件 SHA256SUMS 清单摘要：`fa2c76a518b0d5a97172a01b621bac67e966db394fbc9e4b5bec089390d91048`。
  上传到专用暂存目录后先核验包、清单和脚本摘要，解包拒绝越界路径或链接。
- 生产旧 UI 备份：`/www/wwwroot/admin-service/backups/plugin-visual-561ba23f/ui.tar`，
  摘要 `c610f1cd06a2ed43d26a4a44049036a4682c108422d73da6521e61dbf01678c0`。
  备份目录 0700，未覆盖上轮备份。
- 第一次暂存时，GNU coreutils 9.4 的 `cp -an` 对已有文件返回非零，部署在切换前停止。
  只读确认旧 index 摘要、后端 PID 与启动时间均未变；改为逐项比较同名资源、仅复制缺失文件后重试。
  没有忽略复制失败、跳过摘要校验或删除旧资源。
- 暂存新包保留旧哈希资源，1041 项新文件再次全部校验通过；用 Linux renameat2 原子交换 UI 目录。
  切换后再次核验 1041 项，失败保护会交换恢复；本次最终检查成功，没有执行回退。
- 当前目录 `/www/wwwroot/html/dist`；完整上一版仍保存在
  `/www/wwwroot/admin-service/releases/plugin-visual-561ba23f/ui`。
  原子交换双方目录即可回退；后续若有其它部署，必须重新核对当前 commit，不能盲目重放脚本。
- 公网 `/forge/` 的 index.html、version.json、预加载 JS/CSS、插件组件、ForgeSymbol、关于系统及新插画
  共 170 项 HTTP 200，摘要与构建逐项一致，JS/图片 Content-Type 正确。
  验证脚本：`/private/tmp/forge-plugin-deploy.cHdaDM/verify-public.mjs`。
- 公网 `/forge-api/auth/loginConfig` 返回 code=200；匿名 `/forge-api/system/version` 仍返回 code=401。
  未使用伪造 Token 或登录旁路；登录后完整业务交互仍复用本轮隔离组件验收，不冒充生产账号验收。
- 后端 spring_forge-admin 保持 active/running，PID 2199367，NRestarts=0，启动时间仍为 21:22:36，
  readiness 为 UP。Nginx 配置检查通过且摘要保持
  `613ce14a3c0921936d22e74e1c7a7dae2f1922f9eadad38881677bb008e667c5`，未 reload 或改配置。
- 只部署 Admin UI，不部署 Admin JAR/App/Flow/Report/H5/Website，不执行生产 SQL。
  后端仍是上一轮 100ce072 构建，前后端版本均为 1.2.0 但 commit 不同；“关于系统”的来源差异提示
  仍按既有规则展示，本轮不伪造统一提交号。
- 版本保持不变、CHANGELOG 为 Unreleased；没有创建发布标签或宣称新语义版本发行。
  后续文档提交只记录结果，不改变已部署源码锚点 561ba23f。
- 独立无登录态浏览器访问生产 `/forge/system/plugin`，HTTP 200，正常跳转 `/forge/login`，
  登录输入区可见，pageerror=0；没有尝试验证码、读取用户会话或执行登录/业务操作。
  截图 `/private/tmp/forge-plugin-deploy.cHdaDM/production-login.png` 已检查。
- 本轮浏览器实例已关闭，临时 SSH 控制连接已退出；未留下新增开发服务。
  构建包、干净源码副本与远端回退备份保留供核验。

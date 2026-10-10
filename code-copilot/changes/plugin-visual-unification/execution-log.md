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

# 执行记录

## 2026-10-17 开始

- 当前 `main` / `origin/main`：`7329a52f7`。
- 本轮不是正式发版，版本保持现有值，用户可感知变更写入 `CHANGELOG.md / Unreleased`。
- 确认现有下载审计接口只含成功记录，页面旧实现为全局圆形悬浮按钮。
- 已读项目设计、测试规范与 redesign-existing-projects；本轮保留 Naive UI、主题 Token 和线性工具图标。
- 未访问真实业务数据库/模型/对象存储；未改变原有六个未提交文件。

## 2026-10-17 完成实现与本地验收

### 实现

- `RecentDownloads` 拆分为入口、Panel、Row；使用 Naive UI Popover/Button/Progress/Skeleton/Popconfirm。
- normal/full/simple/nexus/top-side-menu 侧栏及 bento/side-flyout 窄栏接入；无侧栏/窄屏使用全局兜底。
- Nexus 窄屏由 CSS 隐藏改为卸载侧栏，防止同一个下载面板同时 Teleport 两份。
- Pinia 统一记录、分页、即时传输态及账号/租户/会话失效保护；历史来源复用既有字典。
- `downloadFile(fileId, ...)` 读取真实响应字节；没有总长度或响应压缩时不生成假百分比。
- 拒绝 4xx/JSON 错误后的缓存直链绕过；切换会话后的旧响应不再发起浏览器保存。
- COS 直链补记异步执行且跳过全局 Loading，不阻塞实际文件保存。
- 本地清空只保存最近记录的 ID，并按站点前缀/租户/账号隔离；不删服务端审计或实际文件。
- 主线版本 `1.2.0` 不变，更新 `CHANGELOG.md / Unreleased`，不是正式发版。

### 工具链与命令

本地 pnpm 12 的依赖自检会尝试重新安装，受网络和非交互环境限制而退出；没有允许清空 node_modules。
后续直接使用已安装工具验证，没有更改 package.json、锁文件或依赖版本。
当前已安装 Vite 8.2.1 / Vitest 2.1.9，使用已存在的 Node 24.21.0。

在 `forge-admin-ui` 执行：

```bash
source /Users/mini32g/.nvm/nvm.sh && nvm use v24.21.0
node node_modules/vitest/vitest.mjs run \
  src/stores/system/__tests__/recentDownloadsStore.spec.js \
  src/stores/system/__tests__/recentDownloadsUtils.spec.js \
  src/utils/__tests__/download-events.spec.js \
  src/utils/__tests__/file-download.spec.js \
  src/api/system/__tests__/downloads.spec.js \
  src/layouts/components/downloads/__tests__/RecentDownloadPanel.spec.js \
  src/components/ai-form/crud/__tests__/useCrudImportExport.spec.js \
  src/utils/__tests__/navigation-theme.spec.js
```

结果：8 文件、104 用例通过（新增 49 项，相关既有 55 项）。

- 对全部本轮 JS/Vue 文件执行 `node node_modules/eslint/bin/eslint.js <本轮文件>`：退出码 0。
- `git diff --check`：无输出，退出码 0。
- 完整构建（最终代码）：

```bash
node --max-old-space-size=8192 node_modules/vite/bin/vite.js build \
  --outDir /private/tmp/forge-recent-downloads-build-20261017-verified --logLevel error
```

结果：退出码 0，10166 模块完成转换、972 chunks 完成渲染。仅生成临时验证产物，不部署。
首次构建曾提示 UnoCSS 插件耗时、临时 outDir 位于工程外；非阻断，不修改构建规则。

### 全量前端测试与基线对照

`node node_modules/vitest/vitest.mjs run --maxWorkers=4 --minWorkers=1`：

- 344 文件通过、8 文件失败；2529 用例通过、25 用例失败，116.04 秒。
- 存在旧测试的组件 stub 未注册、Sass legacy API 及 transfer 重复注册告警，不代表本轮全部测试通过。
- 八个失败文件：
  - `src/composables/__tests__/useDict.spec.js`
  - `src/utils/__tests__/dict-options.spec.js`
  - `src/views/app-center/__tests__/application-runtime-load.spec.js`
  - `src/components/lowcode-builder/page/__tests__/grid-block-renderer-data-source.spec.js`
  - `src/components/lowcode-builder/page/__tests__/list-page-form-asset-binder.spec.js`
  - `src/components/lowcode-builder/page/__tests__/list-page-grid-designer-nested-selection.spec.js`
  - `src/components/lowcode-builder/shared/__tests__/runtime-crud-props.spec.js`
  - `src/components/lowcode-builder/shared/__tests__/runtime-form-layout.spec.js`
- 使用 `git archive HEAD` 导出干净基线至 `/private/tmp/forge-downloads-baseline.7aacf1`，
  仅复用相同 node_modules，未切换、清理或修改当前/其他工作树。
- 八个文件在干净基线与工作区均复跑为 **25 失败、93 通过**。
  JSON 报告比较结果：`baselineFailed=25, workingFailed=25, identicalFailedCases=true`。
- 报告保留在该临时目录的 `baseline-results.json` 与 `working-results.json`，本轮不修复这些既有失败。

### 浏览器验证

- 使用 `verification/preview.mjs` 在本机 3041 端口隔离挂载正式组件。
- 列表、字典和文件流均为明确标注的合成夹具；正式组件继续调用原真实 API，夹具不进入生产构建。
- 点击历史文件走实际本机流；验证进入接收中、真实字节/百分比、禁止重复点击、失败及恢复后重试。
- 核对错误不显示成空态；清空确认明确保留后台审计；清空后显示空态；Escape 回到入口。
- 验证深色、折叠、375×812 窄屏和滚动；窄屏面板 left=13/right=366/top=192.5/bottom=755，
  位于视口内。较矮桌面高度中，增加传输条目后面板 top=106/bottom=648，未越出 720px 视口。
- 截图保存在当前任务可视化目录的 `recent-downloads/implementation/`，包含 light-final、dark、
  compact、mobile、error、empty 和 progress。
- 最终验证进程 PID **31050**（PTY 58352），已 Ctrl-C 停止，退出码 130；验证标签页已关闭。
  未停止原来的 3038 设计预览或用户其他前后端进程。临时视口已 reset。

### 实现阶段交付状态（追加提交授权前）

- 当前分支仍为 `main`，原有六个无关改动保留。
- 未提交、未推送、未发布、未部署；没有执行后端迁移或真实环境业务调用。

## 2026-10-17 提交推送前复核

- 用户追加授权 `push`，只提交最近下载改造及本变更记录，目标 `main`；不部署、不打标签、不升版。
- `git fetch origin main` 发现远端打印设计器修复 `99448574a`；检查六个变更文件均不与本轮或原有
  未提交文件重叠，执行 `git merge --ff-only origin/main` 成功，保留所有工作区改动。
- Node v24.21.0，复跑前述八个相关测试文件并加入打印设计器 `placement.spec.js`，
  Vitest 结果 9 文件、109 项通过，2.01 秒。
- `pnpm check:edition` 通过（首次检查索引 10324、工作区 10346 个文件）；迁移版本号查重无输出。
- 复用上文完整构建、视觉验收及全量失败的干净基线对照，不将原有失败或真实后端未验收改写为通过。
- 暂存采用明确文件清单；原有 `.DS_Store`、多色菜单图标变更的四份记录和部署手册不纳入提交。
- 此条记录为提交前检查，实际提交号及推送结果以 Git 和本次会话回执为准。

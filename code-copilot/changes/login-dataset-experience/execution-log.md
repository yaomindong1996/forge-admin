# 执行记录

- 2026-10-03：起始分支 codex/workbench-illustrations，HEAD c4704ffd，工作区仅用户 .DS_Store 修改。
- 已读取项目设计、编码与自动测试标准；生成三张新轮播插画。
- 数据集原 SFC 1524 行，editSchema 只引用 datasetEditor；旧插槽可在保持当前配置协议的前提下清理。

## 实现

- 登录保留原认证 composable，仅更换视图、局部主题与默认图标，新增三张独立轮播插画。
- 三张透明 WebP 合计 198168 字节，原始 PNG 保留；完整生成提示词见 image-prompts.md。
- 登录支持链接打开共享 Naive UI 二维码弹窗，优先使用登录接口已配置的群图；其余使用原有社区图片。
- 首页将社区支持入口放在欢迎区下方，二维码按需展开。
- 数据集列表使用公共主从工作区；分类、搜索与附加筛选分层，九列收敛为七列，保留全部七种业务操作。
- 数据集入口缩为 295 行，配置拆为基础与来源、查询条件、执行设置、权限控制面板；共享状态用 Pinia。
- 本轮修改的 SFC 均小于 1000 行。未修改数据集保存、发布、下架与行级权限接口协议。

## 增量验证

环境：Node v20.19.0；复用项目已有 ESLint、Vitest 与 Vite，不安装新依赖。

1. 单测：执行登录体验、密码重置渠道、首页区块、数据集共享上下文与数据集体验五个测试文件。
   `vitest run` 结果：5 个文件、14 项测试全部通过，耗时 2.31 秒。
2. 定向 ESLint：新组件、入口视图、共享 store、局部主题及新增测试全部通过。
   未对历史超大 composable 执行整文件自动修复，避免夹带无关变更。
3. 生产构建：`node --max-old-space-size=8192 node_modules/vite/bin/vite.js build` 成功，耗时 31.92 秒。
   构建仍有项目原有动态/静态混合导入、CSS 注释与插件耗时警告，不是本轮新增编译错误。
4. `git diff --check` 通过；SFC 行数检查通过。
5. 浏览器检查使用真实修改组件与原数据集 composable 的隔离预览，示例接口数据仅用于布局检查：
   - 桌面三张轮播切换、输入与按钮对齐、支持弹窗打开关闭、二维码预览入口正常。
   - 375px / 320px 窄屏登录布局正常，无横向溢出。
   - 320×568 下二维码弹窗与关闭按钮完整可见，二维码内容可以滚动。
   - 首页靠前社区入口可打开同一弹窗。
   - 数据集列表、附加筛选弹层、四步骤切换正常；已发布查看模式的输入与保存保持只读。
   - 复核只读 SQL 编辑器 DOM 的 contenteditable=false，未用可访问性标签误判为可编辑。

## 限制与收尾

- 线上 /forge/data/dataset 重定向到登录页，需要验证码；没有绕过认证，也没有执行真实保存、发布或删除。
- 隔离预览不替代真实后端端到端验收，数据集发布/权限写入仍需测试账号联调。
- 关闭临时浏览器标签与预览服务，恢复临时视口设置。
- 只提交本轮文件，保留用户 .DS_Store 修改；分支 codex/workbench-illustrations，本轮不推送。

## 2026-10-03：轮播指示器首屏可见性修正

- 起点：894f6c77，只有用户 .DS_Store 修改；该提交已按用户要求推送 main。
- 修正前实测 1366×600：品牌区被右侧表单撑到 704px 高，指示器 top=618.77px，箭头 bottom=650.77px，均超出首屏。
- 左侧改为固定 100dvh 的四行布局，箭头/指示器独立于 NCarousel 裁切区域；右侧桌面表单独立滚动。
- NCarousel 与控制行使用同一 currentIndex；指示器增大点击区域、提高对比度，选中项改为白色短条。
- 不改变认证、后端协议与数据集，保留移动端隐藏品牌区和减少动态效果设置。

验证命令（Node v20.19.0，forge-admin-ui 目录）：

```bash
node_modules/.bin/vitest run src/views/login/__tests__/login-experience.spec.js src/views/login/__tests__/reset-password-channel.spec.js
node_modules/.bin/eslint src/views/login/components/LoginBrandPanel.vue src/views/login/__tests__/login-experience.spec.js
node --max-old-space-size=8192 node_modules/vite/bin/vite.js build
git diff --check
```

- 6 项单测通过，包含真实 Naive UI 自动轮播推进、索引同步、箭头首尾循环与二维码原有行为。
- 定向 ESLint 与 diff 检查通过；生产构建成功，耗时37.33秒，原有混合导入与构建耗时警告不阻断。
- 浏览器：1366×600 品牌区/pageHeight=600，控制行 bottom=550.41；右侧 scrollHeight=704、scrollTop=104.5，滚动不影响左侧控件位置。
- 1280×720 控制行 bottom=662.41；1440×900 bottom=822.41，均完整处于视口内。
- 375×812：左品牌区 display=none，页面宽度=375，无横向溢出。
- 临时预览仅使用真实组件与隔离数据，不连接业务后端；恢复视口并关闭本轮标签、预览服务 PID=67677 后提交，不自动推送。

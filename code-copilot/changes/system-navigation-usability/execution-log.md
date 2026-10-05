# 执行记录

## 2026-10-05 范围确认

- 基线 92997659，分支 codex/workbench-illustrations，与 origin/main 一致；用户 .DS_Store 不纳入。
- 根因：layout-chrome.css 将 active/intent/open/hover 合并，并统一赋 top-menu-bg-color-hover，
  覆盖业务工作台基础样式；公共 SystemTableCell 主值为 600；多个组织树重复使用厚重 Material 图标。
- 菜单管理常驻三栏，低于 1280px 直接隐藏详情，筛选/未选批量动作挤占工具栏；本轮改为两栏加按需详情。
- 已读取 AGENTS、DESIGN、编码/测试标准、相关前端踩坑与基线。无匹配的专门项目 UI Skill，按项目规范执行。
- 线上菜单页在临时标签 12 跳转登录，没有进行登录、写入或操作权限；该标签已关闭。

## 实现与修复

- 公共 workbench 顶级导航覆盖只保留文字状态和独立横条，active/intent/open/hover 均透明；
  不是重复渲染父菜单，而是旧公共 CSS 将 hover 背景也赋给 active。
- 用户/组织/岗位/角色/人员选择共用组织图标解析；展开箭头和公共默认树图标线性化，
  自定义图标、受控 keys、懒加载、级联选择保持，SystemTableCell 主值字重改为 400。
- 菜单管理由常驻三栏改为两栏+按需 Naive 详情抽屉；名称进入下级、面包屑返回，筛选按需展开，
  勾选后才出现批量动作；写操作仍调用旧校验/确认/接口，不改权限与资源存储码。
- 菜单 SFC 转为薄 script setup，template ref 连接真实 composable；详情/新增界面状态进入 Pinia，
  新编排独立 composable；新增顶级修正为显式 0，避免继承当前目录。
- 第一次目标 Lint 报 742 项（701 error、41 warning），多数是历史 part 文件的未用导入、
  过量依赖解构与压缩返回格式；清理未用代码、重复私有注册、提取角色组织纯函数并格式化后通过。
  没有关闭规则、降低断言或扩大历史大文件；公开接口与跨 part 延迟实现保留。
- 浏览器发现 JS 返回的新动态图标未生成 CSS；加入既有 dynamic-icons safelist 并重启隔离预览后，
  实际 mask 和 14px 图标尺寸均生成；增加构建清单契约测试，避免开发有图标、生产丢失。
- 菜单列表编辑改为轻量文字操作，避免历史全局主按钮 CSS 将文字按钮填满；只改本菜单操作区，
  不重写全站按钮主题。菜单表面、选中底色、分隔线改用已有 Token，删除旧数据表无用样式。

## 自动化结果

- Node v20.19.0，最后矩阵 24 文件 223 项全部通过，4.48 秒；本轮新增 29 项。
- 初始 6 文件 59 项通过；追加角色组织兼容及 safelist 契约后最终 223 项复跑，没有 skip。
- 全部修改 tracked JS/Vue 与 8 个新增源/测试文件 ESLint 退出 0；git diff --check 通过。
- 生产构建初次退出 0，36.99 秒；最后补语义背景后再次构建，结果在下节追加。
- 新详情组件 102 行、编排 63 行、菜单 SFC 625 行；历史 part/组织页均缩短，剩余边界见 tasks。

## 浏览器验证与限制

- 真实 App、布局、Store、组织树、用户列表、菜单页与 Naive 浮层，使用 /private/tmp 隔离 Vite 预览。
  仅模拟用户/授权菜单/查询 API，所有业务写接口拒绝；不连接真实后端或数据库。
- 第一次预览 page-pathes 桩返回对象而非数组，用户区域行政区划桩同样漏配数组；
  仅修正预览桩，不改产品容错来掩盖错误，最终使用新标签重跑确认。
- 实测浅色父菜单 background=rgba(0,0,0,0)，深色亦透明，深色菜单文字白色；
  选中横条和横向滚动仍在，详情浮层不改变顶栏角色颜色。
- 点击资源名称进入下级；筛选按钮类资源显示 2 项，重置回到 23 项直属菜单；
  勾选显示批量迁移/删除/取消，未提交真实删除或迁移；详情抽屉关闭可回列表。
- 1280/1024px 两栏，1024 下行宽 778px=可视宽、操作 right=997px；390px 页面宽=390px，
  详情 width=390px/right=390px/footerBottom=740px，所有底部动作在视口内。
- 用户页展开后根建筑/分支组织/成员组五个线性图标都有 CSS mask，主文字 computed font-weight=400。
- 未做真实用户详情写入、菜单新增/修改/删除、排序/显示、权限/身份切换或真实登录 E2E；
  这些流程的接口和旧校验保留，由模拟回归验证，不等于线上已验证。

## 最终复查与清理

- 最后生产构建退出 0，35.34 秒；日志 /private/tmp/forge-system-navigation.cfiocl/build-final.log，
  仅有既有插件耗时提示。实际 dist CSS 检查新组织/资源动态图标的选择器均存在。
- 最后新标签 14 在真实用户页展开组织树、通过父级 Mega 菜单直接导航菜单管理、再进入下级列表，
  error 日志为空；父级 is-open 实测 background=rgba(0,0,0,0)。
- 隔离预览仍有未注册 table-scroll-enhance 指令、根路由未配置和既有 transfer 重复注册 warning；
  预览未加载项目完整入口插件，未把这些 warning 隐藏，也不据此宣称真实应用端到端无告警。
- 初期预览旧标签日志保留桩错误；最终新标签无这些错误。工具曾将 Naive 主题按钮误按 checkbox 定位，
  改用已观察到的实际控件；二级菜单已直接导航，后续“进入功能”无匹配，不重复操作或修改产品逻辑。
- 最终截图 /private/tmp/forge-system-navigation-preview.png；组织树截图 /private/tmp/forge-system-navigation-tree.png。
- 标签 13/14 均已关闭，临时视口恢复；仅本轮隔离 Vite 会话已 Ctrl+C 停止，3032 无监听，
  不影响用户其它服务/标签。临时文件保留供核对，不纳入仓库。
- 用户 .DS_Store 原样保留，源码/变更文档本地提交到 codex/workbench-illustrations；不自动 main 合并或 push。

复跑命令：先进入 forge-admin-ui，source NVM 并 nvm use v20.19.0，复用 layout-theme-consistency/T12
日志中的原 16 文件 Vitest 命令，再追加 test-spec 的 8 文件；构建命令仍为
node --max-old-space-size=8192 node_modules/vite/bin/vite.js build。

## T05 2026-10-05 末级菜单配色修复

- 基线 88844f77；fetch 后与 origin/main 相同，用户明确要求本次完成后推送 main。
- 根因确认：公共面板覆盖将 --workbench-primary 设为 side-menu-text-color-active，旧配置白字本应
  使用实色选中底，末级入口却用 6% 混合色，产生白字近白底；并非菜单重复或后端数据问题。
- 将品牌色与选中文字色分离，二级及任意深度末级入口 hover/focus-visible 和 active/current 分别成对
  使用对应侧栏前景/背景 Token，选中规则后置保持悬停选中一致，二级箭头继承按钮文字色。
  删除公共二级 hover/active 混用覆盖；不改租户配置/权限/接口/路由及顶级透明背景/横条。
- 第一次静态测试 107 通过、1 失败：测试 CSS 规则解析器把前置注释并入选择器。
  修正解析器移除 CSS 注释后，原断言不变；最终 6 文件 108 项通过，新增 8 项，1.09 秒。
- 复跑命令（forge-admin-ui，先 source NVM 并 nvm use v20.19.0）：
  node node_modules/vitest/vitest.mjs run src/layouts/__tests__/layout-theme-surfaces.spec.js
  src/utils/__tests__/navigation-theme.spec.js src/components/common/__tests__/system-navigation.spec.js
  src/layouts/business-workbench/__tests__/menu-model.spec.js
  src/layouts/business-workbench/__tests__/menu-scroll.spec.js
  src/layouts/business-workbench/__tests__/settings.spec.js（上述参数在同一条命令中执行）。
- node node_modules/eslint/bin/eslint.js src/layouts/__tests__/layout-theme-surfaces.spec.js 退出 0；
  git diff --check 通过；node --max-old-space-size=8192 node_modules/vite/bin/vite.js build 退出 0。
  构建耗时 37.90 秒，日志 /private/tmp/forge-workbench-menu-state-build.log，仅既有插件耗时提示。
- 复用 /private/tmp/forge-system-navigation.cfiocl 真实 App/布局/组件的隔离预览，追加配色控件与五级菜单。
  Vite 初次启动受沙箱限制 EPERM，提升权限后仅监听 127.0.0.1:3032；没有连接真实后端或写业务数据。
- 浏览器 1280px 实测：白字选中 rgb(255,255,255) 配蓝底 rgb(47,111,237)，文字/图标一致，
  已选项悬停不改变底色；未选项悬停/深层聚焦为黑字配 rgb(238,244,255)。
- 自定义白字悬停配 rgb(51,65,85) 深灰底；深色悬停配 rgb(38,52,73)；默认自动模式文字配浅灰底。
  多层入口正常跳转，重新打开仍有 aria-current=page，is-current/is-nested/is-deep 状态与背景正确。
- 浏览器 error 日志为空；沿用预览根路由缺少 /、设计器 transfer 重复注册等 warning，未隐藏，
  不将隔离预览表述为真实租户环境 E2E；未更改浏览器视口。
- 截图 /private/tmp/forge-workbench-menu-state.png；本轮标签 15 已关闭，临时 Vite PID 57268 已 Ctrl+C 停止，
  3032 无监听，没有停止其它服务。
  用户 .DS_Store 与 main 工作区 .ci-tools/ 保留，不纳入提交；仅本轮源码、回归测试和文档合入 main。

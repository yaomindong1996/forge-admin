# 执行记录

## 2026-10-03

- 最新基线：`75293e3e`，创建 `codex/workbench-illustrations`。
- 本地已有 `.DS_Store` 修改，保留并排除提交。
- 线上 `/forge/flow/model` 重定向到登录页，后续采用本地实际组件视觉预览。
- 已读取 AGENTS.md、DESIGN.md、imagegen Skill、用户偏好及增量测试标准。
- 内置 imagegen 生成三张透明素材，原始文件留在生成目录，WebP 成品接入项目。
- WebP 资源 alpha 均为 true：欢迎图 768×546 / 77,574 bytes；应用图 320×306 / 17,894 bytes；流程图 320×431 / 14,504 bytes。合计 109,972 bytes。
- 新增 `WorkspaceIllustration` / `IllustratedEmpty`；首页欢迎区拆出 `HomeWelcomeProfile`，流程卡片拆出 `FlowModelCard`。
- 首页与应用总览样式移入同域 CSS：SFC 分别收敛至 605 / 966 行；流程模型入口 406 行，卡片 128 行。
- 仅改变展示结构和样式；后端应用图标继续保留，模型标签/分类/权限仍由原页面提供。表单引用显式回调连接原 composable。
- 临时实际组件 Vite 预览仅监听 127.0.0.1:3031，未启动后端；1280、320、390px 与明暗主题检查完成。浏览器控制台 error 列表为空。
- 样例预览点击结果：应用运行、发布、导出；流程编辑、发布、实例、版本历史都正确返回原条目 ID。未发出真实写接口。
- 暗色 CSS 编译检查发现旧 `:global` 写法将目标样式落到根节点，已改为 scoped 后代选择器，并复核操作区背景与文字。
- 目标 ESLint 两轮 exit 0；`vitest run src/views/app-center/__tests__/home-workbench-apps.spec.js`：1 文件 / 3 测试通过。
- 最终 Node 20.19.0 / Vite 8.2.1 生产构建 exit 0，43.20s。保留已有目录导入、CSS 双斜杠注释及无效动态导入警告，不扩大本轮修改范围。
- `git diff --check` 通过。临时预览服务已停止，预览源码已移除，不进入生产提交。浏览器视口已恢复默认。

## 2026-10-03 首页四区增量

- 当前分支 `codex/workbench-illustrations`；已 fetch `origin main`，基线 `0f2a473a` 与 main 无差异。无跨项目或其它分支合并。
- 使用原业务路由和现有数据请求，拆出 `HomeBuildPath`、`HomeApprovalCenter`、`HomeTodoList`、`HomeQuickEntries`；公共身份区与静态 SVG 图标由 `HomeSection` / `HomeActionIcon` 提供。
- 待办改整行原生按钮、节点/人员/时间元信息、字典优先级与状态；空态不再声称“全部处理完成”，初次请求提供骨架屏。
- 欢迎插画收紧至 160×112px；四步导航独立，社区二维码保留预览并移动到业务工作区之后。无需新增图像资源。
- 首页入口 436 行，新增六个 SFC 均小于 210 行，满足 1000 行上限；无后端、SQL、鉴权或菜单配置变更。
- 最终目标 ESLint 10 文件 exit 0；Vitest 两文件、8/8 测试通过（四区 5 个 + 原首页应用协议 3 个）。
- 浏览器隔离预览验证实际组件：1280px、390px、320px；浅色、暗色；正常任务、长标题、空态和待办加载骨架。首页 scrollWidth/clientWidth 在最终 390/320px 下分别均为 390/320。
- 实点待办保留任务 ID、source=home、时间查询；流程编排、用户管理、审批统计 Enter 键均触发原路由。控制台 error 列表为空，无真实写接口调用。
- Node 20.19.0 / Vite 8.2.1 最终构建 exit 0，44.12s、9995 modules。保留既有第三方打包与 CSS 警告，不扩展本轮范围。
- 已关闭临时预览页、恢复浏览器默认视口，停止本人创建的 3031 服务；五个临时预览源码已删除，不进入提交。保留用户 `.DS_Store` 修改。
- 按项目规则完成本地 commit；本轮未获新的 push 指令，不自动推送。

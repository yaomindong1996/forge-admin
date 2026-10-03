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

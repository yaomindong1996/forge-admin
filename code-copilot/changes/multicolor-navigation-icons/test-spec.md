# 增量验证

复用现有 navigation-icons、图标渲染和插件视觉测试，仅新增一级多色回归。

- P0：两版导航映射同源、幂等，旧 forge 配置兼容、未知名称回退，原菜单和子级不修改。
- P0：所有多色符号有两种描边；普通符号仍 currentColor；无外部图片/不安全 SVG 路径。
- P1：浏览器检查真实 SideMenu/IconRenderer 的白底、深色、选中/悬停和折叠状态。
- 双工程相关 Vitest、改动文件 ESLint、生产前端构建及 git diff --check。
- 仅导航显示改动，不连接业务数据库，不执行后端构建或真实部署。

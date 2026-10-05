# 增量验证计划

## 复用基线

- layout-theme-consistency 第七阶段 150 项测试；归档 system-management-ui-interaction-fixes 的菜单直属子项/刷新回绑测试。
- 当前 menu-interaction-utils 9 项回归与已有公共单元格、组织选择相关测试继续执行，不跳过、不削弱断言。

## P0

- 工作台 active/open/intent/hover 透明，横条独立 Token 不变；浅/深色实际样式无冲突。
- 组织根/分支/叶子线性图标与所有调用点一致；自定义树图标不覆盖；展开/选择/级联勾选事件保持。
- 公共单元格主字重 400，实体 activate、关联 +N 与辅助文本保持。
- 面包屑/名称进入当前层正确，默认只直属子项，筛选递归只在当前范围；客户端变化关闭过期浮层。
- 详情抽屉按需出现，可关闭/编辑/新增；工具区域与列表独立滚动且窄屏不丢详情。
- 筛选计数/重置和批量清除可用，原删除与迁移确认、排序/显示协议不变；不触发真实写请求。

## P1 与交付

- 1280/1024/390px 布局、滚动与控件可达性，浅/深主题；图标无空白占位。
- 修改 JS/Vue 目标 ESLint、git diff --check、Vite 生产构建。
- 浏览器复用本地真实 App/Store/组件预览，仅模拟查询；线上访问重定向登录，不主动登录或写入。
- 记录实际命令、失败与修复、最终截图和服务/标签清理；本地提交，不自动 push。

## 本轮落地矩阵

- 复跑 layout-theme-consistency/T12 原 16 文件 150 项，不削弱旧断言。
- 追加 system-navigation（18）、menu-workspace（8）、menu-interaction-utils（12）、
  user-management-store（15）、user-management-components（5）、system-management-ui-contract（5）、
  user-role-order（2）、role-permission-navigation（8）；24 文件合计 223 项。
- 本轮新增 29 项（18+8+3），其余 194 项为原回归；包含真实 Naive UI、PremiumTree、SystemTableCell、
  MenuPage 组件和权限范围纯函数，仅接口/路由/设计器大组件作必要隔离，不以全量桩代替交互。

---
project: Forge Flow Designer
created: 2026-05-04
design_system: UI/UX Pro Max
version: 1.0.0
---

# Forge Flow Designer - Master Design System

## 设计理念

基于 **Flat Design** 风格，打造现代化、专业的企业级流程设计器。强调简洁、清晰的视觉层次，使用纯色和明确的边界，避免过度装饰。

## 配色方案

### 主色调 - Indigo 系列
```css
--primary: #6366F1        /* 主色 - Indigo 500 */
--primary-hover: #4F46E5  /* 主色悬停 - Indigo 600 */
--secondary: #818CF8      /* 次要色 - Indigo 400 */
--success: #10B981        /* 成功色 - Emerald 500 */
```

### 背景与表面
```css
--background: #F5F3FF     /* 页面背景 - Indigo 50 */
--surface: #FFFFFF        /* 卡片/面板背景 */
```

### 文本颜色
```css
--text-primary: #1E1B4B   /* 主要文本 - Indigo 950 */
--text-secondary: #64748B /* 次要文本 - Slate 500 */
```

### 边框与分隔
```css
--border: #E2E8F0         /* 边框颜色 - Slate 200 */
```

## 字体系统

### 字体族
- **主字体**: Fira Sans (Google Fonts)
- **代码字体**: Fira Code (用于技术标签、代码片段)
- **后备字体**: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif

### 字体导入
```css
@import url('https://fonts.googleapis.com/css2?family=Fira+Code:wght@400;500;600;700|Fira+Sans:wght@300;400;500;600;700&display=swap');
```

### 字体使用场景
- **仪表盘/数据**: Fira Sans - 清晰、易读、专业
- **技术标签**: Fira Code - 等宽、精确、技术感

## 视觉效果

### 圆角 (Border Radius)
```css
--radius-sm: 8px   /* 小元素 - 按钮、输入框 */
--radius-md: 12px  /* 中等元素 - 卡片、面板 */
--radius-lg: 16px  /* 大元素 - 容器、模态框 */
```

### 阴影 (Box Shadow)
Flat Design 使用轻量阴影，强调层次而非深度：
```css
--shadow-sm: 0 1px 2px 0 rgba(99, 102, 241, 0.05)
--shadow-md: 0 4px 6px -1px rgba(99, 102, 241, 0.08)
--shadow-lg: 0 10px 15px -3px rgba(99, 102, 241, 0.1)
```

### 过渡动画
```css
--transition: 200ms cubic-bezier(0.4, 0, 0.2, 1)
```

**原则**:
- 快速响应 (150-200ms)
- 使用 ease-out 曲线
- 避免复杂动画
- 尊重 `prefers-reduced-motion`

## 组件规范

### 按钮
- **主按钮**: 纯色背景 `var(--primary)`，白色文字
- **次要按钮**: 透明背景，主色边框和文字
- **悬停**: 颜色变深 + 轻微阴影
- **禁用**: 50% 不透明度 + `cursor-not-allowed`

### 输入框
- **边框**: 2px solid `var(--border)`
- **聚焦**: 边框变为 `var(--primary)`
- **圆角**: `var(--radius-sm)`
- **内边距**: 8px 12px

### 卡片/面板
- **背景**: `var(--surface)`
- **边框**: 2px solid `var(--primary)` (强调) 或 1px solid `var(--border)` (普通)
- **圆角**: `var(--radius-lg)`
- **阴影**: `var(--shadow-md)`
- **悬停**: 阴影加深 + 轻微上移

### 工具栏
- **背景**: 渐变 `linear-gradient(135deg, rgba(99, 102, 241, 0.05) 0%, rgba(129, 140, 248, 0.05) 100%)`
- **边框**: 2px solid `var(--primary)` (底部)
- **内边距**: 12px 16px

## 交互规范

### 悬停状态 (Hover)
- 所有可点击元素必须有 `cursor: pointer`
- 颜色变化或背景高亮
- 过渡时间 200ms
- 避免布局偏移

### 激活状态 (Active)
- 轻微缩放 `transform: scale(0.95)`
- 或背景颜色加深

### 聚焦状态 (Focus)
- 2px 实线轮廓 `outline: 2px solid var(--primary)`
- 轮廓偏移 2px `outline-offset: 2px`
- 确保键盘导航可见

### 加载状态
- 按钮禁用 + 加载图标
- 骨架屏用于内容加载
- 操作超过 300ms 显示反馈

## 响应式设计

### 断点
```css
/* Mobile First */
@media (max-width: 768px)  { /* 移动端 */ }
@media (max-width: 1024px) { /* 平板 */ }
@media (min-width: 1280px) { /* 桌面 */ }
```

### 间距调整
```css
/* 移动端 */
padding: 12px 16px;

/* 桌面端 */
padding: 16px 24px;
```

## 无障碍 (Accessibility)

### 对比度
- 正常文本: 最低 4.5:1
- 大文本 (18px+): 最低 3:1
- 主色 `#6366F1` 在白色背景上符合 WCAG AA

### 键盘导航
- 所有交互元素可通过 Tab 访问
- 聚焦状态清晰可见
- 逻辑 Tab 顺序

### 屏幕阅读器
- 图标按钮使用 `aria-label`
- 表单输入使用 `<label>` 关联
- 语义化 HTML 标签

### 动画偏好
```css
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

## 避免的反模式

### ❌ 不要做
- 使用 emoji 作为 UI 图标
- 复杂的渐变和阴影
- 过度的动画效果
- 混乱的布局和信息层级
- 忽略无障碍要求

### ✅ 应该做
- 使用 SVG 图标 (Heroicons, Lucide)
- 简洁的纯色和清晰边界
- 快速、流畅的过渡
- 清晰的视觉层次
- 完整的键盘和屏幕阅读器支持

## 实施检查清单

在交付前验证：

- [ ] 所有可点击元素有 `cursor: pointer`
- [ ] 悬停状态有平滑过渡 (150-300ms)
- [ ] 聚焦状态对键盘导航可见
- [ ] 文本对比度达到 4.5:1 (正常) 或 3:1 (大文本)
- [ ] 响应式测试: 375px, 768px, 1024px, 1440px
- [ ] 尊重 `prefers-reduced-motion`
- [ ] 无 emoji 图标，使用 SVG
- [ ] 表单输入有关联的 `<label>`
- [ ] 图标按钮有 `aria-label`

## 参考资源

- **UI/UX Pro Max**: 设计系统来源
- **Google Fonts**: https://fonts.google.com/share?selection.family=Fira+Code:wght@400;500;600;700|Fira+Sans:wght@300;400;500;600;700
- **Tailwind Colors**: https://tailwindcss.com/docs/customizing-colors
- **WCAG Guidelines**: https://www.w3.org/WAI/WCAG21/quickref/

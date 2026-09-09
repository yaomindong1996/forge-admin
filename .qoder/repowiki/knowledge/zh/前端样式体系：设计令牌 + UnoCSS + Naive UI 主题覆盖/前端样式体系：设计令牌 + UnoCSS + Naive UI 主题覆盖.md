---
kind: frontend_style
name: 前端样式体系：设计令牌 + UnoCSS + Naive UI 主题覆盖
category: frontend_style
scope:
    - '**'
source_files:
    - forge-admin-ui/src/styles/design-tokens.css
    - forge-admin-ui/src/styles/theme.css
    - forge-admin-ui/src/styles/global.css
    - forge-admin-ui/uno.config.js
    - forge-admin-ui/src/settings.js
    - forge-h5-ui/src/uni.scss
    - forge-report-ui/src/settings/designColor.json
    - forge-report-ui/src/settings/designSetting.ts
---

## 1. 采用的样式系统

仓库包含三套独立的前端应用，各自维护自己的样式方案，但整体遵循“设计令牌（Design Tokens）+ CSS 变量 + 组件库主题覆盖”的统一思路。

- **Forge Admin（管理后台）**：基于 Vue3 + Vite + Naive UI，使用 [UnoCSS](forge-admin-ui/uno.config.js) 作为原子化样式引擎，配合 `presetWind3`、`presetAttributify`、`presetIcons`，并通过自定义 `responsiveFontUnifiedPreset` 与 `@unocss/preset-rem-toPx` 实现响应式字体与 rem→px 转换。主题色通过 `naiveThemeOverrides`（`src/settings.js`）注入 Naive UI，并在 `src/styles/theme.css` 中用大量 `!important` 覆盖 `n-button`、`n-data-table`、`n-menu` 等组件的默认样式，形成统一的扁平化企业风格。
- **Forge H5（移动端）**：基于 uni-app + Vue3，样式集中在 `src/uni.scss`，引入 uview-plus 主题变量 `$uni-*`，并覆写主色 `$uni-color-primary: #007aff` 等基础变量，统一移动端视觉。
- **Forge Report（报表编辑器）**：基于 Vue3 + Vite + NaiveUI，将颜色、图表主题、VChart 主题集中放在 `src/settings/designColor.json`、`src/settings/chartThemes/`、`src/settings/vchartThemes/` 下，由 `designSetting.ts` 提供默认主题配置（如 `appTheme: '#00d4ff'`、`darkTheme: true`）。

## 2. 关键文件与包

| 作用 | 关键路径 | 说明 |
|---|---|---|
| 设计令牌 | `forge-admin-ui/src/styles/design-tokens.css` | 定义 `--primary-*`、`--success-*`、`--warning-*`、`--error-*`、`--info-*`、`--gray-*` 九级灰阶、间距 `--space-*`、圆角 `--radius-*`、阴影 `--shadow-*`、字体、断点等，含 `.dark` 暗色模式与 `prefers-reduced-motion` 适配 |
| 主题覆盖 | `forge-admin-ui/src/styles/theme.css` | 覆盖 Header、顶部菜单、侧边菜单、Naive 按钮/表格/分页/树等全局组件样式 |
| 全局工具类 | `forge-admin-ui/src/styles/global.css` | 滚动条、玻璃态 `.glass`、卡片 `.card-enhanced`、状态标签 `.status-tag`、页面布局 `.page-wrapper/.page-header/.page-card`、表格操作列 `.table-actions`、表单区域、空状态等 |
| UnoCSS 配置 | `forge-admin-ui/uno.config.js` | 启用 Windi/Tailwind 兼容、图标前缀 `ai-/i-/mdi-/fa-`、快捷方式 `wh-full/f-c-c/card-border/auto-bg` 等 |
| 主题配置 | `forge-admin-ui/src/settings.js` | `defaultPrimaryColor: '#4242F7'`、`naiveThemeOverrides`、布局枚举 `layoutSettings.layouts` |
| H5 主题变量 | `forge-h5-ui/src/uni.scss` | 覆写 uview-plus `$uni-color-*`、`$uni-font-size-*`、`$uni-spacing-*` 等 |
| 报表配色 | `forge-report-ui/src/settings/designColor.json` | 3600+ 项带中文名、拼音、CMYK/RGB/hex 的颜色清单 |
| 报表主题 | `forge-report-ui/src/settings/designSetting.ts` | 默认主题色 `#00d4ff`、暗色开关、边框圆角、预览缩放等 |

## 3. 架构与约定

### 3.1 设计令牌分层
- **语义层**：`design-tokens.css` 中的 `--primary-500`、`--success-500`、`--text-primary`、`--bg-page` 等命名语义化，组件不直接引用硬编码色值。
- **组件层**：`theme.css` 把令牌映射到具体组件（如 `--side-menu-bg-color` → `.side-menu-wrapper .n-menu`），并通过 `--font-scale` 变量统一控制字号缩放。
- **运行时层**：`settings.js` 暴露 `defaultPrimaryColor` 和 `naiveThemeOverrides`，在应用启动时动态注入主题。

### 3.2 原子化 + 快捷方式
- UnoCSS 的 `shortcuts` 定义了业务常用组合：`wh-full`、`f-c-c`、`card-border`、`auto-bg`、`auto-bg-hover`、`text-highlight`，减少重复类名。
- 图标通过 `FileSystemIconLoader('./src/assets/icons/ai-icon')` 自动扫描本地 SVG，支持 `ai-`、`i-`、`mdi-`、`fa-` 前缀。

### 3.3 暗色模式策略
- 通过 `.dark` 类切换，`design-tokens.css` 中为每个 token 提供暗色覆盖（如 `--bg-primary: #0f172a`、`--text-primary: var(--gray-100)`）。
- 同时声明 `prefers-contrast: high` 高对比度模式和 `prefers-reduced-motion: reduce` 减少动画模式，满足无障碍需求。

### 3.4 组件库主题覆盖规范
- 所有对 Naive UI 的覆盖集中在 `theme.css`，使用 `!important` 强制生效，避免被组件内部样式覆盖。
- 按钮统一扁平化：去除阴影、固定 `min-height`、统一 `border-radius: 3px`，区分 primary/error/success/warning/info/text 五种类型。
- 表格表头 sticky、操作列固定右侧、行渐入动画（`.n-data-table-tr` staggered animation）。

### 3.5 多应用一致性约定
- 三个前端项目都通过独立的 `settings.js` / `uni.scss` / `designSetting.ts` 暴露主题入口，便于运行时切换。
- 颜色命名保持跨项目一致：Admin/H5/Report 均使用语义化主色（`#4242F7`、`#007aff`、`#00d4ff`），但允许按产品定制。

## 4. 约定与约束

- **禁止散乱色值**：新增颜色应优先从 `design-tokens.css` 的 `--primary-*` / `--success-*` / `--gray-*` 系列选取，而非手写十六进制。
- **组件样式集中管理**：对 Naive UI 的覆盖必须写入 `theme.css`，不得在单个 `.vue` 文件中散落 `style` 覆盖。
- **响应式断点统一**：使用 `design-tokens.css` 定义的 `--breakpoint-sm/md/lg/xl/2xl` 作为媒体查询基准，避免各组件自定断点。
- **H5 项目**：所有主题变量需通过 `uni.scss` 的 `$uni-*` 变量修改，以兼容 uview-plus 插件生态。
- **报表项目**：颜色与图表主题必须通过 `src/settings/` 下的 JSON/TS 配置管理，不允许在组件内硬编码颜色。
- **暗黑模式**：新增 token 必须在 `.dark` 块中提供对应覆盖，否则暗色模式下会出现不可读内容。
- **无障碍**：新增动画需考虑 `prefers-reduced-motion`，确保动画时长归零或禁用。

## 5. 适用性判断

本仓库是一个企业级中后台框架根工程，包含三套完整的前端应用，每套都有明确的主题系统、设计令牌、组件库主题覆盖与响应式策略，因此 `frontend_style` 类别完全适用。
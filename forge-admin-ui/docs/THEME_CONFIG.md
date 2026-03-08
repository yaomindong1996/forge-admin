# 主题配置系统使用文档

## 概述

主题配置系统允许你统一管理所有布局的样式，包括 Header、顶部菜单和侧边菜单的颜色、字体大小等属性。

## 文件结构

```
src/
├── config/
│   └── theme.config.js           # 主题配置文件
├── styles/
│   └── theme.css                 # 主题样式变量
├── components/
│   └── common/
│       ├── ThemeConfig.vue       # 主题配置面板
│       └── ThemeConfigButton.vue # 主题配置按钮
└── store/
    └── modules/
        └── app.js                # 主题状态管理
```

## 配置说明

### 1. 主题配置文件 (`theme.config.js`)

包含以下配置项：

#### Header 配置
- `backgroundColor`: 背景颜色
- `textColor`: 文字颜色
- `fontSize`: 字体大小
- `height`: Header 高度
- `borderColor`: 边框颜色

#### 顶部菜单配置
- `textColor`: 菜单文字颜色
- `textColorHover`: 菜单悬停颜色
- `textColorActive`: 菜单选中文字颜色
- `backgroundColor`: 菜单背景颜色
- `backgroundColorHover`: 菜单悬停背景颜色
- `backgroundColorActive`: 菜单选中背景颜色
- `fontSize`: 菜单字体大小
- `fontWeight`: 菜单字体粗细

#### 侧边菜单配置
- `backgroundColor`: 侧边菜单背景颜色
- `textColor`: 菜单文字颜色
- `textColorHover`: 菜单悬停文字颜色
- `textColorActive`: 菜单选中文字颜色
- `backgroundColorHover`: 菜单悬停背景颜色
- `backgroundColorActive`: 菜单选中背景颜色
- `borderColor`: 边框颜色
- `fontSize`: 菜单字体大小
- `fontWeight`: 菜单字体粗细
- `iconColor`: 图标颜色
- `iconColorActive`: 选中图标颜色
- `collapsedWidth`: 折叠宽度
- `width`: 展开宽度

### 2. 使用方式

#### 可视化配置（推荐）

1. 点击页面右上角的调色板图标打开主题配置面板
2. 在配置面板中调整各项颜色和字体大小
3. 点击"应用主题"保存配置
4. 点击"导出配置"可以导出当前配置为 JSON 文件

#### 代码配置

在 `theme.config.js` 中修改 `defaultThemeConfig`：

```javascript
export const defaultThemeConfig = {
  header: {
    backgroundColor: '#1e40af',  // 蓝色背景
    textColor: '#ffffff',
    fontSize: '14px',
    height: '60px',
    borderColor: '#1e3a8a',
  },
  // ... 其他配置
}
```

#### 使用预设主题

在 `theme.config.js` 中提供了多个预设主题：

```javascript
import { themePresets } from '@/config/theme.config'

// 应用蓝色主题
appStore.setThemeConfig(themePresets.blue)

// 应用深色主题
appStore.setThemeConfig(themePresets.dark)

// 应用绿色主题
appStore.setThemeConfig(themePresets.green)
```

### 3. 程序化使用

#### 获取当前主题配置

```javascript
import { useAppStore } from '@/store'

const appStore = useAppStore()
const currentTheme = appStore.themeConfig
```

#### 更新主题配置

```javascript
// 更新整个主题
appStore.setThemeConfig({
  header: { ... },
  topMenu: { ... },
  sideMenu: { ... }
})

// 只更新 Header 配置
appStore.updateHeaderConfig({
  backgroundColor: '#1e40af',
  textColor: '#ffffff'
})

// 只更新顶部菜单配置
appStore.updateTopMenuConfig({
  textColorActive: '#ff0000'
})

// 只更新侧边菜单配置
appStore.updateSideMenuConfig({
  backgroundColor: '#18181c'
})
```

### 4. 暗色模式支持

系统会自动根据暗色模式切换相应的主题配置：

- 浅色模式：使用 `header`、`topMenu`、`sideMenu` 配置
- 暗色模式：使用 `headerDark`、`topMenuDark`、`sideMenuDark` 配置

如果没有定义暗色模式配置，会自动回退到浅色模式配置。

### 5. CSS 变量

所有主题配置都会转换为 CSS 变量，你可以在样式中直接使用：

```css
.my-component {
  background-color: var(--layout-header-bg-color);
  color: var(--layout-header-text-color);
  font-size: var(--layout-header-font-size);
}
```

可用的 CSS 变量：

**Header:**
- `--layout-header-bg-color`
- `--layout-header-text-color`
- `--layout-header-font-size`
- `--layout-header-height`
- `--layout-header-border-color`

**顶部菜单:**
- `--top-menu-text-color`
- `--top-menu-text-color-hover`
- `--top-menu-text-color-active`
- `--top-menu-bg-color`
- `--top-menu-bg-color-hover`
- `--top-menu-bg-color-active`
- `--top-menu-font-size`
- `--top-menu-font-weight`

**侧边菜单:**
- `--side-menu-bg-color`
- `--side-menu-text-color`
- `--side-menu-text-color-hover`
- `--side-menu-text-color-active`
- `--side-menu-bg-color-hover`
- `--side-menu-bg-color-active`
- `--side-menu-border-color`
- `--side-menu-font-size`
- `--side-menu-font-weight`
- `--side-menu-icon-color`
- `--side-menu-icon-color-active`
- `--side-menu-collapsed-width`
- `--side-menu-width`

## 最佳实践

1. **使用预设主题**: 优先使用预设主题，可以保证视觉一致性
2. **导出配置**: 配置好主题后导出 JSON 文件，便于团队共享和版本控制
3. **暗色模式**: 为暗色模式单独配置主题，提供更好的用户体验
4. **测试**: 修改主题后在不同布局模式下测试效果

## 示例

### 创建自定义主题

```javascript
// 在 theme.config.js 中添加
export const themePresets = {
  // ... 现有主题
  
  custom: {
    header: {
      backgroundColor: '#8b5cf6',
      textColor: '#ffffff',
      fontSize: '15px',
      height: '64px',
      borderColor: '#7c3aed',
    },
    topMenu: {
      textColor: '#ffffff',
      textColorHover: '#e9d5ff',
      textColorActive: '#ffffff',
      backgroundColor: 'transparent',
      backgroundColorHover: 'rgba(255, 255, 255, 0.1)',
      backgroundColorActive: 'rgba(255, 255, 255, 0.2)',
      fontSize: '15px',
      fontWeight: '600',
    },
    sideMenu: {
      backgroundColor: '#fafafa',
      textColor: '#333333',
      textColorHover: '#8b5cf6',
      textColorActive: '#8b5cf6',
      backgroundColorHover: '#f5f3ff',
      backgroundColorActive: '#ede9fe',
      borderColor: '#e5e7eb',
      fontSize: '14px',
      fontWeight: '400',
      iconColor: '#666666',
      iconColorActive: '#8b5cf6',
      collapsedWidth: '64px',
      width: '220px',
    },
  },
}
```

### 在组件中使用

```vue
<script setup>
import { useAppStore } from '@/store'
import { themePresets } from '@/config/theme.config'

const appStore = useAppStore()

// 应用自定义主题
function applyCustomTheme() {
  appStore.setThemeConfig(themePresets.custom)
}
</script>
```

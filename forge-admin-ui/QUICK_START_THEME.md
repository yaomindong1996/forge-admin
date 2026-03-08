# 主题配置系统 - 快速开始

## 🎯 5分钟快速上手

### 1️⃣ 打开主题配置面板

点击页面右上角的 **调色板图标** 🎨

### 2️⃣ 配置你的主题

在弹出的配置面板中，有三个标签页：

#### 📌 Header 标签
- **背景颜色**: 设置 Header 的背景色
- **文字颜色**: 设置 Header 中文字的颜色
- **字体大小**: 调整 Header 文字大小（12-20px）
- **边框颜色**: 设置 Header 下方边框的颜色

#### 📌 顶部菜单 标签
- **文字颜色**: 菜单项的默认文字颜色
- **悬停颜色**: 鼠标悬停时的文字颜色
- **选中颜色**: 当前选中菜单项的文字颜色
- **背景悬停色**: 鼠标悬停时的背景颜色
- **背景选中色**: 当前选中菜单项的背景颜色
- **字体大小**: 调整菜单文字大小（12-18px）

#### 📌 侧边菜单 标签
- **背景颜色**: 侧边栏的背景色
- **文字颜色**: 菜单项的默认文字颜色
- **悬停颜色**: 鼠标悬停时的文字颜色
- **选中颜色**: 当前选中菜单项的文字颜色
- **背景悬停色**: 鼠标悬停时的背景颜色
- **背景选中色**: 当前选中菜单项的背景颜色
- **图标颜色**: 菜单图标的默认颜色
- **选中图标色**: 选中菜单项的图标颜色
- **字体大小**: 调整菜单文字大小（12-18px）
- **边框颜色**: 侧边栏边框的颜色

### 3️⃣ 实时预览

每次调整颜色或大小，主题会 **立即生效**，可以实时查看效果！

### 4️⃣ 保存主题

点击底部的 **"应用主题"** 按钮，保存你的配置。

### 5️⃣ 导出配置（可选）

点击 **"导出配置"** 按钮，将你的主题配置保存为 JSON 文件，方便：
- 📤 分享给团队成员
- 💾 备份你的配置
- 🔄 在不同环境中使用

---

## 🎨 快速切换预设主题

### 方式一：通过代码切换

在浏览器控制台输入：

```javascript
// 应用蓝色主题
const { themePresets } = await import('/src/config/theme.config.js')
const appStore = useAppStore()
appStore.setThemeConfig(themePresets.blue)

// 应用深色主题
appStore.setThemeConfig(themePresets.dark)

// 应用绿色主题
appStore.setThemeConfig(themePresets.green)
```

### 方式二：在组件中切换

```vue
<script setup>
import { useAppStore } from '@/store'
import { themePresets } from '@/config/theme.config'

const appStore = useAppStore()

function switchToBlueTheme() {
  appStore.setThemeConfig(themePresets.blue)
}
</script>
```

---

## 💡 常见使用场景

### 场景1: 公司品牌色适配
1. 打开主题配置
2. 将 Header 背景色改为公司主色
3. 将选中颜色改为公司辅色
4. 导出配置文件

### 场景2: 暗色模式优化
系统会自动根据暗色模式切换主题：
- 浅色模式 → 使用浅色配置
- 暗色模式 → 使用暗色配置

你可以分别为两种模式定制不同的配置。

### 场景3: 提高可读性
1. 增大字体大小（Header 16px，菜单 15px）
2. 提高对比度（深色文字 + 浅色背景）
3. 调整悬停和选中效果，让当前位置更明显

---

## ⚙️ 配置文件位置

如果你想直接修改配置文件：

**配置文件**: `/src/config/theme.config.js`

```javascript
export const defaultThemeConfig = {
  header: {
    backgroundColor: '#ffffff',
    textColor: '#333333',
    fontSize: '14px',
    // ...
  },
  // ...
}
```

修改后刷新页面即可生效！

---

## 🔧 高级用法

### 程序化更新主题

```javascript
import { useAppStore } from '@/store'

const appStore = useAppStore()

// 只更新 Header 背景色
appStore.updateHeaderConfig({
  backgroundColor: '#1e40af'
})

// 只更新顶部菜单选中色
appStore.updateTopMenuConfig({
  textColorActive: '#ef4444'
})

// 只更新侧边菜单背景色
appStore.updateSideMenuConfig({
  backgroundColor: '#f9fafb'
})
```

### 获取当前主题配置

```javascript
import { useAppStore } from '@/store'

const appStore = useAppStore()
const currentTheme = appStore.themeConfig

console.log('当前 Header 背景色:', currentTheme.header.backgroundColor)
```

---

## ❓ 常见问题

### Q: 修改主题后刷新页面会丢失吗？
A: 不会！主题配置会自动保存到 sessionStorage，关闭浏览器后才会重置。

### Q: 如何恢复默认主题？
A: 点击配置面板底部的 **"重置为默认"** 按钮。

### Q: 可以为不同布局设置不同主题吗？
A: 当前版本是全局主题，所有布局共享。如有需求可以扩展。

### Q: 暗色模式如何自定义？
A: 在配置文件中修改 `headerDark`、`topMenuDark`、`sideMenuDark` 配置项。

### Q: 导出的配置文件如何导入？
A: 当前版本暂不支持导入，可以手动复制到配置文件中。

---

## 📞 需要帮助？

详细文档：[THEME_CONFIG.md](./docs/THEME_CONFIG.md)  
完整说明：[THEME_SYSTEM_README.md](./THEME_SYSTEM_README.md)

---

**享受你的自定义主题吧！** 🎉

# 工具函数（新增）

本文档记录新增的前端工具函数。

## clipboard — 剪贴板

复制文本到剪贴板。

### `copyToClipboard(text, onSuccess, onError)`

底层复制方法，使用 `navigator.clipboard` API，带 `execCommand` 降级。

```js
import { copyToClipboard } from '@/utils/clipboard'

copyToClipboard(
  'Hello World',
  () => console.log('复制成功'),
  (err) => console.error('复制失败', err)
)
```

### `copy(text, successMsg, errorMsg)`

高级复制方法，成功后自动弹出 Naive UI 消息提示。

```js
import { copy } from '@/utils/clipboard'

copy('复制的内容', '复制成功', '复制失败')
```

---

## websocket — WebSocket 客户端

基于 SockJS + STOMP 的 WebSocket 客户端。

### `initWebSocketClient()`

初始化并连接 WebSocket 服务。

```js
import { initWebSocketClient, disconnectWebSocketClient } from '@/utils/websocket'

// 初始化连接
initWebSocketClient()

// 断开连接
disconnectWebSocketClient()
```

### 功能

- 连接到 `/ws` 端点
- 开发环境自动适配 `VITE_HTTP_PROXY_TARGET`
- 订阅 `/topic/auth` 主题
- 自动处理认证相关广播（踢出、替换、封禁）并自动登出

### 依赖

- `sockjs-client`
- `@stomp/stompjs`
- `@/store`（useAuthStore, useUserStore）

---

## watermark — 水印

全屏水印管理工具，基于 Naive UI `NWatermark` 组件。

### `createWatermark(options)`

创建水印，返回 `{ destroy() }`。

```js
import { createWatermark } from '@/utils/watermark'

const wm = createWatermark({
  content: '内部机密',
  fontSize: 16,
  fontColor: '#cccccc',
  rotate: -20,
  xGap: 200,
  yGap: 200,
  zIndex: 1000
})

// 销毁水印
wm.destroy()
```

### `setupWatermark()`

获取水印管理器，支持 show/hide/update。

```js
import { setupWatermark } from '@/utils/watermark'

const watermark = setupWatermark()

watermark.show({ content: '内部机密' })
watermark.hide()
watermark.update({ content: '新的水印' })
```

---

## tab — 标签页管理

多标签页导航管理工具。

```js
import {
  closePage,
  closeAndOpen,
  reloadPage,
  closeOtherPages,
  closeAllPages
} from '@/utils/tab'

// 关闭指定标签页
closePage('/user/list')

// 关闭并打开新页面
closeAndOpen('/old-path', '/new-path')

// 刷新当前页面
reloadPage('/user/list')

// 关闭其他页面
closeOtherPages('/user/list')

// 关闭所有页面
closeAllPages()
```

### 导出方法

| 方法 | 说明 |
|------|------|
| `closePage(target)` | 关闭指定标签页 |
| `closePages(targets)` | 批量关闭标签页 |
| `closeAndOpen(closeTarget, openTarget)` | 关闭旧页面，打开新页面 |
| `reloadPage(path)` | 刷新指定页面 |
| `closeOtherPages(path)` | 关闭除指定页面外的所有标签页 |
| `closeLeftPages(path)` | 关闭左侧标签页 |
| `closeRightPages(path)` | 关闭右侧标签页 |
| `closeAllPages()` | 关闭所有标签页 |

---

## echarts-theme — ECharts 主题

ECharts 主题配置，支持 CSS 变量集成和暗色模式。

### `getEChartsTheme()`

获取完整的 ECharts 主题配置（从 CSS 变量派生）。

```js
import { getEChartsTheme, chartPresets } from '@/utils/echarts-theme'

const theme = getEChartsTheme()
chart.setOption({ ...chartPresets.line, theme })
```

### `chartPresets`

预置图表配置：

| 预设 | 说明 |
|------|------|
| `line` | 折线图 |
| `area` | 面积图 |
| `bar` | 柱状图 |
| `pie` | 饼图 |
| `scatter` | 散点图 |
| `radar` | 雷达图 |
| `gauge` | 仪表盘 |

### `getResponsiveConfig(containerWidth)`

根据容器宽度返回响应式网格/标签配置。

### 其他方法

| 方法 | 说明 |
|------|------|
| `generateGradientColor(color, alpha)` | 生成渐变色（Hex → RGBA） |
| `generateChartGradient(ctx, color, direction)` | 创建线性渐变 |
| `toggleEChartsTheme(chartInstance)` | 切换 ECharts 主题 |
| `applyThemeToChart(chartInstance, customTheme)` | 应用自定义主题 |

---

## responsive-font — 响应式字体

根据屏幕宽度自动缩放字体大小。

```js
import { calculateFontScale, initResponsiveFont } from '@/utils/responsive-font'

// 手动计算
const scale = calculateFontScale()

// 自动初始化（带防抖 resize 监听）
initResponsiveFont(() => {
  console.log('字体缩放已更新')
})
```

### 断点

| 屏幕宽度 | 缩放比例 |
|----------|---------|
| ≤ 768px | 0.8 |
| ≤ 1024px | 0.85 |
| ≤ 1440px | 0.9 |
| > 1440px | 1.0 |

---

## enum — 字典/枚举

字典数据获取、缓存和查找。

```js
import { getEnum, getEnumLabel, batchGetEnum } from '@/utils/enum'

// 获取字典（自动缓存）
const items = await getEnum('user_status')

// 查找 Label
const label = getEnumLabel('user_status', 1)  // → "启用"

// 批量获取
const enums = await batchGetEnum(['user_status', 'user_type'])
```

### 导出方法

| 方法 | 说明 |
|------|------|
| `getEnum(type, version, forceRefresh)` | 获取字典数据 |
| `batchGetEnum(types)` | 批量获取字典 |
| `getEnumLabel(type, value)` | 根据值查找标签 |
| `getEnumValue(type, label)` | 根据标签查找值 |
| `clearEnumCache(types)` | 清除指定字典缓存 |
| `clearAllEnumCache()` | 清除所有字典缓存 |

---

## is — 类型检查

类型检查工具函数。

```js
import { isString, isArray, isNumber, isEmpty, isUrl } from '@/utils/is'

isString('hello')    // true
isArray([1, 2, 3])   // true
isNumber(42)         // true
isEmpty({})          // true
isUrl('https://...') // true
```

### 导出方法

`is(val, type)`, `isDef`, `isUndef`, `isNull`, `isWhitespace`, `isObject`, `isArray`, `isString`, `isNumber`, `isBoolean`, `isDate`, `isRegExp`, `isFunction`, `isPromise`, `isElement`, `isWindow`, `isNullOrUndef`, `isNullOrWhitespace`, `isEmpty`, `ifNull(val, def)`, `isUrl`, `isExternal`, `isServer`, `isClient`

---

## menu-utils — 菜单工具

菜单数据处理工具，将原始菜单数据转为 Naive UI 菜单格式。

```js
import { processMenuData, findActiveTopMenu, findMenuItem } from '@/utils/menu-utils'

const { topMenus, sideMenus } = processMenuData(rawMenuData)
const activeTop = findActiveTopMenu(topMenus, route)
const item = findMenuItem(topMenus, 'system-user')
```

### 导出方法

| 方法 | 说明 |
|------|------|
| `processTopMenus(menuItems)` | 处理顶级菜单，生成唯一 ID/Key，包装图标 |
| `processMenuData(menuItems)` | 扁平化 `subapp` 类型，处理子项，转换图标为渲染函数 |
| `findActiveTopMenu(menus, route)` | 查找当前路由对应的顶级菜单 |
| `findMenuItem(menuItems, key)` | 递归查找指定 Key/ID 的菜单项 |

---

## naiveTools — Naive UI 增强

Naive UI 离散 API 的全局化和增强。

### 功能

- **Message 增强** — 键值去重（防止重复弹出）、loading 状态支持
- **Dialog 增强** — 添加 `NDialog.confirm()` 快捷方法
- **全局挂载** — `window.$message`, `window.$dialog`, `window.$notification`, `window.$loadingBar`, `window.$loading`, `window.$copy`, `window.$imagePreview`, `window.$watermark`, `window.$homePath`
- **暗色模式** — 自动适配主题
- **Loading 服务** — 全屏 Loading 遮罩（`show()`, `close()`, `service()`）

### 入口函数

```js
import { setupNaiveDiscreteApi } from '@/utils/naiveTools'

// 在应用初始化时调用
setupNaiveDiscreteApi()
```

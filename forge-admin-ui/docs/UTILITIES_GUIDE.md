# 实用工具使用指南

本文档介绍项目中基于 Naive UI 实现的实用工具功能，包括复制到剪贴板、图片预览和水印功能。

## 目录

- [复制到剪贴板](#复制到剪贴板)
- [图片预览](#图片预览)
- [水印功能](#水印功能)
- [全屏 Loading](#全屏-loading)

---

## 复制到剪贴板

### 全局方法使用

#### 基础用法

```javascript
// 复制文本
window.$copy('要复制的文本内容')

// 自定义成功和失败提示
window.$copy(
  '要复制的文本内容',
  '复制成功！', // 成功提示
  '复制失败，请重试' // 失败提示
)
```

### 指令使用

#### 基础用法

```vue
<template>
  <!-- 字符串形式 - 点击复制固定文本 -->
  <button v-copy="'要复制的内容'">复制</button>

  <!-- 对象形式 - 自定义提示消息 -->
  <button v-copy="{
    text: '要复制的内容',
    successMessage: '已复制到剪贴板',
    errorMessage: '复制失败'
  }">
    复制
  </button>

  <!-- 复制动态内容 -->
  <button v-copy="userInfo.email">复制邮箱</button>
</template>
```

#### 配置选项

| 参数 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| text | String | 要复制的文本内容 | - |
| successMessage | String | 复制成功的提示消息 | '复制成功' |
| errorMessage | String | 复制失败的提示消息 | '复制失败' |

---

## 图片预览

### 全局方法使用

#### 预览单张图片

```javascript
// 预览单张图片
window.$imagePreview.show('https://example.com/image.jpg')
```

#### 预览多张图片

```javascript
// 预览图片列表
const images = [
  'https://example.com/image1.jpg',
  'https://example.com/image2.jpg',
  'https://example.com/image3.jpg'
]

// 从第一张开始预览
window.$imagePreview.showList(images)

// 从第二张开始预览（索引从 0 开始）
window.$imagePreview.showList(images, 1)
```

### 指令使用

#### 基础用法

```vue
<template>
  <!-- 直接预览图片本身 -->
  <img 
    src="https://example.com/thumb.jpg" 
    v-preview 
    alt="图片"
  />

  <!-- 预览不同的图片（常用于缩略图场景） -->
  <img 
    src="https://example.com/thumb.jpg" 
    v-preview="'https://example.com/full-size.jpg'"
    alt="缩略图"
  />

  <!-- 预览响应式数据 -->
  <img 
    :src="product.thumbnail" 
    v-preview="product.fullImage"
    alt="产品图"
  />
</template>
```

#### 注意事项

- `v-preview` 指令**仅支持 `<img>` 元素**
- 点击图片后会自动添加 `cursor: pointer` 样式
- 支持键盘操作（左右箭头切换图片，ESC 关闭预览）

---

## 水印功能

### 全局方法使用

#### 显示全屏水印

```javascript
// 简单文本水印
window.$watermark.show('机密文件')

// 完整配置
window.$watermark.show({
  content: '内部资料',
  fontSize: 20,
  fontColor: 'rgba(255, 0, 0, .1)',
  width: 150,
  height: 80,
  xGap: 20,
  yGap: 80,
  rotate: -22,
  zIndex: 1000
})

// 图片水印
window.$watermark.show({
  image: 'https://example.com/logo.png',
  imageWidth: 120,
  imageHeight: 60,
  imageOpacity: 0.3
})
```

#### 隐藏水印

```javascript
window.$watermark.hide()
```

#### 更新水印

```javascript
// 更新水印内容
window.$watermark.update('新的水印内容')

// 更新完整配置
window.$watermark.update({
  content: '更新后的水印',
  fontSize: 18
})
```

### 指令使用

#### 基础用法

```vue
<template>
  <!-- 为指定元素添加水印 -->
  <div v-watermark="'机密文件'" class="content">
    内容区域
  </div>

  <!-- 完整配置 -->
  <div v-watermark="{
    content: '内部资料',
    fontSize: 20,
    fontColor: 'rgba(255, 0, 0, .1)',
    rotate: -22
  }" class="document">
    文档内容
  </div>

  <!-- 图片水印 -->
  <div v-watermark="{
    image: '/logo.png',
    imageWidth: 100,
    imageHeight: 50,
    imageOpacity: 0.2
  }" class="canvas">
    画布内容
  </div>
</template>

<style scoped>
.content, .document, .canvas {
  position: relative; /* 指令会自动添加相对定位 */
  min-height: 400px;
  padding: 20px;
}
</style>
```

### 配置选项

| 参数 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| content | String | 水印文本内容 | '' |
| image | String | 水印图片地址 | '' |
| fontSize | Number | 字体大小 | 16 |
| fontColor | String | 字体颜色（RGBA） | 'rgba(128, 128, 128, .15)' |
| fontWeight | Number | 字体粗细 | 400 |
| fontFamily | String | 字体家族 | - |
| width | Number | 水印宽度 | 120 |
| height | Number | 水印高度 | 64 |
| xGap | Number | 水印水平间距 | 12 |
| yGap | Number | 水印垂直间距 | 60 |
| xOffset | Number | 水印水平偏移 | 0 |
| yOffset | Number | 水印垂直偏移 | 0 |
| rotate | Number | 水印旋转角度（度） | -22 |
| zIndex | Number | 层级 | 全局: 1000, 指令: 1 |
| fullscreen | Boolean | 是否全屏（仅全局方法） | true |
| selectable | Boolean | 文本是否可选 | false |
| imageWidth | Number | 图片宽度 | - |
| imageHeight | Number | 图片高度 | - |
| imageOpacity | Number | 图片透明度 | 1 |

---

## 全屏 Loading

### 使用方法

#### 显示 Loading

```javascript
// 简单文本
window.$loading.show('加载中...')

// 完整配置
window.$loading.show({
  text: '数据加载中，请稍候...',
  background: '0, 0, 0, 0.8', // RGB + Alpha
  color: '#18a058',
  fontSize: '14px'
})
```

#### 关闭 Loading

```javascript
window.$loading.close()
```

#### 异步操作示例

```javascript
async function fetchData() {
  window.$loading.show('正在获取数据...')
  
  try {
    const response = await fetch('/api/data')
    const data = await response.json()
    // 处理数据...
  } catch (error) {
    window.$message.error('获取数据失败')
  } finally {
    window.$loading.close()
  }
}
```

### v-loading 指令

```vue
<template>
  <!-- 布尔值控制 -->
  <div v-loading="isLoading">
    内容区域
  </div>

  <!-- 对象配置 -->
  <div v-loading="{
    loading: isLoading,
    text: '加载中...',
    background: '255, 255, 255, 0.9'
  }">
    内容区域
  </div>
</template>

<script setup>
import { ref } from 'vue'

const isLoading = ref(false)

async function loadData() {
  isLoading.value = true
  try {
    // 加载数据...
  } finally {
    isLoading.value = false
  }
}
</script>
```

---

## 实际应用场景

### 场景 1：表格操作按钮

```vue
<template>
  <n-data-table :columns="columns" :data="data" />
</template>

<script setup>
const columns = [
  { title: '用户名', key: 'username' },
  { title: '邮箱', key: 'email' },
  {
    title: '操作',
    key: 'actions',
    render(row) {
      return h('div', [
        h('button', {
          'v-copy': row.email
        }, '复制邮箱')
      ])
    }
  }
]
</script>
```

### 场景 2：产品图片展示

```vue
<template>
  <div class="product-gallery">
    <img
      v-for="(image, index) in product.images"
      :key="index"
      :src="image.thumbnail"
      v-preview="image.fullsize"
      class="thumbnail"
    />
  </div>
</template>
```

### 场景 3：敏感文档水印

```vue
<template>
  <div v-watermark="{
    content: `${userName} ${currentDate}`,
    fontSize: 14,
    fontColor: 'rgba(0, 0, 0, .08)',
    rotate: -15
  }" class="document-viewer">
    <div v-html="documentContent"></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/store'

const userStore = useUserStore()
const userName = computed(() => userStore.userName)
const currentDate = computed(() => new Date().toLocaleDateString())
</script>
```

### 场景 4：文件上传

```vue
<template>
  <div>
    <input type="file" @change="handleUpload" />
  </div>
</template>

<script setup>
async function handleUpload(event) {
  const file = event.target.files[0]
  if (!file) return

  const loading = window.$loading.show('文件上传中...')
  
  try {
    const formData = new FormData()
    formData.append('file', file)
    
    const response = await fetch('/api/upload', {
      method: 'POST',
      body: formData
    })
    
    if (response.ok) {
      window.$message.success('上传成功')
    } else {
      throw new Error('上传失败')
    }
  } catch (error) {
    window.$message.error(error.message)
  } finally {
    loading.close()
  }
}
</script>
```

---

## 兼容性说明

- **复制功能**：优先使用现代 Clipboard API，在不支持的浏览器中自动降级到 `document.execCommand('copy')`
- **图片预览**：基于 Naive UI 的 `NImageGroup` 组件，支持所有 Naive UI 支持的浏览器
- **水印功能**：基于 Naive UI 的 `NWatermark` 组件，使用 Canvas 绘制
- **Loading**：支持现代浏览器，使用 CSS3 动画

---

## 常见问题

### Q: 复制功能在某些浏览器不工作？

A: 请确保页面在 HTTPS 协议下运行，或者是 localhost。部分浏览器的 Clipboard API 仅在安全上下文中可用。

### Q: v-preview 指令不生效？

A: 请确保该指令仅用于 `<img>` 元素上，其他元素不支持。

### Q: 水印可以被删除吗？

A: 虽然用户可以通过浏览器开发者工具删除水印元素，但这只是前端显示层的防护。重要数据应该在后端进行权限控制。

### Q: 如何在路由切换时自动清除水印？

A: 可以在路由守卫中调用 `window.$watermark.hide()`：

```javascript
router.beforeEach((to, from, next) => {
  window.$watermark.hide()
  next()
})
```

---

## 技术实现

- 基于 Vue 3 Composition API
- 使用 Naive UI 组件库
- 支持 TypeScript（类型定义来自 Naive UI）
- 单例模式管理全局实例
- 自动内存清理机制

# 文件 URL 使用指南

## 概述

后端文件上传后会返回文件元数据，前端需要将文件路径转换为可访问的 URL。

## 后端返回的数据格式

```javascript
{
  fileId: "1234567890",  // 文件唯一ID（必需，用于访问文件）
  filePath: "tenant-logo/2025/12/10/a111492084ab4a5fb3f149f76b5f8c4c.png",  // 文件存储路径（仅用于记录）
  accessUrl: "http://example.com/uploads/...",  // 可选：直接访问URL
  originalName: "logo.png",
  fileSize: 12345,
  // ... 其他字段
}
```

**重要**: 前端必须使用 `fileId` 来访问文件，`filePath` 仅用于后端存储，无法直接访问。

## 使用方法

### 1. 导入工具函数

```javascript
import { getFileDownloadUrl, getFileUrl, getImageUrl } from '@/utils'
```

### 2. 获取文件访问 URL

#### 方式一：传入完整的文件元数据对象

```javascript
const fileData = {
  fileId: '1234567890',
  filePath: 'tenant-logo/2025/12/10/xxx.png',
  accessUrl: 'http://example.com/uploads/xxx.png'
}

const url = getFileUrl(fileData)
// 优先级: accessUrl > filePath > fileId
```

#### 方式二：传入文件 ID 字符串（推荐）

```javascript
const fileId = '1234567890'
const url = getFileUrl(fileId)
// 结果: /dev-api/api/file/download/1234567890
```

**注意**: 不要直接传入 `filePath`，因为后端没有静态资源映射，无法通过路径访问文件。

### 3. 在模板中使用

#### 显示图片

```vue
<template>
  <!-- 方式一：使用 v-bind -->
  <img :src="getFileUrl(row.systemLogo)" alt="Logo">

  <!-- 方式二：使用计算属性 -->
  <img :src="logoUrl" alt="Logo">

  <!-- 方式三：使用 n-image 组件 -->
  <n-image :src="getFileUrl(row.systemLogo)" />
</template>

<script setup>
import { computed } from 'vue'
import { getFileUrl } from '@/utils'

const props = defineProps({
  row: Object
})

const logoUrl = computed(() => getFileUrl(props.row.systemLogo))
</script>
```

#### 下载文件

```vue
<template>
  <a :href="getFileDownloadUrl(fileId)" download>下载文件</a>
</template>

<script setup>
import { getFileDownloadUrl } from '@/utils'

const fileId = '1234567890'
</script>
```

### 4. 获取缩略图 URL

```javascript
import { getImageUrl } from '@/utils'

// 基础用法
const url = getImageUrl(filePath)

// 指定尺寸
const thumbnailUrl = getImageUrl(filePath, {
  width: 200,
  height: 200,
  mode: 'crop' // 'fit' | 'fill' | 'crop'
})
```

## API 说明

### getFileUrl(fileData)

获取文件访问 URL

**参数:**

- `fileData` - 文件路径字符串或文件元数据对象
  - 字符串: `"tenant-logo/2025/12/10/xxx.png"` 或 `fileId`
  - 对象: `{ fileId, filePath, accessUrl }`

**返回:** `string` - 完整的文件访问 URL

**优先级:**

1. 如果是对象，优先使用 `accessUrl`
2. 然后使用 `filePath`
3. 最后使用 `fileId`

### getFileDownloadUrl(fileId)

获取文件下载 URL

**参数:**

- `fileId` - 文件ID

**返回:** `string` - 文件下载 URL

### getImageUrl(filePath, options)

获取图片预览 URL（带缩略图参数）

**参数:**

- `filePath` - 文件路径
- `options` - 选项对象（可选）
  - `width` - 宽度
  - `height` - 高度
  - `mode` - 缩放模式: `'fit'` | `'fill'` | `'crop'`

**返回:** `string` - 图片预览 URL

## 环境配置

文件 URL 会根据环境自动添加前缀：

- **开发环境** (`VITE_REQUEST_PREFIX=/dev-api`):
  - 下载接口: `/dev-api/api/file/download/1234567890`

- **生产环境** (`VITE_REQUEST_PREFIX=/cbc-server`):
  - 下载接口: `/cbc-server/api/file/download/1234567890`

## 注意事项

1. **必须使用 fileId**: 后端没有静态资源映射，所有文件访问必须通过 `fileId` 和下载接口
2. **上传组件自动处理**: 图片上传和文件上传组件会自动保存 `fileId` 并用于访问
3. **数据库存储**: 建议在数据库中存储 `fileId` 而不是 `filePath`
4. **跨域问题**: 如果文件服务器和应用服务器不在同一域名，需要配置 CORS
5. **完整 URL**: 如果后端返回的 `accessUrl` 是完整的 URL（包含 http/https），会直接使用，不会添加前缀

## 完整示例

```vue
<template>
  <div class="file-display">
    <!-- 显示图片 -->
    <n-image
      v-if="fileData.filePath"
      :src="getFileUrl(fileData)"
      width="200"
    />

    <!-- 下载按钮 -->
    <n-button
      type="primary"
      @click="handleDownload"
    >
      下载文件
    </n-button>
  </div>
</template>

<script setup>
import { getFileDownloadUrl, getFileUrl } from '@/utils'

const fileData = {
  fileId: '1234567890',
  filePath: 'tenant-logo/2025/12/10/a111492084ab4a5fb3f149f76b5f8c4c.png',
  originalName: 'logo.png'
}

function handleDownload() {
  const url = getFileDownloadUrl(fileData.fileId)
  window.open(url, '_blank')
}
</script>
```

# 文件处理工具

## 获取文件访问 URL

```js
import { getFileUrl, getFileDownloadUrl, getImageUrl } from '@/utils/file'

// 通过 fileId 获取访问 URL
const url = getFileUrl('abc123')
// 返回: '/api/file/download/abc123'

// 通过文件对象获取
const url = getFileUrl({
  fileId: 'abc123',
  filePath: '/uploads/2024/01/file.pdf',
  accessUrl: 'https://cdn.example.com/file.pdf'
})
// 优先使用 accessUrl，其次 fileId

// 获取下载 URL
const url = getFileDownloadUrl('abc123')

// 获取图片 URL（带缩略图参数）
const url = getImageUrl('abc123', {
  width: 200,
  height: 200,
  mode: 'crop'  // fit | fill | crop
})
```

## API 参考

| 函数 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| getFileUrl | fileData | string | 获取文件访问 URL |
| getFileDownloadUrl | fileId | string | 获取文件下载 URL |
| getImageUrl | filePath, options? | string | 获取图片 URL |

## 参数说明

### getFileUrl(fileData)

`fileData` 支持以下格式：

```js
// 字符串（fileId 或 filePath）
getFileUrl('abc123')
getFileUrl('/uploads/file.pdf')

// 对象
getFileUrl({
  fileId: 'abc123',
  filePath: '/uploads/file.pdf',
  accessUrl: 'https://cdn.example.com/file.pdf'
})
```

### getImageUrl(filePath, options)

```js
getImageUrl('abc123', {
  width: 200,      // 宽度
  height: 200,     // 高度
  mode: 'crop'     // 缩放模式
})
```

| mode | 说明 |
|------|------|
| fit | 等比缩放，完整显示 |
| fill | 等比缩放，填充裁剪 |
| crop | 按指定尺寸裁剪 |